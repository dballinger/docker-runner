#!/bin/sh
docker build -t spartans/docker-runner-image1:v2 .
docker tag spartans/docker-runner-image1:v2 commregistry-slc.corp.ebay.com/spartans/docker-runner-image1:v2
docker push commregistry-slc.corp.ebay.com/spartans/docker-runner-image1:v2