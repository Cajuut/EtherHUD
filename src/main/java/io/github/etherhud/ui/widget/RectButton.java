package io.github.etherhud.ui.widget;

import io.github.etherhud.render.GlassPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public final class RectButton extends AbstractButton {
    private final Runnable action;

    public RectButton(int x, int y, int width, int height, Component label, Runnable action) {
        super(x, y, width, height, label);
        this.action = action;
    }

    @Override
    public void onPress() {
        action.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(getX(), getY(), getX() + width, getY() + height,
                isHoveredOrFocused() ? 0xFF385966 : 0xFF243948);
        graphics.renderOutline(getX(), getY(), width, height, isFocused() ? GlassPanel.ACCENT : 0x7E8297A5);
        renderScrollingString(graphics, Minecraft.getInstance().font, 4, GlassPanel.TEXT);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
