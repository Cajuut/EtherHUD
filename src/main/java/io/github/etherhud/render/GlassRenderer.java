package io.github.etherhud.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GlassRenderer {
    private enum ProgramStatus { UNCHECKED, AVAILABLE, FAILED }

    private static final Logger LOG = LoggerFactory.getLogger("EtherHUD");
    private static final float PADDING = 2;
    private static final Identifier SHADER_LOCATION = Identifier.fromNamespaceAndPath("etherhud", "core/glass");
    private static final Identifier SHADER_ID = Identifier.fromNamespaceAndPath("etherhud", "glass");
    // Built before RenderPipelines initializes so panels sort under vanilla GUI elements.
    private static final RenderPipeline PIPELINE = RenderPipelines.register(RenderPipeline.builder()
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withLocation(SHADER_ID)
            .withVertexShader(SHADER_LOCATION)
            .withFragmentShader(SHADER_LOCATION)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .build());
    private static ProgramStatus status = ProgramStatus.UNCHECKED;
    private static boolean enabled = true;

    private GlassRenderer() {}

    public static void registerReloadListener() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return SHADER_ID;
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                status = ProgramStatus.UNCHECKED;
            }
        });
    }

    public static void setEnabled(boolean enabled) {
        GlassRenderer.enabled = enabled;
    }

    private static boolean shaderAvailable() {
        if (!enabled) {
            return false;
        }
        if (status == ProgramStatus.UNCHECKED) {
            try {
                var resources = Minecraft.getInstance().getResourceManager();
                status = resources.getResource(Identifier.fromNamespaceAndPath("etherhud", "shaders/core/glass.vsh")).isPresent()
                        && resources.getResource(Identifier.fromNamespaceAndPath("etherhud", "shaders/core/glass.fsh")).isPresent()
                        ? ProgramStatus.AVAILABLE : ProgramStatus.FAILED;
            } catch (RuntimeException exception) {
                status = ProgramStatus.FAILED;
            }
            if (status == ProgramStatus.FAILED) {
                LOG.warn("EtherHUD glass shader unavailable. Using rectangle rendering.");
            }
        }
        return status == ProgramStatus.AVAILABLE;
    }

    public static void rounded(GuiGraphics graphics, int x, int y, int width, int height, float radius, int color) {
        rounded(graphics, x, y, width, height, radius, radius, radius, radius, 0, color, 0);
    }

    public static void circle(GuiGraphics graphics, int x, int y, int diameter, int ringWidth, int fill, int ring) {
        float radius = diameter / 2f;
        rounded(graphics, x, y, diameter, diameter, radius, radius, radius, radius, ringWidth, fill, ring);
    }

    public static void rounded(GuiGraphics graphics, int x, int y, int width, int height,
                               float topLeft, float topRight, float bottomRight, float bottomLeft,
                               int border, int fill, int stroke) {
        if (width <= 0 || height <= 0) {
            return;
        }
        float maximumRadius = Math.min(Math.min(width, height) / 2f, 63.75f);
        topLeft = Math.clamp(topLeft, 0, maximumRadius);
        topRight = Math.clamp(topRight, 0, maximumRadius);
        bottomRight = Math.clamp(bottomRight, 0, maximumRadius);
        bottomLeft = Math.clamp(bottomLeft, 0, maximumRadius);
        border = Math.clamp(border, 0, 15);
        if (!shaderAvailable()) {
            rectangles(graphics, x, y, width, height, topLeft, topRight, bottomRight, bottomLeft, border, fill, stroke);
            return;
        }

        int upper = packRadii(topLeft, topRight);
        int lower = packRadii(bottomLeft, bottomRight);
        int rg = (stroke >>> 8) & 0xffff;
        int ba = ((stroke & 0xff) << 8) | (stroke >>> 24);
        var pose = new Matrix3x2f(graphics.pose());
        int pad = (int) Math.ceil(PADDING);
        ScreenRectangle rect = new ScreenRectangle(x - pad, y - pad, width + pad * 2, height + pad * 2)
                .transformMaxBounds(pose);
        ScreenRectangle scissor = graphics.scissorStack.peek();
        graphics.guiRenderState.submitGuiElement(new GlassQuad(pose, x, y, width, height,
                topLeft, topRight, bottomRight, bottomLeft, border, fill, stroke, upper, lower, rg, ba,
                scissor, scissor != null ? scissor.intersection(rect) : rect));
    }

    private static int packRadii(float left, float right) {
        return Math.round(left * 4) | (Math.round(right * 4) << 8);
    }

    private record GlassQuad(Matrix3x2fc pose, int x, int y, int width, int height,
                             float topLeft, float topRight, float bottomRight, float bottomLeft,
                             int border, int fill, int stroke, int upper, int lower, int rg, int ba,
                             @Nullable ScreenRectangle scissor, @Nullable ScreenRectangle bounds)
            implements GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer consumer) {
            float halfX = width / 2f + PADDING;
            float halfY = height / 2f + PADDING;
            float cx = x + width / 2f;
            float cy = y + height / 2f;
            // NEW_ENTITY supplies position, normalized RGBA, float UV0, signed-short UV1/UV2 and byte Normal.
            vertex(consumer, -halfX, -halfY);
            vertex(consumer, -halfX, halfY);
            vertex(consumer, halfX, halfY);
            vertex(consumer, halfX, -halfY);
        }

        private void vertex(VertexConsumer consumer, float u, float v) {
            consumer.addVertexWith2DPose(pose, x + width / 2f + u, y + height / 2f + v)
                    .setColor(fill).setUv(u, v)
                    .setUv1(upper, lower).setUv2(rg, ba)
                    .setNormal(border / 127f, 0, 0);
        }

        @Override
        public RenderPipeline pipeline() {
            return PIPELINE;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }

        @Override
        public @Nullable ScreenRectangle scissorArea() {
            return scissor;
        }

        @Override
        public @Nullable ScreenRectangle bounds() {
            return bounds;
        }
    }

    private static void rectangles(GuiGraphics graphics, int x, int y, int width, int height,
                                   float tl, float tr, float br, float bl, int border, int fill, int stroke) {
        int top = Math.max(border, (int) Math.ceil(Math.max(tl, tr)));
        int bottom = Math.max(border, (int) Math.ceil(Math.max(bl, br)));
        int row = 0;
        while (row < height) {
            int end = row == top && row < height - bottom ? height - bottom : row + 1;
            boolean upper = row < height / 2f;
            int left = inset(upper ? tl : bl, upper ? row : height - 1 - row);
            int right = width - inset(upper ? tr : br, upper ? row : height - 1 - row);
            if (border == 0) {
                rectangle(graphics, x + left, x + right, y + row, y + end, fill);
            } else if (row < border || row >= height - border || width <= border * 2) {
                rectangle(graphics, x + left, x + right, y + row, y + end, stroke);
            } else {
                int innerRow = upper ? row - border : height - 1 - row - border;
                int innerLeft = Math.min(right, border + inset(Math.max(0, (upper ? tl : bl) - border), innerRow));
                int innerRight = Math.max(innerLeft, width - border
                        - inset(Math.max(0, (upper ? tr : br) - border), innerRow));
                rectangle(graphics, x + left, x + innerLeft, y + row, y + end, stroke);
                rectangle(graphics, x + innerLeft, x + innerRight, y + row, y + end, fill);
                rectangle(graphics, x + innerRight, x + right, y + row, y + end, stroke);
            }
            row = end;
        }
    }

    private static int inset(float radius, int row) {
        if (radius == 3) {
            return Math.max(0, 2 - row); // Match the original EtherHUD panel corners.
        }
        double dy = Math.max(0, radius - row - 0.5);
        return Math.max(0, (int) Math.ceil(radius - Math.sqrt(radius * radius - dy * dy) - 0.5));
    }

    private static void rectangle(GuiGraphics graphics, int left, int right, int top, int bottom, int color) {
        if (left < right && color >>> 24 != 0) {
            graphics.fill(left, top, right, bottom, color);
        }
    }
}
