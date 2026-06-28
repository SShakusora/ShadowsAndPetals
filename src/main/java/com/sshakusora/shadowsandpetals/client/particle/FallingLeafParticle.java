package com.sshakusora.shadowsandpetals.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class FallingLeafParticle extends SingleQuadParticle {
    private static final float ACCELERATION_SCALE = 0.0025F;
    private static final int VANILLA_LEAF_LIFETIME = 300;
    private float rotSpeed;
    private final float spinAcceleration;
    private final float windBig;
    private final boolean swirl;
    private final boolean flowAway;
    private final double xaFlowScale;
    private final double zaFlowScale;
    private final double swirlPeriod;

    protected FallingLeafParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite, float fallAcceleration, float sideAcceleration, boolean swirl, boolean flowAway, float scale, float startVelocity) {
        super(level, x, y, z, sprite);
        this.rotSpeed = (float) Math.toRadians(this.random.nextBoolean() ? -30.0F : 30.0F);
        this.spinAcceleration = (float) Math.toRadians(this.random.nextBoolean() ? -5.0F : 5.0F);
        this.windBig = sideAcceleration;
        this.swirl = swirl;
        this.flowAway = flowAway;
        this.lifetime = VANILLA_LEAF_LIFETIME;
        this.gravity = fallAcceleration * 1.2F * ACCELERATION_SCALE;
        float size = scale * (this.random.nextBoolean() ? 0.05F : 0.075F);
        this.quadSize = size;
        this.setSize(size, size);
        this.friction = 1.0F;
        this.yd = -startVelocity;
        float particleRandom = this.random.nextFloat();
        this.xaFlowScale = Math.cos(Math.toRadians(particleRandom * 60.0F)) * this.windBig;
        this.zaFlowScale = Math.sin(Math.toRadians(particleRandom * 60.0F)) * this.windBig;
        this.swirlPeriod = Math.toRadians(1000.0F + particleRandom * 3000.0F);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.lifetime-- <= 0) {
            this.remove();
            return;
        }

        float aliveTicks = VANILLA_LEAF_LIFETIME - this.lifetime;
        float relativeAge = Math.min(aliveTicks / VANILLA_LEAF_LIFETIME, 1.0F);
        double xa = 0.0D;
        double za = 0.0D;
        if (this.flowAway) {
            xa += this.xaFlowScale * Math.pow(relativeAge, 1.25D);
            za += this.zaFlowScale * Math.pow(relativeAge, 1.25D);
        }

        if (this.swirl) {
            xa += relativeAge * Math.cos(relativeAge * this.swirlPeriod) * this.windBig;
            za += relativeAge * Math.sin(relativeAge * this.swirlPeriod) * this.windBig;
        }

        this.xd += xa * ACCELERATION_SCALE;
        this.zd += za * ACCELERATION_SCALE;
        this.yd -= this.gravity;
        this.rotSpeed += this.spinAcceleration / 20.0F;
        this.oRoll = this.roll;
        this.roll += this.rotSpeed / 20.0F;
        this.move(this.xd, this.yd, this.zd);
        if (this.onGround || this.lifetime < VANILLA_LEAF_LIFETIME - 1 && (this.xd == 0.0D || this.zd == 0.0D)) {
            this.remove();
            return;
        }

        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    private static Particle createVanillaLeafParticle(SpriteSet sprites, ClientLevel level, double x, double y, double z, RandomSource random, float fallAcceleration, float sideAcceleration, boolean swirl, boolean flowAway, float scale, float startVelocity) {
        return new FallingLeafParticle(level, x, y, z, sprites.get(random), fallAcceleration, sideAcceleration, swirl, flowAway, scale, startVelocity);
    }

    public static class GinkgoProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public GinkgoProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double speedX, double speedY, double speedZ, RandomSource random) {
            return createVanillaLeafParticle(this.sprites, level, x, y, z, random, 0.06F, 9.0F, true, false, 2.2F, 0.018F);
        }
    }

    public static class MapleProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public MapleProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double speedX, double speedY, double speedZ, RandomSource random) {
            return createVanillaLeafParticle(this.sprites, level, x, y, z, random, 0.09F, 8.5F, true, false, 1.8F, 0.024F);
        }
    }

    public static class SakuraProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public SakuraProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double speedX, double speedY, double speedZ, RandomSource random) {
            return createVanillaLeafParticle(this.sprites, level, x, y, z, random, 0.25F, 2.0F, false, true, 1.0F, 0.0F);
        }
    }
}
