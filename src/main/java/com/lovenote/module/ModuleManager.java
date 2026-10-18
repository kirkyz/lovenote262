package com.lovenote.module;

import com.lovenote.module.impl.*;

import com.lovenote.module.impl.GroundFog;
import com.lovenote.module.impl.SkyShader;
import com.lovenote.module.impl.SwingAnimation;
import com.lovenote.module.impl.Velocity;
import com.lovenote.module.impl.Interface;
import com.lovenote.module.impl.LogoWidget;
import com.lovenote.module.impl.FunnyCrosshair;
import com.lovenote.module.impl.SkyExtras;
import com.lovenote.module.impl.CubeField;
import com.lovenote.module.impl.CrystalHalo;
import com.lovenote.module.impl.AuroraVeil;
import com.lovenote.module.impl.NeonGrid;
import com.lovenote.module.impl.RibbonTrail;
import com.lovenote.module.impl.StarSphere;
import com.lovenote.module.impl.SparkBurst;
import com.lovenote.module.impl.PrismOrb;
import com.lovenote.module.impl.Vortex;
import com.lovenote.module.impl.PlasmaShell;
import com.lovenote.module.impl.ArcBeams;
import com.lovenote.module.impl.PrismRain;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ModuleManager {
	private final List<Module> modules = new ArrayList<>();

	public ModuleManager() {
		register(new GroundFog()).setEnabled(true);
		register(new SkyShader()).setEnabled(true);
		// ---- lovenote pack
		register(new Interface()).setEnabled(true);
		register(new LogoWidget()).setEnabled(true);
		register(new FunnyCrosshair()).setEnabled(true);
		register(new SkyExtras());
		register(new CubeField());
		register(new CrystalHalo());
		register(new AuroraVeil());
		register(new NeonGrid());
		register(new RibbonTrail());
		register(new StarSphere());
		register(new SparkBurst());
		register(new PrismOrb());
		register(new Vortex());
		register(new PlasmaShell());
		register(new ArcBeams());
		register(new PrismRain());

		register(new Velocity());
		register(new SwingAnimation());
		register(new AutoSprint());
		register(new AutoJump());
		register(new AntiAFK());
		register(new Panic());
		register(new AutoSell());
		register(new AutoTip());
		// Legacy 1.21 registry backbone: these are deliberately disabled, inert placeholders.
		register(new Aura());
		register(new KillAura());
		register(new AimAssist());
		register(new Triggerbot());
		register(new Criticals());
		register(new WTap());
		register(new AutoClicker());
		register(new AutoTotem());
		register(new AutoArmor());
		register(new AutoCrystal());
		register(new CrystalTap());
		register(new CrystalBreak());
		register(new Backtrack());
		register(new TargetStrafe());
		register(new AutoWeb());
		register(new Reach());
		register(new Surround());
		register(new AutoTrap());
		register(new AutoCity());
		register(new OldCombat());
		register(new AutoFarm());
		register(new AutoFish());
		register(new CommandMacro());
		register(new ServerInfo());
		register(new StaffDetector());
		register(new AntiBot());
		register(new AutoPotion());
		register(new AutoGApple());
		register(new AutoTool());
		register(new AutoLog());
		register(new AutoRespawn());
		register(new Sprint());
		register(new Speed());
		register(new Flight());
		register(new Bhop());
		register(new AirJump());
		register(new Parkour());
		register(new Scaffold());
		register(new NoFall());
		register(new AntiVoid());
		register(new Step());
		register(new NoSlow());
		register(new FastFall());
		register(new ElytraPlus());
		register(new ToggleSprint());
		register(new AntiHunger());
		register(new FastLadder());
		register(new ESP());
		register(new ItemESP());
		register(new Glow());
		register(new Chams());
		register(new Tracers());
		register(new TargetESP());
		register(new Wings());
		register(new JumpCircles());
		register(new Trail());
		register(new Orbit());
		register(new HitParticles());
		register(new KillEffects());
		register(new PopEffects());
		register(new AmbientParticles());
		register(new HitMarker());
		register(new HitSound());
		register(new TimeChanger());
		register(new Fullbright());
		register(new Zoom());
		register(new Freelook());
		register(new ParticleField());
		register(new CustomParticles());
		register(new GroundRings());
		register(new TrailRibbon());
		register(new GhostTrail());
		register(new Halo());
		register(new Nebula());
		register(new Shockwave());
		register(new DamageIndicator());
		register(new WorldTint());
		register(new StorageESP());
		register(new HoleESP());
		register(new BlockESP());
		register(new Nametags());
		register(new Trajectories());
		register(new ScreenFX());
		register(new CustomCrosshair());
		register(new Bloom());
		register(new Beams());
		register(new BlockHighlight());
		register(new Waypoints());
		register(new Aurora());
		register(new Starfield());
		register(new SunGlow());
		register(new Vignette());
		register(new Fog());
		register(new RenderDistance());
		register(new Clouds());
		register(new GraphicsMode());
		register(new Particles());
		register(new BiomeBlend());
		register(new EntityShadows());
		register(new MaxFps());
		register(new ChatOpacity());
		register(new Weather());
		register(new SmoothCamera());
		register(new FovOverride());
		register(new NoViewBob());
		register(new AutoBuy());
		register(new ChestStealer());
		register(new SpawnerESP());
		register(new LootBeam());
		register(new AutoReconnect());
		register(new FastUse());
		register(new NoJumpDelay());
		register(new SafeWalk());
		register(new AutoPearl());
		register(new AntiLevitation());
		register(new AutoJumpReset());
		register(new AutoAnchor());
		register(new PlayerTracker());
		register(new KillCounter());
		register(new DeathCoords());
		register(new AutoGG());
		register(new ChestTracker());
		register(new InterfaceModule());
		register(new Watermark());
		register(new DrainerInfoBar());
		register(new SpotifyHud());
		register(new Lyrics());
		register(new StaffList());
		register(new TargetHud());
		register(new SpeedWidget());
		register(new Coordinates());
		register(new KeybindsWidget());
		register(new PotionsWidget());
		register(new CooldownsWidget());
		register(new InventoryWidget());
		register(new Keystrokes());
		register(new Radar());
		register(new ModuleList());
		register(new HitLog());
		register(new InfoHud());
		register(new PotionList());
		register(new ArmorHud());
		register(new CpsHud());
		register(new CoordsHud());
		register(new PingHud());
		register(new TabList());
		register(new ComboCounter());
		register(new Compass());
		register(new FastPlace());
		register(new FakePlayer());
		register(new AutoEat());
		register(new MiddleClick());
		register(new ChatSuffix());

	}

	private <T extends Module> T register(T m) {
		for (Module existing : modules) {
			if (existing.getClass() == m.getClass())
				throw new IllegalStateException("Duplicate Lovenote module class: " + m.getClass().getName());
			if (existing.name.equalsIgnoreCase(m.name))
				throw new IllegalStateException("Duplicate Lovenote module name: " + m.name);
		}
		modules.add(m);
		return m;
	}

	public List<Module> all() { return modules; }

	@SuppressWarnings("unchecked")
	public <T extends Module> T get(Class<T> type) {
		for (Module m : modules) if (type.isInstance(m)) return (T) m;
		return null;
	}

	public List<Module> byCategory(Category c) {
		List<Module> out = new ArrayList<>();
		for (Module m : modules) if (!m.hidden && (m.category == c || m.alsoIn.contains(c))) out.add(m);
		return out;
	}

	public List<Module> search(String query) {
		String q = query.trim().toLowerCase(Locale.ROOT);
		List<Module> out = new ArrayList<>();
		for (Module m : modules) if (!m.hidden && m.matches(q)) out.add(m);
		return out;
	}

	public void tick() {
		for (Module m : modules) if (m.enabled) m.onTick();
	}

	public void render3D(WorldRenderContext ctx) {
		PoseStack stack = ctx.matrixStack();
		for (Module m : modules) if (m.enabled) m.onRender3D(ctx, stack);
	}

	public void hud(GuiGraphicsExtractor ctx, float tickDelta) {
		for (Module m : modules) if (m.enabled) m.onHud(ctx, tickDelta);
	}

	public void attack(Entity target) {
		for (Module m : modules) if (m.enabled) m.onAttack(target);
	}

	public void totemPop(Entity entity) {
		for (Module m : modules) if (m.enabled) m.onTotemPop(entity);
	}
}
