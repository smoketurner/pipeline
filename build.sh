#!/bin/sh

VERSION="1.0.2-SNAPSHOT"

docker build --build-arg VERSION=${VERSION} -t "smoketurner/pipeline:${VERSION}" .
