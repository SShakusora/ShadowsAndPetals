package com.sshakusora.shadowsandpetals.event;

import com.sshakusora.shadowsandpetals.ShadowsAndPetals;
import com.sshakusora.shadowsandpetals.blockentity.irori.IroriBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent;

@EventBusSubscriber(modid = ShadowsAndPetals.MOD_ID)
public final class IroriPhantomRepellent {
    private static final double HORIZONTAL_RADIUS = 32.0D;
    private static final double VERTICAL_RADIUS = 48.0D;
    private static final int REPEL_INTERVAL_TICKS = 10;
    private static final double ESCAPE_ANCHOR_DISTANCE = HORIZONTAL_RADIUS + 12.0D;
    private static final double ESCAPE_ANCHOR_HEIGHT = 32.0D;

    private IroriPhantomRepellent() {
    }

    public static void tick(ServerLevel level, IroriBlockEntity irori) {
        if (Math.floorMod(level.getGameTime() + irori.getBlockPos().asLong(), REPEL_INTERVAL_TICKS) != 0L) {
            return;
        }

        Vec3 center = getCenter(irori);
        AABB bounds = new AABB(
                center.x - HORIZONTAL_RADIUS,
                center.y - VERTICAL_RADIUS,
                center.z - HORIZONTAL_RADIUS,
                center.x + HORIZONTAL_RADIUS,
                center.y + VERTICAL_RADIUS,
                center.z + HORIZONTAL_RADIUS
        );
        for (Phantom phantom : level.getEntitiesOfClass(Phantom.class, bounds)) {
            if (phantom.isAlive() && isWithinRange(phantom.position(), center)) {
                repel(phantom, center);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerSpawnPhantoms(PlayerSpawnPhantomsEvent event) {
        if (event.getResult() == PlayerSpawnPhantomsEvent.Result.DENY || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }

        if (hasBurningIroriNear(level, event.getEntity().position())) {
            event.setResult(PlayerSpawnPhantomsEvent.Result.DENY);
        }
    }

    private static boolean hasBurningIroriNear(ServerLevel level, Vec3 position) {
        int minChunkX = SectionPos.blockToSectionCoord(Mth.floor(position.x - HORIZONTAL_RADIUS));
        int maxChunkX = SectionPos.blockToSectionCoord(Mth.floor(position.x + HORIZONTAL_RADIUS));
        int minChunkZ = SectionPos.blockToSectionCoord(Mth.floor(position.z - HORIZONTAL_RADIUS));
        int maxChunkZ = SectionPos.blockToSectionCoord(Mth.floor(position.z + HORIZONTAL_RADIUS));

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof IroriBlockEntity irori
                            && irori.isValidMaster()
                            && irori.getBurnTime() > 0
                            && isWithinRange(position, getCenter(irori))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static Vec3 getCenter(IroriBlockEntity irori) {
        BlockPos pos = irori.getBlockPos();
        IroriBlockEntity.FirewoodRenderOffset offset = irori.getFirewoodRenderOffset();
        return new Vec3(
                pos.getX() + 0.5D + offset.x(),
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D + offset.z()
        );
    }

    private static boolean isWithinRange(Vec3 position, Vec3 center) {
        double x = position.x - center.x;
        double z = position.z - center.z;
        return x * x + z * z <= HORIZONTAL_RADIUS * HORIZONTAL_RADIUS
                && Math.abs(position.y - center.y) <= VERTICAL_RADIUS;
    }

    private static void repel(Phantom phantom, Vec3 center) {
        phantom.setTarget(null);

        double x = phantom.getX() - center.x;
        double z = phantom.getZ() - center.z;
        double horizontalDistance = Math.sqrt(x * x + z * z);
        if (horizontalDistance < 1.0E-4D) {
            double angle = Math.toRadians(phantom.getYRot() + 90.0F);
            x = Math.cos(angle);
            z = Math.sin(angle);
            horizontalDistance = 1.0D;
        }

        BlockPos escapeAnchor = BlockPos.containing(
                center.x + x / horizontalDistance * ESCAPE_ANCHOR_DISTANCE,
                Math.max(phantom.getY(), center.y + ESCAPE_ANCHOR_HEIGHT),
                center.z + z / horizontalDistance * ESCAPE_ANCHOR_DISTANCE
        );

        // PhantomMoveControl steers toward moveTargetPoint and smooths both rotation and velocity.
        // Moving its circle anchor outside the protected area lets the vanilla AI fly it away naturally.
        phantom.anchorPoint = escapeAnchor;
        phantom.moveTargetPoint = Vec3.atCenterOf(escapeAnchor);
    }
}
