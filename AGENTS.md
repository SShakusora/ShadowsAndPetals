# Repository Guidelines

## Project Structure & Module Organization

Production Java lives in `src/main/java/com/sshakusora/shadowsandpetals`, grouped by feature areas such as `block`, `client`, `item`, `registries`, and `worldgen`. Hand-authored assets, data, mixin configuration, and structures belong in `src/main/resources`. Data-generator output is committed under `src/generated/resources`; update it through the generator instead of editing generated JSON manually. JUnit tests mirror production packages under `src/test/java`. Blockbench export tooling and its JavaScript tests live in `tools/blockbench`. Treat `build/`, `run/`, and `logs/` as local output.

The animated curtains (sixteen dye colors) render through per-bone model files (`models/block/curtain/curtain_upper_r_<color>/*.json`) bound by the block-entity renderer's rig. `tools/curtain/split_curtain_model.js` both derives the per-color model variants from the white masters (parent-reference style) and splits them into per-bone files. After any texture/UV/geometry edit to a white master, re-run `node tools/curtain/split_curtain_model.js`, or the placed block keeps showing old textures.

## Build, Test, and Development Commands

Use the checked-in Gradle wrapper (on Windows, replace `./gradlew` with `./gradlew.bat`):

- `./gradlew build` compiles Java 25 sources, runs tests, and creates the mod JAR in `build/libs`.
- `./gradlew test` runs the JUnit 5 unit suite.
- `./gradlew runClient` launches the primary NeoForge development client and installs test shader packs.
- `./gradlew runServer` launches a local development server.
- `./gradlew runGameTestServer` runs registered NeoForge game tests.
- `./gradlew runClientData` regenerates resources in `src/generated/resources`.
- `./gradlew runStructureEditor` prepares and opens the linked structure-editor world.
- `node --test tools/blockbench/sap_animation_exporter.test.js` tests the Blockbench exporter.

## Coding Style & Naming Conventions

Follow the existing Java style: four-space indentation, braces on the same line, and imports grouped with `java.*` after project/library imports. Use lowercase package names, `UpperCamelCase` types, `lowerCamelCase` methods and fields, and `UPPER_SNAKE_CASE` constants. Registry/bootstrap classes commonly use the `SAP` prefix or `*Registry` suffix. Resource identifiers and paths must be lowercase snake_case under the `shadowsandpetals` namespace. No formatter or linter is configured; keep changes consistent with nearby code and avoid unrelated reformatting.

## Testing Guidelines

Add focused JUnit 5 tests beside the corresponding package. Name test classes `*Test` and test methods by behavior, for example `emptyPoolReturnsNoEntry`. Cover normal behavior, boundaries, and regressions. Run `./gradlew test` during development and `./gradlew build` before submitting. For rendering, shaders, structures, or world generation, also exercise the relevant client or GameTest run.

## Commit & Pull Request Guidelines

Recent history favors concise imperative Conventional Commit subjects such as `feat: add advancements.`, `fix: replace wood pillar model.`, and `refactor: refactor CreativeTab registry.` Keep each commit scoped to one logical change. Pull requests should explain player-visible behavior, implementation impact, and verification commands; link related issues and include screenshots or short clips for visual changes. Commit regenerated resources whenever their generator changes.
