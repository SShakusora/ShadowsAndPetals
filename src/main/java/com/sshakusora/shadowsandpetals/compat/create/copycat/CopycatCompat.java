package com.sshakusora.shadowsandpetals.compat.create.copycat;

import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import com.sshakusora.shadowsandpetals.block.RawConcreteBlock;
import com.sshakusora.shadowsandpetals.compat.copycats.CopycatsCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Create Copycat integration kept behind the optional Create compatibility
 * boundary. The caller loads this class reflectively only when Create exists.
 */
public final class CopycatCompat {
    private CopycatCompat() {
    }

    /**
     * Cycles the raw-concrete pattern stored in a Copycat block entity.
     * Copycats+ is checked first because its multi-state entities are not
     * Create's {@link CopycatBlockEntity} subclasses.
     */
    public static boolean cycleRawConcrete(UseOnContext context) {
        if (CopycatsCompat.cycleRawConcrete(context)) {
            return true;
        }

        if (cycleGenericCreateCopycat(context)) {
            return true;
        }

        CopycatBlockEntity copycat = rawConcreteCopycat(context.getLevel(), context.getClickedPos());
        if (copycat == null) {
            return false;
        }

        copycat.setMaterial(copycat.getMaterial().cycle(RawConcreteBlock.TEXTURE));
        return true;
    }

    /** Returns whether the clicked Copycat material is raw concrete. */
    public static boolean isRawConcrete(UseOnContext context) {
        return CopycatsCompat.isRawConcrete(context)
                || isGenericCreateCopycat(context)
                || rawConcreteCopycat(context.getLevel(), context.getClickedPos()) != null;
    }

    /** Compatibility overload for callers that do not have a hit location. */
    public static boolean cycleRawConcrete(Level level, BlockPos pos) {
        CopycatBlockEntity copycat = rawConcreteCopycat(level, pos);
        if (copycat == null) {
            return false;
        }
        copycat.setMaterial(copycat.getMaterial().cycle(RawConcreteBlock.TEXTURE));
        return true;
    }

    /** Compatibility overload for callers that do not have a hit location. */
    public static boolean isRawConcrete(Level level, BlockPos pos) {
        return rawConcreteCopycat(level, pos) != null;
    }

    private static boolean cycleGenericCreateCopycat(UseOnContext context) {
        GenericTarget target = genericCreateCopycat(context);
        if (target == null) {
            return false;
        }
        try {
            invoke(target.blockEntity(), "setMaterial",
                    new Class<?>[]{BlockState.class}, target.material().cycle(RawConcreteBlock.TEXTURE));
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private static boolean isGenericCreateCopycat(UseOnContext context) {
        return genericCreateCopycat(context) != null;
    }

    private static GenericTarget genericCreateCopycat(UseOnContext context) {
        if (!(context.getLevel().getBlockState(context.getClickedPos()).getBlock()
                instanceof com.simibubi.create.content.decoration.copycat.CopycatBlock)) {
            return null;
        }
        BlockEntity blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
        if (blockEntity == null) {
            return null;
        }
        try {
            BlockState material = (BlockState) invoke(blockEntity, "getMaterial", new Class<?>[0]);
            return material != null
                    && material.getBlock() instanceof RawConcreteBlock
                    && material.hasProperty(RawConcreteBlock.TEXTURE)
                    ? new GenericTarget(blockEntity, material) : null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return null;
        }
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

    private static CopycatBlockEntity rawConcreteCopycat(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CopycatBlockEntity copycat)) {
            return null;
        }

        BlockState material = copycat.getMaterial();
        return material.getBlock() instanceof RawConcreteBlock
                && material.hasProperty(RawConcreteBlock.TEXTURE)
                ? copycat : null;
    }

    private record GenericTarget(BlockEntity blockEntity, BlockState material) {
    }
}
