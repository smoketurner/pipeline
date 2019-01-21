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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.sqs.model.Message;
import com.google.common.io.Resources;
import com.smoketurner.pipeline.application.exceptions.AmazonS3ConstraintException;
import com.smoketurner.pipeline.application.exceptions.AmazonS3ZeroSizeException;
import io.dropwizard.testing.FixtureHelpers;
import org.apache.http.client.methods.HttpRequestBase;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.junit.Before;
import org.junit.Test;

public class MessageProcessorTest {

  private final AmazonS3Downloader s3 = mock(AmazonS3Downloader.class);
  private final InstrumentedSseBroadcaster broadcaster = mock(InstrumentedSseBroadcaster.class);
  private final MessageProcessor processor = new MessageProcessor(s3, broadcaster);
  private Message message;

  @Before
  public void setUp() {
    reset(s3);
    reset(broadcaster);
    message = new Message().withBody("body").withMessageId("id").withReceiptHandle("handle");
  }

  @Test
  public void testProcessNullMessage() throws Exception {
    final boolean actual = processor.test(null);

    verify(broadcaster, never()).isEmpty();
    verify(broadcaster, never()).broadcast(any(OutboundEvent.class));
    verify(s3, never()).fetch(any(S3EventNotificationRecord.class));
    assertThat(actual).isFalse();
  }

  @Test
  public void testProcessNoConnections() throws Exception {
    when(broadcaster.isEmpty()).thenReturn(true);
    final boolean actual = processor.test(message);

    verify(broadcaster).isEmpty();
    verify(broadcaster, never()).test(anyString());
    verify(s3, never()).fetch(any(S3EventNotificationRecord.class));
    assertThat(actual).isFalse();
  }

  @Test
  public void testProcessSNSParseFailure() throws Exception {
    when(broadcaster.isEmpty()).thenReturn(false);
    final boolean actual = processor.test(message);

    verify(broadcaster).isEmpty();
    verify(broadcaster, never()).test(anyString());
    verify(s3, never()).fetch(any(S3EventNotificationRecord.class));
    assertThat(actual).isTrue();
  }

  @Test
  public void testProcessS3EventFetchFailure() throws Exception {
    when(broadcaster.isEmpty()).thenReturn(false);
    when(s3.fetch(any(S3EventNotificationRecord.class)))
        .thenThrow(new AmazonServiceException("error"));

    message.setBody(FixtureHelpers.fixture("fixtures/sns_notification.json"));
    final boolean actual = processor.test(message);

    verify(broadcaster, times(2)).isEmpty();
    verify(broadcaster, never()).test(anyString());
    verify(s3).fetch(any(S3EventNotificationRecord.class));
    assertThat(actual).isFalse();
  }

  @Test
  public void testProcessNoConnectionsAfterParse() throws Exception {
    when(broadcaster.isEmpty()).thenReturn(false, true);

    message.setBody(FixtureHelpers.fixture("fixtures/sns_notification.json"));
    final boolean actual = processor.test(message);

    verify(broadcaster, times(2)).isEmpty();
    verify(broadcaster, never()).test(anyString());
    verify(s3, never()).fetch(any(S3EventNotificationRecord.class));
    assertThat(actual).isFalse();
  }

  @Test
  public void testProcessS3ZeroSizeFailure() throws Exception {
    when(broadcaster.isEmpty()).thenReturn(false);
    when(s3.fetch(any(S3EventNotificationRecord.class))).thenThrow(new AmazonS3ZeroSizeException());

    message.setBody(FixtureHelpers.fixture("fixtures/sns_notification.json"));
    final boolean actual = processor.test(message);

    verify(broadcaster, times(2)).isEmpty();
    verify(broadcaster, never()).test(anyString());
    verify(s3).fetch(any(S3EventNotificationRecord.class));
    assertThat(actual).isTrue();
  }

  @Test
  public void testProcessS3ConstraintFailure() throws Exception {
    when(broadcaster.isEmpty()).thenReturn(false);
    when(s3.fetch(any(S3EventNotificationRecord.class)))
        .thenThrow(new AmazonS3ConstraintException());

    message.setBody(FixtureHelpers.fixture("fixtures/sns_notification.json"));
    final boolean actual = processor.test(message);

    verify(broadcaster, times(2)).isEmpty();
    verify(broadcaster, never()).test(anyString());
    verify(s3).fetch(any(S3EventNotificationRecord.class));
    assertThat(actual).isTrue();
  }

  @Test
  public void testProcessSNS() throws Exception {
    final HttpRequestBase request = mock(HttpRequestBase.class);
    final S3ObjectInputStream stream =
        new S3ObjectInputStream(
            Resources.asByteSource(Resources.getResource("fixtures/s3_object.txt.gz")).openStream(),
            request);

    final ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentEncoding("gzip");
    final S3Object object = new S3Object();
    object.setObjectMetadata(metadata);
    object.setObjectContent(stream);

    when(broadcaster.isEmpty()).thenReturn(false);
    when(s3.fetch(any(S3EventNotificationRecord.class))).thenReturn(object);

    message.setBody(FixtureHelpers.fixture("fixtures/sns_notification.json"));
    final boolean actual = processor.test(message);

    verify(broadcaster, times(2)).isEmpty();
    verify(broadcaster, times(10)).test(anyString());
    verify(s3).fetch(any(S3EventNotificationRecord.class));
    verify(request, never()).abort();
    assertThat(actual).isTrue();
  }

  @Test
  public void testProcessSQS() throws Exception {
    final HttpRequestBase request = mock(HttpRequestBase.class);
    final S3ObjectInputStream stream =
        new S3ObjectInputStream(
            Resources.asByteSource(Resources.getResource("fixtures/s3_object.txt.gz")).openStream(),
            request);

    final ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentEncoding("gzip");
    final S3Object object = new S3Object();
    object.setObjectMetadata(metadata);
    object.setObjectContent(stream);

    when(broadcaster.isEmpty()).thenReturn(false);
    when(s3.fetch(any(S3EventNotificationRecord.class))).thenReturn(object);

    message.setBody(FixtureHelpers.fixture("fixtures/sqs_records.json"));
    final boolean actual = processor.test(message);

    verify(broadcaster, times(2)).isEmpty();
    verify(broadcaster, times(10)).test(anyString());
    verify(s3).fetch(any(S3EventNotificationRecord.class));
    verify(request, never()).abort();
    assertThat(actual).isTrue();
  }

  @Test
  public void testProcessNoConnectionsDuringDownload() throws Exception {
    final HttpRequestBase request = mock(HttpRequestBase.class);
    final S3ObjectInputStream stream =
        new S3ObjectInputStream(
            Resources.asByteSource(Resources.getResource("fixtures/s3_object.txt.gz")).openStream(),
            request);

    final ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentEncoding("gzip");
    final S3Object object = new S3Object();
    object.setObjectMetadata(metadata);
    object.setObjectContent(stream);

    when(broadcaster.test(anyString())).thenReturn(false, false, false, false, true);
    when(s3.fetch(any(S3EventNotificationRecord.class))).thenReturn(object);

    message.setBody(FixtureHelpers.fixture("fixtures/sns_notification.json"));
    final boolean actual = processor.test(message);

    verify(broadcaster, times(2)).isEmpty();
    verify(broadcaster, times(5)).test(anyString());
    verify(s3).fetch(any(S3EventNotificationRecord.class));
    verify(request).abort();
    assertThat(actual).isFalse();
  }
}
