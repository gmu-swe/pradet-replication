#!/bin/bash

if [ $# -ne 1 ]; then echo "Missing RUN ORDER"; exit 1; fi

RUN_ORDER=$1

#DDH="/scratch/datadep-detector/target"
DDH="/Users/gambi/Documents/Saarland/Frameworks/datadep-detector/target"

# Build application-classpath - note this doesn't contain the application
if [ ! -e cp.txt ]; then
  mvn dependency:build-classpath -DincludeScope=test -Dmdep.outputFile=cp.txt
fi

# Build App and Tests
if [ ! -e $(pwd)/target/classes -o ! -e $(pwd)/target/test-classes ]; then
  mvn compile test-compile
fi

JUNIT_VERSION=
if [ $(cat cp.txt | grep "junit-4.11.jar" | wc -l) == 1 ]; then JUNIT_VERSION="-4-11"; fi
echo "JUNIT_VERSION is $JUNIT_VERSION"

if [ ! -e $(pwd)/package-filter ]; then ./create_package_filter.sh; fi

#set -x

#export JAVA_OPTS="-Ddebug=true" # -Ddebug-file=debug.cfg"
export JAVA_OPTS="$DEBUG $JAVA_OPTS"
export EXTRA_JAVA_OPTS="-Djava.awt.headless=true -DPROJECT_PATH=$(pwd) $EXTRA_JAVA_OPTS"

start_time="$(date -u +%s)"

dependency-collector${JUNIT_VERSION} \
  --datadep-detector-home ${DDH} \
  --run-order ${RUN_ORDER} \
  --package-filter $(pwd)/package-filter \
  --application-classpath "$(pwd)/target/classes:$(pwd)/target/test-classes:$(cat cp.txt)" \
  --enums-file $(pwd)/enumerations 2>&1 | tee collection.log

end_time="$(date -u +%s)"

elapsed="$(($end_time-$start_time))"
echo "Total of $elapsed seconds elapsed for process" | tee -a collection.log

