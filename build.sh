#!/bin/bash
#
mvn package -Pnative -DskipTests=true
docker build -f src/main/docker/Dockerfile.native -t api.blockchain:latest .

echo
echo "finished at"
date
