# Repository Guidelines

## Project Structure & Module Organization

Production Java lives in `src/main/java/com/sshakusora/shadowsandpetals`, grouped by feature areas such as `block`, `client`, `item`, `registries`, and `worldgen`. Hand-authored assets, data, mixin configuration, and structures belong in `src/main/resources`. Data-generator output is committed under `src/generated/resources`; update it through the generator instead of editing generated JSON manually. JUnit tests mirror production packages under `src/test/java`. Blockbench export tooling and its JavaScript tests live in `tools/blockbench`. Treat `build/`, `run/`, and `logs/` as local output.

The curtains (sixteen dye colors, `<color>_curtain` blocks under `BlockRegistry.CURTAINS`) render as plain baked block-state models while static and switch to the BER animation rig only for the ~6-tick `ANIMATING` window. The rig/controller/clip resources under `sap/animations` and `neoforge/animations` (`curtain/<half>_<side>` with `opening`/`closing` clips) are color-agnostic. The white masters (`curtain_upper_right.json`, `curtain_upper_left.json`, `curtain_lower_right.json`, `curtain_lower_left.json` and their `*_open` baked variants, `white_curtain.json` item display model, `white.png` texture) are the only hand-authored sources. `tools/curtain/split_curtain_model.js` bakes the open-pose masters from the opening clips, derives every other color as parent-reference variants (texture override only), and splits the closed white masters into per-bone parent models under `models/block/curtain/curtain_<half>_<side>/`; each colored per-bone file under `curtain_<half>_<side>_<color>/` is a texture-override stub of the white bone model, so geometry edits only touch the white files and no open-pose per-bone files exist (the open pose renders through the static block-state model). After any texture/UV/geometry edit to a white master, re-run `node tools/curtain/split_curtain_model.js`, then `./gradlew.bat runClientData`, or the placed block keeps showing old assets. The renderer resolves per-bone models through `BlockModelRegistry.CURTAIN_*` sets keyed by `(DyeColor, bone)`.

The large curtains (sixteen dye colors, `<color>_large_curtain` blocks under `BlockRegistry.LARGE_CURTAINS`, a 2x2 structure with HALF/COLUMN/SIDE properties) follow the same static-plus-ANIMATING architecture through `LargeCurtainBlock` and `LargeCurtainBlockEntityRenderer`. SIDE (`left`/`right`, window side, observer-relative) mirrors the small curtain's pairing: placement derives the side from a neighbouring large curtain (sneak copies it), and a window pair toggles open/close together. The hand-authored sources live in `models/block/large_curtain/`, per side: `large_curtain_<side>.bbmodel`, the Blockbench geometry export `large_curtain_<side>.json`, and `large_curtain_<side>.animation.json` (Blockbench’s native Bedrock export; X position and Y rotation channels are mirrored relative to the editor model, and the left animation additionally has every operation value negated so the mirror lands on the left-bunching pose). `tools/curtain/split_large_curtain_model.js` converts those into the shared item/closed/open masters (right side only), the per-side per-bone parent models under `large_curtain/` and `large_curtain_left/`, and the per-side rig/controller/clip resources under `sap/animations/large_curtain/<side>` and `neoforge/animations/entity/large_curtain/<side>`. The closed/open static quadrant masters `large_curtain_<side>_<quadrant>.json` (quadrant = observer-relative `l1`/`r1`/`l2`/`r2`, l = outer/bunching column, 1 = upper row) are curated files, not tool output. After any geometry or animation edit, re-run `node tools/curtain/split_large_curtain_model.js`, then `./gradlew.bat runClientData`.

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
