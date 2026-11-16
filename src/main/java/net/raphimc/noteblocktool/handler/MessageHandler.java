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
import net.raphimc.noteblocktool.message.backend.ErrorMessage;
import net.raphimc.noteblocktool.message.backend.StatusMessage;
import net.raphimc.noteblocktool.message.backend.WarningMessage;
import net.raphimc.noteblocktool.util.Logger;
import net.raphimc.noteblocktool.util.js.JsNative;
import org.teavm.jso.JSObject;

public abstract class MessageHandler<T extends Message> {

    private final String messageContext;

    protected MessageHandler(final String messageContext) {
        this.messageContext = messageContext;
    }

    public abstract void handleMessage(final T message) throws Throwable;

    protected void postStatusMessage(final String message) {
        Logger.info(message);
        this.postMessage(StatusMessage.create(this.messageContext, message));
    }

    protected void postWarningMessage(final String message) {
        Logger.error(message);
        this.postMessage(WarningMessage.create(this.messageContext, message));
    }

    public void postErrorMessage(final String message) {
        Logger.error(message);
        this.postMessage(ErrorMessage.create(this.messageContext, message));
    }

    protected void postMessage(final JSObject message) {
        JsNative.postMessage(message);
    }

}
