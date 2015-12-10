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
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.smoketurner.pipeline.application.aws.AmazonEventRecord;
import com.smoketurner.pipeline.application.aws.AmazonEventRecordS3;
import com.smoketurner.pipeline.application.aws.AmazonEventRecordS3Bucket;
import com.smoketurner.pipeline.application.aws.AmazonEventRecordS3Object;

public class AmazonS3DownloaderTest {

  private final AmazonS3Client mockS3 = mock(AmazonS3Client.class);
  private final AmazonS3Downloader downloader = new AmazonS3Downloader(mockS3);
  private AmazonEventRecord record;

  @Before
  public void setUp() {
    reset(mockS3);

    final AmazonEventRecordS3Bucket bucket = new AmazonEventRecordS3Bucket("bucket-name");
    final AmazonEventRecordS3Object object = new AmazonEventRecordS3Object("object-key", 100,
        "object eTag", "object version", "event sequence");
    final AmazonEventRecordS3 s3 = new AmazonEventRecordS3(bucket, object);
    record = new AmazonEventRecord("2.0", "aws:s3", "us-east-1", "1970-01-01T00:00:00.000Z",
        "event-type", s3);
  }

  @Test
  public void testFetchNull() throws Exception {
    try {
      downloader.fetch(null);
      failBecauseExceptionWasNotThrown(Exception.class);
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("Failed to convert event record");
    }
  }

  @Test
  public void testFetch() throws Exception {
    final S3Object expected = new S3Object();
    expected.setKey("object-key");
    expected.setBucketName("bucket-name");
    expected.getObjectMetadata().setContentLength(100);

    when(mockS3.getObject(any(GetObjectRequest.class))).thenReturn(expected);

    final S3Object actual = downloader.fetch(record);
    verify(mockS3).getObject(any(GetObjectRequest.class));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testFetchInvalidETag() throws Exception {
    when(mockS3.getObject(any(GetObjectRequest.class))).thenReturn(null);

    try {
      final S3Object download = downloader.fetch(record);
      assertThat(download).isNull();
      failBecauseExceptionWasNotThrown(Exception.class);
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("eTag from object did not match");
    }
    verify(mockS3).getObject(any(GetObjectRequest.class));
  }

  @Test
  public void testFetchZeroSize() throws Exception {
    final S3Object expected = new S3Object();
    expected.setKey("object-key");
    expected.setBucketName("bucket-name");

    when(mockS3.getObject(any(GetObjectRequest.class))).thenReturn(expected);

    try {
      final S3Object download = downloader.fetch(record);
      assertThat(download).isNotNull();
      failBecauseExceptionWasNotThrown(Exception.class);
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("Object size is zero");
    }
    verify(mockS3).getObject(any(GetObjectRequest.class));
  }

  @Test
  public void testFetchServiceException() throws Exception {
    when(mockS3.getObject(any(GetObjectRequest.class)))
        .thenThrow(new AmazonServiceException("error"));

    try {
      downloader.fetch(record);
      failBecauseExceptionWasNotThrown(AmazonServiceException.class);
    } catch (AmazonServiceException e) {
    }
    verify(mockS3).getObject(any(GetObjectRequest.class));
  }

  @Test
  public void testFetchClientException() throws Exception {
    when(mockS3.getObject(any(GetObjectRequest.class)))
        .thenThrow(new AmazonClientException("error"));

    try {
      downloader.fetch(record);
      failBecauseExceptionWasNotThrown(AmazonClientException.class);
    } catch (AmazonClientException e) {
    }
    verify(mockS3).getObject(any(GetObjectRequest.class));
  }

  @Test
  public void testIsGzipped() {
    assertThat(AmazonS3Downloader.isGZipped(null)).isFalse();

    final S3Object object = new S3Object();
    assertThat(AmazonS3Downloader.isGZipped(object)).isFalse();

    final ObjectMetadata metadata = object.getObjectMetadata();
    metadata.setContentEncoding("gzip");
    assertThat(AmazonS3Downloader.isGZipped(object)).isTrue();

    metadata.setContentEncoding("GZIP");
    assertThat(AmazonS3Downloader.isGZipped(object)).isTrue();

    metadata.setContentEncoding(" GzIP ");
    assertThat(AmazonS3Downloader.isGZipped(object)).isTrue();

    metadata.setContentEncoding(null);

    object.setKey("test");
    assertThat(AmazonS3Downloader.isGZipped(object)).isFalse();

    object.setKey("test.gz");
    assertThat(AmazonS3Downloader.isGZipped(object)).isTrue();

    object.setKey("test.gz   ");
    assertThat(AmazonS3Downloader.isGZipped(object)).isTrue();

    object.setKey("test.GZ ");
    assertThat(AmazonS3Downloader.isGZipped(object)).isTrue();
  }

}
