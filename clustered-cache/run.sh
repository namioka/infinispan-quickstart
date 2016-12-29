#!/bin/bash

set -eu

mvn exec:java -Dexec.args="-x -d $1" --quiet
