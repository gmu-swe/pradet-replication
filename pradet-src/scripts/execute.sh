#!/bin/bash

# Requires depedency-refiner to be installed. Check the updateCutBin@*.sh script
# This force a specific order of test execution and produced the run-order and reference-output.csv files

RUN_ORDER_FILE=$1
HOME=$(pwd)

if [ ! -f $RUN_ORDER_FILE ]; then echo "Invalid ${RUN_ORDER_FILE}"; exit 1; fi

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

#DEBUG="-Ddebug=true"

# Projects require specific command line options to run correctly !
if [ -f ".additional-java-options" ]; then EXTRA_JAVA_OPTS="$EXTRA_JAVA_OPTS $(cat .additional-java-options)"; fi

#set -x
export JAVA_OPTS="-Dshow-output=true $JAVA_OPTS $DEBUG";\
export EXTRA_JAVA_OPTS="-Drun-order.file=${RUN_ORDER_FILE}-run-order -Dreference-output.file=${RUN_ORDER_FILE}-reference-output.csv"

execute-tests-with-junit-core${JUNIT_VERSION} 	\
	--application-classpath "$(pwd)/target/classes:$(pwd)/target/test-classes:$(cat cp.txt):" \
	--test-list $(cat ${RUN_ORDER_FILE} | tr -d '\r' | tr "\n" " " ) 2>&1 | tee execution.log

cd ${HOME}
