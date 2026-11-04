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
package net.raphimc.noteblockrenderer.renderer;

import com.google.common.collect.Comparators;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.RangeSet;
import net.raphimc.audiomixer.AudioMixer;
import net.raphimc.audiomixer.automation.finite.ramp.impl.LinearRampAutomation;
import net.raphimc.audiomixer.processor.dynamics.GainProcessor;
import net.raphimc.audiomixer.processor.dynamics.LimiterProcessor;
import net.raphimc.audiomixer.resampler.Resampler;
import net.raphimc.audiomixer.resampler.impl.BlackmanSincResampler;
import net.raphimc.audiomixer.resampler.impl.CubicResampler;
import net.raphimc.audiomixer.resampler.impl.LinearResampler;
import net.raphimc.audiomixer.resampler.impl.PointResampler;
import net.raphimc.audiomixer.source.audio.AudioSource;
import net.raphimc.audiomixer.util.AudioFormat;
import net.raphimc.audiomixer.util.buffer.AudioBuffer;
import net.raphimc.audiomixer.util.buffer.AudioBufferBuilder;
import net.raphimc.audiomixer.util.functional.FloatConsumer;
import net.raphimc.noteblocklib.format.nbs.model.event.NbsSoundStopperEvent;
import net.raphimc.noteblocklib.model.event.Event;
import net.raphimc.noteblocklib.model.note.Instrument;
import net.raphimc.noteblocklib.model.note.Note;
import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblockrenderer.message.request.RenderMessage;
import net.raphimc.noteblockrenderer.renderer.source.InstrumentNoteAudioSource;
import net.raphimc.noteblockrenderer.renderer.source.NoteAudioSource;
import net.raphimc.noteblockrenderer.renderer.source.PreRenderedNoteAudioSource;
import net.raphimc.noteblockrenderer.util.Logger;
import net.raphimc.noteblockrenderer.util.StringUtil;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SongRenderer {

    private final Song song;
    private final Map<Instrument, AudioBuffer> sounds;
    private final RenderMessage.AudioFileOptions audioFileOptions;
    private final RenderMessage.PlaybackOptions playbackOptions;
    private final RenderMessage.RendererOptions rendererOptions;
    private final RenderMessage.AdvancedOptions advancedOptions;
    private final AudioMixer mixer;
    private final Supplier<Resampler> resamplerSupplier;
    private final RangeSet<Integer> includeGroups;
    private final Map<Note, AudioBuffer> preRenderedNotes = new HashMap<>();

    public SongRenderer(final Song song, final Map<Instrument, AudioBuffer> sounds, final RenderMessage.AudioFileOptions audioFileOptions, final RenderMessage.PlaybackOptions playbackOptions, final RenderMessage.RendererOptions rendererOptions, final RenderMessage.ExtraOptions extraOptions, final RenderMessage.AdvancedOptions advancedOptions) {
        this.song = song;
        this.sounds = sounds;
        this.audioFileOptions = audioFileOptions;
        this.playbackOptions = playbackOptions;
        this.rendererOptions = rendererOptions;
        this.advancedOptions = advancedOptions;
        this.mixer = new AudioMixer(new AudioFormat(this.audioFileOptions.getSampleRate(), this.audioFileOptions.getStereo() ? 2 : 1));
        this.resamplerSupplier = switch (this.rendererOptions.getResampler()) {
            case "point" -> PointResampler::new;
            case "linear" -> LinearResampler::new;
            case "cubic" -> CubicResampler::new;
            case "sinc" -> BlackmanSincResampler::new;
            default -> throw new UnsupportedOperationException("Unsupported resampler: " + this.rendererOptions.getResampler());
        };
        this.includeGroups = StringUtil.parseRange(extraOptions.getIncludeGroups(), true);
    }

    public void preRenderMostUsedNotes(final FloatConsumer progressConsumer) {
        final List<Multiset.Entry<Note>> noteGroups = this.song.getNotes().getTicks().stream()
            .map(tick -> this.song.getNotes().get(tick))
            .flatMap(Collection::stream)
            .filter(note -> this.includeGroups.contains(note.getGroupId() + 1))
            .filter(note -> this.sounds.containsKey(note.getInstrument()))
            .collect(Collectors.toCollection(HashMultiset::create)).entrySet().stream()
            .filter(entry -> entry.getCount() > 1)
            .collect(Comparators.greatest(this.advancedOptions.getPreRenderCacheSize(), Comparator.comparingInt(Multiset.Entry<Note>::getCount)));
        for (int i = 0; i < noteGroups.size(); i++) {
            final Note note = noteGroups.get(i).getElement();
            final AudioSource source = new InstrumentNoteAudioSource(this.sounds.get(note.getInstrument()), note, this.audioFileOptions.getStereo(), this.resamplerSupplier.get());
            final AudioBuffer result = new AudioBuffer(this.mixer.getFormat(), this.mixer.getFormat().millisToFrameCount(source.getRemainingMillisecondLength()));
            source.render(result);
            this.preRenderedNotes.put(note, result);
            progressConsumer.accept(((i + 1F) / noteGroups.size()) * 100F);
        }
        final float preRenderedPercentage = ((float) noteGroups.stream().mapToInt(Multiset.Entry::getCount).sum() / this.song.getNotes().getNoteCount() * 100F);
        final long usedMemory = this.preRenderedNotes.values().stream().mapToLong(buffer -> (long) buffer.sampleCount() * Float.BYTES).sum();
        Logger.info("Finished pre rendering most used notes, covering " + String.format("%.2f", preRenderedPercentage) + "% of all notes, using " + StringUtil.formatByteCount(usedMemory) + " of memory");
    }

    public AudioBuffer render(final FloatConsumer progressConsumer) {
        final int expectedSampleCount = this.mixer.getFormat().millisToSampleCount((this.song.getLengthInSeconds() + 5) * 1000F);
        final AudioBufferBuilder bufferBuilder = new AudioBufferBuilder(this.mixer.getFormat(), expectedSampleCount);
        for (int tick = 0; tick <= this.song.getNotes().getLastTick(); tick++) {
            this.executeTick(tick);
            bufferBuilder.append(this.renderTick(tick));
            progressConsumer.accept(((tick + 1F) / this.song.getNotes().getLengthInTicks()) * 100F);
        }
        while (!this.mixer.isEmpty()) {
            bufferBuilder.append(this.mixer.renderMillis(10F));
        }

        final AudioBuffer buffer = bufferBuilder.build();
        switch (this.rendererOptions.getLimiter()) {
            case "windowed" -> {
                new LimiterProcessor().process(buffer);
                buffer.multiply(this.playbackOptions.getVolume() / 100F);
                buffer.clipToUnitRange();
            }
            case "global" -> {
                buffer.limitToUnitRange();
                buffer.multiply(this.playbackOptions.getVolume() / 100F);
                buffer.clipToUnitRange();
            }
            case "none" -> buffer.multiply(this.playbackOptions.getVolume() / 100F);
            default -> throw new UnsupportedOperationException("Unsupported limiter: " + this.rendererOptions.getLimiter());
        }
        return buffer;
    }

    private void executeTick(final int tick) {
        for (Note note : this.song.getNotes().getOrEmpty(tick)) {
            if (!this.includeGroups.contains(note.getGroupId() + 1)) {
                continue;
            }
            if (this.preRenderedNotes.containsKey(note)) {
                this.mixer.add(new PreRenderedNoteAudioSource(this.preRenderedNotes.get(note), note));
            } else if (this.sounds.containsKey(note.getInstrument())) {
                this.mixer.add(new InstrumentNoteAudioSource(this.sounds.get(note.getInstrument()), note, this.audioFileOptions.getStereo(), this.resamplerSupplier.get()));
            }
        }
        for (Event event : this.song.getEvents().getOrEmpty(tick)) {
            if (event instanceof NbsSoundStopperEvent soundStopperEvent) {
                this.mixer.forEach(source -> {
                    if (source instanceof NoteAudioSource noteAudioSource && soundStopperEvent.shouldStop(noteAudioSource.getNote())) {
                        final GainProcessor gainProcessor = new GainProcessor();
                        source.processors().add(gainProcessor);
                        final LinearRampAutomation automation = new LinearRampAutomation(gainProcessor.gain(), 0F, 100F);
                        automation.finishListeners().add(_ -> this.mixer.preRenderTasks().add(() -> this.mixer.remove(source)));
                        source.automations().add(automation);
                    }
                });
            }
        }
    }

    private AudioBuffer renderTick(final int tick) {
        this.mixer.limitSourceCount(this.advancedOptions.getMaxSimultaneousSoundCount());
        float millis = 1000F / this.song.getTempoEvents().getEffectiveTempo(tick);
        if (this.playbackOptions.getTimingJitter()) {
            millis += ThreadLocalRandom.current().nextFloat(-1F, 1F);
            if (millis <= 0F) {
                millis = 0.1F;
            }
        }
        return this.mixer.renderMillis(millis);
    }

}
