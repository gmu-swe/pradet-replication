#!/bin/bash

LOG=git-history.log

if [ ! -f past-commits ]; then
  git rev-list --all > past-commits
fi

echo "Start at $(date)" |tee $LOG

CURRENT_HASH=$(git rev-parse HEAD)
cat past-commits | grep -v $CURRENT_HASH | while read -r CURRENT
do
  echo "Checking OUT $CURRENT" | tee -a $LOG

  git checkout $CURRENT
  echo "CLEAN PROJECT" | tee -a $LOG
  rm cp.txt
  mvn -q clean

  echo "START VERIFICATION"
  RAN=0
 # Do not run while in subshell using | otherwise we lost the value of RAN
  while read -r ID
  do
    echo "VERIFY ./dtd-original/$ID.actual.schedule ./dtd-original/$ID.actual.result" | tee -a $LOG
    ./verify.sh ./dtd-original/$ID.actual.schedule ./dtd-original/$ID.actual.result > /dev/null

    if [ $(grep -c "ERROR TEST COUNT DOES NOT RUN" verify.log) -eq 0 ]; then
	RAN=$(($RAN+1));
    else
     echo "MISSING TEST FOR ./dtd-original/$ID.actual.schedule at $CURRENT" | tee -a $LOG
     mv -v ./dtd-original/$ID.actual.schedule ./dtd-original/$ID.actual.schedule-missing-since-$CURRENT
     continue
    fi

    # THIS SUGGESTS THAT MD IS NOT THERE ANYMORE
    if [ ! $(grep -c "DIFFERENT RESULTS" verify.log) -eq 0 ]; then
      echo "$CURRENT $(tail -1 ./dtd-original/$ID.actual.schedule)" >> past-results
      echo "MD ./dtd-original/$ID.actual.schedule for test $(tail -1 ./dtd-original/$ID.actual.schedule) might not be there at $CURRENT" | tee -a $LOG
    else
      echo "MD ./dtd-original/$ID.actual.schedule for test $(tail -1 ./dtd-original/$ID.actual.schedule) is still there at $CURRENT" | tee -a $LOG
    fi
  # Do not start while inside subshell otherwise RAN will be always 0
   done < <(find ./dtd-original/ -iname "*.schedule" | sort | sed 's|[^0-9]*\([0-9][0-9]*\).*|\1|g')

   echo "We RAN $RAN verifications for $CURRENT" | tee -a $LOG
   if [ ${RAN} -eq 0 ]; then
      echo "No more tests to run. END"
      break
   fi
done

echo "End at $(date)" | tee -a $LOG
