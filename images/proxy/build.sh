#!/bin/sh
docker build -t spartans/docker-runner-proxy .
docker tag spartans/docker-runner-proxy commregistry-slc.corp.ebay.com/spartans/docker-runner-proxy
docker push commregistry-slc.corp.ebay.com/spartans/docker-runner-proxy