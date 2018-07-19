#!/bin/bash

set -x

rm -rf $HOME/bin/cut
rm -rf $HOME/bin/cut-4.11

mvn clean install -DskipTests  package appassembler:assemble

mv target/appassembler $HOME/bin/cut

mvn -P4.11 clean install -DskipTests  package appassembler:assemble

mv target/appassembler $HOME/bin/cut-4.11

bash <<"EOF"

# Create the links
find $HOME/bin/cut -type f -ipath "*/bin/*" -not -iname "*.bat" | while read -r F; do
echo "Update ${F} -> $(basename ${F})"
if [ ! -e $HOME/bin/$(basename ${F}) ]; then
ln -f -s ${F} $HOME/bin/$(basename ${F})
fi
done

# Create the links
find $HOME/bin/cut-4.11 -type f -ipath "*/bin/*" -not -iname "*.bat" | while read -r F; do
echo "Update ${F} -> $(basename ${F})"
if [ ! -e $HOME/bin/$(basename ${F}) ]; then
ln -f -s ${F} $HOME/bin/$(basename ${F})
fi
done

EOF

# Update also the test-listener
mvn clean install -DskipTests -Ptest-listener

# Refresh profile
source ~/.profile
