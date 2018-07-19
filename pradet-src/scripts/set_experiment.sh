#!/bin/bash

# We need to collect run-order and reference-output.csv for all the projects

# TO DO SO: we must enable our profiles: list-test that add a test listener, and pradet which include JB mvn repo
# THIS STEP IS MOSLY MANUAL

# The we run the "default install"

# Use java 1.8
jenv local 1.8

# Run the build
export MAVEN_OPTS="-Xms3000m -Xmx3000m -XX:MaxPermSize=1024m"; mvn -fn install -Plist-test,pradet
