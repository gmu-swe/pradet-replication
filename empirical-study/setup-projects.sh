#!/bin/bash
HERE=`pwd`
while IFS=, read -r name sha git
do
    git clone ${git} ${name}
    cd ${name}
    git checkout ${sha}
done < test-subjects.csv
