/*
 * Copyright © 2018 Smoke Turner, LLC (contact@smoketurner.com)
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
package com.smoketurner.pipeline.application.aws;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class AmazonSNSNotification {

  private final String message;
  private final ZonedDateTime timestamp;

  /**
   * Constructor
   *
   * @param message
   * @param timestamp
   */
  @JsonCreator
  public AmazonSNSNotification(
      @JsonProperty("Message") final String message,
      @JsonProperty("Timestamp") final ZonedDateTime timestamp) {
    this.message = message;
    this.timestamp = timestamp;
  }

  @JsonProperty("Message")
  public String getMessage() {
    return message;
  }

  @JsonProperty("Timestamp")
  public ZonedDateTime getTimestamp() {
    return timestamp;
  }

  @Nullable
  @JsonIgnore
  public Duration getDelayDuration() {
    if (timestamp == null) {
      return null;
    }
    return Duration.between(timestamp, ZonedDateTime.now(Clock.systemUTC()));
  }

  @JsonIgnore
  public boolean isValid() {
    return message != null && timestamp != null;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if ((obj == null) || (getClass() != obj.getClass())) {
      return false;
    }

    final AmazonSNSNotification other = (AmazonSNSNotification) obj;
    return Objects.equals(message, other.message) && Objects.equals(timestamp, other.timestamp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(message, timestamp);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("message", message)
        .add("timestamp", timestamp)
        .toString();
  }
}
