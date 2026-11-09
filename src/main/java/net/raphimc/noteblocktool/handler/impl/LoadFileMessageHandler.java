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
package net.raphimc.noteblocktool.handler.impl;

import com.google.common.base.Stopwatch;
import net.raphimc.noteblocklib.NoteBlockLib;
import net.raphimc.noteblocklib.format.SongFormat;
import net.raphimc.noteblocklib.format.nbs.model.NbsSong;
import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblocklib.util.SongUtil;
import net.raphimc.noteblocktool.NoteBlockTool;
import net.raphimc.noteblocktool.handler.MessageHandler;
import net.raphimc.noteblocktool.message.request.LoadFileMessage;
import net.raphimc.noteblocktool.message.response.SongLoadedMessage;
import net.raphimc.noteblocktool.util.JsNative;
import net.raphimc.noteblocktool.util.StringUtil;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.typedarrays.Int8Array;

public class LoadFileMessageHandler extends MessageHandler<LoadFileMessage> {

    @Override
    public void handleMessage(final LoadFileMessage message) throws Exception {
        final Stopwatch stopwatch = Stopwatch.createStarted();

        this.postStatusMessage("Reading file");
        final byte[] bytes = new Int8Array(JsNative.FileReaderSync_readAsArrayBuffer(message.getFile())).copyToJavaArray();

        this.postStatusMessage("Parsing song");
        final Song song = NoteBlockLib.readSong(bytes, SongFormat.NBS, message.getFile().getName());
        song.getNotes().removeSilentNotes();
        NoteBlockTool.instance().setSong(song);

        this.postStatusMessage("Building song metadata");
        final SongLoadedMessage songLoadedMessage = SongLoadedMessage.create();
        songLoadedMessage.setFileName(song.getFileName());
        songLoadedMessage.setFileSize(StringUtil.formatByteCount(bytes.length));
        songLoadedMessage.setTitle(song.getTitleOrFileName());
        songLoadedMessage.setAuthor(song.getAuthor());
        songLoadedMessage.setOriginalAuthor(song.getOriginalAuthor());
        songLoadedMessage.setDescription(song.getDescription());
        songLoadedMessage.setLength(song.getHumanReadableLength());
        songLoadedMessage.setNoteCount(song.getNotes().getNoteCount());
        songLoadedMessage.setTempo(song.getTempoEvents().getHumanReadableTempoRange());
        songLoadedMessage.setInstruments(SongUtil.getUsedInstruments(song).stream().map(StringUtil::getInstrumentDisplayName).toArray(String[]::new));
        if (song instanceof NbsSong nbsSong) {
            songLoadedMessage.setNbsVersion(JSNumber.valueOf(nbsSong.getVersion()));
        }
        this.postMessage(songLoadedMessage);

        this.postStatusMessage("Loaded " + message.getFile().getName() + " in " + stopwatch);
    }

}
