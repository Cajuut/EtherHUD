package io.github.etherhud.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.etherhud.EtherHud;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Shadow private ItemStack itemActivationItem;
    @Shadow private int itemActivationTicks;
    @Unique private int etherhud$totemDuration = GameRenderer.ITEM_ACTIVATION_ANIMATION_LENGTH;

    @Inject(method = "displayItemActivation", at = @At("RETURN"))
    private void etherhud$setTotemDuration(ItemStack stack, CallbackInfo ci) {
        EtherHud hud = EtherHud.instance();
        if (hud != null && stack != null && stack.is(Items.TOTEM_OF_UNDYING)) {
            int speed = hud.settings().overlays().totemSpeed();
            etherhud$totemDuration = Math.round(GameRenderer.ITEM_ACTIVATION_ANIMATION_LENGTH * 100f / speed);
            itemActivationTicks = etherhud$totemDuration;
        }
    }

    // Minecraft 1.21.4 computes progress as (40 - remainingTicks + partialTick) / 40.
    @ModifyConstant(method = "renderItemActivationAnimation", constant = @Constant(intValue = 40))
    private int etherhud$totemProgressStart(int vanillaDuration) {
        return itemActivationItem != null && itemActivationItem.is(Items.TOTEM_OF_UNDYING)
                ? etherhud$totemDuration : vanillaDuration;
    }

    @ModifyConstant(method = "renderItemActivationAnimation", constant = @Constant(floatValue = 40f))
    private float etherhud$totemProgressDivisor(float vanillaDuration) {
        return etherhud$totemProgressStart((int) vanillaDuration);
    }

    @Inject(method = "renderItemActivationAnimation", at = @At("HEAD"))
    private void etherhud$beginTotemTransform(GuiGraphics graphics, float partialTick, CallbackInfo ci) {
        EtherHud hud = EtherHud.instance();
        if (hud == null || itemActivationItem == null || !itemActivationItem.is(Items.TOTEM_OF_UNDYING)) return;
        var settings = hud.settings().overlays();
        PoseStack pose = graphics.pose();
        float centerX = graphics.guiWidth() / 2.0f;
        float centerY = graphics.guiHeight() / 2.0f + settings.totemOffsetY();
        pose.pushPose();
        pose.translate(centerX, centerY, 0);
        pose.scale(settings.totemScale(), settings.totemScale(), 1);
        pose.translate(-centerX, -centerY, 0);
    }

    @Inject(method = "renderItemActivationAnimation", at = @At("RETURN"))
    private void etherhud$endTotemTransform(GuiGraphics graphics, float partialTick, CallbackInfo ci) {
        EtherHud hud = EtherHud.instance();
        if (hud != null && itemActivationItem != null && itemActivationItem.is(Items.TOTEM_OF_UNDYING)) {
            graphics.pose().popPose();
        }
    }
}
