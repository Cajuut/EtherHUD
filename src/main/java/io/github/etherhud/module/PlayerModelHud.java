package io.github.etherhud.module;

import io.github.etherhud.config.HudSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class PlayerModelHud implements HudModule {
    private static final int WIDTH = 54;
    private static final int HEIGHT = 66;
    private final Vector3f modelOffset = new Vector3f();
    private final Quaternionf rotation = new Quaternionf();

    @Override
    public void update(Minecraft client, HudSettings settings) {}

    @Override
    public int panelWidth(Font font, Component label, HudSettings settings, boolean preview) { return WIDTH; }

    @Override
    public int panelHeight(HudSettings settings, boolean preview) { return HEIGHT; }

    @Override
    public void draw(Minecraft client, GuiGraphics graphics, HudSettings settings, ModuleId id, Component label,
                     int width, int height, boolean preview, boolean ghost) {
        var appearance = settings.appearance(id);
        HudModule.drawPanel(graphics, width, height, appearance, ghost);

        var player = client.player;
        if (player == null) {
            if (preview) {
                graphics.drawCenteredString(client.font, id.label(), width / 2, (height - client.font.lineHeight) / 2,
                        appearance.text(ghost));
            }
            return;
        }

        // Match 1.21.4's inventory projection and lighting. Adjust the view instead of mutating the live entity.
        float yaw = 32 * Mth.sin(player.getYRot() * Mth.DEG_TO_RAD);
        float pitch = Mth.clamp(player.getXRot() * 0.16f, -12, 12);
        rotation.rotationZ(Mth.PI).rotateX(pitch * Mth.DEG_TO_RAD)
                .rotateY((player.yBodyRot - 180 + yaw) * Mth.DEG_TO_RAD);
        modelOffset.set(0, player.getBbHeight() / 2f, 0);
        graphics.enableScissor(2, 2, width - 2, height - 2);
        try {
            InventoryScreen.renderEntityInInventory(graphics, width / 2f, height / 2f + 4,
                    28 / player.getScale(), modelOffset, rotation, null, player);
        } finally {
            graphics.disableScissor();
        }
    }
}
