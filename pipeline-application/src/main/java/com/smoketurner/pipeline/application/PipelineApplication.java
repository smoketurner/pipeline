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
package com.smoketurner.pipeline.application;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.server.Handler;
import org.glassfish.jersey.media.sse.SseFeature;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.smoketurner.pipeline.application.config.AwsConfiguration;
import com.smoketurner.pipeline.application.config.PipelineConfiguration;
import com.smoketurner.pipeline.application.core.AmazonS3Downloader;
import com.smoketurner.pipeline.application.core.AmazonSQSIterator;
import com.smoketurner.pipeline.application.core.InstrumentedSseBroadcaster;
import com.smoketurner.pipeline.application.core.MessageProcessor;
import com.smoketurner.pipeline.application.core.PipelineRunnable;
import com.smoketurner.pipeline.application.managed.AmazonSQSIteratorManager;
import com.smoketurner.pipeline.application.resources.EventResource;
import com.smoketurner.pipeline.application.resources.PingResource;
import com.smoketurner.pipeline.application.resources.VersionResource;
import io.dropwizard.Application;
import io.dropwizard.jetty.BiDiGzipHandler;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

public class PipelineApplication extends Application<PipelineConfiguration> {

    public static void main(final String[] args) throws Exception {
        // http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/java-dg-jvm-ttl.html
        java.security.Security.setProperty("networkaddress.cache.ttl", "60");
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

        // AWS clients
        final AwsConfiguration awsConfig = configuration.getAws();
        final AmazonS3Client s3 = awsConfig.buildS3(environment);
        final AmazonSQSClient sqs = awsConfig.buildSQS(environment);

        final AmazonSQSIterator sqsIterator = new AmazonSQSIterator(sqs,
                awsConfig.getQueueUrl());
        environment.lifecycle()
                .manage(new AmazonSQSIteratorManager(sqsIterator));
        final AmazonS3Downloader s3Downloader = new AmazonS3Downloader(s3);

        // SSE message broadcaster
        final InstrumentedSseBroadcaster broadcaster = new InstrumentedSseBroadcaster();

        final MessageProcessor processor = new MessageProcessor(s3Downloader,
                broadcaster);

        final ExecutorService service = environment.lifecycle()
                .executorService("sqs-%d").minThreads(1).build();

        final PipelineRunnable runnable = new PipelineRunnable(processor,
                sqsIterator, broadcaster);
        service.execute(runnable);

        // send heartbeat pings every second to all connected clients
        final ScheduledExecutorService scheduler = environment.lifecycle()
                .scheduledExecutorService("heartbeat-%d").threads(1).build();
        scheduler.scheduleAtFixedRate(() -> broadcaster.ping(), 0, 1,
                TimeUnit.SECONDS);

        // Disable GZIP content encoding for SSE endpoints
        environment.lifecycle().addServerLifecycleListener(server -> {
            for (Handler handler : server
                    .getChildHandlersByClass(BiDiGzipHandler.class)) {
                ((BiDiGzipHandler) handler)
                        .addExcludedMimeTypes(SseFeature.SERVER_SENT_EVENTS);
            }
        });

        // resources
        environment.jersey().register(new EventResource(broadcaster));
        environment.jersey().register(new PingResource());
        environment.jersey().register(new VersionResource());
    }
}
