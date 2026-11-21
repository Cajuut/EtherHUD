package io.github.etherhud.layout;

import io.github.etherhud.config.HudSettings;
import io.github.etherhud.config.ArmorSettings.HotbarSide;
import io.github.etherhud.module.ModuleId;

public final class LayoutResolver {
    public static final int MARGIN = 8;
    private static final int GAP = 4;
    private static final float REFERENCE_GUI_SCALE = 2;
    // Vanilla 1.21.4 Gui#renderItemHotbar draws a 182 × 22 sprite centered at the bottom.
    private static final int HALF_HOTBAR = 91;
    private static final int HOTBAR_HEIGHT = 22;

    // Resolved bounds are reused by the caller. All positions and offsets use scaled GUI pixels.
    public static final class Bounds {
        private int x;
        private int y;
        private int width;
        private int height;
        private float scale;
        private float unitScale = 1;
        private double alignX;
        private double alignY;

        public int x() { return x; }
        public int y() { return y; }
        public int width() { return width; }
        public int height() { return height; }
        public int right() { return x + width; }
        public int bottom() { return y + height; }
        public float scale() { return scale; }
        public float unitScale() { return unitScale; }
        public double alignX() { return alignX; }
        public double alignY() { return alignY; }

        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }

    public void resolve(HudSettings settings, Bounds[] bounds, int[] widths, int[] heights,
                        int screenWidth, int screenHeight, boolean showDisabled, int reserveLeft, int reserveRight,
                        double guiScale) {
        float unitScale = screenUnits(guiScale);
        int stackY = Math.round(MARGIN * unitScale);
        for (ModuleId id : ModuleId.ALL) {
            ModulePlacement placement = settings.placement(id);
            if (placement.enabled() && widths[id.ordinal()] > 0) {
                stackY = resolveModule(id, placement, bounds[id.ordinal()], widths[id.ordinal()],
                        heights[id.ordinal()], settings, screenWidth, screenHeight, stackY, reserveLeft, reserveRight, unitScale);
            }
        }
        if (showDisabled) {
            // Ghost rows follow the live stack so their presence never moves an enabled module.
            for (ModuleId id : ModuleId.ALL) {
                ModulePlacement placement = settings.placement(id);
                if (!placement.enabled()) {
                    stackY = resolveModule(id, placement, bounds[id.ordinal()], widths[id.ordinal()],
                            heights[id.ordinal()], settings, screenWidth, screenHeight, stackY, reserveLeft, reserveRight, unitScale);
                }
            }
        }
    }

    private int resolveModule(ModuleId id, ModulePlacement placement, Bounds out, int width, int height,
                              HudSettings settings, int screenWidth, int screenHeight, int stackY,
                              int reserveLeft, int reserveRight, float unitScale) {
        boolean nativeSize = !settings.canResize(id);
        out.unitScale = nativeSize ? 1 : unitScale;
        float scale = nativeSize ? 1 : settings.hudScale() * placement.scale() * unitScale;
        if (placement.docked()) {
            var side = settings.armorStatus().hotbarSide();
            if (nativeSize) {
                vanillaDock(out, width, height, screenWidth, screenHeight, screenWidth / 2 - HALF_HOTBAR - reserveLeft,
                        screenWidth / 2 + HALF_HOTBAR + reserveRight, screenHeight, side);
            } else {
                dock(out, width, height, scale, screenWidth, screenHeight, side, reserveLeft, reserveRight);
            }
            return stackY;
        }
        if (nativeSize && (width > screenWidth || height > screenHeight)) {
            out.width = 0;
            out.height = 0;
            return stackY;
        }
        size(out, width, height, scale, screenWidth, screenHeight);
        int availableX = screenWidth - out.width;
        int availableY = screenHeight - out.height;
        if (placement.customPosition()) {
            Anchor anchor = placement.anchor();
            out.alignX = anchor.horizontal;
            out.alignY = anchor.vertical;
            out.x = (int) Math.clamp(anchorOffset(availableX, anchor.horizontal)
                    + Math.round(placement.offsetX() * (double) out.unitScale), 0, availableX);
            out.y = (int) Math.clamp(anchorOffset(availableY, anchor.vertical)
                    + Math.round(placement.offsetY() * (double) out.unitScale), 0, availableY);
            return stackY;
        }
        out.x = Math.min(Math.round(MARGIN * unitScale), availableX);
        out.y = Math.min(stackY, availableY);
        out.alignX = 0;
        out.alignY = 0;
        return stackY + out.height + Math.round(GAP * unitScale);
    }

    public static boolean vanillaDock(Bounds out, int width, int height, int screenWidth, int screenHeight,
                                      int leftEdge, int rightEdge, int bottom, HotbarSide side) {
        out.unitScale = 1;
        boolean fitsLeft = leftEdge - GAP >= width;
        boolean fitsRight = screenWidth - rightEdge - GAP >= width;
        if ((!fitsLeft && !fitsRight) || height > screenHeight) {
            out.width = 0;
            out.height = 0;
            return false;
        }
        boolean left = fitsLeft && (side == HotbarSide.LEFT || !fitsRight);
        measure(out, width, height, 1, screenWidth, screenHeight,
                left ? leftEdge - GAP : rightEdge + GAP, Math.max(height, bottom), left ? 1 : 0, 1);
        return true;
    }

    private void dock(Bounds out, int width, int height, float scale, int screenWidth, int screenHeight,
                      HotbarSide side, int reserveLeft, int reserveRight) {
        int center = screenWidth / 2;
        boolean left = side == HotbarSide.LEFT;
        int edge = left ? center - HALF_HOTBAR - reserveLeft - GAP : center + HALF_HOTBAR + reserveRight + GAP;
        int available = left ? edge : screenWidth - edge;
        if (available < Math.min(width * scale, Math.max(18, width / 3))) {
            int aboveHotbar = Math.max(1, screenHeight - HOTBAR_HEIGHT - GAP);
            measure(out, width, height, Math.min(scale, aboveHotbar / (float) height),
                    screenWidth, screenHeight, center, aboveHotbar, 0.5, 1);
        } else {
            measure(out, width, height, Math.min(scale, available / (float) width),
                    screenWidth, screenHeight, edge, screenHeight, left ? 1 : 0, 1);
        }
    }

    private static void size(Bounds bounds, int width, int height, float scale, int screenWidth, int screenHeight) {
        bounds.scale = Math.min(scale, Math.min((float) screenWidth / width, (float) screenHeight / height));
        bounds.width = Math.min(screenWidth, (int) Math.ceil(width * bounds.scale));
        bounds.height = Math.min(screenHeight, (int) Math.ceil(height * bounds.scale));
    }

    public static void measure(Bounds bounds, int width, int height, float scale, int screenWidth, int screenHeight,
                               double pivotX, double pivotY, double alignX, double alignY) {
        size(bounds, width, height, scale, screenWidth, screenHeight);
        bounds.x = clampX(bounds, screenWidth, (int) Math.round(pivotX - bounds.width * alignX));
        bounds.y = clampY(bounds, screenHeight, (int) Math.round(pivotY - bounds.height * alignY));
        bounds.alignX = alignX;
        bounds.alignY = alignY;
    }

    public static void measure(Bounds bounds, int width, int height, float scale, int screenWidth, int screenHeight,
                               double pivotX, double pivotY, double alignX, double alignY, float unitScale) {
        bounds.unitScale = unitScale;
        measure(bounds, width, height, scale, screenWidth, screenHeight, pivotX, pivotY, alignX, alignY);
    }

    public static float screenUnits(double guiScale) {
        return REFERENCE_GUI_SCALE / (float) Math.max(1, guiScale);
    }

    public ModulePlacement place(ModulePlacement placement, Bounds bounds, int screenWidth, int screenHeight,
                                 int x, int y, Anchor anchor) {
        int availableX = Math.max(0, screenWidth - bounds.width);
        int availableY = Math.max(0, screenHeight - bounds.height);
        x = clampX(bounds, screenWidth, x);
        y = clampY(bounds, screenHeight, y);
        Anchor resolved = anchor == null || anchor == Anchor.HOTBAR_SIDE
                ? Anchor.nearest(x, y, availableX, availableY) : anchor;
        return new ModulePlacement(placement.enabled(), resolved,
                Math.round((x - anchorOffset(availableX, resolved.horizontal)) / bounds.unitScale),
                Math.round((y - anchorOffset(availableY, resolved.vertical)) / bounds.unitScale),
                placement.scale(), true);
    }

    public static int clampX(Bounds bounds, int screenWidth, int x) {
        return Math.clamp(x, 0, Math.max(0, screenWidth - bounds.width));
    }

    public static int clampY(Bounds bounds, int screenHeight, int y) {
        return Math.clamp(y, 0, Math.max(0, screenHeight - bounds.height));
    }

    public int defaultStackY(ModuleId id, int[] heights, float hudScale, float units) {
        int y = Math.round(MARGIN * units);
        for (int index = 0; index < id.ordinal(); index++) {
            y += (int) Math.ceil(heights[index] * hudScale * units) + Math.round(GAP * units);
        }
        return y;
    }

    private int anchorOffset(int available, double fraction) {
        return (int) Math.round(available * fraction);
    }
}
