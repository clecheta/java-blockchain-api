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

CI resolves the `JBlock` library anonymously from the public [java-blockchain GitLab Package Registry](https://gitlab.com/cleberlecheta/java-blockchain/-/packages) (do not send `CI_JOB_TOKEN` to that other project — it returns 401). On version bumps, publish JBlock first so `0.x.y` exists before the API pipeline runs. The `integration` job runs Quarkus tests against Postgres, MongoDB, and Kafka services (Dev Services disabled). The package stage builds and pushes a JVM image to this project's Container Registry.

## Test locally (two nodes)

Needs **Java 25** (SDKMAN interactive shell so `JAVA_HOME` is set), **Docker**, and the `JBlock` library (from the Package Registry, or a sibling install for unpublished library changes).

```shell script
# 0) library — only needed for unpushed java-blockchain changes
#    (otherwise Maven pulls JBlock 0.1.0 from GitLab Packages)
mvn -f ../java-blockchain/pom.xml install -DskipTests

# 1) Kafka + Mongo
docker compose up -d

# 2) root  → http://localhost:8018   (terminal 1)
./mvnw compile quarkus:dev

# 3) peer  → http://localhost:8081   (terminal 2)
./mvnw quarkus:dev -Dquarkus.profile=node -Ddebug=5006

# 4) create alice + bob and print what to paste (terminal 3)
./scripts/init-local-wallets.sh

# 5) optional automated check (transact → mine → peer tip sync)
./scripts/smoke-two-nodes.sh
```

| | Root | Peer |
|---|---|---|
| HTTP | `:8018` | `:8081` |
| Kafka group | `18` | `81` |
| Ready | `/q/health/ready` | `/q/health/ready` |

### Glossary (read this once)

| Term | What it is | Where you paste it |
|---|---|---|
| **uid** | Private id of the wallet that **pays** | Conduct Transaction → *uid (sender)* |
| **address** | Public key of a wallet | Conduct Transaction → *recipient (address)* |
| **recipient** | Always an **address**, never a uid | same as above |
| **amount** | How much to send | Conduct Transaction → *amount* |
| **type** | Always `WALLET` in these tests | sent automatically by the UI / curl |

Do **not** mix them: Alice pays with her **uid**; Bob receives on his **address**.

Home page Address/Balance is the **miner** wallet — ignore it for transfers. Starting the nodes does **not** create user wallets; step 4 does.

### How to test after `init-local-wallets.sh`

The script prints Alice/Bob and two options. Short path in the browser:

1. Open http://localhost:8018/conduct-transaction  
2. Paste **Alice uid** → field *uid (sender)*  
3. Paste **Bob address** → field *recipient (address)*  
4. Amount e.g. `20` → Submit  
5. Open **Transaction Pool** → mine  
6. Open **Blocks** (or peer `:8081`) to see the new tip  

If the root log shows `PoolResource … wrong usage`, the request is missing `uid` / `recipient` / `amount` / `type`.

Unit/IT suite (`./mvnw test`) uses Quarkus Dev Services for Mongo + Kafka — no compose required.

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
./target/api.blockchain-0.1.0-runner
```

## Capacity testing (ledger plane)

Local Phase-1 capacity battery (max ledger `$`, wallets, host throughput) and how to evaluate results:

→ [docs/capacity/README.md](docs/capacity/README.md)

## Health checks

Root node (same paths on peer `:8081`):

- http://localhost:8018/q/health
- http://localhost:8018/q/health/live
- http://localhost:8018/q/health/ready
- http://localhost:8018/q/health-ui/

## License

This project is licensed under the [Apache License, Version 2.0](LICENSE).
See the [NOTICE](NOTICE) file for copyright attribution.

## Source of truth and contributions

The **canonical repository** is on GitLab:

https://gitlab.com/cleberlecheta/java-blockchain-api

Please open **issues** and **merge requests** only on GitLab.
Any GitHub (or other) mirror is a **showcase only** and is not the official contribution workflow.

See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines.
