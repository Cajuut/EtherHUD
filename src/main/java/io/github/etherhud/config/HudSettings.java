package io.github.etherhud.config;

import io.github.etherhud.layout.Anchor;
import io.github.etherhud.layout.ModulePlacement;
import io.github.etherhud.module.ModuleId;

import java.util.Arrays;
import java.util.Objects;

public final class HudSettings {
    private final ModulePlacement[] placements;
    private final ModuleAppearance[] appearances;
    private final float hudScale;
    private final boolean masterEnabled;
    private final boolean snapEnabled;
    private final boolean gridEnabled;
    private final boolean useShaderRendering;
    private final ArmorSettings armorStatus;
    private final OverlaySettings overlays;
    private final boolean showDisabledPreviews;
    private final String imagePath;

    private HudSettings(ModulePlacement[] placements, ModuleAppearance[] appearances, float hudScale, boolean masterEnabled,
                        boolean snapEnabled, boolean gridEnabled, boolean useShaderRendering, ArmorSettings armorStatus,
                        OverlaySettings overlays, boolean showDisabledPreviews, String imagePath) {
        this.placements = placements;
        this.appearances = appearances;
        this.hudScale = ModulePlacement.normalizeScale(hudScale);
        this.masterEnabled = masterEnabled;
        this.snapEnabled = snapEnabled;
        this.gridEnabled = gridEnabled;
        this.useShaderRendering = useShaderRendering;
        this.armorStatus = armorStatus;
        this.overlays = overlays;
        this.showDisabledPreviews = showDisabledPreviews;
        this.imagePath = imagePath;
    }

    public static HudSettings defaults() {
        ModulePlacement[] placements = new ModulePlacement[ModuleId.ALL.length];
        for (ModuleId id : ModuleId.ALL) {
            placements[id.ordinal()] = defaultPlacement(id);
        }
        ModuleAppearance[] appearances = new ModuleAppearance[placements.length];
        Arrays.fill(appearances, ModuleAppearance.DEFAULT);
        return new HudSettings(placements, appearances, 1, true, true, false, true, ArmorSettings.DEFAULT, OverlaySettings.DEFAULT, true, "");
    }

    public ModulePlacement placement(ModuleId id) { return placements[id.ordinal()]; }
    public ModuleAppearance appearance(ModuleId id) { return appearances[id.ordinal()]; }
    public float hudScale() { return hudScale; }
    public boolean masterEnabled() { return masterEnabled; }
    public boolean snapEnabled() { return snapEnabled; }
    public boolean gridEnabled() { return gridEnabled; }
    public boolean useShaderRendering() { return useShaderRendering; }
    public ArmorSettings armorStatus() { return armorStatus; }
    public OverlaySettings overlays() { return overlays; }
    public boolean showDisabledPreviews() { return showDisabledPreviews; }
    public String imagePath() { return imagePath; }
    public boolean canResize(ModuleId id) { return id != ModuleId.ARMOR_STATUS || !armorStatus.vanilla(); }
    public boolean vanillaArmorDocked() { return armorStatus.vanilla() && placement(ModuleId.ARMOR_STATUS).docked(); }

    public HudSettings withPlacement(ModuleId id, ModulePlacement placement) {
        if (placement.equals(placement(id))) {
            return this;
        }
        ModulePlacement[] updated = placements.clone();
        updated[id.ordinal()] = placement;
        return new HudSettings(updated, appearances, hudScale, masterEnabled, snapEnabled, gridEnabled, useShaderRendering, armorStatus, overlays, showDisabledPreviews, imagePath);
    }

    public HudSettings withAppearance(ModuleId id, ModuleAppearance appearance) {
        if (appearance.equals(appearance(id))) {
            return this;
        }
        ModuleAppearance[] updated = appearances.clone();
        updated[id.ordinal()] = appearance;
        return new HudSettings(placements, updated, hudScale, masterEnabled, snapEnabled, gridEnabled, useShaderRendering, armorStatus, overlays, showDisabledPreviews, imagePath);
    }

    public HudSettings withModuleDefaults(ModuleId id) {
        HudSettings updated = withPlacement(id, defaultPlacement(id))
                .withAppearance(id, ModuleAppearance.DEFAULT);
        return switch (id) {
            case ARMOR_STATUS -> updated.withArmorStatus(ArmorSettings.DEFAULT);
            case IMAGE -> updated.withImagePath("");
            default -> updated;
        };
    }

    private static ModulePlacement defaultPlacement(ModuleId id) {
        return switch (id) {
            case ARMOR_STATUS -> ModulePlacement.ARMOR_DEFAULT;
            case PING, TPS, PLAYER_MODEL, ITEM_COUNT, IMAGE -> ModulePlacement.DEFAULT.withEnabled(false);
            case COMPASS -> new ModulePlacement(false, Anchor.TOP_CENTER, 0, 6, 1, true);
            default -> ModulePlacement.DEFAULT;
        };
    }

    public HudSettings withArmorStatus(ArmorSettings armorStatus) {
        return new HudSettings(placements, appearances, hudScale, masterEnabled, snapEnabled, gridEnabled, useShaderRendering, armorStatus, overlays, showDisabledPreviews, imagePath);
    }

    public HudSettings withOverlays(OverlaySettings overlays) {
        return new HudSettings(placements, appearances, hudScale, masterEnabled, snapEnabled, gridEnabled,
                useShaderRendering, armorStatus, overlays, showDisabledPreviews, imagePath);
    }

    public HudSettings withAllEnabled(boolean enabled) {
        HudSettings result = this;
        for (ModuleId id : ModuleId.ALL) {
            result = result.withPlacement(id, placement(id).withEnabled(enabled));
        }
        return result;
    }

    public HudSettings withHudScale(float scale) {
        return new HudSettings(placements, appearances, scale, masterEnabled, snapEnabled, gridEnabled, useShaderRendering, armorStatus, overlays, showDisabledPreviews, imagePath);
    }

    public HudSettings withMaster(boolean enabled) {
        return new HudSettings(placements, appearances, hudScale, enabled, snapEnabled, gridEnabled, useShaderRendering, armorStatus, overlays, showDisabledPreviews, imagePath);
    }

    public HudSettings withSnap(boolean enabled) {
        return new HudSettings(placements, appearances, hudScale, masterEnabled, enabled, gridEnabled, useShaderRendering, armorStatus, overlays, showDisabledPreviews, imagePath);
    }

    public HudSettings withGrid(boolean enabled) {
        return new HudSettings(placements, appearances, hudScale, masterEnabled, snapEnabled, enabled, useShaderRendering, armorStatus, overlays, showDisabledPreviews, imagePath);
    }

    public HudSettings withShaderRendering(boolean enabled) {
        return new HudSettings(placements, appearances, hudScale, masterEnabled, snapEnabled, gridEnabled, enabled, armorStatus, overlays, showDisabledPreviews, imagePath);
    }

    public HudSettings withDisabledPreviews(boolean show) {
        return new HudSettings(placements, appearances, hudScale, masterEnabled, snapEnabled, gridEnabled,
                useShaderRendering, armorStatus, overlays, show, imagePath);
    }

    public HudSettings withImagePath(String path) {
        return new HudSettings(placements, appearances, hudScale, masterEnabled, snapEnabled, gridEnabled,
                useShaderRendering, armorStatus, overlays, showDisabledPreviews, path);
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof HudSettings other
                && hudScale == other.hudScale && masterEnabled == other.masterEnabled
                && snapEnabled == other.snapEnabled && gridEnabled == other.gridEnabled
                && useShaderRendering == other.useShaderRendering
                && showDisabledPreviews == other.showDisabledPreviews
                && imagePath.equals(other.imagePath)
                && armorStatus.equals(other.armorStatus)
                && overlays.equals(other.overlays)
                && Arrays.equals(placements, other.placements) && Arrays.equals(appearances, other.appearances);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(placements), Arrays.hashCode(appearances),
                hudScale, masterEnabled, snapEnabled, gridEnabled, useShaderRendering, armorStatus, overlays, showDisabledPreviews, imagePath);
    }
}
