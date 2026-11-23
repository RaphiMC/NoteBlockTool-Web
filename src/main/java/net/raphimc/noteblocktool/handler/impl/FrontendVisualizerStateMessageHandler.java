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
package net.raphimc.noteblocktool.handler.impl;

import net.raphimc.noteblocktool.NoteBlockTool;
import net.raphimc.noteblocktool.handler.MessageHandler;
import net.raphimc.noteblocktool.message.backend.StatusMessage;
import net.raphimc.noteblocktool.message.frontend.FrontendVisualizerStateMessage;
import net.raphimc.noteblocktool.util.js.OffscreenCanvas;
import net.raphimc.noteblocktool.visualizer.VisualizerRenderer;

public class FrontendVisualizerStateMessageHandler extends MessageHandler<FrontendVisualizerStateMessage> {

    public FrontendVisualizerStateMessageHandler() {
        super(StatusMessage.CONTEXT_PLAYER);
    }

    @Override
    public void handleMessage(final FrontendVisualizerStateMessage message) {
        final VisualizerRenderer visualizer = NoteBlockTool.instance().getVisualizer();
        if (visualizer == null) {
            this.postWarningMessage("Failed to update visualizer state: The visualizer is currently not running");
            return;
        }
        final OffscreenCanvas canvas = visualizer.getVisualizer().getCanvas();
        if (canvas.getWidth() != message.getWidth()) {
            canvas.setWidth(message.getWidth());
        }
        if (canvas.getHeight() != message.getHeight()) {
            canvas.setHeight(message.getHeight());
        }
    }

}
