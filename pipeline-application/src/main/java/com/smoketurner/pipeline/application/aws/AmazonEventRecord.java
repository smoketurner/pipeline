/**
 * Copyright 2016 Smoke Turner, LLC.
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
public final class AmazonEventRecord {

    private final String eventVersion;
    private final String eventSource;
    private final String awsRegion;
    private final String eventTime;
    private final String eventName;
    private final AmazonEventRecordS3 s3;

    @JsonCreator
    public AmazonEventRecord(
            @JsonProperty("eventVersion") final String eventVersion,
            @JsonProperty("eventSource") final String eventSource,
            @JsonProperty("awsRegion") final String awsRegion,
            @JsonProperty("eventTime") final String eventTime,
            @JsonProperty("eventName") final String eventName,
            @JsonProperty("s3") final AmazonEventRecordS3 s3) {
        this.eventVersion = eventVersion;
        this.eventSource = eventSource;
        this.awsRegion = awsRegion;
        this.eventTime = eventTime;
        this.eventName = eventName;
        this.s3 = s3;
    }

    @JsonProperty
    public String getEventVersion() {
        return eventVersion;
    }

    @JsonProperty
    public String getEventSource() {
        return eventSource;
    }

    @JsonProperty
    public String getAwsRegion() {
        return awsRegion;
    }

    @JsonProperty
    public String getEventTime() {
        return eventTime;
    }

    @JsonProperty
    public String getEventName() {
        return eventName;
    }

    @JsonProperty("s3")
    public AmazonEventRecordS3 getS3() {
        return s3;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        final AmazonEventRecord other = (AmazonEventRecord) obj;
        return Objects.equals(eventVersion, other.eventVersion)
                && Objects.equals(eventSource, other.eventSource)
                && Objects.equals(awsRegion, other.awsRegion)
                && Objects.equals(eventTime, other.eventTime)
                && Objects.equals(eventName, other.eventName)
                && Objects.equals(s3, other.s3);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventVersion, eventSource, awsRegion, eventTime,
                eventName, s3);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("eventVersion", eventVersion)
                .add("eventSource", eventSource).add("awsRegion", awsRegion)
                .add("eventTime", eventTime).add("eventName", eventName)
                .add("s3", s3).toString();
    }
}
