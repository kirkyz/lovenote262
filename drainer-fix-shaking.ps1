$ErrorActionPreference = "Stop"

$p = "src/main/java/com/lovenote/util/Logos.java"
$s = [IO.File]::ReadAllText($p)

$pattern = '(?s)public static void drawSmoothWide\(\s*GuiGraphicsExtractor g,.*?\n\s*\}\s*(?=public static boolean imageAvailable\(\))'

$replacement = @'
        public static void drawSmoothWide(
                GuiGraphicsExtractor g,
                int x, int y,
                int width, int height,
                double angle
        ) {
                ensureSmooth();
                if (!smoothLoaded || src == null) return;

                // Continuous horizontal projection, without integer-width jumps.
                float projection = (float) Math.cos(angle);

                // Avoid a zero-width texture when viewed edge-on.
                if (Math.abs(projection) < 0.015f) {
                        projection = projection < 0f ? -0.015f : 0.015f;
                }

                var pose = g.pose();

                pose.pushMatrix();
                try {
                        float centerX = x + width * 0.5f;

                        pose.translate(centerX, (float) y);

                        // Negative values mirror the image during the back half
                        // of the rotation, simulating a full Y-axis turn.
                        pose.scale(projection, 1.0f);

                        Gfx.blit(
                                g,
                                SMOOTH_ID,
                                -width / 2,
                                0,
                                0f, 0f,
                                width, height,
                                src.getWidth(), src.getHeight(),
                                src.getWidth(), src.getHeight()
                        );
                } finally {
                        pose.popMatrix();
                }
        }

'@

$count = [regex]::Matches($s, $pattern).Count

if ($count -ne 1) {
    throw "Expected one smooth logo method, found $count. Nothing changed."
}

Copy-Item $p "$p.before-shake-fix" -Force

$s = [regex]::Replace(
    $s,
    $pattern,
    [System.Text.RegularExpressions.MatchEvaluator]{
        param($m)
        return $replacement
    }
)

[IO.File]::WriteAllText(
    $p,
    $s,
    (New-Object System.Text.UTF8Encoding($false))
)

Write-Host "SUCCESS: Subpixel logo animation installed."
