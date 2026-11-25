package io.github.etherhud.render;

import io.github.etherhud.config.HudSettings;
import io.github.etherhud.layout.LayoutResolver;
import io.github.etherhud.module.HudModules;
import io.github.etherhud.module.ModuleId;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

public final class HudRenderer {
    private final LayoutResolver resolver = new LayoutResolver();
    private final LayoutResolver.Bounds[] bounds = new LayoutResolver.Bounds[ModuleId.ALL.length];
    private final Component[] labels = new Component[ModuleId.ALL.length];
    private final int[] widths = new int[ModuleId.ALL.length];
    private final int[] heights = new int[ModuleId.ALL.length];
    private final Language[] languages = new Language[ModuleId.ALL.length];
    private final HudSettings[] measuredSettings = new HudSettings[ModuleId.ALL.length];
    private final int[] versions = new int[ModuleId.ALL.length];
    private final boolean[] hasContent = new boolean[ModuleId.ALL.length];
    private final boolean preview;
    private HudSettings settings;
    private HudModules modules;

    public HudRenderer(boolean preview) {
        this.preview = preview;
        for (ModuleId id : ModuleId.ALL) {
            bounds[id.ordinal()] = new LayoutResolver.Bounds();
        }
    }

    public void layout(Minecraft client, HudSettings settings, HudModules modules, int width, int height) {
        layout(client, settings, modules, width, height, true);
    }

    public void layout(Minecraft client, HudSettings settings, HudModules modules, int width, int height,
                       boolean includeDisabledPreviews) {
        this.settings = settings;
        this.modules = modules;
        Language currentLanguage = Language.getInstance();
        for (ModuleId id : ModuleId.ALL) {
            int index = id.ordinal();
            var module = modules.get(id);
            if (((!preview || !includeDisabledPreviews) && !settings.placement(id).enabled())
                    || (!preview && !module.active(settings))) {
                widths[index] = 0;
                heights[index] = 0;
                hasContent[index] = false;
                measuredSettings[index] = null;
                continue;
            }
            Component label = preview && labels[index] != null ? labels[index]
                    : preview ? module.previewText() : module.liveText();
            int version = module.layoutVersion(settings, preview);
            if (measuredSettings[index] != settings || labels[index] != label
                    || languages[index] != currentLanguage || versions[index] != version) {
                measuredSettings[index] = settings;
                versions[index] = version;
                labels[index] = label;
                languages[index] = currentLanguage;
                widths[index] = module.panelWidth(client.font, label, settings, preview);
                heights[index] = module.panelHeight(settings, preview);
                hasContent[index] = widths[index] > 0;
                if (preview && widths[index] == 0) {
                    widths[index] = 48;
                    heights[index] = 19;
                }
            }
        }
        resolver.resolve(settings, bounds, widths, heights, width, height, preview,
                modules.armor().reserveLeft(), modules.armor().reserveRight(), client.getWindow().getGuiScale());
    }

    public LayoutResolver.Bounds bounds(ModuleId id) {
        return bounds[id.ordinal()];
    }

    public int panelWidth(ModuleId id) { return widths[id.ordinal()]; }
    public int panelHeight(ModuleId id) { return heights[id.ordinal()]; }
    public boolean hasContent(ModuleId id) { return hasContent[id.ordinal()]; }

    public int defaultStackY(ModuleId id, float hudScale) {
        return resolver.defaultStackY(id, heights, hudScale, bounds[id.ordinal()].unitScale());
    }

    public void draw(Minecraft client, GuiGraphics graphics, ModuleId id, int x, int y, boolean ghost) {
        var frame = bounds[id.ordinal()];
        if (frame.width() > 0) {
            drawScaled(client, graphics, id, x, y, frame.scale(), ghost);
        }
    }

    public void drawScaled(Minecraft client, GuiGraphics graphics, ModuleId id, int x, int y, float scale, boolean ghost) {
        int index = id.ordinal();
        if (widths[index] == 0) {
            return;
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        modules.get(id).draw(client, graphics, settings, id, labels[index], widths[index], heights[index], preview, ghost);
        graphics.pose().popMatrix();
    }
}
