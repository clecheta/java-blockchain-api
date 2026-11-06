## JBlock | Java Blockchain API

> **Honorable mention — craftsmanship era**  
> From June 2021 through August 2023, this API and its surrounding stack were built by hand — software as craft.  
> From November 2026 onward, evolution continues with AI as a collaborator, without erasing that artisan foundation.

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, visit https://quarkus.io/.

## JDK (SDKMAN + GraalVM)

Use **SDKMAN**, not apt. For this repo (Java 17 + Quarkus native in CI), prefer **GraalVM CE 17** so the same JDK can compile, run `quarkus:dev`, and build `-Pnative` with `native-image`:

```shell script
sdk install java 17.0.9-graalce
sdk default java 17.0.9-graalce
# native-image is bundled / installable via: gu install native-image
sdk install maven 3.9.9
```

CI uses `ghcr.io/graalvm/graalvm-ce:22.0.0.2` (JDK 17 lineage). Mandrel 23+ targets newer JDKs — keep CE 17 until Quarkus is upgraded.

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

MongoDB must also be reachable at `mongodb://localhost:27017` (root loads chains from DB on port `8018`).

## Two-node smoke (incremental sync)

Kafka peers sync **one mined tip block** per message (not the full chain). Distinct consumer groups: root `18`, node `81`.

```shell script
# 1) infra
docker-compose up -d
# ensure Mongo is up on :27017

# 2) install library (from java-blockchain repo)
# mvn -f ../java-blockchain/pom.xml install -DskipTests

# 3) root + peer
./mvnw compile quarkus:dev
./mvnw quarkus:dev -Dquarkus.profile=node -Ddebug=5006

# 4) create wallets / pool a tx on root, then mine and assert peer tip
./scripts/smoke-two-nodes.sh
```

Catch-up endpoints (root):

- `POST /CrYP70/Ch41N/from/{number}`
- `POST /Ch41N/from/{number}`

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
