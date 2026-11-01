# Repository Guidelines

## Project Structure & Module Organization

This single-module project targets Minecraft 1.21.1 with NeoForge and Java 21. The mod entry point is `src/main/java/com/sshakusora/shadowsandpetals/ShadowsAndPetals.java`. Group new features into domain packages beneath `com.sshakusora.shadowsandpetals`, such as `block`, `item`, or `worldgen`.

Place hand-authored assets and data in `src/main/resources`; data generation writes to `src/generated/resources`, which is included in the main resource set. Mod metadata lives in `src/main/templates/META-INF/neoforge.mods.toml` and receives properties from `gradle.properties`. Treat `build/`, `run/`, `logs/`, and `.gradle/` as local output.

## Build, Test, and Development Commands

Use the checked-in Gradle wrapper with Java 21. On Windows, replace `./gradlew` with `.\gradlew.bat`.

- `./gradlew build` — compiles the mod, runs configured checks, and produces JARs in `build/libs/`.
- `./gradlew clean build --no-daemon` — reproduces the GitHub Actions build command.
- `./gradlew runClient` — launches the development client in `run/client/`.
- `./gradlew runServer` — launches the development server in `run/server/`.
- `./gradlew runData` — generates resources into `src/generated/resources/`; review generated changes before committing.

## Coding Style & Naming Conventions

Follow existing Java code: four-space indentation, same-line opening braces, explicit imports, `PascalCase` types, `camelCase` methods and fields, and `UPPER_SNAKE_CASE` constants. Keep package names lowercase. Create namespaced resource identifiers with `ShadowsAndPetals.asResource(...)`.

The `.editorconfig` requires UTF-8, LF line endings, and a final newline; batch files use CRLF. No formatter or linter is configured.

## Testing Guidelines

No test sources, testing framework, or coverage threshold are currently configured. Run `build` for every code change and smoke-test gameplay changes with `runClient`; verify server compatibility with `runServer` when relevant. If adding automated tests, place them in `src/test/java` and configure the required framework in `build.gradle`.

## Commit & Pull Request Guidelines

The available history contains `chore: initialize clean NeoForge baseline`. Follow that Conventional Commit-style pattern with concise, imperative subjects, such as `feat: add petal block`. Keep commits focused.

Pull requests should describe behavior changes, link relevant issues, record validation performed, and include screenshots for visible changes. Explain dependency or generated-resource changes. Ensure the CI build passes before requesting review.
