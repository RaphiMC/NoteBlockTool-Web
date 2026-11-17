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

public class SysexMessage extends MidiMessage {

    public static final int SYSTEM_EXCLUSIVE = 0xF0;
    public static final int SPECIAL_SYSTEM_EXCLUSIVE = 0xF7;

    public SysexMessage() {
        this(new byte[2]);
        this.data[0] = (byte) SYSTEM_EXCLUSIVE;
        this.data[1] = (byte) ShortMessage.END_OF_EXCLUSIVE;
    }

    public SysexMessage(final byte[] data, final int length) throws InvalidMidiDataException {
        super(null);
        this.setMessage(data, length);
    }

    public SysexMessage(final int status, final byte[] data, final int length) throws InvalidMidiDataException {
        super(null);
        this.setMessage(status, data, length);
    }

    protected SysexMessage(final byte[] data) {
        super(data);
    }

    @Override
    public void setMessage(final byte[] data, final int length) throws InvalidMidiDataException {
        checkSysexStatus(data, length);
        super.setMessage(data, length);
    }

    public void setMessage(final int status, final byte[] data, final int length) throws InvalidMidiDataException {
        checkSysexStatus(status);
        if (length < 0 || length > data.length) {
            throw new IndexOutOfBoundsException("length out of bounds: " + length);
        }
        this.length = length + 1;
        if (this.data == null || this.data.length < this.length) {
            this.data = new byte[this.length];
        }
        this.data[0] = (byte) status;
        if (length > 0) {
            System.arraycopy(data, 0, this.data, 1, length);
        }
    }

    public byte[] getData() {
        final byte[] returnedArray = new byte[this.length - 1];
        System.arraycopy(this.data, 1, returnedArray, 0, this.length - 1);
        return returnedArray;
    }

    private static void checkSysexStatus(final byte[] data, final int length) throws InvalidMidiDataException {
        if (data.length == 0 || length == 0) {
            throw new InvalidMidiDataException("Status byte is missing");
        }
        checkSysexStatus(data[0] & 0xFF);
    }

    private static void checkSysexStatus(final int status) throws InvalidMidiDataException {
        if (status != SYSTEM_EXCLUSIVE && status != SPECIAL_SYSTEM_EXCLUSIVE) {
            throw new InvalidMidiDataException(String.format("Invalid status byte for sysex message: 0x%X", status));
        }
    }

}
