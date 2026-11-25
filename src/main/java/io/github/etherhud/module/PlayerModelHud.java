package io.github.etherhud.module;

import io.github.etherhud.config.HudSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class PlayerModelHud implements HudModule {
    private static final int WIDTH = 54;
    private static final int HEIGHT = 66;

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
        float yawDegrees = player.yBodyRot - 180 + yaw;
        EntityRenderState state = extractRenderState(player);
        if (state instanceof LivingEntityRenderState living) {
            living.bodyRot = 180.0F + yawDegrees;
            living.yRot = yawDegrees;
            living.xRot = living.pose == Pose.FALL_FLYING ? 0.0F : -pitch;
            living.boundingBoxWidth = living.boundingBoxWidth / living.scale;
            living.boundingBoxHeight = living.boundingBoxHeight / living.scale;
            living.scale = 1.0F;
        }
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI).rotateX(pitch * Mth.DEG_TO_RAD);
        Vector3f offset = new Vector3f(0.0F, state.boundingBoxHeight / 2f, 0.0F);
        graphics.enableScissor(2, 2, width - 2, height - 2);
        try {
            graphics.submitEntityRenderState(state, 28 / player.getScale(), offset, rotation, null,
                    0, 0, width, height);
        } finally {
            graphics.disableScissor();
        }
    }

    private static EntityRenderState extractRenderState(LivingEntity livingEntity) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderer<? super LivingEntity, ?> renderer = dispatcher.getRenderer(livingEntity);
        EntityRenderState state = renderer.createRenderState(livingEntity, 1.0F);
        state.lightCoords = 15728880;
        state.shadowPieces.clear();
        state.outlineColor = 0;
        return state;
    }
}
