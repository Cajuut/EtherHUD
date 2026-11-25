package io.github.etherhud.mixin;

import io.github.etherhud.EtherHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void etherhud$recordPress(long window, MouseButtonInfo buttonInfo, @MouseButtonInfo.Action int action, CallbackInfo callback) {
        if (window == Minecraft.getInstance().getWindow().handle() && action == GLFW.GLFW_PRESS
                && (buttonInfo.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT || buttonInfo.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
            EtherHud hud = EtherHud.instance();
            if (hud != null) {
                hud.recordClick(buttonInfo.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            }
        }
    }
}
