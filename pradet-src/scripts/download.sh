#!/bin/bash
# Requires electric_test_project_info.csv to be in place

# Exit on error
set -e

HOME=$(pwd)

while read -r line; do
  read NAME GIT_NAME GIT_HASH <<<$(echo $line | sed 's|"||g' |  tr "," "\n")
  GITHUB="https://github.com/${GIT_NAME}.git"

  echo $GITHUB
  # Clone project to NAME folder
  git clone ${GITHUB} ${NAME}

  cd ${NAME}
    git reset --hard ${GIT_HASH}
  cd ${HOME}
done < <(sed 1d electric_test_project_info.csv)
# GITHUB="https://github.com/${PROJECT_NAME}.git
