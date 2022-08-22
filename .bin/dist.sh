#!/bin/bash

.bin/build.sh || exit 1
cd ./build || exit 1
tar -xvf ./distributions/al-ml.tar || exit 1
rm -rf dist || exit 1
mv al-ml dist || exit 1
