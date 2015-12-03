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

import java.util.Iterator;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;

public class AmazonSQSIterator implements Iterator<ReceiveMessageResult> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AmazonSQSIterator.class);
  private static final int MAX_NUMBER_OF_MESSAGES = 10;
  private static final int VISIBILITY_TIMEOUT_SECS = 10;
  private static final int WAIT_TIME_SECS = 20;
  private final AmazonSQSClient client;
  private final String queueUrl;
  private final Counter receiveRequests;
  private final Counter deleteRequests;
  private final Histogram messageCounts;
  private final ReceiveMessageRequest request;

  /**
   * Constructor
   *
   * @param client SQS client
   * @param queueUrl Queue URL
   */
  public AmazonSQSIterator(@Nonnull final AmazonSQSClient client, @Nonnull final String queueUrl,
      @Nonnull final MetricRegistry registry) {
    Preconditions.checkNotNull(registry);
    this.client = Preconditions.checkNotNull(client);
    this.queueUrl = Preconditions.checkNotNull(queueUrl);

    this.receiveRequests = registry.counter(name(AmazonSQSIterator.class, "receive-requests"));
    this.deleteRequests = registry.counter(name(AmazonSQSIterator.class, "delete-requests"));
    this.messageCounts = registry.histogram(name(AmazonSQSIterator.class, "message-counts"));

    this.request =
        new ReceiveMessageRequest(queueUrl).withMaxNumberOfMessages(MAX_NUMBER_OF_MESSAGES)
            .withVisibilityTimeout(VISIBILITY_TIMEOUT_SECS).withWaitTimeSeconds(WAIT_TIME_SECS);

    LOGGER.info("SQS Queue URL: {}", queueUrl);
  }

  @Override
  public boolean hasNext() {
    return true;
  }

  @Override
  public ReceiveMessageResult next() {
    LOGGER.debug("Requesting {} messages from SQS", MAX_NUMBER_OF_MESSAGES);
    receiveRequests.inc();
    final ReceiveMessageResult result = client.receiveMessage(request);
    final int numMessages = result.getMessages().size();
    LOGGER.debug("Received {} messages from SQS", numMessages);
    messageCounts.update(numMessages);
    return result;
  }

  /**
   * Delete a message from the SQS queue
   *
   * @param messageHandle Message handle to delete
   * @return true if the delete was successful, otherwise false
   */
  public boolean deleteMessage(final Message message) {
    try {
      LOGGER.debug("Deleting message from SQS: {}", message.getMessageId());
      deleteRequests.inc();
      client.deleteMessage(queueUrl, message.getReceiptHandle());
      return true;
    } catch (Exception e) {
      LOGGER.error("Failed to delete message", e);
    }
    return false;
  }
}
