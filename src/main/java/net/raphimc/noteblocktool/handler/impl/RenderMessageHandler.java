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
import com.google.common.collect.RangeSet;
import net.raphimc.audiomixer.io.AudioOutputStream;
import net.raphimc.audiomixer.io.flac.FlacAudioOutputStream;
import net.raphimc.audiomixer.io.mp3.Mp3AudioOutputStream;
import net.raphimc.audiomixer.io.ogg.opus.OggOpusAudioOutputStream;
import net.raphimc.audiomixer.io.wav.pcm.WavPcmAudioOutputStream;
import net.raphimc.audiomixer.resampler.Resampler;
import net.raphimc.audiomixer.resampler.impl.CubicResampler;
import net.raphimc.audiomixer.resampler.impl.LinearResampler;
import net.raphimc.audiomixer.resampler.impl.PointResampler;
import net.raphimc.audiomixer.util.AudioFormat;
import net.raphimc.audiomixer.util.PcmSampleEncoding;
import net.raphimc.audiomixer.util.buffer.AudioBuffer;
import net.raphimc.audiomixer.util.functional.FloatConsumer;
import net.raphimc.audiomixer.util.io.seekable.SeekableBufferedOutputStream;
import net.raphimc.audiomixer.util.io.seekable.SeekableByteArrayOutputStream;
import net.raphimc.noteblocklib.model.note.Instrument;
import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblockrenderer.AudioMixerNoteBlockRenderer;
import net.raphimc.noteblockrenderer.NoteBlockRenderer;
import net.raphimc.noteblockrenderer.provider.SoundProvider;
import net.raphimc.noteblockrenderer.util.MetadataUtil;
import net.raphimc.noteblocktool.NoteBlockTool;
import net.raphimc.noteblocktool.handler.MessageHandler;
import net.raphimc.noteblocktool.message.request.RenderMessage;
import net.raphimc.noteblocktool.message.response.RenderFinishedMessage;
import net.raphimc.noteblocktool.message.response.RenderProgressMessage;
import net.raphimc.noteblocktool.util.SoundLoader;
import net.raphimc.noteblocktool.util.StringUtil;
import org.concentus.OpusSignal;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public class RenderMessageHandler extends MessageHandler<RenderMessage> {

    @Override
    public void handleMessage(final RenderMessage message) throws IOException {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final SoundLoader soundLoader = NoteBlockTool.instance().getSoundLoader();
        final Song song = NoteBlockTool.instance().getSong();

        final AudioFormat audioFormat = new AudioFormat(message.getAudioFileOptions().getSampleRate(), message.getAudioFileOptions().getStereo() ? 2 : 1);
        final Supplier<Resampler> resamplerSupplier = switch (message.getRendererOptions().getResampler()) {
            case "point" -> PointResampler::new;
            case "linear" -> LinearResampler::new;
            case "cubic" -> CubicResampler::new;
            default -> throw new UnsupportedOperationException("Unsupported resampler: " + message.getRendererOptions().getResampler());
        };
        final RangeSet<Integer> includeGroups = StringUtil.parseRange(message.getExtraOptions().getIncludeGroups(), true);

        final AudioMixerNoteBlockRenderer.Options options = new AudioMixerNoteBlockRenderer.Options()
            .withSoundLoadExceptionHandler((instrument, e) -> {
                this.postWarningMessage("Failed to load sound sample for '" + StringUtil.getInstrumentDisplayName(instrument) + "': " + e.getMessage());
            })
            .withResamplerSupplier(resamplerSupplier)
            .withTimingJitter(message.getPlaybackOptions().getTimingJitter())
            .withMaxSimultaneousSoundCount(message.getAdvancedOptions().getMaxSimultaneousSoundCount())
            .withNoteFilter(note -> includeGroups.contains(note.getGroupId() + 1));
        final NoteBlockRenderer renderer = new NoteBlockRenderer(song, audioFormat, new SoundProvider() {
            @Override
            public InputStream getSoundInputStream(final Instrument instrument) {
                throw new UnsupportedOperationException();
            }

            @Override
            public AudioBuffer getSoundBuffer(final Instrument instrument) throws IOException {
                RenderMessageHandler.this.postStatusMessage("Loading sound sample for '" + StringUtil.getInstrumentDisplayName(instrument) + "'");
                return soundLoader.getOrThrow(instrument);
            }
        }, options);

        this.postStatusMessage("Pre rendering most used notes");
        renderer.preRenderMostUsedNotes(progress -> {
            final RenderProgressMessage renderProgressMessage = RenderProgressMessage.create();
            renderProgressMessage.setProgress(progress);
            this.postMessage(renderProgressMessage);
        }, message.getAdvancedOptions().getPreRenderCacheSize() * 1024L * 1024L);

        this.postStatusMessage("Rendering song");
        final AudioBuffer buffer = renderer.renderFully(progress -> {
            final RenderProgressMessage renderProgressMessage = RenderProgressMessage.create();
            renderProgressMessage.setProgress(progress);
            this.postMessage(renderProgressMessage);
        }, message.getPlaybackOptions().getVolume(), NoteBlockRenderer.Limiter.valueOf(message.getRendererOptions().getLimiter().toUpperCase(Locale.ROOT)));

        this.postStatusMessage("Encoding audio");
        final RenderFinishedMessage renderFinishedMessage = encodeAudio(song, buffer, message.getAudioFileOptions(), progress -> {
            final RenderProgressMessage renderProgressMessage = RenderProgressMessage.create();
            renderProgressMessage.setProgress(progress);
            this.postMessage(renderProgressMessage);
        });
        this.postMessage(renderFinishedMessage);

        this.postStatusMessage("Rendering finished in " + stopwatch);
    }

    private static RenderFinishedMessage encodeAudio(final Song song, final AudioBuffer buffer, final RenderMessage.AudioFileOptions audioFileOptions, final FloatConsumer progressConsumer) throws IOException {
        final RenderFinishedMessage renderFinishedMessage = RenderFinishedMessage.create();
        final SeekableByteArrayOutputStream output = new SeekableByteArrayOutputStream();
        try (AudioOutputStream audioOutputStream = switch (audioFileOptions.getFormat()) {
            case "wav" -> {
                renderFinishedMessage.setFileName(song.getTitleOrFileName() + ".wav");
                renderFinishedMessage.setMimeType("audio/wav");
                final RenderMessage.WavAudioFileOptions wavAudioFileOptions = (RenderMessage.WavAudioFileOptions) audioFileOptions;
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
                final RenderMessage.FlacAudioFileOptions flacAudioFileOptions = (RenderMessage.FlacAudioFileOptions) audioFileOptions;
                final FlacAudioOutputStream.Encoding encoding = new FlacAudioOutputStream.Encoding(flacAudioFileOptions.getBitsPerSample(), flacAudioFileOptions.getCompressionLevel());
                yield new FlacAudioOutputStream(new SeekableBufferedOutputStream(output), buffer.format(), encoding, getVorbisAudioTags(song));
            }
            case "mp3" -> {
                renderFinishedMessage.setFileName(song.getTitleOrFileName() + ".mp3");
                renderFinishedMessage.setMimeType("audio/mpeg");
                final RenderMessage.Mp3AudioFileOptions mp3AudioFileOptions = (RenderMessage.Mp3AudioFileOptions) audioFileOptions;
                final Mp3AudioOutputStream.Encoding encoding = switch (mp3AudioFileOptions.getEncoding()) {
                    case "cbr" -> Mp3AudioOutputStream.Encoding.cbr(mp3AudioFileOptions.getBitRate());
                    case "abr" -> Mp3AudioOutputStream.Encoding.abr(mp3AudioFileOptions.getBitRate());
                    case "vbr" -> Mp3AudioOutputStream.Encoding.vbr(mp3AudioFileOptions.getQuality() / 100F);
                    default -> throw new UnsupportedOperationException("Unsupported encoding: " + mp3AudioFileOptions.getEncoding());
                };
                final Mp3AudioOutputStream.Id3Tags id3Tags = MetadataUtil.getMp3Tags(song).withCustomField("Application", "NoteBlockTool-Web");
                yield new Mp3AudioOutputStream(new SeekableBufferedOutputStream(output), buffer.format(), encoding, id3Tags);
            }
            case "ogg-opus" -> {
                renderFinishedMessage.setFileName(song.getTitleOrFileName() + ".opus");
                renderFinishedMessage.setMimeType("audio/ogg");
                final RenderMessage.OggOpusAudioFileOptions oggOpusAudioFileOptions = (RenderMessage.OggOpusAudioFileOptions) audioFileOptions;
                final OggOpusAudioOutputStream.Encoding encoding = switch (oggOpusAudioFileOptions.getEncoding()) {
                    case "cbr" -> OggOpusAudioOutputStream.Encoding.cbr(oggOpusAudioFileOptions.getBitRate());
                    case "abr" -> OggOpusAudioOutputStream.Encoding.abr(oggOpusAudioFileOptions.getBitRate());
                    case "vbr" -> OggOpusAudioOutputStream.Encoding.vbr(oggOpusAudioFileOptions.getBitRate());
                    default -> throw new UnsupportedOperationException("Unsupported encoding: " + oggOpusAudioFileOptions.getEncoding());
                };
                yield OggOpusAudioOutputStream.createCompatible(new BufferedOutputStream(output), buffer.format(), encoding, OpusSignal.OPUS_SIGNAL_MUSIC, getVorbisAudioTags(song));
            }
            default -> throw new UnsupportedOperationException("Unsupported format: " + audioFileOptions.getFormat());
        }) {
            final int samplesPerBatch = buffer.format().millisToSampleCount(100F);
            for (int i = 0; i < buffer.sampleCount(); i += samplesPerBatch) {
                final int batchSize = Math.min(samplesPerBatch, buffer.sampleCount() - i);
                audioOutputStream.write(buffer.samples(), i, batchSize);
                progressConsumer.accept(((float) (i + batchSize) / buffer.sampleCount()) * 100F);
            }
        }
        final byte[] audioBytes = output.toByteArray();
        renderFinishedMessage.setData(audioBytes);
        renderFinishedMessage.setFileSize(StringUtil.formatByteCount(audioBytes.length));
        return renderFinishedMessage;
    }

    private static Map<String, List<String>> getVorbisAudioTags(final Song song) {
        final Map<String, List<String>> tags = MetadataUtil.getVorbisTags(song);
        tags.put("Application", List.of("NoteBlockTool-Web"));
        return tags;
    }

}
