#!/bin/zsh

docker build -t pablowinck/custom-ngnix:1.7.2 -f Dockerfile.ngnix . && docker push pablowinck/custom-ngnix:1.7.2
