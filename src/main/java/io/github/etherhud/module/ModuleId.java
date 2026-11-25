package io.github.etherhud.module;

import net.minecraft.network.chat.Component;

public enum ModuleId {
    FPS("fps"),
    COORDINATES("coordinates"),
    CPS("cps"),
    ARMOR_STATUS("armorStatus", "armor_status"),
    PING("ping"),
    TPS("tps"),
    PLAYER_MODEL("playerModel", "player_model"),
    ITEM_COUNT("itemCount", "item_count"),
    COMPASS("compass"),
    IMAGE("image");

    public static final ModuleId[] ALL = values();

    private final String key;
    private final String translationKey;
    private final String descriptionKey;

    ModuleId(String key) {
        this(key, key);
    }

    ModuleId(String key, String translationId) {
        this.key = key;
        this.translationKey = "etherhud.module." + translationId;
        this.descriptionKey = translationKey + ".description";
    }

    public String key() { return key; }
    public Component label() { return Component.translatable(translationKey); }
    public Component description() { return Component.translatable(descriptionKey); }
}
