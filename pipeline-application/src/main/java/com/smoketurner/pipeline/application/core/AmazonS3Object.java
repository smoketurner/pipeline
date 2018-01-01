/**
 * Copyright 2018 Smoke Turner, LLC.
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

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import com.google.common.base.MoreObjects;

@Immutable
public final class AmazonS3Object {

    private final String region;
    private final String bucketName;
    private final String key;
    private final long size;
    private final Optional<String> eTag;
    private final Optional<String> versionId;

    /**
     * Constructor
     *
     * @param region
     *            AWS S3 bucket region
     * @param bucketName
     *            AWS S3 bucket name
     * @param key
     *            AWS S3 object key
     * @param size
     *            S3 object size
     * @param eTag
     *            S3 object e-tag
     * @param versionId
     *            S3 object version ID
     */
    public AmazonS3Object(final String region, final String bucketName,
            final String key, final long size, @Nullable final String eTag,
            @Nullable final String versionId) {
        this.region = Objects.requireNonNull(region);
        this.bucketName = Objects.requireNonNull(bucketName);
        this.key = Objects.requireNonNull(key);
        this.size = size;
        this.eTag = Optional.ofNullable(eTag);
        this.versionId = Optional.ofNullable(versionId);
    }

    public String getRegion() {
        return region;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getKey() {
        return key;
    }

    public long getSize() {
        return size;
    }

    public Optional<String> getETag() {
        return eTag;
    }

    public Optional<String> getVersionId() {
        return versionId;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        final AmazonS3Object other = (AmazonS3Object) obj;
        return Objects.equals(region, other.region)
                && Objects.equals(bucketName, other.bucketName)
                && Objects.equals(key, other.key)
                && Objects.equals(size, other.size)
                && Objects.equals(eTag, other.eTag)
                && Objects.equals(versionId, other.versionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(region, bucketName, key, size, eTag, versionId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("region", region)
                .add("bucketName", bucketName).add("key", key).add("size", size)
                .add("eTag", eTag).add("versionId", versionId).toString();
    }
}
