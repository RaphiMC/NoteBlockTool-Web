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

public abstract class MidiMessage {

    protected byte[] data;
    protected int length;

    protected MidiMessage(final byte[] data) {
        this.data = data;
        if (data != null) {
            this.length = data.length;
        }
    }

    protected void setMessage(final byte[] data, final int length) throws InvalidMidiDataException {
        if (length < 0 || (length > 0 && length > data.length)) {
            throw new IndexOutOfBoundsException("length out of bounds: " + length);
        }
        this.length = length;

        if (this.data == null || this.data.length < this.length) {
            this.data = new byte[this.length];
        }
        System.arraycopy(data, 0, this.data, 0, length);
    }

    public byte[] getMessage() {
        final byte[] returnedArray = new byte[this.length];
        System.arraycopy(this.data, 0, returnedArray, 0, this.length);
        return returnedArray;
    }

    public int getStatus() {
        if (this.length > 0) {
            return (this.data[0] & 0xFF);
        }
        return 0;
    }

    public int getLength() {
        return this.length;
    }

}
