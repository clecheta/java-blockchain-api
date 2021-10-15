#!/bin/bash
###
## https://quarkus.io/guides/building-native-image
## ./mvnw package -Pnative -Dquarkus.native.container-build=true -DskipTests=true
## requires a different gitlab-ci configuration to build using native.container-build=true
## @see https://git.okaut.com/Jesus/api.blockchain/-/issues/16
###
mvn package -Pnative -DskipTests=true
docker build -f src/main/docker/Dockerfile.native -t api.blockchain:latest .

echo
echo "finished at"
date
