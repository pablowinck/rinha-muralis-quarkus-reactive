#!/bin/zsh

docker build -t pablowinck/rinha-quarkus-reactive:0.2 -f Dockerfile.jvm . && docker push pablowinck/rinha-quarkus-reactive:0.2
