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
package com.smoketurner.pipeline.application.resources;

import javax.annotation.Nonnull;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.ServiceUnavailableException;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.SseFeature;
import com.google.common.base.Preconditions;
import com.smoketurner.pipeline.application.core.SseBroadcasterWithCount;
import io.dropwizard.util.Duration;
import io.swagger.annotations.Api;

@Singleton
@Path("/v1/events")
@Api(value = "events")
public class EventResource {

  private static final Duration RETRY_AFTER = Duration.seconds(5);
  private final SseBroadcasterWithCount broadcaster;

  /**
   * Constructor
   *
   * @param broadcaster SSE broadcaster
   */
  public EventResource(@Nonnull final SseBroadcasterWithCount broadcaster) {
    this.broadcaster = Preconditions.checkNotNull(broadcaster);
  }

  @GET
  @Produces(SseFeature.SERVER_SENT_EVENTS)
  public EventOutput fetch() {
    final EventOutput output = new EventOutput();
    if (!broadcaster.add(output)) {
      throw new ServiceUnavailableException(RETRY_AFTER.toSeconds());
    }
    return output;
  }
}
