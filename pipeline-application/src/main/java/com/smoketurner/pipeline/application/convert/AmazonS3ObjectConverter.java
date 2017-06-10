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
package com.smoketurner.pipeline.application.convert;

import java.util.Optional;
import com.amazonaws.services.s3.event.S3EventNotification.S3BucketEntity;
import com.amazonaws.services.s3.event.S3EventNotification.S3Entity;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.event.S3EventNotification.S3ObjectEntity;
import com.google.common.base.Converter;
import com.smoketurner.pipeline.application.core.AmazonS3Object;

public class AmazonS3ObjectConverter
        extends Converter<S3EventNotificationRecord, AmazonS3Object> {

    @Override
    protected AmazonS3Object doForward(S3EventNotificationRecord a) {
        final S3Entity s3 = a.getS3();
        final S3ObjectEntity object = s3.getObject();
        return new AmazonS3Object(a.getAwsRegion(), s3.getBucket().getName(),
                object.getKey(), object.getSizeAsLong(),
                Optional.ofNullable(object.geteTag()),
                Optional.ofNullable(object.getVersionId()));
    }

    @Override
    protected S3EventNotificationRecord doBackward(AmazonS3Object b) {
        final S3BucketEntity bucket = new S3BucketEntity(b.getBucketName(),
                null, null);
        final S3ObjectEntity object = new S3ObjectEntity(b.getKey(),
                b.getSize(), b.getETag().orElse(null),
                b.getVersionId().orElse(null), null);
        final S3Entity s3 = new S3Entity(null, bucket, object, null);
        return new S3EventNotificationRecord(null, null, null, null, null, null,
                null, s3, null);
    }
}
