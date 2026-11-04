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
package net.raphimc.noteblockrenderer.message.request;

import net.raphimc.noteblockrenderer.message.Message;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public interface RenderMessage extends Message {

    @JSProperty
    AudioFileOptions getAudioFileOptions();

    @JSProperty
    PlaybackOptions getPlaybackOptions();

    @JSProperty
    RendererOptions getRendererOptions();

    @JSProperty
    ExtraOptions getExtraOptions();

    @JSProperty
    AdvancedOptions getAdvancedOptions();

    interface AudioFileOptions extends JSObject {

        @JSProperty
        String getFormat();

        @JSProperty
        int getSampleRate();

        @JSProperty
        int getChannels();

    }

    interface WavAudioFileOptions extends AudioFileOptions {

        @JSProperty
        String getEncoding();

    }

    interface FlacAudioFileOptions extends AudioFileOptions {

        @JSProperty
        int getBitsPerSample();

        @JSProperty
        int getCompressionLevel();

    }

    interface Mp3AudioFileOptions extends AudioFileOptions {

        @JSProperty
        String getEncoding();

        @JSProperty
        int getBitRate();

        @JSProperty
        int getQuality();

    }

    interface OggOpusAudioFileOptions extends AudioFileOptions {

        @JSProperty
        String getEncoding();

        @JSProperty
        int getBitRate();

    }

    interface PlaybackOptions extends JSObject {

        @JSProperty
        int getVolume();

        @JSProperty
        boolean getTimingJitter();

    }

    interface RendererOptions extends JSObject {

        @JSProperty
        String getResampler();

        @JSProperty
        String getLimiter();

    }

    interface ExtraOptions extends JSObject {

        @JSProperty
        String getIncludeGroups();

    }

    interface AdvancedOptions extends JSObject {

        @JSProperty
        int getMaxSimultaneousSoundCount();

        @JSProperty
        int getPreRenderCacheSize();

    }

}
