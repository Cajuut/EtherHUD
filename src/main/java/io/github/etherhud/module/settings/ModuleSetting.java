package io.github.etherhud.module.settings;

import io.github.etherhud.config.HudSettings;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public sealed interface ModuleSetting {
    String key();

    default Component label() {
        return Component.translatable("etherhud.setting." + key());
    }

    record Section(String key) implements ModuleSetting {}

    record Toggle(String key, Predicate<HudSettings> read,
                  BiFunction<HudSettings, Boolean, HudSettings> write) implements ModuleSetting {}

    record Slider(String key, int min, int max, int step, ToIntFunction<HudSettings> read,
                  BiFunction<HudSettings, Integer, HudSettings> write,
                  IntFunction<Component> display) implements ModuleSetting {}

    record Choice<T>(String key, List<T> options, Function<HudSettings, T> read,
                     BiFunction<HudSettings, T, HudSettings> write,
                     Function<T, Component> display) implements ModuleSetting {}

    record Color(String key, ToIntFunction<HudSettings> read,
                 BiFunction<HudSettings, Integer, HudSettings> write) implements ModuleSetting {}

    record ImageFile(String key, Function<HudSettings, String> read,
                     BiFunction<HudSettings, String, HudSettings> write) implements ModuleSetting {}
}
