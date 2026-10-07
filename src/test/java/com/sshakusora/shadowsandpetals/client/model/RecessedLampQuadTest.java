package com.sshakusora.shadowsandpetals.client.model;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("DataFlowIssue")
class RecessedLampQuadTest {
    @Test
    void clipsAndTranslatesEachCornerWithoutStretchingItsTexture() {
        try (TestSprite sprite = new TestSprite("off")) {
            BakedQuad source = surface(sprite);
            for (var corner : RecessedLampConnectedBlockStateModel.Corner.values()) {
                var quads = RecessedLampConnectedBlockStateModel.clipAndTranslate(source, corner, -0.5F);
                assertEquals(1, quads.size());
                BakedQuad clipped = quads.getFirst();
                for (int i = 0; i < 4; i++) {
                    var pos = clipped.position(i);
                    assertEquals(-0.5F, pos.y());
                    assertTrue(pos.x() >= 0 && pos.x() <= 1);
                    assertTrue(pos.z() >= 0 && pos.z() <= 1);
                    assertEquals(pos.x() - corner.offsetX, UVPair.unpackU(clipped.packedUV(i)), 0.00001F);
                    assertEquals(pos.z() - corner.offsetZ, UVPair.unpackV(clipped.packedUV(i)), 0.00001F);
                }
                assertSame(source.materialInfo(), clipped.materialInfo());
                assertSame(source.direction(), clipped.direction());
            }
        }
    }

    @Test
    void opaqueLampFramesStayInTheLampCutoutPass() {
        try (TestSprite sprite = new TestSprite("edge_binding")) {
            BakedQuad surface = surface(sprite);
            BakedQuad source = new BakedQuad(surface.position0(), surface.position1(), surface.position2(), surface.position3(),
                    surface.packedUV0(), surface.packedUV1(), surface.packedUV2(), surface.packedUV3(), Direction.UP,
                    new BakedQuad.MaterialInfo(sprite, ChunkSectionLayer.SOLID, null, -1, true, 0, true));
            BakedQuad clipped = RecessedLampConnectedBlockStateModel.clipAndTranslate(
                    source, RecessedLampConnectedBlockStateModel.Corner.NORTH_WEST, 0).getFirst();
            assertEquals(ChunkSectionLayer.CUTOUT, clipped.materialInfo().layer());
            assertTrue(clipped.materialInfo().shade());
            assertTrue(clipped.materialInfo().ambientOcclusion());
        }
    }

    @Test
    void onlyLitLampSurfacesBecomeUnshadedAndFullyEmissive() {
        for (String texture : new String[]{"on", "off", "edge_binding"}) {
            try (TestSprite sprite = new TestSprite(texture)) {
                var quad = RecessedLampConnectedBlockStateModel.clipAndTranslate(
                        surface(sprite), RecessedLampConnectedBlockStateModel.Corner.NORTH_WEST, 0).getFirst();
                assertEquals(texture.equals("on") ? 15 : 0, quad.materialInfo().lightEmission());
                assertEquals(!texture.equals("on"), quad.materialInfo().shade());
                assertEquals(!texture.equals("on"), quad.materialInfo().ambientOcclusion());
            }
        }
    }

    private static BakedQuad surface(TestSprite sprite) {
        return new BakedQuad(new Vector3f(0, 0, 0), new Vector3f(0, 0, 1),
                new Vector3f(1, 0, 1), new Vector3f(1, 0, 0),
                UVPair.pack(0, 0), UVPair.pack(0, 1), UVPair.pack(1, 1), UVPair.pack(1, 0),
                Direction.UP, new BakedQuad.MaterialInfo(sprite, ChunkSectionLayer.CUTOUT,
                null, -1, true, 0, true));
    }

    private static final class TestSprite extends TextureAtlasSprite {
        TestSprite(String texture) {
            super(Identifier.withDefaultNamespace("textures/atlas/blocks.png"),
                    new SpriteContents(Identifier.fromNamespaceAndPath("shadowsandpetals", "block/recessed_lamp/" + texture),
                            new FrameSize(1, 1), new NativeImage(1, 1, true)),
                    1, 1, 0, 0, 0);
        }
    }
}
