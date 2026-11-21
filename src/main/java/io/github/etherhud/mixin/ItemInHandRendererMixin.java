package io.github.etherhud.mixin;

import io.github.etherhud.EtherHud;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
abstract class ItemInHandRendererMixin {
    @Inject(method = "renderItem", at = @At("HEAD"))
    private void etherhud$beginHandTransform(LivingEntity entity, ItemStack item, ItemDisplayContext context,
                                              boolean leftHand, PoseStack poseStack, MultiBufferSource buffers,
                                              int packedLight, CallbackInfo ci) {
        EtherHud hud = EtherHud.instance();
        poseStack.pushPose();
        if (hud != null) {
            var settings = hud.settings().overlays();
            if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
                boolean leftSide = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
                boolean mainHand = leftSide == (entity.getMainArm() == HumanoidArm.LEFT);
                float scale = mainHand ? settings.mainHandScale() : settings.offHandScale();
                int offset = mainHand ? settings.mainHandOffsetY() : settings.offHandOffsetY();
                poseStack.translate(0, offset / 100.0, 0);
                poseStack.scale(scale, scale, scale);
            }
        }
    }

    @Inject(method = "renderItem", at = @At("RETURN"))
    private void etherhud$endHandTransform(LivingEntity entity, ItemStack item, ItemDisplayContext context,
                                            boolean leftHand, PoseStack poseStack, MultiBufferSource buffers,
                                            int packedLight, CallbackInfo ci) {
        poseStack.popPose();
    }

}
