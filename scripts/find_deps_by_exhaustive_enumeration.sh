#!/bin/bash

: ${DTDETECTOR_HOME:?Missing}

########################################################
# This script MUST run in the home folder of the maven project
########################################################

# Exit on error
set -e

PROJECT_NAME=$(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.artifactId  | grep -v "\[" )
PROJECT_VERSION=$(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version  | grep -v "\[" )

PROJECT_BUILD=$(pwd)/target/classes
PROJECT_TESTS=$(pwd)/target/test-classes

TEST_ORDER=test-execution-order
DTD_OUTPUT=dtd-results

# By default DTD will minimize dependencies, this takes a long time...
MINIMIZE="${MINIMIZE:-true}"
# We run DTD "reverse" analysis
ANALYSIS="combination"

LOG=${DTD_OUTPUT}/${ANALYSIS}-${MINIMIZE}.log
OUTPUT=${DTD_OUTPUT}/${ANALYSIS}-${MINIMIZE}.txt

########################################################

# Output folder
if [ ! -e ${DTD_OUTPUT} ]; then
mkdir ${DTD_OUTPUT}
fi

if [ ! -e ${TEST_ORDER} ]; then
echo "Test order file ${TEST_ORDER} does not exist !"
exit 1
fi

# Build application-classpath - note this doesn't contain the application itself
if [ -e cp.txt ]; then
rm cp.txt
fi

echo "Building application classpath"
mvn dependency:build-classpath -DincludeScope=test -Dmdep.outputFile=cp.txt

echo "Building application and tests"
# Build the app under analysis and its test dependencies those will be availble under ./target/classes ./target/test-classes
mvn clean compile test-compile

########################################################
echo "Starting DTD ${ANALYSIS} analysis"
set +e

echo "Output results to -->" ${OUTPUT}
echo "Output logs to --> " ${LOG}

start_time="$(date -u +%s)"

java -cp ${DTDETECTOR_HOME}/*:${PROJECT_BUILD}:${PROJECT_TESTS}:$(cat cp.txt) \
edu.washington.cs.dt.main.Main \
--${ANALYSIS} --k=2 \
--comparestacktrace=false \
--tests=${TEST_ORDER} \
--report=${OUTPUT} \
--minimize=${MINIMIZE} 2>&1 | tee ${LOG}

end_time="$(date -u +%s)"

elapsed="$(($end_time-$start_time))"
echo "Total of $elapsed seconds elapsed for process" | tee -a ${LOG}

