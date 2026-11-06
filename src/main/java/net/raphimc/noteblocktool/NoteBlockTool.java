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
package net.raphimc.noteblocktool;

import net.raphimc.noteblocklib.format.minecraft.MinecraftInstrument;
import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblocktool.handler.MessageHandler;
import net.raphimc.noteblocktool.handler.impl.LoadFileMessageHandler;
import net.raphimc.noteblocktool.handler.impl.LoadNbwMessageHandler;
import net.raphimc.noteblocktool.handler.impl.RenderMessageHandler;
import net.raphimc.noteblocktool.handler.impl.SetCustomSoundsMessageHandler;
import net.raphimc.noteblocktool.handler.impl.UnloadSongMessageHandler;
import net.raphimc.noteblocktool.message.Message;
import net.raphimc.noteblocktool.message.response.WorkerReadyMessage;
import net.raphimc.noteblocktool.util.SoundLoader;
import org.teavm.jso.JSExport;
import org.teavm.jso.dom.events.MessageEvent;

import java.io.IOException;
import java.util.Map;

public class NoteBlockTool extends MessageHandler<Message> {

    private static NoteBlockTool INSTANCE;

    private final Map<String, MessageHandler<?>> messageHandlers = Map.of(
        "load-file", new LoadFileMessageHandler(),
        "load-nbw", new LoadNbwMessageHandler(),
        "unload-song", new UnloadSongMessageHandler(),
        "render", new RenderMessageHandler(),
        "set-custom-sounds", new SetCustomSoundsMessageHandler()
    );
    private SoundLoader soundLoader = new SoundLoader();
    private Song song;

    public NoteBlockTool() throws IOException {
        for (MinecraftInstrument instrument : MinecraftInstrument.values()) {
            if (!this.soundLoader.ensureLoaded(instrument)) {
                throw new IllegalStateException("Failed to load sound sample for instrument: " + instrument);
            }
        }
        this.postMessage(WorkerReadyMessage.create());
        this.postStatusMessage("Waiting for song...");
    }

    public static void main(@SuppressWarnings("unused") final String[] args) throws IOException {
        INSTANCE = new NoteBlockTool();
    }

    @JSExport
    public static void onMessage(final MessageEvent event) {
        INSTANCE.handleMessage((Message) event.getData());
    }

    public static NoteBlockTool instance() {
        return INSTANCE;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void handleMessage(final Message message) {
        final MessageHandler handler = this.messageHandlers.get(message.getType());
        if (handler != null) {
            try {
                handler.handleMessage(message);
            } catch (final Throwable e) {
                this.postErrorMessage("Error processing '" + message.getType() + "': " + e.getMessage());
            }
        } else {
            this.postErrorMessage("Unhandled message type: " + message.getType());
        }
    }

    public SoundLoader getSoundLoader() {
        return this.soundLoader;
    }

    public void setSoundLoader(final SoundLoader soundLoader) {
        this.soundLoader = soundLoader;
    }

    public Song getSong() {
        return this.song;
    }

    public void setSong(final Song song) {
        this.song = song;
    }

}
