package io.github.etherhud.config;

public record ModuleAppearance(int backgroundOpacity, int backgroundColor, int outlineColor,
                               boolean outlineEnabled, int cornerRadius, int textColor, boolean textShadow) {
    public static final ModuleAppearance DEFAULT = new ModuleAppearance(65, 0x1A2532, 0x9CAFB6,
            false, 3, 0xF0F7FC, false);

    public ModuleAppearance {
        backgroundOpacity = Math.clamp(backgroundOpacity, 0, 100);
        backgroundColor &= 0xFFFFFF;
        outlineColor &= 0xFFFFFF;
        cornerRadius = Math.clamp(cornerRadius, 0, 12);
        textColor &= 0xFFFFFF;
    }

    public ModuleAppearance withOpacity(int opacity) {
        return new ModuleAppearance(opacity, backgroundColor, outlineColor, outlineEnabled, cornerRadius, textColor, textShadow);
    }

    public ModuleAppearance withBackgroundColor(int color) {
        return new ModuleAppearance(backgroundOpacity, color, outlineColor, outlineEnabled, cornerRadius, textColor, textShadow);
    }

    public ModuleAppearance withOutlineColor(int color) {
        return new ModuleAppearance(backgroundOpacity, backgroundColor, color, outlineEnabled, cornerRadius, textColor, textShadow);
    }

    public ModuleAppearance withOutlineEnabled(boolean enabled) {
        return new ModuleAppearance(backgroundOpacity, backgroundColor, outlineColor, enabled, cornerRadius, textColor, textShadow);
    }

    public ModuleAppearance withCornerRadius(int radius) {
        return new ModuleAppearance(backgroundOpacity, backgroundColor, outlineColor, outlineEnabled, radius, textColor, textShadow);
    }

    public ModuleAppearance withTextColor(int color) {
        return new ModuleAppearance(backgroundOpacity, backgroundColor, outlineColor, outlineEnabled, cornerRadius, color, textShadow);
    }

    public ModuleAppearance withTextShadow(boolean shadow) {
        return new ModuleAppearance(backgroundOpacity, backgroundColor, outlineColor, outlineEnabled, cornerRadius, textColor, shadow);
    }

    public int background(boolean ghost) {
        int alpha = Math.round(backgroundOpacity * (ghost ? 1.28f : 2.55f));
        return (alpha << 24) | backgroundColor;
    }

    public int outline(boolean ghost) {
        return ((ghost ? 0xA8 : 0xFF) << 24) | outlineColor;
    }

    public int text(boolean ghost) {
        return (ghost ? 0xA8000000 : 0xFF000000) | textColor;
    }
}
