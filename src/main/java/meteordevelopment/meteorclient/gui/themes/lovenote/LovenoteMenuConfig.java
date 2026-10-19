package meteordevelopment.meteorclient.gui.themes.lovenote;

/** Look/layout knobs. Stage 2 wires the Themes page to these. */
public final class LovenoteMenuConfig {
    private LovenoteMenuConfig() {}

    public static int menuScale = 82;
    public static int windowWidth = 870;
    public static int windowHeight = 530;
    public static int corner = 7;
    public static int railWidth = 146;
    public static boolean railRight = false;
    public static int columns = 1;
    public static boolean compact = false;
    public static boolean descriptions = true;
    public static boolean footer = true;
    public static boolean tooltips = true;

    public static boolean logo = true;
    /** Menu-only L-heart decoration; off unless explicitly enabled in Appearance. */
    public static boolean decorativeLogo = false;
    public static boolean logoGlow = true;
    public static int logoSize = 26;
    public static long logoSpin = 2600L;

    public static String fontMode = "Clean"; // Render via Windows Segoe UI with a vanilla fallback.
    public static int widgetGlow = 10;
    public static int widgetBorder = 38;
    public static int widgetSheen = 10;
    public static int widgetOpacity = 96;
    public static boolean widgetGlass = true;
    public static String style = "Premium";
    public static String cardStyle = "Solid";
    public static int opacity = 92;
    public static int glow = 3;
    public static boolean shadow = true;
    public static boolean headerLine = true;
    public static boolean sheen = true;

    public static String animation = "Fade";
    public static int animSpeed = 300;
    public static int railSpeed = 300;
    public static boolean stagger = true;
    public static int staggerSpeed = 20;
    public static int hoverLift = 2;
    public static boolean ripples = true;

    public static int particles = 45;
    public static int particleSpeed = 100;

    public static int accentIndex = 0;
    public static String customPrimary = "A855F7";
    public static String customSecondary = "9647FF";

    public static void apply() {
        LovenotePalette.animated = true;
        int i = accentIndex;
        if (i >= 0 && i < LovenotePalette.PRESETS.length) {
            LovenotePalette.setAccent(LovenotePalette.PRESETS[i][0], LovenotePalette.PRESETS[i][1]);
        } else {
            LovenotePalette.setAccent(parse(customPrimary, 0xA855F7), parse(customSecondary, 0x9647FF));
        }
    }

    public static int parse(String hex, int fallback) {
        try { return Integer.parseInt(hex.replace("#", "").trim(), 16) & 0xFFFFFF; }
        catch (Exception e) { return fallback; }
    }
}
