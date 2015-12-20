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
package com.smoketurner.pipeline.application.managed;

import java.util.Objects;
import javax.annotation.Nonnull;
import com.amazonaws.services.sqs.AmazonSQSClient;
import io.dropwizard.lifecycle.Managed;

public class AmazonSQSClientManager implements Managed {

    private final AmazonSQSClient client;

    /**
     * Constructor
     *
     * @param client
     *            AmazonS3Client to manage
     */
    public AmazonSQSClientManager(@Nonnull final AmazonSQSClient client) {
        this.client = Objects.requireNonNull(client);
    }

    @Override
    public void start() throws Exception {
        // nothing to start
    }

    @Override
    public void stop() throws Exception {
        client.shutdown();
    }
}
