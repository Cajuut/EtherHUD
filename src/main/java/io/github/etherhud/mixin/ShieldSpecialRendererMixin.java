package io.github.etherhud.mixin;

import io.github.etherhud.EtherHud;
import io.github.etherhud.render.ShieldSubmitCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.ShieldSpecialRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ShieldSpecialRenderer.class)
abstract class ShieldSpecialRendererMixin {
    @ModifyVariable(method = "submit", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private SubmitNodeCollector etherhud$applyShieldOpacity(SubmitNodeCollector collector) {
        EtherHud hud = EtherHud.instance();
        int opacity = hud == null ? 100 : hud.settings().overlays().shieldOpacity();
        return opacity < 100 ? ShieldSubmitCollector.wrap(collector, opacity) : collector;
    }
}
