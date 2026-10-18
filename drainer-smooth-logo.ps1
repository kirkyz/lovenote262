$ErrorActionPreference = "Stop"

$p = "src/main/java/com/lovenote/util/Logos.java"
$s = [IO.File]::ReadAllText($p)

$anchor = 'public static boolean imageAvailable()'

if (!$s.Contains($anchor)) {
    throw "Cannot find logo insertion point. Nothing changed."
}

if ($s.Contains('public static void drawSmoothWide(')) {
    throw "Smooth renderer already installed. Nothing changed."
}

$method = @'
        private static final Identifier SMOOTH_ID =
                Identifier.fromNamespaceAndPath("lovenote", "gen/drainer_smooth");

        private static boolean smoothLoaded = false;

        private static void ensureSmooth() {
                if (smoothLoaded) return;

                Minecraft mc = Minecraft.getInstance();
                if (mc == null) return;

                try {
                        RenderSystem.assertOnRenderThread();
                        loadSource();
                        if (src == null) return;

                        NativeImage image = new NativeImage(
                                src.getWidth(), src.getHeight(), false
                        );

                        int[] pixels = src.getPixels();

                        for (int y = 0; y < src.getHeight(); y++) {
                                for (int x = 0; x < src.getWidth(); x++) {
                                        image.setPixelABGR(
                                                x, y,
                                                pixels[y * src.getWidth() + x]
                                        );
                                }
                        }

                        mc.getTextureManager().register(
                                SMOOTH_ID,
                                new DynamicTexture(() -> "drainer_smooth", image)
                        );

                        smoothLoaded = true;
                } catch (Throwable t) {
                        t.printStackTrace();
                }
        }

        public static void drawSmoothWide(
                GuiGraphicsExtractor g,
                int x, int y,
                int width, int height,
                double angle
        ) {
                ensureSmooth();
                if (!smoothLoaded || src == null) return;

                double face = Math.cos(angle);
                double projection = Math.max(0.02, Math.abs(face));

                int projectedWidth = Math.max(
                        1, (int) Math.round(width * projection)
                );

                int projectedX = x + (width - projectedWidth) / 2;

                Gfx.blit(
                        g,
                        SMOOTH_ID,
                        projectedX, y,
                        0f, 0f,
                        projectedWidth, height,
                        src.getWidth(), src.getHeight(),
                        src.getWidth(), src.getHeight()
                );
        }

'@

# Insert before the existing imageAvailable method.
$s = $s.Replace($anchor, $method + $anchor)

Copy-Item $p "$p.before-smooth-logo" -Force

[IO.File]::WriteAllText(
    $p, $s,
    (New-Object System.Text.UTF8Encoding($false))
)

Write-Host "SUCCESS: Smooth logo renderer installed."
