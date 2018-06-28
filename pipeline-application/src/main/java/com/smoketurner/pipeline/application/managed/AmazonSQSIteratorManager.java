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

import com.smoketurner.pipeline.application.core.AmazonSQSIterator;
import io.dropwizard.lifecycle.Managed;
import java.util.Objects;
import javax.annotation.Nonnull;

public class AmazonSQSIteratorManager implements Managed {

  private final AmazonSQSIterator iterator;

  /**
   * Constructor
   *
   * @param iterator AmazonSQSIterator to iterator
   */
  public AmazonSQSIteratorManager(@Nonnull final AmazonSQSIterator iterator) {
    this.iterator = Objects.requireNonNull(iterator);
  }

  @Override
  public void start() throws Exception {
    // nothing to start
  }

  @Override
  public void stop() throws Exception {
    iterator.close();
  }
}
