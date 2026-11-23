/*
 * This file is part of NoteBlockTool-Web - https://github.com/RaphiMC/NoteBlockTool-Web
 * Copyright (C) 2026-2026 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.raphimc.noteblocktool.visualizer;

import org.teavm.jso.browser.Window;

public class VisualizerRenderer {

    private final Visualizer visualizer;
    private Integer requestId;

    public VisualizerRenderer(final Visualizer visualizer) {
        this.visualizer = visualizer;
    }

    public void start() {
        if (this.requestId == null) {
            this.requestId = Window.requestAnimationFrame(this::onAnimationFrame);
        }
    }

    public void stop() {
        if (this.requestId != null) {
            Window.cancelAnimationFrame(this.requestId);
            this.requestId = null;
        }
    }

    public Visualizer getVisualizer() {
        return this.visualizer;
    }

    private void onAnimationFrame(final double timestamp) {
        this.visualizer.draw(timestamp);
        this.requestId = Window.requestAnimationFrame(this::onAnimationFrame);
    }

}
