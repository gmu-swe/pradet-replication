#!/bin/bash

# Clone jodatime from github
git clone https://github.com/JodaOrg/joda-time jodatime

# Checkout the b609d7d66d commit
cd jodatime
git checkout b609d7d66d
