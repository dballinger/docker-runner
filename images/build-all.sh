#!/usr/bin/env bash

cd delayed-startup
source build.sh

cd ../proxy
source build.sh

cd ../test-image
source build.sh
