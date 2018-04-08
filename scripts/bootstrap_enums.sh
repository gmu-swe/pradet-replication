#!/bin/bash

: ${BIN:?Missing}

##############################################################################

# Build application-classpath - note this doesn't contain the application
if [ ! -e cp.txt ]; then
  mvn -q dependency:build-classpath -DincludeScope=test -Dmdep.outputFile=cp.txt
fi

# Build App and Tests
if [ ! -e $(pwd)/target/classes -o ! -e $(pwd)/target/test-classes ]; then
  mvn -q -U clean compile test-compile
fi

if [ -f enumerations ]; then
	E="-Denum-list=$(pwd)/enumerations"
fi

###############################################################################

function bootstrap(){
LOG_FILE=enums.log

TEMP=`mktemp -d 2>/dev/null || mktemp -d -t 'mytmpdir'`

java \
	$E \
    -noverify -jar ${BIN}/DependencyDetector-0.0.1-SNAPSHOT.jar \
	--list-enumeration $(cat cp.txt):$(pwd)/target/classes:$(pwd)/target/test-classes $TEMP 2>&1 > $LOG_FILE

BEFORE=$(cat enumerations | wc -l)

cat $LOG_FILE | grep "THIS SHOULD" | awk '{print $NF}' | tr "/" "." | sed 's/^\(.*\)\..*$/\1/' | sort | uniq >> enumerations

mv enumerations enumerations.tmp

cat enumerations.tmp | sort | uniq >> enumerations

AFTER=$(cat enumerations | wc -l); if [ $(($AFTER-$BEFORE)) -gt 0 ]; then echo "NEW $(($AFTER-$BEFORE)) TOTAL $AFTER"; EXIT=1; else echo "Done"; EXIT=0; fi

rm -rf $TEMP
rm $LOG_FILE
rm enumerations.tmp
}

# Run the bootstrap command for 5 times
for i in 1 2 3 4 5; do
  echo "Bootstrap ${i}"
  bootstrap >/dev/null 2>/dev/null
done
