#
# Copyright © 2019 Smoke Turner, LLC (github@smoketurner.com)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

FROM openjdk:11-jdk-slim AS BUILD_IMAGE

WORKDIR /app

RUN mkdir -p pipeline-application pipeline-client

COPY pom.xml mvnw ./
COPY .mvn ./.mvn/
COPY pipeline-application/pom.xml ./pipeline-application/
COPY pipeline-client/pom.xml ./pipeline-client/

RUN ./mvnw install

COPY . .

RUN ./mvnw clean package -DskipTests=true -Dmaven.javadoc.skip=true -Dmaven.source.skip=true && \
    rm pipeline-application/target/original-*.jar && \
    mv pipeline-application/target/*.jar app.jar

FROM openjdk:11-jre-slim

ARG VERSION="1.0.2-SNAPSHOT"

LABEL name="pipeline" version=$VERSION

ENV PORT 8080

RUN apk add --no-cache curl

WORKDIR /app
COPY --from=BUILD_IMAGE /app/app.jar .
COPY --from=BUILD_IMAGE /app/config.yml .

HEALTHCHECK --interval=10s --timeout=5s CMD curl --fail http://127.0.0.1:8080/admin/healthcheck || exit 1

ENTRYPOINT ["java", "-d64", "-server", "-jar", "app.jar"]
CMD ["server", "config.yml"]
