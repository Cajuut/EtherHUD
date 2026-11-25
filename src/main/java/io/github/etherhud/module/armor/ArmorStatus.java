package io.github.etherhud.module.armor;

import io.github.etherhud.config.ArmorSettings;
import io.github.etherhud.config.ArmorWarningSettings;
import io.github.etherhud.config.ArmorWarningSettings.Channel;
import io.github.etherhud.config.HudSettings;
import io.github.etherhud.config.ModuleAppearance;
import io.github.etherhud.layout.Anchor;
import io.github.etherhud.module.HudModule;
import io.github.etherhud.module.ModuleId;
import io.github.etherhud.module.settings.CommonModuleSettings;
import io.github.etherhud.module.settings.ModuleSetting;
import io.github.etherhud.render.GlassRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ArmorStatus implements HudModule {
    private static final int SLOT_SIZE = 24;
    private static final int HOTBAR_TEXTURE_WIDTH = 182;
    private static final int HOTBAR_THICKNESS = 22;
    private static final int HOTBAR_SLOT_STEP = 20;
    private static final Identifier HOTBAR_SPRITE = Identifier.withDefaultNamespace("hud/hotbar");
    private static final int VALUE_HEIGHT = 8;
    private static final int VALUE_COLUMN_WIDTH = 24;
    // Vanilla 1.21.4 Gui#renderItemHotbar extends the hotbar by 29 px for the offhand.
    private static final int OFFHAND_WIDTH = 29;
    private static final int ATTACK_INDICATOR_SPACE = 26;
    private static final float TEXT_SCALE = 0.68f;
    private static final Identifier WARNING_ICON = Identifier.fromNamespaceAndPath(
            "etherhud", "textures/gui/warn.png");

    private final SlotView[] live = new SlotView[ArmorSlot.ALL.length];
    private SlotView[] examples;
    private Language exampleLanguage;
    private LocalPlayer sampledPlayer;
    private int sampleAfter;
    private int sampledMask;
    private int occupied;
    private int liveLowMask;
    private int exampleLowMask;
    private int sampledThreshold;
    private int exampleThreshold;
    private int reserveLeft;
    private int reserveRight;

    private enum PlacementMode { LEFT, RIGHT, MANUAL }

    public ArmorStatus() {
        for (int i = 0; i < live.length; i++) {
            live[i] = new SlotView();
        }
    }

    @Override
    public List<ModuleSetting> settings(ModuleId id) {
        var fields = new ArrayList<ModuleSetting>();
        fields.add(new ModuleSetting.Choice<>("armor.style", List.of(ArmorSettings.Style.values()),
                s -> s.armorStatus().style(), (s, v) -> s.withArmorStatus(s.armorStatus().withStyle(v)),
                value -> option("style", value)));
        fields.add(new ModuleSetting.Choice<>("armor.direction", List.of(ArmorSettings.Direction.values()),
                s -> s.armorStatus().direction(), (s, v) -> s.withArmorStatus(s.armorStatus().withDirection(v)),
                value -> option("direction", value)));
        fields.add(new ModuleSetting.Choice<>("armor.placement", List.of(PlacementMode.values()), this::placementMode,
                this::withPlacementMode, value -> option("placement", value)));
        fields.addAll(HudModule.super.settings(id));
        fields.add(new ModuleSetting.Section("armor.slots"));
        for (ArmorSlot slot : ArmorSlot.ALL) {
            fields.add(new ModuleSetting.Toggle("armor.slot." + slot.key(), s -> s.armorStatus().shows(slot),
                    (s, v) -> s.withArmorStatus(s.armorStatus().withSlot(slot, v))));
        }
        fields.add(new ModuleSetting.Section("armor.display"));
        fields.add(new ModuleSetting.Choice<>("armor.values", List.of(ArmorSettings.ValueMode.values()),
                s -> s.armorStatus().valueMode(), (s, v) -> s.withArmorStatus(s.armorStatus().withValueMode(v)),
                value -> option("values", value)));
        fields.add(new ModuleSetting.Toggle("armor.count", s -> s.armorStatus().showCount(),
                (s, v) -> s.withArmorStatus(s.armorStatus().withShowCount(v))));
        fields.add(new ModuleSetting.Toggle("armor.hide_empty", s -> s.armorStatus().hideEmpty(),
                (s, v) -> s.withArmorStatus(s.armorStatus().withHideEmpty(v))));
        fields.add(new ModuleSetting.Toggle("armor.bar", s -> s.armorStatus().showBar(),
                (s, v) -> s.withArmorStatus(s.armorStatus().withShowBar(v))));
        fields.add(new ModuleSetting.Section("armor.warning"));
        fields.add(new ModuleSetting.Toggle("armor.warning.enabled", s -> s.armorStatus().warning().enabled(),
                (s, v) -> s.withArmorStatus(s.armorStatus().withWarning(s.armorStatus().warning().withEnabled(v)))));
        fields.add(new ModuleSetting.Slider("armor.warning.threshold", 5, 50, 1, s -> s.armorStatus().warning().threshold(),
                (s, v) -> s.withArmorStatus(s.armorStatus().withWarning(s.armorStatus().warning().withThreshold(v))),
                CommonModuleSettings::percent));
        for (Channel channel : Channel.ALL) {
            fields.add(new ModuleSetting.Toggle("armor.warning." + channel.key(), s -> s.armorStatus().warning().uses(channel),
                    (s, v) -> s.withArmorStatus(s.armorStatus().withWarning(s.armorStatus().warning().withChannel(channel, v)))));
        }
        return List.copyOf(fields);
    }

    private PlacementMode placementMode(HudSettings settings) {
        if (!settings.placement(ModuleId.ARMOR_STATUS).docked()) {
            return PlacementMode.MANUAL;
        }
        return settings.armorStatus().hotbarSide() == ArmorSettings.HotbarSide.LEFT ? PlacementMode.LEFT : PlacementMode.RIGHT;
    }

    private HudSettings withPlacementMode(HudSettings settings, PlacementMode mode) {
        var placement = settings.placement(ModuleId.ARMOR_STATUS);
        if (mode == PlacementMode.MANUAL) {
            return placement.docked() ? settings.withPlacement(ModuleId.ARMOR_STATUS, placement.withAnchor(Anchor.TOP_LEFT))
                    : settings;
        }
        var side = mode == PlacementMode.LEFT ? ArmorSettings.HotbarSide.LEFT : ArmorSettings.HotbarSide.RIGHT;
        return settings.withPlacement(ModuleId.ARMOR_STATUS, placement.dock())
                .withArmorStatus(settings.armorStatus().withHotbarSide(side));
    }

    private static Component option(String field, Enum<?> value) {
        return Component.translatable("etherhud.setting.armor." + field + "." + value.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean active(HudSettings settings) {
        return settings.armorStatus().slotMask() != 0;
    }

    @Override
    public void update(Minecraft client, HudSettings settings) {
        ArmorSettings options = settings.armorStatus();
        ArmorWarningSettings warning = options.warning();
        int shown = options.slotMask();
        int threshold = warning.enabled() ? warning.threshold() : 0;
        boolean notify = warning.uses(Channel.SOUND) || warning.uses(Channel.ACTION_BAR);
        boolean newBaseline = sampledPlayer != client.player || sampledThreshold != threshold;
        if (!newBaseline && sampledMask == shown && sampleAfter-- > 0) {
            return;
        }
        int previouslyShown = sampledMask;
        sampledPlayer = client.player;
        sampledMask = shown;
        sampledThreshold = threshold;
        sampleAfter = 4;
        sampleHotbarSpace(client, sampledPlayer);
        occupied = 0;
        liveLowMask = 0;
        for (ArmorSlot slot : ArmorSlot.ALL) {
            if ((shown & slot.bit()) != 0) {
                ItemStack item = client.player.getItemBySlot(slot.equipmentSlot());
                SlotView view = live[slot.ordinal()];
                ItemStack previous = view.item;
                int oldRemaining = view.remaining;
                int oldMaximum = view.maximum;
                view.update(item, client.font);
                if (threshold != 0) {
                    boolean reset = newBaseline || (previouslyShown & slot.bit()) == 0 || oldMaximum != view.maximum;
                    if (reset || oldRemaining != view.remaining) {
                        reset |= !sameEquipment(previous, item);
                        if (view.warning.observe(view.remaining, view.maximum, threshold, reset, notify, System.nanoTime())) {
                            emitWarning(client, slot, warning);
                        }
                    } else if (previous != view.item && !sameEquipment(previous, item)) {
                        view.warning.observe(view.remaining, view.maximum, threshold, true, false, 0);
                    }
                    if (view.warning.low()) {
                        liveLowMask |= slot.bit();
                    }
                }
                if (!item.isEmpty()) {
                    occupied |= slot.bit();
                }
            }
        }
    }

    private void sampleHotbarSpace(Minecraft client, LocalPlayer player) {
        boolean mainLeft = player.getMainArm() == HumanoidArm.LEFT;
        int offhand = player.getOffhandItem().isEmpty() ? 0 : OFFHAND_WIDTH;
        int indicator = client.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR ? ATTACK_INDICATOR_SPACE : 0;
        reserveLeft = mainLeft ? indicator : offhand;
        reserveRight = mainLeft ? offhand : indicator;
    }

    public int reserveLeft() { return reserveLeft; }
    public int reserveRight() { return reserveRight; }

    @Override
    public void onInactive() {
        sampledPlayer = null;
        sampledMask = 0;
        occupied = 0;
        liveLowMask = 0;
    }

    private boolean sameEquipment(ItemStack previous, ItemStack now) {
        if (previous == null || !ItemStack.isSameItem(previous, now)) {
            return false;
        }
        var before = previous.getComponents();
        var after = now.getComponents();
        if (before.size() != after.size()) {
            return false;
        }
        for (var component : before) {
            if (component.type() != DataComponents.DAMAGE && !Objects.equals(component.value(), after.get(component.type()))) {
                return false;
            }
        }
        return true;
    }

    private void emitWarning(Minecraft client, ArmorSlot slot, ArmorWarningSettings settings) {
        if (settings.uses(Channel.SOUND)) {
            client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.15f, 0.65f));
        }
        if (settings.uses(Channel.ACTION_BAR)) {
            client.gui.setOverlayMessage(Component.translatable("etherhud.armor.warning.message", slot.label()), false);
        }
    }

    @Override
    public int layoutVersion(HudSettings settings, boolean preview) {
        return visible(settings.armorStatus(), preview);
    }

    @Override
    public int panelWidth(Font font, Component label, HudSettings settings, boolean preview) {
        ArmorSettings options = settings.armorStatus();
        int slots = Integer.bitCount(visible(options, preview));
        if (slots == 0) {
            return 0;
        }
        if (options.vanilla()) {
            return options.vertical() ? HOTBAR_THICKNESS + (hasValues(options) ? VALUE_COLUMN_WIDTH : 0) : hotbarLength(slots);
        }
        return options.vertical() ? SLOT_SIZE : extent(slots, SLOT_SIZE, options.gap());
    }

    @Override
    public int panelHeight(HudSettings settings, boolean preview) {
        ArmorSettings options = settings.armorStatus();
        int slots = Integer.bitCount(visible(options, preview));
        if (slots == 0) {
            return 0;
        }
        int labels = hasValues(options) ? VALUE_HEIGHT : 0;
        if (options.vanilla()) {
            return options.vertical() ? hotbarLength(slots) : HOTBAR_THICKNESS + labels;
        }
        int height = SLOT_SIZE + labels;
        return options.vertical() ? extent(slots, height, options.gap()) : height;
    }

    private boolean hasValues(ArmorSettings options) {
        return options.valueMode() != ArmorSettings.ValueMode.NONE;
    }

    private int hotbarLength(int slots) {
        return slots * HOTBAR_SLOT_STEP + 2;
    }

    private int slotX(ArmorSettings options, int index) {
        if (options.vertical()) {
            return 0;
        }
        return options.vanilla() ? 1 + index * HOTBAR_SLOT_STEP : index * (SLOT_SIZE + options.gap());
    }

    private int slotY(ArmorSettings options, int index) {
        if (!options.vertical()) {
            return 0;
        }
        return options.vanilla() ? 1 + index * HOTBAR_SLOT_STEP
                : index * (SLOT_SIZE + options.gap() + (hasValues(options) ? VALUE_HEIGHT : 0));
    }

    private int extent(int slots, int slotSize, int gap) {
        return slots * slotSize + (slots - 1) * gap;
    }

    private int visible(ArmorSettings options, boolean preview) {
        if (!options.hideEmpty()) {
            return options.slotMask();
        }
        int nonEmpty = preview ? ArmorSlot.ALL_VISIBLE & ~ArmorSlot.LEGGINGS.bit() : occupied;
        return options.slotMask() & nonEmpty;
    }

    @Override
    public void draw(Minecraft client, GuiGraphics graphics, HudSettings settings, ModuleId id,
                     Component label, int width, int height, boolean preview, boolean ghost) {
        ArmorSettings options = settings.armorStatus();
        int visible = visible(options, preview);
        if (visible == 0) {
            return;
        }
        var appearance = settings.appearance(id);
        var warning = options.warning();
        SlotView[] data = preview ? examples(client.font, warning) : live;
        int low = warning.enabled() && !ghost ? visible & (preview ? exampleLowMask : liveLowMask) : 0;
        boolean flash = low != 0 && warning.uses(Channel.BLINK) && System.nanoTime() / 380_000_000L % 2 == 0;
        int slotWidth = options.vanilla() ? (options.vertical() ? HOTBAR_THICKNESS : HOTBAR_SLOT_STEP) : SLOT_SIZE;
        int slotHeight = options.vanilla() ? (options.vertical() ? HOTBAR_SLOT_STEP : HOTBAR_THICKNESS) : SLOT_SIZE;
        int itemInsetX = (slotWidth - 16) / 2;
        // Keep item icons vertically centered in all three frame styles.
        int itemInsetY = (slotHeight - 16) / 2;
        if (options.style() == ArmorSettings.Style.JOINED) {
            float radius = appearance.cornerRadius();
            GlassRenderer.rounded(graphics, 0, 0, width, height, radius, radius, radius, radius,
                    appearance.outlineEnabled() ? 1 : 0,
                    appearance.background(ghost), appearance.outlineEnabled() ? appearance.outline(ghost) : 0);
        } else if (options.vanilla()) {
            drawHotbar(graphics, Integer.bitCount(visible), options.vertical());
            if (appearance.backgroundOpacity() > 0) {
                int index = 0;
                for (ArmorSlot slot : ArmorSlot.ALL) {
                    if ((visible & slot.bit()) != 0) {
                        int x = slotX(options, index) + 2;
                        int y = slotY(options, index) + 2;
                        GlassRenderer.rounded(graphics, x, y, slotWidth - 4, slotHeight - 4, 1,
                                appearance.background(ghost));
                        index++;
                    }
                }
            }
        }

        int index = 0;
        for (ArmorSlot slot : ArmorSlot.ALL) {
            if ((visible & slot.bit()) != 0) {
                drawShapes(graphics, options, appearance, data[slot.ordinal()],
                        slotX(options, index), slotY(options, index), slotWidth, slotHeight,
                        ghost, flash && (low & slot.bit()) != 0);
                index++;
            }
        }
        index = 0;
        for (ArmorSlot slot : ArmorSlot.ALL) {
            if ((visible & slot.bit()) != 0) {
                SlotView view = data[slot.ordinal()];
                if (!view.empty()) {
                    graphics.renderItem(view.item, slotX(options, index) + itemInsetX, slotY(options, index) + itemInsetY);
                }
                index++;
            }
        }
        int icons = warning.uses(Channel.ICON) ? low : 0;
        if (icons != 0) {
            index = 0;
            int selectedIndex = -1;
            ArmorSlot selectedSlot = null;
            float lowestDurability = Float.POSITIVE_INFINITY;
            for (ArmorSlot slot : ArmorSlot.ALL) {
                if ((visible & slot.bit()) != 0) {
                    if ((icons & slot.bit()) != 0) {
                        SlotView view = data[slot.ordinal()];
                        float durability = view.maximum > 0 ? (float) view.remaining / view.maximum : 1.0f;
                        if (selectedSlot == null || (view.warning.critical() && !data[selectedSlot.ordinal()].warning.critical())
                                || (view.warning.critical() == data[selectedSlot.ordinal()].warning.critical()
                                && durability < lowestDurability)) {
                            selectedIndex = index;
                            selectedSlot = slot;
                            lowestDurability = durability;
                        }
                    }
                    index++;
                }
            }
            if (selectedSlot != null) {
                int x = slotX(options, selectedIndex);
                int y = slotY(options, selectedIndex);
                drawWarningIcon(graphics, x + (slotWidth - 8) / 2, y);
            }
        }
        index = 0;
        for (ArmorSlot slot : ArmorSlot.ALL) {
            if ((visible & slot.bit()) != 0) {
                int x = slotX(options, index);
                int y = slotY(options, index);
                SlotView view = data[slot.ordinal()];
                if (options.showBar() && view.barVisible) {
                    drawItemBar(graphics, view, x + itemInsetX, y + itemInsetY);
                }
                drawContents(graphics, client.font, options, appearance, view, x, y, slotWidth, slotHeight, ghost);
                index++;
            }
        }
    }

    private void drawHotbar(GuiGraphics graphics, int slots, boolean vertical) {
        if (vertical) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(HOTBAR_THICKNESS, 0);
            graphics.pose().rotate((float) Math.toRadians(90));
        }
        // The 1.21.4 hotbar sprite has two end pixels around nine cells of 20 GUI pixels.
        // Crop whole cells and keep the original right cap, so cell borders stay at their native pitch.
        int bodyWidth = hotbarLength(slots) - 1;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, HOTBAR_TEXTURE_WIDTH, HOTBAR_THICKNESS,
                0, 0, 0, 0, bodyWidth, HOTBAR_THICKNESS);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, HOTBAR_TEXTURE_WIDTH, HOTBAR_THICKNESS,
                HOTBAR_TEXTURE_WIDTH - 1, 0, bodyWidth, 0, 1, HOTBAR_THICKNESS);
        if (vertical) {
            graphics.pose().popMatrix();
        }
    }

    private void drawShapes(GuiGraphics graphics, ArmorSettings options, ModuleAppearance appearance,
                            SlotView slot, int x, int y, int width, int height, boolean ghost, boolean flash) {
        boolean empty = slot.empty();
        int background = appearance.background(ghost || empty);
        int border = flash ? (slot.warning.critical() ? 0xFFFF3039 : 0xFFDF5558)
                : appearance.outlineEnabled() ? appearance.outline(ghost || empty) : 0;
        switch (options.style()) {
            case CURRENT -> GlassRenderer.rounded(graphics, x, y, width, height,
                    appearance.cornerRadius(), appearance.cornerRadius(), appearance.cornerRadius(), appearance.cornerRadius(),
                    appearance.outlineEnabled() || flash ? 1 : 0, background, border);
            case JOINED -> {
                if (flash) {
                    GlassRenderer.rounded(graphics, x + 1, y + 1, width - 2, height - 2,
                            3, 3, 3, 3, 1, 0, border);
                }
            }
            case VANILLA -> {
                if (flash || appearance.outlineEnabled()) {
                    graphics.renderOutline(x, y, width, height,
                            flash ? border : appearance.outline(ghost || slot.empty()));
                }
            }
        }
    }

    private void drawItemBar(GuiGraphics graphics, SlotView slot, int itemX, int itemY) {
        // Match GuiGraphics#renderItemBar in 1.21.4 with the cached vanilla ItemStack bar properties.
        int x = itemX + 2;
        int y = itemY + 13;
        graphics.fill(RenderPipelines.GUI, x, y, x + 13, y + 2, 0xFF000000);
        graphics.fill(RenderPipelines.GUI, x, y, x + slot.barWidth, y + 1, 0xFF000000 | slot.barColor);
    }

    private void drawWarningIcon(GuiGraphics graphics, int x, int slotY) {
        // Only the most urgent low-durability slot gets a marker. The icon bobs while the active HUD is rendered.
        int bob = (int) Math.round((1.0 + Math.sin(System.nanoTime() / 110_000_000.0)) * 1.5);
        graphics.blit(RenderPipelines.GUI_TEXTURED, WARNING_ICON, x, slotY - 8 - bob, 0.0f, 0.0f, 8, 8, 8, 8);
    }

    private void drawContents(GuiGraphics graphics, Font font, ArmorSettings options, ModuleAppearance appearance,
                              SlotView slot, int x, int y, int width, int height, boolean ghost) {
        if (slot.empty()) {
            return;
        }
        if (options.showCount() && slot.count != null) {
            drawSmall(graphics, font, slot.count, slot.countWidth, x + width - 2, y + 12,
                    TEXT_SCALE, 1, appearance, ghost);
        }
        if (slot.maximum > 0 && hasValues(options)) {
            boolean sideLabel = options.vanilla() && options.vertical();
            boolean numeric = options.valueMode() == ArmorSettings.ValueMode.NUMERIC;
            int labelWidth = numeric ? slot.numericWidth : slot.percentWidth;
            int available = (sideLabel ? VALUE_COLUMN_WIDTH : width) - 2;
            float scale = Math.min(TEXT_SCALE, available / (float) Math.max(1, labelWidth));
            int labelX = sideLabel ? x + width + VALUE_COLUMN_WIDTH / 2 : x + width / 2;
            int labelY = sideLabel ? y + (height - Math.round(font.lineHeight * scale)) / 2 : y + height + 1;
            drawSmall(graphics, font, numeric ? slot.numeric : slot.percent, labelWidth, labelX, labelY,
                    scale, 0.5f, appearance, ghost);
        }
    }

    private void drawSmall(GuiGraphics graphics, Font font, Component label, int labelWidth, int x, int y,
                           float scale, float alignment, ModuleAppearance appearance, boolean ghost) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x - labelWidth * scale * alignment, y);
        graphics.pose().scale(scale, scale);
        graphics.drawString(font, label, 0, 0, appearance.text(ghost), appearance.textShadow());
        graphics.pose().popMatrix();
    }

    private SlotView[] examples(Font font, ArmorWarningSettings warning) {
        Language currentLanguage = Language.getInstance();
        if (examples == null) {
            ItemStack[] items = {
                    sample(Items.DIAMOND_SWORD, 1200), new ItemStack(Items.TORCH, 32),
                    sample(Items.GOLDEN_HELMET, 3), sample(Items.ELYTRA, 1), ItemStack.EMPTY,
                    sample(Items.NETHERITE_BOOTS, 290)
            };
            examples = new SlotView[items.length];
            for (int i = 0; i < items.length; i++) {
                examples[i] = new SlotView();
                examples[i].update(items[i], font);
            }
        } else if (exampleLanguage != currentLanguage) {
            for (SlotView view : examples) {
                view.update(view.item, font);
            }
        }
        exampleLanguage = currentLanguage;
        int threshold = warning.enabled() ? warning.threshold() : 0;
        if (exampleThreshold != threshold) {
            exampleThreshold = threshold;
            exampleLowMask = 0;
            for (ArmorSlot slot : ArmorSlot.ALL) {
                SlotView view = examples[slot.ordinal()];
                view.warning.observe(view.remaining, view.maximum, threshold, true, false, 0);
                if (view.warning.low()) {
                    exampleLowMask |= slot.bit();
                }
            }
        }
        return examples;
    }

    private static ItemStack sample(Item item, int remaining) {
        ItemStack stack = new ItemStack(item);
        stack.setDamageValue(Math.max(0, stack.getMaxDamage() - remaining));
        return stack;
    }

    private static final class SlotView {
        private ItemStack item;
        private int maximum;
        private int remaining;
        private boolean barVisible;
        private int barWidth;
        private int barColor;
        private Component count;
        private int countWidth;
        private Component numeric;
        private int numericWidth;
        private Component percent;
        private int percentWidth;
        private Language language;
        private int displayedCount;
        private final LowDurabilityWarning warning = new LowDurabilityWarning();

        boolean empty() {
            return item == null || item.isEmpty();
        }

        void update(ItemStack stack, Font font) {
            Language currentLanguage = Language.getInstance();
            if (item != null && ItemStack.matches(item, stack) && currentLanguage == language) {
                return;
            }
            boolean languageChanged = language != currentLanguage;
            language = currentLanguage;
            item = stack.copy();
            int max = stack.isDamageableItem() ? stack.getMaxDamage() : 0;
            int left = Math.clamp(max - stack.getDamageValue(), 0, max);
            barVisible = max > 0 && stack.isBarVisible();
            if (barVisible) {
                barWidth = stack.getBarWidth();
                barColor = stack.getBarColor();
            }
            if (max != maximum || left != remaining || languageChanged) {
                maximum = max;
                remaining = left;
                if (max > 0) {
                    float fraction = left / (float) max;
                    numeric = Component.translatable("etherhud.armor.value.numeric", left, max);
                    percent = Component.translatable("etherhud.advanced.percent", Math.round(fraction * 100));
                } else {
                    numeric = null;
                    percent = null;
                }
            }
            int nextCount = !stack.isEmpty() && stack.getMaxStackSize() > 1 ? stack.getCount() : 0;
            if (nextCount != displayedCount || languageChanged) {
                displayedCount = nextCount;
                count = nextCount == 0 ? null : Component.translatable("etherhud.armor.value.count", nextCount);
                countWidth = count == null ? 0 : font.width(count);
            }
            if (maximum > 0) {
                numericWidth = font.width(numeric);
                if (numericWidth * TEXT_SCALE > SLOT_SIZE - 2) {
                    numeric = Component.translatable("etherhud.armor.value.numeric", abbreviated(remaining), abbreviated(maximum));
                    numericWidth = font.width(numeric);
                }
                percentWidth = font.width(percent);
            }
        }

        private static Object abbreviated(int value) {
            if (value < 1000) {
                return value;
            }
            boolean millions = value >= 1_000_000;
            long tenths = Math.round(value / (millions ? 100_000d : 100d));
            return Component.translatable(millions ? "etherhud.armor.value.millions" : "etherhud.armor.value.thousands",
                    tenths / 10 + "." + tenths % 10);
        }
    }
}
