#!/bin/sh

VERSION=`xmllint --xpath "//*[local-name()='project']/*[local-name()='version']/text()" pom.xml`

docker run \
--name pipeline \
--rm \
-e PORT=8080 \
-p 8080:8080 \
smoketurner/pipeline:${VERSION}
