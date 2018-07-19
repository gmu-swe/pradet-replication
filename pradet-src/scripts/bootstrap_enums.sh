#!/bin/bash

#unameOut="$(uname -s)"
#case "${unameOut}" in
#    Linux*)     CREAT_TEMP="mktemp -d";;
#    Darwin*)    CREAT_TEMP="mktemp -d";;
#    *)          machine="UNKNOWN:${unameOut}"
#esac
#echo "Running on ${machine}"

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

LOG_FILE=enums.log

TEMP=`mktemp -d 2>/dev/null || mktemp -d -t 'mytmpdir'`

java \
	$E \
	-noverify -jar $HOME/.m2/repository/edu/gmu/swe/DependencyDetector/0.0.1-SNAPSHOT/DependencyDetector-0.0.1-SNAPSHOT.jar \
	--list-enumeration $(cat cp.txt):$(pwd)/target/classes:$(pwd)/target/test-classes $TEMP 2>&1 | tee $LOG_FILE
BEFORE=$(cat enumerations | wc -l)
cat $LOG_FILE | grep "THIS SHOULD" | awk '{print $NF}' | tr "/" "." | sed 's/^\(.*\)\..*$/\1/' | sort | uniq >> enumerations
mv enumerations enumerations.tmp
cat enumerations.tmp | sort | uniq >> enumerations
AFTER=$(cat enumerations | wc -l); if [ $(($AFTER-$BEFORE)) -gt 0 ]; then echo "NEW $(($AFTER-$BEFORE)) TOTAL $AFTER"; EXIT=1; else echo "Done"; EXIT=0; fi
rm -rf $TEMP
rm $LOG_FILE

exit $EXIT

