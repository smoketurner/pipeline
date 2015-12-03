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

import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.smoketurner.pipeline.application.aws.AmazonEventRecord;
import com.smoketurner.pipeline.application.convert.AmazonS3ObjectConverter;

public class S3Downloader {

  private static final Logger LOGGER = LoggerFactory.getLogger(S3Downloader.class);
  private static final String GZIP_ENCODING = "gzip";
  private static final String GZIP_EXTENSION = ".gz";
  private final AmazonS3ObjectConverter converter = new AmazonS3ObjectConverter();
  private final AmazonS3Client s3;

  /**
   * Constructor
   *
   * @param s3 Amazon S3 client
   */
  public S3Downloader(@Nonnull final AmazonS3Client s3) {
    this.s3 = Preconditions.checkNotNull(s3);
  }

  /**
   * Retrieves a file from S3
   *
   * @param record S3 event notification record to download
   * @return S3 object
   * @throws Exception if unable to download the object
   */
  public S3Object fetch(final AmazonEventRecord record) throws Exception {
    final AmazonS3Object object = converter.convert(record);
    if (object == null) {
      throw new Exception("Failed to convert event record");
    }

    LOGGER.debug("Retrieving key: {}", object.getKey());

    final GetObjectRequest request = new GetObjectRequest(object.getBucketName(), object.getKey());
    if (object.getVersionId().isPresent()) {
      request.setVersionId(object.getVersionId().get());
    }
    if (object.getETag().isPresent()) {
      request.setMatchingETagConstraints(ImmutableList.of(object.getETag().get()));
    }

    final S3Object download;
    try {
      download = s3.getObject(request);
    } catch (AmazonServiceException e) {
      LOGGER.error("Failed to download object from S3", e);
      throw e;
    } catch (AmazonClientException e) {
      LOGGER.error("Failed to download object from S3", e);
      throw e;
    }

    if (download == null) {
      LOGGER.error("eTag from object did not match for key: {}", object.getKey());
      throw new Exception("eTag from object did not match");
    }

    final long contentLength = download.getObjectMetadata().getContentLength();
    if (contentLength < 1) {
      LOGGER.debug("Object size is zero");
      throw new Exception("Object size is zero");
    }

    LOGGER.debug("Streaming key ({} bytes): {}", contentLength, download.getKey());

    return download;
  }

  /**
   * Determine whether the object is gzipped or not by inspecting the ContentEncoding object
   * property or whether the key ends in .gz
   * 
   * @param object S3 object to inspect
   * @return true if the file is gzipped, otherwise false
   */
  public static boolean isGZipped(final S3Object object) {
    final String encoding = object.getObjectMetadata().getContentEncoding();
    if (GZIP_ENCODING.equalsIgnoreCase(encoding)) {
      return true;
    } else if (object.getKey().endsWith(GZIP_EXTENSION)) {
      return true;
    }
    return false;
  }
}
