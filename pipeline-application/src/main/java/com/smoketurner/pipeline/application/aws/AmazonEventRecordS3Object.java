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
import com.google.common.base.Optional;

@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class AmazonEventRecordS3Object {

  private final String key;
  private final Integer size;
  private final String eTag;
  private final String versionId;
  private final String sequencer;

  @JsonCreator
  public AmazonEventRecordS3Object(@JsonProperty("key") final String key,
      @JsonProperty("size") final Integer size, @JsonProperty("eTag") final String eTag,
      @JsonProperty("versionId") final String versionId,
      @JsonProperty("sequencer") final String sequencer) {
    this.key = key;
    this.size = size;
    this.eTag = eTag;
    this.versionId = versionId;
    this.sequencer = sequencer;
  }

  @JsonProperty
  public String getKey() {
    return key;
  }

  @JsonProperty
  public Integer getSize() {
    return size;
  }

  @JsonProperty("eTag")
  public Optional<String> getEtag() {
    return Optional.fromNullable(eTag);
  }

  @JsonProperty
  public Optional<String> getVersionId() {
    return Optional.fromNullable(versionId);
  }

  @JsonProperty
  public String getSequencer() {
    return sequencer;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if ((obj == null) || (getClass() != obj.getClass())) {
      return false;
    }

    final AmazonEventRecordS3Object other = (AmazonEventRecordS3Object) obj;
    return Objects.equals(key, other.key) && Objects.equals(size, other.size)
        && Objects.equals(eTag, other.eTag) && Objects.equals(versionId, other.versionId)
        && Objects.equals(sequencer, other.sequencer);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, size, eTag, versionId, sequencer);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("key", key).add("size", size).add("eTag", eTag)
        .add("versionId", versionId).add("sequencer", sequencer).toString();
  }
}
