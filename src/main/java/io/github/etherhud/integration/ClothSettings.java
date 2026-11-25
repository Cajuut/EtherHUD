package io.github.etherhud.integration;

import io.github.etherhud.EtherHud;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ClothSettings {
    private ClothSettings() {}

    // Called only after Fabric Loader confirms that Cloth Config is present.
    public static Screen create(Screen parent, EtherHud hud) {
        var builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("etherhud.advanced.title"))
                .setSavingRunnable(hud::saveSettings);
        builder.getOrCreateCategory(Component.translatable("etherhud.toggle.master"))
                .addEntry(builder.entryBuilder().startIntSlider(Component.translatable("etherhud.advanced.hud_scale"),
                                Math.round(hud.settings().hudScale() * 100), 50, 300)
                        .setDefaultValue(100)
                        .setTextGetter(scale -> Component.translatable("etherhud.advanced.percent", scale))
                        .setSaveConsumer(scale -> hud.setSettings(hud.settings().withHudScale(scale / 100f)))
                        .build());
        return builder.build();
    }
}
