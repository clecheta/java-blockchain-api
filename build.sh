#!/bin/bash
mvn package -Pnative
#./mvnw package -Pnative -Dquarkus.native.container-build=true -DskipTests=true
#docker build -f src/main/docker/Dockerfile.native -t api.blockchain:latest .

echo
echo "finished at"
date
