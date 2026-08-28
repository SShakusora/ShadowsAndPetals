package com.sshakusora.shadowsandpetals.block.nature;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.api.outline.BlockOutlineContext;
import com.sshakusora.shadowsandpetals.api.outline.BlockOutlineProvider;
import com.sshakusora.shadowsandpetals.api.outline.OutlineGeometry;
import com.sshakusora.shadowsandpetals.block.RockeryDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A decorative rockery block that assembles a W×H×D multi-block structure
 * on placement. Each part of the rockery knows its local coordinate within
 * the grid and renders the corresponding model.
 * <p>
 * Only part 0 (the block clicked by the player) drops loot; breaking any
 * part destroys the entire rockery.
 */
public class RockeryBlock extends Block implements BlockOutlineProvider, SimpleWaterloggedBlock {
    private static final String OBJ_COLLISION_KEY = ShadowsAndPetals.MOD_ID + ":collision";
    public static final MapCodec<RockeryBlock> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    ExtraCodecs.NON_NEGATIVE_INT.fieldOf("width").forGetter(b -> b.dimensions.width()),
                    ExtraCodecs.NON_NEGATIVE_INT.fieldOf("height").forGetter(b -> b.dimensions.height()),
                    ExtraCodecs.NON_NEGATIVE_INT.fieldOf("depth").forGetter(b -> b.dimensions.depth()),
                    propertiesCodec()
            ).apply(instance, RockeryBlock::new)
    );

    private static final int MAX_PARTS = 64;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, MAX_PARTS - 1);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private final RockeryDimensions dimensions;
    private volatile @Nullable VoxelShape[][] shapeCache;
    private volatile @Nullable OutlineGeometry[][] outlineCache;

    public RockeryBlock(int width, int height, int depth, BlockBehaviour.Properties properties) {
        this(new RockeryDimensions(width, height, depth), properties);
    }

    public RockeryBlock(RockeryDimensions dimensions, BlockBehaviour.Properties properties) {
        super(properties);
        this.dimensions = dimensions;
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, 0)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<RockeryBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection().getClockWise();
        BlockPos origin = ctx.getClickedPos();
        Level level = ctx.getLevel();

        for (int i = 1; i < dimensions.partCount(); i++) {
            BlockPos partPos = partWorldPos(origin, i, facing);
            if (!level.getBlockState(partPos).canBeReplaced(ctx)) {
                return null;
            }
        }

        return defaultBlockState().setValue(FACING, facing).setValue(PART, 0)
                .setValue(WATERLOGGED, level.getFluidState(origin).getType() == Fluids.WATER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        Direction facing = state.getValue(FACING);
        for (int i = 1; i < dimensions.partCount(); i++) {
            BlockPos partPos = partWorldPos(pos, i, facing);
            boolean waterlogged = level.getFluidState(partPos).getType() == Fluids.WATER;
            level.setBlock(partPos, defaultBlockState()
                    .setValue(FACING, facing)
                    .setValue(PART, i)
                    .setValue(WATERLOGGED, waterlogged), Block.UPDATE_ALL);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos root = findRoot(level, pos, state);
            if (!pos.equals(root)) {
                // Redirect to root so loot drops correctly
                level.destroyBlock(root, !player.isCreative() && player.hasCorrectToolForDrops(state, level, pos));
                // Destroy all other parts silently
                Direction facing = state.getValue(FACING);
                for (int i = 0; i < dimensions.partCount(); i++) {
                    BlockPos partPos = partWorldPos(root, i, facing);
                    if (!partPos.equals(root) && !partPos.equals(pos)) {
                        level.removeBlock(partPos, false);
                    }
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return state.getValue(PART) == 0 ? List.of(new ItemStack(Blocks.STONE, dimensions.partCount())) : List.of();
    }

    @Override
    protected BlockState updateShape(
            BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighborPos,
            BlockState neighborState, RandomSource random) {

        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        int part = state.getValue(PART);
        Direction facing = state.getValue(FACING);
        BlockPos root = findRoot(level, pos, state);

        // If the neighboring part is gone, self-destruct
        for (int i = 0; i < dimensions.partCount(); i++) {
            if (i == part) continue;
            BlockPos otherPos = partWorldPos(root, i, facing);
            if (otherPos.equals(neighborPos) && !neighborState.is(this)) {
                return Blocks.AIR.defaultBlockState();
            }
        }

        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return !state.getValue(WATERLOGGED);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        int part = state.getValue(PART);
        if (part == 0) {
            return true;
        }
        // Non-root parts survive as long as the root exists
        Direction facing = state.getValue(FACING);
        BlockPos root = pos.offset(dimensions.worldOffset(0, facing)
                .subtract(dimensions.worldOffset(part, facing)));
        return level.getBlockState(root).is(this);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapeFor(state);
    }

    @Override
    public @Nullable OutlineGeometry getOutline(BlockState state, BlockOutlineContext context) {
        int part = state.getValue(PART);
        if (part >= dimensions.partCount()) {
            return null;
        }

        OutlineGeometry[][] cache = outlineCache;
        if (cache == null) {
            cache = buildOutlineCache();
            outlineCache = cache;
        }
        return cache[state.getValue(FACING).ordinal()][part];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, WATERLOGGED);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    public RockeryDimensions dimensions() {
        return dimensions;
    }

    private BlockPos partWorldPos(BlockPos root, int part, Direction facing) {
        Vec3i offset = dimensions.worldOffset(part, facing);
        return root.offset(offset);
    }

    private BlockPos findRoot(BlockGetter level, BlockPos pos, BlockState state) {
        int part = state.getValue(PART);
        if (part == 0) {
            return pos;
        }
        Direction facing = state.getValue(FACING);
        Vec3i offset = dimensions.worldOffset(part, facing);
        return pos.subtract(offset);
    }

    private VoxelShape shapeFor(BlockState state) {
        int part = state.getValue(PART);
        if (part >= dimensions.partCount()) {
            return Shapes.empty();
        }

        VoxelShape[][] cache = shapeCache;
        if (cache == null) {
            cache = buildShapeCache();
            shapeCache = cache;
        }
        return cache[state.getValue(FACING).ordinal()][part];
    }

    private VoxelShape[][] buildShapeCache() {
        VoxelShape[][] shapes = new VoxelShape[Direction.values().length][MAX_PARTS];
        for (Direction direction : Direction.values()) {
            for (int part = 0; part < shapes[direction.ordinal()].length; part++) {
                shapes[direction.ordinal()][part] = part < dimensions.partCount() ? Shapes.block() : Shapes.empty();
            }
        }

        for (int part = 0; part < dimensions.partCount(); part++) {
            VoxelShape southShape = loadPartShape(part);
            shapes[Direction.SOUTH.ordinal()][part] = southShape;
            shapes[Direction.WEST.ordinal()][part] = rotateClockwise(southShape);
            shapes[Direction.NORTH.ordinal()][part] = rotateClockwise(shapes[Direction.WEST.ordinal()][part]);
            shapes[Direction.EAST.ordinal()][part] = rotateClockwise(shapes[Direction.NORTH.ordinal()][part]);
        }
        return shapes;
    }

    private OutlineGeometry[][] buildOutlineCache() {
        OutlineGeometry[][] geometries = new OutlineGeometry[Direction.values().length][MAX_PARTS];
        List<RockeryOutlineGeometry.ModelPart> modelParts = new ArrayList<>(dimensions.partCount());
        for (int part = 0; part < dimensions.partCount(); part++) {
            JsonObject model = loadPartModel(part);
            if (model == null) {
                continue;
            }

            Vec3i local = dimensions.localPos(part);
            modelParts.add(new RockeryOutlineGeometry.ModelPart(
                    model,
                    new Vec3(local.getX() * 16.0D, local.getY() * 16.0D, local.getZ() * 16.0D)
            ));
        }

        OutlineGeometry complete = RockeryOutlineGeometry.fromModels(modelParts);
        if (complete == null) {
            return geometries;
        }

        // The complete geometry is rooted at PART 0. Return it relative to
        // whichever PART was selected, then apply the block's horizontal
        // facing just as the model renderer does.
        for (int part = 0; part < dimensions.partCount(); part++) {
            Vec3i local = dimensions.localPos(part);
            OutlineGeometry southGeometry = RockeryOutlineGeometry.translate(
                    complete,
                    -local.getX() * 16.0D,
                    -local.getY() * 16.0D,
                    -local.getZ() * 16.0D
            );
            geometries[Direction.SOUTH.ordinal()][part] = southGeometry;
            geometries[Direction.WEST.ordinal()][part] = RockeryOutlineGeometry.rotateClockwise(southGeometry);
            geometries[Direction.NORTH.ordinal()][part] = RockeryOutlineGeometry.rotateClockwise(
                    geometries[Direction.WEST.ordinal()][part]
            );
            geometries[Direction.EAST.ordinal()][part] = RockeryOutlineGeometry.rotateClockwise(
                    geometries[Direction.NORTH.ordinal()][part]
            );
        }
        return geometries;
    }

    private VoxelShape loadPartShape(int part) {
        String resourcePath = "assets/" + ShadowsAndPetals.MOD_ID + "/models/" + dimensions.modelPath(part) + ".json";
        JsonObject model = loadPartModel(resourcePath);
        return model == null ? Shapes.block() : shapeFromModel(model);
    }

    private @Nullable JsonObject loadPartModel(int part) {
        return loadPartModel("assets/" + ShadowsAndPetals.MOD_ID + "/models/"
                + dimensions.modelPath(part) + ".json");
    }

    private @Nullable JsonObject loadPartModel(String resourcePath) {
        ClassLoader classLoader = RockeryBlock.class.getClassLoader();
        try (InputStream stream = classLoader.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return null;
            }

            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static VoxelShape shapeFromModel(JsonObject model) {
        if (model.has(OBJ_COLLISION_KEY)) {
            return shapeFromCollisionMetadata(model.get(OBJ_COLLISION_KEY));
        }
        if (!model.has("elements") || !model.get("elements").isJsonArray()) {
            return Shapes.block();
        }

        VoxelShape shape = Shapes.empty();
        for (JsonElement element : model.getAsJsonArray("elements")) {
            if (!element.isJsonObject()) {
                continue;
            }

            Bounds bounds = Bounds.fromElement(element.getAsJsonObject());
            if (bounds == null) {
                continue;
            }
            bounds = bounds.rotated(element.getAsJsonObject()).clamped();
            if (bounds.isEmpty()) {
                continue;
            }
            shape = Shapes.or(shape, box(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ()));
        }
        return shape.isEmpty() ? Shapes.block() : shape.optimize();
    }

    private static VoxelShape shapeFromCollisionMetadata(JsonElement metadata) {
        if (!metadata.isJsonArray()) {
            return Shapes.block();
        }

        VoxelShape shape = Shapes.empty();
        for (JsonElement element : metadata.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject box = element.getAsJsonObject();
            if (!box.has("from") || !box.has("to")
                    || !box.get("from").isJsonArray() || !box.get("to").isJsonArray()) {
                continue;
            }
            JsonArray from = box.getAsJsonArray("from");
            JsonArray to = box.getAsJsonArray("to");
            if (from.size() < 3 || to.size() < 3) {
                continue;
            }

            double minX = Math.clamp(from.get(0).getAsDouble(), 0.0D, 16.0D);
            double minY = Math.clamp(from.get(1).getAsDouble(), 0.0D, 16.0D);
            double minZ = Math.clamp(from.get(2).getAsDouble(), 0.0D, 16.0D);
            double maxX = Math.clamp(to.get(0).getAsDouble(), 0.0D, 16.0D);
            double maxY = Math.clamp(to.get(1).getAsDouble(), 0.0D, 16.0D);
            double maxZ = Math.clamp(to.get(2).getAsDouble(), 0.0D, 16.0D);
            if (maxX - minX <= 1.0E-6D || maxY - minY <= 1.0E-6D || maxZ - minZ <= 1.0E-6D) {
                continue;
            }
            shape = Shapes.or(shape, box(minX, minY, minZ, maxX, maxY, maxZ));
        }
        return shape.isEmpty() ? Shapes.empty() : shape.optimize();
    }

    private static VoxelShape rotateClockwise(VoxelShape shape) {
        VoxelShape[] rotated = new VoxelShape[]{Shapes.empty()};
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> rotated[0] = Shapes.or(
                rotated[0],
                Shapes.box(1.0D - maxZ, minY, minX, 1.0D - minZ, maxY, maxX)
        ));
        return rotated[0].optimize();
    }

    private record Bounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        private static @Nullable Bounds fromElement(JsonObject element) {
            if (!element.has("from") || !element.has("to")) {
                return null;
            }

            JsonArray from = element.getAsJsonArray("from");
            JsonArray to = element.getAsJsonArray("to");
            return new Bounds(
                    from.get(0).getAsDouble(),
                    from.get(1).getAsDouble(),
                    from.get(2).getAsDouble(),
                    to.get(0).getAsDouble(),
                    to.get(1).getAsDouble(),
                    to.get(2).getAsDouble()
            );
        }

        private Bounds rotated(JsonObject element) {
            if (!element.has("rotation") || !element.get("rotation").isJsonObject()) {
                return this;
            }

            JsonObject rotation = element.getAsJsonObject("rotation");
            if (!rotation.has("origin") || !rotation.has("axis") || !rotation.has("angle")) {
                return this;
            }

            JsonArray origin = rotation.getAsJsonArray("origin");
            double originX = origin.get(0).getAsDouble();
            double originY = origin.get(1).getAsDouble();
            double originZ = origin.get(2).getAsDouble();
            String axis = rotation.get("axis").getAsString();
            double radians = Math.toRadians(rotation.get("angle").getAsDouble());
            double sin = Math.sin(radians);
            double cos = Math.cos(radians);

            BoundsBuilder builder = new BoundsBuilder();
            for (double x : new double[]{minX, maxX}) {
                for (double y : new double[]{minY, maxY}) {
                    for (double z : new double[]{minZ, maxZ}) {
                        builder.include(rotatePoint(x, y, z, originX, originY, originZ, axis, sin, cos));
                    }
                }
            }
            return builder.build();
        }

        private Bounds clamped() {
            return new Bounds(
                    clamp(minX),
                    clamp(minY),
                    clamp(minZ),
                    clamp(maxX),
                    clamp(maxY),
                    clamp(maxZ)
            );
        }

        private boolean isEmpty() {
            return maxX - minX <= 1.0E-6D || maxY - minY <= 1.0E-6D || maxZ - minZ <= 1.0E-6D;
        }

        private static double[] rotatePoint(double x, double y, double z,
                                            double originX, double originY, double originZ,
                                            String axis, double sin, double cos) {
            double dx = x - originX;
            double dy = y - originY;
            double dz = z - originZ;
            return switch (axis) {
                case "x" -> new double[]{originX + dx, originY + dy * cos - dz * sin, originZ + dy * sin + dz * cos};
                case "y" -> new double[]{originX + dx * cos + dz * sin, originY + dy, originZ - dx * sin + dz * cos};
                case "z" -> new double[]{originX + dx * cos - dy * sin, originY + dx * sin + dy * cos, originZ + dz};
                default -> new double[]{x, y, z};
            };
        }

        private static double clamp(double value) {
            return Math.clamp(value, 0.0D, 16.0D);
        }
    }

    private static final class BoundsBuilder {
        private double minX = Double.POSITIVE_INFINITY;
        private double minY = Double.POSITIVE_INFINITY;
        private double minZ = Double.POSITIVE_INFINITY;
        private double maxX = Double.NEGATIVE_INFINITY;
        private double maxY = Double.NEGATIVE_INFINITY;
        private double maxZ = Double.NEGATIVE_INFINITY;

        private void include(double[] point) {
            minX = Math.min(minX, point[0]);
            minY = Math.min(minY, point[1]);
            minZ = Math.min(minZ, point[2]);
            maxX = Math.max(maxX, point[0]);
            maxY = Math.max(maxY, point[1]);
            maxZ = Math.max(maxZ, point[2]);
        }

        private Bounds build() {
            return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }
}
