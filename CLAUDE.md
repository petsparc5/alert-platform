# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

Newly scaffolded Maven project — the only committed source is `pom.xml`. There is no application code, tests, or dependencies yet. The Maven coordinates are `com.petsparc5.alerts:alert-platform` (an "alert platform"), though the repository/IDE project is named `Sonrisa`.

## Build environment

- **Java 25** (`maven.compiler.source`/`target` = 25). The IDE (`.idea/misc.xml`) also pins language level JDK 25. Ensure `JAVA_HOME` points at a JDK 25.
- **Maven** — no wrapper (`mvnw`) is committed, so use a locally installed `mvn`.
- Standard Maven layout applies once code is added: `src/main/java`, `src/test/java`.

## Common commands

```bash
mvn compile                  # compile main sources
mvn test                     # run all tests
mvn -Dtest=ClassName test    # run a single test class
mvn -Dtest=ClassName#method test   # run a single test method
mvn package                  # build the artifact
mvn clean install            # full clean build + install to local repo
```

No test framework (JUnit, etc.) is declared in `pom.xml` yet — add the relevant dependency before writing tests.
