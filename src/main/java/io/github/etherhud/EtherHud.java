package io.github.etherhud;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.etherhud.config.ConfigStore;
import io.github.etherhud.config.HudSettings;
import io.github.etherhud.layout.LayoutResolver;
import io.github.etherhud.mixin.ContainerScreenAccessor;
import io.github.etherhud.mixin.RecipeBookScreenAccessor;
import io.github.etherhud.module.HudModules;
import io.github.etherhud.module.ModuleId;
import io.github.etherhud.render.HudRenderer;
import io.github.etherhud.render.GlassRenderer;
import io.github.etherhud.render.WorldFireModel;
import io.github.etherhud.ui.HudEditorScreen;
import io.github.etherhud.ui.ModuleToggleScreen;
import io.github.etherhud.ui.ModuleSettingsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class EtherHud implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("etherhud", "etherhud"));
    private final HudModules modules = new HudModules();
    private final HudRenderer renderer = new HudRenderer(false);
    private final LayoutResolver.Bounds containerDock = new LayoutResolver.Bounds();
    private ConfigStore config;
    private KeyMapping toggleKey;
    private KeyMapping editorKey;
    private HudSettings settings = HudSettings.defaults();
    private final boolean[] ticking = new boolean[ModuleId.ALL.length];
    private boolean hudWasVisible;
    private static EtherHud instance;

    public static EtherHud instance() { return instance; }
    public HudModules modules() { return modules; }
    public HudSettings settings() { return settings; }

    public void setSettings(HudSettings settings) {
        if (!settings.masterEnabled() || !settings.placement(ModuleId.CPS).enabled()) {
            modules.clearClicks();
        }
        this.settings = settings;
        GlassRenderer.setEnabled(settings.useShaderRendering());
    }

    public void saveSettings() {
        WorldFireModel.applyScale(settings.overlays().worldFireScale(), Minecraft.getInstance());
        config.save(settings);
    }

    public boolean tipsCompleted() {
        return config.tipsCompleted();
    }

    public void completeTips() {
        config.completeTips();
        saveSettings();
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        GlassRenderer.registerReloadListener();
        config = new ConfigStore(FabricLoader.getInstance().getConfigDir().resolve("etherhud.json"));
        setSettings(config.load());
        WorldFireModel.register(settings.overlays().worldFireScale());
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("etherhud.key.settings",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));
        editorKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("etherhud.key.editor",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof AbstractContainerScreen<?> container) {
                ScreenEvents.afterRender(screen).register((ignored, graphics, x, y, tickDelta) ->
                        drawContainerArmor(client, container, graphics));
            }
        });
        // Attach before CHAT so panels sit above the hotbar/status bars but below chat.
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("etherhud", "panels"), (graphics, delta) -> {
                    Minecraft client = Minecraft.getInstance();
                    if (!visible(client)) {
                        return;
                    }
                    renderer.layout(client, settings, modules, graphics.guiWidth(), graphics.guiHeight());
                    for (ModuleId id : ModuleId.ALL) {
                        if (settings.placement(id).enabled() && !(id == ModuleId.ARMOR_STATUS
                                && settings.vanillaArmorDocked() && client.screen instanceof AbstractContainerScreen<?>)) {
                            var bounds = renderer.bounds(id);
                            renderer.draw(client, graphics, id, bounds.x(), bounds.y(), false);
                        }
                    }
                });
    }

    private void drawContainerArmor(Minecraft client, AbstractContainerScreen<?> container, GuiGraphics graphics) {
        ModuleId id = ModuleId.ARMOR_STATUS;
        if (!settings.vanillaArmorDocked() || !settings.placement(id).enabled()
                || !visible(client) || !modules.armor().active(settings)) {
            return;
        }
        renderer.layout(client, settings, modules, graphics.guiWidth(), graphics.guiHeight());
        if (!renderer.hasContent(id)) {
            return;
        }
        // These are the actual 1.21.4 container bounds, including recipe-book repositioning.
        var bounds = (ContainerScreenAccessor) container;
        int left = bounds.etherhud$left();
        int right = left + bounds.etherhud$imageWidth();
        if (container instanceof RecipeBookScreenAccessor recipe && recipe.etherhud$recipeBook().isVisible()) {
            if (recipe.etherhud$recipeReplacesInventory()) {
                return;
            }
            // 1.21.4 RecipeBookComponent uses a 147 px wide panel beside the container.
            left -= 147 + 4;
        }
        if (LayoutResolver.vanillaDock(containerDock, renderer.panelWidth(id), renderer.panelHeight(id),
                graphics.guiWidth(), graphics.guiHeight(), left, right,
                bounds.etherhud$top() + bounds.etherhud$imageHeight(), settings.armorStatus().hotbarSide())) {
            renderer.drawScaled(client, graphics, id, containerDock.x(), containerDock.y(), 1, false);
        }
    }

    private void tick(Minecraft client) {
        while (toggleKey.consumeClick()) {
            if (client.screen == null && client.player != null) {
                client.setScreen(new ModuleToggleScreen(null, this));
            }
        }
        while (editorKey.consumeClick()) {
            if (client.screen == null && client.player != null) {
                client.setScreen(new HudEditorScreen(null, this));
            }
        }
        boolean hudVisible = visible(client);
        if (!hudVisible && !hudWasVisible) {
            return;
        }
        hudWasVisible = hudVisible;
        for (ModuleId id : ModuleId.ALL) {
            var module = modules.get(id);
            boolean active = hudVisible && settings.placement(id).enabled() && module.active(settings);
            if (active) {
                module.update(client, settings);
            } else if (ticking[id.ordinal()]) {
                module.onInactive();
            }
            ticking[id.ordinal()] = active;
        }
    }

    private boolean visible(Minecraft client) {
        return settings.masterEnabled() && client.player != null && !client.options.hideGui
                && !(client.screen instanceof HudEditorScreen) && !(client.screen instanceof ModuleSettingsScreen)
                && !client.getDebugOverlay().showDebugScreen();
    }

    public void recordClick(boolean right) {
        Minecraft client = Minecraft.getInstance();
        if (client.screen == null && visible(client) && settings.placement(ModuleId.CPS).enabled()) {
            modules.click(right);
        }
    }

    public void recordServerTime(long gameTime) {
        if (settings.placement(ModuleId.TPS).enabled()) {
            Minecraft client = Minecraft.getInstance();
            if (client.level != null && visible(client)) {
                modules.tps().observe(client.level, gameTime);
            }
        }
    }
}
