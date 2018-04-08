#!/bin/bash

# Merging the pieces back
dynoptic.partaa > dynoptic.tar.gz
dynoptic.partab >> dynoptic.tar.gz
dynoptic.partac >> dynoptic.tar.gz

# expanding the tar archive
tar -xzf dynoptic.tar.gz
