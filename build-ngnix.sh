#!/bin/zsh

docker build -t pablowinck/custom-ngnix:1.1 -f Dockerfile.ngnix . && docker push pablowinck/custom-ngnix:1.1
