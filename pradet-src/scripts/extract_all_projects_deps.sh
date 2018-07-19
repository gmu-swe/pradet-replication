#!/bin/bash
# Requires electric_test_project_info.csv to be in place
# Requires electrictest_deps.tsv to be in place

# Exit on error
set -e

HOME=$(pwd)

# Might take a while
while read -r line; do
  read NAME GIT_NAME GIT_HASH <<<$(echo $line | sed 's|"||g' |  tr "," "\n")

  echo "Processing ${NAME}"
  set -x
# match the entire word with grep
  grep -w ${NAME} electrictest_deps.tsv | awk '{print $2"."$3","$4"."$5}' | \
    sort | uniq | less > ${NAME}/deps.csv

done < <(sed 1d electric_test_project_info.csv)
