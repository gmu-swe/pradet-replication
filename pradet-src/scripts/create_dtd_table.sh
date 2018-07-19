#! /bin/bash

#set -x

function original {
for EXP in isolate reverse combination
do
 if [ ! -e "${EXP}${MINIMIZE}.log" -o ! -e "${EXP}${MINIMIZE}.txt" ]; then
  echo "MISSING FILES for ${EXP}${MINIMIZE}"
  continue
 fi

  TIME=$(tail -1 ${EXP}${MINIMIZE}.log | sed 's/Total of \(.*\) seconds .*/\1/')
  TESTS=$(cat ${EXP}${MINIMIZE}.log | grep "Test being" | wc -l)
  MANIFEST_DEP=$( cat ${EXP}${MINIMIZE}.txt | grep "Test: " | wc -l)

 echo "$EXP TESTS:$TESTS TIME:$TIME MD:$MANIFEST_DEP"
done
}

function true_and_false {
for MINIMIZE in true false
do
export MINIMIZE="-${MINIMIZE}"
echo "Minimize is ${MINIMIZE}"
original
done
unset MINIMIZE
}

###

if [ $(find . -iname "*-false.*" | wc -l) -gt 0 ]; then
  true_and_false
else
  export MINIMIZE="-true"
  original
fi

