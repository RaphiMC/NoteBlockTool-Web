/*
 * This file is part of NoteBlockRenderer - https://github.com/RaphiMC/NoteBlockRenderer
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
package net.raphimc.noteblockrenderer.util;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSByRef;
import org.teavm.jso.file.File;
import org.teavm.jso.typedarrays.ArrayBuffer;

@SuppressWarnings("checkstyle:MethodName")
public final class JsNative {

    private JsNative() {
    }

    @JSBody(params = {"value", "precision"}, script = "return value.toPrecision(precision);")
    public static native String Number_toPrecision(final double value, final int precision);

    @JSBody(params = {"a", "fromIndex", "toIndex", "val"}, script = "a.fill(val, fromIndex, toIndex);")
    public static native void Array_fill(@JSByRef final float[] a, final int fromIndex, final int toIndex, final float val);

    @JSBody(params = "file", script = "return new FileReaderSync().readAsArrayBuffer(file);")
    public static native ArrayBuffer FileReaderSync_readAsArrayBuffer(final File file);

    @JSBody(params = "file", script = "return file.webkitRelativePath;")
    public static native String File_getWebkitRelativePath(final File file);

}
