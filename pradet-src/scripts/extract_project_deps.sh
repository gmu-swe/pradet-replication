#!/bin/bash
# Requires electric_test_project_info.csv to be in place
# Requires electrictest_deps.tsv to be in place

# Exit on error
set -e

HOME=$(pwd)
NAME=$1

# Might take a while
echo "Processing ${NAME}"
set -x

# match the entire word with grep
# Get rid of deps which involves PARAMETRIZED TESTs
grep -w ${NAME} electrictest_deps.tsv | awk '{print $2"."$3","$4"."$5}' | \
    sort | uniq | sed '/\[/d' > ${NAME}/deps.csv
