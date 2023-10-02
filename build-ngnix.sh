#!/bin/zsh

docker build -t pablowinck/custom-ngnix:1.5 -f Dockerfile.ngnix . && docker push pablowinck/custom-ngnix:1.5
