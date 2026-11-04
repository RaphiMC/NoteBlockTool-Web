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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.raphimc.audiomixer.io.AudioOutputStream;
import net.raphimc.audiomixer.io.flac.FlacAudioOutputStream;
import net.raphimc.audiomixer.io.mp3.Mp3AudioOutputStream;
import net.raphimc.audiomixer.io.ogg.opus.OggOpusAudioOutputStream;
import net.raphimc.audiomixer.io.wav.pcm.WavPcmAudioOutputStream;
import net.raphimc.audiomixer.util.PcmSampleEncoding;
import net.raphimc.audiomixer.util.buffer.AudioBuffer;
import net.raphimc.audiomixer.util.io.seekable.SeekableBufferedOutputStream;
import net.raphimc.audiomixer.util.io.seekable.SeekableByteArrayOutputStream;
import net.raphimc.noteblocklib.model.note.Instrument;
import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblocklib.util.SongUtil;
import net.raphimc.noteblockrenderer.NoteBlockRenderer;
import net.raphimc.noteblockrenderer.handler.MessageHandler;
import net.raphimc.noteblockrenderer.message.request.RenderMessage;
import net.raphimc.noteblockrenderer.message.response.RenderFinishedMessage;
import net.raphimc.noteblockrenderer.message.response.RenderProgressMessage;
import net.raphimc.noteblockrenderer.renderer.SongRenderer;
import net.raphimc.noteblockrenderer.util.SoundLoader;
import net.raphimc.noteblockrenderer.util.StringUtil;
import org.concentus.OpusSignal;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RenderMessageHandler extends MessageHandler<RenderMessage> {

    @Override
    public void handleMessage(final RenderMessage message) throws IOException {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final SoundLoader soundLoader = NoteBlockRenderer.instance().getSoundLoader();
        final Song song = NoteBlockRenderer.instance().getSong();

        for (Instrument instrument : Sets.union(SongUtil.getUsedVanillaInstruments(song), SongUtil.getUsedNbsCustomInstruments(song))) {
            final String displayName = StringUtil.getInstrumentDisplayName(instrument);
            this.postStatusMessage("Loading sound sample for '" + displayName + "'");
            try {
                if (!soundLoader.ensureLoaded(instrument)) {
                    throw new IOException("Missing sound sample");
                }
            } catch (final IOException e) {
                this.postWarningMessage("Failed to load sound sample for '" + displayName + "': " + e.getMessage());
            }
        }

        final SongRenderer renderer = new SongRenderer(song, soundLoader.getSounds(), message.getAudioFileOptions(), message.getPlaybackOptions(), message.getRendererOptions(), message.getExtraOptions(), message.getAdvancedOptions());
        this.postStatusMessage("Pre rendering most used notes");
        renderer.preRenderMostUsedNotes(progress -> {
            final RenderProgressMessage renderProgressMessage = RenderProgressMessage.create();
            renderProgressMessage.setProgress(progress);
            this.postMessage(renderProgressMessage);
        });
        this.postStatusMessage("Rendering song");
        final AudioBuffer buffer = renderer.render(progress -> {
            final RenderProgressMessage renderProgressMessage = RenderProgressMessage.create();
            renderProgressMessage.setProgress(progress);
            this.postMessage(renderProgressMessage);
        });

        this.postStatusMessage("Encoding audio to " + message.getAudioFileOptions().getFormat());
        final RenderFinishedMessage renderFinishedMessage = RenderFinishedMessage.create();
        final SeekableByteArrayOutputStream output = new SeekableByteArrayOutputStream();
        try (AudioOutputStream audioOutputStream = switch (message.getAudioFileOptions().getFormat()) {
            case "wav" -> {
                renderFinishedMessage.setFileName(song.getTitleOrFileName() + ".wav");
                renderFinishedMessage.setMimeType("audio/wav");
                final RenderMessage.WavAudioFileOptions wavAudioFileOptions = (RenderMessage.WavAudioFileOptions) message.getAudioFileOptions();
                final PcmSampleEncoding encoding = switch (wavAudioFileOptions.getEncoding()) {
                    case "u8" -> PcmSampleEncoding.U8;
                    case "s16le" -> PcmSampleEncoding.S16_LE;
                    case "s24le" -> PcmSampleEncoding.S24_LE;
                    case "s32le" -> PcmSampleEncoding.S32_LE;
                    case "f32le" -> PcmSampleEncoding.F32_LE;
                    default -> throw new UnsupportedOperationException("Unsupported encoding: " + wavAudioFileOptions.getEncoding());
                };
                yield new WavPcmAudioOutputStream(new BufferedOutputStream(output), buffer.format(), encoding, buffer.sampleCount());
            }
            case "flac" -> {
                renderFinishedMessage.setFileName(song.getTitleOrFileName() + ".flac");
                renderFinishedMessage.setMimeType("audio/flac");
                final RenderMessage.FlacAudioFileOptions flacAudioFileOptions = (RenderMessage.FlacAudioFileOptions) message.getAudioFileOptions();
                final FlacAudioOutputStream.Encoding encoding = new FlacAudioOutputStream.Encoding(flacAudioFileOptions.getBitsPerSample(), flacAudioFileOptions.getCompressionLevel());
                yield new FlacAudioOutputStream(new SeekableBufferedOutputStream(output), buffer.format(), encoding, getVorbisAudioTags(song));
            }
            case "mp3" -> {
                renderFinishedMessage.setFileName(song.getTitleOrFileName() + ".mp3");
                renderFinishedMessage.setMimeType("audio/mpeg");
                final RenderMessage.Mp3AudioFileOptions mp3AudioFileOptions = (RenderMessage.Mp3AudioFileOptions) message.getAudioFileOptions();
                final Mp3AudioOutputStream.Encoding encoding = switch (mp3AudioFileOptions.getEncoding()) {
                    case "cbr" -> Mp3AudioOutputStream.Encoding.cbr(mp3AudioFileOptions.getBitRate());
                    case "abr" -> Mp3AudioOutputStream.Encoding.abr(mp3AudioFileOptions.getBitRate());
                    case "vbr" -> Mp3AudioOutputStream.Encoding.vbr(mp3AudioFileOptions.getQuality() / 100F);
                    default -> throw new UnsupportedOperationException("Unsupported encoding: " + mp3AudioFileOptions.getEncoding());
                };
                final Mp3AudioOutputStream.Id3Tags id3Tags = new Mp3AudioOutputStream.Id3Tags()
                    .withTitle(song.getTitle())
                    .withArtist(song.getAuthor())
                    .withComment(song.getDescription())
                    .withCustomField("Renderer", "NoteBlockRenderer");
                yield new Mp3AudioOutputStream(new SeekableBufferedOutputStream(output), buffer.format(), encoding, id3Tags);
            }
            case "ogg-opus" -> {
                renderFinishedMessage.setFileName(song.getTitleOrFileName() + ".opus");
                renderFinishedMessage.setMimeType("audio/ogg");
                final RenderMessage.OggOpusAudioFileOptions oggOpusAudioFileOptions = (RenderMessage.OggOpusAudioFileOptions) message.getAudioFileOptions();
                final OggOpusAudioOutputStream.Encoding encoding = switch (oggOpusAudioFileOptions.getEncoding()) {
                    case "cbr" -> OggOpusAudioOutputStream.Encoding.cbr(oggOpusAudioFileOptions.getBitRate());
                    case "abr" -> OggOpusAudioOutputStream.Encoding.abr(oggOpusAudioFileOptions.getBitRate());
                    case "vbr" -> OggOpusAudioOutputStream.Encoding.vbr(oggOpusAudioFileOptions.getBitRate());
                    default -> throw new UnsupportedOperationException("Unsupported encoding: " + oggOpusAudioFileOptions.getEncoding());
                };
                yield OggOpusAudioOutputStream.createCompatible(new BufferedOutputStream(output), buffer.format(), encoding, OpusSignal.OPUS_SIGNAL_MUSIC, getVorbisAudioTags(song));
            }
            default -> throw new UnsupportedOperationException("Unsupported format: " + message.getAudioFileOptions().getFormat());
        }) {
            final int samplesPerBatch = buffer.format().millisToSampleCount(100F);
            for (int i = 0; i < buffer.sampleCount(); i += samplesPerBatch) {
                final int batchSize = Math.min(samplesPerBatch, buffer.sampleCount() - i);
                audioOutputStream.write(buffer.samples(), i, batchSize);
                final RenderProgressMessage renderProgressMessage = RenderProgressMessage.create();
                renderProgressMessage.setProgress(((float) (i + batchSize) / buffer.sampleCount()) * 100F);
                this.postMessage(renderProgressMessage);
            }
        }
        final byte[] audioBytes = output.toByteArray();
        renderFinishedMessage.setData(audioBytes);
        renderFinishedMessage.setFileSize(StringUtil.formatByteCount(audioBytes.length));
        this.postMessage(renderFinishedMessage);

        this.postStatusMessage("Rendering finished in " + stopwatch);
    }

    private static Map<String, List<String>> getVorbisAudioTags(final Song song) {
        final Map<String, String> tags = new LinkedHashMap<>();
        tags.put("TITLE", song.getTitle());
        tags.put("ARTIST", song.getAuthor());
        tags.put("DESCRIPTION", song.getDescription());
        tags.put("Renderer", "NoteBlockRenderer");
        return Maps.transformValues(Maps.filterValues(tags, Objects::nonNull), List::of);
    }

}
