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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.dropwizard.jackson.Jackson;
import io.dropwizard.testing.FixtureHelpers;

public class AmazonEventRecordTest {
  private static final ObjectMapper MAPPER = Jackson.newObjectMapper();
  private AmazonEventRecord record;

  @Before
  public void setUp() throws Exception {
    final AmazonEventRecordS3Bucket bucket = new AmazonEventRecordS3Bucket("bucket-name");
    final AmazonEventRecordS3Object object = new AmazonEventRecordS3Object("object-key", 100,
        "object eTag", "object version", "event sequence");
    final AmazonEventRecordS3 s3 = new AmazonEventRecordS3(bucket, object);
    record = new AmazonEventRecord("2.0", "aws:s3", "us-east-1", "1970-01-01T00:00:00.000Z",
        "event-type", s3);
  }

  @Test
  public void serializesToJSON() throws Exception {
    final String actual = MAPPER.writeValueAsString(record);
    final String expected = MAPPER.writeValueAsString(MAPPER.readValue(
        FixtureHelpers.fixture("fixtures/amazon_event_record.json"), AmazonEventRecord.class));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void deserializesFromJSON() throws Exception {
    final AmazonEventRecord actual = MAPPER.readValue(
        FixtureHelpers.fixture("fixtures/amazon_event_record.json"), AmazonEventRecord.class);
    assertThat(actual).isEqualTo(record);
  }
}
