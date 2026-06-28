# ShadowsAndPetals - Agent Notes

Minecraft NeoForge mod (26.1.2 / NeoForge 26.1.2.0-beta). Java 25. Gradle with `net.neoforged.moddev` plugin (2.0.141).
Root package: `com.sshakusora.shadowsandpetals`.

## Build & Run

```bash
./gradlew build          # Build the mod JAR
./gradlew runClient      # Launch main client instance
./gradlew runClient2     # Launch second client (separate run2 dir, username Dev2)
./gradlew runServer      # Launch server instance
./gradlew runData        # Run data generators -> src/generated/resources/
./gradlew runGameTestServer  # Run gametests then exit (will crash - no gametest files exist)
```

Data generation outputs to `src/generated/resources/`, which is included in the `main` source set (`build.gradle:104`). Commit generated files.
No tests exist — no `src/test/` directory, no gametest files.

## Architecture

- **Entrypoint**: `ShadowsAndPetals.java` — `@Mod(ShadowsAndPetals.MOD_ID)` class. Calls `SAPRegistries.register(modEventBus)` then 8 `*Registry.init()` + `SAPFeatures.init()` in order: `ItemRegistry`, `BlockRegistry`, `BlockEntityRegistry`, `EntityRegistry`, `ParticleRegistry`, `SoundRegistry`, `RecipeSerializerRegistry`, `CreativeTabRegistry`, then `SAPFeatures.init()`. Provides `ShadowsAndPetals.asResource(String path)` convenience method.
- **Registration pattern**: All blocks/items/entities/tabs/particles/features/sounds use `DeferredRegister` via `SAPRegistries`, wrapped in fluent builder classes under `registries/builder/`:
  - `RegBlockBuilder` — blocks + block items + datagen wiring + CT (connected textures) + legacy aliases
  - `RegItemBuilder` — items + model + recipe + lang. Also contains nested `BlockItemBuilder` for block-item-only registration.
  - `RegBlockEntityBuilder` — block entities + legacy alias support
  - `RegCreativeTabBuilder` — creative tabs
  - `RegEntityBuilder` / `RegEntityBuilder.MobBuilder` — entities
  - `RegParticleBuilder` — particles
  - `RegSoundBuilder` — sounds
  - Canonical builder chain order (see `BlockRegistry.java`): `.block(...)` / `.properties(...)` / `.tags(...)` / `.withItem()` / `.creativeTab(...)` / `.blockstate(...)` / `.loot(...)` / `.recipe(...)` / `.lang(...)` / `.register()`. Not all steps are required, and order is flexible — `.lang()` can appear before `.blockstate()`, and `.tags()` can appear after `.creativeTab()`. Some blocks append `.tooltipDescription(...)`, `.tooltipComponent(...)`, `.connectedTexture(...)`, or `.clientItem(modelId)`.
  - Additional builder methods: `.clientItem(modelId)` — custom item model; `.connectedTexture(baseTexture, connectedTexture, type)` — connected textures; `.alias(oldPath)` / `.alias(namespace, oldPath)` / `.stateAliasProperties(...)` — legacy migration.
- **`init()` methods** on registries are empty — side effects happen during static initialization (builder chains at field declaration time).
- **Multi-variant blocks**: `DyedBlockList<T>` (16 `DyeColor`s) and `WoodBlockList<T>` (15 wood types — 9 vanilla + 3 custom + 2 nether + bamboo) generate families from a single builder template lambda.
- **`WoodSetList`**: Groups plank/stair/slab/log blocks for custom wood types (sakura, maple, ginkgo). Used as supplier for `WoodBlockList.WoodType`. Each `WoodSetList.WoodSet` record carries 18 block fields: log, strippedLog, wood, strippedWood, planks, post, strippedPost, woodPost, strippedWoodPost, slab, stairs, fence, fenceGate, pressurePlate, button, sapling, leaves, hedge.
- **`SAPRegistries`**: Houses all `DeferredRegister` instances (`BLOCKS`, `ITEMS`, `CREATIVE_TABS`, `ENTITIES`, `BLOCK_ENTITIES`, `PARTICLES`, `FEATURES`, `SOUNDS`, `RECIPE_SERIALIZERS`) plus factory methods for builder chains. Uses NeoForge's `DeferredRegister.createBlocks()`/`createItems()` shorthand.
- **`BlockList<TEnum, TValue>`**: Generic array-backed lookup. `DyedBlockList`, `WoodBlockList`, and `WoodSetList` all extend it. Use `getByOrdinal()` for index-based access or named accessors.

## Package Overview

| Package | Contents |
|---------|----------|
| `block/decoration/` | Furniture blocks: CafeChair, CafeTable, DiningChair, ModularDesk, VanityBlock, IroriBlock, SamonBlock, IngotPileBlock, WoodPostBlock, HedgeBlock, RoofTileBlock, ShishiOdoshiBlock, ShishiOdoshiPipeBlock, lamps (BedroomLamp, DeskLamp, EmergencyLamp, WallLamp). Abstract bases: AbstractSeatBlock, AbstractConnectingTableBlock. |
| `block/nature/` | RockeryBlock, SAPLeavesBlock |
| `block/agriculture/` | Empty (placeholder for future crops) |
| `blockentity/` | IroriBlockEntity, VanityBlockEntity, ShishiOdoshiBlockEntity, ShishiOdoshiPipeBlockEntity. All 4 have renderers. |
| `client/ct/` | Connected texture system: CTRegistry, CTModelRegistry, CTBlockStateModel, CTContext, CTTextureType |
| `client/tooltip/` | Rockery tooltip: RockeryTooltipComponent, RockeryPreviewRenderer, RockeryPreviewState, ClientRockeryTooltip |
| `compat/` | CompatInfo (alias conventions), jade/ (ShadowsAndPetalsJadePlugin + 5 component/data providers) |
| `data/` | All datagen providers and registries. Model helpers in `data/model/`. |
| `entity/` | SeatEntity — invisible chair-sitting entity, non-summonable, 0.01×0.01 dimensions, NoopRenderer |
| `item/` | HammerItem, HarrowItem |
| `item/chime/` | WindChimeDyeRecipe, WindChimeColors, WindChimeTooltipModifier |
| `legacy/` | Chinjufumod migration: BlockStateAliasRegistry, BlockEntityAliasRegistry, LegacyStateBlock, LegacyBlockEntity, LegacyCompatIds |
| `mixin/` | Two server-side mixins only |
| `registries/builder/` | 7 fluent builder classes |
| `util/` | VoxelShapeUtils, WoolUtils |
| `worldgen/` | SAPFeatures (DeferredRegister), SAPConfiguredFeatures, SAPPlacedFeatures, SAPBiomeModifiers, SAPTreeGrowers (standalone), feature/PrefabTreeFeature, feature/config/PrefabTreeConfiguration |

## Worldgen

- `worldgen/feature/PrefabTreeFeature` — custom tree feature registered via `SAPRegistries.FEATURES` as `"prefab_tree"`. Places nbt templates from the structure manager; configurable rotation, mirroring, trunk base extension, and leaf distance update.
- `PrefabTreeConfiguration` — record with 5 fields: templates, allowRotation, allowMirror, trunkBaseExtensionMax, updateLeafDistance.
- `SAPConfiguredFeatures`, `SAPPlacedFeatures`, `SAPBiomeModifiers`, `SAPTreeGrowers` are standalone utility classes that define content via vanilla datapack registry methods, NOT through `DeferredRegister`.
- Generated worldgen JSON → `src/generated/resources/data/shadowsandpetals/worldgen/` (configured + placed features) and `src/generated/resources/data/shadowsandpetals/neoforge/biome_modifier/` (biome modifiers).
- Tree growers: `SAPTreeGrowers.SAKURA`, `.MAPLE`, `.GINKGO`, `.AUTUMN_OAK`. Trees are configured but not yet placed in biomes (only ore bauxite has placement/biome modifier chains).

## Creative Tabs

Two tabs defined in `CreativeTabType` enum:
- `MAIN` ("Shadows & Petals" / "织影落花") — general blocks/items. Icon: `ItemRegistry.HAMMER`.
- `NATURE` ("Shadows & Petals: Nature" / "织影落花：自然") — nature blocks. Icon: `BlockRegistry.MAPLE_SET.sapling()`.
- Content population via `CreativeTabContentsRegistry` (EnumMap-based, entries stored with `CreativeTabOrder` for sorting).
- `CreativeTabOrder` — enum with 22 constants: 21 nature-specific (NATURE_LOGS, NATURE_STRIPPED_LOGS, … NATURE_HEDGES) + DEFAULT. Controls item ordering within nature tab.
- Tabs registered in `CreativeTabRegistry` with a `bind()` pattern that ties `DeferredHolder` back to the enum for later lookup.

## Connected Textures (CT)

- Located in `client/ct/` package. Refer to `client/ct/NOTICE.md` for licensing of the CT algorithm.
- Builder method: `.connectedTexture(baseTexture, connectedTexture, CTTextureType)` on `RegBlockBuilder` (NOT `.ct()` — that method name does not exist).
- CT texture types: `OMNIDIRECTIONAL` (8-direction), `HORIZONTAL`, `VERTICAL` (defined in `CTTextureType`).
- Auto-wires to `ModConnectedTextureBleedProvider` during datagen.

## Legacy Compatibility

Handles migration from the old `chinjufumod` mod:
- `CompatInfo` — alias naming conventions for dyed blocks and wood blocks. **Check here before adding legacy aliases.**
  - `getDyedBlockAlias(DyeColor, prefix)` → e.g. `cafechair_white`
  - `getWoodBlockAlias1(WoodType, prefix)` → e.g. `bin_sakura` (empty for oak)
  - `getWoodBlockAlias2(WoodType, prefix)` → short suffixes: `_s` (spruce), `_b` (birch), `_j` (jungle), `_a` (acacia), `_d` (dark_oak)
  - `ingotPileStateAlias(builder, legacyPath)` — specialized state migration for ingot piles
- `BlockStateAliasRegistry` / `BlockEntityAliasRegistry` — state/entity migration rules via `ChunkEvent.Load` subscribers
- `LegacyStateBlock`, `LegacyBlockEntity`, `LegacyCompatIds` — runtime conversion support
- Builder methods: `.alias(oldPath)`, `.alias(namespace, oldPath)`, `.stateAliasProperties(...)` — append migration data
- `SAPRegistries.blockEntityAlias(name)` — factory for block entity alias registration
- Note: legacy alias methods are defined on builders but not yet wired into any Registry file — available for future migration use.

## BlockEntities & Entities

- `IroriBlockEntity` — registered via `SAPRegistries.blockEntity("irori")`. Has a renderer (`IroriBlockEntityRenderer`).
- `VanityBlockEntity` — registered per-wood-type via `WoodBlockList` template. Has a renderer (`VanityBlockEntityRenderer`).
- `ShishiOdoshiBlockEntity` — registered via `SAPRegistries.blockEntity("shishi_odoshi")`. Implements `FluidHandler` capability for water. Has a renderer (`ShishiOdoshiBlockEntityRenderer`). Uses `SAPCapabilities` to register the fluid capability.
- `ShishiOdoshiPipeBlockEntity` — registered via `SAPRegistries.blockEntity("shishi_odoshi_pipe")`. Has a renderer (`ShishiOdoshiPipeBlockEntityRenderer`).
- `SeatEntity` — invisible entity for chair sitting. Registered via `SAPRegistries.entity("seat", MobCategory.MISC)` with tiny dimensions (0.01×0.01), non-summonable. Rendered with `NoopRenderer`.

## Items

Items registered in `ItemRegistry`:
- `HammerItem` — custom tool for rockery shaping. Has custom use animation via `HammerClientExtensions` and `HammerArmPoseEnumExtensions` (META-INF/enumextensions.json).
- `HarrowItem` — another custom tool item.
- `RAW_BAUXITE` / `ALUMINUM_INGOT` / `CHISEL` — standard items (registry entries, no separate class files).
- Wind chime items with dyeing recipe — see `item/chime/` (WindChimeDyeRecipe, WindChimeColors, WindChimeTooltipModifier).

## Recipe Serializers

- `RecipeSerializerRegistry` — registers `WIND_CHIME_DYEING` via `SAPRegistries.RECIPE_SERIALIZERS`. Single `DeferredHolder` with static init; no builder class.
- `WindChimeDyeRecipe` in `item/chime/` — custom recipe serializer for dyeing wind chimes (in-world crafting).

## Client

- `BlockModelRegistry` — registers wood post block-state model loader (standalone model registration, baking modification, and caching)
- `CafeChairDyeHintHandler` — JEI/Jade dye color tooltip
- `ClientRenderEvents` — hooks particle providers, entity/block entity renderers, and model events
- `FallingLeafParticle` — procedural leaf particle with per-tree-type variants (Ginkgo, Maple, Sakura, via `WoodSetList.Type.fallingLeafParticleSupplier`)
- `HammerClientExtensions` / `HammerArmPoseEnumExtensions` — custom arm pose for hammer animation
- `IroriBlockEntityRenderer` — irori block entity renderer
- `VanityBlockEntityRenderer` — vanity block entity renderer
- `ShishiOdoshiBlockEntityRenderer` / `ShishiOdoshiPipeBlockEntityRenderer` / `ShishiOdoshiFluidRenderInfo` — shishi-odoshi renderers with fluid rendering support
- `WoodPostBlockStateModel` — custom BakedModel for wood posts (14-pixel diameter cylinder)
- `tooltip/` — RockeryTooltipComponent, RockeryPreviewRenderer, RockeryPreviewState, ClientRockeryTooltip — custom tooltip rendering for rockery blocks

## Mixins

Two server-side mixins in `mixin/` package:
- `CeilingHangingSignBlockMixin`
- `LanternBlockMixin`

Both use `@ModifyReturnValue` to delegate `canSurvive` checks through `WoodPostBlock.canSupportHanging()`. Use MixinExtras (`com.llamalad7.mixinextras`).
Config: `shadowsandpetals.mixins.json`, `compatibilityLevel = "JAVA_21"`, `requireAnnotations = true`.
The `client` array is empty — no client-side mixins.

## Access Transformers

`src/main/resources/META-INF/accesstransformer.cfg` has 5 entries:
1. `DoublePlantBlock.preventDropFromBottomPart` — server-side
2. `TreeFeature.updateLeaves` — server-side
3. `TagValueInput.input` — server-side
4. `BlockModelRenderState.modelParts` — client-side
5. `BlockModelRenderState.renderType` — client-side

The `.add()` line in `build.gradle` is commented out because moddev auto-detects AT files from `neoforge.mods.toml` (which declares `[[accessTransformers]]`).

## Enum Extensions

`src/main/resources/META-INF/enumextensions.json` adds `SHADOWSANDPETALS_HAMMER_AND_CHISEL` to `HumanoidModel.ArmPose` for the hammer item animation.

## Data Generation

- `ModDataGenerator.gatherData()` wires 14 providers via `@EventBusSubscriber` (hooked to `GatherDataEvent.Client`):
  `ModBlockStateProvider`, `ModRockeryModelProvider`, `ModWoodModelProvider`, `ModItemModelProvider`, `ModClientItemProvider`, `ModConnectedTextureBleedProvider`, `ModLanguageProvider` (en_us + zh_cn), `ModRecipeProvider`, `LootTableProvider` (via `ModBlockLootProvider`), `ModBlockTagProvider`, `ModDataMapProvider`, `ModSoundDefinitionsProvider`, `WorldGenProvider`
- Builders self-register datagen: `.blockstate()`, `.recipe()`, `.loot()`, `.lang()` on builders auto-wire to datagen registries
- **Never touch provider classes directly** — content flows from builders only
- `DatagenRecipeFactory` provides common recipe helpers (`storageBlock`, `ingotPile`) with pack/unpack patterns
- Language: two `ModLanguageProvider` instances (default English and `zh_cn`). English names are auto-generated from block/item ids.

## Adding a New Block

1. Create the block class in `block/decoration/`, `block/nature/`, or `block/agriculture/`.
2. Add a `SAPRegistries.block("id", BlockClass::new)` builder chain in `BlockRegistry`.
3. Use `.withItem()` if the block has an item form.
4. Use `.creativeTab(CreativeTabType.MAIN)` or `.NATURE`.
5. Use `.tags(...)` for mining level / tool type tags.
6. Use `.loot(...)` or rely on default.
7. Use `.recipe(...)` to add a datagen recipe (use `DatagenRecipeFactory` helpers when possible).
8. Use `.blockstate(...)` for blockstate/model generation.
9. If the item model differs from the block model, use `.clientItem(modelId)`.
10. For dyed/wood variants, wrap in `DyedBlockList` or `WoodBlockList`.
11. Add `.lang("zh_cn", "...")` for Chinese name; English name auto-generated from block id.
12. If the block needs connected textures, add `.connectedTexture(baseTexture, connectedTexture, CTTextureType)`.
13. Run `./gradlew runData` to regenerate assets and data.
14. Add manual model JSONs under `src/main/resources/assets/shadowsandpetals/models/block/<type>/` if the datagen blockstate references `.models().getExistingFile(...)`.

## Adding a New Item

Add to `ItemRegistry` using `SAPRegistries.item("id")` builder chain: `.model(...)` / `.recipe(...)` / `.lang(...)` / `.creativeTab(...)` / `.register()`.

## Key Files

| File | Purpose |
|------|---------|
| `build.gradle` | Build config, dependencies, run profiles |
| `gradle.properties` | Mod metadata, version pins |
| `src/main/templates/META-INF/neoforge.mods.toml` | Mod metadata template (property expansion) |
| `src/main/resources/shadowsandpetals.mixins.json` | Mixin config |
| `src/main/resources/META-INF/accesstransformer.cfg` | Access transformers (5 entries) |
| `src/main/resources/META-INF/enumextensions.json` | Enum extension for hammer arm pose |
| `src/main/resources/assets/shadowsandpetals/models/` | Manual model JSONs (cafe_chair, ingot_pile, irori, vanity) |
| `registries/SAPRegistries.java` | All DeferredRegisters + builder factory methods |
| `registries/BlockRegistry.java` | Canonical builder chain examples |
| `registries/ItemRegistry.java` | Item registration |
| `registries/CreativeTabType.java` | Tab enum with icon suppliers |
| `registries/CreativeTabOrder.java` | Nature tab item ordering (22 constants) |
| `data/ModDataGenerator.java` | Data generation wiring |
| `data/DatagenRecipeFactory.java` | Common recipe helpers |
| `compat/CompatInfo.java` | Legacy alias conventions |
| `client/ct/NOTICE.md` | CT algorithm license notice |

## Dependencies

- JEI: `compileOnly` for `jei-26.1.2-common-api:29.5.0.28` + `jei-26.1.2-neoforge-api:29.5.0.28`; `runtimeOnly` for `jei-26.1.2-neoforge:29.5.0.28`
- Jade: `implementation` via `maven.modrinth:jade:26.0.10+neoforge` — includes `ShadowsAndPetalsJadePlugin` + 5 component/data providers in `compat/jade/`

## Notes

- `generateModMetadata` task expands `gradle.properties` into the TOML template. Wired to run on IDE sync (`neoForge.ideSyncTask`).
- `.codegraph/` contains the code intelligence knowledge graph index (gitignored). Use codegraph tools for fast structural queries.
- No gametest files exist; `runGameTestServer` will crash unless gametests are added.
- No test infrastructure at all — no `src/test/`, no unit tests, no gametests.
- No `README` exists; this file is the primary agent reference.
- No `opencode.json` or `.opencode/` directory exists.
- `.gitignore` excludes: `build/`, `run/`, `run2/`, `.idea/`, `.gradle/`, `docs/`, `.omo/`, `reference/`, `.codegraph/`.
- `org.jspecify.annotations.Nullable` is used (e.g. `CreativeTabType`) for nullability annotations.
- Two maven repos: `maven.blamejared.com` (JEI) and `api.modrinth.com` (Jade).
- The `block/agriculture/` package exists but is empty — only contains `package-info.java`.
- There is no `compat/jei/` directory — JEI integration (if planned) has not been implemented.
