#!/bin/bash

IAM_USER_KEY="/home/pablo/Documents/rinhabackend-ec2-keypair.pem"
SERVER="ubuntu@ec2-54-89-252-232.compute-1.amazonaws.com"

echo "Copying compose to server"
scp -i $IAM_USER_KEY docker-compose.yml $SERVER:/home/ubuntu || echo "failed to copy compose"