package io.github.etherhud.module.armor;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;

public enum ArmorSlot {
    MAIN_HAND("main_hand", EquipmentSlot.MAINHAND),
    OFF_HAND("off_hand", EquipmentSlot.OFFHAND),
    HELMET("helmet", EquipmentSlot.HEAD),
    CHESTPLATE("chestplate", EquipmentSlot.CHEST),
    LEGGINGS("leggings", EquipmentSlot.LEGS),
    BOOTS("boots", EquipmentSlot.FEET);

    public static final ArmorSlot[] ALL = values();
    public static final int ALL_VISIBLE = (1 << ALL.length) - 1;

    private final String key;
    private final EquipmentSlot equipmentSlot;

    ArmorSlot(String key, EquipmentSlot equipmentSlot) {
        this.key = key;
        this.equipmentSlot = equipmentSlot;
    }

    public String key() { return key; }
    public EquipmentSlot equipmentSlot() { return equipmentSlot; }
    public int bit() { return 1 << ordinal(); }

    public Component label() {
        return Component.translatable("etherhud.setting.armor.slot." + key);
    }
}
