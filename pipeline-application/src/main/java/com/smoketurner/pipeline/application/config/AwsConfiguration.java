/**
 * Copyright 2017 Smoke Turner, LLC.
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
package com.smoketurner.pipeline.application.config;

import java.util.Objects;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.valuehandling.UnwrapValidatedValue;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
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

    @NotNull
    private Regions region = Regions.DEFAULT_REGION;

    private String accessKey;

    private String secretKey;

    private String stsRoleArn;

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

    @JsonProperty
    public Regions getRegion() {
        return region;
    }

    @JsonProperty
    public void setRegion(Regions region) {
        this.region = region;
    }

    @JsonProperty
    public String getAccessKey() {
        return accessKey;
    }

    @JsonProperty
    public void setAcccessKey(String key) {
        this.accessKey = key;
    }

    @JsonProperty
    public String getSecretKey() {
        return secretKey;
    }

    @JsonProperty
    public void setSecretKey(String key) {
        this.secretKey = key;
    }

    @JsonProperty
    public String getStsRoleArn() {
        return stsRoleArn;
    }

    @JsonProperty
    public void setStsRoleArn(String arn) {
        this.stsRoleArn = arn;
    }

    @JsonIgnore
    public ClientConfiguration getClientConfiguration() {
        final ClientConfiguration clientConfig = new ClientConfiguration();
        if (proxy.isPresent()) {
            clientConfig.setProxyHost(proxy.get().getHostText());
            clientConfig.setProxyPort(proxy.get().getPort());
        }
        clientConfig.setUseTcpKeepAlive(true);
        // needs to be false to support streaming gunzipping
        clientConfig.setUseGzip(false);
        return clientConfig;
    }

    @JsonIgnore
    public AWSCredentialsProvider getProvider() {
        final AWSCredentialsProvider provider;
        if (!Strings.isNullOrEmpty(accessKey)
                && !Strings.isNullOrEmpty(secretKey)) {
            provider = new AWSStaticCredentialsProvider(
                    new BasicAWSCredentials(accessKey, secretKey));
        } else {
            provider = new DefaultAWSCredentialsProviderChain();
        }

        if (Strings.isNullOrEmpty(stsRoleArn)) {
            return provider;
        }

        final ClientConfiguration clientConfig = getClientConfiguration();
        final AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder
                .standard().withCredentials(provider)
                .withClientConfiguration(clientConfig).withRegion(region)
                .build();

        return new STSAssumeRoleSessionCredentialsProvider.Builder(stsRoleArn,
                "pipeline").withStsClient(stsClient).build();
    }

    @JsonIgnore
    public AmazonS3Client buildS3(final Environment environment) {
        final Region region = Region.getRegion(this.region);
        Objects.requireNonNull(region);

        Preconditions.checkArgument(region.isServiceSupported("s3"),
                "S3 is not supported in " + region);

        final AWSCredentialsProvider provider = getProvider();
        final ClientConfiguration clientConfig = getClientConfiguration();
        final AmazonS3Client s3 = region.createClient(AmazonS3Client.class,
                provider, clientConfig);
        environment.lifecycle().manage(new AmazonS3ClientManager(s3));
        return s3;
    }

    @JsonIgnore
    public AmazonSQSClient buildSQS(final Environment environment) {
        final Region region = Region.getRegion(this.region);
        Objects.requireNonNull(region);

        Preconditions.checkArgument(region.isServiceSupported("sqs"),
                "SQS is not supported in " + region);

        final AWSCredentialsProvider provider = getProvider();
        final ClientConfiguration clientConfig = getClientConfiguration();
        final AmazonSQSClient sqs = region.createClient(AmazonSQSClient.class,
                provider, clientConfig);
        environment.lifecycle().manage(new AmazonSQSClientManager(sqs));
        return sqs;
    }
}
