package io.github.etherhud.render;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

public final class WorldFireModel extends WrapperBlockStateModel {
    private static volatile float scale = 1;

    private WorldFireModel(BlockStateModel model) {
        super(model);
    }

    public static void register(float initialScale) {
        scale = initialScale;
        // Fabric's model loading API replaces the baked model before either chunk renderer consumes it.
        ModelLoadingPlugin.register(context ->
                context.modifyBlockModelAfterBake().register(ModelModifier.WRAP_LAST_PHASE,
                        (model, bakeContext) -> bakeContext.state().is(Blocks.FIRE)
                                || bakeContext.state().is(Blocks.SOUL_FIRE)
                                ? new WorldFireModel(model) : model));
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
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos,
                          BlockState state, RandomSource random, Predicate<Direction> cullTest) {
        float current = scale;
        if (current == 1) {
            wrapped.emitQuads(emitter, level, pos, state, random, cullTest);
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
            wrapped.emitQuads(emitter, level, pos, state, random, cullTest);
        } finally {
            emitter.popTransform();
        }
    }

    private static float scaleFromCenter(float value, float factor) {
        return 0.5f + (value - 0.5f) * factor;
    }
}
