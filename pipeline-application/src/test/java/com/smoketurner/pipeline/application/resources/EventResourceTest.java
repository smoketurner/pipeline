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
package com.smoketurner.pipeline.application.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.SseFeature;
import org.junit.ClassRule;
import org.junit.Test;
import com.smoketurner.pipeline.application.core.InstrumentedSseBroadcaster;
import io.dropwizard.testing.junit.ResourceTestRule;

public class EventResourceTest {

    private static final InstrumentedSseBroadcaster broadcaster = mock(
            InstrumentedSseBroadcaster.class);

    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addProvider(SseFeature.class)
            .addResource(new EventResource(broadcaster)).build();

    @Test
    public void testGetEventsUnavailable() throws Exception {
        final Response response = resources.client().target("/v1/events")
                .request().get();
        assertThat(response.getStatus()).isEqualTo(503);
    }

    @Test
    public void testGetEvents() throws Exception {
        when(broadcaster.add(any(EventOutput.class))).thenReturn(true);
        final WebTarget target = resources.client().target("/v1/events");
        final EventSource source = EventSource.target(target).build();
        // TODO not sure how to test this
    }
}
