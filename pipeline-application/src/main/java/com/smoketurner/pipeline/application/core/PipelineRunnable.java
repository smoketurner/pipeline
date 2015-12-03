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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nonnull;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.sse.OutboundEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.smoketurner.pipeline.application.aws.AmazonEventRecord;
import com.smoketurner.pipeline.application.aws.AmazonEventRecords;
import com.smoketurner.pipeline.application.aws.AmazonSNSNotification;

import io.dropwizard.jackson.Jackson;

public class PipelineRunnable implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(PipelineRunnable.class);
  private static final ObjectMapper MAPPER = Jackson.newObjectMapper();
  private static final OutboundEvent PING_EVENT =
      new OutboundEvent.Builder().name("ping").data("ping").build();

  private final AmazonS3Downloader s3;
  private final AmazonSQSIterator sqs;
  private final SseBroadcasterWithCount broadcaster;

  // metrics
  private final Histogram recordCounts;
  private final Meter eventRate;
  private final Meter pingRate;

  /**
   * Constructor
   *
   * @param s3 Amazon S3 downloader
   * @param sqs Amazon SQS iterator
   * @param registry Metric Registry
   * @param broadcaster SSE broadcaster
   */
  public PipelineRunnable(@Nonnull final AmazonS3Downloader s3,
      @Nonnull final AmazonSQSIterator sqs, @Nonnull final MetricRegistry registry,
      @Nonnull final SseBroadcasterWithCount broadcaster) {

    this.s3 = Preconditions.checkNotNull(s3);
    this.sqs = Preconditions.checkNotNull(sqs);
    this.broadcaster = Preconditions.checkNotNull(broadcaster);

    Preconditions.checkNotNull(registry);
    this.recordCounts = registry.histogram(name(PipelineRunnable.class, "record-counts"));
    this.eventRate = registry.meter(name(PipelineRunnable.class, "broadcast", "event-sends"));
    this.pingRate = registry.meter(name(PipelineRunnable.class, "broadcast", "ping-sends"));
  }

  @Override
  public void run() {
    while (sqs.hasNext() && !Thread.currentThread().isInterrupted()) {

      // send heartbeat ping event to all connections to flush out disconnected clients
      broadcaster.broadcast(PING_EVENT);
      pingRate.mark();

      if (broadcaster.size() < 1) {
        try {
          LOGGER.info("No active connections found, sleeping for 1 second");
          TimeUnit.SECONDS.sleep(1);
          continue;
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }

      final ReceiveMessageResult result = sqs.next();
      for (Message message : result.getMessages()) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Received SQS message: {}", message);
        } else if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Received SQS message: {}", message.getMessageId());
        }

        if (broadcaster.size() < 1) {
          LOGGER.debug("No connections found, skipping SQS message processing");
          break;
        }

        final AmazonSNSNotification notification;
        try {
          notification = MAPPER.readValue(message.getBody(), AmazonSNSNotification.class);
        } catch (IOException e) {
          LOGGER.error("Failed to parse SNS notification", e);
          continue;
        }

        LOGGER.trace("Parsed SNS notification: {}", notification);

        final AmazonEventRecords records;
        try {
          records = MAPPER.readValue(notification.getMessage(), AmazonEventRecords.class);
        } catch (IOException e) {
          LOGGER.error("Failed to parse S3 event records", e);
          continue;
        }

        LOGGER.trace("Parsed S3 event records: {}", records);

        final int recordCount = records.getRecords().size();
        LOGGER.debug("Parsed {} event records from S3", recordCount);
        this.recordCounts.update(recordCount);

        boolean fullyProcessed = false;

        for (AmazonEventRecord record : records.getRecords()) {
          if (broadcaster.size() < 1) {
            LOGGER.debug("No connections found, not downloading from S3");
            break;
          }

          final S3Object download;
          try {
            download = s3.fetch(record);
          } catch (Exception e) {
            LOGGER.error("Failed to download file from S3", e);
            continue;
          }

          try (S3ObjectInputStream input = download.getObjectContent()) {
            final AtomicLong eventCount = new AtomicLong(0L);

            final BufferedReader reader;
            if (AmazonS3Downloader.isGZipped(download)) {
              reader = new BufferedReader(new InputStreamReader(new StreamingGZIPInputStream(input),
                  StandardCharsets.UTF_8));
            } else {
              reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            }

            boolean broadcastFailure = false;
            String line = null;
            while ((line = reader.readLine()) != null) {
              // skip empty lines
              if (line.isEmpty()) {
                continue;
              }

              eventCount.incrementAndGet();

              final OutboundEvent event = new OutboundEvent.Builder().name("event")
                  .mediaType(MediaType.APPLICATION_JSON_TYPE).data(line).build();

              broadcaster.broadcast(event);
              eventRate.mark();

              // If we have no active connections, assume the broadcast failed
              if (broadcaster.size() < 1) {
                LOGGER.error("No connections found, aborting download");
                broadcastFailure = true;
                break;
              }
            }

            if (broadcastFailure) {
              LOGGER.error("Partial events broadcast ({} sent) from key: {}", eventCount.get(),
                  download.getKey());
              fullyProcessed = false;
            } else {
              LOGGER.info("Successfully broadcast all {} events from key: {}", eventCount.get(),
                  download.getKey());
              fullyProcessed = true;
            }

          } catch (IOException e) {
            LOGGER.error("Error processing key: " + download.getKey(), e);
            fullyProcessed = false;
            break;
          }
        }

        // If all of the S3 event notifications in a given SQS message have been processed, it is
        // safe to delete the message from SQS. Otherwise, we need to wait until the visibility
        // timeout expires and try the message again.
        if (fullyProcessed) {
          sqs.deleteMessage(message);
        }
      }
    }
  }
}
