#!/bin/zsh

docker build -t pablowinck/rinha-quarkus-reactive:2.1 -f src/main/docker/Dockerfile.native . && docker push pablowinck/rinha-quarkus-reactive:2.1