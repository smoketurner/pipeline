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
import org.junit.Before;
import org.junit.Test;

public class AmazonS3ObjectTest {

    private AmazonS3Object object;

    @Before
    public void setUp() {
        object = new AmazonS3Object("us-east-1", "bucket-name", "object-key",
                100, "object eTag", "object version");
    }

    @Test
    public void testAccessors() {
        assertThat(object.getRegion()).isEqualTo("us-east-1");
        assertThat(object.getBucketName()).isEqualTo("bucket-name");
        assertThat(object.getKey()).isEqualTo("object-key");
        assertThat(object.getSize()).isEqualTo(100);
        assertThat(object.getETag().orElse(null)).isEqualTo("object eTag");
        assertThat(object.getVersionId().orElse(null))
                .isEqualTo("object version");
    }

    @Test
    public void testEquals() {
        final AmazonS3Object object2 = new AmazonS3Object("us-east-1",
                "bucket-name", "object-key", 100, "object eTag",
                "object version");
        assertThat(object).isEqualTo(object2);
    }

    @Test
    public void testHashCode() {
        assertThat(object.hashCode()).isEqualTo(-1614203949);
    }

    @Test
    public void testToString() {
        final String expected = "AmazonS3Object{region=us-east-1, bucketName=bucket-name, key=object-key,"
                + " size=100, eTag=Optional[object eTag], versionId=Optional[object version]}";
        assertThat(object.toString()).isEqualTo(expected);
    }
}
