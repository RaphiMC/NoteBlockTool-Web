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
package net.raphimc.noteblocktool.renderer.source;

import net.raphimc.audiomixer.resampler.Resampler;
import net.raphimc.audiomixer.source.audio.impl.BufferedAudioSource;
import net.raphimc.audiomixer.util.buffer.AudioBuffer;
import net.raphimc.noteblocklib.model.note.Note;

public abstract sealed class NoteAudioSource extends BufferedAudioSource permits PreRenderedNoteAudioSource, InstrumentNoteAudioSource {

    private final Note note;

    protected NoteAudioSource(final AudioBuffer buffer, final Note note, final Resampler resampler) {
        super(buffer, resampler);
        this.note = note;
    }

    public Note getNote() {
        return this.note;
    }

}
