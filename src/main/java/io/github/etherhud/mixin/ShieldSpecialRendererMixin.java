package io.github.etherhud.mixin;

import io.github.etherhud.EtherHud;
import io.github.etherhud.render.ShieldAlphaBufferSource;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.special.ShieldSpecialRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ShieldSpecialRenderer.class)
abstract class ShieldSpecialRendererMixin {
    @ModifyVariable(method = "render(Lnet/minecraft/core/component/DataComponentMap;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IIZ)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private MultiBufferSource etherhud$applyShieldOpacity(MultiBufferSource buffers) {
        EtherHud hud = EtherHud.instance();
        int opacity = hud == null ? 100 : hud.settings().overlays().shieldOpacity();
        return opacity < 100 ? new ShieldAlphaBufferSource(buffers, opacity) : buffers;
    }

    @Redirect(method = "render(Lnet/minecraft/core/component/DataComponentMap;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IIZ)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ShieldModel;renderType(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"))
    private RenderType etherhud$useBlendedShieldType(ShieldModel model, ResourceLocation texture) {
        EtherHud hud = EtherHud.instance();
        if (hud != null && hud.settings().overlays().shieldOpacity() < 100) {
            return RenderType.entityTranslucent(texture);
        }
        return model.renderType(texture);
    }
}
