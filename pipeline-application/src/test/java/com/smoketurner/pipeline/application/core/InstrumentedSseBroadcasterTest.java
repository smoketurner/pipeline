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
package com.smoketurner.pipeline.application.core;

import static org.assertj.core.api.Assertions.assertThat;
import org.glassfish.jersey.media.sse.EventOutput;
import org.junit.Test;
import com.codahale.metrics.MetricRegistry;

public class InstrumentedSseBroadcasterTest {

    private final MetricRegistry registry = new MetricRegistry();

    @Test
    public void testAdd() {
        final InstrumentedSseBroadcaster broadcaster = new InstrumentedSseBroadcaster(
                registry);
        assertThat(broadcaster.isEmpty()).isTrue();
        final EventOutput output = new EventOutput();
        broadcaster.add(output);
        assertThat(broadcaster.isEmpty()).isFalse();
    }
}
