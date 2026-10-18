from pathlib import Path
import shutil, sys
p=Path('src/main/java/meteordevelopment/meteorclient/gui/screens/LovenoteModulesScreen.java')
if not p.is_file(): sys.exit('ERROR: Run from the Minecraft project root (folder containing gradlew).')
s=p.read_text(encoding='utf-8')
if 'DRainer_STAGE2_THREE_COLUMN' in s: sys.exit('Stage 2 three-column patch already installed.')
def replace(old,new, count=1):
 global s
 n=s.count(old)
 if n != count: sys.exit(f'ERROR: Expected {count} occurrence(s), found {n}: {old[:110]!r}. No files changed.')
 s=s.replace(old,new,count)
replace('    private int contentW() { return winW - railW - 28; }', '''    // DRainer_STAGE2_THREE_COLUMN: fixed space for settings, even when no module is selected.
    private int premiumGap() { return 12; }
    private int premiumSettingsW() { return Math.max(180, Math.min(252, (winW - railW - 42) * 40 / 100)); }
    private int premiumSettingsX() { return wx + winW - 14 - premiumSettingsW(); }
    private int contentW() { return Math.max(140, premiumSettingsX() - premiumGap() - contentX()); }''')
replace('        gs = Mth.clamp(LovenoteMenuConfig.menuScale / 100f, 0.4f, 1.1f);', '''        float desired = Mth.clamp(LovenoteMenuConfig.menuScale / 100f, 0.4f, 1.1f);
        float fitX = (width - 28f) / Math.max(1, LovenoteMenuConfig.windowWidth);
        float fitY = (height - 28f) / Math.max(1, LovenoteMenuConfig.windowHeight);
        gs = Math.max(0.1f, Math.min(desired, Math.min(fitX, fitY)));''')
replace('        if (panelModule != null && panelA > 0.01f) drawPanel(ctx, mx, my, panelA, corner);', '''        drawPremiumSettings(ctx, mx, my, panelA, corner);''')
replace('        int ncols = Mth.clamp(LovenoteMenuConfig.columns, 1, 3);','        int ncols = 1;')
replace('        boolean compact = LovenoteMenuConfig.compact;','        boolean compact = true;')
# fixed sidebar, labels remain visible, icons stay functional
replace('        int sbw = RAIL_MIN + (int) ((railW - RAIL_MIN) * sb);','        int sbw = railW;')
replace('        if (q.isEmpty() && sb > 0.05f) {','        if (q.isEmpty()) {')
replace('            if (sb > 0.05f) txt(ctx, pageLabel(i), sx + 42 + (int) slide, ry + (rowH - 8) / 2,','            if (true) txt(ctx, trim(pageLabel(i), sbw - 47), sx + 42 + (int) slide, ry + (rowH - 8) / 2,')
# Rewrite old drawPanel method only; keeps settings controls, replaces old panel dimensions.
a=s.index('    private void drawPanel(GuiGraphicsExtractor ctx, int mx, int my, float panelA, int corner) {')
b=s.index('    // ---------------------------------------------------------------- input',a)
new='''    private void drawPremiumSettings(GuiGraphicsExtractor ctx, int mx, int my, float panelA, int corner) {
        int px = premiumSettingsX();
        int pw = premiumSettingsW();
        int py = wy + 112;
        int ph = winH - 142;
        if (ph < 80) return;
        rr(ctx, px - 1, py - 1, pw + 2, ph + 2, corner, LovenotePalette.alpha(LovenotePalette.C2, 65));
        rr(ctx, px, py, pw, ph, corner, LovenotePalette.alpha(LovenotePalette.card(), 238));
        Row m = selected;
        if (m == null) {
            txtS(ctx, "MODULE SETTINGS", px + 12, py + 13, LovenotePalette.text());
            fill(ctx, px + 10, py + 36, px + pw - 10, py + 37, LovenotePalette.alpha(LovenotePalette.track(), 170));
            txt(ctx, "Select a module", px + 12, py + 52, LovenotePalette.faint());
            txt(ctx, "to edit its settings", px + 12, py + 68, LovenotePalette.dim());
            panelMax = 0;
            return;
        }
        txtS(ctx, trim(m.title(), pw - 84), px + 12, py + 11, LovenotePalette.text());
        txt(ctx, trim(m.description(), pw - 24), px + 12, py + 25, LovenotePalette.faint());
        float on = ease(m.title() + ":s", m.active() ? 1f : 0f, 170);
        drawSwitch(ctx, px + pw - 64, py + 12, on);
        addHit(px + pw - 67, py + 8, 28, 18, K_PTOGGLE, m, null, 0);
        circle(ctx, px + pw - 17, py + 17, 4, 0xFFEF4444);
        addHit(px + pw - 23, py + 10, 13, 14, K_PCLOSE, m, null, 0);
        fill(ctx, px + 10, py + 38, px + pw - 10, py + 39, LovenotePalette.alpha(LovenotePalette.track(), 170));
        int top = py + 44, bottom = py + ph - 10;
        int settingW = pw - 26;
        int total = 4 + panelTotal(m, settingW);
        panelMax = Math.max(0, total - (bottom - top));
        panelScrollTarget = Mth.clamp(panelScrollTarget, 0, panelMax);
        panelScrollShown += (panelScrollTarget - panelScrollShown) * (1f - (float) Math.exp(-dt / 40f));
        if (Math.abs(panelScrollTarget - panelScrollShown) < 0.2f) panelScrollShown = panelScrollTarget;
        int firstHit = hits.size();
        ctx.enableScissor(px, top, px + pw, bottom);
        int y = top + 4 - Math.round(panelScrollShown);
        boolean first = true;
        for (Group g : m.groups()) {
            boolean any = false;
            for (Object st : g.settings()) if (rowH(st, settingW) > 0) { any = true; break; }
            if (!any) continue;
            if (!first) fill(ctx, px + 13, y - 2, px + pw - 13, y - 1, LovenotePalette.alpha(LovenotePalette.track(), 120));
            first = false;
            y += 4;
            txt(ctx, g.name(), px + 13, y, LovenotePalette.alpha(LovenotePalette.faint(), 190));
            y += 14;
            for (Object st : g.settings()) y += drawSetting(ctx, m, st, px + 13, y, settingW);
        }
        ctx.disableScissor();
        // Scissoring does not clip hitboxes: remove invisible controls from the input list.
        for (int i = hits.size() - 1; i >= firstHit; i--) {
            Hit h = hits.get(i);
            if (h.y < top || h.y + h.h > bottom) hits.remove(i);
        }
        scrollbar(ctx, px + pw - 6, top, bottom, panelScrollShown, panelMax);
    }

'''
s=s[:a]+new+s[b:]
replace('        if (button == 0 && selected != null && mx >= panX && mx <= panX + PW && my >= panY && my <= panY + 34) {\n            dragWin = 2; grabX = (int) mx - panX; grabY = (int) my - panY; return true;\n        }\n','')
replace('        if (selected != null && x >= panX && x <= panX + PW && y >= panY && y <= panY + PH) {','        if (selected != null && x >= premiumSettingsX() && x <= premiumSettingsX() + premiumSettingsW() && y >= wy + 112 && y <= wy + winH - 30) {')
# clip module card hits to viewport, no hidden cards intercept clicks
replace('            addHit(x, ly, cardW, cardH,\n                K_CARD, m, null, 0);\n\n            addHit(switchX - 4, switchY - 5,\n                34, 22, K_SWITCH, m, null, 0);','''            if (ly >= top && ly + cardH <= bottom) {
                addHit(x, ly, cardW, cardH, K_CARD, m, null, 0);
                addHit(switchX - 4, switchY - 5, 34, 22, K_SWITCH, m, null, 0);
            }''')
backup=p.with_name(p.name+'.before-premium-stage2-real-fix')
if not backup.exists(): shutil.copy2(p,backup)
p.write_text(s,encoding='utf-8')
print('SUCCESS: Actual 3-column layout installed. Backup:',backup)
print('Updated:',p)
