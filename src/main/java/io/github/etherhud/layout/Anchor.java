package io.github.etherhud.layout;

import net.minecraft.network.chat.Component;
import java.util.List;

public enum Anchor {
    TOP_LEFT(0, 0, "top_left"),
    TOP_RIGHT(1, 0, "top_right"),
    BOTTOM_LEFT(0, 1, "bottom_left"),
    BOTTOM_RIGHT(1, 1, "bottom_right"),
    CENTER(0.5, 0.5, "center"),
    TOP_CENTER(0.5, 0, "top_center"),
    HOTBAR_SIDE(0.5, 1, "hotbar_side");

    private static final Anchor[] OPTIONS = {TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER, TOP_CENTER};
    public static final List<Anchor> SCREEN_ANCHORS = List.of(OPTIONS);

    final double horizontal;
    final double vertical;
    private final String translationKey;

    Anchor(double horizontal, double vertical, String key) {
        this.horizontal = horizontal;
        this.vertical = vertical;
        this.translationKey = "etherhud.anchor." + key;
    }

    public Anchor next() {
        return this == HOTBAR_SIDE ? TOP_LEFT : OPTIONS[(ordinal() + 1) % OPTIONS.length];
    }

    public Component label() {
        return Component.translatable(translationKey);
    }

    static Anchor nearest(int x, int y, int availableX, int availableY) {
        Anchor nearest = TOP_LEFT;
        double shortest = Double.POSITIVE_INFINITY;
        for (Anchor anchor : OPTIONS) {
            double dx = x - availableX * anchor.horizontal;
            double dy = y - availableY * anchor.vertical;
            double distance = dx * dx + dy * dy;
            if (distance < shortest) {
                shortest = distance;
                nearest = anchor;
            }
        }
        return nearest;
    }
}
