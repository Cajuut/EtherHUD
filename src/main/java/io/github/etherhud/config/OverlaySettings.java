package io.github.etherhud.config;

/** Client-side adjustments to vanilla first-person and camera overlays. */
public record OverlaySettings(float totemScale, int totemOffsetY, int fireOffsetY,
                              float worldFireScale,
                              int totemSpeed, float mainHandScale, int mainHandOffsetY,
                              float offHandScale, int offHandOffsetY, int shieldOpacity) {
    public static final OverlaySettings DEFAULT = new OverlaySettings(1, 0, 0, 1, 100, 1, 0, 1, 0, 100);

    public OverlaySettings {
        totemScale = clamp(totemScale, 0.1f, 2.0f);
        totemOffsetY = Math.clamp(totemOffsetY, -100, 100);
        fireOffsetY = Math.clamp(fireOffsetY, -100, 100);
        worldFireScale = clamp(worldFireScale, 0.25f, 2.0f);
        mainHandScale = clamp(mainHandScale, 0.5f, 2.0f);
        mainHandOffsetY = Math.clamp(mainHandOffsetY, -100, 100);
        offHandScale = clamp(offHandScale, 0.5f, 2.0f);
        offHandOffsetY = Math.clamp(offHandOffsetY, -100, 100);
        shieldOpacity = Math.clamp(shieldOpacity, 0, 100);
        totemSpeed = Math.clamp(totemSpeed, 25, 300);
    }

    private static float clamp(float value, float min, float max) {
        return Float.isFinite(value) ? Math.clamp(value, min, max) : 1;
    }

    public OverlaySettings withTotemScale(float value) { return new OverlaySettings(value, totemOffsetY, fireOffsetY, worldFireScale, totemSpeed, mainHandScale, mainHandOffsetY, offHandScale, offHandOffsetY, shieldOpacity); }
    public OverlaySettings withTotemOffsetY(int value) { return new OverlaySettings(totemScale, value, fireOffsetY, worldFireScale, totemSpeed, mainHandScale, mainHandOffsetY, offHandScale, offHandOffsetY, shieldOpacity); }
    public OverlaySettings withFireOffsetY(int value) { return new OverlaySettings(totemScale, totemOffsetY, value, worldFireScale, totemSpeed, mainHandScale, mainHandOffsetY, offHandScale, offHandOffsetY, shieldOpacity); }
    public OverlaySettings withWorldFireScale(float value) { return new OverlaySettings(totemScale, totemOffsetY, fireOffsetY, value, totemSpeed, mainHandScale, mainHandOffsetY, offHandScale, offHandOffsetY, shieldOpacity); }
    public OverlaySettings withMainHandScale(float value) { return new OverlaySettings(totemScale, totemOffsetY, fireOffsetY, worldFireScale, totemSpeed, value, mainHandOffsetY, offHandScale, offHandOffsetY, shieldOpacity); }
    public OverlaySettings withMainHandOffsetY(int value) { return new OverlaySettings(totemScale, totemOffsetY, fireOffsetY, worldFireScale, totemSpeed, mainHandScale, value, offHandScale, offHandOffsetY, shieldOpacity); }
    public OverlaySettings withOffHandScale(float value) { return new OverlaySettings(totemScale, totemOffsetY, fireOffsetY, worldFireScale, totemSpeed, mainHandScale, mainHandOffsetY, value, offHandOffsetY, shieldOpacity); }
    public OverlaySettings withOffHandOffsetY(int value) { return new OverlaySettings(totemScale, totemOffsetY, fireOffsetY, worldFireScale, totemSpeed, mainHandScale, mainHandOffsetY, offHandScale, value, shieldOpacity); }
    public OverlaySettings withShieldOpacity(int value) { return new OverlaySettings(totemScale, totemOffsetY, fireOffsetY, worldFireScale, totemSpeed, mainHandScale, mainHandOffsetY, offHandScale, offHandOffsetY, value); }
    public OverlaySettings withTotemSpeed(int value) {
        return new OverlaySettings(totemScale, totemOffsetY, fireOffsetY, worldFireScale,
                value, mainHandScale, mainHandOffsetY, offHandScale, offHandOffsetY, shieldOpacity);
    }
}
