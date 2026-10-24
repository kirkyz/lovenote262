package com.lovenote.util;

/** Small allocation-free palette math shared by Drainer sky layers. */
public final class SkyColorMath {
    private SkyColorMath() {}

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Hues wrap around the colour wheel. Inputs use degrees and unit saturation/value. */
    public static int hsv(float degrees, float saturation, float value) {
        float h = (degrees % 360f + 360f) % 360f / 60f;
        float s = clamp(saturation, 0f, 1f), v = clamp(value, 0f, 1f);
        float c = v * s;
        float x = c * (1f - Math.abs(h % 2f - 1f));
        float m = v - c;
        float r = 0, g = 0, b = 0;
        if (h < 1) { r=c; g=x; }
        else if (h < 2) { r=x; g=c; }
        else if (h < 3) { g=c; b=x; }
        else if (h < 4) { g=x; b=c; }
        else if (h < 5) { r=x; b=c; }
        else { r=c; b=x; }
        return 0xFF000000 | (Math.round((r+m)*255f)<<16) | (Math.round((g+m)*255f)<<8) | Math.round((b+m)*255f);
    }

    public static int mix(int a, int b, float weight) {
        float t = clamp(weight, 0f, 1f);
        int r = Math.round(((a>>16)&255)*(1f-t)+((b>>16)&255)*t);
        int g = Math.round(((a>>8)&255)*(1f-t)+((b>>8)&255)*t);
        int blue = Math.round((a&255)*(1f-t)+(b&255)*t);
        return 0xFF000000 | (r<<16) | (g<<8) | blue;
    }

    public static int brightness(int color, float factor) {
        float f = clamp(factor,0f,2f);
        int r = Math.min(255,Math.round(((color>>16)&255)*f));
        int g = Math.min(255,Math.round(((color>>8)&255)*f));
        int b = Math.min(255,Math.round((color&255)*f));
        return 0xFF000000 | (r<<16) | (g<<8) | b;
    }
}
