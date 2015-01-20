#!/bin/sh
docker build -t spartans/docker-runner-image1 .
docker tag spartans/docker-runner-image1 commregistry-slc.corp.ebay.com/spartans/docker-runner-image1
docker push commregistry-slc.corp.ebay.com/spartans/docker-runner-image1