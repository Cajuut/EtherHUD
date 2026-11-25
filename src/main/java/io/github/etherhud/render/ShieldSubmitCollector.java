package io.github.etherhud.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Swaps opaque entity render types to the translucent pipeline and scales the tint alpha,
 * reproducing the shield opacity setting on the 1.21.11 submit-node pipeline.
 */
public final class ShieldSubmitCollector {
    private ShieldSubmitCollector() {}

    public static SubmitNodeCollector wrap(SubmitNodeCollector delegate, int opacityPercent) {
        return new Collector(delegate, Math.clamp(opacityPercent, 0, 100) / 100.0f);
    }

    private static int tinted(int color, float alpha) {
        if (color == -1) {
            return ((int) Math.round(alpha * 255) << 24) | 0x00FFFFFF;
        }
        int scaled = Math.round(((color >>> 24) & 0xFF) * alpha);
        return (scaled << 24) | (color & 0x00FFFFFF);
    }

    private static RenderType translucent(RenderType type, @Nullable TextureAtlasSprite sprite) {
        if (sprite == null) {
            return type;
        }
        var pipeline = type.pipeline();
        if (pipeline == RenderPipelines.ENTITY_SOLID || pipeline == RenderPipelines.ENTITY_SOLID_Z_OFFSET_FORWARD
                || pipeline == RenderPipelines.ENTITY_CUTOUT || pipeline == RenderPipelines.ENTITY_CUTOUT_NO_CULL
                || pipeline == RenderPipelines.ENTITY_TRANSLUCENT) {
            return RenderTypes.entityTranslucent(sprite.atlasLocation());
        }
        return type;
    }

    private static final class Collector extends OrderedCollector implements SubmitNodeCollector {
        Collector(SubmitNodeCollector delegate, float alpha) {
            super(delegate, alpha);
        }

        @Override
        public OrderedSubmitNodeCollector order(int order) {
            return new OrderedCollector(((SubmitNodeCollector) delegate).order(order), alpha);
        }
    }

    private static class OrderedCollector implements OrderedSubmitNodeCollector {
        protected final OrderedSubmitNodeCollector delegate;
        protected final float alpha;

        OrderedCollector(OrderedSubmitNodeCollector delegate, float alpha) {
            this.delegate = delegate;
            this.alpha = alpha;
        }

        @Override
        public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {
            delegate.submitShadow(poseStack, radius, pieces);
        }

        @Override
        public void submitNameTag(PoseStack poseStack, @Nullable Vec3 offset, int light, Component text,
                                  boolean seeThrough, int lineWidth, double distance, CameraRenderState camera) {
            delegate.submitNameTag(poseStack, offset, light, text, seeThrough, lineWidth, distance, camera);
        }

        @Override
        public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence text, boolean shadow,
                               Font.DisplayMode displayMode, int light, int color, int backgroundColor, int outlineColor) {
            delegate.submitText(poseStack, x, y, text, shadow, displayMode, light, color, backgroundColor, outlineColor);
        }

        @Override
        public void submitFlame(PoseStack poseStack, EntityRenderState state, Quaternionf rotation) {
            delegate.submitFlame(poseStack, state, rotation);
        }

        @Override
        public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leash) {
            delegate.submitLeash(poseStack, leash);
        }

        @Override
        public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType,
                                    int light, int overlay, int tintedColor, @Nullable TextureAtlasSprite sprite,
                                    int outlineColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
            delegate.submitModel(model, state, poseStack, translucent(renderType, sprite), light, overlay,
                    tinted(tintedColor, alpha), sprite, outlineColor, crumbling);
        }

        @Override
        public void submitModelPart(ModelPart part, PoseStack poseStack, RenderType renderType, int light, int overlay,
                                    @Nullable TextureAtlasSprite sprite, boolean sheeted, boolean hasFoil, int tintedColor,
                                    ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling, int outlineColor) {
            delegate.submitModelPart(part, poseStack, translucent(renderType, sprite), light, overlay, sprite,
                    sheeted, hasFoil, tinted(tintedColor, alpha), crumbling, outlineColor);
        }

        @Override
        public void submitBlock(PoseStack poseStack, BlockState state, int light, int overlay, int outlineColor) {
            delegate.submitBlock(poseStack, state, light, overlay, outlineColor);
        }

        @Override
        public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState state) {
            delegate.submitMovingBlock(poseStack, state);
        }

        @Override
        public void submitBlockModel(PoseStack poseStack, RenderType renderType, BlockStateModel model,
                                     float r, float g, float b, int light, int overlay, int outlineColor) {
            delegate.submitBlockModel(poseStack, renderType, model, r, g, b, light, overlay, outlineColor);
        }

        @Override
        public void submitItem(PoseStack poseStack, ItemDisplayContext context, int light, int overlay, int outlineColor,
                               int[] tintLayers, List<BakedQuad> quads, RenderType renderType,
                               ItemStackRenderState.FoilType foilType) {
            delegate.submitItem(poseStack, context, light, overlay, outlineColor, tintLayers, quads,
                    translucent(renderType, null), foilType);
        }

        @Override
        public void submitCustomGeometry(PoseStack poseStack, RenderType renderType,
                                         SubmitNodeCollector.CustomGeometryRenderer renderer) {
            delegate.submitCustomGeometry(poseStack, renderType, renderer);
        }

        @Override
        public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer renderer) {
            delegate.submitParticleGroup(renderer);
        }
    }
}
