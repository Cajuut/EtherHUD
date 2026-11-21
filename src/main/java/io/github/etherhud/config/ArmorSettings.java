package io.github.etherhud.config;

import io.github.etherhud.module.armor.ArmorSlot;

public record ArmorSettings(int slotMask, Direction direction, ValueMode valueMode,
                            boolean showCount, boolean hideEmpty, boolean showBar, ArmorWarningSettings warning,
                            HotbarSide hotbarSide, Style style) {
    public enum Direction { HORIZONTAL, VERTICAL }
    public enum ValueMode { NONE, NUMERIC, PERCENT }
    public enum HotbarSide { LEFT, RIGHT }
    public enum Style { CURRENT, JOINED, VANILLA }

    public static final ArmorSettings DEFAULT = new ArmorSettings(
            ArmorSlot.ALL_VISIBLE, Direction.HORIZONTAL, ValueMode.NONE, true, true, true,
            ArmorWarningSettings.DEFAULT, HotbarSide.RIGHT, Style.CURRENT);

    public ArmorSettings {
        slotMask &= ArmorSlot.ALL_VISIBLE;
    }

    public boolean shows(ArmorSlot slot) { return (slotMask & slot.bit()) != 0; }
    public boolean vanilla() { return style == Style.VANILLA; }
    public boolean vertical() { return direction == Direction.VERTICAL; }
    public int gap() { return style == Style.CURRENT ? 2 : 0; }

    public ArmorSettings withSlot(ArmorSlot slot, boolean enabled) {
        return new ArmorSettings(enabled ? slotMask | slot.bit() : slotMask & ~slot.bit(),
                direction, valueMode, showCount, hideEmpty, showBar, warning, hotbarSide, style);
    }

    public ArmorSettings withDirection(Direction direction) {
        return new ArmorSettings(slotMask, direction, valueMode, showCount, hideEmpty, showBar, warning, hotbarSide, style);
    }

    public ArmorSettings withValueMode(ValueMode valueMode) {
        return new ArmorSettings(slotMask, direction, valueMode, showCount, hideEmpty, showBar, warning, hotbarSide, style);
    }

    public ArmorSettings withShowCount(boolean show) {
        return new ArmorSettings(slotMask, direction, valueMode, show, hideEmpty, showBar, warning, hotbarSide, style);
    }

    public ArmorSettings withHideEmpty(boolean hide) {
        return new ArmorSettings(slotMask, direction, valueMode, showCount, hide, showBar, warning, hotbarSide, style);
    }

    public ArmorSettings withShowBar(boolean show) {
        return new ArmorSettings(slotMask, direction, valueMode, showCount, hideEmpty, show, warning, hotbarSide, style);
    }

    public ArmorSettings withWarning(ArmorWarningSettings warning) {
        return new ArmorSettings(slotMask, direction, valueMode, showCount, hideEmpty, showBar, warning, hotbarSide, style);
    }

    public ArmorSettings withHotbarSide(HotbarSide side) {
        return new ArmorSettings(slotMask, direction, valueMode, showCount, hideEmpty, showBar, warning, side, style);
    }

    public ArmorSettings withStyle(Style style) {
        return new ArmorSettings(slotMask, direction, valueMode, showCount, hideEmpty, showBar, warning, hotbarSide, style);
    }
}
