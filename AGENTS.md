# ShadowsAndPetals - Agent Notes

Minecraft NeoForge mod (26.1.2 / NeoForge 26.1.2.0-beta). Java 25. Gradle with `net.neoforged.moddev` plugin.
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

## Architecture

- **Entrypoint**: `ShadowsAndPetals.java` — `@Mod("shadowsandpetals")` class. Calls `SAPRegistries.register(modEventBus)` then each `*Registry.init()` and `SAPFeatures.init()`. Also provides `ShadowsAndPetals.asResource(String path)` convenience method.
- **Registration pattern**: All blocks/items/entities/tabs/particles/features use `DeferredRegister` via `SAPRegistries`, wrapped in fluent builder classes under `registries/builder/`:
  - `RegBlockBuilder` — blocks + block items + datagen wiring + legacy aliases
  - `RegItemBuilder` — items + model + recipe + lang
  - `RegBlockEntityBuilder` — block entities + legacy alias support
  - `RegCreativeTabBuilder` — creative tabs
  - `RegEntityBuilder` / `RegEntityBuilder.MobBuilder` — entities
  - `RegParticleBuilder` — particles
  - Canonical builder chain (see `BlockRegistry.java`): `.block(...)` / `.properties(...)` / `.tags(...)` / `.withItem()` / `.creativeTab(...)` / `.blockstate(...)` / `.loot(...)` / `.recipe(...)` / `.lang(...)` / `.register()`
- **`init()` methods** on registries are empty — side effects happen during static initialization (builder chains at field declaration time).
- **Multi-variant blocks**: `DyedBlockList<T>` (16 DyeColors) and `WoodBlockList<T>` (15 wood types — 9 vanilla + 3 custom + 3 nether) generate families from a single builder template lambda.
- **`WoodSetList`**: Groups plank/stair/slab/log blocks for custom wood types (sakura, maple, ginkgo). Used as supplier for `WoodBlockList.WoodType`. Each `WoodSetList.Type` carries sapling, leaves, log, stripped log, wood, stripped wood, planks, stairs, slab, fence, fence gate, door, trapdoor, button, pressure plate, sign, hanging sign, boat, post, and hedge.
- **`SAPRegistries`**: Houses all `DeferredRegister` instances (`BLOCKS`, `ITEMS`, `CREATIVE_TABS`, `ENTITIES`, `BLOCK_ENTITIES`, `PARTICLES`, `FEATURES`) plus factory methods for builder chains. Uses NeoForge's `DeferredRegister.createBlocks()`/`createItems()` shorthand.

## Worldgen

- `worldgen/` package: `SAPFeatures`, `SAPConfiguredFeatures`, `SAPPlacedFeatures`, `SAPBiomeModifiers`, `SAPTreeGrowers`
- Features registered via `SAPRegistries.FEATURES` DeferredRegister
- Generated worldgen JSON → `src/generated/resources/data/shadowsandpetals/worldgen/`

## Creative Tabs

Two tabs defined in `CreativeTabType` enum:
- `MAIN` ("Shadows & Petals" / "织影落花") — general blocks/items
- `NATURE` ("Shadows & Petals: Nature" / "织影落花：自然") — nature blocks
- Content population via `CreativeTabContentsRegistry`
- Tabs registered in `CreativeTabRegistry` with a `bind()` pattern that ties `DeferredHolder` back to the enum for later lookup

## Legacy Compatibility

Handles migration from the old `chinjufumod` mod:
- `CompatInfo` — alias naming conventions for dyed blocks and wood blocks. **Check here before adding legacy aliases.**
- `BlockStateAliasRegistry` / `BlockEntityAliasRegistry` — state/entity migration rules
- `LegacyStateBlock`, `LegacyBlockEntity`, `LegacyCompatIds` — runtime conversion support
- Builder methods: `.alias(oldPath)`, `.alias(namespace, oldPath)`, `.stateAliasProperties(...)` — append migration data
- Alias naming conventions differ between dyed blocks (e.g. `cafechair_white`) and wood blocks (e.g. `bin_sakura`)

## BlockEntities & Entities

- `IroriBlockEntity` — registered via `SAPRegistries.blockEntity("irori")`. Has a renderer (`IroriBlockEntityRenderer`).
- `VanityBlockEntity` — registered per-wood-type via `WoodBlockList` template. Has a renderer (`VanityBlockEntityRenderer`).
- `SeatEntity` — invisible entity for chair sitting. Registered via `SAPRegistries.entity("seat", MobCategory.MISC)` with tiny dimensions (0.01×0.01), non-summonable. Rendered with `NoopRenderer`.

## Client

- `BlockModelRegistry` — registers wood post block-state model loader (standalone model registration, baking modification, and caching)
- `CafeChairDyeHintHandler` — JEI/Jade dye color tooltip
- `ClientRenderEvents` — hooks particle providers, entity/block entity renderers, and model events
- `FallingLeafParticle` — procedural leaf particle with per-tree-type variants (Ginkgo, Maple, Sakura)

## Mixins

Two server-side mixins in `mixin/` package:
- `CeilingHangingSignBlockMixin`
- `LanternBlockMixin`

Config: `shadowsandpetals.mixins.json`, `compatibilityLevel = "JAVA_21"`, `requireAnnotations = true`.
The `client` array is empty — no client-side mixins.

## Access Transformers

`src/main/resources/META-INF/accesstransformer.cfg` has 3 active entries:
1. `DoublePlantBlock.preventDropFromBottomPart`
2. `TreeFeature.updateLeaves`
3. `TagValueInput.input`

The `.add()` line in `build.gradle` is commented out because moddev auto-detects AT files from `neoforge.mods.toml` (which declares `[[accessTransformers]]`).

## Data Generation

- `ModDataGenerator.gatherData()` wires 11 `addProvider` calls via `@EventBusSubscriber` (hooked to `GatherDataEvent.Client`)
- Builders self-register datagen: `.blockstate()`, `.recipe()`, `.loot()`, `.lang()` on builders auto-wire to datagen registries
- **Never touch provider classes directly** — content flows from builders only
- `DatagenRecipeFactory` provides common recipe helpers (`storageBlock`, `ingotPile`, etc.)
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
9. If the item model differs from the block model, use `.clientItem(modelId)` (vanity, cafe chairs, ingot piles, irori).
10. For dyed/wood variants, wrap in `DyedBlockList` or `WoodBlockList`.
11. Add `.lang("zh_cn", "...")` for Chinese name; English name auto-generated from block id.
12. Run `./gradlew runData` to regenerate assets and data.
13. Add manual model JSONs under `src/main/resources/assets/shadowsandpetals/models/block/<type>/` if the datagen blockstate references `.models().getExistingFile(...)`.

## Adding a New Item

Add to `ItemRegistry` using `SAPRegistries.item("id")` builder chain: `.model(...)` / `.recipe(...)` / `.lang(...)` / `.creativeTab(...)` / `.register()`.

## Key Files

| File | Purpose |
|------|---------|
| `build.gradle` | Build config, dependencies, run profiles |
| `gradle.properties` | Mod metadata, version pins |
| `src/main/templates/META-INF/neoforge.mods.toml` | Mod metadata template (property expansion) |
| `src/main/resources/shadowsandpetals.mixins.json` | Mixin config |
| `src/main/resources/META-INF/accesstransformer.cfg` | Access transformers |
| `src/main/resources/assets/shadowsandpetals/models/` | Manual model JSONs (cafe_chair, ingot_pile, irori, vanity) |
| `registries/SAPRegistries.java` | All DeferredRegisters + builder factory methods |
| `registries/BlockRegistry.java` | Canonical builder chain examples |
| `registries/ItemRegistry.java` | Item registration |
| `data/ModDataGenerator.java` | Data generation wiring |
| `compat/CompatInfo.java` | Legacy alias conventions |

## Dependencies

- JEI: `compileOnly` for `jei-26.1.2-common-api:29.5.0.28` + `jei-26.1.2-neoforge-api:29.5.0.28`; `runtimeOnly` for `jei-26.1.2-neoforge:29.5.0.28`
- Jade: `implementation` via `maven.modrinth:jade:26.0.10+neoforge` — includes `ShadowsAndPetalsJadePlugin`

## Notes

- `generateModMetadata` task expands `gradle.properties` into the TOML template. Wired to run on IDE sync (`neoForge.ideSyncTask`).
- No gametest files exist; `runGameTestServer` will crash unless gametests are added.
- No `README` exists; this file is the primary agent reference.
- `.gitignore` excludes: `build/`, `run/`, `run2/`, `.idea/`, `.gradle/`, `docs/`, `.omo/`, `reference/`.
- `org.jspecify.annotations.Nullable` is used (e.g. `CreativeTabType`) for nullability annotations.
