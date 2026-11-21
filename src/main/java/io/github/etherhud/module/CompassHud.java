package io.github.etherhud.module;

import io.github.etherhud.config.HudSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class CompassHud implements HudModule {
    private static final int WIDTH = 204;
    private static final int HEIGHT = 22;
    private static final int STEP_DEGREES = 15;
    private static final float PIXELS_PER_DEGREE = 1.35f;
    private static final String[] DIRECTIONS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
    private static final String[] DEGREES = new String[360 / STEP_DEGREES];

    static {
        for (int i = 0; i < DEGREES.length; i++) {
            DEGREES[i] = Integer.toString(i * STEP_DEGREES);
        }
    }

    @Override
    public void update(Minecraft client, HudSettings settings) {}

    @Override
    public int panelWidth(Font font, Component label, HudSettings settings, boolean preview) { return WIDTH; }

    @Override
    public int panelHeight(HudSettings settings, boolean preview) { return HEIGHT; }

    @Override
    public void draw(Minecraft client, GuiGraphics graphics, HudSettings settings, ModuleId id, Component label,
                     int width, int height, boolean preview, boolean ghost) {
        var appearance = settings.appearance(id);
        HudModule.drawPanel(graphics, width, height, appearance, ghost);
        // Minecraft yaw is zero at south and increases toward west.
        float heading = preview || client.player == null ? 12 : (client.player.getYRot() + 180) % 360;
        int center = width / 2;
        int first = (int) Math.floor((heading - center / PIXELS_PER_DEGREE) / STEP_DEGREES);
        int last = (int) Math.ceil((heading + center / PIXELS_PER_DEGREE) / STEP_DEGREES);
        int color = appearance.text(ghost);
        graphics.enableScissor(3, 1, width - 3, height - 1);
        for (int tick = first; tick <= last; tick++) {
            int angle = Math.floorMod(tick, DEGREES.length);
            int x = center + Math.round((tick * STEP_DEGREES - heading) * PIXELS_PER_DEGREE);
            boolean direction = angle % 3 == 0;
            String text = direction ? DIRECTIONS[angle / 3] : DEGREES[angle];
            int alpha = direction ? color & 0xFF000000 : ghost ? 0x78000000 : 0xA0000000;
            int tickColor = alpha | color & 0xFFFFFF;
            graphics.drawString(client.font, text, x - client.font.width(text) / 2, 7, tickColor, appearance.textShadow());
            graphics.fill(x, direction ? 16 : 17, x + 1, 19, tickColor);
        }
        graphics.fill(center - 3, 2, center + 4, 3, color);
        graphics.fill(center - 2, 3, center + 3, 4, color);
        graphics.fill(center - 1, 4, center + 2, 5, color);
        graphics.disableScissor();
    }
}
