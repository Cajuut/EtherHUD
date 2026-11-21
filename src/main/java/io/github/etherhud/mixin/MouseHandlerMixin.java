package io.github.etherhud.mixin;

import io.github.etherhud.EtherHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
    @Inject(method = "onPress", at = @At("HEAD"))
    private void etherhud$recordPress(long window, int button, int action, int modifiers, CallbackInfo callback) {
        if (window == Minecraft.getInstance().getWindow().getWindow() && action == GLFW.GLFW_PRESS
                && (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
            EtherHud hud = EtherHud.instance();
            if (hud != null) {
                hud.recordClick(button == GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            }
        }
    }
}
