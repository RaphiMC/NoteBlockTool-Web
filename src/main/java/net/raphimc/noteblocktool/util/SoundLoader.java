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
package net.raphimc.noteblocktool.util;

import net.raphimc.audiomixer.io.AudioIo;
import net.raphimc.audiomixer.util.buffer.AudioBuffer;
import net.raphimc.noteblocklib.format.minecraft.MinecraftInstrument;
import net.raphimc.noteblocklib.format.nbs.model.NbsCustomInstrument;
import net.raphimc.noteblocklib.model.note.Instrument;
import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.file.File;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

public class SoundLoader {

    private final Map<String, File> customSoundFiles = new HashMap<>();
    private final Map<Instrument, AudioBuffer> sounds = new HashMap<>();

    public SoundLoader() {
    }

    public SoundLoader(final File[] customSoundFiles) {
        for (File file : customSoundFiles) {
            final String path = JsNative.File_getWebkitRelativePath(file);
            this.customSoundFiles.put(path.substring(path.indexOf('/') + 1), file);
        }
    }

    public AudioBuffer getOrThrow(final Instrument instrument) throws IOException {
        if (!this.sounds.containsKey(instrument)) {
            if (!this.load(instrument)) {
                throw new IOException("Missing sound sample");
            }
        }
        return this.sounds.get(instrument);
    }

    public void store(final Instrument instrument, final byte[] soundData) throws IOException {
        this.sounds.put(instrument, AudioIo.read(new ByteArrayInputStream(soundData)));
    }

    private boolean load(final Instrument instrument) throws IOException {
        final String path = getInstrumentSoundPath(instrument);
        if (path.isBlank()) {
            return false;
        }
        AudioBuffer audioBuffer = this.loadCustom(path);
        if (audioBuffer == null && path.startsWith("minecraft/note/")) {
            audioBuffer = this.loadBuiltIn(path.substring(15));
        }
        if (audioBuffer == null && path.startsWith("minecraft/")) {
            audioBuffer = this.loadRemote(path.substring(10));
        }
        if (audioBuffer != null) {
            this.sounds.put(instrument, audioBuffer);
            return true;
        }
        return false;
    }

    private AudioBuffer loadCustom(final String path) throws IOException {
        if (this.customSoundFiles.containsKey(path)) {
            final byte[] bytes = new Int8Array(JsNative.FileReaderSync_readAsArrayBuffer(this.customSoundFiles.get(path))).copyToJavaArray();
            return AudioIo.read(new ByteArrayInputStream(bytes));
        }
        return null;
    }

    private AudioBuffer loadBuiltIn(final String name) throws IOException {
        final InputStream resourceInputStream = SoundLoader.class.getClassLoader().getResourceAsStream(name);
        if (resourceInputStream != null) {
            try (resourceInputStream) {
                return AudioIo.read(resourceInputStream);
            }
        }
        return null;
    }

    private AudioBuffer loadRemote(final String path) throws IOException {
        final XMLHttpRequest xhr = new XMLHttpRequest();
        xhr.open("GET", "https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/refs/heads/26.3/assets/minecraft/sounds/" + path, false);
        xhr.setResponseType("arraybuffer");
        xhr.send();
        if (xhr.getStatus() == HttpURLConnection.HTTP_OK && xhr.getResponse() != null) {
            final byte[] response = new Int8Array((ArrayBuffer) xhr.getResponse()).copyToJavaArray();
            return AudioIo.read(new ByteArrayInputStream(response));
        }
        return null;
    }

    private static String getInstrumentSoundPath(final Instrument instrument) {
        if (instrument instanceof MinecraftInstrument minecraftInstrument) {
            return "minecraft/" + minecraftInstrument.mcSoundPath() + ".ogg";
        } else if (instrument instanceof NbsCustomInstrument customInstrument) {
            return customInstrument.getSoundFilePathOr("").replace('\\', '/');
        } else {
            throw new IllegalArgumentException("Unsupported instrument type: " + instrument.getClass().getName());
        }
    }

}
