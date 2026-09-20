# Repository Guidelines

## Project Structure & Module Organization

This is a single-module Java 21 NeoForge mod for Minecraft 1.21.1. Production code lives under `src/main/java/com/sshakusora/shadowsandpetals`; keep features grouped by domain, such as `block`, `item`, `worldgen`, `event`, or `compat`. Hand-authored assets, data, mixin configuration, and structures belong in `src/main/resources`. Data generators write to `src/generated/resources`, which is included in the main resource set. Unit tests mirror production packages in `src/test/java`, with fixtures in `src/test/resources`. Treat `build/`, `run/`, `logs/`, and `.gradle/` as local output, not source.

## Build, Test, and Development Commands

Use the checked-in Gradle wrapper (replace `./gradlew` with `.\gradlew.bat` on Windows):

- `./gradlew build` — compiles Java, runs tests, and creates the mod JAR in `build/libs`.
- `./gradlew test` — runs the JUnit 5 unit-test suite.
- `./gradlew runClient` — launches a development client and installs configured shader packs.
- `./gradlew runServer` — launches the NeoForge development server.
- `./gradlew runGameTestServer` — executes registered NeoForge GameTests.
- `./gradlew runData` — regenerates resources in `src/generated/resources`; review generated diffs before committing.
- `./gradlew runStructureEditor` — prepares and opens the dedicated structure-editing world.

## Coding Style & Naming Conventions

Use four-space indentation, braces on the same line, and standard Java conventions: `PascalCase` types, `camelCase` methods and fields, and `UPPER_SNAKE_CASE` constants. Keep packages lowercase beneath `com.sshakusora.shadowsandpetals`. Use descriptive registry classes such as `ItemRegistry` and namespace resource identifiers through `ShadowsAndPetals.asResource(...)`. No formatter or linter is configured; match nearby code and keep imports explicit and organized.

## Testing Guidelines

Write focused JUnit 5 tests named `*Test.java`; test methods should describe behavior, for example `selectsEntriesByTheirCumulativeWeights`. Mirror the source package and place reusable golden files under `src/test/resources`. Run `./gradlew test` for logic changes and the appropriate client, server, or GameTest task for rendering, registration, world-generation, or interaction changes. There is no enforced coverage threshold; cover regressions and edge cases.

## Commit & Pull Request Guidelines

Recent history uses short, imperative Conventional Commit-style subjects such as `feat: add create compat`, `fix: fix recessed lamp rendering`, and `refactor: extract ...`. Keep each commit scoped to one concern. Pull requests should explain the change, testing performed, compatibility impact, and any generated-resource updates. Link relevant issues and include screenshots or video for visual, UI, model, texture, or rendering changes. Ensure `./gradlew build` passes before requesting review.
