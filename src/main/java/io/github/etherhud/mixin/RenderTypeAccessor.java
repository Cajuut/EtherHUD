package io.github.etherhud.mixin;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderType.CompositeRenderType.class)
public abstract class RenderTypeAccessor {
    @Invoker("state") public abstract RenderType.CompositeState etherhud$state();

    public RenderStateShard.EmptyTextureStateShard etherhud$textureState() {
        return ((RenderTypeCompositeStateAccessor) (Object) etherhud$state()).etherhud$textureState();
    }
}
