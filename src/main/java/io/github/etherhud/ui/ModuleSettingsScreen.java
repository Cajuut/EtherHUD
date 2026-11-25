package io.github.etherhud.ui;

import io.github.etherhud.EtherHud;
import io.github.etherhud.config.HudSettings;
import io.github.etherhud.layout.LayoutResolver;
import io.github.etherhud.module.ModuleId;
import io.github.etherhud.module.settings.ModuleSetting;
import io.github.etherhud.render.GlassPanel;
import io.github.etherhud.render.GlassRenderer;
import io.github.etherhud.render.HudRenderer;
import io.github.etherhud.ui.widget.GlassButton;
import io.github.etherhud.ui.widget.GlassSlider;
import io.github.etherhud.ui.widget.ToggleSwitch;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class ModuleSettingsScreen extends GlassSettingsScreen {
    private static final int ROW_HEIGHT = 23;
    private final Screen parent;
    private final EtherHud hud;
    private final ModuleId id;
    private final Supplier<HudSettings> settings;
    private final Consumer<UnaryOperator<HudSettings>> change;
    private final Runnable exit;
    private final HudRenderer preview = new HudRenderer(true);
    private final List<Row> rows = new ArrayList<>();
    private int left;
    private int panelWidth;
    private int listTop;
    private int listBottom;
    private int scroll;
    private boolean resetWidgets;

    private record Row(ModuleSetting setting, Component label, AbstractWidget widget) {}

    public ModuleSettingsScreen(Screen parent, EtherHud hud, ModuleId id) {
        this(parent, hud, id, hud::settings, edit -> hud.setSettings(edit.apply(hud.settings())), () -> {});
    }

    public ModuleSettingsScreen(Screen parent, EtherHud hud, ModuleId id, Supplier<HudSettings> settings,
                                Consumer<UnaryOperator<HudSettings>> change, Runnable exit) {
        super(Component.translatable("etherhud.setting.title", id.label()));
        this.parent = parent;
        this.hud = hud;
        this.id = id;
        this.settings = settings;
        this.change = change;
        this.exit = exit;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(420, width - 16);
        left = (width - panelWidth) / 2;
        listTop = 86;
        listBottom = Math.max(listTop + 1, height - 38);
        rows.clear();
        for (ModuleSetting setting : hud.modules().get(id).settings(id)) {
            Component label = setting.label();
            AbstractWidget widget = createWidget(setting, label);
            if (widget != null) {
                addWidget(widget);
            }
            rows.add(new Row(setting, label, widget));
        }
        addRenderableWidget(new GlassButton(left + 8, height - 31, panelWidth - 92, 20,
                Component.translatable("etherhud.setting.reset"), () -> {
                    update(s -> s.withModuleDefaults(id));
                    resetWidgets = true;
                }));
        addRenderableWidget(new GlassButton(left + panelWidth - 78, height - 31, 70, 20,
                Component.translatable("etherhud.setting.done"), this::onClose));
        syncLockedControls();
        repositionRows();
        refreshPreview();
    }

    private AbstractWidget createWidget(ModuleSetting setting, Component label) {
        int controlWidth = panelWidth / 2 - 8;
        int right = left + panelWidth - 15;
        int x = right - controlWidth;
        return switch (setting) {
            case ModuleSetting.Section ignored -> null;
            case ModuleSetting.Toggle toggle -> createToggle(right, label, toggle);
            case ModuleSetting.Slider slider -> new GlassSlider(x, controlWidth, label, slider.min(), slider.max(),
                    slider.step(), slider.read().applyAsInt(settings.get()), slider.display(), v -> update(s -> slider.write().apply(s, v)));
            case ModuleSetting.Choice<?> choice -> createChoice(x, controlWidth, label, choice);
            case ModuleSetting.Color color -> createColor(x, controlWidth, label, color);
            case ModuleSetting.ImageFile file -> createImageFile(x, controlWidth, file);
        };
    }

    private ToggleSwitch createToggle(int right, Component label, ModuleSetting.Toggle toggle) {
        return new ToggleSwitch(right - 34, 0, label, toggle.read().test(settings.get()), () -> {
            boolean next = !toggle.read().test(settings.get());
            update(s -> toggle.write().apply(s, next));
        });
    }

    private <T> GlassButton createChoice(int x, int width, Component label, ModuleSetting.Choice<T> choice) {
        var button = new GlassButton(x, 0, width, 18, Component.empty(), () -> {
            int index = choice.options().indexOf(choice.read().apply(settings.get()));
            T next = choice.options().get((index + 1) % choice.options().size());
            update(s -> choice.write().apply(s, next));
        });
        button.setMessage(choice.display().apply(choice.read().apply(settings.get())));
        button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(label));
        return button;
    }

    private GlassButton createColor(int x, int width, Component label, ModuleSetting.Color color) {
        var button = new GlassButton(x, 0, width, 18, Component.empty(), () -> minecraft.setScreen(
                new ColorPickerScreen(this, label, color, settings, change)));
        button.setMessage(colorLabel(color.read().applyAsInt(settings.get())));
        button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(label));
        return button;
    }

    private Component colorLabel(int rgb) {
        return Component.literal(String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF));
    }

    private GlassButton createImageFile(int x, int width, ModuleSetting.ImageFile file) {
        var button = new GlassButton(x, 0, width, 18, Component.empty(), () -> openImagePicker(file));
        button.setTooltip(Tooltip.create(Component.translatable("etherhud.image.choose")));
        button.setMessage(imageFileLabel(file.read().apply(settings.get())));
        return button;
    }

    private void openImagePicker(ModuleSetting.ImageFile file) {
        String title = Component.translatable("etherhud.image.choose").getString();
        String selected;
        try (var stack = MemoryStack.stackPush()) {
            var formats = stack.pointers(stack.UTF8("*.png"), stack.UTF8("*.jpg"), stack.UTF8("*.jpeg"),
                    stack.UTF8("*.gif"), stack.UTF8("*.bmp"), stack.UTF8("*.wbmp"), stack.UTF8("*.tif"), stack.UTF8("*.tiff"));
            selected = TinyFileDialogs.tinyfd_openFileDialog(title, file.read().apply(settings.get()), formats,
                    Component.translatable("etherhud.image.files").getString(), false);
        }
        if (selected != null && !selected.isBlank()) {
            update(s -> file.write().apply(s, selected));
        }
    }

    private Component imageFileLabel(String file) {
        if (file.isBlank()) {
            return Component.translatable("etherhud.image.choose");
        }
        try {
            Path name = Path.of(file).getFileName();
            return Component.literal(name == null ? file : name.toString());
        } catch (InvalidPathException ignored) {
            return Component.translatable("etherhud.image.unavailable");
        }
    }

    private void update(UnaryOperator<HudSettings> edit) {
        HudSettings before = settings.get();
        change.accept(edit);
        HudSettings current = settings.get();
        if (!before.equals(current)) {
            syncLockedControls();
            for (Row row : rows) {
                switch (row.setting()) {
                    case ModuleSetting.Toggle toggle -> ((ToggleSwitch) row.widget()).setEnabled(toggle.read().test(current));
                    case ModuleSetting.Slider slider -> ((GlassSlider) row.widget()).setCurrent(slider.read().applyAsInt(current));
                    case ModuleSetting.Choice<?> choice -> ((GlassButton) row.widget()).setMessage(choiceLabel(choice, current));
                    case ModuleSetting.Color color -> ((GlassButton) row.widget()).setMessage(colorLabel(color.read().applyAsInt(current)));
                    case ModuleSetting.ImageFile file -> ((GlassButton) row.widget()).setMessage(imageFileLabel(file.read().apply(current)));
                    default -> {}
                }
            }
            refreshPreview();
        }
    }

    private void syncLockedControls() {
        boolean resizable = settings.get().canResize(id);
        for (Row row : rows) {
            if (row.widget() != null && row.setting().key().equals("scale")) {
                row.widget().active = resizable;
            }
        }
    }

    private <T> Component choiceLabel(ModuleSetting.Choice<T> choice, HudSettings settings) {
        return choice.display().apply(choice.read().apply(settings));
    }

    private void refreshPreview() {
        preview.layout(minecraft, settings.get(), hud.modules(), width, height);
    }

    private int maxScroll() {
        int visibleRows = Math.max(1, (listBottom - listTop) / ROW_HEIGHT);
        return Math.max(0, rows.size() - visibleRows);
    }

    private void repositionRows() {
        scroll = Math.clamp(scroll, 0, maxScroll());
        for (int i = 0; i < rows.size(); i++) {
            AbstractWidget widget = rows.get(i).widget();
            if (widget != null) {
                int y = listTop + (i - scroll) * ROW_HEIGHT + 2;
                widget.setY(y);
                widget.visible = y - 2 >= listTop && y - 2 + ROW_HEIGHT <= listBottom;
                if (!widget.visible && getFocused() == widget) {
                    clearFocus();
                }
            }
        }
    }

    private boolean scrollBy(int rows) {
        int old = scroll;
        scroll += rows;
        repositionRows();
        return old != scroll;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        return x >= left && x < left + panelWidth && y >= listTop && y < listBottom
                && vertical != 0 && scrollBy(vertical > 0 ? -1 : 1);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_PAGE_UP || event.key() == GLFW.GLFW_KEY_PAGE_DOWN) {
            return scrollBy(event.key() == GLFW.GLFW_KEY_PAGE_UP ? -3 : 3);
        }
        return super.keyPressed(event);
    }

    @Override
    public void tick() {
        if (resetWidgets) {
            resetWidgets = false;
            rebuildWidgets();
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderFrostedBackdrop(graphics, delta);
        drawFrostedPanel(graphics, left, 6, panelWidth, height - 12);
        GlassPanel.rounded(graphics, left + 8, 27, panelWidth - 16, 54, 0x60405260);
        graphics.drawCenteredString(font, title, width / 2, 14, GlassPanel.TEXT);
        var bounds = preview.bounds(id);
        var value = settings.get();
        boolean enabled = value.placement(id).enabled();
        drawPlacementMap(graphics, bounds, enabled);
        int sampleLeft = left + 94;
        int sampleWidth = Math.max(1, panelWidth - 105);
        int baseWidth = preview.panelWidth(id);
        int baseHeight = preview.panelHeight(id);
        float scale = Math.min(value.canResize(id) ? value.hudScale() * value.placement(id).scale() : 1,
                Math.min(sampleWidth / (float) baseWidth, 44f / baseHeight));
        preview.drawScaled(minecraft, graphics, id, sampleLeft + (sampleWidth - Math.round(baseWidth * scale)) / 2,
                27 + (54 - Math.round(baseHeight * scale)) / 2, scale, !enabled);
        drawRowBackgrounds(graphics, mouseX, mouseY);
    }

    private void drawRowBackgrounds(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.enableScissor(left + 7, listTop, left + panelWidth - 7, listBottom);
        for (int i = scroll; i < rows.size(); i++) {
            int y = listTop + (i - scroll) * ROW_HEIGHT;
            if (y + ROW_HEIGHT > listBottom) {
                break;
            }
            Row row = rows.get(i);
            if (row.setting() instanceof ModuleSetting.Section) {
                graphics.fill(left + 12, y + ROW_HEIGHT - 2, left + panelWidth - 18, y + ROW_HEIGHT - 1,
                        0x3F7893A1);
                GlassRenderer.rounded(graphics, left + 9, y + 7, 2, 10, 1, GlassPanel.ACCENT);
            } else if (row.widget() != null && row.widget().visible
                    && row.widget().isMouseOver(mouseX, mouseY)) {
                GlassRenderer.rounded(graphics, left + 8, y + 1, panelWidth - 20, ROW_HEIGHT - 2,
                        4, 4, 4, 4, 1, 0x49395461, 0x4F79DDE6);
            }
        }
        graphics.disableScissor();
    }

    private void drawPlacementMap(GuiGraphics graphics, LayoutResolver.Bounds bounds, boolean enabled) {
        int mapX = left + 13;
        int mapY = 33;
        int mapWidth = 72;
        int mapHeight = 42;
        GlassRenderer.rounded(graphics, mapX, mapY, mapWidth, mapHeight, 3, 3, 3, 3, 1,
                0xA612222E, 0x656C8897);
        if (preview.hasContent(id) && bounds.width() > 0) {
            int screenWidth = Math.max(1, width);
            int screenHeight = Math.max(1, height);
            int x = Math.min(mapWidth - 2, bounds.x() * mapWidth / screenWidth);
            int y = Math.min(mapHeight - 2, bounds.y() * mapHeight / screenHeight);
            int w = Math.clamp(bounds.width() * mapWidth / screenWidth, 2, mapWidth - x);
            int h = Math.clamp(bounds.height() * mapHeight / screenHeight, 2, mapHeight - y);
            GlassRenderer.rounded(graphics, mapX + x, mapY + y, w, h, 1, enabled ? GlassPanel.ACCENT : GlassPanel.MUTED);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        graphics.enableScissor(left + 7, listTop, left + panelWidth - 7, listBottom);
        for (int i = scroll; i < rows.size(); i++) {
            int y = listTop + (i - scroll) * ROW_HEIGHT;
            if (y + ROW_HEIGHT > listBottom) {
                break;
            }
            Row row = rows.get(i);
            boolean section = row.setting() instanceof ModuleSetting.Section;
            AbstractWidget widget = row.widget();
            graphics.drawString(font, row.label(), left + 12, y + 7, section ? GlassPanel.ACCENT : GlassPanel.TEXT, false);
            if (row.setting() instanceof ModuleSetting.Color color) {
                GlassRenderer.circle(graphics, widget.getX() - 15, y + 6, 10, 1,
                        0xFF000000 | color.read().applyAsInt(settings.get()), GlassPanel.MUTED);
            }
            if (widget != null) {
                widget.render(graphics, mouseX, mouseY, delta);
            }
        }
        graphics.disableScissor();
        int max = maxScroll();
        if (max > 0) {
            int trackHeight = listBottom - listTop;
            int thumbHeight = Math.max(6, trackHeight * (rows.size() - max) / rows.size());
            int y = listTop + scroll * (trackHeight - thumbHeight) / max;
            GlassRenderer.rounded(graphics, left + panelWidth - 6, y, 2, thumbHeight, 1, GlassPanel.ACCENT);
        }
    }

    @Override
    public void onClose() {
        exit.run();
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
