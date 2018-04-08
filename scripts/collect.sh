#!/bin/bash

: ${BIN:?Missing}

# This one stores the instrumented jre
: ${DATADEP_DETECTOR_HOME:?Missing}

##############################################################################

TEST_ORDER=test-execution-order
LOG=pradet/collection.log

##############################################################################

if [ ! -e ${TEST_ORDER} ]; then
  echo "Test order file ${TEST_ORDER} does not exist ! Run generate_test_order.sh"
  exit 1
fi

if [ ! -e enumerations ]; then
  echo "Enumeration file does not exist ! Run bootstrap_enum.sh"
  exit 1
fi

if [ ! -e package-filter ]; then
  echo "package-filter file does not exist ! Run create_package_filter.sh"
  exit 1
fi

if [ ! -e pradet ]; then
mkdir pradet
fi


# Build application-classpath - note this doesn't contain the application
if [ ! -e cp.txt ]; then
  mvn dependency:build-classpath -DincludeScope=test -Dmdep.outputFile=cp.txt
fi

# Build App and Tests
if [ ! -e $(pwd)/target/classes -o ! -e $(pwd)/target/test-classes ]; then
  mvn compile test-compile
fi

JUNIT_VERSION=4.12
if [ $(cat cp.txt | grep "junit-4.11.jar" | wc -l) == 1 ]; then JUNIT_VERSION="-4-11"; fi
echo "JUNIT_VERSION is $JUNIT_VERSION"



export JAVA_OPTS="$DEBUG $JAVA_OPTS"
export EXTRA_JAVA_OPTS="-Djava.awt.headless=true -DPROJECT_PATH=$(pwd) $EXTRA_JAVA_OPTS"

##############################################################################

start_time="$(date -u +%s)"

${BIN}/junit-${JUNIT_VERSION}/bin/dependency-collector \
  --datadep-detector-home ${DATADEP_DETECTOR_HOME}/target \
  --run-order ${TEST_ORDER} \
  --package-filter $(pwd)/package-filter \
  --application-classpath "$(pwd)/target/classes:$(pwd)/target/test-classes:$(cat cp.txt)" \
--enums-file $(pwd)/enumerations 2>&1 | tee ${LOG}

end_time="$(date -u +%s)"

elapsed="$(($end_time-$start_time))"
echo "Total of $elapsed seconds elapsed for process" | tee -a ${LOG}

