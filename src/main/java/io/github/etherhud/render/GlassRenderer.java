package io.github.etherhud.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.etherhud.mixin.GuiGraphicsAccessor;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GlassRenderer {
    private enum ProgramStatus { UNCHECKED, AVAILABLE, FAILED }

    private static final Logger LOG = LoggerFactory.getLogger("EtherHUD");
    private static final float PADDING = 2;
    // 1.21.4 loads the core JSON by key and links it against a VertexFormat supplied by Java.
    private static final ShaderProgram PROGRAM = new ShaderProgram(
            ResourceLocation.fromNamespaceAndPath("etherhud", "core/glass"), DefaultVertexFormat.NEW_ENTITY, ShaderDefines.EMPTY);
    private static final RenderType LAYER = RenderType.create("etherhud_glass", PROGRAM.vertexFormat(),
            VertexFormat.Mode.QUADS, 8192, RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(PROGRAM))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));
    private static ProgramStatus status = ProgramStatus.UNCHECKED;
    private static boolean enabled = true;

    private GlassRenderer() {}

    public static void registerReloadListener() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return ResourceLocation.fromNamespaceAndPath("etherhud", "glass");
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
                // In 1.21.4 getProgramForLoading throws without triggering vanilla's shader recovery.
                Minecraft.getInstance().getShaderManager().getProgramForLoading(PROGRAM);
                status = ProgramStatus.AVAILABLE;
            } catch (ShaderManager.CompilationException | RuntimeException exception) {
                status = ProgramStatus.FAILED;
                LOG.warn("EtherHUD glass shader unavailable. Using rectangle rendering.", exception);
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

        // 1.21.4 GuiGraphics uses a BufferSource. The shared RenderType batches adjacent shapes
        // and respects vanilla ordering, flush boundaries and scissor changes without per-shape uniforms.
        VertexConsumer vertices = ((GuiGraphicsAccessor) graphics).etherhud$buffers().getBuffer(LAYER);
        Matrix4f pose = graphics.pose().last().pose();
        float halfX = width / 2f + PADDING;
        float halfY = height / 2f + PADDING;
        float cx = x + width / 2f;
        float cy = y + height / 2f;
        int upper = packRadii(topLeft, topRight);
        int lower = packRadii(bottomLeft, bottomRight);
        int rg = (stroke >>> 8) & 0xffff;
        int ba = ((stroke & 0xff) << 8) | (stroke >>> 24);
        vertex(vertices, pose, cx, cy, -halfX, -halfY, fill, upper, lower, rg, ba, border);
        vertex(vertices, pose, cx, cy, -halfX, halfY, fill, upper, lower, rg, ba, border);
        vertex(vertices, pose, cx, cy, halfX, halfY, fill, upper, lower, rg, ba, border);
        vertex(vertices, pose, cx, cy, halfX, -halfY, fill, upper, lower, rg, ba, border);
    }

    private static int packRadii(float left, float right) {
        return Math.round(left * 4) | (Math.round(right * 4) << 8);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f pose, float cx, float cy, float u, float v,
                               int fill, int upper, int lower, int rg, int ba, int border) {
        // NEW_ENTITY supplies position, normalized RGBA, float UV0, signed-short UV1/UV2 and byte Normal.
        vertices.addVertex(pose, cx + u, cy + v, 0).setColor(fill).setUv(u, v)
                .setUv1(upper, lower).setUv2(rg, ba).setNormal(border / 127f, 0, 0);
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
