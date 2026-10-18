package com.lovenote.util;

/** Per-frame claims modules make on the vanilla renderer. Reset at frame start. */
public final class LovenoteRender {
        private LovenoteRender() {}

        public static boolean skyClaimed;
        public static boolean fogOverride;
        public static float fogStart = 1f;
        public static float fogEnd = 512f;
        public static int fogColor = 0xFFFFFFFF;

        public static void resetFrame() {
                skyClaimed = false;
                fogOverride = false;
        }

        public static void claimSky() {
                skyClaimed = true;
        }

        public static void overrideFog(float start, float end, int color) {
                fogOverride = true;
                fogStart = start;
                fogEnd = end;
                fogColor = color;
        }
}
