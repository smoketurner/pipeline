/**
 * Copyright 2017 Smoke Turner, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smoketurner.pipeline.application.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.Before;
import org.junit.Test;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.event.S3EventNotification.S3BucketEntity;
import com.amazonaws.services.s3.event.S3EventNotification.S3Entity;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.event.S3EventNotification.S3ObjectEntity;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.smoketurner.pipeline.application.exceptions.AmazonS3ConstraintException;
import com.smoketurner.pipeline.application.exceptions.AmazonS3ZeroSizeException;

public class AmazonS3DownloaderTest {

    private final AmazonS3Client mockS3 = mock(AmazonS3Client.class);
    private final AmazonS3Downloader downloader = new AmazonS3Downloader(
            mockS3);
    private S3EventNotificationRecord record;

    @Before
    public void setUp() {
        reset(mockS3);

        final S3BucketEntity bucket = new S3BucketEntity("bucket-name", null,
                null);
        final S3ObjectEntity object = new S3ObjectEntity("object-key", 100L,
                "object eTag", "object version", null);
        final S3Entity s3 = new S3Entity(null, bucket, object, null);
        record = new S3EventNotificationRecord("us-east-1", null, "aws:s3",
                "1970-01-01T00:00:00.000Z", "2.0", null, null, s3, null);
    }

    @Test(expected = NullPointerException.class)
    public void testFetchNull() throws Exception {
        downloader.fetch(null);
    }

    @Test
    public void testFetch() throws Exception {
        final S3Object expected = new S3Object();
        expected.setKey("object-key");
        expected.setBucketName("bucket-name");
        expected.getObjectMetadata().setContentLength(100);

        when(mockS3.getObject(any(GetObjectRequest.class)))
                .thenReturn(expected);

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
            failBecauseExceptionWasNotThrown(AmazonS3ConstraintException.class);
        } catch (AmazonS3ConstraintException e) {
        }
        verify(mockS3).getObject(any(GetObjectRequest.class));
    }

    @Test
    public void testFetchZeroSize() throws Exception {
        final S3Object expected = new S3Object();
        expected.setKey("object-key");
        expected.setBucketName("bucket-name");

        when(mockS3.getObject(any(GetObjectRequest.class)))
                .thenReturn(expected);

        try {
            final S3Object download = downloader.fetch(record);
            assertThat(download).isNotNull();
            failBecauseExceptionWasNotThrown(AmazonS3ZeroSizeException.class);
        } catch (AmazonS3ZeroSizeException e) {
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
