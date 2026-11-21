package io.github.etherhud.render;

import net.minecraft.client.gui.GuiGraphics;

public final class GlassPanel {
    public static final int ACCENT = 0xFF79DDE6;
    public static final int TEXT = 0xFFF0F7FC;
    public static final int MUTED = 0xFFA8BAC7;
    public static final int HUD = 0xA61A2532;

    private GlassPanel() {}

    public static void draw(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        draw(graphics, x, y, width, height, 3, color);
    }

    public static void draw(GuiGraphics graphics, int x, int y, int width, int height, int radius, int color) {
        GlassRenderer.rounded(graphics, x, y, width, height, radius, color);
    }

    public static void rounded(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        GlassRenderer.rounded(graphics, x, y, width, height, width >= 6 && height >= 6 ? 3 : 0, color);
    }
}
