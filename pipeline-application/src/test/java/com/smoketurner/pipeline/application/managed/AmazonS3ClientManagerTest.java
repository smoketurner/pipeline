/*
 * Copyright © 2018 Smoke Turner, LLC (contact@smoketurner.com)
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
package com.smoketurner.pipeline.application.managed;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.amazonaws.services.s3.AmazonS3Client;
import org.junit.Test;

public class AmazonS3ClientManagerTest {

  private final AmazonS3Client client = mock(AmazonS3Client.class);
  private final AmazonS3ClientManager manager = new AmazonS3ClientManager(client);

  @Test
  public void testStop() throws Exception {
    manager.stop();
    verify(client).shutdown();
  }
}
