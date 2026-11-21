package io.github.etherhud.render;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.DelegateBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class WorldFireModel extends DelegateBakedModel {
    private static final ResourceLocation FIRE = ResourceLocation.withDefaultNamespace("fire");
    private static final ResourceLocation SOUL_FIRE = ResourceLocation.withDefaultNamespace("soul_fire");
    private static volatile float scale = 1;

    private WorldFireModel(BakedModel model) {
        super(model);
    }

    public static void register(float initialScale) {
        scale = initialScale;
        // Fabric's model loading API for 1.21.4 replaces the model before either chunk renderer consumes it.
        ModelLoadingPlugin.register(plugin -> plugin.modifyBlockModelAfterBake().register(ModelModifier.WRAP_LAST_PHASE,
                (model, context) -> {
                    ResourceLocation id = context.id().id();
                    return FIRE.equals(id) || SOUL_FIRE.equals(id) ? new WorldFireModel(model) : model;
                }));
    }

    public static void applyScale(float value, Minecraft client) {
        if (scale != value) {
            scale = value;
            if (client.level != null) {
                client.levelRenderer.allChanged();
            }
        }
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction face, RandomSource random) {
        List<BakedQuad> quads = parent.getQuads(state, face, random);
        float current = scale;
        if (current == 1 || quads.isEmpty()) {
            return quads;
        }
        return quads.stream().map(quad -> scaleQuad(quad, current)).toList();
    }

    @Override
    public void emitBlockQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockState state, BlockPos pos,
                               Supplier<RandomSource> random, Predicate<Direction> cullTest) {
        float current = scale;
        if (current == 1) {
            parent.emitBlockQuads(emitter, level, state, pos, random, cullTest);
            return;
        }
        emitter.pushTransform(quad -> {
            for (int vertex = 0; vertex < 4; vertex++) {
                quad.pos(vertex, scaleFromCenter(quad.x(vertex), current), quad.y(vertex) * current,
                        scaleFromCenter(quad.z(vertex), current));
            }
            return true;
        });
        try {
            parent.emitBlockQuads(emitter, level, state, pos, random, cullTest);
        } finally {
            emitter.popTransform();
        }
    }

    private static BakedQuad scaleQuad(BakedQuad quad, float factor) {
        // In 1.21.4 the first three packed ints of each vertex are its XYZ floats.
        int[] vertices = quad.getVertices().clone();
        int stride = vertices.length / 4;
        for (int index = 0; index < vertices.length; index += stride) {
            vertices[index] = Float.floatToRawIntBits(scaleFromCenter(Float.intBitsToFloat(vertices[index]), factor));
            vertices[index + 1] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[index + 1]) * factor);
            vertices[index + 2] = Float.floatToRawIntBits(scaleFromCenter(Float.intBitsToFloat(vertices[index + 2]), factor));
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), quad.getSprite(),
                quad.isShade(), quad.getLightEmission());
    }

    private static float scaleFromCenter(float value, float factor) {
        return 0.5f + (value - 0.5f) * factor;
    }
}
