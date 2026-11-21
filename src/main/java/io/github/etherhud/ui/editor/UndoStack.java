package io.github.etherhud.ui.editor;

import io.github.etherhud.config.HudSettings;

import java.util.ArrayDeque;

public final class UndoStack {
    private static final int LIMIT = 20;
    private final ArrayDeque<HudSettings> past = new ArrayDeque<>(LIMIT);
    private final ArrayDeque<HudSettings> future = new ArrayDeque<>(LIMIT);

    public void record(HudSettings previous) {
        if (past.size() == LIMIT) {
            past.removeFirst();
        }
        past.addLast(previous);
        future.clear();
    }

    public HudSettings undo(HudSettings current) {
        HudSettings previous = past.pollLast();
        if (previous == null) {
            return current;
        }
        future.addLast(current);
        return previous;
    }

    public HudSettings redo(HudSettings current) {
        HudSettings next = future.pollLast();
        if (next == null) {
            return current;
        }
        past.addLast(current);
        return next;
    }
}
