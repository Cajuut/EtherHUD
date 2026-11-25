package io.github.etherhud.ui;

import io.github.etherhud.EtherHud;
import io.github.etherhud.config.HudSettings;
import io.github.etherhud.integration.ClothSettings;
import io.github.etherhud.module.ModuleId;
import io.github.etherhud.render.GlassPanel;
import io.github.etherhud.ui.widget.GlassButton;
import io.github.etherhud.ui.widget.RectButton;
import io.github.etherhud.ui.widget.ToggleSwitch;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public final class ModuleToggleScreen extends GlassSettingsScreen {
    private static final int INSET = 10;
    private static final int HEADER_HEIGHT = 32;
    private static final int ROW_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 5;
    private static final int SWITCH_WIDTH = 34;
    private static final int DETAILS_WIDTH = 46;

    private enum Page { MODULES, OPTIONS, RESET }
    private enum Tip {
        MODULES("modules"), MASTER("master"), STYLE("style"), EDITOR("editor"), MORE("more"), OVERLAY("overlay");

        private final String key;
        Tip(String key) { this.key = key; }
    }

    private static final Tip[] TIPS = Tip.values();
    private static final int TIP_WIDTH = 168;
    private static final int TIP_INSET = 6;
    private static final int TIP_BUTTON_HEIGHT = 16;

    private final Screen parent;
    private final EtherHud hud;
    private final ToggleSwitch[] switches = new ToggleSwitch[ModuleId.ALL.length];
    private final FormattedCharSequence[] labels = new FormattedCharSequence[ModuleId.ALL.length];
    private final Component heading = Component.translatable("etherhud.toggle.title").withStyle(ChatFormatting.BOLD);
    private final Component masterLabel = Component.translatable("etherhud.toggle.master");
    private final Component shaderLabel = Component.translatable("etherhud.toggle.shader");
    private final Component moreLabel = Component.translatable("etherhud.toggle.more");
    private final Component optionsTitle = Component.translatable("etherhud.toggle.options").withStyle(ChatFormatting.BOLD);
    private final Component confirmation = Component.translatable("etherhud.toggle.confirm");
    private final Component detailsLabel = Component.translatable("etherhud.toggle.module_settings");
    private final Component backLabel = Component.translatable("etherhud.toggle.back");
    private FormattedCharSequence fittedHeading;
    private List<FormattedCharSequence> confirmationLines;
    private ToggleSwitch master;
    private ToggleSwitch shader;
    private Page page = Page.MODULES;
    private boolean rebuildPending;
    private boolean focusMore;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int masterLabelX;
    private int modulePage;
    private int rowsPerPage;
    private int pageCount;
    private Component pageLabel;
    private Tip tip;
    private AbstractWidget tipTarget;
    private List<FormattedCharSequence> tipLines;
    private Component tipProgress;
    private int tipWidth;
    private int tipHeight;
    private int tipX;
    private int tipY;

    public ModuleToggleScreen(Screen parent, EtherHud hud) {
        super(Component.translatable("etherhud.toggle.title"));
        this.parent = parent;
        this.hud = hud;
        this.tip = hud.tipsCompleted() ? null : Tip.MODULES;
    }

    @Override
    protected void init() {
        boolean clothInstalled = FabricLoader.getInstance().isModLoaded("cloth-config");
        panelWidth = Math.min(244, width - 16);
        boolean showTip = tip != null && page != Page.RESET;
        boolean beside = showTip && width >= panelWidth + TIP_WIDTH + 24;
        tipTarget = null;
        if (showTip) {
            tipWidth = Math.min(TIP_WIDTH, width - 16);
            tipLines = font.split(Component.translatable("etherhud.tips." + tip.key + ".body"),
                    tipWidth - 2 * TIP_INSET);
            tipHeight = 2 * TIP_INSET + (tipLines.size() + 1) * font.lineHeight + TIP_BUTTON_HEIGHT + 8;
        }
        int reservedHeight = showTip && !beside ? tipHeight + 8 : 0;
        int verticalSpace = height - reservedHeight - 16 - HEADER_HEIGHT - GAP - BUTTON_HEIGHT - INSET;
        rowsPerPage = Math.clamp(verticalSpace / ROW_HEIGHT, 1, ModuleId.ALL.length);
        pageCount = Math.ceilDiv(ModuleId.ALL.length, rowsPerPage);
        modulePage = Math.min(modulePage, pageCount - 1);
        panelHeight = switch (page) {
            case MODULES -> HEADER_HEIGHT + rowsPerPage * ROW_HEIGHT + GAP + BUTTON_HEIGHT + INSET;
            case OPTIONS -> clothInstalled ? 148 : 124;
            case RESET -> 66;
        };
        panelX = (width - panelWidth - (beside ? tipWidth + 8 : 0)) / 2;
        panelY = Math.max(4, (height - panelHeight - reservedHeight) / 2);
        switch (page) {
            case MODULES -> initModules();
            case OPTIONS -> initOptions(clothInstalled);
            case RESET -> initReset();
        }
        if (showTip && tipTarget != null) {
            initTip(beside);
        }
    }

    private void initModules() {
        int right = contentRight();
        masterLabelX = right - SWITCH_WIDTH - GAP - font.width(masterLabel);
        fittedHeading = fit(heading, masterLabelX - contentLeft() - GAP);
        master = addRenderableWidget(new ToggleSwitch(right - SWITCH_WIDTH, panelY + 6, masterLabel,
                hud.settings().masterEnabled(), () -> {
                    boolean enabled = !hud.settings().masterEnabled();
                    hud.setSettings(hud.settings().withMaster(enabled));
                    master.setEnabled(enabled);
                }));
        // The master switch pauses the HUD without changing any per-module switch.
        master.setTooltip(Tooltip.create(Component.translatable("etherhud.toggle.master_hint")));
        if (tip == Tip.MASTER) tipTarget = master;
        int detailsX = right - DETAILS_WIDTH;
        int switchX = detailsX - GAP - SWITCH_WIDTH;
        int labelWidth = switchX - contentLeft() - GAP;
        for (int index = firstModule(); index < lastModule(); index++) {
            ModuleId id = ModuleId.ALL[index];
            Component label = id.label();
            labels[index] = fit(label, labelWidth);
            int y = rowY(id) + 1;
            switches[id.ordinal()] = addRenderableWidget(new ToggleSwitch(switchX, y, label,
                    hud.settings().placement(id).enabled(), () -> {
                        var placement = hud.settings().placement(id);
                        var next = placement.withEnabled(!placement.enabled());
                        hud.setSettings(hud.settings().withPlacement(id, next));
                        switches[id.ordinal()].setEnabled(next.enabled());
                    }));
            switches[index].setTooltip(Tooltip.create(id.description()));
            var details = addRenderableWidget(new GlassButton(detailsX, y, DETAILS_WIDTH, 18, detailsLabel,
                    () -> minecraft.setScreen(new ModuleSettingsScreen(this, hud, id))));
            details.setTooltip(Tooltip.create(Component.translatable("etherhud.setting.title", label)));
            if (index == firstModule()) {
                if (tip == Tip.MODULES) tipTarget = switches[index];
                if (tip == Tip.STYLE) tipTarget = details;
            }
        }
        int moreWidth = 54;
        int editorWidth = 72;
        var editor = addRenderableWidget(new GlassButton(contentLeft(), footerY(), editorWidth, BUTTON_HEIGHT,
                Component.translatable("etherhud.toggle.open_editor"),
                () -> minecraft.setScreen(new HudEditorScreen(this, hud))));
        if (tip == Tip.EDITOR) tipTarget = editor;
        var more = addRenderableWidget(new GlassButton(right - moreWidth, footerY(), moreWidth, BUTTON_HEIGHT,
                moreLabel, () -> show(Page.OPTIONS)));
        if (tip == Tip.MORE) tipTarget = more;
        if (pageCount > 1) {
            int navigationX = navigationX();
            pageLabel = Component.translatable("etherhud.toggle.page", modulePage + 1, pageCount);
            var previous = addRenderableWidget(new GlassButton(navigationX - 38, footerY(), 17, BUTTON_HEIGHT,
                    Component.literal("<"), () -> turnPage(-1)));
            previous.active = modulePage > 0;
            previous.setTooltip(Tooltip.create(Component.translatable("etherhud.toggle.previous")));
            var next = addRenderableWidget(new GlassButton(navigationX + 21, footerY(), 17, BUTTON_HEIGHT,
                    Component.literal(">"), () -> turnPage(1)));
            next.active = modulePage < pageCount - 1;
            next.setTooltip(Tooltip.create(Component.translatable("etherhud.toggle.next")));
        }
        if (focusMore) {
            focusMore = false;
            setInitialFocus(more);
        }
    }

    private void initOptions(boolean clothInstalled) {
        int left = contentLeft();
        int halfWidth = (contentRight() - left - GAP) / 2;
        addRenderableWidget(new GlassButton(left, panelY + 28, halfWidth, BUTTON_HEIGHT,
                Component.translatable("etherhud.toggle.all_on"), () -> setAll(true)));
        addRenderableWidget(new GlassButton(contentRight() - halfWidth, panelY + 28, halfWidth, BUTTON_HEIGHT,
                Component.translatable("etherhud.toggle.all_off"), () -> setAll(false)));
        shader = addRenderableWidget(new ToggleSwitch(contentRight() - SWITCH_WIDTH, panelY + 50,
                shaderLabel, hud.settings().useShaderRendering(), () -> {
                    boolean enabled = !hud.settings().useShaderRendering();
                    hud.setSettings(hud.settings().withShaderRendering(enabled));
                    shader.setEnabled(enabled);
                }));
        var overlay = addRenderableWidget(new GlassButton(left, panelY + 71, halfWidth, BUTTON_HEIGHT,
                Component.translatable("etherhud.overlay.open"),
                () -> minecraft.setScreen(new OverlaySettingsScreen(this, hud))));
        addRenderableWidget(new GlassButton(contentRight() - halfWidth, panelY + 71, halfWidth, BUTTON_HEIGHT,
                Component.translatable("etherhud.tips.replay"), this::replayTips));
        if (tip == Tip.OVERLAY) tipTarget = overlay;
        if (clothInstalled) {
            addRenderableWidget(new GlassButton(left, panelY + 94, contentRight() - left, BUTTON_HEIGHT,
                    Component.translatable("etherhud.advanced.title"),
                    () -> minecraft.setScreen(ClothSettings.create(this, hud))));
        }
        addRenderableWidget(new GlassButton(left, footerY(), halfWidth, BUTTON_HEIGHT,
                Component.translatable("etherhud.toggle.reset"), () -> show(Page.RESET)));
        var back = addRenderableWidget(new GlassButton(contentRight() - halfWidth, footerY(), halfWidth, BUTTON_HEIGHT,
                backLabel, () -> show(Page.MODULES)));
        setInitialFocus(back);
    }

    private void initReset() {
        confirmationLines = font.split(confirmation, contentRight() - contentLeft());
        int halfWidth = (contentRight() - contentLeft() - GAP) / 2;
        addRenderableWidget(new GlassButton(contentLeft(), footerY(), halfWidth, BUTTON_HEIGHT,
                Component.translatable("etherhud.toggle.reset"), () -> {
                    hud.setSettings(HudSettings.defaults());
                    show(Page.MODULES);
                }));
        var cancel = addRenderableWidget(new GlassButton(contentRight() - halfWidth, footerY(), halfWidth,
                BUTTON_HEIGHT, Component.translatable("etherhud.ui.cancel"), () -> show(Page.OPTIONS)));
        setInitialFocus(cancel);
    }

    private int contentLeft() { return panelX + INSET; }
    private int contentRight() { return panelX + panelWidth - INSET; }
    private int footerY() { return panelY + panelHeight - INSET - BUTTON_HEIGHT; }
    private int firstModule() { return modulePage * rowsPerPage; }
    private int lastModule() { return Math.min(firstModule() + rowsPerPage, ModuleId.ALL.length); }
    private int rowY(ModuleId id) { return panelY + HEADER_HEIGHT + (id.ordinal() - firstModule()) * ROW_HEIGHT; }
    private int navigationX() { return (contentLeft() + 72 + contentRight() - 54) / 2; }

    private boolean turnPage(int direction) {
        int next = Math.clamp(modulePage + direction, 0, pageCount - 1);
        if (modulePage == next) {
            return false;
        }
        modulePage = next;
        rebuildPending = true;
        return true;
    }

    private FormattedCharSequence fit(Component label, int availableWidth) {
        return Language.getInstance().getVisualOrder(font.substrByWidth(label, Math.max(0, availableWidth)));
    }

    private void setAll(boolean enabled) {
        hud.setSettings(hud.settings().withAllEnabled(enabled));
        show(Page.MODULES);
    }

    private void show(Page next) {
        if (tip != null) {
            if (next == Page.OPTIONS) tip = Tip.OVERLAY;
            if (next == Page.MODULES && tip == Tip.OVERLAY) tip = Tip.MORE;
        }
        page = next;
        focusMore = next == Page.MODULES;
        rebuildPending = true;
    }

    private void initTip(boolean beside) {
        tipX = beside ? panelX + panelWidth + 8 : (width - tipWidth) / 2;
        tipY = beside ? Math.clamp(tipTarget.getY(), 4, Math.max(4, height - tipHeight - 4))
                : panelY + panelHeight + 8;
        int current = tip.ordinal() + 1;
        tipProgress = Component.translatable("etherhud.tips.progress", current, TIPS.length, TIPS.length - current);
        int buttonY = tipY + tipHeight - TIP_INSET - TIP_BUTTON_HEIGHT;
        int buttonWidth = (tipWidth - 2 * TIP_INSET - GAP) / 2;
        addRenderableWidget(new RectButton(tipX + TIP_INSET, buttonY, buttonWidth, TIP_BUTTON_HEIGHT,
                Component.translatable("etherhud.tips.exit"), this::finishTips));
        addRenderableWidget(new RectButton(tipX + tipWidth - TIP_INSET - buttonWidth, buttonY, buttonWidth,
                TIP_BUTTON_HEIGHT, Component.translatable(tip == Tip.OVERLAY ? "etherhud.setting.done" : "etherhud.tips.next"),
                this::advanceTip));
    }

    private void advanceTip() {
        if (tip == Tip.OVERLAY) {
            finishTips();
        } else {
            tip = TIPS[tip.ordinal() + 1];
            show(tip == Tip.OVERLAY ? Page.OPTIONS : Page.MODULES);
        }
    }

    private void replayTips() {
        tip = Tip.MODULES;
        modulePage = 0;
        show(Page.MODULES);
    }

    private void finishTips() {
        tip = null;
        hud.completeTips();
        rebuildPending = true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        boolean handled = super.mouseClicked(event, doubled);
        rebuildIfPending();
        return handled;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (page == Page.MODULES && vertical != 0 && turnPage(vertical < 0 ? 1 : -1)) {
            rebuildIfPending();
            return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        boolean handled = super.keyPressed(event);
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
        drawFrostedPanel(graphics, panelX, panelY, panelWidth, panelHeight);
        switch (page) {
            case MODULES -> drawModules(graphics, mouseX, mouseY);
            case OPTIONS -> {
                graphics.drawString(font, optionsTitle, contentLeft(), panelY + 10, GlassPanel.TEXT, false);
                graphics.drawString(font, shaderLabel, contentLeft(), panelY + 55, GlassPanel.TEXT, false);
            }
            case RESET -> {
                int y = panelY + 12;
                for (var line : confirmationLines) {
                    graphics.drawCenteredString(font, line, width / 2, y, GlassPanel.TEXT);
                    y += font.lineHeight;
                }
            }
        }
        if (tipTarget != null) {
            graphics.renderOutline(tipTarget.getX() - 2, tipTarget.getY() - 1,
                    tipTarget.getWidth() + 4, tipTarget.getHeight() + 2, GlassPanel.ACCENT);
            graphics.fill(tipX, tipY, tipX + tipWidth, tipY + tipHeight, 0xF0142632);
            graphics.renderOutline(tipX, tipY, tipWidth, tipHeight, GlassPanel.ACCENT);
            int y = tipY + TIP_INSET;
            graphics.drawString(font, tipProgress, tipX + TIP_INSET, y, GlassPanel.ACCENT, false);
            y += font.lineHeight + 4;
            for (var line : tipLines) {
                graphics.drawString(font, line, tipX + TIP_INSET, y, GlassPanel.TEXT, false);
                y += font.lineHeight;
            }
        }
    }

    private void drawModules(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = contentLeft();
        int right = contentRight();
        graphics.drawString(font, fittedHeading, left, panelY + 11, GlassPanel.TEXT, false);
        graphics.drawString(font, masterLabel, masterLabelX, panelY + 11, GlassPanel.MUTED, false);
        graphics.fill(left, panelY + 28, right, panelY + 29, 0x49698595);

        for (int i = firstModule(); i < lastModule(); i++) {
            ModuleId id = ModuleId.ALL[i];
            int y = rowY(id);
            if (mouseX >= left && mouseX < right && mouseY >= y && mouseY < y + ROW_HEIGHT) {
                GlassPanel.rounded(graphics, left - 3, y, right - left + 6, ROW_HEIGHT, 0x323D6070);
            }
            graphics.drawString(font, labels[id.ordinal()], left, y + 6, GlassPanel.TEXT, false);
        }
        if (pageCount > 1) {
            graphics.drawCenteredString(font, pageLabel, navigationX(), footerY() + 6, GlassPanel.MUTED);
        }
    }

    @Override
    public void onClose() {
        if (tipTarget != null) {
            finishTips();
            rebuildIfPending();
            return;
        }
        switch (page) {
            case MODULES -> minecraft.setScreen(parent);
            case OPTIONS -> show(Page.MODULES);
            case RESET -> show(Page.OPTIONS);
        }
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
