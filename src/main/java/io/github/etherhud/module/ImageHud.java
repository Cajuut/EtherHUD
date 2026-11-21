package io.github.etherhud.module;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.etherhud.config.HudSettings;
import io.github.etherhud.module.settings.CommonModuleSettings;
import io.github.etherhud.module.settings.ModuleSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

final class ImageHud implements HudModule {
    private static final Logger LOG = LoggerFactory.getLogger(ImageHud.class);
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("etherhud", "hud_image");
    private static final Component EMPTY = Component.translatable("etherhud.image.choose");
    private static final Component LOADING = Component.translatable("etherhud.image.loading");
    private static final Component UNAVAILABLE = Component.translatable("etherhud.image.unavailable");
    private static final int WIDTH = 120;
    private static final int HEIGHT = 88;
    private static final int PADDING = 4;

    private String requestedPath = "";
    private CompletableFuture<HudImage> pending;
    private HudImage image;
    private DynamicTexture texture;
    private long started;
    private long nextUpload;
    private int uploadedFrame;

    @Override
    public List<ModuleSetting> settings(ModuleId id) {
        var fields = new ArrayList<ModuleSetting>();
        fields.add(new ModuleSetting.Section("image.source"));
        fields.add(new ModuleSetting.ImageFile("image.file", HudSettings::imagePath, HudSettings::withImagePath));
        fields.addAll(CommonModuleSettings.forModule(id));
        return List.copyOf(fields);
    }

    @Override
    public boolean active(HudSettings settings) { return !settings.imagePath().isBlank(); }

    @Override
    public void update(Minecraft client, HudSettings settings) {
        request(client, settings.imagePath());
    }

    @Override
    public void onInactive() {
        request(Minecraft.getInstance(), "");
    }

    @Override
    public int panelWidth(Font font, Component label, HudSettings settings, boolean preview) { return WIDTH; }

    @Override
    public int panelHeight(HudSettings settings, boolean preview) { return HEIGHT; }

    @Override
    public void draw(Minecraft client, GuiGraphics graphics, HudSettings settings, ModuleId id, Component label,
                     int width, int height, boolean preview, boolean ghost) {
        request(client, settings.imagePath());
        long now = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
        prepareTexture(client, now);
        var appearance = settings.appearance(id);
        HudModule.drawPanel(graphics, width, height, appearance, ghost);
        if (texture == null) {
            Component message = pending != null ? LOADING : requestedPath.isBlank() ? EMPTY : UNAVAILABLE;
            var text = client.font.plainSubstrByWidth(message.getString(), width - 2 * PADDING);
            graphics.drawString(client.font, text, (width - client.font.width(text)) / 2,
                    (height - client.font.lineHeight) / 2, appearance.text(ghost), appearance.textShadow());
            return;
        }
        float fit = Math.min((width - 2 * PADDING) / (float) image.width(), (height - 2 * PADDING) / (float) image.height());
        int w = Math.max(1, Math.round(image.width() * fit));
        int h = Math.max(1, Math.round(image.height() * fit));
        graphics.blit(RenderType::guiTextured, TEXTURE, (width - w) / 2, (height - h) / 2, 0, 0, w, h,
                image.width(), image.height(), image.width(), image.height());
    }

    private void request(Minecraft client, String path) {
        if (requestedPath.equals(path)) {
            return;
        }
        if (pending != null) {
            pending.cancel(false);
        }
        releaseTexture(client);
        requestedPath = path;
        pending = path.isBlank() ? null : CompletableFuture.supplyAsync(() -> {
            try {
                return HudImage.load(Path.of(path));
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    private void prepareTexture(Minecraft client, long now) {
        if (pending != null && pending.isDone()) {
            try {
                image = pending.join();
                var pixels = new NativeImage(image.width(), image.height(), false);
                try {
                    copyPixels(image.frames().getFirst(), pixels);
                    texture = new DynamicTexture(pixels);
                } catch (RuntimeException exception) {
                    pixels.close();
                    throw exception;
                }
                client.getTextureManager().register(TEXTURE, texture);
                started = now;
                uploadedFrame = 0;
                nextUpload = started + image.frames().getFirst().endMillis();
            } catch (RuntimeException exception) {
                LOG.warn("Could not load HUD image", exception);
                releaseTexture(client);
            } finally {
                pending = null;
            }
        }
        if (texture != null && image.frames().size() > 1 && now >= nextUpload) {
            long position = Math.floorMod(now - started, image.durationMillis());
            int frame = 0;
            while (position >= image.frames().get(frame).endMillis()) {
                frame++;
            }
            nextUpload = now + image.frames().get(frame).endMillis() - position;
            if (frame != uploadedFrame) {
                try {
                    copyPixels(image.frames().get(frame), texture.getPixels());
                    texture.upload();
                    uploadedFrame = frame;
                } catch (RuntimeException exception) {
                    LOG.warn("Could not upload a HUD image frame", exception);
                    releaseTexture(client);
                }
            }
        }
    }

    private void releaseTexture(Minecraft client) {
        if (texture != null) {
            client.getTextureManager().release(TEXTURE);
            texture = null;
        }
        image = null;
    }

    private void copyPixels(HudImage.Frame frame, NativeImage target) {
        int index = 0;
        int[] pixels = frame.pixels();
        for (int y = 0; y < image.height(); y++) {
            for (int x = 0; x < image.width(); x++) {
                target.setPixel(x, y, pixels[index++]);
            }
        }
    }
}
