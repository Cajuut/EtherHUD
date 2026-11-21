package io.github.etherhud.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.etherhud.EtherHud;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
abstract class ScreenEffectRendererMixin {
    @Inject(method = "renderFire", at = @At("HEAD"))
    private static void etherhud$beginFireOffset(PoseStack poseStack, MultiBufferSource buffers, CallbackInfo ci) {
        EtherHud hud = EtherHud.instance();
        if (hud != null) {
            poseStack.pushPose();
            poseStack.translate(0, hud.settings().overlays().fireOffsetY() / 100.0, 0);
        }
    }

    @Inject(method = "renderFire", at = @At("RETURN"))
    private static void etherhud$endFireOffset(PoseStack poseStack, MultiBufferSource buffers, CallbackInfo ci) {
        if (EtherHud.instance() != null) poseStack.popPose();
    }
}
