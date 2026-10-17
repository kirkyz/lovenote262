package meteordevelopment.meteorclient.gui.themes.lovenote;

import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public enum LovenotePresets {
    Lovenote(0x9ACB, 0xB28DFF), Purple(0xA855F7, 0x9647FF), Red(0xEF4444, 0xF97316),
    Orange(0xF97316, 0xFACC15), Yellow(0xFACC15, 0xA3E635), Green(0x22C55E, 0x10B981),
    Cyan(0x06B6D4, 0x3B82F6), Blue(0x3B82F6, 0x6366F1), Pink(0xEC4899, 0xF472B6),
    Sunset(0xFF7A45, 0xFFB84D), Dawn(0xFFC48A, 0xFF7AB0), Mint(0x3BE8B0, 0x2BD9C4),
    Forest(0x2E7D32, 0x66BB6A), Ocean(0x0E7490, 0x22D3EE), Ice(0xA5F3FC, 0x67E8F9),
    Sky(0x38BDF8, 0x818CF8), Lavender(0xC4B5FD, 0xE9D5FF), Magenta(0xE935C1, 0xFF5FD2),
    Rose(0xFB7185, 0xF472B6), Coral(0xFF7F50, 0xFFA36B), Amber(0xFFB020, 0xFFD166),
    Gold(0xFFD700, 0xFFB800), Lime(0xA3E635, 0x84CC16), Emerald(0x10B981, 0x34D399),
    Teal(0x14B8A6, 0x0D9488), Aqua(0x22D3EE, 0x38BDF8), Azure(0x2563EB, 0x60A5FA),
    Indigo(0x4F46E5, 0x7C3AED), Violet(0x8B5CF6, 0xA78BFA), Plum(0xA21CAF, 0xD946EF),
    Crimson(0xDC143C, 0xFF4D6D), Ember(0xFF4500, 0xFF8C00), Neon(0x39FF14, 0x00E5FF),
    Vapor(0xFF71CE, 0x01CDFE), Matrix(0x00FF41, 0x008F11), Candy(0xFF9ACB, 0x9BF6FF),
    Midnight(0x191970, 0x4B0082), Blood(0x8A0303, 0xDC143C), Frost(0xB3E5FC, 0x4FC3F7),
    Haze(0x9D8DF1, 0x6FD1E8), Sakura(0xFFB7C5, 0xFF8FAB), Toxic(0xB6FF00, 0x00FF9C);

    public final SettingColor primary;
    public final SettingColor secondary;

    LovenotePresets(int p, int s) {
        this.primary = fromHex(p);
        this.secondary = fromHex(s);
    }

    private static SettingColor fromHex(int rgb) {
        return new SettingColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255);
    }
}
