#!/bin/bash

for STRATEGY in simple-random random source-first
do

./refine.sh $STRATEGY run-order reference-output.csv

mkdir $STRATEGY

cp deps.csv run-order reference-output.csv $STRATEGY/
mv refined* refinement.log manifest* iteration* $STRATEGY/

done

# Produce the output
for STRATEGY in simple-random random source-first
do
  echo $STRATEGY $(tail -11 $STRATEGY/refinement.log | grep -v "AVG" | grep -v "==="| sed 's|^.*:\(.*\)$|\1|' | tr "\n" "\t")
done
