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
package com.smoketurner.pipeline.application.core;

import java.util.concurrent.atomic.AtomicInteger;

import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;
import org.glassfish.jersey.server.ChunkedOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SseBroadcasterWithCount extends SseBroadcaster {

  private static final Logger LOGGER = LoggerFactory.getLogger(SseBroadcasterWithCount.class);
  private final AtomicInteger connectionCounter = new AtomicInteger(0);

  public boolean add(final EventOutput chunkedOutput) {
    if (chunkedOutput.isClosed()) {
      return false;
    }

    final boolean result = super.add(chunkedOutput);
    if (result) {
      final int active = connectionCounter.incrementAndGet();
      LOGGER.debug("Opened new connection (total {})", active);
    }
    return result;
  }

  @Override
  public void onClose(final ChunkedOutput<OutboundEvent> chunkedOutput) {
    final int active = connectionCounter.decrementAndGet();
    LOGGER.debug("Closed connection (total {})", active);
  }

  public int size() {
    return connectionCounter.get();
  }
}
