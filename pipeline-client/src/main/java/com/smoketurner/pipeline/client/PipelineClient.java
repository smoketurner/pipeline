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
package com.smoketurner.pipeline.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

import javax.annotation.Nonnull;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.SseFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class PipelineClient implements Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(PipelineClient.class);
  private final Client client;
  private final URI destination;

  /**
   * Constructor
   *
   * @param client Jersey Client
   * @param destination API endpoint
   */
  public PipelineClient(@Nonnull final Client client, @Nonnull final URI destination) {
    this.client = Preconditions.checkNotNull(client);
    client.register(SseFeature.class);
    this.destination = Preconditions.checkNotNull(destination);
  }

  /**
   * Return an {@link EventSource} to consume events
   * 
   * @return EventSource
   */
  public EventSource fetch() {
    final URI uri = UriBuilder.fromUri(destination).path("/events").build();
    LOGGER.debug("GET {}", uri);
    final WebTarget target = client.target(uri);
    return EventSource.target(target).build();
  }

  /**
   * Return the ping response
   *
   * @return true if the ping response was successful, otherwise false
   */
  public boolean ping() {
    final URI uri = UriBuilder.fromUri(destination).path("/ping").build();
    LOGGER.debug("GET {}", uri);
    final String response = client.target(uri).request().get(String.class);
    return "pong".equals(response);
  }

  /**
   * Return the service version
   *
   * @return service version
   */
  public String version() {
    final URI uri = UriBuilder.fromUri(destination).path("/version").build();
    LOGGER.debug("GET {}", uri);
    return client.target(uri).request().get(String.class);
  }

  @Override
  public void close() throws IOException {
    client.close();
  }
}
