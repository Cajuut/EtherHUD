package io.github.etherhud.module;

import io.github.etherhud.config.HudSettings;
import io.github.etherhud.config.ModuleAppearance;
import io.github.etherhud.module.settings.CommonModuleSettings;
import io.github.etherhud.module.settings.ModuleSetting;
import io.github.etherhud.render.GlassRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.List;

public interface HudModule {
    int TEXT_PADDING = 6;

    void update(Minecraft client, HudSettings settings);
    default void onInactive() {}

    default Component liveText() { return null; }
    default Component previewText() { return null; }
    default boolean active(HudSettings settings) { return true; }
    default int layoutVersion(HudSettings settings, boolean preview) { return 0; }

    default List<ModuleSetting> settings(ModuleId id) {
        return CommonModuleSettings.forModule(id);
    }

    default int panelWidth(Font font, Component text, HudSettings settings, boolean preview) {
        return Math.max(48, font.width(text) + 2 * TEXT_PADDING);
    }

    default int panelHeight(HudSettings settings, boolean preview) {
        return 19;
    }

    default void draw(Minecraft client, GuiGraphics graphics, HudSettings settings, ModuleId id,
                      Component label, int width, int height, boolean preview, boolean ghost) {
        var appearance = settings.appearance(id);
        drawPanel(graphics, width, height, appearance, ghost);
        graphics.drawString(client.font, label, TEXT_PADDING, (height - client.font.lineHeight) / 2,
                appearance.text(ghost), appearance.textShadow());
    }

    static void drawPanel(GuiGraphics graphics, int width, int height, ModuleAppearance appearance, boolean ghost) {
        if (appearance.backgroundOpacity() > 0 || appearance.outlineEnabled()) {
            float radius = appearance.cornerRadius();
            GlassRenderer.rounded(graphics, 0, 0, width, height, radius, radius, radius, radius,
                    appearance.outlineEnabled() ? 1 : 0, appearance.background(ghost), appearance.outline(ghost));
        }
    }
}
