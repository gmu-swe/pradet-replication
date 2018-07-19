#!/bin/bash

set -x

find . -iname "pom.xml" | while read -r pomfile; do /scratch/PRADET/PRADET/code/python/updatePom.py -i $pomfile -p /scratch/PRADET/list-test-profile.xml -o $pomfile-updated; done

find . -iname "pom.xml" -exec mv -v {} {}-original \;

find . -iname "pom.xml-updated" | while read -r updated; do mv -v $updated ${updated%-*}; done


# TO REVERT

# Put the original in place

# find . -iname "pom.xml-original" | while read -r updated; do mv -v $updated ${updated%-*}; done

