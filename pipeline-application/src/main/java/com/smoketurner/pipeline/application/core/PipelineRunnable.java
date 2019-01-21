/*
 * Copyright © 2019 Smoke Turner, LLC (github@smoketurner.com)
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

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.OverLimitException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PipelineRunnable implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(PipelineRunnable.class);
  private static final long DEFAULT_SLEEP_SECS = 1;
  private static final long SQS_FAILURE_SLEEP_SECS = 10;

  private final MessageProcessor processor;
  private final AmazonSQSIterator sqs;
  private final InstrumentedSseBroadcaster broadcaster;

  /**
   * Constructor
   *
   * @param processor Message processor
   * @param sqs Amazon SQS iterator
   * @param broadcaster SSE broadcaster
   */
  public PipelineRunnable(
      @Nonnull final MessageProcessor processor,
      @Nonnull final AmazonSQSIterator sqs,
      @Nonnull final InstrumentedSseBroadcaster broadcaster) {

    this.processor = Objects.requireNonNull(processor);
    this.sqs = Objects.requireNonNull(sqs);
    this.broadcaster = Objects.requireNonNull(broadcaster);
  }

  @Override
  public void run() {
    while (sqs.hasNext() && !Thread.currentThread().isInterrupted()) {
      long sleepSeconds = DEFAULT_SLEEP_SECS;

      if (!broadcaster.isEmpty()) {

        try {
          final List<Message> messages = sqs.next();

          // Process each SQS message in parallel. If the message was
          // successfully processed and all of the events in the S3
          // download were successfully broadcast, we can safely
          // delete the message.
          messages.parallelStream().filter(processor::test).forEach(sqs::deleteMessage);

        } catch (OverLimitException e) {
          LOGGER.error("Reached SQS request limit, sleeping for " + sleepSeconds + " seconds", e);
          sleepSeconds = SQS_FAILURE_SLEEP_SECS;
        } catch (Exception e) {
          LOGGER.error(
              "Failed to request messages from SQS, sleeping for " + sleepSeconds + " seconds", e);
          sleepSeconds = SQS_FAILURE_SLEEP_SECS;
        }

      } else {
        LOGGER.trace("No active connections found, sleeping for {} seconds", sleepSeconds);
      }

      try {
        TimeUnit.SECONDS.sleep(sleepSeconds);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
