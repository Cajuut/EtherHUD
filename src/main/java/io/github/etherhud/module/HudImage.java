package io.github.etherhud.module;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadataNode;

record HudImage(int width, int height, List<Frame> frames, long durationMillis) {
    private static final int MAX_FILE_BYTES = 32 * 1024 * 1024;
    private static final int MAX_DIMENSION = 2048;
    private static final int MAX_ANIMATED_DIMENSION = 480;
    private static final int MAX_FRAMES = 256;
    private static final long MAX_FRAME_PIXELS = 16_000_000;

    record Frame(int[] pixels, long endMillis) {}

    static HudImage load(Path path) throws IOException {
        if (!Files.isRegularFile(path) || Files.size(path) > MAX_FILE_BYTES) {
            throw new IOException("Image is missing or larger than 32 MiB");
        }
        try (var input = ImageIO.createImageInputStream(path.toFile())) {
            if (input == null) {
                throw new IOException("Could not open image");
            }
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IOException("Unsupported image format");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                if (reader.getFormatName().equalsIgnoreCase("gif")) {
                    return readGif(reader);
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                checkDimensions(width, height);
                return new HudImage(width, height, List.of(new Frame(pixels(reader.read(0)), 0)), 0);
            } finally {
                reader.dispose();
            }
        }
    }

    private static HudImage readGif(ImageReader reader) throws IOException {
        var metadata = reader.getStreamMetadata();
        var stream = (IIOMetadataNode) metadata.getAsTree("javax_imageio_gif_stream_1.0");
        var screen = node(stream, "LogicalScreenDescriptor");
        int width = number(screen, "logicalScreenWidth");
        int height = number(screen, "logicalScreenHeight");
        checkDimensions(width, height);
        int count = reader.getNumImages(true);
        Color canvasBackground = gifBackground(stream);
        double scale = Math.min(1, MAX_ANIMATED_DIMENSION / (double) Math.max(width, height));
        int outWidth = Math.max(1, (int) Math.round(width * scale));
        int outHeight = Math.max(1, (int) Math.round(height * scale));
        if (count <= 0 || count > MAX_FRAMES || (long) count * outWidth * outHeight > MAX_FRAME_PIXELS) {
            throw new IOException("GIF frame limit exceeded");
        }

        List<Frame> frames = new ArrayList<>(count);
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        long duration = 0;
        try {
            for (int i = 0; i < count; i++) {
                var frameMeta = (IIOMetadataNode) reader.getImageMetadata(i).getAsTree("javax_imageio_gif_image_1.0");
                var descriptor = node(frameMeta, "ImageDescriptor");
                var control = node(frameMeta, "GraphicControlExtension");
                int x = number(descriptor, "imageLeftPosition");
                int y = number(descriptor, "imageTopPosition");
                int frameWidth = reader.getWidth(i);
                int frameHeight = reader.getHeight(i);
                checkDimensions(frameWidth, frameHeight);
                if (x < 0 || y < 0 || x + frameWidth > width || y + frameHeight > height) {
                    throw new IOException("GIF frame exceeds canvas");
                }

                boolean transparent = control != null && "TRUE".equalsIgnoreCase(control.getAttribute("transparentColorFlag"));
                Color background = transparent ? null : canvasBackground;
                if (i == 0 && background != null) {
                    restoreBackground(graphics, background, 0, 0, width, height);
                }
                String disposal = control == null ? "" : control.getAttribute("disposalMethod");
                int[] previous = disposal.equals("restoreToPrevious")
                        ? canvas.getRGB(x, y, frameWidth, frameHeight, null, 0, frameWidth) : null;
                graphics.drawImage(reader.read(i), x, y, null);
                int delayMillis = control == null ? 100 : number(control, "delayTime") * 10;
                duration += delayMillis < 20 ? 100 : delayMillis;
                frames.add(new Frame(scaledPixels(canvas, outWidth, outHeight), duration));

                if (previous != null) {
                    canvas.setRGB(x, y, frameWidth, frameHeight, previous, 0, frameWidth);
                } else if (disposal.equals("restoreToBackgroundColor")) {
                    restoreBackground(graphics, background, x, y, frameWidth, frameHeight);
                }
            }
        } finally {
            graphics.dispose();
        }
        return new HudImage(outWidth, outHeight, List.copyOf(frames), duration);
    }

    private static Color gifBackground(IIOMetadataNode stream) {
        var table = node(stream, "GlobalColorTable");
        if (table != null) {
            int index = number(table, "backgroundColorIndex");
            for (var child = table.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child instanceof IIOMetadataNode color && number(color, "index") == index) {
                    return new Color(number(color, "red"), number(color, "green"), number(color, "blue"));
                }
            }
        }
        return null;
    }

    private static void restoreBackground(Graphics2D g, Color color, int x, int y, int width, int height) {
        g.setComposite(color == null ? AlphaComposite.Clear : AlphaComposite.Src);
        if (color != null) {
            g.setColor(color);
        }
        g.fillRect(x, y, width, height);
        g.setComposite(AlphaComposite.SrcOver);
    }

    private static int[] scaledPixels(BufferedImage source, int width, int height) {
        if (source.getWidth() == width && source.getHeight() == height) {
            return pixels(source);
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source, 0, 0, width, height, null);
            return pixels(image);
        } finally {
            graphics.dispose();
        }
    }

    private static int[] pixels(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        return image.getRGB(0, 0, width, height, null, 0, width);
    }

    private static IIOMetadataNode node(IIOMetadataNode root, String name) {
        return (IIOMetadataNode) root.getElementsByTagName(name).item(0);
    }

    private static int number(IIOMetadataNode node, String attribute) {
        return Integer.parseInt(node.getAttribute(attribute));
    }

    private static void checkDimensions(int width, int height) throws IOException {
        if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION) {
            throw new IOException("Unsupported image dimensions");
        }
    }
}
