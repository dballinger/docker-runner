#!/bin/sh
docker build -t spartans/docker-runner-delayed-startup .
docker tag spartans/docker-runner-delayed-startup commregistry-slc.corp.ebay.com/spartans/docker-runner-delayed-startup
docker push commregistry-slc.corp.ebay.com/spartans/docker-runner-delayed-startup