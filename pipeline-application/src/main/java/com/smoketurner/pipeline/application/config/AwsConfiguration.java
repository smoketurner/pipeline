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
package com.smoketurner.pipeline.application.config;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.valuehandling.UnwrapValidatedValue;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.net.HostAndPort;
import com.smoketurner.pipeline.application.managed.AmazonS3ClientManager;
import com.smoketurner.pipeline.application.managed.AmazonSQSClientManager;
import io.dropwizard.setup.Environment;

public class AwsConfiguration {

  @NotEmpty
  private String queueUrl;

  @NotNull
  @Valid
  @UnwrapValidatedValue(false)
  private Optional<HostAndPort> proxy = Optional.absent();

  @JsonProperty
  public String getQueueUrl() {
    return queueUrl;
  }

  @JsonProperty
  public void setQueueUrl(final String queueUrl) {
    this.queueUrl = queueUrl;
  }

  @JsonProperty
  public Optional<HostAndPort> getProxy() {
    return proxy;
  }

  @JsonProperty
  public void setProxy(final Optional<HostAndPort> proxy) {
    this.proxy = proxy;
  }

  @JsonIgnore
  public ClientConfiguration getClientConfiguration() {
    final ClientConfiguration clientConfig = new ClientConfiguration();
    if (proxy.isPresent()) {
      clientConfig.setProxyHost(proxy.get().getHostText());
      clientConfig.setProxyPort(proxy.get().getPort());
    }
    clientConfig.setUseTcpKeepAlive(true);
    clientConfig.setUseGzip(false);
    return clientConfig;
  }

  public AmazonS3Client buildS3(final Environment environment) {
    final ClientConfiguration clientConfig = getClientConfiguration();
    final AmazonS3Client s3 = new AmazonS3Client(clientConfig);
    environment.lifecycle().manage(new AmazonS3ClientManager(s3));
    return s3;
  }

  public AmazonSQSClient buildSQS(final Environment environment) {
    final ClientConfiguration clientConfig = getClientConfiguration();
    final AmazonSQSClient sqs = new AmazonSQSClient(clientConfig);
    environment.lifecycle().manage(new AmazonSQSClientManager(sqs));
    return sqs;
  }
}
