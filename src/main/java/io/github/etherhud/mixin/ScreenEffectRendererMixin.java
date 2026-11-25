package io.github.etherhud.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.etherhud.EtherHud;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
abstract class ScreenEffectRendererMixin {
    @Shadow private ItemStack itemActivationItem;
    @Shadow private int itemActivationTicks;
    @Unique private int etherhud$totemDuration = ScreenEffectRenderer.ITEM_ACTIVATION_ANIMATION_LENGTH;

    @Inject(method = "renderFire", at = @At("HEAD"))
    private static void etherhud$beginFireOffset(PoseStack poseStack, MultiBufferSource buffers, TextureAtlasSprite sprite, CallbackInfo ci) {
        EtherHud hud = EtherHud.instance();
        if (hud != null) {
            poseStack.pushPose();
            poseStack.translate(0, hud.settings().overlays().fireOffsetY() / 100.0, 0);
        }
    }

    @Inject(method = "renderFire", at = @At("RETURN"))
    private static void etherhud$endFireOffset(PoseStack poseStack, MultiBufferSource buffers, TextureAtlasSprite sprite, CallbackInfo ci) {
        if (EtherHud.instance() != null) poseStack.popPose();
    }

    @Inject(method = "displayItemActivation", at = @At("RETURN"))
    private void etherhud$setTotemDuration(ItemStack stack, RandomSource random, CallbackInfo ci) {
        EtherHud hud = EtherHud.instance();
        if (hud != null && stack != null && stack.is(Items.TOTEM_OF_UNDYING)) {
            int speed = hud.settings().overlays().totemSpeed();
            etherhud$totemDuration = Math.round(ScreenEffectRenderer.ITEM_ACTIVATION_ANIMATION_LENGTH * 100f / speed);
            itemActivationTicks = etherhud$totemDuration;
        }
    }

    // Minecraft computes progress as (40 - remainingTicks + partialTick) / 40.
    @ModifyConstant(method = "renderItemActivationAnimation", constant = @Constant(intValue = 40))
    private int etherhud$totemProgressStart(int vanillaDuration) {
        return etherhud$totemActive() ? etherhud$totemDuration : vanillaDuration;
    }

    @ModifyConstant(method = "renderItemActivationAnimation", constant = @Constant(floatValue = 40f))
    private float etherhud$totemProgressDivisor(float vanillaDuration) {
        return etherhud$totemProgressStart((int) vanillaDuration);
    }

    // The totem is rendered in view space, so scaling the pose around the camera
    // origin would only change its distance, not its apparent size. Scale the
    // item's own 0.8F model scale instead.
    @ModifyConstant(method = "renderItemActivationAnimation", constant = @Constant(floatValue = 0.8f))
    private float etherhud$totemScale(float vanillaScale) {
        EtherHud hud = EtherHud.instance();
        return hud != null && etherhud$totemActive()
                ? vanillaScale * hud.settings().overlays().totemScale() : vanillaScale;
    }

    @Inject(method = "renderItemActivationAnimation", at = @At("HEAD"))
    private void etherhud$beginTotemTransform(PoseStack poseStack, float partialTick, SubmitNodeCollector collector, CallbackInfo ci) {
        EtherHud hud = EtherHud.instance();
        if (hud == null || !etherhud$totemActive()) return;
        var settings = hud.settings().overlays();
        poseStack.pushPose();
        poseStack.translate(0, -settings.totemOffsetY() * 0.03, 0);
    }

    @Inject(method = "renderItemActivationAnimation", at = @At("RETURN"))
    private void etherhud$endTotemTransform(PoseStack poseStack, float partialTick, SubmitNodeCollector collector, CallbackInfo ci) {
        if (EtherHud.instance() != null && etherhud$totemActive()) {
            poseStack.popPose();
        }
    }

    @Unique
    private boolean etherhud$totemActive() {
        return itemActivationItem != null && itemActivationItem.is(Items.TOTEM_OF_UNDYING);
    }
}
