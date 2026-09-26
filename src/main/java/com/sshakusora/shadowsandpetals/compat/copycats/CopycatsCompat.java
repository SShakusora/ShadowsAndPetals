package com.sshakusora.shadowsandpetals.compat.copycats;

import com.sshakusora.shadowsandpetals.block.RawConcreteBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Reflection-only Copycats+ interaction adapter. Copycats+ is optional and is
 * deliberately not a compile-time dependency of the base mod.
 */
public final class CopycatsCompat {
    private static final String COPYCATS_BLOCK_INTERFACE =
            "com.copycatsplus.copycats.foundation.copycat.ICopycatBlock";
    private static final String COPYCATS_ENTITY_INTERFACE =
            "com.copycatsplus.copycats.foundation.copycat.ICopycatBlockEntity";
    private static final String COPYCATS_MULTI_ENTITY_INTERFACE =
            "com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlockEntity";

    private CopycatsCompat() {
    }

    public static boolean cycleRawConcrete(UseOnContext context) {
        CopycatsTarget target = findTarget(context);
        if (target == null) {
            return false;
        }

        try {
            BlockState next = target.material().cycle(RawConcreteBlock.TEXTURE);
            if (target.multiState()) {
                invoke(target.blockEntity(), "setMaterial",
                        new Class<?>[]{String.class, BlockState.class}, target.property(), next);
            } else {
                invoke(target.blockEntity(), "setMaterial",
                        new Class<?>[]{BlockState.class}, next);
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    public static boolean isRawConcrete(UseOnContext context) {
        return findTarget(context) != null;
    }

    @Nullable
    private static CopycatsTarget findTarget(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null
                || !implementsNamedInterface(blockEntity.getClass(), COPYCATS_ENTITY_INTERFACE)) {
            return null;
        }

        try {
            if (implementsNamedInterface(blockEntity.getClass(), COPYCATS_MULTI_ENTITY_INTERFACE)) {
                Block block = level.getBlockState(pos).getBlock();
                if (!implementsNamedInterface(block.getClass(), COPYCATS_BLOCK_INTERFACE)) {
                    return null;
                }

                String property = (String) invoke(
                        block,
                        "getPropertyFromInteraction",
                        new Class<?>[]{
                                BlockState.class,
                                BlockGetter.class,
                                BlockPos.class,
                                Vec3.class,
                                Direction.class,
                                boolean.class
                        },
                        level.getBlockState(pos),
                        level,
                        pos,
                        context.getClickLocation(),
                        context.getClickedFace(),
                        true);
                if (property == null || property.isBlank()) {
                    return null;
                }

                Object storage = invoke(blockEntity, "getMaterialItemStorage", new Class<?>[0]);
                Object materialItem = invoke(storage, "getMaterialItem",
                        new Class<?>[]{String.class}, property);
                BlockState material = (BlockState) invoke(materialItem, "material", new Class<?>[0]);
                return isRawConcrete(material)
                        ? new CopycatsTarget(blockEntity, property, material, true)
                        : null;
            }

            BlockState material = (BlockState) invoke(blockEntity, "getMaterial", new Class<?>[0]);
            return isRawConcrete(material)
                    ? new CopycatsTarget(blockEntity, null, material, false)
                    : null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private static boolean isRawConcrete(@Nullable BlockState material) {
        return material != null
                && material.getBlock() instanceof RawConcreteBlock
                && material.hasProperty(RawConcreteBlock.TEXTURE);
    }

    private static Object invoke(Object target, String name, Class<?>[] parameterTypes, Object... args)
            throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(name, parameterTypes);
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw exception;
        }
    }

    private static boolean implementsNamedInterface(Class<?> type, String interfaceName) {
        for (Class<?> candidate = type; candidate != null; candidate = candidate.getSuperclass()) {
            for (Class<?> implemented : candidate.getInterfaces()) {
                if (implemented.getName().equals(interfaceName)
                        || implementsNamedInterface(implemented, interfaceName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private record CopycatsTarget(
            Object blockEntity,
            @Nullable String property,
            BlockState material,
            boolean multiState
    ) {
    }
}
