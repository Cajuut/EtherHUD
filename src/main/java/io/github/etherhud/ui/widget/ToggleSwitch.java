package io.github.etherhud.ui.widget;

import io.github.etherhud.render.GlassPanel;
import io.github.etherhud.render.GlassRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

public final class ToggleSwitch extends AbstractButton {
    private static final long DURATION = 150_000_000L;
    private static final Component ON_TEXT = Component.translatable("etherhud.state.on");
    private static final Component OFF_TEXT = Component.translatable("etherhud.state.off");

    private final Runnable action;
    private final Component label;
    private boolean enabled;
    private float start;
    private long startedAt;

    public ToggleSwitch(int x, int y, Component label, boolean enabled, Runnable action) {
        super(x, y, 34, 18, label);
        this.action = action;
        this.label = label;
        this.enabled = enabled;
        updateMessage();
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        long now = System.nanoTime();
        start = position(now);
        startedAt = now;
        this.enabled = enabled;
        updateMessage();
    }

    private void updateMessage() {
        setMessage(Component.translatable("etherhud.state.label", label, stateLabel(enabled)));
    }

    public static Component stateLabel(boolean enabled) {
        return enabled ? ON_TEXT : OFF_TEXT;
    }

    private float position(long now) {
        float target = enabled ? 1 : 0;
        if (startedAt == 0) {
            return target;
        }
        float time = Math.clamp((float) (now - startedAt) / DURATION, 0, 1);
        if (time == 1) {
            startedAt = 0;
            return target;
        }
        float eased = time * time * (3 - 2 * time);
        return start + (target - start) * eased;
    }

    private int trackColor(float position) {
        int red = Math.round(0x62 + (0x35 - 0x62) * position);
        int green = Math.round(0x70 + (0xB9 - 0x70) * position);
        int blue = Math.round(0x7C + (0xC8 - 0x7C) * position);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        action.run();
    }

    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        float position = position(startedAt == 0 ? 0 : System.nanoTime());
        int x = getX();
        int y = getY();
        int stroke = isFocused() ? GlassPanel.ACCENT : isHovered() ? 0xAAB9E7EE : 0x4DFFFFFF;
        GlassRenderer.rounded(graphics, x, y, width, height, 9, 9, 9, 9, 1, trackColor(position), stroke);
        int knobX = x + 3 + Math.round(position * 16);
        GlassRenderer.circle(graphics, knobX, y + 3, 12, 1, 0xFFF7FBFC, 0x70FFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
