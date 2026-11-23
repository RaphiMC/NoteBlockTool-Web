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
package net.raphimc.noteblocktool.teavm.classlib.javax.sound.sampled;

import net.raphimc.noteblocktool.message.backend.CreateSourceDataLineMessage;
import net.raphimc.noteblocktool.util.js.JsNative;
import net.raphimc.noteblocktool.util.js.MessageChannel;
import net.raphimc.noteblocktool.util.js.WebAudioSourceDataLine;
import org.teavm.jso.browser.Window;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.io.InputStream;

public class AudioSystem {

    public static SourceDataLine getSourceDataLine(final AudioFormat format) throws LineUnavailableException {
        if (!JsNative.isCrossOriginIsolated()) {
            throw new LineUnavailableException("SourceDataLine requires cross-origin isolation. See https://developer.mozilla.org/docs/Web/API/Window/crossOriginIsolated for more information.");
        }
        if (!JsNative.SharedArrayBuffer_isAvailable()) {
            throw new LineUnavailableException("SourceDataLine requires SharedArrayBuffer, which is unavailable in this environment");
        }
        final MessageChannel channel = new MessageChannel();
        Window.worker().postMessage(CreateSourceDataLineMessage.create(channel.getPort2()), channel.getPort2());
        return new WebAudioSourceDataLine(channel.getPort1(), format);
    }

    public static AudioInputStream getAudioInputStream(final InputStream stream) throws UnsupportedAudioFileException, IOException {
        throw new UnsupportedAudioFileException("Stream of unsupported format");
    }

}
