$ErrorActionPreference = "Stop"

$p = "src/main/java/com/lovenote/util/Logos.java"
$s = [IO.File]::ReadAllText($p)

# The wide atlas is separate in size from procedural logo frames.
$constants = @'
        public static final int IMAGE_W = 256;
        public static final int IMAGE_H = 96;
'@

$newBake = @'
        private static NativeImage bakeImage(int tint) {
                int sw = src.getWidth(), sh = src.getHeight();
                int[] pix = src.getPixels();

                int y0 = (int) (sh * Math.max(0f, Math.min(0.8f, cropTop)));
                int y1 = (int) (sh * (1f - Math.max(0f, Math.min(0.8f, cropBottom))));

                int minX = sw, maxX = -1, minY = sh, maxY = -1;

                for (int yy = y0; yy < y1; yy++) {
                        for (int xx = 0; xx < sw; xx++) {
                                int alpha = (pix[yy * sw + xx] >>> 24) & 255;
                                if (alpha > 12) {
                                        minX = Math.min(minX, xx);
                                        maxX = Math.max(maxX, xx);
                                        minY = Math.min(minY, yy);
                                        maxY = Math.max(maxY, yy);
                                }
                        }
                }

                if (maxX < minX) {
                        minX = 0;
                        maxX = sw - 1;
                        minY = y0;
                        maxY = Math.max(y0, y1 - 1);
                }

                int artW = Math.max(1, maxX - minX + 1);
                int artH = Math.max(1, maxY - minY + 1);

                double scale = Math.min(
                        (IMAGE_W - 8.0) / artW,
                        (IMAGE_H - 6.0) / artH
                ) * Math.max(0.2f, zoom);

                double midX = (minX + maxX + 1) * 0.5;
                double midY = (minY + maxY + 1) * 0.5;

                NativeImage img = new NativeImage(
                        IMAGE_W * FRAMES, IMAGE_H, false
                );

                int c1 = tintColour(tint, 0f);
                int c3 = tintColour(tint, 1f);

                for (int f = 0; f < FRAMES; f++) {
                        double angle = f * Math.PI * 2.0 / FRAMES;
                        double face = Math.cos(angle);
                        double projectedWidth = Math.max(0.025, Math.abs(face));
                        double light = (0.72 + 0.28 * Math.abs(face))
                                * (face < 0 ? 0.85 : 1.0);

                        for (int py = 0; py < IMAGE_H; py++) {
                                for (int px = 0; px < IMAGE_W; px++) {
                                        double ux = (px + 0.5 - IMAGE_W * 0.5)
                                                / (scale * projectedWidth);
                                        double uy = (py + 0.5 - IMAGE_H * 0.5)
                                                / scale;

                                        int sxp = (int) Math.floor(
                                                midX + ux * (face < 0 ? -1 : 1)
                                        );
                                        int syp = (int) Math.floor(midY + uy);

                                        if (sxp < minX || sxp > maxX
                                                || syp < minY || syp > maxY) {
                                                img.setPixelABGR(
                                                        px + f * IMAGE_W, py, 0
                                                );
                                                continue;
                                        }

                                        int v = pix[syp * sw + sxp];
                                        int alpha = (v >>> 24) & 255;
                                        int b = (v >>> 16) & 255;
                                        int g = (v >>> 8) & 255;
                                        int r = v & 255;

                                        if (keyBlack) {
                                                alpha = Math.min(
                                                        alpha,
                                                        (r * 30 + g * 59 + b * 11) / 100
                                                );
                                        }

                                        int col = tint == 3
                                                ? (r << 16) | (g << 8) | b
                                                : Colors.lerp(
                                                        c1, c3,
                                                        py / (float) (IMAGE_H - 1)
                                                ) & 0xFFFFFF;

                                        int rr = (int) (((col >> 16) & 255) * light);
                                        int gg = (int) (((col >> 8) & 255) * light);
                                        int bb = (int) ((col & 255) * light);

                                        img.setPixelABGR(
                                                px + f * IMAGE_W, py,
                                                argb(alpha, rr, gg, bb)
                                        );
                                }
                        }
                }

                return img;
        }

'@

$newDrawWide = @'
        public static void drawWide(
                GuiGraphicsExtractor g,
                int x, int y,
                int width, int height,
                double angle
        ) {
                ensure(IMAGE_STYLE, tintMode);

                int f = (int) Math.floor(
                        (angle / (Math.PI * 2)) * FRAMES
                ) % FRAMES;

                if (f < 0) f += FRAMES;

                Gfx.blit(
                        g, ID, x, y,
                        f * (float) IMAGE_W, 0f,
                        width, height,
                        IMAGE_W, IMAGE_H,
                        IMAGE_W * FRAMES, IMAGE_H
                );
        }

'@

$constantPattern = 'public static final int CELL\s*=\s*\d+;'
$bakePattern = '(?s)private static NativeImage bakeImage\(int tint\)\s*\{.*?(?=\s*private static int tintColour\()'
$widePattern = '(?s)public static void drawWide\(.*?(?=\s*public static boolean imageAvailable\()'

foreach ($pattern in @($constantPattern, $bakePattern, $widePattern)) {
    $count = [regex]::Matches($s, $pattern).Count
    if ($count -ne 1) {
        throw "Expected one code section, found $count. Nothing changed."
    }
}

# Work on a copy in memory; write only after validation.
$s = [regex]::Replace(
    $s, $constantPattern,
    '${0}' + "`n" + $constants, 1
)

$s = [regex]::Replace(
    $s, $bakePattern,
    [System.Text.RegularExpressions.MatchEvaluator]{
        param($m)
        return $newBake
    }
)

$s = [regex]::Replace(
    $s, $widePattern,
    [System.Text.RegularExpressions.MatchEvaluator]{
        param($m)
        return $newDrawWide
    }
)

# 32 frames reduce the atlas size and generation cost.
$s = [regex]::Replace(
    $s,
    'public static final int FRAMES\s*=\s*\d+;',
    'public static final int FRAMES = 32;'
)

# The normal square renderer is retained for procedural shapes.
# Image-style square calls are not used by the new menu logos.

Copy-Item $p "$p.before-drainer-atlas" -Force

[IO.File]::WriteAllText(
    $p, $s,
    (New-Object System.Text.UTF8Encoding($false))
)

Write-Host "SUCCESS: Proportional wide logo atlas installed."
