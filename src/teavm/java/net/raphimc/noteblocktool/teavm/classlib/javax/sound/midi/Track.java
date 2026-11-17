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
package net.raphimc.noteblocktool.teavm.classlib.javax.sound.midi;

import java.util.ArrayList;
import java.util.HashSet;

public final class Track {

    private final ArrayList<MidiEvent> eventsList = new ArrayList<>();
    private final HashSet<MidiEvent> set = new HashSet<>();
    private final MidiEvent eotEvent;

    Track() {
        final MetaMessage eot = new ImmutableEndOfTrack();
        this.eotEvent = new MidiEvent(eot, 0);
        this.eventsList.add(this.eotEvent);
        this.set.add(this.eotEvent);
    }

    public boolean add(final MidiEvent event) {
        if (event == null) {
            return false;
        }
        synchronized (this.eventsList) {
            if (!this.set.contains(event)) {
                int eventsCount = this.eventsList.size();

                MidiEvent lastEvent = null;
                if (eventsCount > 0) {
                    lastEvent = this.eventsList.get(eventsCount - 1);
                }

                if (lastEvent != this.eotEvent) {
                    if (lastEvent != null) {
                        this.eotEvent.setTick(lastEvent.getTick());
                    } else {
                        this.eotEvent.setTick(0);
                    }

                    this.eventsList.add(this.eotEvent);
                    this.set.add(this.eotEvent);
                    eventsCount = this.eventsList.size();
                }

                if (isMetaEndOfTrack(event.getMessage())) {
                    if (event.getTick() > this.eotEvent.getTick()) {
                        this.eotEvent.setTick(event.getTick());
                    }
                    return true;
                }

                this.set.add(event);

                int i = eventsCount;
                for (; i > 0; i--) {
                    if (event.getTick() >= this.eventsList.get(i - 1).getTick()) {
                        break;
                    }
                }
                if (i == eventsCount) {
                    this.eventsList.set(eventsCount - 1, event);

                    if (this.eotEvent.getTick() < event.getTick()) {
                        this.eotEvent.setTick(event.getTick());
                    }

                    this.eventsList.add(this.eotEvent);
                } else {
                    this.eventsList.add(i, event);
                }
                return true;
            }
        }

        return false;
    }

    public boolean remove(final MidiEvent event) {
        synchronized (this.eventsList) {
            if (this.set.remove(event)) {
                final int i = this.eventsList.indexOf(event);
                if (i >= 0) {
                    this.eventsList.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    public MidiEvent get(final int index) throws ArrayIndexOutOfBoundsException {
        try {
            synchronized (this.eventsList) {
                return this.eventsList.get(index);
            }
        } catch (final IndexOutOfBoundsException ioobe) {
            throw new ArrayIndexOutOfBoundsException(ioobe.getMessage());
        }
    }

    public int size() {
        synchronized (this.eventsList) {
            return this.eventsList.size();
        }
    }

    public long ticks() {
        synchronized (this.eventsList) {
            return this.eventsList.isEmpty() ? 0 : this.eventsList.getLast().getTick();
        }
    }

    private static boolean isMetaEndOfTrack(final MidiMessage midiMsg) {
        if (midiMsg.getLength() != 3 || midiMsg.getStatus() != MetaMessage.META) {
            return false;
        }
        final byte[] msg = midiMsg.getMessage();
        return ((msg[1] & 0xFF) == 0x2F) && (msg[2] == 0);
    }

    private static final class ImmutableEndOfTrack extends MetaMessage {

        private ImmutableEndOfTrack() {
            super(new byte[3]);
            this.data[0] = (byte) META;
            this.data[1] = 0x2F;
            this.data[2] = 0;
        }

        @Override
        public void setMessage(final int type, final byte[] data, final int length) throws InvalidMidiDataException {
            throw new InvalidMidiDataException("cannot modify end of track message");
        }

    }

}
