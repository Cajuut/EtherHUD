package io.github.etherhud.ui.editor;

import io.github.etherhud.layout.LayoutResolver;
import io.github.etherhud.layout.ModulePlacement;

public final class ResizeController {
    public enum Corner {
        TOP_LEFT(false, false), TOP_RIGHT(true, false), BOTTOM_LEFT(false, true), BOTTOM_RIGHT(true, true);

        public static final Corner[] ALL = values();
        private final boolean right;
        private final boolean bottom;

        Corner(boolean right, boolean bottom) {
            this.right = right;
            this.bottom = bottom;
        }

        public int x(LayoutResolver.Bounds bounds) { return right ? bounds.right() : bounds.x(); }
        public int y(LayoutResolver.Bounds bounds) { return bottom ? bounds.bottom() : bounds.y(); }

        public static Corner hit(LayoutResolver.Bounds bounds, double mouseX, double mouseY) {
            Corner nearest = null;
            double best = Double.POSITIVE_INFINITY;
            for (Corner corner : ALL) {
                double dx = mouseX - corner.x(bounds);
                double dy = mouseY - corner.y(bounds);
                double distance = dx * dx + dy * dy;
                if (Math.abs(dx) <= 4 && Math.abs(dy) <= 4 && distance < best) {
                    nearest = corner;
                    best = distance;
                }
            }
            return nearest;
        }
    }

    private static final int SNAP_RANGE = 5;
    private final LayoutResolver.Bounds target = new LayoutResolver.Bounds();
    private int screenWidth;
    private int screenHeight;
    private int panelWidth;
    private int panelHeight;
    private float globalScale;
    private float unitScale;
    private double anchorX;
    private double anchorY;
    private double fixedX;
    private double fixedY;
    private double startMouseX;
    private double startMouseY;
    private double initialScale;
    private double axisX;
    private double axisY;
    private int maximumStep;
    private int rawStep;
    private int rawEdgeX;
    private int rawEdgeY;
    private int guideX;
    private int guideY;
    private int bestSnap;
    private int snappedStep;

    public void start(LayoutResolver.Bounds bounds, double anchorX, double anchorY, float globalScale,
                      int panelWidth, int panelHeight, int screenWidth, int screenHeight,
                      Corner handle, double mouseX, double mouseY) {
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.fixedX = bounds.x() + bounds.width() * anchorX;
        this.fixedY = bounds.y() + bounds.height() * anchorY;
        this.globalScale = globalScale;
        this.unitScale = bounds.unitScale();
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.startMouseX = mouseX;
        this.startMouseY = mouseY;
        this.initialScale = bounds.scale() / globalScale;
        axisX = (handle.right ? 1 : 0) - anchorX;
        axisY = (handle.bottom ? 1 : 0) - anchorY;
        if (axisX == 0 && axisY == 0) {
            axisX = handle.right ? -1 : 1;
            axisY = handle.bottom ? -1 : 1;
        }
        double limit = Math.min(limit(fixedX, anchorX, screenWidth) / panelWidth,
                limit(fixedY, anchorY, screenHeight) / panelHeight) / globalScale;
        maximumStep = Math.clamp((int) Math.floor(limit * ModulePlacement.SCALE_STEPS + 1e-5), 10, 60);
        guideX = SnapEngine.NO_GUIDE;
        guideY = SnapEngine.NO_GUIDE;
    }

    private double limit(double pivot, double fraction, int extent) {
        double left = fraction == 0 ? Double.POSITIVE_INFINITY : pivot / fraction;
        double right = fraction == 1 ? Double.POSITIVE_INFINITY : (extent - pivot) / (1 - fraction);
        return Math.min(left, right);
    }

    public boolean moved(double mouseX, double mouseY) {
        return Math.max(Math.abs(mouseX - startMouseX), Math.abs(mouseY - startMouseY)) >= 1;
    }

    public void move(double mouseX, double mouseY, boolean snapping) {
        double vectorX = axisX * panelWidth * globalScale;
        double vectorY = axisY * panelHeight * globalScale;
        double change = ((mouseX - startMouseX) * vectorX + (mouseY - startMouseY) * vectorY)
                / (vectorX * vectorX + vectorY * vectorY);
        setScale((float) (initialScale + change));
        if (snapping) {
            considerX(0);
            considerX(screenWidth / 2);
            considerX(screenWidth);
            considerY(0);
            considerY(screenHeight / 2);
            considerY(screenHeight);
        }
    }

    public void setScale(float scale) {
        rawStep = step(scale);
        snappedStep = rawStep;
        measure(rawStep);
        rawEdgeX = edgeX();
        rawEdgeY = edgeY();
        bestSnap = Integer.MAX_VALUE;
        guideX = SnapEngine.NO_GUIDE;
        guideY = SnapEngine.NO_GUIDE;
    }

    public void setWheelScale(float scale) {
        maximumStep = 60;
        setScale(scale);
    }

    public void consider(LayoutResolver.Bounds other) {
        considerX(other.x());
        considerX(other.right());
        considerY(other.y());
        considerY(other.bottom());
    }

    private void considerX(int line) {
        if (axisX != 0 && Math.abs(line - rawEdgeX) <= SNAP_RANGE) {
            int candidate = step((line - fixedX) / (axisX * panelWidth * globalScale));
            measure(candidate);
            int error = Math.abs(edgeX() - line);
            consider(candidate, line, SnapEngine.NO_GUIDE, error, Math.abs(line - rawEdgeX));
        }
    }

    private void considerY(int line) {
        if (axisY != 0 && Math.abs(line - rawEdgeY) <= SNAP_RANGE) {
            int candidate = step((line - fixedY) / (axisY * panelHeight * globalScale));
            measure(candidate);
            int error = Math.abs(edgeY() - line);
            consider(candidate, SnapEngine.NO_GUIDE, line, error, Math.abs(line - rawEdgeY));
        }
    }

    private void consider(int candidate, int x, int y, int error, int distance) {
        int score = error * (SNAP_RANGE + 1) + distance;
        if (error <= 2 && score < bestSnap) {
            bestSnap = score;
            snappedStep = candidate;
            guideX = x;
            guideY = y;
        }
    }

    private int step(double scale) {
        return (int) Math.clamp(Math.round(scale * ModulePlacement.SCALE_STEPS), 10, maximumStep);
    }

    private int edgeX() { return axisX > 0 ? target.right() : target.x(); }
    private int edgeY() { return axisY > 0 ? target.bottom() : target.y(); }

    private void measure(int step) {
        LayoutResolver.measure(target, panelWidth, panelHeight, globalScale * (step / (float) ModulePlacement.SCALE_STEPS),
                screenWidth, screenHeight, fixedX, fixedY, anchorX, anchorY, unitScale);
    }

    public LayoutResolver.Bounds bounds() {
        measure(snappedStep);
        return target;
    }

    public float scale() { return snappedStep / (float) ModulePlacement.SCALE_STEPS; }
    public double alignX() { return anchorX; }
    public double alignY() { return anchorY; }
    public int guideX() { return guideX; }
    public int guideY() { return guideY; }
}
