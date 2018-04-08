#!/bin/bash

: ${BIN:?Missing}

# This one stores the instrumented jre
: ${DATADEP_DETECTOR_HOME:?Missing}

########################################################################

STRATEGY="source-first"
RUN_ORDER_FILE="$(pwd)/test-execution-order"
REFERENCE_OUTPUT_FILE="$(pwd)/reference-output.csv"
DEPS_FILE="$(pwd)/deps.csv"
OUTPUT_FOLDER=pradet
LOG=${OUTPUT_FOLDER}/refinement.log

########################################################################

if [ ! -f ${DEPS_FILE} ]; then echo "${DEPS_FILE} does not exist. Run collect.sh"; exit 1; fi
if [ ! -f ${RUN_ORDER_FILE} ]; then echo "${RUN_ORDER_FILE} does not exist. Run generate_test_order.sh"; exit 1; fi
if [ ! -f ${REFERENCE_OUTPUT_FILE} ]; then echo "${REFERENCE_OUTPUT_FILE} does not exist. Run collect.sh"; exit 1; fi


if [ ! -e cp.txt ]; then
  mvn dependency:build-classpath -DincludeScope=test -Dmdep.outputFile=cp.txt
fi

if [ ! -e $(pwd)/target/classes -o ! -e $(pwd)/target/test-classes ]; then
  mvn compile test-compile
fi

if [ ! -e ${OUTPUT_FOLDER} ]; then
mkdir ${OUTPUT_FOLDER}
fi

JUNIT_VERSION=4.12
if [ $(cat cp.txt | grep "junit-4.11.jar" | wc -l) == 1 ]; then JUNIT_VERSION="-4-11"; fi

## TODO Why those ?
# JAVA_OPTS="-Drun-order.file=${RUN_ORDER_FILE}-run-order -Dreference-output.file=${RUN_ORDER_FILE}-reference-output.csv"

# Default Java Option for the refiner
JAVA_OPTS="-Xms3000m -Xmx3000m -XX:MaxPermSize=512m $JAVA_OPTS $DEBUG"

# Projects require specific command line options to run correctly !
# Is there any ?!
#if [ -f ".additional-java-options" ]; then
#  echo "Including ADDITIONAL java options $(cat .additional-java-options)"
#  JAVA_OPTS="$JAVA_OPTS $(cat .additional-java-options)"
#  EXTRA_JAVA_OPTS="$EXTRA_JAVA_OPTS $(cat .additional-java-options)"
#fi

# Those are hardcoded for Crystal test-subject which opens a GUI otherwise, this will not work on servers
EXTRA_JAVA_OPTS="$EXTRA_JAVA_OPTS -Djava.awt.headless=true -DPROJECT_PATH=$(pwd)";

# Add the	--show-progress  option to generate .dot files that illustrate the progress as graph of deps
COMMAND=$(cat << EOL

export JAVA_OPTS="$JAVA_OPTS $DEBUG"; \
export EXTRA_JAVA_OPTS="$EXTRA_JAVA_OPTS" ; \

${BIN}/junit-${JUNIT_VERSION}/bin/dependency-refiner \
	--application-classpath "$(pwd)/target/classes:$(pwd)/target/test-classes:$(cat cp.txt):" \
	--refinement-strategy "${STRATEGY}" \
	--dependencies $(pwd)/deps.csv \
	--run-order "${RUN_ORDER_FILE}" \
	--reference-output "${REFERENCE_OUTPUT_FILE}" 2>&1| \
        tee -a ${LOG}
EOL
)

echo "${COMMAND}" | tee ${LOG}

start_time="$(date -u +%s)"

  eval ${COMMAND}

end_time="$(date -u +%s)"

elapsed="$(($end_time-$start_time))"

echo "Total of $elapsed seconds elapsed for process" | tee -a ${LOG}

## Copyt all the files to OUT folder

mv manifest_dependency_at_* ${OUTPUT_FOLDER}
mv *.dot ${OUTPUT_FOLDER}


