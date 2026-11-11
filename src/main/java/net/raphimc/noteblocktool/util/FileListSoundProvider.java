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

import net.raphimc.noteblocklib.format.minecraft.MinecraftInstrument;
import net.raphimc.noteblocklib.format.nbs.model.NbsCustomInstrument;
import net.raphimc.noteblocklib.model.note.Instrument;
import net.raphimc.noteblockrenderer.provider.SoundProvider;
import org.teavm.jso.file.File;
import org.teavm.jso.typedarrays.Int8Array;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class FileListSoundProvider implements SoundProvider {

    private final Map<String, File> sounds = new HashMap<>();

    public FileListSoundProvider(final File[] sounds) {
        for (File file : sounds) {
            final String path = JsNative.File_getWebkitRelativePath(file);
            this.sounds.put(path.substring(path.indexOf('/') + 1), file);
        }
    }

    @Override
    public InputStream getSoundInputStream(final Instrument instrument) throws IOException {
        final String soundPath = getInstrumentSoundPath(instrument);
        if (this.sounds.containsKey(soundPath)) {
            return new ByteArrayInputStream(new Int8Array(JsNative.FileReaderSync_readAsArrayBuffer(this.sounds.get(soundPath))).copyToJavaArray());
        } else {
            throw new IOException("Missing sound sample");
        }
    }

    private static String getInstrumentSoundPath(final Instrument instrument) throws IOException {
        if (instrument instanceof MinecraftInstrument minecraftInstrument) {
            return "minecraft/" + minecraftInstrument.mcSoundPath() + ".ogg";
        } else if (instrument instanceof NbsCustomInstrument customInstrument) {
            return customInstrument.getSoundFilePathOr("").replace('\\', '/');
        }
        throw new IOException("Unsupported instrument type: " + instrument.getClass().getName());
    }

}
