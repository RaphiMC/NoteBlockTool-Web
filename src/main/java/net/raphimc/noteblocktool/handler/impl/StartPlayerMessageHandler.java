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

import net.raphimc.audiomixer.resampler.Resampler;
import net.raphimc.audiomixer.resampler.impl.CubicResampler;
import net.raphimc.audiomixer.resampler.impl.LinearResampler;
import net.raphimc.audiomixer.resampler.impl.PointResampler;
import net.raphimc.audiomixer.util.JavaAudioFormatUtil;
import net.raphimc.audiomixer.util.PcmSampleEncoding;
import net.raphimc.audiomixer.util.buffer.AudioBuffer;
import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblockrenderer.AudioMixerNoteBlockRenderer;
import net.raphimc.noteblockrenderer.NoteBlockPlayer;
import net.raphimc.noteblockrenderer.provider.SoundProvider;
import net.raphimc.noteblockrenderer.provider.impl.NoOpSoundProvider;
import net.raphimc.noteblockrenderer.provider.impl.ResourceSoundProvider;
import net.raphimc.noteblockrenderer.provider.impl.UrlSoundProvider;
import net.raphimc.noteblocktool.NoteBlockTool;
import net.raphimc.noteblocktool.handler.MessageHandler;
import net.raphimc.noteblocktool.message.backend.BackendPlayerStateMessage;
import net.raphimc.noteblocktool.message.backend.StatusMessage;
import net.raphimc.noteblocktool.message.frontend.StartPlayerMessage;
import net.raphimc.noteblocktool.util.StringUtil;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.function.Supplier;

public class StartPlayerMessageHandler extends MessageHandler<StartPlayerMessage> {

    private static final AudioFormat AUDIO_FORMAT = JavaAudioFormatUtil.getJavaAudioFormat(48000, 2, PcmSampleEncoding.F32_BE);

    public StartPlayerMessageHandler() {
        super(StatusMessage.CONTEXT_PLAYER);
    }

    @Override
    public void handleMessage(final StartPlayerMessage message) throws LineUnavailableException {
        this.postStatusMessage("Initializing player");
        final Supplier<Resampler> resamplerSupplier = switch (message.getRendererOptions().getResampler()) {
            case "point" -> PointResampler::new;
            case "linear" -> LinearResampler::new;
            case "cubic" -> CubicResampler::new;
            default -> throw new UnsupportedOperationException("Unsupported resampler: " + message.getRendererOptions().getResampler());
        };
        final AudioMixerNoteBlockRenderer.Options options = new AudioMixerNoteBlockRenderer.Options()
            .withSoundLoadExceptionHandler((instrument, e) -> {
                this.postWarningMessage("Failed to load sound sample for '" + StringUtil.getInstrumentDisplayName(instrument) + "': " + e.getMessage());
            })
            .withResamplerSupplier(resamplerSupplier)
            .withTimingJitter(message.getPlaybackOptions().getTimingJitter())
            .withMaxSimultaneousSoundCount(message.getAdvancedOptions().getMaxSimultaneousSoundCount());
        SoundProvider soundProvider = new ResourceSoundProvider().then(new UrlSoundProvider()).then(new NoOpSoundProvider());
        if (NoteBlockTool.instance().getBundledSoundProvider() != null) {
            soundProvider = NoteBlockTool.instance().getBundledSoundProvider().then(soundProvider);
        }
        if (NoteBlockTool.instance().getCustomSoundProvider() != null) {
            soundProvider = NoteBlockTool.instance().getCustomSoundProvider().then(soundProvider);
        }
        final NoteBlockPlayer player = new WebSongPlayer(AudioSystem.getSourceDataLine(AUDIO_FORMAT), NoteBlockTool.instance().getSong(), soundProvider, options, message.getAdvancedOptions().getAudioBufferMillis());
        NoteBlockTool.instance().setPlayer(player);

        this.postStatusMessage("Pre rendering the most used notes");
        player.preRenderMostUsedNotes(_ -> {
        }, message.getAdvancedOptions().getPreRenderCacheSize() * 1024L * 1024L);
    }

    private final class WebSongPlayer extends NoteBlockPlayer {

        private WebSongPlayer(final SourceDataLine sourceDataLine, final Song song, final SoundProvider soundProvider, final Options options, final int audioBufferMillis) throws LineUnavailableException {
            super(sourceDataLine, song, soundProvider, options, audioBufferMillis);
        }

        @Override
        public AudioBuffer tick() {
            final AudioBuffer buffer = super.tick();
            final BackendPlayerStateMessage backendPlayerStateMessage = BackendPlayerStateMessage.create();
            backendPlayerStateMessage.setPaused(this.isPaused());
            backendPlayerStateMessage.setProgress(this.getProgress());
            backendPlayerStateMessage.setHumanReadablePosition(this.getHumanReadablePosition());
            backendPlayerStateMessage.setActiveSoundCount(this.getMixer().getMixedSourceCount());
            backendPlayerStateMessage.setProcessingLoad(this.getSourceDataLineWriter().getProcessingLoad());
            StartPlayerMessageHandler.this.postMessage(backendPlayerStateMessage);
            Thread.yield(); // Give TeaVM a chance to process the message queue to keep the controls responsive
            return buffer;
        }

    }

}
