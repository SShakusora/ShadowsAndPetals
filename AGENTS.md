# ShadowsAndPetals - Agent Notes

Minecraft NeoForge mod (MC 26.1.2 / NeoForge 26.1.2.0-beta). Java 25. Gradle with `net.neoforged.moddev` plugin (2.0.141).
Root package: `com.sshakusora.shadowsandpetals`.

## Build & Run

```bash
./gradlew build          # Build the mod JAR
./gradlew runClient      # Launch main client instance
./gradlew runClient2     # Launch second client (run2 dir, username Dev2)
./gradlew runServer      # Launch server instance
./gradlew runData        # Run data generators -> src/generated/resources/
./gradlew runStructureEditor # Launch the structure-editor void world
./gradlew runGameTestServer  # Runs gametests then exits (will crash - none exist)
```

Data generation outputs to `src/generated/resources/`, included in the `main` source set (`build.gradle:127`). Commit generated files.
No tests exist — no `src/test/`, no gametest files.

`runStructureEditor` depends on `prepareStructureEditorWorld`, which creates `run/saves/sap_structure_editor` and links its `generated/shadowsandpetals/structure/` directory to `src/main/resources/data/shadowsandpetals/structure/`. Each execution clears and rebuilds the editor world's generated layout.

## Architecture

- **Entrypoint**: `ShadowsAndPetals.java` — `@Mod(ShadowsAndPetals.MOD_ID)` class. Calls `SAPRegistries.register(modEventBus)` then `ItemRegistry.init()`, `BlockRegistry.init()`, `BlockEntityRegistry.init()`, `MenuRegistry.init()`, `EntityRegistry.init()`, `ParticleRegistry.init()`, `SoundRegistry.init()`, `RecipeSerializerRegistry.init()`, `CreativeTabRegistry.init()`, and finally `SAPFeatures.init()`. Provides `ShadowsAndPetals.asResource(String path)` convenience method.
- **Registries**: `SAPRegistries` holds all `DeferredRegister` instances: `BLOCKS`, `ITEMS`, `CREATIVE_TABS`, `ENTITIES`, `BLOCK_ENTITIES`, `PARTICLES`, `FEATURES`, `SOUNDS`, `MENUS`, `RECIPE_SERIALIZERS`. Use the fluent builder factory methods (`SAPRegistries.block(...)`, `.item(...)`, etc.) instead of touching `DeferredRegister` directly.
- **`init()` methods are empty** — side effects happen during static initialization (builder chains at field declaration time).

## Registration Conventions

All content is registered through fluent builders in `registries/builder/`:

- `RegBlockBuilder` — blocks, block items, datagen wiring, connected textures, legacy aliases, tooltip hooks
- `RegItemBuilder` — items, models, recipes, lang; nested `BlockItemBuilder` for standalone block items
- `RegBlockEntityBuilder` — block entities + legacy alias support
- `RegCreativeTabBuilder` — creative tabs
- `RegEntityBuilder` / `MobBuilder` — entities
- `RegParticleBuilder` — particles
- `RegSoundBuilder` — sounds

Canonical block chain (order is flexible; see `BlockRegistry.java`):
`.block(...)` → `.properties(...)` → `.tags(...)` → `.withItem()` → `.creativeTab(...)` → `.blockstate(...)` → `.loot(...)` → `.recipe(...)` → `.lang(...)` → `.register()`

Useful builder extras:
- `.connectedTexture(...)` — connected textures (types in `CTTextureType`: `OMNIDIRECTIONAL`, `HORIZONTAL`, `VERTICAL`)
- `.clientItem(modelId)` / `.customClientItem(...)` — custom item model mappings for `ModClientItemProvider`
- `.tooltipDescription()` / `.tooltipModifier()` / `.tooltipComponent(...)` — tooltip pipeline wiring
- `.alias(oldPath)` / `.alias(namespace, oldPath)` / `.stateAliasProperties(...)` — legacy migration

Multi-variant helpers:
- `DyedBlockList<T>` — 16 dye colors
- `WoodBlockList<T>` — 15 wood types (9 vanilla + 3 custom `WoodSetList` types + bamboo + 2 nether)
- `WoodSetList` — groups the 18 custom-wood blocks (log, stripped log, wood, stripped wood, planks, post, stripped post, wood post, stripped wood post, slab, stairs, fence, fence gate, pressure plate, button, sapling, leaves, hedge) for sakura, maple, ginkgo

## Data Generation

- `ModDataGenerator.gatherData()` wires 14 providers via `@EventBusSubscriber` on `GatherDataEvent.Client`.
- **Content flows from builders only** — `.blockstate()`, `.recipe()`, `.loot()`, `.lang()` on builders auto-wire to datagen registries. Do not edit provider classes directly.
- Block tags live in `BlockTagRegistry`; use `.tags(...)` on blocks and `BlockTagRegistry.include(...)` for nested tag references.
- Languages: default English (auto-generated from ids) + `zh_cn` (manual via `.lang("zh_cn", "...")`).
- Recipe helpers: `DatagenRecipeFactory` (`storageBlock`, `ingotPile`, etc.).
- After adding/modifying builder content, run `./gradlew runData` and commit generated files.

## Adding a Block

1. Create the block class in `block/decoration/`, `block/nature/`, or `block/agriculture/`.
2. Add a `SAPRegistries.block("id", BlockClass::new)` chain in `BlockRegistry`.
3. Use `.withItem()` if it has an item form.
4. Assign `.creativeTab(CreativeTabType.MAIN)` or `.NATURE`.
5. Add `.tags(...)` for mining/tool tags.
6. Add `.loot(...)` or rely on defaults.
7. Add `.recipe(...)` using `DatagenRecipeFactory` helpers where possible.
8. Add `.blockstate(...)` for model generation.
9. Add `.lang("zh_cn", "...")` for Chinese; English is auto-generated.
10. Run `./gradlew runData`.
11. Add manual model JSONs under `src/main/resources/assets/shadowsandpetals/models/block/<type>/` only if the blockstate references `.models().getExistingFile(...)`.

## Adding an Item

Add to `ItemRegistry` using `SAPRegistries.item("id")` chain: `.model(...)` / `.recipe(...)` / `.lang(...)` / `.creativeTab(...)` / `.register()`.

## Creative Tabs

Two tabs in `CreativeTabType`:
- `MAIN` — general blocks/items; icon `ItemRegistry.HAMMER`
- `NATURE` — nature blocks; icon `BlockRegistry.MAPLE_SET.sapling()`

Content is populated via `CreativeTabContentsRegistry` and sorted by `CreativeTabOrder` (nature-specific groups + `DEFAULT`).

## Legacy Compatibility

Migration from old `chinjufumod`:
- `CompatInfo` defines alias naming conventions for dyed and wood blocks. **Check here before adding aliases.**
- `BlockStateAliasRegistry` / `BlockEntityAliasRegistry` handle runtime conversion via `ChunkEvent.Load` subscribers.
- Builder methods `.alias(...)`, `.stateAliasProperties(...)` attach migration data.
- Note: legacy alias methods exist on builders but are not yet wired into Registry files — available for future migration use.

## Block Entities, Entities & Menus

- `IroriBlockEntity` — has renderer, menu (`IroriMenu`), and screen (`IroriScreen`).
- `VanityBlockEntity` — registered per wood type via `WoodBlockList`; has renderer.
- `ShishiOdoshiBlockEntity` / `ShishiOdoshiPipeBlockEntity` — have renderers; water capability via `SAPCapabilities`.
- `WindChimeBlockEntity`, `CopperTeapotBlockEntity` — additional renderers in `client/renderer/`.
- `SeatEntity` — invisible sit entity, non-summonable, 0.01×0.01, rendered with `NoopRenderer`.

## Client

- `ClientRenderEvents` — hooks particle providers, entity/block entity renderers, model events.
- `client/ct/` — connected texture system (see `client/ct/NOTICE.md` for licensing).
- `client/model/` — `WoodPostBlockStateModel` custom baked model + `BlockModelRegistry` loader registration.
- `client/particle/` — `FallingLeafParticle` with per-tree-type variants.
- `client/renderer/` — all block entity renderers.
- `client/screen/` — `IroriScreen`.
- `client/tooltip/` — Rockery tooltip rendering.
- Hammer arm pose added via `META-INF/enumextensions.json`.

## Access Transformers & Mixins

- `src/main/resources/META-INF/accesstransformer.cfg` has 5 entries. The `.add()` line in `build.gradle` is commented out because moddev auto-detects AT files declared in `neoforge.mods.toml`.
- `shadowsandpetals.mixins.json` config is registered but empty; the `mixin/` source package is also empty.

## Dependencies

- JEI: `compileOnly` for `jei-26.1.2-common-api:29.5.0.28` + `jei-26.1.2-neoforge-api:29.5.0.28`; `runtimeOnly` for `jei-26.1.2-neoforge:29.5.0.28`
- Jade: `implementation` via `maven.modrinth:jade:26.0.10+neoforge` — plugin + providers in `compat/jade/`

## Notes

- `generateModMetadata` expands `gradle.properties` into the TOML template and runs on IDE sync (`neoForge.ideSyncTask`).
- `org.jspecify.annotations.Nullable` is used for nullability.
- `.codegraph/` is gitignored and holds the codegraph index.
- No README, no `opencode.json`, no `.cursorrules`, no root `.github/` workflows.
- `.gitignore` excludes: `build/`, `run/`, `run2/`, `.idea/`, `.gradle/`, `docs/`, `.omo/`, `reference/`, `.codegraph/`.
