#!/bin/bash

# TODO Double check the output anyway. A too broad package-filter (e.g., org., com. ) creates an uncessarily large number of dependencies

# Here we select the folders which contains at least 1 java class
find src/main/java -type f -iname "*.java" | while read -r F; do dirname $F | sed 's|/|.|g'; done | sed -e "s|src.main.java.||" -e "/^$/d" > package-filter-all
find src/test/java -type f -iname "*.java" | while read -r F; do dirname $F | sed 's|/|.|g'; done | sed -e "s|src.test.java.||" -e "/^$/d" >> package-filter-all
cat package-filter-all | sort | uniq > package-filter

rm package-filter-all
# NOTE AN EMPTY package-filter means that everything shall be included in the HeapWalking...
#if [ -e $(pwd)/package-filter ]; then rm package-filter; fi
#touch package-filter
