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
package com.smoketurner.pipeline.application.convert;

import com.google.common.base.Converter;
import com.smoketurner.pipeline.application.aws.AmazonEventRecord;
import com.smoketurner.pipeline.application.aws.AmazonEventRecordS3;
import com.smoketurner.pipeline.application.aws.AmazonEventRecordS3Bucket;
import com.smoketurner.pipeline.application.aws.AmazonEventRecordS3Object;
import com.smoketurner.pipeline.application.core.AmazonS3Object;

public class AmazonS3ObjectConverter
        extends Converter<AmazonEventRecord, AmazonS3Object> {

    @Override
    protected AmazonS3Object doForward(AmazonEventRecord a) {
        final AmazonEventRecordS3 s3 = a.getS3();
        final AmazonEventRecordS3Object object = s3.getObject();
        return new AmazonS3Object(a.getAwsRegion(), s3.getBucket().getName(),
                object.getKey(), object.getSize(), object.getEtag(),
                object.getVersionId());
    }

    @Override
    protected AmazonEventRecord doBackward(AmazonS3Object b) {
        final AmazonEventRecordS3Bucket bucket = new AmazonEventRecordS3Bucket(
                b.getBucketName());
        final AmazonEventRecordS3Object object = new AmazonEventRecordS3Object(
                b.getKey(), b.getSize(), b.getETag().orElse(null),
                b.getVersionId().orElse(null), null);
        final AmazonEventRecordS3 s3 = new AmazonEventRecordS3(bucket, object);
        return new AmazonEventRecord(null, null, null, null, null, s3);
    }
}
