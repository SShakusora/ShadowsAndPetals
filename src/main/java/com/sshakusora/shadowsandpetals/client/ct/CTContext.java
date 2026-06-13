package com.sshakusora.shadowsandpetals.client.ct;

/**
 * Describes which of the eight surrounding directions are connected for a given
 * block face. Used by {@link CTTextureType} to determine the correct sub-tile
 * index within the connected texture atlas.
 */
public final class CTContext {
    public boolean up, down, left, right;
    public boolean topLeft, topRight, bottomLeft, bottomRight;

    public static final CTContext EMPTY = new CTContext();
}
