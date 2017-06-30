FROM openjdk:8-jdk-alpine AS BUILD_IMAGE

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

FROM openjdk:8-jre-alpine

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
