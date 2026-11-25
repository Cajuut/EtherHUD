package io.github.etherhud.layout;

public record ModulePlacement(
        boolean enabled, Anchor anchor, int offsetX, int offsetY, float scale, boolean customPosition
) {
    public static final int SCALE_STEPS = 20;
    public static final ModulePlacement DEFAULT = new ModulePlacement(true, Anchor.TOP_LEFT, 0, 0, 1, false);
    public static final ModulePlacement ARMOR_DEFAULT = new ModulePlacement(true, Anchor.HOTBAR_SIDE, 0, 0, 1, false);

    public ModulePlacement {
        if (anchor == null) {
            anchor = Anchor.TOP_LEFT;
        }
        if (anchor == Anchor.HOTBAR_SIDE) {
            customPosition = false;
            offsetX = 0;
            offsetY = 0;
        }
        scale = Math.round(normalizeScale(scale) * SCALE_STEPS) / (float) SCALE_STEPS;
    }

    public static float normalizeScale(float scale) {
        return Float.isFinite(scale) ? Math.clamp(scale, 0.5f, 3.0f) : 1;
    }

    public ModulePlacement withEnabled(boolean enabled) {
        return new ModulePlacement(enabled, anchor, offsetX, offsetY, scale, customPosition);
    }

    public ModulePlacement withScale(float scale) {
        return new ModulePlacement(enabled, anchor, offsetX, offsetY, scale, customPosition);
    }

    public ModulePlacement withAnchor(Anchor anchor) {
        return new ModulePlacement(enabled, anchor, offsetX, offsetY, scale, true);
    }

    public ModulePlacement withOffsets(int x, int y) {
        return new ModulePlacement(enabled, docked() ? Anchor.TOP_LEFT : anchor, x, y, scale, true);
    }

    public boolean docked() { return anchor == Anchor.HOTBAR_SIDE; }

    public ModulePlacement dock() {
        return new ModulePlacement(enabled, Anchor.HOTBAR_SIDE, 0, 0, scale, false);
    }

    public ModulePlacement returnToStack() {
        return new ModulePlacement(enabled, Anchor.TOP_LEFT, 0, 0, scale, false);
    }
}
