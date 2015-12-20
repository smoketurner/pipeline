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
package com.smoketurner.pipeline.application;

import java.util.concurrent.ExecutorService;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.codahale.metrics.MetricRegistry;
import com.smoketurner.pipeline.application.config.AwsConfiguration;
import com.smoketurner.pipeline.application.config.PipelineConfiguration;
import com.smoketurner.pipeline.application.core.AmazonS3Downloader;
import com.smoketurner.pipeline.application.core.AmazonSQSIterator;
import com.smoketurner.pipeline.application.core.InstrumentedSseBroadcaster;
import com.smoketurner.pipeline.application.core.MessageProcessor;
import com.smoketurner.pipeline.application.core.PipelineRunnable;
import com.smoketurner.pipeline.application.resources.EventResource;
import com.smoketurner.pipeline.application.resources.PingResource;
import com.smoketurner.pipeline.application.resources.VersionResource;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

public class PipelineApplication extends Application<PipelineConfiguration> {

    public static void main(final String[] args) throws Exception {
        new PipelineApplication().run(args);
    }

    @Override
    public String getName() {
        return "pipeline";
    }

    @Override
    public void initialize(Bootstrap<PipelineConfiguration> bootstrap) {
        bootstrap.addBundle(new SwaggerBundle<PipelineConfiguration>() {
            @Override
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(
                    final PipelineConfiguration configuration) {
                return configuration.getSwagger();
            }
        });
    }

    @Override
    public void run(final PipelineConfiguration configuration,
            final Environment environment) throws Exception {

        final MetricRegistry registry = environment.metrics();

        // AWS clients
        final AwsConfiguration awsConfig = configuration.getAws();
        final AmazonS3Client s3 = awsConfig.buildS3(environment);
        final AmazonSQSClient sqs = awsConfig.buildSQS(environment);

        final AmazonSQSIterator sqsIterator = new AmazonSQSIterator(sqs,
                awsConfig.getQueueUrl(), registry);
        final AmazonS3Downloader s3Downloader = new AmazonS3Downloader(s3);

        final InstrumentedSseBroadcaster broadcaster = new InstrumentedSseBroadcaster(
                registry);

        final MessageProcessor processor = new MessageProcessor(registry,
                s3Downloader, broadcaster);

        final ExecutorService service = environment.lifecycle()
                .executorService("sqs-%d").minThreads(1).build();

        final PipelineRunnable runnable = new PipelineRunnable(processor,
                sqsIterator, broadcaster);
        service.execute(runnable);

        // resources
        environment.jersey().register(new EventResource(broadcaster));
        environment.jersey().register(new PingResource());
        environment.jersey().register(new VersionResource());
    }
}
