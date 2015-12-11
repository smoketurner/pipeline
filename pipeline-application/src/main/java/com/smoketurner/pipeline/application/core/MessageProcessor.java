/**
 * Copyright 2015 Smoke Turner, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.smoketurner.pipeline.application.core;

import static com.codahale.metrics.MetricRegistry.name;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.sse.OutboundEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.sqs.model.Message;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.smoketurner.pipeline.application.aws.AmazonEventRecord;
import com.smoketurner.pipeline.application.aws.AmazonEventRecords;
import com.smoketurner.pipeline.application.aws.AmazonSNSNotification;
import com.smoketurner.pipeline.application.exceptions.AmazonS3ConstraintException;
import com.smoketurner.pipeline.application.exceptions.AmazonS3ZeroSizeException;

import io.dropwizard.jackson.Jackson;

public class MessageProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(MessageProcessor.class);
  private static final ObjectMapper MAPPER = Jackson.newObjectMapper();
  private final OutboundEvent.Builder event =
      new OutboundEvent.Builder().name("event").mediaType(MediaType.APPLICATION_JSON_TYPE);
  private final AmazonS3Downloader s3;
  private final InstrumentedSseBroadcaster broadcaster;

  // metrics
  private final Histogram recordCounts;

  /**
   * Constructor
   *
   * @param registry Metric registry
   * @param s3 S3 Downloader
   * @param broadcaster SSE broadcaster
   */
  public MessageProcessor(@Nonnull final MetricRegistry registry,
      @Nonnull final AmazonS3Downloader s3, @Nonnull final InstrumentedSseBroadcaster broadcaster) {
    Preconditions.checkNotNull(registry);
    this.s3 = Preconditions.checkNotNull(s3);
    this.broadcaster = Preconditions.checkNotNull(broadcaster);
    this.recordCounts = registry.histogram(name(MessageProcessor.class, "record-counts"));
  }

  /**
   * Process an SQS {@link Message} by parsing the SNS notification out of the message body. Then
   * download the S3 object out of the SNS notification, decompress the object, then broadcast each
   * event.
   * 
   * @param message SQS message
   * @return true if the file was fully processed (and the message can be deleted from SQS),
   *         otherwise false.
   */
  public boolean process(@Nonnull final Message message) {
    if (message == null) {
      return false;
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Received SQS message: {}", message);
    } else if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Received SQS message: {}", message.getMessageId());
    }

    if (broadcaster.isEmpty()) {
      LOGGER.debug("No connections found, skipping SQS message processing");
      return false;
    }

    final AmazonSNSNotification notification;
    try {
      notification = MAPPER.readValue(message.getBody(), AmazonSNSNotification.class);
    } catch (IOException e) {
      LOGGER.error("Failed to parse SNS notification, deleting SQS message", e);
      return true;
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Parsed SNS notification: {}", notification);
    } else if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("SNS notification created at: {}", notification.getTimestamp());
    }

    final AmazonEventRecords records;
    try {
      records = MAPPER.readValue(notification.getMessage(), AmazonEventRecords.class);
    } catch (IOException e) {
      LOGGER.error("Failed to parse S3 event records, deleting SQS message", e);
      return true;
    }

    final int recordCount = records.getRecords().size();
    recordCounts.update(recordCount);

    LOGGER.debug("Parsed {} S3 event records from SNS notification", recordCount);

    if (recordCount < 1) {
      LOGGER.debug("No S3 event records found in SNS notification, deleting SQS message");
      return true;
    }

    int recordsProcessed = 0;

    for (AmazonEventRecord record : records.getRecords()) {
      if (broadcaster.isEmpty()) {
        LOGGER.debug("No connections found, not downloading from S3");
        return false;
      }

      LOGGER.trace("Event Record: {}", record);

      final S3Object download;
      try {
        download = s3.fetch(record);
      } catch (AmazonS3ConstraintException | AmazonS3ZeroSizeException e) {
        LOGGER.error("Unable to download file from S3, skipping to next record", e);
        recordsProcessed++;
        continue;
      } catch (Exception e) {
        LOGGER.error("Failed to download file from S3, skipping remaining records", e);
        return false;
      }

      int eventCount = 0;
      try (S3ObjectInputStream input = download.getObjectContent()) {

        final BufferedReader reader;
        if (AmazonS3Downloader.isGZipped(download)) {
          reader = new BufferedReader(
              new InputStreamReader(new StreamingGZIPInputStream(input), StandardCharsets.UTF_8));
        } else {
          reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        }

        String line = null;
        while ((line = reader.readLine()) != null) {
          // skip empty lines
          if (line.isEmpty()) {
            continue;
          }

          eventCount++;

          broadcaster.broadcast(event.data(line).build());

          // If we have no active connections, assume the broadcast failed
          if (broadcaster.isEmpty()) {
            LOGGER.error("No connections found, aborting download");
            // abort the current S3 download
            input.abort();
            LOGGER.error("Partial events broadcast ({} sent) from key: {}", eventCount,
                download.getKey());
            return false;
          }
        }

      } catch (IOException e) {
        LOGGER.error("Error streaming key: " + download.getKey(), e);
        return false;
      }

      recordsProcessed++;
    }

    // if we've processed all of the records, which includes skipping over empty S3 files, the
    // message has been fully processed.
    if (recordsProcessed == recordCount) {
      LOGGER.debug("Processed {} of {} records, deleting SQS message", recordsProcessed,
          recordCount);
      return true;
    }

    return false;
  }
}
