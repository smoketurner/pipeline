/*
 * Copyright © 2019 Smoke Turner, LLC (github@smoketurner.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smoketurner.pipeline.application;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.smoketurner.pipeline.application.config.PipelineConfiguration;
import com.smoketurner.pipeline.application.resources.PingResource;
import com.smoketurner.pipeline.application.resources.VersionResource;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.setup.Environment;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class PipelineApplicationTest {
  private final MetricRegistry registry = new MetricRegistry();
  private final Environment environment = mock(Environment.class);
  private final JerseyEnvironment jersey = mock(JerseyEnvironment.class);
  private final LifecycleEnvironment lifecycle = mock(LifecycleEnvironment.class);
  private final HealthCheckRegistry healthChecks = mock(HealthCheckRegistry.class);
  private final PipelineApplication application = new PipelineApplication();
  private final PipelineConfiguration config = new PipelineConfiguration();

  @Before
  public void setup() throws Exception {
    config.getAws().setQueueUrl("https://sqs.us-east-1.amazonaws.com/1234/test-queue");
    when(environment.metrics()).thenReturn(registry);
    when(environment.jersey()).thenReturn(jersey);
    when(environment.lifecycle()).thenReturn(lifecycle);
    when(environment.healthChecks()).thenReturn(healthChecks);
  }

  @Test
  @Ignore
  public void buildsAVersionResource() throws Exception {
    application.run(config, environment);
    verify(jersey).register(isA(VersionResource.class));
  }

  @Test
  @Ignore
  public void buildsAPingResource() throws Exception {
    application.run(config, environment);
    verify(jersey).register(isA(PingResource.class));
  }
}
