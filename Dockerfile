FROM java:openjdk-8-jre-alpine
MAINTAINER Justin Plock <jplock@smoketurner.com>

ARG VERSION="1.0.2-SNAPSHOT"

LABEL name="pipeline" version=$VERSION

ENV PORT 8080

RUN apk add --no-cache openjdk8="$JAVA_ALPINE_VERSION"

WORKDIR /app

RUN mkdir -p pipeline-application pipeline-client

COPY pom.xml mvnw ./
COPY .mvn ./.mvn/
COPY pipeline-application/pom.xml ./pipeline-application/
COPY pipeline-client/pom.xml ./pipeline-client/

RUN ./mvnw install

COPY . .

RUN ./mvnw package -DskipTests=true -Dmaven.javadoc.skip=true -Dmaven.source.skip=true && \
    rm pipeline-application/target/original-*.jar && \
    mv pipeline-application/target/pipeline-application-1.0.2-SNAPSHOT.jar app.jar && \
    rm -rf /root/.m2 && \
    rm -rf pipeline-application/target && \
    rm -rf pipeline-client/target && \
    apk del openjdk8

ENTRYPOINT ["java", "-d64", "-server", "-jar", "app.jar"]
CMD ["server", "config.yml"]
