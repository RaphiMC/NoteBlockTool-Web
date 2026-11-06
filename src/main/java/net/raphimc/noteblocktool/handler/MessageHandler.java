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
package net.raphimc.noteblocktool.handler;

import net.raphimc.noteblocktool.message.Message;
import net.raphimc.noteblocktool.message.response.ErrorMessage;
import net.raphimc.noteblocktool.message.response.StatusMessage;
import net.raphimc.noteblocktool.message.response.WarningMessage;
import net.raphimc.noteblocktool.util.Logger;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

public abstract class MessageHandler<T extends Message> {

    public abstract void handleMessage(final T message) throws Throwable;

    protected void postStatusMessage(final String message) {
        Logger.info(message);
        final StatusMessage statusMessage = StatusMessage.create();
        statusMessage.setMessage(message);
        this.postMessage(statusMessage);
    }

    protected void postWarningMessage(final String message) {
        Logger.error(message);
        final WarningMessage warningMessage = WarningMessage.create();
        warningMessage.setMessage(message);
        this.postMessage(warningMessage);
    }

    protected void postErrorMessage(final String message) {
        Logger.error(message);
        final ErrorMessage errorMessage = ErrorMessage.create();
        errorMessage.setMessage(message);
        this.postMessage(errorMessage);
    }

    protected void postMessage(final JSObject message) {
        self_postMessage(message);
    }

    @JSBody(params = "message", script = "self.postMessage(message);")
    private static native void self_postMessage(final JSObject message);

}
