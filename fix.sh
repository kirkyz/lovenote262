#!/usr/bin/env bash
set -u
REPO=$(find /workspaces -maxdepth 3 -name gradlew 2>/dev/null | head -1 | xargs -r dirname)
[ -z "$REPO" ] && { echo "!! repo not found"; exit 1; }
SRC="$REPO/src"
echo "repo: $REPO"
f() { find "$SRC" -name "$1" 2>/dev/null | head -1; }
addimp() { [ -f "$1" ] || return; grep -qF "$2" "$1" || sed -i "/^package /a $2" "$1"; }

P=$(f RenderUtil.java)
if [ -n "$P" ]; then
  sed -i -e :a -e '/^\n*$/{$d;N;ba' -e '}' "$P"
  head -n -1 "$P" > /tmp/ru.java
  cat >> /tmp/ru.java <<'JAVA'

    public static VertexConsumerProvider.Immediate imBuffers() {
        return MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
    }
    public static VertexConsumer imTex(Identifier id) {
        return imBuffers().getBuffer(RenderLayer.getTextSeeThrough(id));
    }
    public static VertexConsumer imQuads() {
        return imBuffers().getBuffer(RenderLayer.getDebugQuads());
    }
    public static void imFlush() { imBuffers().draw(); }
}
JAVA
  mv /tmp/ru.java "$P"
  addimp "$P" "import net.minecraft.client.render.VertexConsumer;"
  addimp "$P" "import net.minecraft.client.render.VertexConsumerProvider;"
  addimp "$P" "import net.minecraft.client.render.RenderLayer;"
  echo "ok RenderUtil"
fi

python3 - "$SRC" <<'PY'
import sys, re, pathlib
src = pathlib.Path(sys.argv[1])
names = ["SkyShader.java","Starfield.java","ParticleField.java","WorldTint.java","Bloom.java","SunGlow.java"]
STATE = r'RenderSystem\.(enableBlend|disableBlend|defaultBlendFunc|enableCull|disableCull|enableDepthTest|disableDepthTest|depthMask|setShaderFog\w+)\([^)]*\)'
for n in names:
    hits = list(src.rglob(n))
    if not hits: print("MISS", n); continue
    p = hits[0]
    out, pending, var, textured = [], None, None, False
    for line in p.read_text().splitlines():
        s = line.strip()
        m = re.match(r'RenderSystem\.setShaderTexture\(0,\s*(.+?)\);\s*$', s)
        if m: pending = m.group(1); continue
        m = re.match(r'(\w+)\s*=\s*Tessellator\.getInstance\(\)\.getBuffer\(\);\s*$', s)
        if m:
            var = m.group(1); textured = pending is not None
            expr = ("RenderUtil.imTex(%s)" % pending) if textured else "RenderUtil.imQuads()"
            out.append("        VertexConsumer %s = %s;" % (var, expr))
            pending = None; continue
        if var and re.match(re.escape(var) + r'\.begin\(.*\);\s*$', s): continue
        if 'Tessellator.getInstance().draw();' in s:
            out.append(line.replace('Tessellator.getInstance().draw();','RenderUtil.imFlush();'))
            textured, var = False, None; continue
        if re.match(r'RenderSystem\.setShader\(.*\);\s*$', s): continue
        if re.match(r'^\s*' + STATE + r';\s*$', line): continue
        if re.match(r'^\s*if \(.*\)\s*' + STATE + r';\s*$', line):
            out.append(re.sub(STATE + r';', '{ }', line)); continue
        if '.next();' in line:
            line = line.replace('.next();', '.light(LightmapTextureManager.MAX_LIGHT_COORDINATE);' if textured else ';')
        out.append(line)
    t = "\n".join(out) + "\n"
    t = re.sub(r'mc\.world\.getDimensionEffects\(\)\.getSkyType\(\)\s*!=\s*[A-Za-z0-9_.]+\.SkyType\.NORMAL', 'true', t)
    t = re.sub(r'^\s*import .*\.DimensionEffects;\s*$', '', t, flags=re.M)
    t = re.sub(r'\.getSkyAngle(?:Radians)?\([^)]*\)', '.getSkyAngleRadians()', t)
    p.write_text(t); print("ok", n)
PY

for FILE in SkyShader Starfield ParticleField WorldTint Bloom SunGlow; do
  P=$(f "$FILE.java"); [ -z "$P" ] && continue
  addimp "$P" "import net.minecraft.client.render.VertexConsumer;"
  addimp "$P" "import net.minecraft.client.render.LightmapTextureManager;"
done

P=$(f BackgroundRendererMixin.java)
if [ -n "$P" ]; then
  sed -i 's#^import net\.minecraft\.client\.render\.BackgroundRenderer;#import net.minecraft.client.render.fog.FogRenderer;#' "$P"
  sed -i 's/@Mixin(BackgroundRenderer\.class)/@Mixin(FogRenderer.class)/' "$P"
  echo "ok BackgroundRendererMixin"
fi

P=$(f SkyShader.java);  [ -n "$P" ] && sed -i '/^package /a import net.minecraft.client.render.DimensionEffects;' "$P"
P=$(f Particles.java);  [ -n "$P" ] && sed -i 's#^import net\.minecraft\.client\.option\.ParticlesMode;#import net.minecraft.client.particle.ParticlesMode;#' "$P"
P=$(f AutoArmor.java);  [ -n "$P" ] && sed -i 's#^import net\.minecraft\.item\.equipment\.ArmorItem;#import net.minecraft.item.ArmorItem;#' "$P"
P=$(f AutoPotion.java); [ -n "$P" ] && sed -i 's#^import net\.minecraft\.component\.type\.PotionContents;#import net.minecraft.component.type.PotionContentsComponent;#' "$P"

P=$(f FastFall.java);      [ -n "$P" ] && sed -i 's/\.isFallFlying()/.isGliding()/g' "$P"
P=$(f TabList.java);       [ -n "$P" ] && sed -i 's/\.getProfile()\.getName()/.getProfile().name()/g' "$P"
P=$(f PotionsWidget.java); [ -n "$P" ] && sed -i 's/\.getEffectType()\.getName()/.getEffectType().value().getName()/g; s/\.getEffectType()\.isBeneficial()/.getEffectType().value().isBeneficial()/g' "$P"
P=$(f SpeedWidget.java);   [ -n "$P" ] && sed -i 's/\.prevX/.lastX/g; s/\.prevY/.lastY/g; s/\.prevZ/.lastZ/g' "$P"
P=$(f Backtrack.java);     [ -n "$P" ] && sed -i 's/\.getPos()/.getEntityPos()/g' "$P"
P=$(f Freelook.java);      [ -n "$P" ] && sed -i 's/InputUtil\.isKeyPressed(mc\.getWindow()\.getHandle(),/InputUtil.isKeyPressed(mc.getWindow(),/g' "$P"
P=$(f AutoTool.java);      [ -n "$P" ] && sed -i 's/\.selectedSlot = \([^;]*\);/\.setSelectedSlot(\1);/g; s/\.selectedSlot/\.getSelectedSlot()/g' "$P"
P=$(f Criticals.java);     [ -n "$P" ] && sed -i 's/PositionAndOnGround(\(.*\), false)/PositionAndOnGround(\1, false, false)/g' "$P"
P=$(f PopEffects.java);    [ -n "$P" ] && sed -i 's/addParticle(ParticleTypes\.TOTEM_OF_UNDYING,/addParticle(ParticleTypes.TOTEM_OF_UNDYING, false, false,/g' "$P"
P=$(f HitParticles.java);  [ -n "$P" ] && sed -i 's/addParticle(ParticleUtil\.get(particle\.get()),/addParticle(ParticleUtil.get(particle.get()), false, false,/g' "$P"
P=$(f TargetStrafe.java);  [ -n "$P" ] && sed -i 's/\.input\.movementSideways = \([^;]*\);/\.input.setMovementSideways(\1);/g; s/\.input\.movementForward = \([^;]*\);/\.input.setMovementForward(\1);/g' "$P"
P=$(f Sprint.java);        [ -n "$P" ] && sed -i 's/\.input\.movementForward/.input.getMovementForward()/g; s/\.input\.movementSideways/.input.getMovementSideways()/g' "$P"
P=$(f KeybindsWidget.java);[ -n "$P" ] && sed -i 's/InputUtil\.fromKeyCode(\([^,]*\), -1)/InputUtil.fromKeyCode(InputUtil.Type.KEYSYM.createFromCode(\1))/g' "$P"
P=$(f LovenoteScreen.java);[ -n "$P" ] && sed -i 's/InputUtil\.fromKeyCode(\([^,]*\), -1)/InputUtil.fromKeyCode(InputUtil.Type.KEYSYM.createFromCode(\1))/g' "$P"
P=$(f WorldTint.java);     [ -n "$P" ] && sed -i 's/ctx\.getMatrices()\.peek()\.getPositionMatrix()/new Matrix4f()/g' "$P"
P=$(f TargetHud.java)
if [ -n "$P" ]; then
  sed -i 's/MatrixStack m = ctx\.getMatrices();/Matrix3x2fStack m = ctx.getMatrices();/' "$P"
  sed -i 's/\bm\.push()/m.pushMatrix()/g; s/\bm\.pop()/m.popMatrix()/g' "$P"
  sed -i 's/m\.translate(\([^,]*\), \([^,]*\), 0)/m.translate(\1, \2)/g' "$P"
  sed -i 's/m\.scale(\([^,]*\), \([^,]*\), 1)/m.scale(\1, \2)/g' "$P"
fi

for FILE in HudWidget LovenoteScreen; do
  P=$(f "$FILE.java
cd /workspaces/lovenote262
cat > fix.sh <<'FIXEOF'
#!/usr/bin/env bash
set -u
REPO=$(find /workspaces -maxdepth 3 -name gradlew 2>/dev/null | head -1 | xargs -r dirname)
[ -z "$REPO" ] && { echo "!! repo not found"; exit 1; }
SRC="$REPO/src"
echo "repo: $REPO"
f() { find "$SRC" -name "$1" 2>/dev/null | head -1; }
addimp() { [ -f "$1" ] || return; grep -qF "$2" "$1" || sed -i "/^package /a $2" "$1"; }

P=$(f RenderUtil.java)
if [ -n "$P" ]; then
  sed -i -e :a -e '/^\n*$/{$d;N;ba' -e '}' "$P"
  head -n -1 "$P" > /tmp/ru.java
  cat >> /tmp/ru.java <<'JAVA'

    public static VertexConsumerProvider.Immediate imBuffers() {
        return MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
    }
    public static VertexConsumer imTex(Identifier id) {
        return imBuffers().getBuffer(RenderLayer.getTextSeeThrough(id));
    }
    public static VertexConsumer imQuads() {
        return imBuffers().getBuffer(RenderLayer.getDebugQuads());
    }
    public static void imFlush() { imBuffers().draw(); }
}
JAVA
  mv /tmp/ru.java "$P"
  addimp "$P" "import net.minecraft.client.render.VertexConsumer;"
  addimp "$P" "import net.minecraft.client.render.VertexConsumerProvider;"
  addimp "$P" "import net.minecraft.client.render.RenderLayer;"
  echo "ok RenderUtil"
fi

python3 - "$SRC" <<'PY'
import sys, re, pathlib
src = pathlib.Path(sys.argv[1])
names = ["SkyShader.java","Starfield.java","ParticleField.java","WorldTint.java","Bloom.java","SunGlow.java"]
STATE = r'RenderSystem\.(enableBlend|disableBlend|defaultBlendFunc|enableCull|disableCull|enableDepthTest|disableDepthTest|depthMask|setShaderFog\w+)\([^)]*\)'
for n in names:
    hits = list(src.rglob(n))
    if not hits: print("MISS", n); continue
    p = hits[0]
    out, pending, var, textured = [], None, None, False
    for line in p.read_text().splitlines():
        s = line.strip()
        m = re.match(r'RenderSystem\.setShaderTexture\(0,\s*(.+?)\);\s*$', s)
        if m: pending = m.group(1); continue
        m = re.match(r'(\w+)\s*=\s*Tessellator\.getInstance\(\)\.getBuffer\(\);\s*$', s)
        if m:
            var = m.group(1); textured = pending is not None
            expr = ("RenderUtil.imTex(%s)" % pending) if textured else "RenderUtil.imQuads()"
            out.append("        VertexConsumer %s = %s;" % (var, expr))
            pending = None; continue
        if var and re.match(re.escape(var) + r'\.begin\(.*\);\s*$', s): continue
        if 'Tessellator.getInstance().draw();' in s:
            out.append(line.replace('Tessellator.getInstance().draw();','RenderUtil.imFlush();'))
            textured, var = False, None; continue
        if re.match(r'RenderSystem\.setShader\(.*\);\s*$', s): continue
        if re.match(r'^\s*' + STATE + r';\s*$', line): continue
        if re.match(r'^\s*if \(.*\)\s*' + STATE + r';\s*$', line):
            out.append(re.sub(STATE + r';', '{ }', line)); continue
        if '.next();' in line:
            line = line.replace('.next();', '.light(LightmapTextureManager.MAX_LIGHT_COORDINATE);' if textured else ';')
        out.append(line)
    t = "\n".join(out) + "\n"
    t = re.sub(r'mc\.world\.getDimensionEffects\(\)\.getSkyType\(\)\s*!=\s*[A-Za-z0-9_.]+\.SkyType\.NORMAL', 'true', t)
    t = re.sub(r'^\s*import .*\.DimensionEffects;\s*$', '', t, flags=re.M)
    t = re.sub(r'\.getSkyAngle(?:Radians)?\([^)]*\)', '.getSkyAngleRadians()', t)
    p.write_text(t); print("ok", n)
PY

for FILE in SkyShader Starfield ParticleField WorldTint Bloom SunGlow; do
  P=$(f "$FILE.java"); [ -z "$P" ] && continue
  addimp "$P" "import net.minecraft.client.render.VertexConsumer;"
  addimp "$P" "import net.minecraft.client.render.LightmapTextureManager;"
done

P=$(f BackgroundRendererMixin.java)
if [ -n "$P" ]; then
  sed -i 's#^import net\.minecraft\.client\.render\.BackgroundRenderer;#import net.minecraft.client.render.fog.FogRenderer;#' "$P"
  sed -i 's/@Mixin(BackgroundRenderer\.class)/@Mixin(FogRenderer.class)/' "$P"
  echo "ok BackgroundRendererMixin"
fi

P=$(f SkyShader.java);  [ -n "$P" ] && sed -i '/^package /a import net.minecraft.client.render.DimensionEffects;' "$P"
P=$(f Particles.java);  [ -n "$P" ] && sed -i 's#^import net\.minecraft\.client\.option\.ParticlesMode;#import net.minecraft.client.particle.ParticlesMode;#' "$P"
P=$(f AutoArmor.java);  [ -n "$P" ] && sed -i 's#^import net\.minecraft\.item\.equipment\.ArmorItem;#import net.minecraft.item.ArmorItem;#' "$P"
P=$(f AutoPotion.java); [ -n "$P" ] && sed -i 's#^import net\.minecraft\.component\.type\.PotionContents;#import net.minecraft.component.type.PotionContentsComponent;#' "$P"

P=$(f FastFall.java);      [ -n "$P" ] && sed -i 's/\.isFallFlying()/.isGliding()/g' "$P"
P=$(f TabList.java);       [ -n "$P" ] && sed -i 's/\.getProfile()\.getName()/.getProfile().name()/g' "$P"
P=$(f PotionsWidget.java); [ -n "$P" ] && sed -i 's/\.getEffectType()\.getName()/.getEffectType().value().getName()/g; s/\.getEffectType()\.isBeneficial()/.getEffectType().value().isBeneficial()/g' "$P"
P=$(f SpeedWidget.java);   [ -n "$P" ] && sed -i 's/\.prevX/.lastX/g; s/\.prevY/.lastY/g; s/\.prevZ/.lastZ/g' "$P"
P=$(f Backtrack.java);     [ -n "$P" ] && sed -i 's/\.getPos()/.getEntityPos()/g' "$P"
P=$(f Freelook.java);      [ -n "$P" ] && sed -i 's/InputUtil\.isKeyPressed(mc\.getWindow()\.getHandle(),/InputUtil.isKeyPressed(mc.getWindow(),/g' "$P"
P=$(f AutoTool.java);      [ -n "$P" ] && sed -i 's/\.selectedSlot = \([^;]*\);/\.setSelectedSlot(\1);/g; s/\.selectedSlot/\.getSelectedSlot()/g' "$P"
P=$(f Criticals.java);     [ -n "$P" ] && sed -i 's/PositionAndOnGround(\(.*\), false)/PositionAndOnGround(\1, false, false)/g' "$P"
P=$(f PopEffects.java);    [ -n "$P" ] && sed -i 's/addParticle(ParticleTypes\.TOTEM_OF_UNDYING,/addParticle(ParticleTypes.TOTEM_OF_UNDYING, false, false,/g' "$P"
P=$(f HitParticles.java);  [ -n "$P" ] && sed -i 's/addParticle(ParticleUtil\.get(particle\.get()),/addParticle(ParticleUtil.get(particle.get()), false, false,/g' "$P"
P=$(f TargetStrafe.java);  [ -n "$P" ] && sed -i 's/\.input\.movementSideways = \([^;]*\);/\.input.setMovementSideways(\1);/g; s/\.input\.movementForward = \([^;]*\);/\.input.setMovementForward(\1);/g' "$P"
P=$(f Sprint.java);        [ -n "$P" ] && sed -i 's/\.input\.movementForward/.input.getMovementForward()/g; s/\.input\.movementSideways/.input.getMovementSideways()/g' "$P"
P=$(f KeybindsWidget.java);[ -n "$P" ] && sed -i 's/InputUtil\.fromKeyCode(\([^,]*\), -1)/InputUtil.fromKeyCode(InputUtil.Type.KEYSYM.createFromCode(\1))/g' "$P"
P=$(f LovenoteScreen.java);[ -n "$P" ] && sed -i 's/InputUtil\.fromKeyCode(\([^,]*\), -1)/InputUtil.fromKeyCode(InputUtil.Type.KEYSYM.createFromCode(\1))/g' "$P"
P=$(f WorldTint.java);     [ -n "$P" ] && sed -i 's/ctx\.getMatrices()\.peek()\.getPositionMatrix()/new Matrix4f()/g' "$P"
P=$(f TargetHud.java)
if [ -n "$P" ]; then
  sed -i 's/MatrixStack m = ctx\.getMatrices();/Matrix3x2fStack m = ctx.getMatrices();/' "$P"
  sed -i 's/\bm\.push()/m.pushMatrix()/g; s/\bm\.pop()/m.popMatrix()/g' "$P"
  sed -i 's/m\.translate(\([^,]*\), \([^,]*\), 0)/m.translate(\1, \2)/g' "$P"
  sed -i 's/m\.scale(\([^,]*\), \([^,]*\), 1)/m.scale(\1, \2)/g' "$P"
fi

for FILE in HudWidget LovenoteScreen; do
  P=$(f "$FILE.java"); [ -z "$P" ] && continue
  addimp "$P" "import net.minecraft.client.render.RenderPipelines;"
  grep -q 'drawTexture(RenderPipelines' "$P" || sed -i 's/ctx\.drawTexture(\([^,]*\),/ctx.drawTexture(RenderPipelines.GUI_TEXTURED, \1,/g' "$P"
  sed -i 's/p\.getSkinTextures()\.texture()/p.getPlayerListEntry().getSkinTextures().texture()/g' "$P"
  echo "ok $FILE"
done
echo "================ done ================"
