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
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.sse.OutboundEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import com.google.common.io.LineProcessor;
import com.smoketurner.pipeline.application.aws.AmazonEventRecord;
import com.smoketurner.pipeline.application.aws.AmazonEventRecords;
import com.smoketurner.pipeline.application.aws.AmazonSNSNotification;

public class PipelineRunnable implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(PipelineRunnable.class);
  private final AmazonS3Downloader s3;
  private final AmazonSQSIterator sqs;
  private final ObjectMapper mapper;
  private final SseBroadcasterWithCount broadcaster;
  private final Meter messagesMeter;
  private final Meter recordsMeter;

  /**
   * Constructor
   *
   * @param s3 Amazon S3 client
   * @param sqs Amazon SQS client
   * @param registry Metric Registry
   * @param mapper Object Mapper
   * @param broadcaster SSE broadcaster
   */
  public PipelineRunnable(@Nonnull final AmazonS3Downloader s3,
      @Nonnull final AmazonSQSIterator sqs, @Nonnull final MetricRegistry registry,
      @Nonnull final ObjectMapper mapper, @Nonnull final SseBroadcasterWithCount broadcaster) {
    Preconditions.checkNotNull(registry);
    this.s3 = Preconditions.checkNotNull(s3);
    this.sqs = Preconditions.checkNotNull(sqs);

    this.mapper = Preconditions.checkNotNull(mapper);
    this.broadcaster = Preconditions.checkNotNull(broadcaster);

    this.messagesMeter = registry.meter(name(PipelineRunnable.class, "sqs-messages"));
    this.recordsMeter = registry.meter(name(PipelineRunnable.class, "s3-records"));
  }

  @Override
  public void run() {
    while (!Thread.currentThread().isInterrupted()) {

      // send heartbeat ping event to all connections
      final OutboundEvent.Builder builder = new OutboundEvent.Builder();
      builder.name("ping");
      builder.data("ping");
      broadcaster.broadcast(builder.build());

      if (broadcaster.size() < 1) {
        try {
          LOGGER.info("No active connections found, sleeping for 1 second");
          TimeUnit.SECONDS.sleep(1);
          continue;
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }

      LOGGER.debug("Requesting messages");

      final ReceiveMessageResult result = sqs.next();
      this.messagesMeter.mark(result.getMessages().size());

      if (broadcaster.size() < 1) {
        LOGGER.debug("No connections found, not processing SQS messages");
        continue;
      }

      for (Message message : result.getMessages()) {
        LOGGER.debug("Received message: {}", message);

        final AmazonSNSNotification notification;
        try {
          notification = mapper.readValue(message.getBody(), AmazonSNSNotification.class);
        } catch (IOException e) {
          LOGGER.error("Failed to parse SNS notification", e);
          continue;
        }

        LOGGER.debug("Parsed notification: {}", notification);

        final AmazonEventRecords records;
        try {
          records = mapper.readValue(notification.getMessage(), AmazonEventRecords.class);
        } catch (IOException e) {
          LOGGER.error("Failed to parse S3 event records", e);
          continue;
        }

        this.recordsMeter.mark(records.getRecords().size());

        boolean fullyProcessed = true;

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

          boolean broadcastFailure = false;

          try (S3ObjectInputStream input = download.getObjectContent()) {
            final Reader reader;

            if (AmazonS3Downloader.isGZipped(download)) {
              reader = new BufferedReader(new InputStreamReader(new StreamingGZIPInputStream(input),
                  StandardCharsets.UTF_8));
            } else {
              reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            }

            broadcastFailure = CharStreams.readLines(reader, new LineProcessor<Boolean>() {
              private boolean isFailure = false;

              @Override
              public boolean processLine(final String line) throws IOException {
                if (Strings.isNullOrEmpty(line)) {
                  // skip empty lines
                  return true;
                }

                final OutboundEvent.Builder builder = new OutboundEvent.Builder();
                builder.name("event");
                builder.mediaType(MediaType.APPLICATION_JSON_TYPE);
                builder.data(line);

                broadcaster.broadcast(builder.build());

                // If we have no active connections, assume the broadcast failed
                if (broadcaster.size() < 1) {
                  LOGGER.error("No connections found, aborting download");
                  isFailure = true;
                  return false;
                }
                return true;
              }

              @Override
              public Boolean getResult() {
                return isFailure;
              }
            });

          } catch (IOException e) {
            LOGGER.error("Failed to download object from S3", e);
            fullyProcessed = false;
            break;
          }

          if (broadcastFailure) {
            LOGGER.error("Failed to broadcast all events");
            fullyProcessed = false;
            break;
          }
        }

        if (fullyProcessed) {
          sqs.deleteMessage(message.getReceiptHandle());
        }
      }
    }
  }
}
