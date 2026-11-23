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
package net.raphimc.noteblocktool.visualizer.impl;

import net.raphimc.noteblocktool.util.js.OffscreenCanvas;
import net.raphimc.noteblocktool.visualizer.Visualizer;
import org.teavm.jso.canvas.CanvasRenderingContext2D;

public class TestVisualizer extends Visualizer {

    private final CanvasRenderingContext2D ctx;

    public TestVisualizer(final OffscreenCanvas canvas) {
        super(canvas);
        this.ctx = (CanvasRenderingContext2D) canvas.getContext("2d");
    }

    public void draw(final double timestamp) {
        this.ctx.clearRect(0, 0, this.getCanvas().getWidth(), this.getCanvas().getHeight());
        this.ctx.setFillStyle("#ffffff");
        this.ctx.setFont("24px sans-serif");
        this.ctx.fillText("Work in progress", 100, 100);
    }

}
