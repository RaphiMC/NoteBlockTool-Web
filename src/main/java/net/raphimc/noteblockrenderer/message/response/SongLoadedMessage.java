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
package net.raphimc.noteblockrenderer.message.response;

import net.raphimc.noteblockrenderer.message.Message;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.core.JSObjects;

public interface SongLoadedMessage extends Message {

    static SongLoadedMessage create() {
        final SongLoadedMessage songLoadedMessage = JSObjects.create();
        songLoadedMessage.setType("song-loaded");
        return songLoadedMessage;
    }

    @JSProperty
    void setFileName(final String fileName);

    @JSProperty
    void setFileSize(final String fileSize);

    @JSProperty
    void setTitle(final String title);

    @JSProperty
    void setAuthor(final String author);

    @JSProperty
    void setOriginalAuthor(final String originalAuthor);

    @JSProperty
    void setDescription(final String description);

    @JSProperty
    void setLength(final String length);

    @JSProperty
    void setNoteCount(final int noteCount);

    @JSProperty
    void setTempo(final String tempo);

    @JSProperty
    void setInstruments(final String[] instruments);

    @JSProperty
    void setNbsVersion(final JSNumber nbsVersion);

}
