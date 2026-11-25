package io.github.etherhud.ui;

import io.github.etherhud.config.HudSettings;
import io.github.etherhud.module.settings.ModuleSetting;
import io.github.etherhud.render.GlassPanel;
import io.github.etherhud.render.GlassRenderer;
import io.github.etherhud.ui.widget.GlassButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/** Glass styled hue/saturation/value picker for schema-driven color settings. */
public final class ColorPickerScreen extends GlassSettingsScreen {
    private final Screen parent;
    private final ModuleSetting.Color setting;
    private final Supplier<HudSettings> settings;
    private final Consumer<UnaryOperator<HudSettings>> change;
    private float hue;
    private float saturation;
    private float brightness;
    private int panelWidth;
    private int left;
    private int top;
    private int planeX;
    private int planeY;
    private int planeWidth;
    private int planeHeight;
    private int hueY;
    private int hueHeight;
    private EditBox hexBox;
    private boolean updatingHex;
    private boolean draggingPlane;
    private boolean draggingHue;

    public ColorPickerScreen(Screen parent, Component label, ModuleSetting.Color setting,
                             Supplier<HudSettings> settings, Consumer<UnaryOperator<HudSettings>> change) {
        super(Component.translatable("etherhud.color.title", label));
        this.parent = parent;
        this.setting = setting;
        this.settings = settings;
        this.change = change;
        setColor(setting.read().applyAsInt(settings.get()));
    }

    @Override
    protected void init() {
        panelWidth = Math.min(390, width - 20);
        left = (width - panelWidth) / 2;
        top = Math.max(8, (height - 306) / 2);
        planeX = left + 14;
        planeY = top + 72;
        planeWidth = panelWidth - 28;
        planeHeight = Math.min(174, height - top - 119);
        hueY = planeY + planeHeight + 9;
        hueHeight = 14;

        int rgb = selectedRgb();
        hexBox = new EditBox(font, left + 75, top + 35, panelWidth - 90, 22,
                Component.translatable("etherhud.color.hex"));
        hexBox.setMaxLength(7);
        hexBox.setValue(hex(rgb));
        hexBox.setResponder(this::hexChanged);
        addRenderableWidget(hexBox);
        addRenderableWidget(new GlassButton(left + 12, top + 273, panelWidth - 24, 21,
                Component.translatable("etherhud.setting.done"), this::onClose));
    }

    private void hexChanged(String value) {
        if (updatingHex) {
            return;
        }
        String digits = value.startsWith("#") ? value.substring(1) : value;
        if (digits.length() == 6) {
            try {
                setColor(Integer.parseInt(digits, 16));
                applyColor();
            } catch (NumberFormatException ignored) {
                // Retain the last valid color while the hex field is being edited.
            }
        }
    }

    private static String hex(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    private int selectedRgb() {
        return Color.HSBtoRGB(hue, saturation, brightness) & 0xFFFFFF;
    }

    private void setColor(int rgb) {
        float[] hsv = Color.RGBtoHSB(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, null);
        hue = hsv[0];
        saturation = hsv[1];
        brightness = hsv[2];
    }

    private void updateHex() {
        if (hexBox != null) {
            updatingHex = true;
            hexBox.setValue(hex(selectedRgb()));
            updatingHex = false;
        }
    }

    private void applyColor() {
        int rgb = selectedRgb();
        change.accept(current -> setting.write().apply(current, rgb));
        updateHex();
    }

    private void setPlane(double x, double y) {
        saturation = (float) Math.clamp((x - planeX) / planeWidth, 0.0, 1.0);
        brightness = 1.0f - (float) Math.clamp((y - planeY) / planeHeight, 0.0, 1.0);
        applyColor();
    }

    private void setHue(double x) {
        hue = (float) Math.clamp((x - planeX) / planeWidth, 0.0, 1.0);
        applyColor();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderFrostedBackdrop(graphics, delta);
        drawFrostedPanel(graphics, left, top, panelWidth, 302);
        graphics.drawCenteredString(font, title, width / 2, top + 11, GlassPanel.TEXT);
        int rgb = selectedRgb();
        GlassRenderer.rounded(graphics, left + 14, top + 34, 48, 24, 4, 4, 4, 4, 1,
                0xFF000000 | rgb, 0xAAB6DCE8);
        graphics.drawString(font, Component.translatable("etherhud.color.hex"), left + 75, top + 24,
                GlassPanel.MUTED, false);

        graphics.fill(planeX, planeY, planeX + planeWidth, planeY + planeHeight, 0xFF000000 | Color.HSBtoRGB(hue, 1, 1));
        horizontalGradient(graphics, planeX, planeY, planeWidth, planeHeight, 0xFFFFFFFF, 0x00FFFFFF);
        graphics.fillGradient(planeX, planeY, planeX + planeWidth, planeY + planeHeight,
                0x00000000, 0xFF000000);
        graphics.renderOutline(planeX, planeY, planeWidth, planeHeight, 0xAAB6DCE8);

        int markerX = planeX + Math.round(saturation * (planeWidth - 1));
        int markerY = planeY + Math.round((1 - brightness) * (planeHeight - 1));
        GlassRenderer.circle(graphics, markerX, markerY, 7, 2, 0x00000000, 0xFFFFFFFF);
        drawHueBar(graphics);
        int hueX = planeX + Math.round(hue * (planeWidth - 1));
        GlassRenderer.rounded(graphics, hueX - 5, hueY - 2, 10, hueHeight + 4, 6, 6, 6, 6, 2,
                0xFF075AFF, 0xFFFFFFFF);
    }

    private void horizontalGradient(GuiGraphics graphics, int x, int y, int width, int height,
                                   int leftColor, int rightColor) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + width, y);
        graphics.pose().rotate((float) Math.toRadians(90));
        graphics.fillGradient(0, 0, height, width, rightColor, leftColor);
        graphics.pose().popMatrix();
    }

    private void drawHueBar(GuiGraphics graphics) {
        for (int segment = 0; segment < 6; segment++) {
            int startX = planeX + segment * planeWidth / 6;
            int endX = planeX + (segment + 1) * planeWidth / 6;
            int start = Color.HSBtoRGB(segment / 6.0f, 1, 1) | 0xFF000000;
            int end = Color.HSBtoRGB((segment + 1) / 6.0f, 1, 1) | 0xFF000000;
            horizontalGradient(graphics, startX, hueY, endX - startX + 1, hueHeight, start, end);
        }
        graphics.renderOutline(planeX, hueY, planeWidth, hueHeight, 0xAAB6DCE8);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (super.mouseClicked(event, doubled)) {
            return true;
        }
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (insidePlane(event.x(), event.y())) {
            draggingPlane = true;
            setPlane(event.x(), event.y());
            return true;
        }
        if (event.x() >= planeX && event.x() <= planeX + planeWidth
                && event.y() >= hueY - 4 && event.y() <= hueY + hueHeight + 4) {
            draggingHue = true;
            setHue(event.x());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingPlane) {
            setPlane(event.x(), event.y());
            return true;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingHue) {
            setHue(event.x());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && (draggingHue || draggingPlane)) {
            draggingHue = false;
            draggingPlane = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    private boolean insidePlane(double x, double y) {
        return x >= planeX && x <= planeX + planeWidth && y >= planeY && y <= planeY + planeHeight;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
