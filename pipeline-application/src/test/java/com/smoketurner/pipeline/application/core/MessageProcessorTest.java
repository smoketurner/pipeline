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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sqs.model.Message;
import com.codahale.metrics.MetricRegistry;
import com.smoketurner.pipeline.application.aws.AmazonEventRecord;

import io.dropwizard.testing.FixtureHelpers;

public class MessageProcessorTest {

  private final AmazonS3Downloader s3 = mock(AmazonS3Downloader.class);
  private final InstrumentedSseBroadcaster broadcaster = mock(InstrumentedSseBroadcaster.class);
  private final MessageProcessor processor =
      new MessageProcessor(new MetricRegistry(), s3, broadcaster);
  private Message message;

  @Before
  public void setUp() {
    reset(s3);
    reset(broadcaster);
    message = new Message().withBody("body").withMessageId("id").withReceiptHandle("handle");
  }

  @Test
  public void testProcessNullMessage() throws Exception {
    final boolean actual = processor.process(null);

    verify(broadcaster, never()).isEmpty();
    verify(s3, never()).fetch(any(AmazonEventRecord.class));
    assertThat(actual).isFalse();
  }

  @Test
  public void testProcessNoConnections() throws Exception {
    when(broadcaster.isEmpty()).thenReturn(true);
    final boolean actual = processor.process(message);

    verify(broadcaster).isEmpty();
    verify(s3, never()).fetch(any(AmazonEventRecord.class));
    assertThat(actual).isFalse();
  }

  @Test
  public void testProcessSNSParseFailure() throws Exception {
    when(broadcaster.isEmpty()).thenReturn(false);
    final boolean actual = processor.process(message);

    verify(broadcaster).isEmpty();
    verify(s3, never()).fetch(any(AmazonEventRecord.class));
    assertThat(actual).isTrue();
  }

  @Test
  public void testProcessS3EventParseFailure() throws Exception {
    when(broadcaster.isEmpty()).thenReturn(false);
    when(s3.fetch(any(AmazonEventRecord.class))).thenThrow(new AmazonServiceException("error"));
    message.setBody(FixtureHelpers.fixture("fixtures/sns_notification.json"));
    final boolean actual = processor.process(message);

    verify(broadcaster, times(2)).isEmpty();
    verify(s3).fetch(any(AmazonEventRecord.class));
    assertThat(actual).isFalse();
  }

  @Test
  public void testProcessNoConnectionsAfterParse() throws Exception {
    when(broadcaster.isEmpty()).thenReturn(false, true);
    when(s3.fetch(any(AmazonEventRecord.class))).thenThrow(new AmazonServiceException("error"));
    message.setBody(FixtureHelpers.fixture("fixtures/sns_notification.json"));
    final boolean actual = processor.process(message);

    verify(broadcaster, times(2)).isEmpty();
    verify(s3, never()).fetch(any(AmazonEventRecord.class));
    assertThat(actual).isFalse();
  }
}
