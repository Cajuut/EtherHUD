package io.github.etherhud.ui.widget;

import io.github.etherhud.render.GlassPanel;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public final class RectButton extends AbstractButton {
    private final Runnable action;

    public RectButton(int x, int y, int width, int height, Component label, Runnable action) {
        super(x, y, width, height, label);
        this.action = action;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        action.run();
    }

    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(getX(), getY(), getX() + width, getY() + height,
                isHoveredOrFocused() ? 0xFF385966 : 0xFF243948);
        graphics.renderOutline(getX(), getY(), width, height, isFocused() ? GlassPanel.ACCENT : 0x7E8297A5);
        Component message = getMessage().copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(GlassPanel.TEXT & 0xFFFFFF)));
        ActiveTextCollector collector = graphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE);
        renderScrollingStringOverContents(collector, message, 4);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
