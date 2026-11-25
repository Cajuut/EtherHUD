package io.github.etherhud.ui.widget;

import io.github.etherhud.render.GlassPanel;
import io.github.etherhud.render.GlassRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public final class GlassButton extends AbstractButton {
    private final Runnable action;

    public GlassButton(int x, int y, int width, int height, Component label, Runnable action) {
        super(x, y, width, height, label);
        this.action = action;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        action.run();
    }

    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        boolean highlighted = active && isHoveredOrFocused();
        int color = !active ? 0xA01E2A34 : highlighted ? 0xE0385966 : 0xC4243948;
        int stroke = isFocused() ? GlassPanel.ACCENT : highlighted ? 0x8C79DDE6 : 0x4C8297A5;
        GlassRenderer.rounded(graphics, getX(), getY(), width, height, 5, 5, 5, 5, 1, color, stroke);
        int textColor = active ? GlassPanel.TEXT : GlassPanel.MUTED;
        Component message = getMessage().copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(textColor & 0xFFFFFF)));
        ActiveTextCollector collector = graphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE);
        renderScrollingStringOverContents(collector, message, 4);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
