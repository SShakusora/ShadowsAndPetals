package com.sshakusora.shadowsandpetals.client.renderer;
public final class WoodenBarrelFluidSpecialRenderer {
 public static final WoodenBarrelFluidSpecialRenderer INSTANCE = new WoodenBarrelFluidSpecialRenderer();
 private WoodenBarrelFluidSpecialRenderer() {}
 public record RenderData(Object sprite,int color,int amount,int capacity,int lightEmission) {}
}