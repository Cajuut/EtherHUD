package io.github.etherhud.ui.editor;

import io.github.etherhud.config.HudSettings;
import io.github.etherhud.layout.LayoutResolver;
import io.github.etherhud.layout.ModulePlacement;
import io.github.etherhud.module.ModuleId;
import io.github.etherhud.render.HudRenderer;

import java.util.ArrayList;
import java.util.Set;

public final class PanelGroup {
    private record Panel(ModuleId id, int x, int y, int width, int height, float units, ModulePlacement placement) {}

    private final HudSettings original;
    private final int screenWidth;
    private final int screenHeight;
    private final ArrayList<Panel> panels = new ArrayList<>();
    private final LayoutResolver resolver = new LayoutResolver();
    private final LayoutResolver.Bounds frame = new LayoutResolver.Bounds();
    private final LayoutResolver.Bounds panelBounds = new LayoutResolver.Bounds();

    public PanelGroup(Set<ModuleId> selection, HudRenderer preview, HudSettings original, int width, int height) {
        this.original = original;
        this.screenWidth = width;
        this.screenHeight = height;
        updateFrame(frame, selection, preview, width, height);
        for (ModuleId id : selection) {
            var bounds = preview.bounds(id);
            panels.add(new Panel(id, bounds.x(), bounds.y(), preview.panelWidth(id), preview.panelHeight(id),
                    bounds.unitScale(), original.placement(id)));
        }
    }

    public static void updateFrame(LayoutResolver.Bounds out, Set<ModuleId> selection, HudRenderer preview, int width, int height) {
        int left = width;
        int top = height;
        int right = 0;
        int bottom = 0;
        for (ModuleId id : ModuleId.ALL) {
            if (selection.contains(id)) {
                var bounds = preview.bounds(id);
                left = Math.min(left, bounds.x());
                top = Math.min(top, bounds.y());
                right = Math.max(right, bounds.right());
                bottom = Math.max(bottom, bounds.bottom());
            }
        }
        LayoutResolver.measure(out, Math.max(1, right - left), Math.max(1, bottom - top), 1, width, height, left, top, 0, 0);
    }

    public HudSettings original() { return original; }
    public LayoutResolver.Bounds frame() { return frame; }

    public HudSettings move(int x, int y) {
        int dx = LayoutResolver.clampX(frame, screenWidth, x) - frame.x();
        int dy = LayoutResolver.clampY(frame, screenHeight, y) - frame.y();
        if (dx == 0 && dy == 0) {
            return original;
        }
        HudSettings updated = original;
        for (Panel panel : panels) {
            updated = position(updated, panel, panel.placement(), panel.x() + dx, panel.y() + dy);
        }
        return updated;
    }

    public HudSettings resize(double factor, double anchorX, double anchorY) {
        double pivotX = frame.x() + frame.width() * anchorX;
        double pivotY = frame.y() + frame.height() * anchorY;
        double min = 0;
        double max = Math.min(room(pivotX, anchorX, screenWidth) / frame.width(),
                room(pivotY, anchorY, screenHeight) / frame.height());
        for (Panel panel : panels) {
            if (!original.canResize(panel.id())) {
                return original;
            }
            min = Math.max(min, 0.5 / panel.placement().scale());
            max = Math.min(max, 3.0 / panel.placement().scale());
        }
        factor = Math.clamp(factor, min, Math.max(min, max));
        if (Math.abs(factor - 1) < 0.0001) {
            return original;
        }
        HudSettings updated = original;
        for (Panel panel : panels) {
            var placement = panel.placement().withScale((float) (panel.placement().scale() * factor));
            int x = (int) Math.round(pivotX + (panel.x() - pivotX) * factor);
            int y = (int) Math.round(pivotY + (panel.y() - pivotY) * factor);
            updated = position(updated, panel, placement, x, y);
        }
        return updated;
    }

    private HudSettings position(HudSettings updated, Panel panel, ModulePlacement placement, int x, int y) {
        float scale = original.canResize(panel.id()) ? original.hudScale() * placement.scale() * panel.units() : 1;
        LayoutResolver.measure(panelBounds, panel.width(), panel.height(), scale, screenWidth, screenHeight,
                x, y, 0, 0, panel.units());
        return updated.withPlacement(panel.id(), resolver.place(placement, panelBounds, screenWidth, screenHeight,
                panelBounds.x(), panelBounds.y(), null));
    }

    private double room(double pivot, double alignment, int extent) {
        double before = alignment == 0 ? Double.POSITIVE_INFINITY : pivot / alignment;
        double after = alignment == 1 ? Double.POSITIVE_INFINITY : (extent - pivot) / (1 - alignment);
        return Math.min(before, after);
    }
}
