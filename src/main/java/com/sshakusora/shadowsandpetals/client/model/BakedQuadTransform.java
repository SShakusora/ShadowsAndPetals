package com.sshakusora.shadowsandpetals.client.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** Small shared vertex transform used by dynamic block models. */
public final class BakedQuadTransform {
    private BakedQuadTransform() {
    }

    public static BakedQuad rotateY(BakedQuad quad, Vec3 pivot, float degrees) {
        int[] vertices = quad.getVertices().clone();
        Direction direction = rotateDirection(quad.getDirection(), degrees);
        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * 8;
            float x = Float.intBitsToFloat(vertices[offset]);
            float y = Float.intBitsToFloat(vertices[offset + 1]);
            float z = Float.intBitsToFloat(vertices[offset + 2]);
            float[] point = rotatePoint(x, y, z, pivot, degrees);
            vertices[offset] = Float.floatToRawIntBits(point[0]);
            vertices[offset + 1] = Float.floatToRawIntBits(point[1]);
            vertices[offset + 2] = Float.floatToRawIntBits(point[2]);
            vertices[offset + 7] = rotateNormal(vertices[offset + 7], degrees);
        }
        return new BakedQuad(
                vertices,
                quad.getTintIndex(),
                direction,
                quad.getSprite(),
                quad.isShade(),
                quad.hasAmbientOcclusion()
        );
    }

    private static float[] rotatePoint(
            float x,
            float y,
            float z,
            Vec3 pivot,
            float degrees
    ) {
        float px = x - (float) pivot.x;
        float pz = z - (float) pivot.z;
        double radians = Math.toRadians(degrees);
        float sin = (float) Math.sin(radians);
        float cos = (float) Math.cos(radians);
        return new float[]{
                px * cos + pz * sin + (float) pivot.x,
                y,
                -px * sin + pz * cos + (float) pivot.z
        };
    }

    private static Direction rotateDirection(Direction direction, float degrees) {
        double radians = Math.toRadians(degrees);
        float sin = (float) Math.sin(radians);
        float cos = (float) Math.cos(radians);
        float x = direction.getStepX() * cos + direction.getStepZ() * sin;
        float z = -direction.getStepX() * sin + direction.getStepZ() * cos;
        return Direction.getNearest(x, direction.getStepY(), z);
    }

    private static int rotateNormal(int packed, float degrees) {
        float x = ((byte) (packed & 0xFF)) / 127.0F;
        float y = ((byte) ((packed >>> 8) & 0xFF)) / 127.0F;
        float z = ((byte) ((packed >>> 16) & 0xFF)) / 127.0F;
        double radians = Math.toRadians(degrees);
        float sin = (float) Math.sin(radians);
        float cos = (float) Math.cos(radians);
        float nx = x * cos + z * sin;
        float nz = -x * sin + z * cos;
        int packedX = Math.round(nx * 127.0F) & 0xFF;
        int packedY = Math.round(y * 127.0F) & 0xFF;
        int packedZ = Math.round(nz * 127.0F) & 0xFF;
        return packedX | (packedY << 8) | (packedZ << 16);
    }
}
