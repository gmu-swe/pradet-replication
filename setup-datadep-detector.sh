#!/bin/bash

# Setup the custom version of the datadep-detector framework for instrumenting the JRE
git clone https://github.com/skappler/datadep-detector

# Install the project, and create the necessary file, including instrumenting the JRE

cd datadep-detector

mvn clean install -DskipTests
