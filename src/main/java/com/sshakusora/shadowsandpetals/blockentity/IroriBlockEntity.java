package com.sshakusora.shadowsandpetals.blockentity;

import com.sshakusora.shadowsandpetals.block.decoration.IroriBlock;
import com.sshakusora.shadowsandpetals.data.BuiltinLanguageKeys;
import com.sshakusora.shadowsandpetals.menu.IroriMenu;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import com.sshakusora.shadowsandpetals.registries.SAPBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class IroriBlockEntity extends BlockEntity implements Container, MenuProvider {
    private static final String MASTER_POS_KEY = "MasterPos";
    private static final String FIREWOOD_MODEL_KEY = "FirewoodModel";
    private static final String FUEL_STACK_KEY = "FuelStack";
    private static final String BURN_TIME_KEY = "BurnTime";
    private static final String BURN_TIME_TOTAL_KEY = "BurnTimeTotal";
    private static final String BURN_CYCLE_KEY = "BurnCycle";
    private static final String ASH_STATE_KEY = "AshState";
    private static final int CONTAINER_SIZE = 1;
    private static final int SMALL_MODEL_THRESHOLD = 32;
    private static final float FIREWOOD_APPEAR_ANIMATION_SPEED = 0.4F;
    private static final double FIREWOOD_EFFECT_Y = 12.0D / 16.0D;
    private static final double ASH_DROP_Y = 10.0D / 16.0D + 0.05D;
    private static final double FIREWOOD_FLAME_SPREAD = 3.0D / 16.0D;
    private static final double FIREWOOD_SMOKE_SPREAD = 4.0D / 16.0D;
    private static final int MIN_ASH_BONE_MEAL_DROPS = 1;
    private static final int MAX_ASH_BONE_MEAL_DROPS = 3;
    private static final FirewoodRenderOffset ZERO_RENDER_OFFSET = new FirewoodRenderOffset(0.0D, 0.0D);

    private @Nullable BlockPos masterPos;
    private @Nullable FirewoodModel firewoodModel;
    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private int burnTime;
    private int burnTimeTotal;
    private int burnCycle;
    private boolean ashState;
    private double cachedRenderOffsetX;
    private double cachedRenderOffsetZ;
    private int cachedComponentWidth = 1;
    private int cachedComponentDepth = 1;
    private boolean renderOffsetCached;
    private boolean firewoodAppearAnimationInitialized;
    private boolean hadVisibleFirewood;
    private boolean firewoodEffectModelInitialized;
    private @Nullable FirewoodModel previousFirewoodEffectModel;
    private float firewoodAppearProgress = 1.0F;
    private float firewoodAppearProgressOld = 1.0F;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            IroriBlockEntity master = getMaster();
            return switch (index) {
                case 0 -> master.burnTime;
                case 1 -> master.burnTimeTotal;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            IroriBlockEntity master = getMaster();
            switch (index) {
                case 0 -> master.burnTime = value;
                case 1 -> master.burnTimeTotal = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(BuiltinLanguageKeys.IRORI_CONTAINER_NAME.key());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        IroriBlockEntity master = resolveMaster();
        return new IroriMenu(id, playerInv, master, master.getDataAccess());
    }

    public IroriBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.IRORI.get(), pos, blockState);
    }

    public IroriBlockEntity getMaster() {
        if (masterPos == null || level == null) {
            return this;
        }

        BlockEntity blockEntity = level.getBlockEntity(masterPos);
        if (blockEntity instanceof IroriBlockEntity master && master.isValidMaster()) {
            return master;
        }
        return this;
    }

    public IroriBlockEntity resolveMaster() {
        IroriBlockEntity master = getMaster();
        if (master != this || masterPos == null) {
            return master;
        }

        masterPos = null;
        setChanged();
        invalidateRenderOffsetCache();
        return this;
    }

    public boolean isValidMaster() {
        return masterPos == null;
    }

    public void setMasterPos(@Nullable BlockPos pos) {
        if (Objects.equals(masterPos, pos)) {
            return;
        }

        masterPos = pos;
        invalidateRenderOffsetCache();
        setChanged();
    }

    public @Nullable BlockPos getMasterPos() {
        return masterPos;
    }

    public boolean canIgnite() {
        IroriBlockEntity master = getMaster();
        ItemStack fuelStack = master.getStoredFuelStack();
        return master.burnTime <= 0 && !fuelStack.isEmpty() && (master.level == null || getFuelBurnTime(fuelStack, master.level) > 0);
    }

    public @Nullable FirewoodModel getFirewoodModel() {
        return getMaster().firewoodModel;
    }

    public ItemStack getFuelStack() {
        return getMaster().getStoredFuelStack();
    }

    public int getBurnTime() {
        return getMaster().burnTime;
    }

    public int getBurnTimeTotal() {
        return getMaster().burnTimeTotal;
    }

    public int getBurnCycle() {
        return getMaster().burnCycle;
    }

    public boolean tryIgnite(Level level, RandomSource random) {
        IroriBlockEntity master = resolveMaster();
        if (master.burnTime > 0) {
            return false;
        }

        if (!master.startBurningFromFuel(level, random)) {
            return false;
        }

        master.setChanged();
        master.syncToClient();
        return true;
    }

    public void setFuelStack(ItemStack stack, RandomSource random) {
        IroriBlockEntity master = resolveMaster();
        ItemStack normalized = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        if (ItemStack.matches(master.getStoredFuelStack(), normalized)) {
            return;
        }

        master.setStoredFuelStack(normalized);
        if (!normalized.isEmpty()) {
            master.ashState = false;
        } else if (master.burnTime <= 0) {
            master.ashState = false;
        }
        master.updateFirewoodModelFromFuel(random);
        master.setChanged();
        master.syncToClient();
    }

    public void dropContentsAndReset() {
        if (level == null || level.isClientSide()) {
            return;
        }

        IroriBlockEntity master = resolveMaster();
        if (master.isEmpty() && master.firewoodModel == null) {
            return;
        }

        Containers.dropContents(level, master.getBlockPos(), master);
        master.resetStoredState();
        syncFirewoodLightState(level, collectConnectedComponent(level, master.getBlockPos()), master.getBlockPos(), false);
        master.setChanged();
        master.syncToClient();
    }

    public void dropContentsOnRemoval(BlockPos dropPos) {
        if (level == null || level.isClientSide()) {
            return;
        }

        IroriBlockEntity master = resolveMaster();
        if (master.isEmpty() && master.firewoodModel == null) {
            return;
        }

        Containers.dropContents(level, dropPos, master);
        if (master.burnTime > 0 || master.isAshModel()) {
            master.dropBoneMealAsh(dropPos);
        }
        master.resetStoredState();
        syncFirewoodLightState(level, collectConnectedComponent(level, master.getBlockPos()), master.getBlockPos(), false);
        master.setChanged();
        master.syncToClient();
    }

    public boolean clearAshAndDropBoneMeal() {
        if (level == null || level.isClientSide()) {
            return false;
        }

        IroriBlockEntity master = resolveMaster();
        if (!master.isAshModel()) {
            return false;
        }

        master.dropBoneMealAsh();
        master.ashState = false;
        master.firewoodModel = null;
        master.setChanged();
        master.syncToClient();
        return true;
    }

    public boolean shouldRenderFirewood() {
        return getMaster() == this && firewoodModel != null;
    }

    public boolean shouldDropBoneMealAsh() {
        return getMaster().isAshModel();
    }

    public float getFirewoodAppearProgress(float partialTick) {
        return firewoodAppearProgressOld + (firewoodAppearProgress - firewoodAppearProgressOld) * partialTick;
    }

    public FirewoodRenderOffset getFirewoodRenderOffset() {
        if (level == null) {
            return ZERO_RENDER_OFFSET;
        }
        if (renderOffsetCached) {
            return new FirewoodRenderOffset(cachedRenderOffsetX, cachedRenderOffsetZ);
        }

        RectangularComponent component = computeComponentBounds(level, getBlockPos());
        cachedComponentWidth = component.width();
        cachedComponentDepth = component.depth();
        cachedRenderOffsetX = cachedComponentWidth % 2 == 0 ? 0.5D : 0.0D;
        cachedRenderOffsetZ = cachedComponentDepth % 2 == 0 ? 0.5D : 0.0D;
        renderOffsetCached = true;
        return new FirewoodRenderOffset(cachedRenderOffsetX, cachedRenderOffsetZ);
    }

    public int getComponentSize() {
        getFirewoodRenderOffset();
        return cachedComponentWidth * cachedComponentDepth;
    }

    public @Nullable GrillLayoutInfo getGrillLayoutInfo() {
        if (level == null || getMaster() != this) {
            return null;
        }

        FirewoodRenderOffset renderOffset = getFirewoodRenderOffset();
        boolean widthEven = cachedComponentWidth % 2 == 0;
        boolean depthEven = cachedComponentDepth % 2 == 0;

        GrillModel model;
        if (widthEven && depthEven) {
            model = GrillModel.TWO_BY_TWO;
        } else if (widthEven || depthEven) {
            model = GrillModel.ONE_BY_TWO;
        } else {
            model = GrillModel.ONE_BY_ONE;
        }

        return new GrillLayoutInfo(
                model,
                renderOffset.x(),
                renderOffset.z(),
                widthEven && !depthEven,
                widthEven ? 2 : 1,
                depthEven ? 2 : 1
        );
    }

    public @Nullable GrillRenderInfo getGrillRenderInfo() {
        GrillLayoutInfo layout = getGrillLayoutInfo();
        Level level = this.level;
        if (layout == null || level == null) {
            return null;
        }

        for (int x = 0; x < layout.centerWidth(); x++) {
            for (int z = 0; z < layout.centerDepth(); z++) {
                if (level.getBlockState(worldPosition.offset(x, 1, z)).is(SAPBlockTags.SUPPORTS_IRORI_GRILL)) {
                    return new GrillRenderInfo(
                            layout.model(),
                            layout.offsetX(),
                            layout.offsetZ(),
                            layout.rotated()
                    );
                }
            }
        }
        return null;
    }

    public boolean isComponentWideAndDeep() {
        getFirewoodRenderOffset();
        return cachedComponentWidth > 1 && cachedComponentDepth > 1;
    }

    public static BlockPos electMaster(Set<BlockPos> positions) {
        if (positions.isEmpty()) {
            throw new IllegalArgumentException("Empty positions");
        }
        if (positions.size() == 1) {
            return positions.iterator().next();
        }

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int y = positions.iterator().next().getY();

        for (BlockPos pos : positions) {
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        return new BlockPos(Math.floorDiv(minX + maxX, 2), y, Math.floorDiv(minZ + maxZ, 2));
    }

    public static RectangularComponent computeComponentBounds(Level level, BlockPos origin) {
        Set<BlockPos> component = collectConnectedComponent(level, origin);
        return computeComponentBounds(component);
    }

    private static RectangularComponent computeComponentBounds(Set<BlockPos> component) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : component) {
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        return new RectangularComponent(minX, maxX, minZ, maxZ);
    }

    public static void reelectMaster(Level level, Set<BlockPos> component) {
        if (component.isEmpty()) {
            return;
        }

        BlockPos masterPos = electMaster(component);
        FirewoodState carriedState = resolveComponentFirewoodState(level, component, masterPos);

        for (BlockPos pos : component) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof IroriBlockEntity irori) {
                irori.invalidateRenderOffsetCache();
                irori.setMasterPos(pos.equals(masterPos) ? null : masterPos);
                if (!pos.equals(masterPos)) {
                    irori.items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
                    irori.firewoodModel = null;
                    irori.burnTime = 0;
                    irori.burnTimeTotal = 0;
                    irori.burnCycle = 0;
                    irori.ashState = false;
                }
            }
        }

        BlockEntity blockEntity = level.getBlockEntity(masterPos);
        if (blockEntity instanceof IroriBlockEntity master) {
            master.items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
            master.items.set(0, carriedState.fuelStack().copy());
            master.firewoodModel = carriedState.firewoodModel();
            master.burnTime = carriedState.burnTime();
            master.burnTimeTotal = carriedState.burnTimeTotal();
            master.burnCycle = carriedState.burnCycle();
            master.ashState = carriedState.ashState();
            master.invalidateRenderOffsetCache();
            master.setChanged();
        }

        syncFirewoodLightState(level, component, masterPos, carriedState.burnTime() > 0);
        syncComponentToClient(level, component);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, IroriBlockEntity blockEntity) {
        if (level.isClientSide()) {
            if (!blockEntity.isValidMaster()) {
                return;
            }
            blockEntity.tickFirewoodAppearAnimation();
            blockEntity.tickFirewoodModelChangeEffects(level, state);
            blockEntity.tickBurningEffects(level, state);
            return;
        }
        if (!blockEntity.isValidMaster() || !blockEntity.getBlockPos().equals(pos)) {
            return;
        }
        if (state.getValue(IroriBlock.WATERLOGGED)) {
            if (blockEntity.burnTime > 0) {
                blockEntity.burnTime = 0;
                blockEntity.burnTimeTotal = 0;
                blockEntity.ashState = false;
                blockEntity.updateFirewoodModelFromFuel(level.getRandom());
                syncFirewoodLightState(level, collectConnectedComponent(level, pos), pos, false);
                blockEntity.setChanged();
                blockEntity.syncToClient();
            }
            return;
        }
        if (blockEntity.burnTime <= 0) {
            return;
        }

        blockEntity.burnTime--;
        if (blockEntity.burnTime > 0) {
            return;
        }

        if (!blockEntity.startBurningFromFuel(level, level.getRandom())) {
            blockEntity.ashState = true;
            blockEntity.burnTimeTotal = 0;
            blockEntity.updateFirewoodModelFromFuel(level.getRandom());
            syncFirewoodLightState(level, collectConnectedComponent(level, pos), pos, false);
        }
        blockEntity.setChanged();
        blockEntity.syncToClient();
    }

    private void tickBurningEffects(Level level, BlockState state) {
        if (!isValidMaster() || burnTime <= 0 || firewoodModel == null || state.getValue(IroriBlock.WATERLOGGED)) {
            return;
        }

        RandomSource random = level.getRandom();
        FirewoodEffectOrigin origin = getFirewoodEffectOrigin();

        if (random.nextInt(16) == 0) {
            addFireParticle(level, random, origin.x(), origin.y(), origin.z(), origin.flameSpreadX(), origin.flameSpreadZ());
        }
        if (random.nextInt(90) == 0) {
            addFireParticle(level, random, origin.x(), origin.y() + 0.03D, origin.z(), origin.flameSpreadX(), origin.flameSpreadZ());
        }
        if (random.nextInt(12) == 0) {
            level.addParticle(
                    ParticleTypes.SMOKE,
                    offset(random, origin.x(), origin.smokeSpreadX()),
                    origin.y() + 0.18D + random.nextDouble() * 0.08D,
                    offset(random, origin.z(), origin.smokeSpreadZ()),
                    random.nextGaussian() * 0.004D,
                    0.025D + random.nextDouble() * 0.015D,
                    random.nextGaussian() * 0.004D
            );
        }
        if (random.nextInt(45) == 0) {
            level.addParticle(
                    ParticleTypes.ASH,
                    offset(random, origin.x(), origin.smokeSpreadX()),
                    origin.y() + 0.12D,
                    offset(random, origin.z(), origin.smokeSpreadZ()),
                    random.nextGaussian() * 0.01D,
                    0.01D + random.nextDouble() * 0.015D,
                    random.nextGaussian() * 0.01D
            );
        }
        if (random.nextInt(150) == 0) {
            level.addParticle(
                    ParticleTypes.LAVA,
                    offset(random, origin.x(), origin.flameSpreadX() * 0.65D),
                    origin.y() + 0.08D,
                    offset(random, origin.z(), origin.flameSpreadZ() * 0.65D),
                    0.0D,
                    0.0D,
                    0.0D
                );
        }
        if (random.nextInt(90) == 0) {
            level.playLocalSound(origin.x(), origin.y(), origin.z(), SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.18F, 0.75F + random.nextFloat() * 0.25F, false);
        } else if (random.nextInt(180) == 0) {
            level.playLocalSound(origin.x(), origin.y(), origin.z(), SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.08F, 1.8F + random.nextFloat() * 0.25F, false);
        }
    }

    private void tickFirewoodModelChangeEffects(Level level, BlockState state) {
        if (!isValidMaster() || firewoodModel == null || state.getValue(IroriBlock.WATERLOGGED)) {
            firewoodEffectModelInitialized = false;
            previousFirewoodEffectModel = firewoodModel;
            return;
        }

        FirewoodModel currentModel = firewoodModel;
        if (!firewoodEffectModelInitialized) {
            firewoodEffectModelInitialized = true;
            previousFirewoodEffectModel = currentModel;
            return;
        }
        if (previousFirewoodEffectModel == currentModel) {
            return;
        }

        FirewoodEffectStage previousStage = FirewoodEffectStage.from(previousFirewoodEffectModel);
        FirewoodEffectStage currentStage = FirewoodEffectStage.from(currentModel);
        RandomSource random = level.getRandom();
        FirewoodEffectOrigin origin = getFirewoodEffectOrigin();

        if (previousStage == FirewoodEffectStage.LIT && currentStage == FirewoodEffectStage.SMALL_LIT) {
            spawnFirewoodShrinkEffects(level, random, origin);
        } else if (previousStage == FirewoodEffectStage.SMALL_LIT && currentStage == FirewoodEffectStage.ASH) {
            spawnFirewoodAshEffects(level, random, origin);
        }

        previousFirewoodEffectModel = currentModel;
    }

    private FirewoodEffectOrigin getFirewoodEffectOrigin() {
        FirewoodRenderOffset renderOffset = getFirewoodRenderOffset();
        double centerX = worldPosition.getX() + 0.5D + renderOffset.x();
        double centerY = worldPosition.getY() + FIREWOOD_EFFECT_Y;
        double centerZ = worldPosition.getZ() + 0.5D + renderOffset.z();
        double flameSpreadX = Math.min(FIREWOOD_FLAME_SPREAD * cachedComponentWidth, 0.5D);
        double flameSpreadZ = Math.min(FIREWOOD_FLAME_SPREAD * cachedComponentDepth, 0.5D);
        double smokeSpreadX = Math.min(FIREWOOD_SMOKE_SPREAD * cachedComponentWidth, 0.65D);
        double smokeSpreadZ = Math.min(FIREWOOD_SMOKE_SPREAD * cachedComponentDepth, 0.65D);
        return new FirewoodEffectOrigin(centerX, centerY, centerZ, flameSpreadX, flameSpreadZ, smokeSpreadX, smokeSpreadZ);
    }

    private static void spawnFirewoodShrinkEffects(Level level, RandomSource random, FirewoodEffectOrigin origin) {
        for (int i = 0; i < 3; i++) {
            level.addParticle(
                    ParticleTypes.ASH,
                    offset(random, origin.x(), origin.smokeSpreadX() * 0.75D),
                    origin.y() + 0.08D + random.nextDouble() * 0.12D,
                    offset(random, origin.z(), origin.smokeSpreadZ() * 0.75D),
                    random.nextGaussian() * 0.018D,
                    0.018D + random.nextDouble() * 0.025D,
                    random.nextGaussian() * 0.018D
            );
        }
        for (int i = 0; i < 2; i++) {
            level.addParticle(
                    ParticleTypes.SMOKE,
                    offset(random, origin.x(), origin.smokeSpreadX()),
                    origin.y() + 0.18D + random.nextDouble() * 0.12D,
                    offset(random, origin.z(), origin.smokeSpreadZ()),
                    random.nextGaussian() * 0.01D,
                    0.035D + random.nextDouble() * 0.02D,
                    random.nextGaussian() * 0.01D
            );
        }
        if (random.nextBoolean()) {
            level.addParticle(
                    ParticleTypes.LAVA,
                    offset(random, origin.x(), origin.flameSpreadX() * 0.55D),
                    origin.y() + 0.08D,
                    offset(random, origin.z(), origin.flameSpreadZ() * 0.55D),
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
        level.playLocalSound(origin.x(), origin.y(), origin.z(), SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.32F, 0.6F + random.nextFloat() * 0.16F, false);
    }

    private static void spawnFirewoodAshEffects(Level level, RandomSource random, FirewoodEffectOrigin origin) {
        for (int i = 0; i < 5; i++) {
            level.addParticle(
                    random.nextBoolean() ? ParticleTypes.ASH : ParticleTypes.WHITE_ASH,
                    offset(random, origin.x(), origin.smokeSpreadX()),
                    origin.y() + 0.04D + random.nextDouble() * 0.16D,
                    offset(random, origin.z(), origin.smokeSpreadZ()),
                    random.nextGaussian() * 0.02D,
                    0.012D + random.nextDouble() * 0.025D,
                    random.nextGaussian() * 0.02D
            );
        }
        for (int i = 0; i < 2; i++) {
            level.addParticle(
                    ParticleTypes.SMOKE,
                    offset(random, origin.x(), origin.smokeSpreadX() * 0.8D),
                    origin.y() + 0.1D + random.nextDouble() * 0.12D,
                    offset(random, origin.z(), origin.smokeSpreadZ() * 0.8D),
                    random.nextGaussian() * 0.012D,
                    0.025D + random.nextDouble() * 0.02D,
                    random.nextGaussian() * 0.012D
            );
        }
        level.playLocalSound(origin.x(), origin.y(), origin.z(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.16F, 1.8F + random.nextFloat() * 0.2F, false);
        level.playLocalSound(origin.x(), origin.y(), origin.z(), SoundEvents.SAND_FALL, SoundSource.BLOCKS, 0.22F, 0.7F + random.nextFloat() * 0.16F, false);
    }

    private static void addFireParticle(Level level, RandomSource random, double centerX, double centerY, double centerZ, double spreadX, double spreadZ) {
        level.addParticle(
                random.nextBoolean() ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME,
                offset(random, centerX, spreadX),
                centerY + random.nextDouble() * 0.1D,
                offset(random, centerZ, spreadZ),
                random.nextGaussian() * 0.003D,
                0.01D + random.nextDouble() * 0.01D,
                random.nextGaussian() * 0.003D
        );
    }

    private static double offset(RandomSource random, double center, double spread) {
        return center + (random.nextDouble() * 2.0D - 1.0D) * spread;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (masterPos != null) {
            output.putLong(MASTER_POS_KEY, masterPos.asLong());
        }
        if (isValidMaster()) {
            if (firewoodModel != null) {
                output.putString(FIREWOOD_MODEL_KEY, firewoodModel.name());
            }
            if (burnTime > 0) {
                output.putInt(BURN_TIME_KEY, burnTime);
            }
            if (burnTimeTotal > 0) {
                output.putInt(BURN_TIME_TOTAL_KEY, burnTimeTotal);
            }
            if (burnCycle > 0) {
                output.putInt(BURN_CYCLE_KEY, burnCycle);
            }
            if (ashState) {
                output.putBoolean(ASH_STATE_KEY, true);
            }
            ContainerHelper.saveAllItems(output, items);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        masterPos = input.getLong(MASTER_POS_KEY).map(BlockPos::of).orElse(null);
        firewoodModel = input.getString(FIREWOOD_MODEL_KEY)
                .flatMap(IroriBlockEntity::parseFirewoodModel)
                .orElse(null);
        burnTime = input.getInt(BURN_TIME_KEY).orElse(0);
        burnTimeTotal = input.getInt(BURN_TIME_TOTAL_KEY).orElse(burnTime);
        burnCycle = input.getInt(BURN_CYCLE_KEY).orElse(0);
        ashState = input.getBooleanOr(ASH_STATE_KEY, false);
        items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        if (items.getFirst().isEmpty()) {
            input.read(FUEL_STACK_KEY, ItemStack.CODEC).ifPresent(stack -> items.set(0, stack));
        }
        invalidateRenderOffsetCache();
    }

    public void syncToClient() {
        if (level != null && !level.isClientSide()) {
            syncComponentToClient(level, collectConnectedComponent(level, getBlockPos()));
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        loadCustomOnly(input);
        syncFirewoodLightStateFromBlockEntityData();
    }

    @Override
    public void onDataPacket(Connection net, ValueInput input) {
        loadWithComponents(input);
        syncFirewoodLightStateFromBlockEntityData();
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public boolean isEmpty() {
        IroriBlockEntity master = getMaster();
        return master.getStoredFuelStack().isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot != 0) {
            return ItemStack.EMPTY;
        }
        return getMaster().getStoredFuelStack();
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != 0 || amount <= 0) {
            return ItemStack.EMPTY;
        }

        IroriBlockEntity master = resolveMaster();
        ItemStack removed = ContainerHelper.removeItem(master.items, slot, amount);
        if (!removed.isEmpty()) {
            master.afterFuelChanged(master.getLevelRandom());
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0) {
            return ItemStack.EMPTY;
        }

        IroriBlockEntity master = resolveMaster();
        ItemStack removed = ContainerHelper.takeItem(master.items, slot);
        if (!removed.isEmpty()) {
            master.afterFuelChanged(master.getLevelRandom());
        }
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != 0) {
            return;
        }

        IroriBlockEntity master = resolveMaster();
        ItemStack normalized = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        int maxStackSize = master.getMaxStackSize(normalized);
        if (!normalized.isEmpty() && normalized.getCount() > maxStackSize) {
            normalized.setCount(maxStackSize);
        }
        ItemStack current = master.getStoredFuelStack();
        if (ItemStack.isSameItemSameComponents(current, normalized)
                && current.getCount() == normalized.getCount()) {
            return;
        }

        master.setStoredFuelStack(normalized);
        master.afterFuelChanged(master.getLevelRandom());
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D
        ) <= 64.0D;
    }

    @Override
    public void clearContent() {
        IroriBlockEntity master = resolveMaster();
        if (master.isEmpty()) {
            return;
        }

        master.items.set(0, ItemStack.EMPTY);
        master.afterFuelChanged(master.getLevelRandom());
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot != 0 || stack.isEmpty() || level == null) {
            return false;
        }
        return stack.getBurnTime(RecipeType.SMELTING, level.fuelValues()) > 0;
    }

    private void setStoredFuelStack(ItemStack stack) {
        items.set(0, stack);
    }

    private ItemStack getStoredFuelStack() {
        return items.get(0);
    }

    private boolean startBurningFromFuel(Level level, RandomSource random) {
        ItemStack fuelStack = getStoredFuelStack();
        if (fuelStack.isEmpty()) {
            return false;
        }

        int nextBurnTime = getFuelBurnTime(fuelStack, level);
        if (nextBurnTime <= 0) {
            return false;
        }

        burnTime = nextBurnTime;
        burnTimeTotal = nextBurnTime;
        burnCycle = burnCycle == Integer.MAX_VALUE ? 1 : burnCycle + 1;
        ashState = false;
        consumeFuel();
        updateFirewoodModelFromFuel(random);
        syncFirewoodLightState(level, collectConnectedComponent(level, getBlockPos()), getBlockPos(), true);
        return true;
    }

    private void consumeFuel() {
        ItemStack fuelStack = getStoredFuelStack();
        if (fuelStack.isEmpty()) {
            return;
        }

        Item fuelItem = fuelStack.getItem();
        fuelStack.shrink(1);
        if (fuelStack.isEmpty()) {
            ItemStackTemplate remainder = fuelItem.getCraftingRemainder();
            setStoredFuelStack(remainder != null ? remainder.create() : ItemStack.EMPTY);
        }
    }

    private int getRenderableFuelCount() {
        int fuelCount = burnTime > 0 ? 1 : 0;
        ItemStack fuelStack = getStoredFuelStack();
        if (fuelStack.isEmpty()) {
            return fuelCount;
        }
        if (level == null || getFuelBurnTime(fuelStack, level) > 0) {
            fuelCount += fuelStack.getCount();
        }
        return fuelCount;
    }

    private static int getFuelBurnTime(ItemStack stack, Level level) {
        return stack.getBurnTime(RecipeType.SMELTING, level.fuelValues());
    }

    private boolean isAshModel() {
        return firewoodModel != null && firewoodModel.isAsh();
    }

    private void dropBoneMealAsh() {
        FirewoodRenderOffset renderOffset = getFirewoodRenderOffset();
        spawnBoneMealAsh(
                worldPosition.getX() + 0.5D + renderOffset.x(),
                worldPosition.getY() + ASH_DROP_Y,
                worldPosition.getZ() + 0.5D + renderOffset.z()
        );
    }

    private void dropBoneMealAsh(BlockPos dropPos) {
        spawnBoneMealAsh(dropPos.getX() + 0.5D, dropPos.getY() + ASH_DROP_Y, dropPos.getZ() + 0.5D);
    }

    private void spawnBoneMealAsh(double x, double y, double z) {
        if (level == null) {
            return;
        }

        int count = MIN_ASH_BONE_MEAL_DROPS + level.getRandom().nextInt(MAX_ASH_BONE_MEAL_DROPS - MIN_ASH_BONE_MEAL_DROPS + 1);
        ItemEntity itemEntity = new ItemEntity(level, x, y, z, new ItemStack(Items.BONE_MEAL, count));
        itemEntity.setDeltaMovement(
                level.getRandom().triangle(0.0D, 0.035D),
                0.04D,
                level.getRandom().triangle(0.0D, 0.035D)
        );
        level.addFreshEntity(itemEntity);
    }

    private void resetStoredState() {
        items.set(0, ItemStack.EMPTY);
        firewoodModel = null;
        burnTime = 0;
        burnTimeTotal = 0;
        burnCycle = 0;
        ashState = false;
        invalidateRenderOffsetCache();
    }

    public void onFuelSlotChanged() {
        IroriBlockEntity master = resolveMaster();
        master.afterFuelChanged(master.getLevelRandom());
    }

    private void afterFuelChanged(RandomSource random) {
        if (!getStoredFuelStack().isEmpty()) {
            ashState = false;
        }
        updateFirewoodModelFromFuel(random);
        setChanged();
        syncToClient();
    }

    private void tickFirewoodAppearAnimation() {
        firewoodAppearProgressOld = firewoodAppearProgress;
        boolean visibleFirewood = shouldRenderFirewood();

        if (!firewoodAppearAnimationInitialized) {
            firewoodAppearAnimationInitialized = true;
            hadVisibleFirewood = visibleFirewood;
            firewoodAppearProgress = visibleFirewood ? 1.0F : 0.0F;
            firewoodAppearProgressOld = firewoodAppearProgress;
            return;
        }

        if (visibleFirewood && !hadVisibleFirewood) {
            firewoodAppearProgress = 0.0F;
            firewoodAppearProgressOld = 0.0F;
        }
        hadVisibleFirewood = visibleFirewood;

        if (visibleFirewood) {
            firewoodAppearProgress = Math.min(1.0F, firewoodAppearProgress + FIREWOOD_APPEAR_ANIMATION_SPEED);
        } else {
            firewoodAppearProgress = 0.0F;
            firewoodAppearProgressOld = 0.0F;
        }
    }

    private void updateFirewoodModelFromFuel(RandomSource random) {
        FirewoodModel nextModel = selectModelForFuel(random, getRenderableFuelCount(), burnTime > 0, ashState, firewoodModel);
        if (firewoodModel != nextModel) {
            firewoodModel = nextModel;
        }
    }

    private void invalidateRenderOffsetCache() {
        renderOffsetCached = false;
        cachedRenderOffsetX = 0.0D;
        cachedRenderOffsetZ = 0.0D;
        cachedComponentWidth = 1;
        cachedComponentDepth = 1;
    }

    private RandomSource getLevelRandom() {
        return level != null ? level.getRandom() : RandomSource.create(worldPosition.asLong());
    }

    private static Optional<FirewoodModel> parseFirewoodModel(String name) {
        try {
            return Optional.of(FirewoodModel.valueOf(name));
        } catch (IllegalArgumentException _) {
            return Optional.empty();
        }
    }

    private static FirewoodState resolveComponentFirewoodState(Level level, Set<BlockPos> component, BlockPos electedMasterPos) {
        BlockEntity elected = level.getBlockEntity(electedMasterPos);
        if (elected instanceof IroriBlockEntity irori) {
            FirewoodState electedState = irori.currentFirewoodState();
            if (!electedState.isEmpty()) {
                return electedState;
            }
        }

        for (BlockPos pos : component) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof IroriBlockEntity irori && irori.isValidMaster()) {
                FirewoodState state = irori.currentFirewoodState();
                if (!state.isEmpty()) {
                    return state;
                }
            }
        }

        for (BlockPos pos : component) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof IroriBlockEntity irori) {
                FirewoodState state = irori.currentFirewoodState();
                if (!state.isEmpty()) {
                    return state;
                }
            }
        }

        return FirewoodState.EMPTY;
    }

    private FirewoodState currentFirewoodState() {
        return new FirewoodState(getStoredFuelStack().copy(), firewoodModel, burnTime, burnTimeTotal, burnCycle, ashState);
    }

    private static @Nullable FirewoodModel selectModelForFuel(RandomSource random, int fuelCount, boolean lit, boolean ashState, @Nullable FirewoodModel currentModel) {
        FirewoodModelStage stage = selectModelStage(fuelCount, lit, ashState);
        if (stage == null) {
            return null;
        }
        return FirewoodModel.select(random, stage, currentModel);
    }

    private static @Nullable FirewoodModelStage selectModelStage(int fuelCount, boolean lit, boolean ashState) {
        if (ashState && fuelCount <= 0) {
            return FirewoodModelStage.ASH;
        }
        if (fuelCount <= 0) {
            return null;
        }
        boolean small = fuelCount <= SMALL_MODEL_THRESHOLD;
        if (lit) {
            return small ? FirewoodModelStage.SMALL_LIT : FirewoodModelStage.LIT;
        }
        return small ? FirewoodModelStage.SMALL_UNLIT : FirewoodModelStage.UNLIT;
    }

    private static void syncComponentToClient(Level level, Set<BlockPos> component) {
        if (level.isClientSide()) {
            return;
        }

        for (BlockPos pos : component) {
            BlockState state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    private static void syncFirewoodLightState(Level level, Set<BlockPos> component, BlockPos masterPos, boolean lit) {
        Set<BlockPos> firewoodLightPositions = lit ? getFirewoodLightPositions(component, masterPos) : Set.of();
        for (BlockPos pos : component) {
            BlockState state = level.getBlockState(pos);
            AuxiliaryLightManager lightManager = level.getAuxLightManager(pos);
            if (lightManager != null) {
                boolean shouldLight = firewoodLightPositions.contains(pos) && state.hasProperty(IroriBlock.WATERLOGGED) && !state.getValue(IroriBlock.WATERLOGGED);
                lightManager.setLightAt(pos, shouldLight ? 13 : 0);
            }
        }
    }

    private void syncFirewoodLightStateFromBlockEntityData() {
        if (level == null) {
            return;
        }

        IroriBlockEntity master = getMaster();
        syncFirewoodLightState(level, collectConnectedComponent(level, master.getBlockPos()), master.getBlockPos(), master.burnTime > 0);
    }

    private static Set<BlockPos> getFirewoodLightPositions(Set<BlockPos> component, BlockPos masterPos) {
        RectangularComponent bounds = computeComponentBounds(component);
        int centerMinX = bounds.minX() + (bounds.width() - 1) / 2;
        int centerMaxX = bounds.minX() + bounds.width() / 2;
        int centerMinZ = bounds.minZ() + (bounds.depth() - 1) / 2;
        int centerMaxZ = bounds.minZ() + bounds.depth() / 2;

        Set<BlockPos> lightPositions = new HashSet<>();
        for (int x = centerMinX; x <= centerMaxX; x++) {
            for (int z = centerMinZ; z <= centerMaxZ; z++) {
                BlockPos lightPos = new BlockPos(x, masterPos.getY(), z);
                if (component.contains(lightPos)) {
                    lightPositions.add(lightPos);
                }
            }
        }
        if (lightPositions.isEmpty() && component.contains(masterPos)) {
            lightPositions.add(masterPos);
        }
        return lightPositions;
    }

    private static Set<BlockPos> collectConnectedComponent(Level level, BlockPos origin) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        BlockPos originImmutable = origin.immutable();
        queue.add(originImmutable);
        visited.add(originImmutable);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.removeFirst();
            BlockState currentState = level.getBlockState(currentPos);

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (!hasConnection(currentState, direction)) {
                    continue;
                }

                BlockPos nextPos = currentPos.relative(direction).immutable();
                if (visited.contains(nextPos)) {
                    continue;
                }

                BlockState nextState = level.getBlockState(nextPos);
                if (!nextState.is(currentState.getBlock()) || !hasConnection(nextState, direction.getOpposite())) {
                    continue;
                }

                visited.add(nextPos);
                queue.add(nextPos);
            }
        }

        return visited;
    }

    private static boolean hasConnection(BlockState state, Direction direction) {
        BooleanProperty property = IroriBlock.getConnectionProperty(direction);
        return state.hasProperty(property) && state.getValue(property);
    }

    private enum FirewoodEffectStage {
        OTHER,
        LIT,
        SMALL_LIT,
        ASH;

        private static FirewoodEffectStage from(@Nullable FirewoodModel model) {
            if (model == null) {
                return OTHER;
            }
            return model.stage.effectStage();
        }
    }

    private enum FirewoodModelStage {
        UNLIT(FirewoodEffectStage.OTHER),
        SMALL_UNLIT(FirewoodEffectStage.OTHER),
        LIT(FirewoodEffectStage.LIT),
        SMALL_LIT(FirewoodEffectStage.SMALL_LIT),
        ASH(FirewoodEffectStage.ASH);

        private static final int VARIANT_COUNT = 3;

        private final FirewoodEffectStage effectStage;

        FirewoodModelStage(FirewoodEffectStage effectStage) {
            this.effectStage = effectStage;
        }

        private FirewoodEffectStage effectStage() {
            return effectStage;
        }
    }

    private record FirewoodEffectOrigin(
            double x,
            double y,
            double z,
            double flameSpreadX,
            double flameSpreadZ,
            double smokeSpreadX,
            double smokeSpreadZ
    ) {
    }

    public record FirewoodRenderOffset(double x, double z) {
    }

    public record GrillRenderInfo(GrillModel model, double offsetX, double offsetZ, boolean rotated) {
    }

    public record GrillLayoutInfo(
            GrillModel model,
            double offsetX,
            double offsetZ,
            boolean rotated,
            int centerWidth,
            int centerDepth
    ) {
    }

    public enum GrillModel {
        ONE_BY_ONE("1_1"),
        ONE_BY_TWO("1_2"),
        TWO_BY_TWO("2_2");

        private final String modelName;

        GrillModel(String modelName) {
            this.modelName = modelName;
        }

        public String modelName() {
            return modelName;
        }
    }

    public record RectangularComponent(int minX, int maxX, int minZ, int maxZ) {
        public int width() {
            return maxX - minX + 1;
        }

        public int depth() {
            return maxZ - minZ + 1;
        }
    }

    private record FirewoodState(ItemStack fuelStack, @Nullable FirewoodModel firewoodModel, int burnTime, int burnTimeTotal, int burnCycle, boolean ashState) {
        private static final FirewoodState EMPTY = new FirewoodState(ItemStack.EMPTY, null, 0, 0, 0, false);

        private boolean isEmpty() {
            return fuelStack.isEmpty() && firewoodModel == null && burnTime <= 0 && !ashState;
        }
    }

    public enum FirewoodModel {
        UNLIT_1(FirewoodModelStage.UNLIT, "unlit_1", 0),
        UNLIT_2(FirewoodModelStage.UNLIT, "unlit_2", 1),
        UNLIT_3(FirewoodModelStage.UNLIT, "unlit_3", 2),
        SMALL_UNLIT_1(FirewoodModelStage.SMALL_UNLIT, "small_unlit_1", 0),
        SMALL_UNLIT_2(FirewoodModelStage.SMALL_UNLIT, "small_unlit_2", 1),
        SMALL_UNLIT_3(FirewoodModelStage.SMALL_UNLIT, "small_unlit_3", 2),
        LIT_1(FirewoodModelStage.LIT, "lit_1", 0),
        LIT_2(FirewoodModelStage.LIT, "lit_2", 1),
        LIT_3(FirewoodModelStage.LIT, "lit_3", 2),
        SMALL_LIT_1(FirewoodModelStage.SMALL_LIT, "small_lit_1", 0),
        SMALL_LIT_2(FirewoodModelStage.SMALL_LIT, "small_lit_2", 1),
        SMALL_LIT_3(FirewoodModelStage.SMALL_LIT, "small_lit_3", 2),
        ASH_1(FirewoodModelStage.ASH, "ash_1", 0),
        ASH_2(FirewoodModelStage.ASH, "ash_2", 1),
        ASH_3(FirewoodModelStage.ASH, "ash_3", 2);

        private final FirewoodModelStage stage;
        private final String modelName;
        private final int variantIndex;

        FirewoodModel(FirewoodModelStage stage, String modelName, int variantIndex) {
            this.stage = stage;
            this.modelName = modelName;
            this.variantIndex = variantIndex;
        }

        public String modelName() {
            return modelName;
        }

        private boolean isAsh() {
            return stage == FirewoodModelStage.ASH;
        }

        private static FirewoodModel select(RandomSource random, FirewoodModelStage stage, @Nullable FirewoodModel currentModel) {
            if (currentModel != null && currentModel.stage == stage) {
                return currentModel;
            }
            int variantIndex = currentModel != null
                    ? currentModel.variantIndex
                    : random.nextInt(FirewoodModelStage.VARIANT_COUNT);
            return byStageAndVariant(stage, variantIndex);
        }

        private static FirewoodModel byStageAndVariant(FirewoodModelStage stage, int variantIndex) {
            for (FirewoodModel model : values()) {
                if (model.stage == stage && model.variantIndex == variantIndex) {
                    return model;
                }
            }
            throw new IllegalStateException("Missing firewood model for stage " + stage + " variant " + variantIndex);
        }
    }
}
