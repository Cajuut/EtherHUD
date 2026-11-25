package io.github.etherhud.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;
import io.github.etherhud.layout.Anchor;
import io.github.etherhud.layout.ModulePlacement;
import io.github.etherhud.module.ModuleId;
import io.github.etherhud.module.armor.ArmorSlot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public final class ConfigStore {
    private static final Logger LOG = LoggerFactory.getLogger("EtherHUD");
    private static final Gson GSON = new GsonBuilder().setStrictness(Strictness.STRICT).setPrettyPrinting().create();

    private final Path path;
    private HudSettings persisted = HudSettings.defaults();
    private boolean backupPending;
    private boolean tipsCompleted;
    private boolean persistedTipsCompleted;

    public ConfigStore(Path path) {
        this.path = path.toAbsolutePath();
    }

    public HudSettings load() {
        try {
            JsonObject root = GSON.fromJson(Files.readString(path), JsonObject.class);
            if (root == null) {
                throw new JsonParseException("Expected a JSON object");
            }
            persisted = decode(root);
            tipsCompleted = persistedTipsCompleted = booleanValue(root.get("contextTipsCompleted"), false);
            backupPending = false;
        } catch (NoSuchFileException ignored) {
            persisted = HudSettings.defaults();
            tipsCompleted = persistedTipsCompleted = false;
            backupPending = false;
        } catch (JsonParseException | CharacterCodingException exception) {
            LOG.warn("Invalid EtherHUD configuration, using defaults", exception);
            persisted = HudSettings.defaults();
            tipsCompleted = persistedTipsCompleted = false;
            backupPending = !backupInvalidFile();
        } catch (IOException exception) {
            LOG.warn("Could not read EtherHUD configuration", exception);
        }
        return persisted;
    }

    private boolean backupInvalidFile() {
        for (int number = 0; ; number++) {
            String suffix = number == 0 ? ".bak" : "." + number + ".bak";
            Path backup = path.resolveSibling(path.getFileName() + suffix);
            try {
                Files.move(path, backup);
                LOG.warn("Moved invalid EtherHUD configuration to {}", backup);
                return true;
            } catch (FileAlreadyExistsException ignored) {
                // Keep earlier backups.
            } catch (NoSuchFileException ignored) {
                return true;
            } catch (IOException exception) {
                LOG.warn("Could not back up invalid EtherHUD configuration", exception);
                return false;
            }
        }
    }

    private HudSettings decode(JsonObject root) {
        HudSettings result = HudSettings.defaults();
        result = result.withMaster(booleanValue(root.get("masterEnabled"), result.masterEnabled()))
                .withSnap(booleanValue(root.get("snapEnabled"), result.snapEnabled()))
                .withGrid(booleanValue(root.get("gridEnabled"), result.gridEnabled()))
                .withDisabledPreviews(booleanValue(root.get("showDisabledPreviews"), result.showDisabledPreviews()))
                .withShaderRendering(booleanValue(root.get("useShaderRendering"), result.useShaderRendering()))
                .withHudScale(floatValue(root.get("hudScale"), floatValue(root.get("scale"), result.hudScale())));

        if (root.get("overlays") instanceof JsonObject overlays) {
            OverlaySettings defaults = result.overlays();
            result = result.withOverlays(new OverlaySettings(
                    floatValue(overlays.get("totemScale"), defaults.totemScale()),
                    intValue(overlays.get("totemOffsetY"), defaults.totemOffsetY()),
                    intValue(overlays.get("fireOffsetY"), defaults.fireOffsetY()),
                    floatValue(overlays.get("worldFireScale"), defaults.worldFireScale()),
                    intValue(overlays.get("totemSpeed"), defaults.totemSpeed()),
                    floatValue(overlays.get("mainHandScale"), defaults.mainHandScale()),
                    intValue(overlays.get("mainHandOffsetY"), defaults.mainHandOffsetY()),
                    floatValue(overlays.get("offHandScale"), defaults.offHandScale()),
                    intValue(overlays.get("offHandOffsetY"), defaults.offHandOffsetY()),
                    intValue(overlays.get("shieldOpacity"), defaults.shieldOpacity())));
        }

        JsonElement modules = root.get("modules");
        for (ModuleId id : ModuleId.ALL) {
            JsonElement module = modules instanceof JsonObject object ? object.get(id.key()) : null;
            boolean enabled = booleanValue(root.get(id.key() + "Enabled"), result.placement(id).enabled());
            if (id == ModuleId.ARMOR_STATUS && !root.has(id.key() + "Enabled")) {
                enabled = legacyArmorEnabled(root, modules, enabled);
            }
            if (module instanceof JsonObject fields) {
                var defaultsPlacement = result.placement(id);
                boolean custom = booleanValue(fields.get("customPosition"), defaultsPlacement.customPosition());
                Anchor anchor = enumValue(fields.get("anchor"), defaultsPlacement.anchor());
                if (id == ModuleId.ARMOR_STATUS && !custom) {
                    anchor = Anchor.HOTBAR_SIDE;
                } else if (anchor == Anchor.HOTBAR_SIDE) {
                    anchor = Anchor.TOP_LEFT;
                }
                var placement = new ModulePlacement(
                        booleanValue(fields.get("enabled"), enabled),
                        anchor,
                        intValue(fields.get("offsetX"), defaultsPlacement.offsetX()),
                        intValue(fields.get("offsetY"), defaultsPlacement.offsetY()),
                        floatValue(fields.get("scale"), 1),
                        custom);
                result = result.withPlacement(id, placement);
                if (fields.get("appearance") instanceof JsonObject appearance) {
                    var defaults = ModuleAppearance.DEFAULT;
                    result = result.withAppearance(id, new ModuleAppearance(
                            intValue(appearance.get("backgroundOpacity"), defaults.backgroundOpacity()),
                            intValue(appearance.get("backgroundColor"), defaults.backgroundColor()),
                            intValue(appearance.get("outlineColor"), defaults.outlineColor()),
                            booleanValue(appearance.get("outlineEnabled"), defaults.outlineEnabled()),
                            intValue(appearance.get("cornerRadius"), defaults.cornerRadius()),
                            intValue(appearance.get("textColor"), defaults.textColor()),
                            booleanValue(appearance.get("textShadow"), defaults.textShadow())));
                }
                if (id == ModuleId.ARMOR_STATUS && fields.get("options") instanceof JsonObject options) {
                    result = result.withArmorStatus(decodeArmor(options));
                }
                if (id == ModuleId.IMAGE && fields.get("file") instanceof JsonPrimitive file && file.isString()) {
                    result = result.withImagePath(file.getAsString());
                }
            } else {
                result = result.withPlacement(id, result.placement(id).withEnabled(booleanValue(module, enabled)));
            }
        }
        return result;
    }

    private boolean legacyArmorEnabled(JsonObject root, JsonElement modules, boolean fallback) {
        boolean enabled = booleanValue(root.get("durabilityEnabled"), fallback);
        JsonElement old = modules instanceof JsonObject object ? object.get("durability") : null;
        return old instanceof JsonObject fields ? booleanValue(fields.get("enabled"), enabled) : booleanValue(old, enabled);
    }

    private ArmorSettings decodeArmor(JsonObject options) {
        ArmorSettings defaults = ArmorSettings.DEFAULT;
        ArmorSettings result = defaults
                .withStyle(enumValue(options.get("style"), defaults.style()))
                .withDirection(enumValue(options.get("direction"), defaults.direction()))
                .withHotbarSide(enumValue(options.get("hotbarSide"), defaults.hotbarSide()))
                .withValueMode(enumValue(options.get("valueMode"), defaults.valueMode()))
                .withShowCount(booleanValue(options.get("showCount"), defaults.showCount()))
                .withHideEmpty(booleanValue(options.get("hideEmpty"), defaults.hideEmpty()))
                .withShowBar(booleanValue(options.get("showBar"), defaults.showBar()));
        if (options.get("slots") instanceof JsonObject slots) {
            for (ArmorSlot slot : ArmorSlot.ALL) {
                result = result.withSlot(slot, booleanValue(slots.get(slot.key()), defaults.shows(slot)));
            }
        }
        if (options.get("warning") instanceof JsonObject warning) {
            ArmorWarningSettings old = defaults.warning();
            ArmorWarningSettings updated = old.withEnabled(booleanValue(warning.get("enabled"), old.enabled()))
                    .withThreshold(intValue(warning.get("threshold"), old.threshold()));
            for (var channel : ArmorWarningSettings.Channel.ALL) {
                updated = updated.withChannel(channel, booleanValue(warning.get(channel.key()), old.uses(channel)));
            }
            result = result.withWarning(updated);
        }
        return result;
    }

    private JsonObject encodeArmor(ArmorSettings settings) {
        var options = new JsonObject();
        var slots = new JsonObject();
        for (ArmorSlot slot : ArmorSlot.ALL) {
            slots.addProperty(slot.key(), settings.shows(slot));
        }
        options.add("slots", slots);
        options.addProperty("direction", settings.direction().name());
        options.addProperty("style", settings.style().name());
        options.addProperty("hotbarSide", settings.hotbarSide().name());
        options.addProperty("valueMode", settings.valueMode().name());
        options.addProperty("showCount", settings.showCount());
        options.addProperty("hideEmpty", settings.hideEmpty());
        options.addProperty("showBar", settings.showBar());
        var warning = new JsonObject();
        warning.addProperty("enabled", settings.warning().enabled());
        warning.addProperty("threshold", settings.warning().threshold());
        for (var channel : ArmorWarningSettings.Channel.ALL) {
            warning.addProperty(channel.key(), settings.warning().uses(channel));
        }
        options.add("warning", warning);
        return options;
    }

    private boolean booleanValue(JsonElement value, boolean fallback) {
        return value instanceof JsonPrimitive primitive && primitive.isBoolean() ? primitive.getAsBoolean() : fallback;
    }

    private float floatValue(JsonElement value, float fallback) {
        if (value instanceof JsonPrimitive primitive && primitive.isNumber()) {
            float number = primitive.getAsFloat();
            if (Float.isFinite(number)) {
                return number;
            }
        }
        return fallback;
    }

    private int intValue(JsonElement value, int fallback) {
        if (value instanceof JsonPrimitive primitive && primitive.isNumber()) {
            try {
                return primitive.getAsBigDecimal().intValueExact();
            } catch (ArithmeticException | NumberFormatException ignored) {
                // The field cannot represent a GUI pixel offset.
            }
        }
        return fallback;
    }

    private <E extends Enum<E>> E enumValue(JsonElement value, E fallback) {
        if (value instanceof JsonPrimitive primitive && primitive.isString()) {
            try {
                return Enum.valueOf(fallback.getDeclaringClass(), primitive.getAsString().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // Unknown enum values fall back to the default.
            }
        }
        return fallback;
    }

    public void save(HudSettings settings) {
        if (persisted.equals(settings) && tipsCompleted == persistedTipsCompleted) {
            return;
        }
        if (backupPending) {
            backupPending = !backupInvalidFile();
            if (backupPending) {
                return;
            }
        }
        try {
            Files.createDirectories(path.getParent());
            writeConfig(settings);
            persisted = settings;
            persistedTipsCompleted = tipsCompleted;
        } catch (IOException exception) {
            LOG.warn("Could not save EtherHUD configuration", exception);
        }
    }

    public boolean tipsCompleted() {
        return tipsCompleted;
    }

    public void completeTips() {
        tipsCompleted = true;
    }

    private void writeConfig(HudSettings settings) throws IOException {
        Path temporary = Files.createTempFile(path.getParent(), "etherhud-", ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(encode(settings)) + "\n");
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private JsonObject encode(HudSettings settings) {
        var root = new JsonObject();
        root.addProperty("masterEnabled", settings.masterEnabled());
        root.addProperty("contextTipsCompleted", tipsCompleted);
        root.addProperty("hudScale", settings.hudScale());
        root.addProperty("snapEnabled", settings.snapEnabled());
        root.addProperty("gridEnabled", settings.gridEnabled());
        root.addProperty("showDisabledPreviews", settings.showDisabledPreviews());
        root.addProperty("useShaderRendering", settings.useShaderRendering());
        root.add("overlays", GSON.toJsonTree(settings.overlays()));
        var modules = new JsonObject();
        for (ModuleId id : ModuleId.ALL) {
            JsonObject module = GSON.toJsonTree(settings.placement(id)).getAsJsonObject();
            module.add("appearance", GSON.toJsonTree(settings.appearance(id)));
            if (id == ModuleId.ARMOR_STATUS) {
                module.add("options", encodeArmor(settings.armorStatus()));
            }
            if (id == ModuleId.IMAGE && !settings.imagePath().isBlank()) {
                module.addProperty("file", settings.imagePath());
            }
            modules.add(id.key(), module);
        }
        root.add("modules", modules);
        return root;
    }
}
