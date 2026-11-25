package io.github.etherhud.module;

import io.github.etherhud.config.HudSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;

public final class TpsHud implements HudModule {
    private static final long SECOND = 1_000_000_000L;
    private static final Component UNAVAILABLE = label("—");
    private ClientLevel sampledLevel;
    private long sampleTime;
    private long sampleTick;
    private double estimate = Double.NaN;
    private int lastTenths = -1;
    private Component live = UNAVAILABLE;

    public void observe(ClientLevel level, long gameTime) {
        long now = System.nanoTime();
        long ticks = gameTime - sampleTick;
        if (level != sampledLevel || ticks < 0) {
            estimate = Double.NaN;
        } else if (ticks < 20 || now == sampleTime) {
            return;
        } else {
            double target = level.tickRateManager().tickrate();
            double sample = Math.min(target, ticks * (double) SECOND / (now - sampleTime));
            estimate = Double.isNaN(estimate) ? sample : estimate + (sample - estimate) / 3;
        }
        sampledLevel = level;
        sampleTime = now;
        sampleTick = gameTime;
    }

    @Override
    public void update(Minecraft client, HudSettings settings) {
        var clock = client.level.tickRateManager();
        double target = clock.tickrate();
        double staleAfter = Math.max(8 * SECOND, 60 * SECOND / target);
        int tenths = -1;
        if (clock.isFrozen()) {
            sampledLevel = null;
            estimate = Double.NaN;
            tenths = 0;
        } else if (sampledLevel == client.level && !Double.isNaN(estimate)
                && System.nanoTime() - sampleTime <= staleAfter) {
            tenths = (int) Math.round(Math.min(estimate, target) * 10);
        }
        if (tenths != lastTenths) {
            lastTenths = tenths;
            live = tenths < 0 ? UNAVAILABLE : label(tenths / 10 + "." + tenths % 10);
        }
    }

    @Override
    public void onInactive() {
        sampledLevel = null;
        estimate = Double.NaN;
        lastTenths = -1;
        live = UNAVAILABLE;
    }

    @Override
    public Component liveText() { return live; }

    @Override
    public Component previewText() { return label("20.0"); }

    private static Component label(String value) {
        return Component.translatable("etherhud.hud.tps", value);
    }
}
