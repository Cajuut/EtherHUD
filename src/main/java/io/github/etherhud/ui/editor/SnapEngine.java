package io.github.etherhud.ui.editor;

import io.github.etherhud.layout.LayoutResolver;

public final class SnapEngine {
    private static final int DISTANCE = 5;
    public static final int NO_GUIDE = -1;

    private final Axis horizontal = new Axis();
    private final Axis vertical = new Axis();

    public void start(int x, int y, LayoutResolver.Bounds bounds, int width, int height) {
        horizontal.start(x, bounds.width(), width);
        vertical.start(y, bounds.height(), height);
    }

    public void consider(LayoutResolver.Bounds other) {
        horizontal.considerEdges(other.x(), other.right());
        vertical.considerEdges(other.y(), other.bottom());
    }

    public int x() { return horizontal.position; }
    public int y() { return vertical.position; }
    public int guideX() { return horizontal.guide; }
    public int guideY() { return vertical.guide; }

    private static final class Axis {
        private int origin;
        private int size;
        private int limit;
        private int position;
        private int distance;
        private int guide;

        void start(int origin, int size, int screen) {
            this.origin = origin;
            this.size = size;
            this.limit = screen - size;
            this.position = origin;
            this.distance = DISTANCE + 1;
            this.guide = NO_GUIDE;
            consider(0, 0);
            consider(limit, screen);
            consider((limit + 1) / 2, (screen + 1) / 2);
        }

        void considerEdges(int first, int last) {
            consider(first, first);
            consider(first - size, first);
            consider(last, last);
            consider(last - size, last);
        }

        private void consider(int candidate, int line) {
            int delta = Math.abs(candidate - origin);
            if (candidate >= 0 && candidate <= limit && delta < distance) {
                distance = delta;
                position = candidate;
                guide = line;
            }
        }
    }
}
