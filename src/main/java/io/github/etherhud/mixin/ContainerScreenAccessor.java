package io.github.etherhud.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
    @Accessor("leftPos")
    int etherhud$left();

    @Accessor("topPos")
    int etherhud$top();

    @Accessor("imageWidth")
    int etherhud$imageWidth();

    @Accessor("imageHeight")
    int etherhud$imageHeight();
}
