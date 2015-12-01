FROM maven:3-jdk-8
MAINTAINER Justin Plock <jplock@smoketurner.com>

LABEL name="pipeline" version="1.0.0-SNAPSHOT"

RUN mkdir -p /src
WORKDIR /src
ADD . /src
RUN mvn package -DskipTests=true && rm -rf $HOME/.m2
WORKDIR pipeline-application
VOLUME ["/src/pipline-application"]

EXPOSE 8080 8180
ENTRYPOINT ["java", "-d64", "-server", "-jar", "target/pipeline-application-1.0.2-SNAPSHOT.jar"]
CMD ["server", "pipeline.yml"]
