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
package net.raphimc.noteblockrenderer.renderer.source;

import net.raphimc.audiomixer.processor.dynamics.GainProcessor;
import net.raphimc.audiomixer.processor.spatial.GainPanProcessor;
import net.raphimc.audiomixer.processor.spatial.PanProcessor;
import net.raphimc.audiomixer.resampler.Resampler;
import net.raphimc.audiomixer.util.buffer.AudioBuffer;
import net.raphimc.noteblocklib.model.note.Note;

public final class InstrumentNoteAudioSource extends NoteAudioSource {

    public InstrumentNoteAudioSource(final AudioBuffer buffer, final Note note, final boolean stereo, final Resampler resampler) {
        super(buffer, note, resampler);
        this.pitch().set(note.getPitch());
        if (stereo) {
            if (note.getPanning() != 0F && note.getVolume() != 1F) {
                this.processors().add(new GainPanProcessor(note.getVolume(), note.getPanning()));
            } else if (note.getVolume() != 1F) {
                this.processors().add(new GainProcessor(note.getVolume()));
            } else if (note.getPanning() != 0F) {
                this.processors().add(new PanProcessor(note.getPanning()));
            }
        } else {
            if (note.getVolume() != 1F) {
                this.processors().add(new GainProcessor(note.getVolume()));
            }
        }
    }

}
