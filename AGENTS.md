# ShadowsAndPetals - Agent Notes

Minecraft NeoForge mod (MC 26.1.2 / NeoForge 26.1.2.48-beta). Java 25. Gradle with `net.neoforged.moddev` plugin (2.0.141).
Root package: `com.sshakusora.shadowsandpetals`.

**Version gotcha**: MC 26.x renamed `ResourceLocation` → `net.minecraft.resources.Identifier`. This codebase uses `Identifier` everywhere (`ShadowsAndPetals.asResource(...)` returns one). Do not import `ResourceLocation`.

## Build & Run

```bash
./gradlew build          # Build the mod JAR
./gradlew runClient      # Launch main client instance (auto-installs shaderpacks first)
./gradlew runClient2     # Launch second client (shared `run` dir, username SShakusora)
./gradlew runServer      # Launch server instance
./gradlew runData        # Run data generators -> src/generated/resources/
./gradlew runStructureEditor # Launch the structure-editor void world
./gradlew runGameTestServer  # Runs gametests then exits (will crash - none exist)
./gradlew installShaderpacks # Only copy shader packs into run/shaderpacks
```

- Data generation outputs to `src/generated/resources/`, included in the `main` source set. Commit generated files.
- JUnit 5 tests exist in `src/test/java/` (run with `./gradlew test`); no gametest files.
- `runClient` / `runClient2` / `runStructureEditor` depend on `installClientShaderpacks`, which copies 5 dev shader packs (Complementary Reimagined/Unbound, BSL, Photon, Makeup) from the `shaderpacks` configuration into `run/shaderpacks`. Sodium + Iris are `runtimeOnly` deps, so shaders are active in every client run.
- `runStructureEditor` also depends on `prepareStructureEditorWorld` (`dev/StructureEditorWorldBootstrap`), which creates `run/saves/sap_structure_editor`, junctions its `generated/shadowsandpetals/structure/` dir to `src/main/resources/data/shadowsandpetals/structure/`, and requests a layout rebuild on each run.

## Architecture

- **Entrypoint**: `ShadowsAndPetals.java` — `@Mod(MOD_ID)` class. Constructor order: `SAPRegistries.register(modEventBus)` → `CustomEventBootstrap.register(modEventBus)` → `SandExcavationDataMaps.register(modEventBus)` → `FluidRegistry.init()`, `AttachmentRegistry.init()`, `ItemRegistry.init()`, `BlockRegistry.init()`, `BlockEntityRegistry.init()`, `MenuRegistry.init()`, `EntityRegistry.init()`, `ParticleRegistry.init()`, `SoundRegistry.init()`, `RecipeSerializerRegistry.init()`, `CreativeTabRegistry.init()`, `TriggerRegistry.init()`, `AdvancementRegistry.init()`, `SAPFeatures.init()`.
- **Registries**: `SAPRegistries` holds 15 `DeferredRegister` instances: `BLOCKS`, `ITEMS`, `FLUID_TYPES`, `FLUIDS`, `CREATIVE_TABS`, `ENTITIES`, `BLOCK_ENTITIES`, `PARTICLES`, `FEATURES`, `SOUNDS`, `MENUS`, `RECIPE_SERIALIZERS`, `RECIPE_TYPES`, `ATTACHMENT_TYPES`, `TRIGGER_TYPES`. Use the fluent builder factory methods (`SAPRegistries.block(...)`, `.item(...)`, `.fluid(...)`, `.recipe(...)`, `.advancement(...)`, `.trigger(...)`, etc.) instead of touching `DeferredRegister` directly.
- **`init()` methods are empty** — side effects happen during static initialization (builder chains at field declaration time). Registration order in the entrypoint matters only for cross-registry static references.

## Registration Conventions

All content is registered through fluent builders in `registries/builder/`:

- `RegBlockBuilder` — blocks, block items, datagen wiring, connected textures, legacy aliases, tooltip hooks
- `RegItemBuilder` — items, models, recipes, lang; nested `BlockItemBuilder` for standalone block items
- `RegBlockEntityBuilder` — block entities (+ `SAPRegistries.blockEntityAlias(...)` for legacy BE aliases)
- `RegFluidBuilder` — fluids (FluidType + Fluid + flowing/block forms)
- `RegRecipeBuilder` — recipe serializer + recipe type pairs
- `RegCreativeTabBuilder`, `RegEntityBuilder`/`MobBuilder`, `RegParticleBuilder`, `RegSoundBuilder`
- `RegAdvancementBuilder` (advancements), `RegCriterionTriggerBuilder` (custom criterion triggers)

Canonical block chain (order is flexible; see `BlockRegistry.java`):
`.block(...)` → `.properties(...)` → `.tags(...)` → `.withItem()` → `.creativeTab(...)` → `.blockstate(...)` → `.loot(...)` → `.recipe(...)` → `.lang(...)` → `.register()`

Builder details worth knowing:
- `SAPRegistries.block(name)` (no factory arg) defaults the blockstate to `StandardBlockModels::cubeAll`.
- `.connectedTexture(...)` — connected textures (types in `CTTextureType`: `OMNIDIRECTIONAL`, `HORIZONTAL`, `VERTICAL`)
- `.clientItem(modelId)` / `.customClientItem(...)` — custom item model mappings for the client item model provider
- `.tooltipDescription()` / `.tooltipModifier()` / `.tooltipComponent(...)` — tooltip pipeline wiring
- `.alias(oldPath)` / `.alias(namespace, oldPath)` / `.stateAliasProperties(...)` — legacy migration (see Legacy Compatibility)

Multi-variant helpers:
- `DyedBlockList<T>` — 16 dye colors
- `WoodBlockList<T>` (in `block/`) — 15 wood types: 9 vanilla (oak→pale_oak) + 3 custom `WoodSetList` (sakura, maple, ginkgo) + bamboo + crimson + warped
- `WoodSetList` — groups the 18 custom-wood blocks (log, stripped log, wood, stripped wood, planks, post, stripped post, wood post, stripped wood post, slab, stairs, fence, fence gate, pressure plate, button, sapling, leaves, hedge) per custom wood set

## Data Generation

- `ModDataGenerator.gatherData()` wires providers via `@EventBusSubscriber` on `GatherDataEvent.Client`: models, rockery models, connected-texture bleed, en_us + zh_cn lang, advancements, recipes, block loot, block tags, item tags, data maps, sound definitions, worldgen.
- **Content flows from builders only** — `.blockstate()`, `.recipe()`, `.loot()`, `.lang()` on builders auto-wire to datagen registries (`DatagenBlockLootRegistry`, `DatagenRecipeRegistry`, `DatagenLangRegistry`, `DatagenSoundRegistry`). Do not edit provider classes directly.
- Block tags live in `BlockTagRegistry`; use `.tags(...)` on blocks and `BlockTagRegistry.include(...)` for nested tag references.
- Languages: default English (auto-generated from ids) + `zh_cn` (manual via `.lang("zh_cn", "...")`).
- Recipe helpers: `DatagenRecipeFactory` (`storageBlock`, `ingotPile`, etc.).
- After adding/modifying builder content, run `./gradlew runData` and commit generated files.

## Adding a Block

1. Create the block class in `block/decoration/`, `block/nature/`, or `block/agriculture/`.
2. Add a `SAPRegistries.block("id", BlockClass::new)` chain in `BlockRegistry`.
3. Use `.withItem()` if it has an item form.
4. Assign `.creativeTab(CreativeTabKey.MAIN)` / `.NATURE` / `.AGRICULTURE`.
5. Add `.tags(...)` for mining/tool tags.
6. Add `.loot(...)` or rely on defaults.
7. Add `.recipe(...)` using `DatagenRecipeFactory` helpers where possible.
8. Add `.blockstate(...)` for model generation (skip for simple cube-all blocks — use the `block(name)` overload).
9. Add `.lang("zh_cn", "...")` for Chinese; English is auto-generated.
10. Run `./gradlew runData`.
11. Add manual model JSONs under `src/main/resources/assets/shadowsandpetals/models/block/<type>/` only if the blockstate references `.models().getExistingFile(...)`.

## Adding an Item

Add to `ItemRegistry` using `SAPRegistries.item("id")` chain: `.model(...)` / `.recipe(...)` / `.lang(...)` / `.creativeTab(...)` / `.register()`.

## Creative Tabs

Three tabs, keyed by the `CreativeTabKey` enum (`MAIN` / `NATURE` / `AGRICULTURE`). The actual `CreativeModeTab` holders — display names (en + zh_cn) and icons — are declared in `CreativeTabRegistry`. Content is populated via `CreativeTabContentsRegistry` and sorted by `CreativeTabOrder`. Blocks/items opt in with `.creativeTab(CreativeTabKey.X)`.

## Advancements

Declared in `AdvancementRegistry` via `SAPRegistries.advancement("...")` (see `RegAdvancementBuilder`). Custom criterion triggers are registered in `TriggerRegistry` via `SAPRegistries.trigger(...)` (e.g. `ShishiOdoshiFluidPouredTrigger`). Generated by `ModAdvancementProvider` during `runData`.

## Legacy Compatibility

Migration from old `chinjufumod`, split across two packages:
- `compat/CompatInfo` — alias naming conventions for dyed and wood blocks (`getDyedBlockAlias`, `getWoodBlockAlias1/2`, `ingotPileStateAlias`). **Check here before adding aliases.**
- `legacy/` — runtime migration machinery:
  - `LegacyCompatIds` — legacy content is registered under hidden hashed ids (`lcb_` / `lcbe_` prefixes), excluded from suggestions.
  - `LegacyStateBlock` / `LegacyBlockEntity` — placeholder compat blocks/BEs that only exist to be replaced.
  - `BlockStateAliasRegistry` — rules added via `add(legacyBlock, targetState, converter)`; a `ChunkEvent.Load` subscriber (server-side) scans loaded chunks and swaps legacy states for targets.
  - `BlockEntityAliasRegistry` — same idea for block entities.
- Builder methods `.alias(...)` / `.stateAliasProperties(...)` attach migration data, but are **not yet wired into Registry files** — available for future migration use.

## Worldgen

- `worldgen/SAPFeatures` — feature registration (called from entrypoint).
- `worldgen/SAPConfiguredFeatures` / `SAPPlacedFeatures` / `SAPBiomeModifiers` / `SAPTreeGrowers` — configured/placed feature keys, biome modifier JSON datagen, tree growers for custom saplings.
- Generated by `WorldGenProvider` during `runData`.

## Extension Points (API)

Public behavior-API for other mods/scripts lives in `api/`:
- `api/irori/` — `RegisterIroriBehaviorsEvent`, `IroriCookingProvider`, `IroriFuelRule`, `IroriGrillRule`, `IroriIgnitionBehavior`, etc.
- `api/shishiOdoshi/` — `RegisterShishiOdoshiFluidsEvent`, `ShishiOdoshiFluidRegistry`.
- `api/excavation/` — `SandExcavationDataMaps` (data-map driven excavation drops; registered in the entrypoint via `SandExcavationDataMaps.register(...)`).
- `api/outline/` — `BlockOutlineProvider` / `OutlineGeometry` (custom block outlines; rendered by `client/outline/`).

Defaults are wired in `registries/event/` (`IroriBehaviorRegistry`, `ShishiOdoshiFluidBehaviorRegistry`) and fired by `CustomEventBootstrap` during mod construction. Add new default behaviors there, not in block classes.

## Block Entities, Entities & Menus

- `IroriBlockEntity` — has renderer, menu (`IroriMenu`), and screen (`IroriScreen`).
- `VanityBlockEntity` — registered per wood type via `WoodBlockList`; has renderer.
- `ShishiOdoshiBlockEntity` / `ShishiOdoshiPipeBlockEntity` — have renderers; fluid capabilities registered in `registries/CapabilityRegistry` (`RegisterCapabilitiesEvent`).
- `WindChimeBlockEntity`, `CopperTeapotBlockEntity`, `RecessedLampBlockEntity`, `SandExcavationBlockEntity` — additional renderers/behaviour (see `blockentity/`).
- `SeatEntity` — invisible sit entity, non-summonable, 0.01×0.01, rendered with `NoopRenderer`.
- `event/` (top-level) — game-event handlers: `IroriPhantomRepellent`, `IroriSurfacePlacementEvents`.

## Client

- `client/ClientRenderEvents` — hooks particle providers, entity/block entity renderers, model events.
- `client/ct/` — connected texture system (see `client/ct/NOTICE.md` for licensing).
- `client/model/` — `WoodPostBlockStateModel` custom baked model + loader registration (`model/registry/`, `model/builder/`).
- `client/animation/` — animation + use-animation system (`SAPAnimations`, `UseAnimationPlayer`, `RegUseAnimationBuilder`).
- `client/outline/` — custom block outlines (`BlockOutlineRegistry`, `BlockOutlineRenderer`), backed by `api/outline`.
- `client/effect/`, `client/particle/` (`FallingLeafParticle` per-tree variants), `client/renderer/`, `client/screen/` (`IroriScreen`), `client/tooltip/` (Rockery tooltip rendering).
- Top-level `tooltip/` package holds the shared tooltip pipeline (`TooltipComponentRegistry`, `TooltipModifier`).
- Extra `ItemUseAnimation`/`ArmPose` enums (hammer `SHADOWSANDPETALS_HAMMER_AND_CHISEL`, harrow `SHADOWSANDPETALS_HARROW_DIGGING`) added via `META-INF/enumextensions.json` (declared in `neoforge.mods.toml` template).

## Access Transformers & Mixins

- `src/main/resources/META-INF/accesstransformer.cfg` has 7 entries. The `accessTransformers.add(...)` line in `build.gradle` is commented out because moddev auto-detects AT files declared in `neoforge.mods.toml`.
- `shadowsandpetals.mixins.json` currently declares **no mixins** (`mixins` and `client` arrays are empty) but keeps `overwrites.requireAnnotations=true`. The former `LocalPlayerMixin` / `RecessedLampTargeting` raycast correction was removed.

## Dependencies

- JEI: `compileOnly` for `jei-26.1.2-common-api:29.5.0.28` + `jei-26.1.2-neoforge-api:29.5.0.28`; `runtimeOnly` for `jei-26.1.2-neoforge:29.5.0.28`
- Jade: `implementation` via `maven.modrinth:jade:26.0.10+neoforge` — plugin + providers in `compat/jade/`
- Serene Seasons: `implementation` via `com.github.glitchfiend:SereneSeasons-neoforge:26.1.2-26.1.2.0.4` — compat in `compat/sereneseasons/`
- Shader dev runtime: `runtimeOnly` Sodium (`mc26.1.2-0.9.1-neoforge`) + Iris (`1.11.2+26.1-neoforge`); 5 shader packs via the custom `shaderpacks` configuration (see Build & Run)
- Tests: JUnit 5 (`testImplementation`), run with `./gradlew test`

## Notes

- `generateModMetadata` expands `gradle.properties` into `src/main/templates/META-INF/neoforge.mods.toml` and runs on IDE sync (`neoForge.ideSyncTask`).
- `org.jspecify.annotations.Nullable` is used for nullability.
- No README, no `opencode.json`, no `.cursorrules`. CI: `.github/workflows/build-latest-jar.yml` builds on push to the `26.1.2/Teacon` branch and publishes `ShadowsAndPetals-latest.jar` to a GitHub `latest-build` release.
- `.gitignore` excludes: `build/`, `run*/`, `logs/`, `.idea/`, `.gradle/`, `docs/`, `.omo/`, `reference/` (note: `.codegraph/` is **not** gitignored).
