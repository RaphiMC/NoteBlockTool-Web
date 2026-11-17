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

public class ShortMessage extends MidiMessage {

    public static final int MIDI_TIME_CODE = 0xF1;
    public static final int SONG_POSITION_POINTER = 0xF2;
    public static final int SONG_SELECT = 0xF3;
    public static final int TUNE_REQUEST = 0xF6;
    public static final int END_OF_EXCLUSIVE = 0xF7;
    public static final int TIMING_CLOCK = 0xF8;
    public static final int START = 0xFA;
    public static final int CONTINUE = 0xFB;
    public static final int STOP = 0xFC;
    public static final int ACTIVE_SENSING = 0xFE;
    public static final int SYSTEM_RESET = 0xFF;
    public static final int NOTE_OFF = 0x80;
    public static final int NOTE_ON = 0x90;
    public static final int POLY_PRESSURE = 0xA0;
    public static final int CONTROL_CHANGE = 0xB0;
    public static final int PROGRAM_CHANGE = 0xC0;
    public static final int CHANNEL_PRESSURE = 0xD0;
    public static final int PITCH_BEND = 0xE0;

    public ShortMessage() {
        this(new byte[3]);
        this.data[0] = (byte) NOTE_ON;
        this.data[1] = 64;
        this.data[2] = 127;
        this.length = 3;
    }

    public ShortMessage(final int status) throws InvalidMidiDataException {
        super(null);
        this.setMessage(status);
    }

    public ShortMessage(final int status, final int data1, final int data2) throws InvalidMidiDataException {
        super(null);
        this.setMessage(status, data1, data2);
    }

    public ShortMessage(final int command, final int channel, final int data1, final int data2) throws InvalidMidiDataException {
        super(null);
        this.setMessage(command, channel, data1, data2);
    }

    protected ShortMessage(final byte[] data) {
        super(data);
    }

    public void setMessage(final int status) throws InvalidMidiDataException {
        final int dataLength = this.getDataLength(status);
        if (dataLength != 0) {
            throw new InvalidMidiDataException("Status byte; " + status + " requires " + dataLength + " data bytes");
        }
        this.setMessage(status, 0, 0);
    }

    public void setMessage(final int status, final int data1, final int data2) throws InvalidMidiDataException {
        final int dataLength = this.getDataLength(status);
        if (dataLength > 0) {
            if (data1 < 0 || data1 > 127) {
                throw new InvalidMidiDataException("data1 out of range: " + data1);
            }
            if (dataLength > 1 && (data2 < 0 || data2 > 127)) {
                throw new InvalidMidiDataException("data2 out of range: " + data2);
            }
        }
        this.length = dataLength + 1;
        if (this.data == null || this.data.length < this.length) {
            this.data = new byte[3];
        }
        this.data[0] = (byte) status;
        if (this.length > 1) {
            this.data[1] = (byte) data1;
            if (this.length > 2) {
                this.data[2] = (byte) data2;
            }
        }
    }

    public void setMessage(final int command, final int channel, final int data1, final int data2) throws InvalidMidiDataException {
        if (command >= 0xF0 || command < 0x80) {
            throw new InvalidMidiDataException("command out of range: 0x" + Integer.toHexString(command));
        }
        if ((channel & 0xFFFFFFF0) != 0) {
            throw new InvalidMidiDataException("channel out of range: " + channel);
        }
        this.setMessage((command & 0xF0) | (channel & 0x0F), data1, data2);
    }

    public int getChannel() {
        return (this.getStatus() & 0x0F);
    }

    public int getCommand() {
        return (this.getStatus() & 0xF0);
    }

    public int getData1() {
        if (this.length > 1) {
            return (this.data[1] & 0xFF);
        }
        return 0;
    }

    public int getData2() {
        if (this.length > 2) {
            return (this.data[2] & 0xFF);
        }
        return 0;
    }

    protected final int getDataLength(final int status) throws InvalidMidiDataException {
        return switch (status) {
            case 0xF6, 0xF7, 0xF8, 0xF9, 0xFA, 0xFB, 0xFC, 0xFD, 0xFE, 0xFF -> 0;
            case 0xF1, 0xF3 -> 1;
            case 0xF2 -> 2;
            default -> switch (status & 0xF0) {
                case 0x80, 0x90, 0xA0, 0xB0, 0xE0 -> 2;
                case 0xC0, 0xD0 -> 1;
                default -> throw new InvalidMidiDataException("Invalid status byte: " + status);
            };
        };
    }

}
