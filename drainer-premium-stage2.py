from pathlib import Path
import shutil,sys
root=Path('src/main/java/meteordevelopment/meteorclient/gui')
p=root/'screens/LovenoteModulesScreen.java'
if not p.is_file(): sys.exit('Run from the Minecraft project root; ClickGUI file not found.')
s=p.read_text(encoding='utf-8')
if 'private boolean premiumLayout()' in s: sys.exit('Stage 2 already installed. No changes made.')
original=s

def rep(a,b,n=1):
 global s
 count=s.count(a)
 if count!=n: sys.exit(f'Patch stopped safely: expected {n} occurrence(s), found {count}: {a[:100]!r}')
 s=s.replace(a,b,n)

rep('    private void readLayout() {','''    private boolean premiumLayout() { return LovenoteMenuConfig.style.equals("Premium"); }
    private int premiumPanelW() { return Math.min(288, Math.max(230, winW * 38 / 100)); }
    private int premiumPanelX() { return wx + winW - premiumPanelW() - 12; }
    private int premiumPanelY() { return wy + 113; }
    private int premiumPanelH() { return winH - 143; }

    private void readLayout() {''')
rep('        gs = Mth.clamp(LovenoteMenuConfig.menuScale / 100f, 0.4f, 1.1f);','''        float preferred = Mth.clamp(LovenoteMenuConfig.menuScale / 100f, 0.4f, 1.1f);
        float fitX = (width - 32f) / Math.max(1, LovenoteMenuConfig.windowWidth);
        float fitY = (height - 32f) / Math.max(1, LovenoteMenuConfig.windowHeight);
        gs = Math.max(0.1f, Math.min(preferred, Math.min(fitX, fitY)));''')
rep('    private int contentW() { return winW - railW - 28; }','    private int contentW() { return winW - railW - 28 - (premiumLayout() ? premiumPanelW() + 10 : 0); }')
rep('        if (panelModule != null && panelA > 0.01f) drawPanel(ctx, mx, my, panelA, corner);','''        if (premiumLayout()) {
            if (panelModule != null && panelA > 0.01f) drawPanel(ctx, mx, my, panelA, corner);
            else drawPremiumEmptyPanel(ctx, corner);
        } else if (panelModule != null && panelA > 0.01f) drawPanel(ctx, mx, my, panelA, corner);''')
rep('com.lovenote.util.Logos.drawSmoothWide(ctx, cx + (cw - 230) / 2, wy, 230, 76, 0.0);','com.lovenote.util.Logos.drawSmoothWide(ctx, premiumLayout() ? wx + (winW - 230) / 2 : cx + (cw - 230) / 2, wy, 230, 76, 0.0);')
rep('        int ncols = Mth.clamp(LovenoteMenuConfig.columns, 1, 3);','        int ncols = premiumLayout() ? 1 : Mth.clamp(LovenoteMenuConfig.columns, 1, 3);')
rep('        boolean compact = LovenoteMenuConfig.compact;','        boolean compact = premiumLayout() || LovenoteMenuConfig.compact;')
rep('        float sb = ease("sb", hover ? 1f : 0f, LovenoteMenuConfig.railSpeed);','        float sb = premiumLayout() ? 1f : ease("sb", hover ? 1f : 0f, LovenoteMenuConfig.railSpeed);')
# Replace panel method in isolation to retain original setting rendering and interaction
start=s.index('    private void drawPanel(GuiGraphicsExtractor ctx, int mx, int my, float panelA, int corner) {')
end=s.index('    // ---------------------------------------------------------------- input',start)
block=s[start:end]
block=block.replace('        Row m = panelModule;','''        Row m = panelModule;
        boolean premium = premiumLayout();
        int panelW = premium ? premiumPanelW() : PW;
        int panelH = premium ? premiumPanelH() : PH;''',1)
block=block.replace('        float sc = 0.94f + 0.06f * panelA;','        float sc = premium ? 1f : 0.94f + 0.06f * panelA;',1)
block=block.replace('        float ccx = panX + PW / 2f, ccy = panY + PH / 2f;','        float ccx = (premium ? premiumPanelX() : panX) + panelW / 2f, ccy = (premium ? premiumPanelY() : panY) + panelH / 2f;',1)
block=block.replace('        int px = panX + (int) ((1f - panelA) * 22), py = panY;','        int px = premium ? premiumPanelX() : panX + (int) ((1f - panelA) * 22), py = premium ? premiumPanelY() : panY;',1)
block=block.replace('PW','panelW').replace('PH','panelH')
block=block.replace('int panelW = premium ? premiumPanelW() : panelW;', 'int panelW = premium ? premiumPanelW() : PW;').replace('int panelH = premium ? premiumPanelH() : panelH;', 'int panelH = premium ? premiumPanelH() : PH;')
# remove redundant traffic-light dots on premium, but preserve close hitbox
block=block.replace('        circle(ctx, px + panelW - 40, py + 17, 4, 0xFF3DD68C);','        if (!premium) circle(ctx, px + panelW - 40, py + 17, 4, 0xFF3DD68C);')
block=block.replace('        circle(ctx, px + panelW - 28, py + 17, 4, 0xFFF5B83D);','        if (!premium) circle(ctx, px + panelW - 28, py + 17, 4, 0xFFF5B83D);')
block=block.replace('        circle(ctx, px + panelW - 16, py + 17, 4, 0xFFEF4444);','        if (!premium) circle(ctx, px + panelW - 16, py + 17, 4, 0xFFEF4444);')
block=block.replace('        addHit(px + panelW - 22, py + 11, 12, 12, K_PCLOSE, m, null, 0);','''        if (premium) txt(ctx, "x", px + panelW - 18, py + 12, LovenotePalette.faint());
        addHit(px + panelW - 22, py + 11, 12, 12, K_PCLOSE, m, null, 0);''')
s=s[:start]+'''    private void drawPremiumEmptyPanel(GuiGraphicsExtractor ctx, int corner) {
        int x = premiumPanelX(), y = premiumPanelY();
        int w = premiumPanelW(), h = premiumPanelH();
        rr(ctx, x, y, w, h, corner, LovenotePalette.alpha(LovenotePalette.card(), 230));
        rr(ctx, x + 1, y + 1, w - 2, h - 2, corner, LovenotePalette.alpha(LovenotePalette.C2, 16));
        txtS(ctx, "Module settings", x + 14, y + 16, LovenotePalette.text());
        fill(ctx, x + 12, y + 39, x + w - 12, y + 40, LovenotePalette.alpha(LovenotePalette.C2, 70));
        txt(ctx, "Select a module to edit", x + 14, y + 60, LovenotePalette.faint());
        txt(ctx, "its settings here.", x + 14, y + 75, LovenotePalette.faint());
    }

'''+block+s[end:]
rep('        if (button == 0 && selected != null && mx >= panX && mx <= panX + PW && my >= panY && my <= panY + 34) {','        if (!premiumLayout() && button == 0 && selected != null && mx >= panX && mx <= panX + PW && my >= panY && my <= panY + 34) {')
rep('        if (selected != null && x >= panX && x <= panX + PW && y >= panY && y <= panY + PH) {','''        if (selected != null && (premiumLayout()
                ? x >= premiumPanelX() && x <= premiumPanelX() + premiumPanelW()
                    && y >= premiumPanelY() && y <= premiumPanelY() + premiumPanelH()
                : x >= panX && x <= panX + PW && y >= panY && y <= panY + PH)) {''')
# prevent clicks on blank settings panel from starting dragging the main window (only top header draggable already)
# don't let setting hitboxes behind scroll clipped region be clickable (existing drawSetting behavior unchanged)
backup=p.with_suffix('.java.before-premium-stage2')
if backup.exists(): sys.exit(f'Backup already exists: {backup}; not overwriting.')
shutil.copy2(p,backup)
p.write_text(s,encoding='utf-8')
print('Stage 2 applied. Backup:',backup)
print('Changed only:',p)
print('Run ./gradlew compileJava and inspect GUI.')
