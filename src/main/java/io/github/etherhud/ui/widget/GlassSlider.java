package io.github.etherhud.ui.widget;

import io.github.etherhud.render.GlassPanel;
import io.github.etherhud.render.GlassRenderer;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.function.IntConsumer;
import java.util.function.IntFunction;

public final class GlassSlider extends AbstractSliderButton {
    private final int minimum;
    private final int maximum;
    private final int step;
    private final Component label;
    private final IntFunction<Component> display;
    private final IntConsumer update;

    public GlassSlider(int x, int width, Component label, int min, int max, int step, int current,
                       IntFunction<Component> display, IntConsumer update) {
        super(x, 0, width, 18, Component.empty(), (Math.clamp(current, min, max) - min) / (double) (max - min));
        this.minimum = min;
        this.maximum = max;
        this.step = step;
        this.label = label;
        this.display = display;
        this.update = update;
        updateMessage();
    }

    private int selectedValue() {
        return minimum + (int) Math.round(value * (maximum - minimum) / step) * step;
    }

    public void setCurrent(int current) {
        value = (Math.clamp(current, minimum, maximum) - minimum) / (double) (maximum - minimum);
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(display.apply(selectedValue()));
    }

    @Override
    protected MutableComponent createNarrationMessage() {
        return Component.translatable("etherhud.state.label", label, getMessage());
    }

    @Override
    protected void applyValue() {
        update.accept(selectedValue());
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (active && isFocused() && (keyEvent.isLeft() || keyEvent.isRight())) {
            int next = Math.clamp(selectedValue() + (keyEvent.isLeft() ? -step : step), minimum, maximum);
            value = (next - minimum) / (double) (maximum - minimum);
            applyValue();
            updateMessage();
            return true;
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        boolean highlighted = isHoveredOrFocused();
        GlassRenderer.rounded(graphics, x, y, width, height, 5, 5, 5, 5, 1,
                highlighted ? 0xE02C4654 : 0xC8203441,
                isFocused() ? GlassPanel.ACCENT : highlighted ? 0x8C79DDE6 : 0x477996A5);
        int trackWidth = width - 12;
        graphics.fill(x + 6, y + height - 4, x + 6 + trackWidth, y + height - 2, 0x8C172630);
        int progress = (int) Math.round(value * trackWidth);
        if (progress > 0) {
            GlassRenderer.rounded(graphics, x + 6, y + height - 4, progress, 2, 1,
                    active ? GlassPanel.ACCENT : GlassPanel.MUTED);
        }
        int textColor = active ? GlassPanel.TEXT : GlassPanel.MUTED;
        Component message = getMessage().copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(textColor & 0xFFFFFF)));
        ActiveTextCollector collector = graphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE);
        renderScrollingStringOverContents(collector, message, 4);
    }
}
