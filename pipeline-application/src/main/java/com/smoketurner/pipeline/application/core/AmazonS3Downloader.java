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

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.base.Strings;
import com.smoketurner.pipeline.application.convert.AmazonS3ObjectConverter;
import com.smoketurner.pipeline.application.exceptions.AmazonS3ConstraintException;
import com.smoketurner.pipeline.application.exceptions.AmazonS3ZeroSizeException;
import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmazonS3Downloader {

  private static final Logger LOGGER = LoggerFactory.getLogger(AmazonS3Downloader.class);
  private static final String GZIP_ENCODING = "gzip";
  private static final String GZIP_EXTENSION = ".gz";
  private final AmazonS3ObjectConverter converter = new AmazonS3ObjectConverter();
  private final AmazonS3 s3;

  /**
   * Constructor
   *
   * @param s3 Amazon S3 client
   */
  public AmazonS3Downloader(@Nonnull final AmazonS3 s3) {
    this.s3 = Objects.requireNonNull(s3);
  }

  /**
   * Retrieves a file from S3
   *
   * @param record S3 event notification record to download
   * @return S3 object
   * @throws AmazonS3ConstraintException if the etag constraints weren't met
   * @throws AmazonS3ZeroSizeException if the file size of the object is zero
   */
  public S3Object fetch(@Nonnull final S3EventNotificationRecord record)
      throws AmazonS3ConstraintException, AmazonS3ZeroSizeException {
    final AmazonS3Object object = converter.convert(Objects.requireNonNull(record));

    final GetObjectRequest request = new GetObjectRequest(object.getBucketName(), object.getKey());
    object.getVersionId().ifPresent(request::setVersionId);
    object
        .getETag()
        .ifPresent(etag -> request.setMatchingETagConstraints(Collections.singletonList(etag)));

    LOGGER.debug("Fetching key: {}/{}", object.getBucketName(), object.getKey());

    final S3Object download;
    try {
      download = s3.getObject(request);
    } catch (AmazonServiceException e) {
      LOGGER.error("Service error while fetching object from S3", e);
      throw e;
    } catch (AmazonClientException e) {
      LOGGER.error("Client error while fetching object from S3", e);
      throw e;
    }

    if (download == null) {
      LOGGER.error(
          "eTag from object did not match for key: {}/{}", object.getBucketName(), object.getKey());
      throw new AmazonS3ConstraintException(object.getKey());
    }

    final long contentLength = download.getObjectMetadata().getContentLength();
    if (contentLength < 1) {
      try {
        download.close();
      } catch (IOException e) {
        LOGGER.error(
            String.format(
                "Failed to close S3 stream for key: %s/%s",
                download.getBucketName(), download.getKey()),
            e);
      }

      LOGGER.debug(
          "Object size is zero for key: {}/{}", download.getBucketName(), download.getKey());
      throw new AmazonS3ZeroSizeException(object.getKey());
    }

    LOGGER.debug(
        "Streaming key ({} bytes): {}/{}",
        contentLength,
        download.getBucketName(),
        download.getKey());

    return download;
  }

  /**
   * Determine whether the object is gzipped or not by inspecting the ContentEncoding object
   * property or whether the key ends in .gz
   *
   * @param object S3 object to inspect
   * @return true if the file is gzipped, otherwise false
   */
  public static boolean isGZipped(@Nullable final S3Object object) {
    if (object == null) {
      return false;
    }

    final String encoding = Strings.nullToEmpty(object.getObjectMetadata().getContentEncoding());
    if (GZIP_ENCODING.equalsIgnoreCase(encoding.trim())) {
      return true;
    }

    final String key = Strings.nullToEmpty(object.getKey());
    return key.trim().toLowerCase().endsWith(GZIP_EXTENSION);
  }
}
