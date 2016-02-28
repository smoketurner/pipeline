Pipeline
========
[![Build Status](https://travis-ci.org/smoketurner/pipeline.svg?branch=master)](https://travis-ci.org/smoketurner/pipeline)
[![Coverage Status](https://coveralls.io/repos/smoketurner/pipeline/badge.svg?branch=master&service=github)](https://coveralls.io/github/smoketurner/pipeline?branch=master)
[![Maven Central](https://img.shields.io/maven-central/v/com.smoketurner.pipeline/pipeline-parent.svg?style=flat-square)](https://maven-badges.herokuapp.com/maven-central/com.smoketurner.pipeline/pipeline-parent/)
[![GitHub license](https://img.shields.io/github/license/smoketurner/pipeline.svg?style=flat-square)](https://github.com/smoketurner/pipeline/tree/master)

Pipeline is an implementation of an event pipeline using [Amazon Web Services](https://aws.amazon.com) (AWS) [Simple Storage Service](http://aws.amazon.com/s3/) (S3) and [Simple Queue Service](http://aws.amazon.com/sqs/) (SQS). S3 can be configured to publish a notification to the [Simple Notification Service](http://aws.amazon.com/sns/) (SNS) whenever a new object is created in a bucket. SNS can then publish the notification into an SQS queue which Pipeline will consume from. Pipeline will consume from the SQS queue, then download the object from S3, optionally decompress it, then emit each line of data over a [server-sent events](https://en.wikipedia.org/wiki/Server-sent_events) (SSE) HTTP endpoint.

Installation
------------
To build this code locally, clone the repository then use [Maven](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html) to build the jar:
```
git clone https://github.com/smoketurner/pipeline.git
cd pipeline
mvn package
cd pipeline-application
java -jar target/pipeline-application/pipeline-application-1.0.0-SNAPSHOT.jar server pipeline.yml
```

The Pipeline service should be listening on port `8080` for API requests, and Dropwizard's administrative interface is available on port `8180` (both of these ports can be changed in the `pipeline.yml` configuration file).

Production
----------
To deploy the Pipeline service into production, it can safely sit behind any HTTP-based load balancer (nginx, haproxy, F5, etc.).

*NOTE*: The pipeline service provides no authentication or authorization of requests. It is recommended to use a separate service such as [Kong](http://www.getkong.org) or the [Amazon API Gateway](https://aws.amazon.com/api-gateway/) to authenticate and authorize users.

Usage
-----
The Pipeline service provides RESTful URLs for consuming events.

API documentation is also available via [Swagger](http://swagger.io) at `http://localhost:8080/swagger`.

### Retrieving events

```
curl -X GET localhost:8080/v1/events -i
HTTP/1.1 200 OK
Date: Thu, 03 Dec 2015 20:22:25 GMT
Content-Type: text/event-stream
Transfer-Encoding: chunked

event: ping
data: ping
```

As messages are published into the SQS queue as new files are uploaded to S3, Pipeline will consume the SQS messages, download the S3 files, and publish the events over the HTTP connection.

Support
-------

Please file bug reports and feature requests in [GitHub issues](https://github.com/smoketurner/pipeline/issues).


License
-------

Copyright (c) 2016 Justin Plock

This library is licensed under the Apache License, Version 2.0.

See http://www.apache.org/licenses/LICENSE-2.0.html or the [LICENSE](LICENSE) file in this repository for the full license text.
