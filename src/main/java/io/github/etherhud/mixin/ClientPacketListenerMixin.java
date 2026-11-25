package io.github.etherhud.mixin;

import io.github.etherhud.EtherHud;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {
    // Minecraft 1.21.4 synchronizes server game time every 20 server ticks. RETURN runs on the client thread.
    @Inject(method = "handleSetTime", at = @At("RETURN"))
    private void etherhud$sampleServerTime(ClientboundSetTimePacket packet, CallbackInfo ci) {
        EtherHud hud = EtherHud.instance();
        if (hud != null) {
            hud.recordServerTime(packet.gameTime());
        }
    }
}
