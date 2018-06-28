/*
 * Copyright © 2018 Smoke Turner, LLC (contact@smoketurner.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smoketurner.pipeline.application.core;

import static com.codahale.metrics.MetricRegistry.name;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.sqs.model.Message;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smoketurner.pipeline.application.aws.AmazonSNSNotification;
import com.smoketurner.pipeline.application.exceptions.AmazonS3ConstraintException;
import com.smoketurner.pipeline.application.exceptions.AmazonS3ZeroSizeException;
import io.dropwizard.jackson.Jackson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageProcessor implements Predicate<Message> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MessageProcessor.class);
  private static final ObjectMapper MAPPER = Jackson.newObjectMapper();
  private final AmazonS3Downloader s3;
  private final InstrumentedSseBroadcaster broadcaster;

  // metrics
  private final Histogram recordCounts;
  private final Histogram eventCounts;

  /**
   * Constructor
   *
   * @param s3 S3 Downloader
   * @param broadcaster SSE broadcaster
   */
  public MessageProcessor(
      @Nonnull final AmazonS3Downloader s3, @Nonnull final InstrumentedSseBroadcaster broadcaster) {
    this.s3 = Objects.requireNonNull(s3);
    this.broadcaster = Objects.requireNonNull(broadcaster);

    final MetricRegistry registry = SharedMetricRegistries.getOrCreate("default");
    this.recordCounts = registry.histogram(name(MessageProcessor.class, "record-counts"));
    this.eventCounts = registry.histogram(name(MessageProcessor.class, "event-counts"));
  }

  /**
   * Process an SQS {@link Message} by parsing the SNS notification out of the message body. Then
   * download the S3 object out of the SNS notification, decompress the object, then broadcast each
   * event.
   *
   * @param message SQS message
   * @return true if the file was fully processed (and the message can be deleted from SQS),
   *     otherwise false.
   */
  @Override
  public boolean test(@Nullable final Message message) {
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

    LOGGER.debug(
        "SNS notification created at: {} ({} behind)",
        notification.getTimestamp(),
        notification.getDelayDuration());

    // if we don't have a valid SNS notification, try parsing the body as S3
    // event records
    final String body;
    if (notification.isValid()) {
      body = notification.getMessage();
    } else {
      body = message.getBody();
    }

    final S3EventNotification records;
    try {
      records = S3EventNotification.parseJson(body);
    } catch (AmazonClientException e) {
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

    for (S3EventNotificationRecord record : records.getRecords()) {
      if (broadcaster.isEmpty()) {
        LOGGER.debug("No connections found, not downloading from S3");
        return false;
      }

      if (processRecord(record)) {
        recordsProcessed++;
      }
    }

    // if we've processed all of the records, which includes skipping over
    // empty S3 files, the message has been fully processed.
    if (recordsProcessed == recordCount) {
      LOGGER.debug(
          "Processed {} of {} records, deleting SQS message", recordsProcessed, recordCount);
      return true;
    }

    LOGGER.debug(
        "Processed {} of {} records, not deleting SQS message: {}",
        recordsProcessed,
        recordCount,
        message.getMessageId());
    return false;
  }

  /**
   * Process an S3 event notification record by streaming object in {@link streamObject}
   *
   * @param record S3 event notification record
   * @return true if the record was fully processed, otherwise false
   */
  private boolean processRecord(@Nonnull final S3EventNotificationRecord record) {
    LOGGER.trace("Event Record: {}", record);

    final S3Object download;
    try {
      download = s3.fetch(record);
    } catch (AmazonS3ConstraintException | AmazonS3ZeroSizeException e) {
      LOGGER.error("Unable to download file from S3, skipping to next record", e);
      return true;
    } catch (AmazonS3Exception e) {
      if (e.getStatusCode() == 404) {
        LOGGER.warn("File does not exist in S3, skipping to next record", e);
        return true;
      }
      LOGGER.error("Amazon S3 exception, skipping remaining records", e);
      return false;
    } catch (Exception e) {
      LOGGER.error("Failed to download file from S3, skipping remaining records", e);
      return false;
    }

    final int eventCount;
    try {
      eventCount = streamObject(download);
    } catch (IOException e) {
      LOGGER.error(
          String.format("Error streaming key: %s/%s", download.getBucketName(), download.getKey()),
          e);
      return false;
    }

    eventCounts.update(eventCount);

    LOGGER.debug(
        "Broadcast {} events from key: {}/{}",
        eventCount,
        download.getBucketName(),
        download.getKey());
    return true;
  }

  /**
   * Stream an {@link S3Object} object and process each line with the processor.
   *
   * @param object S3Object to download and process
   * @return number of events processed
   * @throws IOException if unable to stream the object
   */
  private int streamObject(@Nonnull final S3Object object) throws IOException {

    final AtomicInteger eventCount = new AtomicInteger(0);
    try (S3ObjectInputStream input = object.getObjectContent()) {

      final BufferedReader reader;
      if (AmazonS3Downloader.isGZipped(object)) {
        reader =
            new BufferedReader(
                new InputStreamReader(new StreamingGZIPInputStream(input), StandardCharsets.UTF_8));
      } else {
        reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
      }

      // failed will be true if we did not successfully broadcast all
      // of the events because of no consumers
      final boolean failed =
          reader.lines().peek(event -> eventCount.incrementAndGet()).anyMatch(broadcaster::test);

      if (failed) {
        // abort the current S3 download
        input.abort();
        LOGGER.error(
            "Partial events broadcast ({} sent) from key: {}/{}",
            eventCount.get(),
            object.getBucketName(),
            object.getKey());
        throw new IOException("aborting download");
      }
    }
    return eventCount.get();
  }
}
