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
package net.raphimc.noteblocktool.util.js;

import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.EventTarget;

@JSClass
public class OffscreenCanvas implements JSObject, EventTarget {

    public OffscreenCanvas(final int width, final int height) {
    }

    @JSProperty
    public native int getWidth();

    @JSProperty
    public native void setWidth(final int width);

    @JSProperty
    public native int getHeight();

    @JSProperty
    public native void setHeight(final int height);

    public native JSObject getContext(final String contextType);

    public native JSObject getContext(final String contextType, final JSObject contextAttributes);

    @Override
    public native void addEventListener(final String type, final EventListener<?> listener, final boolean useCapture);

    @Override
    public native void addEventListener(final String type, final EventListener<?> listener);

    @Override
    public native void removeEventListener(final String type, final EventListener<?> listener, final boolean useCapture);

    @Override
    public native void removeEventListener(final String type, final EventListener<?> listener);

    @Override
    public native boolean dispatchEvent(final Event evt);

}
