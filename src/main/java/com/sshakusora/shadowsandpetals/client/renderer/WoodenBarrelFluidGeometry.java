package com.sshakusora.shadowsandpetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/**
 * Shared geometry for the fluid surface inside a wooden barrel block and its item model.
 */
public final class WoodenBarrelFluidGeometry {
    public static final float MIN_X = 4.5F / 16.0F;
    public static final float MAX_X = 11.5F / 16.0F;
    public static final float MIN_Z = 4.5F / 16.0F;
    public static final float MAX_Z = 11.5F / 16.0F;
    public static final float MIN_SURFACE_Y = 1.05F / 16.0F;
    public static final float MAX_SURFACE_Y = 8.45F / 16.0F;
    public static final float SURFACE_TEXTURE_MAX_U = 7.0F / 16.0F;
    public static final float SURFACE_TEXTURE_MAX_V = 7.0F / 16.0F;
    public static final float ITEM_SURFACE_EPSILON = 1.0F / 1024.0F;

    private WoodenBarrelFluidGeometry() {
    }

    public static void addExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(MIN_X, MIN_SURFACE_Y, MIN_Z));
        output.accept(new Vector3f(MIN_X, MIN_SURFACE_Y, MAX_Z));
        output.accept(new Vector3f(MAX_X, MIN_SURFACE_Y, MAX_Z));
        output.accept(new Vector3f(MAX_X, MIN_SURFACE_Y, MIN_Z));
        float maxExtentY = MAX_SURFACE_Y + ITEM_SURFACE_EPSILON;
        output.accept(new Vector3f(MIN_X, maxExtentY, MIN_Z));
        output.accept(new Vector3f(MIN_X, maxExtentY, MAX_Z));
        output.accept(new Vector3f(MAX_X, maxExtentY, MAX_Z));
        output.accept(new Vector3f(MAX_X, maxExtentY, MIN_Z));
    }

    public static void renderSurface(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            float surfaceY,
            int color,
            int lightCoords
    ) {
        renderSurface(buffer, pose, sprite, surfaceY, color, lightCoords, Direction.Axis.Y);
    }

    public static void renderSurface(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            float surfaceY,
            int color,
            int lightCoords,
            Direction.Axis axis
    ) {
        addVertex(buffer, pose, sprite, MIN_X, surfaceY, MIN_Z, color, 0.0F, 0.0F, lightCoords, 1.0F, axis);
        addVertex(buffer, pose, sprite, MIN_X, surfaceY, MAX_Z, color, 0.0F, SURFACE_TEXTURE_MAX_V, lightCoords, 1.0F, axis);
        addVertex(buffer, pose, sprite, MAX_X, surfaceY, MAX_Z, color, SURFACE_TEXTURE_MAX_U, SURFACE_TEXTURE_MAX_V, lightCoords, 1.0F, axis);
        addVertex(buffer, pose, sprite, MAX_X, surfaceY, MIN_Z, color, SURFACE_TEXTURE_MAX_U, 0.0F, lightCoords, 1.0F, axis);

        addVertex(buffer, pose, sprite, MAX_X, surfaceY, MIN_Z, color, SURFACE_TEXTURE_MAX_U, 0.0F, lightCoords, -1.0F, axis);
        addVertex(buffer, pose, sprite, MAX_X, surfaceY, MAX_Z, color, SURFACE_TEXTURE_MAX_U, SURFACE_TEXTURE_MAX_V, lightCoords, -1.0F, axis);
        addVertex(buffer, pose, sprite, MIN_X, surfaceY, MAX_Z, color, 0.0F, SURFACE_TEXTURE_MAX_V, lightCoords, -1.0F, axis);
        addVertex(buffer, pose, sprite, MIN_X, surfaceY, MIN_Z, color, 0.0F, 0.0F, lightCoords, -1.0F, axis);
    }

    private static void addVertex(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            float x,
            float y,
            float z,
            int color,
            float u,
            float v,
            int lightCoords,
            float normalY,
            Direction.Axis axis
    ) {
        // The X block-state variant applies a clockwise 90-degree Y rotation;
        // the Z variant and the item geometry keep the base orientation.
        float transformedX = axis == Direction.Axis.X ? 1.0F - z : x;
        float transformedY = y;
        float transformedZ = axis == Direction.Axis.X ? x : z;

        buffer.addVertex(pose, transformedX, transformedY, transformedZ)
                .setColor(color)
                .setUv(sprite.getU(u), sprite.getV(v))
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(lightCoords)
                .setNormal(pose, 0.0F, normalY, 0.0F);
    }
}
