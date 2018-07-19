#!/bin/bash

sudo rm -rf /opt/cut
sudo rm -rf /opt/cut-4.11

mvn -U clean install -DskipTests  package appassembler:assemble

sudo mv target/appassembler /opt/cut

mvn -U -P4.11 clean install -DskipTests  package appassembler:assemble

sudo mv target/appassembler /opt/cut-4.11

sudo bash <<"EOF"

# Create the links
find /opt/cut -type f -ipath "*/bin/*" -not -iname "*.bat" | while read -r F; do
echo "Update ${F} -> $(basename ${F})"
if [ ! -e /usr/local/bin/$(basename ${F}) ]; then
ln -f -s ${F} /usr/local/bin/$(basename ${F})
fi
done

# Create the links
find /opt/cut-4.11 -type f -ipath "*/bin/*" -not -iname "*.bat" | while read -r F; do
echo "Update ${F} -> $(basename ${F})"
if [ ! -e /usr/local/bin/$(basename ${F}) ]; then
ln -f -s ${F} /usr/local/bin/$(basename ${F})
fi
done

EOF

# Update also the test-listener
mvn clean install -DskipTests -Ptest-listener
