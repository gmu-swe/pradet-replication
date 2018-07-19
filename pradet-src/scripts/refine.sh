#!/bin/bash

#NAME=$1
# Default to simple-random
STRATEGY=${1:-simple-random}
RUN_ORDER_FILE=${2:-$(pwd)/run-order}
REFERENCE_OUTPUT_FILE=${3:-$(pwd)/reference-output.csv}

echo "STRATEGY is $STRATEGY"
echo "RUN_ORDER_FILE is $RUN_ORDER_FILE"
echo "REFERENCE_OUTPUT_FILE is $REFERENCE_OUTPUT_FILE"

HOME=$(pwd)

#if [ ! -d $NAME ]; then echo "invalid project folder ${NAME}"; exit 1; fi
#cd ${NAME}

# Build application-classpath - note this doesn't contain the application
if [ ! -e cp.txt ]; then
  mvn dependency:build-classpath -DincludeScope=test -Dmdep.outputFile=cp.txt
fi

# Build App and Tests
if [ ! -e $(pwd)/target/classes -o ! -e $(pwd)/target/test-classes ]; then
  mvn compile test-compile
fi


DEPS_FILE="$(pwd)/deps.csv"
#RUN_ORDER_FILE="$(pwd)/run-order"
#REFERENCE_OUTPUT_FILE="$(pwd)/reference-output.csv"

# Prob this check is already inside depedency-refiner
if [ ! -f ${DEPS_FILE} ]; then echo "${DEPS_FILE} does not exist"; exit 1; fi
if [ ! -f ${RUN_ORDER_FILE} ]; then echo "${RUN_ORDER_FILE} does not exist"; exit 1; fi
if [ ! -f ${REFERENCE_OUTPUT_FILE} ]; then echo "${REFERENCE_OUTPUT_FILE} does not exist"; exit 1; fi

JUNIT_VERSION=
if [ $(cat cp.txt | grep "junit-4.11.jar" | wc -l) == 1 ]; then JUNIT_VERSION="-4-11"; fi


echo "JUNIT_VERSION is $JUNIT_VERSION"

# DEBUG="-Ddebug=true"
# EXTRA_JAVA_OPTS="-Xms3000m -Xmx3000m -XX:MaxPermSize=512m"

JAVA_OPTS="-Drun-order.file=${RUN_ORDER_FILE}-run-order -Dreference-output.file=${RUN_ORDER_FILE}-reference-output.csv"
# Default Java Option for the refiner
JAVA_OPTS="-Xms3000m -Xmx3000m -XX:MaxPermSize=512m $JAVA_OPTS $DEBUG"

# Projects require specific command line options to run correctly !
if [ -f ".additional-java-options" ]; then
  echo "Including ADDITIONAL java options $(cat .additional-java-options)"
  JAVA_OPTS="$JAVA_OPTS $(cat .additional-java-options)"
  EXTRA_JAVA_OPTS="$EXTRA_JAVA_OPTS $(cat .additional-java-options)"
fi

# Crystal test-subject
EXTRA_JAVA_OPTS="$EXTRA_JAVA_OPTS -Djava.awt.headless=true -DPROJECT_PATH=$(pwd)"; \

COMMAND=$(cat << EOL

export JAVA_OPTS="$JAVA_OPTS $DEBUG"; \
export EXTRA_JAVA_OPTS="$EXTRA_JAVA_OPTS" ; \
dependency-refiner${JUNIT_VERSION} 	\
	--application-classpath "$(pwd)/target/classes:$(pwd)/target/test-classes:$(cat cp.txt):" \
	--refinement-strategy "${STRATEGY}" \
	--dependencies $(pwd)/deps.csv \
	--show-progress \
	--run-order "${RUN_ORDER_FILE}" \
	--reference-output "${REFERENCE_OUTPUT_FILE}" 2>&1| \
		tee -a refinement.log
EOL
)

echo "${COMMAND}" | tee refinement.log

eval ${COMMAND}

# cd ${HOME}
