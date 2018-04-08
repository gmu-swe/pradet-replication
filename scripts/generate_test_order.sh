#!/bin/bash

: ${BIN:?Missing}

########################################################################

# This one contains the list of Classes as well
INPUT_RUN_ORDER_FILE=$1

: ${INPUT_RUN_ORDER_FILE:?Missing}

OUTPUT_FOLDER=pradet
LOG=${OUTPUT_FOLDER}/execution.log

########################################################################

if [ ! -f ${INPUT_RUN_ORDER_FILE} ]; then echo "Invalid ${INPUT_RUN_ORDER_FILE}"; exit 1; fi

if [ ! -e cp.txt ]; then
  mvn dependency:build-classpath -DincludeScope=test -Dmdep.outputFile=cp.txt
fi


if [ ! -e $(pwd)/target/classes -o ! -e $(pwd)/target/test-classes ]; then
  mvn compile test-compile
fi

if [ ! -e ${OUTPUT_FOLDER} ]; then
mkdir ${OUTPUT_FOLDER}
fi

#set -x

JUNIT_VERSION=4.12
if [ $(cat cp.txt | grep "junit-4.11.jar" | wc -l) == 1 ]; then JUNIT_VERSION="4.11"; fi

# TODO Do we need those ?
DEBUG="-Ddebug=true"
export JAVA_OPTS="-Dshow-output=true $JAVA_OPTS $DEBUG"

export EXTRA_JAVA_OPTS="-Drun-order.file=run-order -Dreference-output.file=reference-output.csv"

${BIN}/junit-${JUNIT_VERSION}/bin/execute-tests-with-junit-core	\
	--application-classpath "$(pwd)/target/classes:$(pwd)/target/test-classes:$(cat cp.txt):" \
    --test-list $(cat ${INPUT_RUN_ORDER_FILE} | tr -d '\r' | tr "\n" " " ) 2>&1 | tee ${LOG}

### Now remove all the unecessarily files

REF_OUT=$(find . -iname "reference-output.csv-*" | sort | tail -1)
mv ${REF_OUT} reference-output.csv
rm reference-output.csv-*

REF_RUN=$(find . -iname "run-order-*" | sort | tail -1)
mv ${REF_RUN} test-execution-order
rm run-order-*
