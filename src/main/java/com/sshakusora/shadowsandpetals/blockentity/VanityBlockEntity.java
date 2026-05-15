package com.sshakusora.shadowsandpetals.blockentity;

import com.sshakusora.shadowsandpetals.data.BuiltinLanguageKeys;
import com.sshakusora.shadowsandpetals.registries.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

public class VanityBlockEntity extends RandomizableContainerBlockEntity {
    public static final int CONTAINER_SIZE = 9;
    private static final int DRAWER_EVENT_ID = 1;
    private static final float DRAWER_SPEED = 0.12F;
    private static final Component DEFAULT_NAME = Component.translatable(BuiltinLanguageKeys.VANITY_CONTAINER_NAME.key());

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState blockState) {
            VanityBlockEntity.this.playSound(SoundEvents.BARREL_OPEN);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState blockState) {
            VanityBlockEntity.this.playSound(SoundEvents.BARREL_CLOSE);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState blockState, int previous, int current) {
            level.blockEvent(pos, blockState.getBlock(), DRAWER_EVENT_ID, current);
        }

        @Override
        public boolean isOwnContainer(Player player) {
            if (player.containerMenu instanceof ChestMenu chestMenu) {
                Container container = chestMenu.getContainer();
                return container == VanityBlockEntity.this;
            }
            return false;
        }
    };
    private int openCount;
    private float drawerProgress;
    private float drawerProgressOld;
    private int openCycle;
    private float drawerTravelScale;

    public VanityBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.VANITY.get(), pos, blockState);
        this.drawerTravelScale = randomTravelScale(pos.asLong(), 0);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VanityBlockEntity blockEntity) {
        if (level.isClientSide()) {
            blockEntity.drawerProgressOld = blockEntity.drawerProgress;
            float target = blockEntity.openCount > 0 ? 1.0F : 0.0F;
            if (blockEntity.openCount > 0 && blockEntity.drawerProgressOld <= 0.001F && blockEntity.drawerProgress <= 0.001F) {
                blockEntity.openCycle++;
                blockEntity.drawerTravelScale = randomTravelScale(pos.asLong(), blockEntity.openCycle);
            }
            if (blockEntity.drawerProgress < target) {
                blockEntity.drawerProgress = Math.min(target, blockEntity.drawerProgress + DRAWER_SPEED);
            } else if (blockEntity.drawerProgress > target) {
                blockEntity.drawerProgress = Math.max(target, blockEntity.drawerProgress - DRAWER_SPEED);
            }
        }
    }

    public float getDrawerProgress(float partialTick) {
        return drawerProgressOld + (drawerProgress - drawerProgressOld) * partialTick;
    }

    public float getDrawerTravelScale() {
        return this.drawerTravelScale;
    }

    public void recheckOpen() {
        if (!this.remove) {
            this.openersCounter.recheckOpeners(this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(input)) {
            ContainerHelper.loadAllItems(input, this.items);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.trySaveLootTable(output)) {
            ContainerHelper.saveAllItems(output, this.items);
        }
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new ChestMenu(MenuType.GENERIC_9x1, containerId, inventory, this, 1);
    }

    @Override
    public void startOpen(ContainerUser containerUser) {
        if (!this.remove && !containerUser.getLivingEntity().isSpectator()) {
            this.openersCounter.incrementOpeners(
                    containerUser.getLivingEntity(),
                    this.getLevel(),
                    this.getBlockPos(),
                    this.getBlockState(),
                    containerUser.getContainerInteractionRange()
            );
        }
    }

    @Override
    public void stopOpen(ContainerUser containerUser) {
        if (!this.remove && !containerUser.getLivingEntity().isSpectator()) {
            this.openersCounter.decrementOpeners(containerUser.getLivingEntity(), this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    @Override
    public List<ContainerUser> getEntitiesWithContainerOpen() {
        return this.openersCounter.getEntitiesWithContainerOpen(this.getLevel(), this.getBlockPos());
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id == DRAWER_EVENT_ID) {
            this.openCount = type;
            return true;
        }
        return super.triggerEvent(id, type);
    }

    private void playSound(SoundEvent soundEvent) {
        if (this.level != null) {
            this.level.playSound(
                    null,
                    this.worldPosition,
                    soundEvent,
                    SoundSource.BLOCKS,
                    0.5F,
                    this.level.getRandom().nextFloat() * 0.1F + 0.9F
            );
        }
    }

    private static float randomTravelScale(long blockSeed, int openCycle) {
        long mixed = blockSeed * 31L + openCycle * 0x9E3779B97F4A7C15L;
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= mixed >>> 33;
        float normalized = (float) ((mixed >>> 40) & 0xFFFFFFL) / 0xFFFFFFL;
        return 0.84F + normalized * 0.32F;
    }
}
