$ErrorActionPreference = "Stop"

$path = "src/main/java/meteordevelopment/meteorclient/gui/screens/LovenoteModulesScreen.java"
$full = Join-Path (Get-Location).Path $path
$s = [System.IO.File]::ReadAllText($full)

$startMarker = "    private void renderCards("
$endMarker = "    private void drawRail("

$start = $s.IndexOf($startMarker)
$end = $s.IndexOf($endMarker, $start)

if ($start -lt 0 -or $end -lt 0) {
    throw "Could not locate the card renderer. No changes made."
}

# Preserve the trim helper and any methods between renderCards and drawRail.
$trimMarker = "    private String trim("
$trimStart = $s.IndexOf($trimMarker, $start)

if ($trimStart -lt 0 -or $trimStart -gt $end) {
    throw "Could not locate trim helper. No changes made."
}

$replacement = @'
    private void renderCards(GuiGraphicsExtractor ctx, int mx, int my,
                             int cx, int top, int cw, int bottom,
                             String q, boolean interactive, int corner) {
        List<Row> list = visibleModules(q);

        int ncols = Mth.clamp(LovenoteMenuConfig.columns, 1, 3);
        boolean compact = LovenoteMenuConfig.compact;
        int gap = 9;
        int cardW = (cw - (ncols - 1) * gap) / ncols;
        int cardH = compact ? 54 : 82;

        int rows = (list.size() + ncols - 1) / ncols;
        maxScroll = Math.max(0, rows * (cardH + gap) - (bottom - top));

        scrollTarget = Mth.clamp(scrollTarget, 0, maxScroll);
        scrollShown += (scrollTarget - scrollShown)
            * (1f - (float) Math.exp(-dt / 40f));

        if (Math.abs(scrollTarget - scrollShown) < 0.2f)
            scrollShown = scrollTarget;

        if (mx > cx - 6 && mx < cx + cw + 12
            && my > top && my < bottom)
            sbAlpha = 1f;

        int cr = Math.max(7, corner);

        ctx.enableScissor(cx - 2, top - 2, cx + cw + 4, bottom);

        for (int i = 0; i < list.size(); i++) {
            Row m = list.get(i);

            int col = i % ncols;
            int x = cx + col * (cardW + gap);
            int y = top + (i / ncols) * (cardH + gap)
                - Math.round(scrollShown);

            if (y + cardH < top - 8 || y > bottom + 8)
                continue;

            float st = stagger(i);
            int sy = y + (int) ((1f - st) * 10f);

            boolean hov = interactive
                && mx >= x && mx < x + cardW
                && my >= sy && my < sy + cardH
                && my >= top && my <= bottom;

            float hv = ease(m.title() + ":h", hov ? 1f : 0f, 150);
            float on = ease(m.title() + ":s", m.active() ? 1f : 0f, 180);
            float sel = ease(m.title() + ":sel",
                selected == m ? 1f : 0f, 180);

            int ly = sy - (int) (hv * LovenoteMenuConfig.hoverLift);
            int accent = LovenotePalette.wave(i * 0.07f);

            // Animated breathing glow
            float pulse = (float) (
                0.5 + 0.5 * Math.sin(
                    System.currentTimeMillis() / 650.0 + i * 0.65
                )
            );

            if (on > 0.01f || hv > 0.01f || sel > 0.01f) {
                int glowAlpha = (int) (
                    22 + 40 * pulse * on + 65 * hv + 75 * sel
                );

                rr(ctx, x - 2, ly - 2,
                    cardW + 4, cardH + 4, cr + 2,
                    LovenotePalette.alpha(accent, glowAlpha));
            }

            // Glass card surface
            rr(ctx, x, ly, cardW, cardH, cr,
                LovenotePalette.lerp(
                    LovenotePalette.card(),
                    LovenotePalette.cardHover(), hv));

            // Accent line
            if (on > 0.01f) {
                rr(ctx, x + 12, ly + 1, cardW - 24, 2, 1,
                    LovenotePalette.alpha(accent,
                        (int) (110 * on)));
            }

            // Rounded illuminated icon tile
            int tile = compact ? 25 : 32;
            int ix = x + 12;
            int iy = ly + 11;

            rr(ctx, ix, iy, tile, tile, 8,
                LovenotePalette.alpha(accent,
                    (int) (65 + 70 * on + 40 * hv)));

            rr(ctx, ix + 2, iy + 2,
                tile - 4, tile - 4, 6,
                LovenotePalette.alpha(
                    LovenotePalette.card(), 160));

            // Minimal geometric module emblem
            circle(ctx, ix + tile / 2, iy + tile / 2,
                compact ? 5 : 6,
                LovenotePalette.alpha(accent, 235));

            circle(ctx, ix + tile / 2, iy + tile / 2,
                2, LovenotePalette.card());

            // Module title
            int textX = ix + tile + 9;
            int textW = Math.max(10, cardW - (textX - x) - 12);

            String name = trim(m.title(), textW);

            txt(ctx, name, textX, ly + 13,
                LovenotePalette.text());

            // Description
            if (!compact && LovenoteMenuConfig.descriptions) {
                txt(ctx,
                    trim(m.description(), Math.max(10, cardW - 26)),
                    x + 13, ly + 49,
                    LovenotePalette.dim());
            }

            // Status and animated switch
            String status = m.active() ? "ENABLED" : "DISABLED";

            txt(ctx, status, x + 13, ly + cardH - 17,
                LovenotePalette.lerp(
                    LovenotePalette.faint(), accent, on));

            int switchX = x + cardW - 35;
            int switchY = ly + cardH - 19;

            drawSwitch(ctx, switchX, switchY, on);

            // Existing interaction handling
            addHit(x, ly, cardW, cardH,
                K_CARD, m, null, 0);

            addHit(switchX - 4, switchY - 5,
                34, 22, K_SWITCH, m, null, 0);

            if (hov && LovenoteMenuConfig.tooltips
                && !name.equals(m.title()))
                tooltip = m.title();
        }

        ctx.disableScissor();

        scrollbar(ctx, cx + cw + 6,
            top, bottom, scrollShown, maxScroll);

        if (list.isEmpty())
            txt(ctx, "No modules found",
                cx + 4, top + 8, LovenotePalette.faint());
    }

'@

Copy-Item $full "$full.before-modern-cards" -Force

# Only replace renderCards, keeping trim(), theme editor and other methods.
$s = $s.Substring(0, $start) + $replacement + $s.Substring($trimStart)

[System.IO.File]::WriteAllText(
    $full, $s, [System.Text.UTF8Encoding]::new($false)
)

Write-Host "SUCCESS: Modern cards installed."
