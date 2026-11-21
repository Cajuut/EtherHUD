package io.github.etherhud.module;

import io.github.etherhud.config.HudSettings;
import io.github.etherhud.module.armor.ArmorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;

public final class HudModules {
    private static final long SECOND = 1_000_000_000L;

    private final Clicks clicks = new Clicks();
    private final ArmorStatus armor = new ArmorStatus();
    private final TpsHud tps = new TpsHud();
    private final HudModule[] modules = {
            new Fps(), new Coordinates(), clicks, armor, new Ping(), tps, new PlayerModelHud(),
            new ItemCountHud(), new CompassHud(), new ImageHud()
    };

    public ArmorStatus armor() { return armor; }
    public TpsHud tps() { return tps; }

    public HudModule get(ModuleId id) {
        return modules[id.ordinal()];
    }

    public void click(boolean right) {
        clicks.click(right);
    }

    public void clearClicks() {
        clicks.left.clear();
        clicks.right.clear();
    }

    private abstract static class TextModule implements HudModule {
        Component live = Component.empty();

        @Override
        public Component liveText() { return live; }

    }

    private static final class Fps extends TextModule {
        private int last = -1;

        @Override
        public Component previewText() { return Component.translatable("etherhud.hud.fps", 144); }

        @Override
        public void update(Minecraft client, HudSettings settings) {
            int fps = client.getFps();
            if (last != fps) {
                last = fps;
                live = Component.translatable("etherhud.hud.fps", fps);
            }
        }
    }

    private static final class Coordinates extends TextModule {
        private long x = Long.MIN_VALUE;
        private long y;
        private long z;

        @Override
        public Component previewText() { return Component.translatable("etherhud.hud.coordinates", "123.4", "64.0", "-56.7"); }

        @Override
        public void update(Minecraft client, HudSettings settings) {
            long nextX = Math.round(client.player.getX() * 10);
            long nextY = Math.round(client.player.getY() * 10);
            long nextZ = Math.round(client.player.getZ() * 10);
            if (x != nextX || y != nextY || z != nextZ) {
                x = nextX;
                y = nextY;
                z = nextZ;
                live = Component.translatable("etherhud.hud.coordinates", tenths(x), tenths(y), tenths(z));
            }
        }

        private String tenths(long value) {
            return (value < 0 ? "-" : "") + Math.abs(value / 10) + "." + Math.abs(value % 10);
        }
    }

    private static final class Clicks extends TextModule {
        private final ArrayDeque<Long> left = new ArrayDeque<>();
        private final ArrayDeque<Long> right = new ArrayDeque<>();
        private int lastLeft = -1;
        private int lastRight = -1;

        @Override
        public Component previewText() { return Component.translatable("etherhud.hud.cps", 8, 6); }

        void click(boolean rightButton) {
            (rightButton ? right : left).addLast(System.nanoTime());
        }

        @Override
        public void update(Minecraft client, HudSettings settings) {
            long cutoff = System.nanoTime() - SECOND;
            expire(left, cutoff);
            expire(right, cutoff);
            if (left.size() != lastLeft || right.size() != lastRight) {
                lastLeft = left.size();
                lastRight = right.size();
                live = Component.translatable("etherhud.hud.cps", lastLeft, lastRight);
            }
        }

        private void expire(ArrayDeque<Long> clicks, long cutoff) {
            while (!clicks.isEmpty() && clicks.peekFirst() <= cutoff) {
                clicks.removeFirst();
            }
        }
    }

    private static final class Ping extends TextModule {
        private static final Component UNAVAILABLE = Component.translatable("etherhud.hud.ping", "—");
        private int last = -1;
        private int ticksUntilSample;

        Ping() {
            live = UNAVAILABLE;
        }

        @Override
        public Component previewText() {
            return Component.translatable("etherhud.hud.ping", 42);
        }

        @Override
        public void update(Minecraft client, HudSettings settings) {
            if (ticksUntilSample-- > 0) {
                return;
            }
            ticksUntilSample = 19;
            var connection = client.getConnection();
            var info = connection == null ? null : connection.getPlayerInfo(client.player.getUUID());
            int latency = client.isLocalServer() || info == null ? -1 : info.getLatency();
            if (latency != last) {
                last = latency;
                live = latency < 0 ? UNAVAILABLE : Component.translatable("etherhud.hud.ping", latency);
            }
        }

        @Override
        public void onInactive() {
            last = -1;
            ticksUntilSample = 0;
            live = UNAVAILABLE;
        }
    }
}
