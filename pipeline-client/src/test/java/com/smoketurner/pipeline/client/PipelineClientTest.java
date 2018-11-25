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
package com.smoketurner.pipeline.client;

import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.junit.DropwizardClientRule;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.ClientBuilder;
import org.glassfish.jersey.media.sse.EventListener;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class PipelineClientTest {

  @Path("/events")
  public static class EventResource {
    @GET
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    public EventOutput fetch() {
      final EventOutput eventOutput = new EventOutput();
      new Thread(
              new Runnable() {

                @Override
                public void run() {
                  try {
                    for (int i = 0; i < 10; i++) {
                      final OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
                      eventBuilder.name("message-to-client");
                      eventBuilder.data(String.class, "Hello world " + i + "!");
                      final OutboundEvent event = eventBuilder.build();
                      eventOutput.write(event);
                    }
                  } catch (IOException e) {
                    throw new RuntimeException("Error when writing the event.", e);
                  } finally {
                    try {
                      eventOutput.close();
                    } catch (IOException ioClose) {
                      throw new RuntimeException("Error when closing the event output.", ioClose);
                    }
                  }
                }
              })
          .start();
      return eventOutput;
    }
  }

  @Path("/ping")
  public static class PingResource {
    @GET
    public String ping() {
      return "pong";
    }
  }

  @Path("/version")
  public static class VersionResource {
    @GET
    public String version() {
      return "1.0.0";
    }
  }

  @ClassRule
  public static final DropwizardClientRule resources =
      new DropwizardClientRule(new EventResource(), new PingResource(), new VersionResource());

  private PipelineClient client;

  @Before
  public void setUp() {
    client = new PipelineClient(ClientBuilder.newClient(), resources.baseUri());
  }

  @After
  public void tearDown() throws Exception {
    client.close();
  }

  @Test
  public void testFetch() throws Exception {
    final CountDownLatch latch = new CountDownLatch(10);
    final EventSource source = client.fetch();

    final EventListener listener =
        new EventListener() {
          @Override
          public void onEvent(InboundEvent inboundEvent) {
            latch.countDown();
          }
        };
    source.register(listener, "message-to-client");
    source.open();
    latch.await(10, TimeUnit.SECONDS);
    source.close();
  }

  @Test
  public void testPing() throws Exception {
    assertThat(client.ping()).isTrue();
  }

  @Test
  public void testVersion() throws Exception {
    assertThat(client.version()).isEqualTo("1.0.0");
  }
}
