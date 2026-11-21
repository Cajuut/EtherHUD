package io.github.etherhud.ui.editor;

import io.github.etherhud.layout.LayoutResolver;
import io.github.etherhud.module.ModuleId;

public final class DragController {
    private ModuleId module;
    private double grabX;
    private double grabY;
    private double pressX;
    private double pressY;
    private boolean moving;
    private int x;
    private int y;

    public void begin(ModuleId module, LayoutResolver.Bounds bounds, double mouseX, double mouseY) {
        this.module = module;
        this.pressX = mouseX;
        this.pressY = mouseY;
        this.grabX = mouseX - bounds.x();
        this.grabY = mouseY - bounds.y();
        this.x = bounds.x();
        this.y = bounds.y();
        this.moving = false;
    }

    public boolean passedThreshold(double mouseX, double mouseY) {
        return Math.max(Math.abs(mouseX - pressX), Math.abs(mouseY - pressY)) >= 2;
    }

    public void move(LayoutResolver.Bounds bounds, int screenWidth, int screenHeight, double mouseX, double mouseY) {
        x = LayoutResolver.clampX(bounds, screenWidth, (int) Math.round(mouseX - grabX));
        y = LayoutResolver.clampY(bounds, screenHeight, (int) Math.round(mouseY - grabY));
        moving = true;
    }

    public void snapTo(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public ModuleId module() { return module; }
    public int x() { return x; }
    public int y() { return y; }
    public boolean moving() { return moving; }
    public boolean isMoving(ModuleId id) { return moving && module == id; }

    public void clear() {
        module = null;
        moving = false;
    }
}
