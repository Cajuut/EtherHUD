package io.github.etherhud.ui.widget;

import io.github.etherhud.render.GlassPanel;
import io.github.etherhud.render.GlassRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public final class GlassButton extends AbstractButton {
    private final Runnable action;

    public GlassButton(int x, int y, int width, int height, Component label, Runnable action) {
        super(x, y, width, height, label);
        this.action = action;
    }

    @Override
    public void onPress() {
        action.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        boolean highlighted = active && isHoveredOrFocused();
        int color = !active ? 0xA01E2A34 : highlighted ? 0xE0385966 : 0xC4243948;
        int stroke = isFocused() ? GlassPanel.ACCENT : highlighted ? 0x8C79DDE6 : 0x4C8297A5;
        GlassRenderer.rounded(graphics, getX(), getY(), width, height, 5, 5, 5, 5, 1, color, stroke);
        renderScrollingString(graphics, Minecraft.getInstance().font, 4, active ? GlassPanel.TEXT : GlassPanel.MUTED);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
