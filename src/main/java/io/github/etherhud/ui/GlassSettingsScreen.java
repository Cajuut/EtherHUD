package io.github.etherhud.ui;

import io.github.etherhud.render.GlassRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class GlassSettingsScreen extends Screen {
    private static final Logger LOG = LoggerFactory.getLogger("EtherHUD");
    private static boolean blurUnavailable;

    protected GlassSettingsScreen(Component title) {
        super(title);
    }

    protected final void renderFrostedBackdrop(GuiGraphics graphics, float delta) {
        if (minecraft.level == null) {
            renderPanorama(graphics, delta);
        }
        if (!blurUnavailable && minecraft.options.getMenuBackgroundBlurriness() > 0) {
            graphics.flush();
            try {
                // Minecraft 1.21.4's menu blur respects the player's accessibility setting.
                renderBlurredBackground();
            } catch (RuntimeException exception) {
                blurUnavailable = true;
                minecraft.getMainRenderTarget().bindWrite(false);
                LOG.warn("Could not blur the settings backdrop. Using the glass tint.", exception);
            }
        }
        graphics.fill(0, 0, width, height, 0x3B081321);
    }

    protected static void drawFrostedPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        GlassRenderer.rounded(graphics, x, y, width, height, 8, 8, 8, 8, 1, 0xAC162938, 0x7EB6DBE8);
    }
}
