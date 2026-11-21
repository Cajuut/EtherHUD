package io.github.etherhud.ui.editor;

import io.github.etherhud.layout.ModulePlacement;
import io.github.etherhud.module.ModuleId;
import io.github.etherhud.render.GlassPanel;
import io.github.etherhud.ui.widget.GlassButton;
import io.github.etherhud.ui.widget.ToggleSwitch;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

public final class ModuleContextPanel {
    public enum Action { TOGGLE, NEXT_ANCHOR, SCALE_DOWN, SCALE_UP, RESET_POSITION, RETURN_TO_STACK, SETTINGS }

    private static final int HEIGHT = 151;
    private static final int ROW_HEIGHT = 18;

    private final int x;
    private final int y;
    private final int width;
    private final Component title;
    private final Component visibility = Component.translatable("etherhud.editor.visible");
    private final ToggleSwitch enabled;
    private final GlassButton anchor;
    private final GlassButton smaller;
    private final GlassButton larger;
    private final GlassButton backToStack;
    private final boolean resizable;
    private final List<AbstractWidget> widgets;
    private Component scale;

    public ModuleContextPanel(int mouseX, int mouseY, int screenWidth, int screenHeight, int toolbarBottom, ModuleId id,
                              ModulePlacement placement, boolean resizable, Consumer<Action> action) {
        this.resizable = resizable;
        width = Math.min(176, screenWidth - 8);
        x = Math.clamp(mouseX + 4, 4, screenWidth - width - 4);
        int bottom = Math.max(4, screenHeight - HEIGHT - 4);
        y = Math.clamp(mouseY + 4, Math.min(toolbarBottom + 4, bottom), bottom);
        title = id.label();

        int left = x + 6;
        int right = x + width - 6;
        enabled = new ToggleSwitch(right - 34, y + 19, visibility, placement.enabled(), () -> action.accept(Action.TOGGLE));
        anchor = new GlassButton(left, y + 39, width - 12, ROW_HEIGHT,
                Component.empty(), () -> action.accept(Action.NEXT_ANCHOR));
        smaller = new GlassButton(left, y + 61, 22, ROW_HEIGHT,
                Component.translatable("etherhud.editor.scale_down"), () -> action.accept(Action.SCALE_DOWN));
        larger = new GlassButton(right - 22, y + 61, 22, ROW_HEIGHT,
                Component.translatable("etherhud.editor.scale_up"), () -> action.accept(Action.SCALE_UP));
        GlassButton reset = new GlassButton(left, y + 83, width - 12, ROW_HEIGHT,
                Component.translatable("etherhud.editor.reset_position"), () -> action.accept(Action.RESET_POSITION));
        backToStack = new GlassButton(left, y + 105, width - 12, ROW_HEIGHT,
                Component.translatable(id == ModuleId.ARMOR_STATUS ? "etherhud.editor.return_to_hotbar" : "etherhud.editor.return_to_stack"),
                () -> action.accept(Action.RETURN_TO_STACK));
        GlassButton details = new GlassButton(left, y + 127, width - 12, ROW_HEIGHT,
                Component.translatable("etherhud.advanced.title"), () -> action.accept(Action.SETTINGS));
        widgets = List.of(enabled, anchor, smaller, larger, reset, backToStack, details);
        update(placement);
    }

    public void update(ModulePlacement placement) {
        enabled.setEnabled(placement.enabled());
        Component value = placement.customPosition() || placement.docked() ? placement.anchor().label()
                : Component.translatable("etherhud.editor.automatic");
        anchor.setMessage(Component.translatable("etherhud.editor.anchor", value));
        smaller.active = resizable && placement.scale() > 0.5f;
        larger.active = resizable && placement.scale() < 3;
        scale = resizable ? Component.translatable("etherhud.editor.scale", Math.round(placement.scale() * 100))
                : Component.translatable("etherhud.editor.gui_scale_locked");
        backToStack.active = placement.customPosition();
    }

    public List<AbstractWidget> widgets() { return widgets; }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + HEIGHT;
    }

    public void draw(GuiGraphics graphics, Font font) {
        GlassPanel.draw(graphics, x, y, width, HEIGHT, 0xEE1B2A37);
        graphics.drawString(font, title, x + 8, y + 7, GlassPanel.TEXT, false);
        graphics.drawString(font, visibility, x + 8, y + 24, GlassPanel.MUTED, false);
        graphics.drawCenteredString(font, scale, x + width / 2, y + 66, GlassPanel.TEXT);
    }
}
