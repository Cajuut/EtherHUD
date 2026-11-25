package io.github.etherhud.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.etherhud.EtherHud;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
abstract class ItemInHandRendererMixin {
    @Inject(method = "renderItem", at = @At("HEAD"))
    private void etherhud$beginHandTransform(LivingEntity entity, ItemStack item, ItemDisplayContext context,
                                              PoseStack poseStack, SubmitNodeCollector collector, int light,
                                              CallbackInfo ci) {
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
                                            PoseStack poseStack, SubmitNodeCollector collector, int light,
                                            CallbackInfo ci) {
        poseStack.popPose();
    }

}
