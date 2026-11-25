package io.github.etherhud.module.settings;

import io.github.etherhud.config.HudSettings;
import io.github.etherhud.layout.Anchor;
import io.github.etherhud.module.ModuleId;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class CommonModuleSettings {
    private CommonModuleSettings() {}

    public static List<ModuleSetting> forModule(ModuleId id) {
        return List.of(
                new ModuleSetting.Section("common"),
                new ModuleSetting.Toggle("enabled", s -> s.placement(id).enabled(),
                        (s, v) -> s.withPlacement(id, s.placement(id).withEnabled(v))),
                new ModuleSetting.Slider("scale", 50, 300, 5, s -> Math.round(s.placement(id).scale() * 100),
                        (s, v) -> s.withPlacement(id, s.placement(id).withScale(v / 100f)), CommonModuleSettings::percent),
                new ModuleSetting.Section("appearance"),
                new ModuleSetting.Slider("opacity", 0, 100, 1, s -> s.appearance(id).backgroundOpacity(),
                        (s, v) -> s.withAppearance(id, s.appearance(id).withOpacity(v)), CommonModuleSettings::percent),
                new ModuleSetting.Color("background_color", s -> s.appearance(id).backgroundColor(),
                        (s, v) -> s.withAppearance(id, s.appearance(id).withBackgroundColor(v))),
                new ModuleSetting.Toggle("outline_enabled", s -> s.appearance(id).outlineEnabled(),
                        (s, v) -> s.withAppearance(id, s.appearance(id).withOutlineEnabled(v))),
                new ModuleSetting.Color("outline_color", s -> s.appearance(id).outlineColor(),
                        (s, v) -> s.withAppearance(id, s.appearance(id).withOutlineColor(v))),
                new ModuleSetting.Slider("corner_radius", 0, 12, 1, s -> s.appearance(id).cornerRadius(),
                        (s, v) -> s.withAppearance(id, s.appearance(id).withCornerRadius(v)), CommonModuleSettings::pixels),
                new ModuleSetting.Color("text_color", s -> s.appearance(id).textColor(),
                        (s, v) -> s.withAppearance(id, s.appearance(id).withTextColor(v))),
                new ModuleSetting.Toggle("text_shadow", s -> s.appearance(id).textShadow(),
                        (s, v) -> s.withAppearance(id, s.appearance(id).withTextShadow(v))),
                new ModuleSetting.Section("position"),
                new ModuleSetting.Toggle(id == ModuleId.ARMOR_STATUS ? "auto_hotbar" : "auto_stack",
                        s -> !s.placement(id).customPosition(),
                        (s, v) -> autoPosition(s, id, v)),
                new ModuleSetting.Choice<>("anchor", Anchor.SCREEN_ANCHORS, s -> s.placement(id).anchor(),
                        (s, v) -> s.withPlacement(id, s.placement(id).withAnchor(v)), Anchor::label),
                new ModuleSetting.Slider("offset_x", -2048, 2048, 1, s -> s.placement(id).offsetX(),
                        (s, v) -> s.withPlacement(id, s.placement(id).withOffsets(v, s.placement(id).offsetY())),
                        CommonModuleSettings::pixels),
                new ModuleSetting.Slider("offset_y", -2048, 2048, 1, s -> s.placement(id).offsetY(),
                        (s, v) -> s.withPlacement(id, s.placement(id).withOffsets(s.placement(id).offsetX(), v)),
                        CommonModuleSettings::pixels));
    }

    private static HudSettings autoPosition(HudSettings settings, ModuleId id, boolean automatic) {
        var placement = settings.placement(id);
        if (!automatic) {
            return settings.withPlacement(id, placement.withAnchor(placement.docked() ? Anchor.TOP_LEFT : placement.anchor()));
        }
        return settings.withPlacement(id, id == ModuleId.ARMOR_STATUS ? placement.dock() : placement.returnToStack());
    }

    public static Component percent(int value) {
        return Component.translatable("etherhud.advanced.percent", value);
    }

    private static Component pixels(int value) {
        return Component.translatable("etherhud.setting.pixels", value);
    }
}
