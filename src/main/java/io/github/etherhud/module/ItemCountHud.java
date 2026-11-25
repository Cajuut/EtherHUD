package io.github.etherhud.module;

import io.github.etherhud.config.HudSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class ItemCountHud implements HudModule {
    private static final int ICON_X = 5;
    private static final int LABEL_X = 26;
    private static final Component EXAMPLE = Component.literal("148");
    private ItemStack exampleStack;
    private ItemStack held = ItemStack.EMPTY;
    private Component live = Component.empty();
    private int remainingTicks;
    private int total;

    @Override
    public void update(Minecraft client, HudSettings settings) {
        ItemStack hand = client.player.getMainHandItem();
        boolean changed = !ItemStack.matches(held, hand);
        if (changed) {
            held = hand.copy();
        }
        if (hand.isEmpty()) {
            total = 0;
            return;
        }
        if (!changed && remainingTicks-- > 0) {
            return;
        }
        remainingTicks = 4;
        var inventory = client.player.getInventory();
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && ItemStack.isSameItem(hand, stack)) {
                count += stack.getCount();
            }
        }
        if (count != total) {
            total = count;
            live = Component.literal(Integer.toString(total));
        }
    }

    @Override
    public void onInactive() {
        held = ItemStack.EMPTY;
        total = 0;
        remainingTicks = 0;
    }

    @Override
    public Component liveText() { return live; }

    @Override
    public Component previewText() { return EXAMPLE; }

    @Override
    public int layoutVersion(HudSettings settings, boolean preview) {
        return preview ? 0 : total;
    }

    @Override
    public int panelWidth(Font font, Component label, HudSettings settings, boolean preview) {
        return preview || total > 0 ? Math.max(48, LABEL_X + font.width(label) + 6) : 0;
    }

    @Override
    public int panelHeight(HudSettings settings, boolean preview) { return 22; }

    @Override
    public void draw(Minecraft client, GuiGraphics graphics, HudSettings settings, ModuleId id, Component label,
                     int width, int height, boolean preview, boolean ghost) {
        if (preview && exampleStack == null) {
            exampleStack = new ItemStack(Items.TORCH);
        }
        var appearance = settings.appearance(id);
        HudModule.drawPanel(graphics, width, height, appearance, ghost);
        graphics.renderItem(preview ? exampleStack : held, ICON_X, (height - 16) / 2);
        graphics.drawString(client.font, label, LABEL_X, (height - client.font.lineHeight) / 2,
                appearance.text(ghost), appearance.textShadow());
    }
}
