#!/usr/bin/env python3
import re, sys, pathlib

ROOT = pathlib.Path('/workspaces')
SKIP = {'.gradle', 'build', '.git', 'node_modules', 'out'}

def find(name):
    for p in ROOT.rglob(name):
        if SKIP & set(p.parts):
            continue
        return p
    return None

sky = find('SkyShader.java')
if sky is None:
    print("!! SkyShader.java not found under /workspaces"); sys.exit(1)
print("java root:", sky.parent)

def addimp(p, imp):
    t = p.read_text()
    if imp in t:
        return
    t = re.sub(r'^(package [^\n]*;\n)', r'\1' + imp + '\n', t, count=1)
    p.write_text(t)

def patch(name, fn):
    p = find(name)
    if p is None:
        print("MISS", name); return
    t = p.read_text()
    n = fn(t)
    if n and n != t:
        p.write_text(n)
    print("ok", name)

# ---------- 1. RenderUtil helpers ----------
HELPERS = '''
    public static VertexConsumerProvider.Immediate imBuffers() {
        return MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
    }

    public static VertexConsumer imTex(Identifier id) {
        return imBuffers().getBuffer(RenderLayer.getTextSeeThrough(id));
    }

    public static VertexConsumer imQuads() {
        return imBuffers().getBuffer(RenderLayer.getDebugQuads());
    }

    public static void imFlush() {
        imBuffers().draw();
    }
'''
ru = find('RenderUtil.java')
if ru:
    t = ru.read_text().rstrip()
    if 'imFlush()' not in t and t.endswith('}'):
        ru.write_text(t[:-1].rstrip() + '\n' + HELPERS + '}\n')
    addimp(ru, 'import net.minecraft.client.render.VertexConsumer;')
    addimp(ru, 'import net.minecraft.client.render.VertexConsumerProvider;')
    addimp(ru, 'import net.minecraft.client.render.RenderLayer;')
    print('ok RenderUtil')

# ---------- 2. Tessellator -> RenderLayer ----------
STATE = r'RenderSystem\.(enableBlend|disableBlend|defaultBlendFunc|enableCull|disableCull|enableDepthTest|disableDepthTest|depthMask|setShaderFog\w+)\([^)]*\)'

def renderfile(t):
    out, pending, var, textured = [], None, None, False
    for line in t.split('\n'):
        s = line.strip()
        m = re.match(r'RenderSystem\.setShaderTexture\(0,\s*(.+?)\);\s*$', s)
        if m:
            pending = m.group(1); continue
        m = re.match(r'(\w+)\s*=\s*Tessellator\.getInstance\(\)\.getBuffer\(\);\s*$', s)
        if m:
            var = m.group(1); textured = pending is not None
            expr = ('RenderUtil.imTex(%s)' % pending) if textured else 'RenderUtil.imQuads()'
            out.append('        VertexConsumer %s = %s;' % (var, expr))
            pending = None; continue
        if var and re.match(re.escape(var) + r'\.begin\(.*\);\s*$', s):
            continue
        if 'Tessellator.getInstance().draw();' in s:
            out.append(line.replace('Tessellator.getInstance().draw();', 'RenderUtil.imFlush();'))
            textured, var = False, None; continue
        if re.match(r'RenderSystem\.setShader\(.*\);\s*$', s):
            continue
        if re.match(r'^\s*' + STATE + r';\s*$', line):
            continue
        if re.match(r'^\s*if \(.*\)\s*' + STATE + r';\s*$', line):
            out.append(re.sub(STATE, '{ }', line)); continue
        if '.next();' in line:
            line = line.replace('.next();', '.light(LightmapTextureManager.MAX_LIGHT_COORDINATE);' if textured else ';')
        out.append(line)
    t = '\n'.join(out)
    t = re.sub(r'mc\.world\.getDimensionEffects\(\)\.getSkyType\(\)\s*!=\s*[A-Za-z0-9_.]+\.SkyType\.NORMAL', 'true', t)
    t = re.sub(r'^[ \t]*import [^\n]*\.DimensionEffects;[ \t]*\n', '', t, flags=re.M)
    t = re.sub(r'\.getSkyAngle(?:Radians)?\([^)]*\)', '.getSkyAngleRadians()', t)
    return t

for n in ['SkyShader.java','Starfield.java','ParticleField.java','WorldTint.java','Bloom.java','SunGlow.java']:
    patch(n, renderfile)
    p = find(n)
    if p:
        addimp(p, 'import net.minecraft.client.render.VertexConsumer;')
        addimp(p, 'import net.minecraft.client.render.LightmapTextureManager;')

patch('SkyShader.java', lambda t: re.sub(r'^(package [^\n]*;\n)', r'\1import net.minecraft.client.render.DimensionEffects;\n', t, count=1))

# ---------- 3. mixin ----------
def mixin(t):
    t = t.replace('import net.minecraft.client.render.BackgroundRenderer;',
                  'import net.minecraft.client.render.fog.FogRenderer;')
    return t.replace('@Mixin(BackgroundRenderer.class)', '@Mixin(FogRenderer.class)')
patch('BackgroundRendererMixin.java', mixin)

# ---------- 4. imports ----------
patch('Particles.java',  lambda t: t.replace('import net.minecraft.client.option.ParticlesMode;', 'import net.minecraft.client.particle.ParticlesMode;'))
patch('AutoArmor.java',  lambda t: t.replace('import net.minecraft.item.equipment.ArmorItem;', 'import net.minecraft.item.ArmorItem;'))
patch('AutoPotion.java', lambda t: t.replace('import net.minecraft.component.type.PotionContents;', 'import net.minecraft.component.type.PotionContentsComponent;'))

# ---------- 5. renames ----------
patch('FastFall.java',    lambda t: t.replace('.isFallFlying()', '.isGliding()'))
patch('TabList.java',     lambda t: t.replace('.getProfile().getName()', '.getProfile().name()'))
patch('PotionsWidget.java', lambda t: t.replace('.getEffectType().getName()', '.getEffectType().value().getName()')
                                        .replace('.getEffectType().isBeneficial()', '.getEffectType().value().isBeneficial()'))
patch('SpeedWidget.java', lambda t: t.replace('.prevX', '.lastX').replace('.prevY', '.lastY').replace('.prevZ', '.lastZ'))
patch('Backtrack.java',   lambda t: t.replace('.getPos()', '.getEntityPos()'))
patch('Freelook.java',    lambda t: t.replace('mc.getWindow().getHandle(),', 'mc.getWindow(),'))
patch('PopEffects.java',  lambda t: t.replace('addParticle(ParticleTypes.TOTEM_OF_UNDYING,', 'addParticle(ParticleTypes.TOTEM_OF_UNDYING, false, false,'))
patch('HitParticles.java',lambda t: t.replace('addParticle(ParticleUtil.get(particle.get()),', 'addParticle(ParticleUtil.get(particle.get()), false, false,'))
patch('WorldTint.java',   lambda t: t.replace('ctx.getMatrices().peek().getPositionMatrix()', 'new Matrix4f()'))
patch('AutoTool.java',    lambda t: re.sub(r'\.selectedSlot = ([^;]*);', r'.setSelectedSlot(\1);', t).replace('.selectedSlot', '.getSelectedSlot()'))
patch('Criticals.java',   lambda t: re.sub(r'PositionAndOnGround\((.*), false\)', r'PositionAndOnGround(\1, false, false)', t))
patch('TargetStrafe.java',lambda t: re.sub(r'\.input\.movementSideways = ([^;]*);', r'.input.setMovementSideways(\1);', t)
                                       .replace('.input.movementForward = ', '.input.setMovementForward = '))
patch('Sprint.java',      lambda t: t.replace('.input.movementForward', '.input.getMovementForward()').replace('.input.movementSideways', '.input.getMovementSideways()'))
patch('KeybindsWidget.java', lambda t: re.sub(r'InputUtil\.fromKeyCode\(([^,]*), -1\)', r'InputUtil.fromKeyCode(InputUtil.Type.KEYSYM.createFromCode(\1))', t))
patch('LovenoteScreen.java', lambda t: re.sub(r'InputUtil\.fromKeyCode\(([^,]*), -1\)', r'InputUtil.fromKeyCode(InputUtil.Type.KEYSYM.createFromCode(\1))', t))

def targethud(t):
    t = t.replace('MatrixStack m = ctx.getMatrices();', 'Matrix3x2fStack m = ctx.getMatrices();')
    t = re.sub(r'\bm\.push\(\)', 'm.pushMatrix()', t)
    t = re.sub(r'\bm\.pop\(\)', 'm.popMatrix()', t)
    t = re.sub(r'm\.translate\(([^,]*), ([^,]*), 0\)', r'm.translate(\1, \2)', t)
    t = re.sub(r'm\.scale\(([^,]*), ([^,]*), 1\)', r'm.scale(\1, \2)', t)
    return t
patch('TargetHud.java', targethud)

def drawtex(t):
    t = re.sub(r'ctx\.drawTexture\((?!RenderPipelines)([^,]*),', r'ctx.drawTexture(RenderPipelines.GUI_TEXTURED, \1,', t)
    return t.replace('p.getSkinTextures().texture()', 'p.getPlayerListEntry().getSkinTextures().texture()')
for n in ['HudWidget.java', 'LovenoteScreen.java']:
    patch(n, drawtex)
    p = find(n)
    if p:
        addimp(p, 'import net.minecraft.client.render.RenderPipelines;')

print("================ DONE ================")
