#!/bin/bash

set -eu

#mvn exec:java -Dexec.args="-x -d $1" --quiet
mvn exec:java -Dexec.mainClass=org.infinispan.quickstart.clusteredcache.Main --quiet
