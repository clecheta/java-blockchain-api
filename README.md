## JBlock | Java Blockchain API

> **Honorable mention — craftsmanship era**  
> From June 2021 through August 2023, this API and its surrounding stack were built by hand — software as craft.  
> From November 2026 onward, evolution continues with AI as a collaborator, without erasing that artisan foundation.

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, visit https://quarkus.io/.

## Running the application in dev mode

You can run the master node in dev mode (with live coding) using:

```shell script
./mvnw compile quarkus:dev
```

To start the second node:

```shell script
mvn quarkus:dev -Dquarkus-profile=node -Ddebug=5006
```

## Kafka cluster

Start the Apache Kafka cluster by running:

```shell script
docker-compose up
```

## Running the webapp in dev mode

```shell script
cd src/main/webapp
npm run dev
```

This launches the web client at http://localhost:1234.

For this to work correctly in development, you may need to change the fetch base URL in each component. Components default to `${document.location.origin}`, which is intended for the production build.

## Creating a native executable

During the build, Maven runs `npm run dist` and copies the webapp client into `/resources/META-INF/resources` so the container can serve the UI together with the API.

Create a native executable with:

```shell script
./mvnw package -Pnative
```

If you do not have GraalVM installed, you can build the native executable in a container:

```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

Then run the native executable with:

```shell script
./target/api.blockchain-1.0.0-SNAPSHOT-runner
```

## Health checks

Define a health check in the Dockerfile that probes the SmallRye Health endpoints:

- http://localhost:8080/q/health
- http://localhost:8080/q/health/live
- http://localhost:8080/q/health/ready

For a human-readable health UI, open: http://localhost:8080/q/health-ui/

## License

This project is licensed under the [Apache License, Version 2.0](LICENSE).
See the [NOTICE](NOTICE) file for copyright attribution.

## Source of truth and contributions

The **canonical repository** is on GitLab:

https://gitlab.com/cleberlecheta/java-blockchain-api

Please open **issues** and **merge requests** only on GitLab.
Any GitHub (or other) mirror is a **showcase only** and is not the official contribution workflow.

See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines.
