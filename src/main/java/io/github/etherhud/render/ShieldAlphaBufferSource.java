package io.github.etherhud.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.etherhud.mixin.RenderTypeAccessor;
import io.github.etherhud.mixin.TextureStateShardAccessor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Changes textured shield layers to 1.21.4's translucent entity pipeline and scales vertex alpha. */
public final class ShieldAlphaBufferSource implements MultiBufferSource {
    private static final Map<RenderType, RenderType> TRANSLUCENT_TYPES = new HashMap<>();
    private final MultiBufferSource delegate;
    private final float alpha;

    public ShieldAlphaBufferSource(MultiBufferSource delegate, int opacityPercent) {
        this.delegate = delegate;
        this.alpha = Math.clamp(opacityPercent, 0, 100) / 100.0f;
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        return new AlphaVertexConsumer(delegate.getBuffer(translucent(type)), alpha);
    }

    private static RenderType translucent(RenderType type) {
        if (!(type instanceof RenderType.CompositeRenderType composite)) return type;
        try {
            RenderStateShard.EmptyTextureStateShard textureState = ((RenderTypeAccessor) (Object) composite).etherhud$textureState();
            Optional<ResourceLocation> texture = ((TextureStateShardAccessor) textureState).etherhud$texture();
            return texture.map(location -> TRANSLUCENT_TYPES.computeIfAbsent(type, ignored -> RenderType.entityTranslucent(location)))
                    .orElse(type);
        } catch (RuntimeException exception) {
            return type;
        }
    }

    private record AlphaVertexConsumer(VertexConsumer delegate, float alpha) implements VertexConsumer {
        @Override public VertexConsumer addVertex(float x, float y, float z) { delegate.addVertex(x, y, z); return this; }
        @Override public VertexConsumer setColor(int red, int green, int blue, int value) {
            delegate.setColor(red, green, blue, Math.round(value * alpha)); return this;
        }
        @Override public VertexConsumer setUv(float u, float v) { delegate.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { delegate.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { delegate.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { delegate.setNormal(x, y, z); return this; }
    }
}
