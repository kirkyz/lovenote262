package com.lovenote.util;

/** Stable world-grid positions. No player-relative coordinates or per-frame allocations. */
public final class WorldFxGrid {
    private WorldFxGrid() {}
    public static int cell(double coordinate, double span) {return (int)Math.floor(coordinate/span);}
    public static float hash(int x, int y, int z, int seed) {
        int h = x * 0x9E3779B9 ^ y * 0x85EBCA6B ^ z * 0xC2B2AE35 ^ seed * 0x27D4EB2F;
        h ^= h >>> 16; h *= 0x7FEB352D; h ^= h >>> 15; h *= 0x846CA68B; h ^= h >>> 16;
        return (h & 0x00ffffff) / 16777216f;
    }
    public static float edgeFade(double dx, double dy, double dz, double range) {
        double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);
        double fade = Math.min(1.0, Math.max(0.0, (range-distance)/Math.max(0.6,range*0.32)));
        return (float)(fade*fade*(3-2*fade));
    }
}
