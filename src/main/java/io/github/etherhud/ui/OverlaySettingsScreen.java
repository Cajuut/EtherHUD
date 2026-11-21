package io.github.etherhud.ui;

import io.github.etherhud.EtherHud;
import io.github.etherhud.config.HudSettings;
import io.github.etherhud.config.OverlaySettings;
import io.github.etherhud.render.GlassPanel;
import io.github.etherhud.ui.widget.GlassButton;
import io.github.etherhud.ui.widget.GlassSlider;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class OverlaySettingsScreen extends GlassSettingsScreen {
    private static final int ROW_HEIGHT = 24;
    private static final List<Control> CONTROLS = List.of(
            new Control("totem_scale", 10, 200, 5, s -> Math.round(s.totemScale() * 100), (s, v) -> s.withTotemScale(v / 100f), Display.PERCENT),
            new Control("totem_height", -100, 100, 1, OverlaySettings::totemOffsetY, OverlaySettings::withTotemOffsetY, Display.PIXELS),
            new Control("totem_speed", 25, 300, 5, OverlaySettings::totemSpeed, OverlaySettings::withTotemSpeed, Display.PERCENT),
            new Control("fire_height", -100, 100, 1, OverlaySettings::fireOffsetY, OverlaySettings::withFireOffsetY, Display.BLOCKS),
            new Control("world_fire_scale", 25, 200, 5, s -> Math.round(s.worldFireScale() * 100),
                    (s, v) -> s.withWorldFireScale(v / 100f), Display.PERCENT),
            new Control("main_hand_scale", 50, 200, 5, s -> Math.round(s.mainHandScale() * 100), (s, v) -> s.withMainHandScale(v / 100f), Display.PERCENT),
            new Control("main_hand_height", -100, 100, 1, OverlaySettings::mainHandOffsetY, OverlaySettings::withMainHandOffsetY, Display.BLOCKS),
            new Control("off_hand_scale", 50, 200, 5, s -> Math.round(s.offHandScale() * 100), (s, v) -> s.withOffHandScale(v / 100f), Display.PERCENT),
            new Control("off_hand_height", -100, 100, 1, OverlaySettings::offHandOffsetY, OverlaySettings::withOffHandOffsetY, Display.BLOCKS),
            new Control("shield_opacity", 0, 100, 1, OverlaySettings::shieldOpacity, OverlaySettings::withShieldOpacity, Display.PERCENT));

    private enum Display { PERCENT, PIXELS, BLOCKS }

    private record Control(String key, int min, int max, int step, Reader read, Writer write, Display display) {
        Component label() { return Component.translatable("etherhud.overlay." + key); }
        int read(HudSettings settings) { return read.read(settings.overlays()); }
        HudSettings write(HudSettings settings, int value) { return settings.withOverlays(write.write(settings.overlays(), value)); }
        Component display(int value) {
            return switch (display) {
                case PERCENT -> Component.translatable("etherhud.overlay.percent", value);
                case PIXELS -> Component.translatable("etherhud.overlay.pixels", value);
                case BLOCKS -> Component.translatable("etherhud.overlay.blocks", String.format(java.util.Locale.ROOT, "%+.2f", value / 100.0));
            };
        }
    }

    @FunctionalInterface private interface Reader { int read(OverlaySettings settings); }
    @FunctionalInterface private interface Writer { OverlaySettings write(OverlaySettings settings, int value); }

    private final EtherHud hud;
    private final Screen parent;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private int rowsPerPage;
    private int page;
    private int pageCount;
    private boolean rebuildPending;

    public OverlaySettingsScreen(Screen parent, EtherHud hud) {
        super(Component.translatable("etherhud.overlay.title"));
        this.parent = parent;
        this.hud = hud;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(340, width - 16);
        panelHeight = Math.min(300, height - 16);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        rowsPerPage = Math.clamp((panelHeight - 60) / ROW_HEIGHT, 1, CONTROLS.size());
        pageCount = Math.ceilDiv(CONTROLS.size(), rowsPerPage);
        page = Math.clamp(page, 0, pageCount - 1);
        int first = page * rowsPerPage;
        int last = Math.min(first + rowsPerPage, CONTROLS.size());
        int sliderWidth = Math.max(72, panelWidth / 2 - 20);
        int sliderX = left + panelWidth - sliderWidth - 12;
        for (int i = first; i < last; i++) {
            Control control = CONTROLS.get(i);
            var slider = new GlassSlider(sliderX, sliderWidth, control.label(), control.min(), control.max(), control.step(),
                    control.read(hud.settings()), control::display,
                    value -> hud.setSettings(control.write(hud.settings(), value)));
            slider.setY(top + 32 + (i - first) * ROW_HEIGHT);
            addRenderableWidget(slider);
        }
        int footerY = top + panelHeight - 24;
        addRenderableWidget(new GlassButton(left + 10, footerY, 54, 18, Component.literal("<"), () -> turnPage(-1))).active = page > 0;
        addRenderableWidget(new GlassButton(left + 68, footerY, 54, 18, Component.literal(">"), () -> turnPage(1))).active = page < pageCount - 1;
        addRenderableWidget(new GlassButton(left + panelWidth - 82, footerY, 72, 18,
                Component.translatable("etherhud.setting.done"), this::onClose));
    }

    private void turnPage(int direction) {
        page = Math.clamp(page + direction, 0, pageCount - 1);
        rebuildPending = true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        rebuildIfPending();
        return handled;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        boolean handled = super.keyPressed(key, scanCode, modifiers);
        rebuildIfPending();
        return handled;
    }

    private void rebuildIfPending() {
        if (rebuildPending) {
            rebuildPending = false;
            rebuildWidgets();
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderFrostedBackdrop(graphics, delta);
        drawFrostedPanel(graphics, left, top, panelWidth, panelHeight);
        graphics.drawString(font, Component.translatable("etherhud.overlay.title"), left + 10, top + 10, GlassPanel.TEXT, false);
        int first = page * rowsPerPage;
        int last = Math.min(first + rowsPerPage, CONTROLS.size());
        for (int i = first; i < last; i++) {
            graphics.drawString(font, CONTROLS.get(i).label(), left + 10, top + 37 + (i - first) * ROW_HEIGHT,
                    GlassPanel.TEXT, false);
        }
        if (pageCount > 1) {
            graphics.drawCenteredString(font, Component.translatable("etherhud.overlay.page", page + 1, pageCount),
                    left + panelWidth / 2, top + panelHeight - 18, GlassPanel.MUTED);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        hud.saveSettings();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
