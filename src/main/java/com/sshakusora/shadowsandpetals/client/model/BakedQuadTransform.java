package com.sshakusora.shadowsandpetals.client.model;

import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.quad.MutableQuad;
import org.joml.Matrix4f;

/** Small shared vertex transform used by dynamic block models. */
public final class BakedQuadTransform {
    private BakedQuadTransform() {
    }

    public static BakedQuad rotateY(BakedQuad quad, Vec3 pivot, float degrees) {
        Matrix4f transform = new Matrix4f()
                .translate((float) pivot.x, (float) pivot.y, (float) pivot.z)
                .rotateY((float) Math.toRadians(degrees))
                .translate((float) -pivot.x, (float) -pivot.y, (float) -pivot.z);
        return new MutableQuad()
                .setFrom(quad)
                .transform(transform)
                .recomputeNormals(true)
                .toBakedQuad();
    }
}
