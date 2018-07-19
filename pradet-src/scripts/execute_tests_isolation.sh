#!/bin/bash

MANIFEST_DEP_SCHEDULE=$1
REFERENCE_OUTPUT=${MANIFEST_DEP_SCHEDULE}-reference-output-isolation.csv
if [ -f $REFERENCE_OUTPUT ]; then rm -v $REFERENCE_OUTPUT; fi

while read -r testName; do
  echo ${testName} > ${testName}
  echo "EXECUTING ${testName} IN ISOLATION"
#   ./execute.sh ${testName}
set -x
  find . -iname "${testName}*" -and -iname "*reference-output.csv" -exec cat {} \; >> $REFERENCE_OUTPUT
set +x
done < $MANIFEST_DEP_SCHEDULE

# Remove execTime
sed -i -e 's|,[0-9][0-9]*$||' $REFERENCE_OUTPUT
