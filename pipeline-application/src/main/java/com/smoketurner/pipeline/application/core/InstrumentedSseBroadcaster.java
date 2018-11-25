/*
 * Copyright © 2018 Smoke Turner, LLC (github@smoketurner.com)
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
package com.smoketurner.pipeline.application.core;

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;
import org.glassfish.jersey.server.ChunkedOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstrumentedSseBroadcaster extends SseBroadcaster implements Predicate<String> {

  private static final Logger LOGGER = LoggerFactory.getLogger(InstrumentedSseBroadcaster.class);
  private static final OutboundEvent PING_EVENT =
      new OutboundEvent.Builder().name("ping").data("ping").build();
  private static final OutboundEvent.Builder EVENT_BUILDER =
      new OutboundEvent.Builder().name("event").mediaType(MediaType.APPLICATION_JSON_TYPE);
  private final AtomicInteger connectionCounter = new AtomicInteger(0);
  private final Meter pingRate;
  private final Meter eventRate;

  /** Constructor */
  public InstrumentedSseBroadcaster() {
    super();
    final MetricRegistry registry = SharedMetricRegistries.getOrCreate("default");
    this.pingRate = registry.meter(name(SseBroadcaster.class, "broadcast", "ping-sends"));
    this.eventRate = registry.meter(name(SseBroadcaster.class, "broadcast", "event-sends"));
  }

  @Override
  public <OUT extends ChunkedOutput<OutboundEvent>> boolean add(final OUT chunkedOutput) {
    if (chunkedOutput.isClosed()) {
      return false;
    }

    final boolean result = super.add(chunkedOutput);
    if (result) {
      final int active = connectionCounter.incrementAndGet();
      LOGGER.debug("Opened new connection ({} total)", active);
    }
    return result;
  }

  @Override
  public void onException(
      final ChunkedOutput<OutboundEvent> chunkedOutput, final Exception exception) {
    LOGGER.trace("Connection exception", exception);
  }

  @Override
  public void onClose(final ChunkedOutput<OutboundEvent> chunkedOutput) {
    final int active = connectionCounter.decrementAndGet();
    LOGGER.debug("Closed connection ({} total)", active);
  }

  @Override
  public void broadcast(final OutboundEvent chunk) {
    super.broadcast(chunk);
    eventRate.mark();
    LOGGER.trace("sent event");
  }

  /**
   * Broadcasts an event to all connected consumers.
   *
   * @param event Event to broadcast
   * @return true if there were no connected consumers before or after broadcasting, otherwise false
   */
  @Override
  public boolean test(final String event) {
    if (isEmpty()) {
      LOGGER.trace("No consumers, returning true");
      return true;
    }

    broadcast(EVENT_BUILDER.data(event).build());

    if (isEmpty()) {
      LOGGER.trace("No consumers, returning true");
      return true;
    }
    LOGGER.trace("Successfully broadcast event, returning false");
    return false;
  }

  /** Send a ping event to all connected consumers */
  public void ping() {
    super.broadcast(PING_EVENT);
    pingRate.mark();
    LOGGER.trace("sent ping event");
  }

  /**
   * Do we have any connections?
   *
   * @return true if we have no connections, otherwise false
   */
  public boolean isEmpty() {
    return connectionCounter.get() < 1;
  }
}
