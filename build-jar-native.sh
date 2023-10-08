#!/bin/zsh

docker build -t pablowinck/rinha-quarkus-reactive:2.6.1.1 -f src/main/docker/Dockerfile.native-micro . && docker push pablowinck/rinha-quarkus-reactive:2.6.1.1