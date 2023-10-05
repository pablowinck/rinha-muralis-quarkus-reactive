#!/bin/zsh

docker build -t pablowinck/rinha-quarkus-reactive:2.6.8-Final -f src/main/docker/Dockerfile.native-micro . && docker push pablowinck/rinha-quarkus-reactive:2.6.8-Final