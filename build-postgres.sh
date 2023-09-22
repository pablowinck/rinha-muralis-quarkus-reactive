#!/bin/zsh

docker build -t pablowinck/custom-postgres:1.0 -f Dockerfile.postgres . && docker push pablowinck/custom-postgres:1.0
