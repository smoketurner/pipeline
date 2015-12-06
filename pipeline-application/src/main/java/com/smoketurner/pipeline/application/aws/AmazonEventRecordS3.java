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
package com.smoketurner.pipeline.application.aws;

import java.util.Objects;

import javax.annotation.concurrent.Immutable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class AmazonEventRecordS3 {

  private final AmazonEventRecordS3Bucket bucket;
  private final AmazonEventRecordS3Object object;

  @JsonCreator
  public AmazonEventRecordS3(@JsonProperty("bucket") final AmazonEventRecordS3Bucket bucket,
      @JsonProperty("object") final AmazonEventRecordS3Object object) {
    this.bucket = bucket;
    this.object = object;
  }

  @JsonProperty
  public AmazonEventRecordS3Bucket getBucket() {
    return bucket;
  }

  @JsonProperty
  public AmazonEventRecordS3Object getObject() {
    return object;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if ((obj == null) || (getClass() != obj.getClass())) {
      return false;
    }

    final AmazonEventRecordS3 other = (AmazonEventRecordS3) obj;
    return Objects.equals(bucket, other.bucket) && Objects.equals(object, other.object);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bucket, object);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("bucket", bucket).add("object", object).toString();
  }
}
