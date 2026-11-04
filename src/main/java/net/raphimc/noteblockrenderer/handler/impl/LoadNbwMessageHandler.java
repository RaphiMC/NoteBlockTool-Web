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
package net.raphimc.noteblockrenderer.handler.impl;

import com.google.common.base.Stopwatch;
import net.raphimc.noteblocklib.NoteBlockLib;
import net.raphimc.noteblocklib.format.SongFormat;
import net.raphimc.noteblocklib.format.nbs.model.NbsCustomInstrument;
import net.raphimc.noteblocklib.format.nbs.model.NbsSong;
import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblocklib.util.SongUtil;
import net.raphimc.noteblockrenderer.NoteBlockRenderer;
import net.raphimc.noteblockrenderer.handler.MessageHandler;
import net.raphimc.noteblockrenderer.message.request.LoadNbwMessage;
import net.raphimc.noteblockrenderer.message.response.SongLoadedMessage;
import net.raphimc.noteblockrenderer.util.HttpUtil;
import net.raphimc.noteblockrenderer.util.StringUtil;
import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class LoadNbwMessageHandler extends MessageHandler<LoadNbwMessage> {

    @Override
    public void handleMessage(final LoadNbwMessage message) throws Exception {
        final Stopwatch stopwatch = Stopwatch.createStarted();

        this.postStatusMessage("Fetching download url");
        final XMLHttpRequest openRequest = new XMLHttpRequest();
        openRequest.open("GET", HttpUtil.withCorsProxy("https://api.noteblock.world/v1/song/" + message.getId() + "/open", Map.entry("src", "downloadButton")), false);
        openRequest.setResponseType("text");
        openRequest.send();
        if (openRequest.getStatus() != HttpURLConnection.HTTP_OK || openRequest.getResponse() == null) {
            throw new IOException("Failed to get download url: " + openRequest.getStatus() + " " + openRequest.getResponseText());
        }
        final String downloadUrl = openRequest.getResponseText();

        this.postStatusMessage("Downloading song archive");
        final XMLHttpRequest downloadRequest = new XMLHttpRequest();
        downloadRequest.open("GET", HttpUtil.withCorsProxy(downloadUrl), false);
        downloadRequest.setResponseType("arraybuffer");
        downloadRequest.send();
        if (downloadRequest.getStatus() != HttpURLConnection.HTTP_OK || downloadRequest.getResponse() == null) {
            throw new IOException("Failed to download song archive. See console for more details");
        }
        final byte[] songArchive = new Int8Array((ArrayBuffer) downloadRequest.getResponse()).copyToJavaArray();

        this.postStatusMessage("Extracting song archive");
        final Map<String, byte[]> files = new HashMap<>();
        final ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(songArchive));
        ZipEntry entry;
        while ((entry = zipInputStream.getNextEntry()) != null) {
            if (!entry.isDirectory()) {
                files.put(entry.getName(), zipInputStream.readAllBytes());
            }
        }
        final byte[] songBytes = files.get("song.nbs");
        if (songBytes == null) {
            throw new IOException("Song archive does not contain song.nbs");
        }

        this.postStatusMessage("Parsing song");
        final Song song = NoteBlockLib.readSong(songBytes, SongFormat.NBS, "NoteBlockWorld Song #" + message.getId());
        song.getNotes().removeSilentNotes();
        NoteBlockRenderer.instance().setSong(song);

        this.postStatusMessage("Loading sound samples");
        for (NbsCustomInstrument instrument : SongUtil.getUsedNbsCustomInstruments(song)) {
            final byte[] soundBytes = files.get("sounds/" + instrument.getSoundFilePathOr(""));
            if (soundBytes != null) {
                NoteBlockRenderer.instance().getSoundLoader().store(instrument, soundBytes);
            }
        }

        this.postStatusMessage("Building song metadata");
        final SongLoadedMessage songLoadedMessage = SongLoadedMessage.create();
        songLoadedMessage.setFileName(song.getFileName());
        songLoadedMessage.setFileSize(StringUtil.formatByteCount(songBytes.length));
        songLoadedMessage.setTitle(song.getTitleOrFileName());
        songLoadedMessage.setAuthor(song.getAuthor());
        songLoadedMessage.setOriginalAuthor(song.getOriginalAuthor());
        songLoadedMessage.setDescription(song.getDescription());
        songLoadedMessage.setLength(song.getHumanReadableLength());
        songLoadedMessage.setNoteCount(song.getNotes().getNoteCount());
        songLoadedMessage.setTempo(song.getTempoEvents().getHumanReadableTempoRange());
        songLoadedMessage.setInstruments(Stream.concat(SongUtil.getUsedVanillaInstruments(song).stream(), SongUtil.getUsedNbsCustomInstruments(song).stream()).map(StringUtil::getInstrumentDisplayName).toArray(String[]::new));
        if (song instanceof NbsSong nbsSong) {
            songLoadedMessage.setNbsVersion(JSNumber.valueOf(nbsSong.getVersion()));
        }
        this.postMessage(songLoadedMessage);

        this.postStatusMessage("Loaded NoteBlockWorld song " + message.getId() + " in " + stopwatch);
    }

}
