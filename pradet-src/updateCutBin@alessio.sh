#!/bin/bash

rm -rf /usr/local/opt/cut

mvn clean install -DskipTests  package appassembler:assemble

mv target/appassembler /usr/local/opt/cut

mvn -P4.11 clean install -DskipTests  package appassembler:assemble

mv target/appassembler /usr/local/opt/cut

# Create the links
find /usr/local/opt/cut -type f -ipath "*/bin/*" -not -iname "*.bat" | while read -r F; do
  echo "Update ${F} -> $(basename ${F})"
  if [ ! -e /usr/local/bin/$(basename ${F}) ]; then
	  ln -f -s ${F} /usr/local/bin/$(basename ${F})
  fi
done
