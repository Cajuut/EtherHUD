package io.github.etherhud.module.armor;

public final class LowDurabilityWarning {
    private static final long COOLDOWN_NANOS = 10_000_000_000L;
    private static final int RECOVERY_MARGIN = 5;

    private boolean low;
    private boolean critical;
    private boolean armed;
    private boolean emitted;
    private long lastEmission;

    public boolean observe(int remaining, int maximum, int threshold, boolean newBaseline, boolean notify, long now) {
        long percent = 100L * remaining;
        boolean wasLow = low;
        low = maximum > 0 && percent < (long) maximum * threshold;
        critical = low && (remaining <= 1 || percent <= 5L * maximum);
        if (newBaseline || maximum <= 0) {
            armed = !low;
            return false;
        }
        if (percent > (long) maximum * (threshold + RECOVERY_MARGIN)) {
            armed = true;
        }
        if (!low || wasLow || !armed) {
            return false;
        }
        armed = false;
        if (!notify || emitted && now - lastEmission < COOLDOWN_NANOS) {
            return false;
        }
        emitted = true;
        lastEmission = now;
        return true;
    }

    public boolean low() { return low; }
    public boolean critical() { return critical; }
}
