# Repository Guidelines

## Project Structure & Module Organization

This is a Java 25 NeoForge mod. Production code lives in `src/main/java/com/sshakusora/shadowsandpetals`, organized by feature areas such as `block`, `client`, `data`, `item`, and `worldgen`. Hand-authored assets, data files, mixin configuration, and structures belong in `src/main/resources`. Data generation writes to `src/generated/resources`; update generators rather than hand-editing generated JSON. JUnit tests mirror the production package tree under `src/test/java`, with fixtures in `src/test/resources`. Developer utilities live in `tools/blockbench` and `tools/petalledger`. Treat `build/`, `run/`, and `.gradle/` as disposable local output.

## Build, Test, and Development Commands

Use the checked-in Gradle wrapper (on Windows, replace `./gradlew` with `.\gradlew.bat`):

- `./gradlew build` — compile, run tests, and produce the mod JAR in `build/libs`.
- `./gradlew test` — run the JUnit 5 unit suite.
- `./gradlew runClient` — launch a development Minecraft client and install test shader packs.
- `./gradlew runServer` — launch the dedicated development server.
- `./gradlew runGameTestServer` — execute registered NeoForge GameTests.
- `./gradlew runClientData` — regenerate resources under `src/generated/resources`.
- `./gradlew runStructureEditor` — prepare and open the structure-editor world.

Node-based utility tests can be run with `node --test tools/blockbench/sap_animation_exporter.test.js`.

## Coding Style & Naming Conventions

Follow the existing Java style: four-space indentation, braces on the declaration line, one public top-level type per file, and package names in lowercase. Use `PascalCase` for types, `camelCase` for methods and variables, and `UPPER_SNAKE_CASE` for constants. Keep packages under `com.sshakusora.shadowsandpetals`; resource identifiers and file names use lowercase `snake_case`. Preserve `@NullMarked` package defaults and use `@Nullable` only where null is intentional. No formatter is enforced, so format consistently with nearby code and keep imports tidy.

## Testing Guidelines

Add focused JUnit 5 tests for behavior changes. Name classes `*Test` and test methods as readable behavior statements, for example `impactTicksFollowTheExportedAnimationWithoutAccumulatingDrift`. Mirror the source package, place golden fixtures in `src/test/resources`, and run `./gradlew test` before submitting. There is no configured coverage threshold; prioritize regressions, edge cases, data-generation stability, and client logic that can be tested without launching Minecraft.

## Commit & Pull Request Guidelines

Recent history uses imperative Conventional Commit-style prefixes such as `fix:`, `refactor:`, `merge:`, and `delete:`. Keep each commit scoped and explain the user-visible or technical outcome. Pull requests should include a concise summary, testing performed, linked issue when applicable, and screenshots or short clips for rendering, GUI, model, texture, or animation changes. Commit regenerated resources alongside generator changes and ensure `./gradlew build` passes.
