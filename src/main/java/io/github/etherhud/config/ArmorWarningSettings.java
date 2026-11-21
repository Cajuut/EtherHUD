package io.github.etherhud.config;

public record ArmorWarningSettings(boolean enabled, int threshold, int channels) {
    public enum Channel {
        BLINK("blink"), ICON("icon"), SOUND("sound"), ACTION_BAR("action_bar");

        public static final Channel[] ALL = values();
        private final String key;

        Channel(String key) { this.key = key; }

        public String key() { return key; }
        public int bit() { return 1 << ordinal(); }
    }

    public static final ArmorWarningSettings DEFAULT =
            new ArmorWarningSettings(true, 20, Channel.BLINK.bit() | Channel.ICON.bit());

    public ArmorWarningSettings {
        threshold = Math.clamp(threshold, 5, 50);
        channels &= (1 << Channel.ALL.length) - 1;
    }

    public boolean uses(Channel channel) {
        return (channels & channel.bit()) != 0;
    }

    public ArmorWarningSettings withEnabled(boolean enabled) {
        return new ArmorWarningSettings(enabled, threshold, channels);
    }

    public ArmorWarningSettings withThreshold(int threshold) {
        return new ArmorWarningSettings(enabled, threshold, channels);
    }

    public ArmorWarningSettings withChannel(Channel channel, boolean enabled) {
        int mask = enabled ? channels | channel.bit() : channels & ~channel.bit();
        return new ArmorWarningSettings(this.enabled, threshold, mask);
    }
}
