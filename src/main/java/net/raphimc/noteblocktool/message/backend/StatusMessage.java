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
package net.raphimc.noteblocktool.message.backend;

import net.raphimc.noteblocktool.message.Message;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;

public interface StatusMessage extends Message {

    String TYPE = "status";

    String CONTEXT_MAIN = "main";
    String CONTEXT_RENDER = "render";
    String CONTEXT_PLAYER = "player";

    static StatusMessage create() {
        final StatusMessage statusMessage = JSObjects.create();
        statusMessage.setType(TYPE);
        return statusMessage;
    }

    static StatusMessage create(final String context, final String message) {
        final StatusMessage statusMessage = create();
        statusMessage.setContext(context);
        statusMessage.setMessage(message);
        return statusMessage;
    }

    @JSProperty
    void setContext(final String context);

    @JSProperty
    void setMessage(final String message);

}
