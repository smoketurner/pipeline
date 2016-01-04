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

import java.util.Collection;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class AmazonEventRecords {

    private final ImmutableList<AmazonEventRecord> records;

    @JsonCreator
    public AmazonEventRecords(
            @JsonProperty("Records") final Collection<AmazonEventRecord> records) {
        if (records == null) {
            this.records = ImmutableList.of();
        } else {
            this.records = ImmutableList.copyOf(records);
        }
    }

    @JsonProperty("Records")
    public ImmutableList<AmazonEventRecord> getRecords() {
        return records;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        final AmazonEventRecords other = (AmazonEventRecords) obj;
        return Objects.equals(records, other.records);
    }

    @Override
    public int hashCode() {
        return Objects.hash(records);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("records", records)
                .toString();
    }
}
