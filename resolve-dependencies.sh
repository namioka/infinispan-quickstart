#!/bin/bash

set -eu

mvn -T 1.0C clean install -DskipTests
mvn -T 1.0C dependency:sources
mvn -T 1.0C dependency:resolve -Dclassifier=javadoc

