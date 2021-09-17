# Java Blockchain API

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run master node in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```
to start the second node: 
```shell script
mvn quarkus:dev -Dquarkus-profile=node -Ddebug=5006 
```

## Kafka cluster

Start up the Apache Kafka cluster by running `docker-compose up`

## Running the webapp in dev mode
```shell script
cd src/main/webapp
npm run dev
```
This will launch the web client in http://localhost:1234.
For this to run properly it's necessary to change the fetch address
in each component as they use ${document.location.origin} as a default for production build.

## Creating a native executable

During the build mvn targets ```npm run dist``` and copy the webapp client content
to /resources/META-INF/resources so that we can successfully run the container with UI integrated to it.

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