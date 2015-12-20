/**
 * Copyright 2015 Smoke Turner, LLC.
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
package com.smoketurner.pipeline.application.convert;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smoketurner.pipeline.application.aws.AmazonEventRecord;
import com.smoketurner.pipeline.application.core.AmazonS3Object;
import io.dropwizard.testing.FixtureHelpers;

public class AmazonS3ObjectConverterTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final AmazonS3ObjectConverter converter = new AmazonS3ObjectConverter();

    @Test
    public void testConverter() throws Exception {
        final AmazonEventRecord record = MAPPER.readValue(
                FixtureHelpers.fixture("fixtures/amazon_event_record.json"),
                AmazonEventRecord.class);
        final AmazonS3Object actual = converter.convert(record);
        assertThat(actual.getRegion()).isEqualTo("us-east-1");
        assertThat(actual.getBucketName()).isEqualTo("bucket-name");
        assertThat(actual.getKey()).isEqualTo("object-key");
        assertThat(actual.getSize()).isEqualTo(100);
        assertThat(actual.getETag().get()).isEqualTo("object eTag");
    }
}
