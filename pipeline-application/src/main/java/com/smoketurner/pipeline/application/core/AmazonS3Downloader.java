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

import java.io.IOException;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.base.Preconditions;
import com.smoketurner.pipeline.application.aws.AmazonEventRecord;
import com.smoketurner.pipeline.application.convert.AmazonS3ObjectConverter;

public class AmazonS3Downloader {

  private static final Logger LOGGER = LoggerFactory.getLogger(AmazonS3Downloader.class);
  private static final String GZIP_ENCODING = "gzip";
  private static final String GZIP_EXTENSION = ".gz";
  private final AmazonS3ObjectConverter converter = new AmazonS3ObjectConverter();
  private final AmazonS3Client s3;

  /**
   * Constructor
   *
   * @param s3 Amazon S3 client
   */
  public AmazonS3Downloader(@Nonnull final AmazonS3Client s3) {
    this.s3 = Preconditions.checkNotNull(s3);
  }

  public S3Object fetch(final AmazonEventRecord record) throws Exception {
    final AmazonS3Object object = converter.convert(record);
    if (object == null) {
      throw new IOException("Failed to convert event record");
    }

    LOGGER.debug("S3 Object: {}", object);

    final GetObjectRequest request = new GetObjectRequest(object.getBucketName(), object.getKey());
    if (object.getVersionId().isPresent()) {
      request.setVersionId(object.getVersionId().get());
    }

    final S3Object download;
    try {
      download = s3.getObject(request);
    } catch (Exception e) {
      LOGGER.error("Failed to download object from S3", e);
      throw e;
    }

    if (download == null) {
      LOGGER.error("Constraints not met for {}/{}", object.getBucketName(), object.getKey());
      throw new IOException("Constraints not met for download");
    }

    if (download.getObjectMetadata().getContentLength() < 1) {
      LOGGER.debug("Object size is zero, skipping download");
      throw new IOException("Object size is zero");
    }

    return download;
  }

  public static boolean isGZipped(final S3Object object) {
    final ObjectMetadata metadata = object.getObjectMetadata();
    final String encoding = metadata.getContentEncoding();
    if (GZIP_ENCODING.equalsIgnoreCase(encoding)) {
      return true;
    } else if (object.getKey().endsWith(GZIP_EXTENSION)) {
      return true;
    }
    return false;
  }
}
