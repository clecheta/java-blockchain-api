# Java Blockchain API

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run master node in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```
to start the second node: create a native executable and run it with:
```shell script
./target/api.blockchain-1.0.0-SNAPSHOT-runner -Dquarkus-profile=node
```

## Kafka cluster

Start up the Apache Kafka cluster by running `docker-compose up`

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/api.blockchain-1.0.0-SNAPSHOT-runner`

## Health Checks

Define a health check in Dockerfile that tests the SmallRye Health generated paths:
- http://localhost:8080/q/health
- http://localhost:8080/q/health/live
- http://localhost:8080/q/health/ready

For human-readable check purpose you can go to: http://localhost:8080/q/health-ui/