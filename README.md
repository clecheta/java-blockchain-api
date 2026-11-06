## JBlock | Java Blockchain API

> **Honorable mention — craftsmanship era**  
> From June 2021 through August 2023, this API and its surrounding stack were built by hand — software as craft.  
> From November 2026 onward, evolution continues with AI as a collaborator, without erasing that artisan foundation.

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, visit https://quarkus.io/.

## JDK (SDKMAN + Java 25 LTS)

Use **SDKMAN**, not apt. Default toolchain is **Temurin 25 LTS**. For `-Pnative`, switch to **GraalVM CE 25** (bundles `native-image`):

```shell script
sdk install java 25.0.5.0+8-tem
sdk default java 25.0.5.0+8-tem
sdk install java 25.5.5.0+3-graalce   # optional, for native builds
sdk install maven 3.9.11
```

Platform target: **Quarkus 3.40.1.1 LTS** + **Java 25**. Maven wrapper pins **3.9.11** (Quarkus 3.40 requires Maven ≥ 3.9). CI uses Eclipse Temurin 25 for JVM tests and GraalVM native-image 25 for the native job.

## Running the application in dev mode

You can run the master node in dev mode (with live coding) using:

```shell script
./mvnw compile quarkus:dev
```

To start the second node:

```shell script
mvn quarkus:dev -Dquarkus-profile=node -Ddebug=5006
```

## Kafka + Mongo (local)

`docker-compose` brings up **Kafka 3.9 (KRaft)** and **Mongo 7** (no ZooKeeper):

```shell script
docker compose up -d
```

Root (`quarkus:dev`) loads chains from `mongodb://localhost:27017` on port `8018`.  
`mvn test` uses Quarkus **Dev Services** for Mongo + Kafka (Docker required); no compose needed for the unit/IT suite.

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

# 4) full HTTP smoke (seed funded wallet → transact → mine → peer tip/hash)
./scripts/smoke-two-nodes.sh
```

The script does **not** use the UI. Strategy / alternatives: GitLab **api#41**.

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
