#!/bin/zsh

docker build -t pablowinck/rinha-quarkus-reactive:2.6.5 -f src/main/docker/Dockerfile.native-micro . && docker push pablowinck/rinha-quarkus-reactive:2.6.5