package io.github.etherhud.ui;

import io.github.etherhud.EtherHud;
import io.github.etherhud.config.HudSettings;
import io.github.etherhud.layout.Anchor;
import io.github.etherhud.layout.LayoutResolver;
import io.github.etherhud.layout.ModulePlacement;
import io.github.etherhud.module.ModuleId;
import io.github.etherhud.render.GlassPanel;
import io.github.etherhud.render.HudRenderer;
import io.github.etherhud.ui.editor.DragController;
import io.github.etherhud.ui.editor.ModuleContextPanel;
import io.github.etherhud.ui.editor.PanelGroup;
import io.github.etherhud.ui.editor.ResizeController;
import io.github.etherhud.ui.editor.SnapEngine;
import io.github.etherhud.ui.editor.UndoStack;
import io.github.etherhud.ui.widget.GlassButton;
import io.github.etherhud.ui.widget.ToggleSwitch;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.EnumSet;
import java.util.List;

public final class HudEditorScreen extends Screen {
    private static final int TOOLBAR_TOP = 6;
    private static final int TOOLBAR_HEIGHT = 37;
    private static final int GRID_STEP = 16;

    private final Screen parent;
    private final EtherHud hud;
    private final HudRenderer preview = new HudRenderer(true);
    private final LayoutResolver resolver = new LayoutResolver();
    private final DragController drag = new DragController();
    private final SnapEngine snap = new SnapEngine();
    private final ResizeController resize = new ResizeController();
    private final UndoStack history = new UndoStack();
    private GlassButton snapButton;
    private GlassButton gridButton;
    private GlassButton disabledPreviewsButton;
    private List<GlassButton> toolbarButtons = List.of();
    private boolean toolbarVisible = true;
    private HudSettings working;
    private HudSettings dragBefore;
    private HudSettings resizeBefore;
    private ModuleId selected;
    private final EnumSet<ModuleId> selection = EnumSet.noneOf(ModuleId.class);
    private final LayoutResolver.Bounds selectionFrame = new LayoutResolver.Bounds();
    private boolean rangeSelecting;
    private boolean rangeAdditive;
    private double rangeStartX;
    private double rangeStartY;
    private double rangeCurrentX;
    private double rangeCurrentY;
    private PanelGroup gestureGroup;
    private Component heading = title;
    private ModuleContextPanel context;
    private int toolbarX;
    private int toolbarWidth;

    public HudEditorScreen(Screen parent, EtherHud hud) {
        super(Component.translatable("etherhud.editor.title"));
        this.parent = parent;
        this.hud = hud;
        this.working = hud.settings();
    }

    @Override
    protected void init() {
        if (dragBefore != null) {
            working = dragBefore;
            dragBefore = null;
        }
        drag.clear();
        if (resizeBefore != null) {
            working = resizeBefore;
            resizeBefore = null;
        }
        gestureGroup = null;
        context = null;

        toolbarWidth = Math.min(310, width - 12);
        toolbarX = width - toolbarWidth - 6;
        int buttonWidth = (toolbarWidth - 22) / 4;
        int x = toolbarX + 5;
        int y = TOOLBAR_TOP + 15;
        var cancel = addRenderableWidget(new GlassButton(x, y, buttonWidth, 18,
                Component.translatable("etherhud.ui.cancel"), () -> minecraft.setScreen(parent)));
        x += buttonWidth + 4;
        snapButton = addRenderableWidget(new GlassButton(x, y, buttonWidth, 18, Component.empty(), () -> {
            finishGesture();
            change(working.withSnap(!working.snapEnabled()));
        }));
        x += buttonWidth + 4;
        gridButton = addRenderableWidget(new GlassButton(x, y, buttonWidth, 18, Component.empty(), () -> {
            finishGesture();
            change(working.withGrid(!working.gridEnabled()));
        }));
        x += buttonWidth + 4;
        disabledPreviewsButton = addRenderableWidget(new GlassButton(x, y, buttonWidth, 18, Component.empty(), () -> {
            finishGesture();
            change(working.withDisabledPreviews(!working.showDisabledPreviews()));
        }));
        disabledPreviewsButton.setTooltip(Tooltip.create(Component.translatable("etherhud.editor.disabled_hint")));
        var hide = addRenderableWidget(new GlassButton(toolbarX + toolbarWidth - 18, TOOLBAR_TOP + 2, 13, 11,
                Component.literal("−"), this::toggleToolbar));
        hide.setTooltip(Tooltip.create(Component.translatable("etherhud.editor.toolbar_hint")));
        toolbarButtons = List.of(cancel, snapButton, gridButton, disabledPreviewsButton, hide);
        updateToolbarVisibility();
        updateToolbar();
        layout();
    }

    private void updateToolbar() {
        snapButton.setMessage(Component.translatable("etherhud.editor.snap", ToggleSwitch.stateLabel(working.snapEnabled())));
        gridButton.setMessage(Component.translatable("etherhud.editor.grid", ToggleSwitch.stateLabel(working.gridEnabled())));
        disabledPreviewsButton.setMessage(Component.translatable("etherhud.editor.disabled_previews",
                ToggleSwitch.stateLabel(working.showDisabledPreviews())));
    }

    private void toggleToolbar() {
        toolbarVisible = !toolbarVisible;
        clearFocus();
        updateToolbarVisibility();
    }

    private void updateToolbarVisibility() {
        for (GlassButton button : toolbarButtons) {
            button.visible = toolbarVisible;
        }
    }

    private void layout() {
        preview.layout(minecraft, working, hud.modules(), width, height, working.showDisabledPreviews());
        if (!working.showDisabledPreviews() && selection.removeIf(id -> !working.placement(id).enabled())) {
            if (context != null) {
                closeContext();
            }
            if (!selection.contains(selected)) {
                selected = selection.stream().findFirst().orElse(null);
            }
            refreshHeading();
        }
    }

    @Override
    public void onClose() {
        finishGesture();
        hud.setSettings(working);
        hud.saveSettings();
        minecraft.setScreen(parent);
    }

    private void change(HudSettings next) {
        if (!working.equals(next)) {
            history.record(working);
            apply(next);
        }
    }

    private void apply(HudSettings next) {
        working = next;
        layout();
        updateToolbar();
        if (context != null) {
            context.update(working.placement(selected));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, minecraft.level == null ? 0xFF151E29 : 0x25060C12);
        if (working.gridEnabled()) {
            drawGrid(graphics);
        }
        ModuleId hovered = hit(mouseX, mouseY);
        for (ModuleId id : ModuleId.ALL) {
            if (id != selected && !working.placement(id).enabled()) {
                drawPanel(graphics, id, hovered == id);
            }
        }
        for (ModuleId id : ModuleId.ALL) {
            if (id != selected && working.placement(id).enabled()) {
                drawPanel(graphics, id, hovered == id);
            }
        }
        if (selected != null) {
            drawPanel(graphics, selected, hovered == selected);
        }
        if (rangeSelecting) {
            int x1 = (int) Math.floor(Math.min(rangeStartX, rangeCurrentX));
            int y1 = (int) Math.floor(Math.min(rangeStartY, rangeCurrentY));
            int x2 = (int) Math.ceil(Math.max(rangeStartX, rangeCurrentX));
            int y2 = (int) Math.ceil(Math.max(rangeStartY, rangeCurrentY));
            graphics.fill(x1, y1, x2, y2, 0x2879DDE6);
            graphics.renderOutline(x1, y1, x2 - x1, y2 - y1, GlassPanel.ACCENT);
        }
        if (working.snapEnabled()) {
            if (resizeBefore != null) {
                drawGuides(graphics, resize.guideX(), resize.guideY());
            } else if (drag.moving()) {
                drawGuides(graphics, snap.guideX(), snap.guideY());
            }
        }
        if (selected != null && context == null) {
            var frame = selectionBounds();
            if (multipleSelected()) {
                graphics.renderOutline(frame.x(), frame.y(), frame.width(), frame.height(), GlassPanel.ACCENT);
            }
            if (!drag.moving() && canResizeSelection() && frame.width() > 0) {
                drawHandles(graphics, frame, mouseX, mouseY);
            }
        }
        if (toolbarVisible) {
            GlassPanel.draw(graphics, toolbarX, TOOLBAR_TOP, toolbarWidth, TOOLBAR_HEIGHT, 0xE0243340);
            graphics.drawCenteredString(font, heading, toolbarX + toolbarWidth / 2, TOOLBAR_TOP + 4, GlassPanel.TEXT);
        }
        if (context != null) {
            context.draw(graphics, font);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        for (var child : children()) {
            if (child instanceof Renderable renderable) {
                renderable.render(graphics, mouseX, mouseY, delta);
            }
        }
    }

    private void drawGrid(GuiGraphics graphics) {
        for (int x = GRID_STEP; x < width; x += GRID_STEP) {
            graphics.fill(x, 0, x + 1, height, 0x2794BAC9);
        }
        for (int y = GRID_STEP; y < height; y += GRID_STEP) {
            graphics.fill(0, y, width, y + 1, 0x2794BAC9);
        }
    }

    private void drawGuides(GuiGraphics graphics, int guideX, int guideY) {
        if (guideX != SnapEngine.NO_GUIDE) {
            int line = Math.min(width - 1, guideX);
            graphics.fill(line, 0, line + 1, height, 0xCB79DDE6);
        }
        if (guideY != SnapEngine.NO_GUIDE) {
            int line = Math.min(height - 1, guideY);
            graphics.fill(0, line, width, line + 1, 0xCB79DDE6);
        }
    }

    private void drawHandles(GuiGraphics graphics, LayoutResolver.Bounds bounds, int mouseX, int mouseY) {
        var hovered = ResizeController.Corner.hit(bounds, mouseX, mouseY);
        for (var corner : ResizeController.Corner.ALL) {
            int x = Math.clamp(corner.x(bounds) - 2, 0, Math.max(0, width - 5));
            int y = Math.clamp(corner.y(bounds) - 2, 0, Math.max(0, height - 5));
            graphics.fill(x, y, x + 5, y + 5, 0xFF13252F);
            graphics.fill(x + 1, y + 1, x + 4, y + 4, corner == hovered ? GlassPanel.TEXT : GlassPanel.ACCENT);
        }
    }

    private void drawPanel(GuiGraphics graphics, ModuleId id, boolean hovered) {
        var bounds = preview.bounds(id);
        if (bounds.width() == 0) {
            return;
        }
        boolean singleMoving = gestureGroup == null && drag.isMoving(id);
        boolean moving = singleMoving || gestureGroup != null && drag.moving() && selection.contains(id);
        int x = singleMoving ? drag.x() : bounds.x();
        int y = singleMoving ? drag.y() : bounds.y();
        boolean ghost = !working.placement(id).enabled() || !preview.hasContent(id);
        preview.draw(minecraft, graphics, id, x, y, ghost);
        if (ghost) {
            drawDashedOutline(graphics, x, y, bounds.width(), bounds.height(),
                    hovered || selection.contains(id) ? 0xD879DDE6 : 0x8994AAB8);
            return;
        }
        if (moving || hovered || selection.contains(id)) {
            graphics.renderOutline(x, y, bounds.width(), bounds.height(), moving ? GlassPanel.TEXT : GlassPanel.ACCENT);
        }
    }

    private void drawDashedOutline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        for (int offset = 0; offset < width; offset += 6) {
            int end = x + Math.min(offset + 3, width);
            graphics.fill(x + offset, y, end, y + 1, color);
            graphics.fill(x + offset, y + height - 1, end, y + height, color);
        }
        for (int offset = 1; offset < height - 1; offset += 6) {
            int end = y + Math.min(offset + 3, height - 1);
            graphics.fill(x, y + offset, x + 1, end, color);
            graphics.fill(x + width - 1, y + offset, x + width, end, color);
        }
    }

    private ModuleId hit(double x, double y) {
        if (insideToolbar(x, y) || context != null && context.contains(x, y)) {
            return null;
        }
        if (selected != null && preview.bounds(selected).contains(x, y)) {
            return selected;
        }
        ModuleId ghost = null;
        for (int i = ModuleId.ALL.length - 1; i >= 0; i--) {
            ModuleId id = ModuleId.ALL[i];
            if (preview.bounds(id).contains(x, y)) {
                if (working.placement(id).enabled()) {
                    return id;
                }
                if (ghost == null) {
                    ghost = id;
                }
            }
        }
        return ghost;
    }

    private boolean insideToolbar(double x, double y) {
        return toolbarVisible && x >= toolbarX && x < toolbarX + toolbarWidth
                && y >= TOOLBAR_TOP && y < TOOLBAR_TOP + TOOLBAR_HEIGHT;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (drag.module() != null || resizeBefore != null) {
            return false;
        }
        if (context != null) {
            if (context.contains(mouseX, mouseY)) {
                super.mouseClicked(event, doubled);
                return true;
            }
            closeContext();
        }
        if (super.mouseClicked(event, doubled)) {
            if (!toolbarVisible && getFocused() != null && toolbarButtons.contains(getFocused())) {
                clearFocus();
            }
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && selected != null && canResizeSelection()
                && !insideToolbar(mouseX, mouseY)) {
            var corner = ResizeController.Corner.hit(selectionBounds(), mouseX, mouseY);
            if (corner != null) {
                clearFocus();
                resizeBefore = working;
                startResize(corner, mouseX, mouseY);
                return true;
            }
        }
        ModuleId clicked = hit(mouseX, mouseY);
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && clicked != null) {
            if (event.hasControlDown()) {
                toggleSelection(clicked);
                clearFocus();
                return true;
            }
            select(clicked);
            openContext((int) mouseX, (int) mouseY);
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || insideToolbar(mouseX, mouseY)) {
            return false;
        }
        if (clicked == null) {
            rangeSelecting = true;
            rangeAdditive = event.hasControlDown();
            if (!rangeAdditive) {
                selection.clear();
                selected = null;
                refreshHeading();
            }
            rangeStartX = rangeCurrentX = mouseX;
            rangeStartY = rangeCurrentY = mouseY;
            return true;
        }
        if (clicked != null && selection.contains(clicked) && multipleSelected()) {
            selected = clicked;
        } else {
            select(clicked);
        }
        if (selected == null) {
            return false;
        }
        clearFocus();
        if (!multipleSelected() && !working.placement(selected).enabled()) {
            change(working.withPlacement(selected, working.placement(selected).withEnabled(true)));
            return true;
        }
        if (multipleSelected()) {
            gestureGroup = captureGroup();
        }
        drag.begin(selected, gestureGroup == null ? preview.bounds(selected) : gestureGroup.frame(), mouseX, mouseY);
        return true;
    }

    private void select(ModuleId id) {
        selection.clear();
        selected = id;
        if (id != null) {
            selection.add(id);
        }
        refreshHeading();
    }

    private void toggleSelection(ModuleId id) {
        if (selection.remove(id)) {
            if (!selection.contains(selected)) {
                selected = selection.stream().findFirst().orElse(null);
            }
        } else {
            selection.add(id);
            selected = id;
        }
        refreshHeading();
    }

    private void refreshHeading() {
        heading = multipleSelected() ? Component.translatable("etherhud.editor.group_selection", title, selection.size())
                : selected == null ? title : Component.translatable("etherhud.editor.selection", title, selected.label());
    }

    private boolean multipleSelected() { return selection.size() > 1; }

    private boolean canResizeSelection() {
        for (ModuleId id : ModuleId.ALL) {
            if (selection.contains(id) && !working.canResize(id)) {
                return false;
            }
        }
        return true;
    }

    private LayoutResolver.Bounds selectionBounds() {
        if (!multipleSelected()) {
            return preview.bounds(selected);
        }
        PanelGroup.updateFrame(selectionFrame, selection, preview, width, height);
        return selectionFrame;
    }

    private PanelGroup captureGroup() {
        return new PanelGroup(selection, preview, working, width, height);
    }

    private void openContext(int x, int y) {
        clearFocus();
        int toolbarBottom = toolbarVisible ? TOOLBAR_TOP + TOOLBAR_HEIGHT : 0;
        context = new ModuleContextPanel(x, y, width, height, toolbarBottom, selected, working.placement(selected),
                working.canResize(selected), this::contextAction);
        for (var widget : context.widgets()) {
            addRenderableWidget(widget);
        }
        setInitialFocus(context.widgets().getFirst());
    }

    private void closeContext() {
        clearFocus();
        for (var widget : context.widgets()) {
            removeWidget(widget);
        }
        context = null;
    }

    private void contextAction(ModuleContextPanel.Action action) {
        if (action == ModuleContextPanel.Action.SETTINGS) {
            openModuleSettings();
            return;
        }
        var placement = working.placement(selected);
        var bounds = preview.bounds(selected);
        var next = switch (action) {
            case TOGGLE -> placement.withEnabled(!placement.enabled());
            case NEXT_ANCHOR -> resolver.place(placement, bounds, width, height,
                    bounds.x(), bounds.y(), placement.anchor().next());
            case SCALE_DOWN -> placement.withScale(placement.scale() - 1f / ModulePlacement.SCALE_STEPS);
            case SCALE_UP -> placement.withScale(placement.scale() + 1f / ModulePlacement.SCALE_STEPS);
            case RESET_POSITION -> resolver.place(placement, bounds, width, height, Math.round(LayoutResolver.MARGIN * bounds.unitScale()),
                    preview.defaultStackY(selected, working.hudScale()), Anchor.TOP_LEFT);
            case RETURN_TO_STACK -> selected == ModuleId.ARMOR_STATUS ? placement.dock() : placement.returnToStack();
            case SETTINGS -> throw new IllegalStateException("Module settings handled before placement actions");
        };
        change(working.withPlacement(selected, next));
    }

    private void openModuleSettings() {
        HudSettings before = working;
        minecraft.setScreen(new ModuleSettingsScreen(this, hud, selected, () -> working,
                update -> working = update.apply(working), () -> {
            if (!before.equals(working)) {
                history.record(before);
            }
        }));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && rangeSelecting) {
            rangeCurrentX = Math.clamp(mouseX, 0, width);
            rangeCurrentY = Math.clamp(mouseY, 0, height);
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && resizeBefore != null) {
            moveResize(mouseX, mouseY);
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || drag.module() == null) {
            return super.mouseDragged(event, dragX, dragY);
        }
        moveDrag(mouseX, mouseY);
        return true;
    }

    private void moveDrag(double mouseX, double mouseY) {
        ModuleId id = drag.module();
        if (gestureGroup != null) {
            if (!drag.moving() && !drag.passedThreshold(mouseX, mouseY)) {
                return;
            }
            dragBefore = gestureGroup.original();
            drag.move(gestureGroup.frame(), width, height, mouseX, mouseY);
            snapDrag(gestureGroup.frame());
            HudSettings next = gestureGroup.move(drag.x(), drag.y());
            if (!next.equals(working)) {
                apply(next);
            }
            return;
        }
        if (!drag.moving()) {
            if (!drag.passedThreshold(mouseX, mouseY)) {
                return;
            }
            dragBefore = working;
            var bounds = preview.bounds(id);
            working = working.withPlacement(id, resolver.place(working.placement(id), bounds, width, height,
                    bounds.x(), bounds.y(), null));
            layout();
        }
        drag.move(preview.bounds(id), width, height, mouseX, mouseY);
        snapDrag(preview.bounds(id));
    }

    private void snapDrag(LayoutResolver.Bounds frame) {
        if (working.snapEnabled()) {
            snap.start(drag.x(), drag.y(), frame, width, height);
            for (ModuleId other : ModuleId.ALL) {
                if (!selection.contains(other) && working.placement(other).enabled() && preview.bounds(other).width() > 0) {
                    snap.consider(preview.bounds(other));
                }
            }
            drag.snapTo(snap.x(), snap.y());
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && rangeSelecting) {
            rangeCurrentX = Math.clamp(mouseX, 0, width);
            rangeCurrentY = Math.clamp(mouseY, 0, height);
            finishRangeSelection();
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && resizeBefore != null) {
            moveResize(mouseX, mouseY);
            finishResize();
            return true;
        }
        ModuleId id = drag.module();
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && id != null) {
            moveDrag(mouseX, mouseY);
            finishDrag();
            return true;
        }
        return super.mouseReleased(event);
    }

    private void finishRangeSelection() {
        double left = Math.min(rangeStartX, rangeCurrentX);
        double top = Math.min(rangeStartY, rangeCurrentY);
        double right = Math.max(rangeStartX, rangeCurrentX);
        double bottom = Math.max(rangeStartY, rangeCurrentY);
        for (ModuleId id : ModuleId.ALL) {
            var bounds = preview.bounds(id);
            if (bounds.width() > 0 && bounds.height() > 0 && bounds.x() < right && bounds.x() + bounds.width() > left
                    && bounds.y() < bottom && bounds.y() + bounds.height() > top) {
                selection.add(id);
                selected = id;
            }
        }
        rangeSelecting = false;
        refreshHeading();
    }

    private void startResize(ResizeController.Corner corner, double mouseX, double mouseY) {
        if (multipleSelected()) {
            gestureGroup = captureGroup();
            var frame = gestureGroup.frame();
            double anchorX = corner.x(frame) == frame.x() ? 1 : 0;
            double anchorY = corner.y(frame) == frame.y() ? 1 : 0;
            resize.start(frame, anchorX, anchorY, 1, frame.width(), frame.height(), width, height, corner, mouseX, mouseY);
            return;
        }
        var bounds = preview.bounds(selected);
        resize.start(bounds, bounds.alignX(), bounds.alignY(), working.hudScale() * bounds.unitScale(),
                preview.panelWidth(selected), preview.panelHeight(selected), width, height, corner, mouseX, mouseY);
    }

    private void moveResize(double mouseX, double mouseY) {
        HudSettings next = resizeBefore;
        if (resize.moved(mouseX, mouseY)) {
            resize.move(mouseX, mouseY, resizeBefore.snapEnabled());
            if (resizeBefore.snapEnabled()) {
                for (ModuleId id : ModuleId.ALL) {
                    if (!selection.contains(id) && resizeBefore.placement(id).enabled() && preview.bounds(id).width() > 0
                            && (gestureGroup != null || resizeBefore.placement(selected).customPosition()
                                || resizeBefore.placement(selected).docked() || resizeBefore.placement(id).docked()
                                || resizeBefore.placement(id).customPosition() || id.ordinal() < selected.ordinal())) {
                        resize.consider(preview.bounds(id));
                    }
                }
            }
            next = resized(resizeBefore);
        } else {
            resize.setScale(gestureGroup == null ? resizeBefore.placement(selected).scale() : 1);
        }
        if (!next.equals(working)) {
            apply(next);
        }
    }

    private HudSettings resized(HudSettings source) {
        if (gestureGroup != null) {
            return gestureGroup.resize(resize.scale(), resize.alignX(), resize.alignY());
        }
        var placement = source.placement(selected);
        if (placement.scale() == resize.scale()) {
            return source;
        }
        var changed = placement.withScale(resize.scale());
        if (placement.customPosition()) {
            var bounds = resize.bounds();
            changed = resolver.place(changed, bounds, width, height, bounds.x(), bounds.y(), placement.anchor());
        }
        return source.withPlacement(selected, changed);
    }

    private void finishResize() {
        if (resizeBefore != null) {
            if (!resizeBefore.equals(working)) {
                history.record(resizeBefore);
            }
            resizeBefore = null;
            gestureGroup = null;
        }
    }

    private void finishGesture() {
        finishDrag();
        finishResize();
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (!minecraft.hasControlDown() || vertical == 0 || context != null || drag.module() != null || resizeBefore != null) {
            return super.mouseScrolled(x, y, horizontal, vertical);
        }
        if (selected == null) {
            select(hit(x, y));
        }
        if (selected == null) {
            return false;
        }
        if (!canResizeSelection()) {
            return true;
        }
        if (multipleSelected()) {
            change(captureGroup().resize(vertical > 0 ? 1.05 : 0.95, 0, 0));
            return true;
        }
        var placement = working.placement(selected);
        startResize(ResizeController.Corner.BOTTOM_RIGHT, x, y);
        resize.setWheelScale(placement.scale() + (vertical > 0 ? 1f : -1f) / ModulePlacement.SCALE_STEPS);
        change(resized(working));
        return true;
    }

    private void finishDrag() {
        if (gestureGroup != null && drag.module() != null) {
            if (drag.moving() && !gestureGroup.original().equals(working)) {
                history.record(gestureGroup.original());
            }
            gestureGroup = null;
            dragBefore = null;
            drag.clear();
            return;
        }
        if (drag.moving()) {
            ModuleId id = drag.module();
            var bounds = preview.bounds(id);
            HudSettings previous = dragBefore;
            HudSettings next = previous;
            if (drag.x() != bounds.x() || drag.y() != bounds.y()) {
                next = previous.withPlacement(id, resolver.place(previous.placement(id), bounds,
                        width, height, drag.x(), drag.y(), null));
            }
            working = previous;
            dragBefore = null;
            if (next.equals(previous)) {
                layout();
            } else {
                change(next);
            }
        }
        drag.clear();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE && context != null) {
            closeContext();
            return true;
        }
        boolean control = event.hasControlDown();
        boolean shift = event.hasShiftDown();
        if (!control && key == GLFW.GLFW_KEY_H) {
            if (context != null) {
                closeContext();
            }
            toggleToolbar();
            return true;
        }
        if (control && key == GLFW.GLFW_KEY_TAB && drag.module() == null && resizeBefore == null) {
            if (context != null) {
                closeContext();
            }
            clearFocus();
            int index = selected == null ? (shift ? 0 : -1) : selected.ordinal();
            for (int step = 1; step <= ModuleId.ALL.length; step++) {
                ModuleId candidate = ModuleId.ALL[Math.floorMod(index + (shift ? -step : step), ModuleId.ALL.length)];
                if (preview.bounds(candidate).width() > 0) {
                    select(candidate);
                    break;
                }
            }
            return true;
        }
        if (control && (key == GLFW.GLFW_KEY_Z || key == GLFW.GLFW_KEY_Y)) {
            finishGesture();
            HudSettings next = key == GLFW.GLFW_KEY_Z ? history.undo(working) : history.redo(working);
            if (next != working) {
                apply(next);
            }
            return true;
        }
        if (selected != null && drag.module() == null && resizeBefore == null && getFocused() == null) {
            int distance = shift ? 10 : 1;
            switch (key) {
                case GLFW.GLFW_KEY_LEFT -> { return nudge(-distance, 0); }
                case GLFW.GLFW_KEY_RIGHT -> { return nudge(distance, 0); }
                case GLFW.GLFW_KEY_UP -> { return nudge(0, -distance); }
                case GLFW.GLFW_KEY_DOWN -> { return nudge(0, distance); }
            }
        }
        return super.keyPressed(event);
    }

    private boolean nudge(int dx, int dy) {
        if (preview.bounds(selected).width() == 0) {
            return false;
        }
        if (multipleSelected()) {
            var group = captureGroup();
            var frame = group.frame();
            float units = preview.bounds(selected).unitScale();
            change(group.move(frame.x() + screenStep(dx, units), frame.y() + screenStep(dy, units)));
            return true;
        }
        var bounds = preview.bounds(selected);
        int x = LayoutResolver.clampX(bounds, width, bounds.x() + screenStep(dx, bounds.unitScale()));
        int y = LayoutResolver.clampY(bounds, height, bounds.y() + screenStep(dy, bounds.unitScale()));
        if (x != bounds.x() || y != bounds.y()) {
            var placement = working.placement(selected);
            change(working.withPlacement(selected, resolver.place(placement, bounds, width, height,
                    x, y, placement.customPosition() ? placement.anchor() : null)));
        }
        return true;
    }

    private int screenStep(int units, float scale) {
        return units == 0 ? 0 : Integer.signum(units) * Math.max(1, Math.round(Math.abs(units) * scale));
    }
}
