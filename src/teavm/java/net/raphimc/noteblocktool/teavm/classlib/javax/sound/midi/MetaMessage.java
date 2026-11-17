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

public class MetaMessage extends MidiMessage {

    public static final int META = 0xFF;
    private static final long MASK = 0x7F;

    private int dataLength;

    public MetaMessage() {
        this(new byte[]{(byte) META, 0});
    }

    public MetaMessage(final int type, final byte[] data, final int length) throws InvalidMidiDataException {
        super(null);
        this.setMessage(type, data, length);
    }

    protected MetaMessage(final byte[] data) {
        super(data);
        if (data.length >= 3) {
            this.dataLength = data.length - 3;
            int pos = 2;
            while (pos < data.length && (data[pos] & 0x80) != 0) {
                this.dataLength--;
                pos++;
            }
        }
    }

    public void setMessage(final int type, final byte[] data, final int length) throws InvalidMidiDataException {
        if (type >= 128 || type < 0) {
            throw new InvalidMidiDataException("Invalid meta event with type " + type);
        }
        if ((length > 0 && length > data.length) || length < 0) {
            throw new InvalidMidiDataException("length out of bounds: " + length);
        }

        this.length = 2 + this.getVarIntLength(length) + length;
        this.dataLength = length;
        this.data = new byte[this.length];
        this.data[0] = (byte) META;
        this.data[1] = (byte) type;
        this.writeVarInt(this.data, 2, length);
        if (length > 0) {
            System.arraycopy(data, 0, this.data, this.length - this.dataLength, this.dataLength);
        }
    }

    public int getType() {
        if (this.length >= 2) {
            return this.data[1] & 0xFF;
        }
        return 0;
    }

    public byte[] getData() {
        final byte[] returnedArray = new byte[this.dataLength];
        System.arraycopy(this.data, this.length - this.dataLength, returnedArray, 0, this.dataLength);
        return returnedArray;
    }

    private int getVarIntLength(long value) {
        int length = 0;
        do {
            value >>= 7;
            length++;
        } while (value > 0);
        return length;
    }

    private void writeVarInt(final byte[] data, int off, final long value) {
        int shift = 63;
        while ((shift > 0) && ((value & (MASK << shift)) == 0)) {
            shift -= 7;
        }
        while (shift > 0) {
            data[off++] = (byte) (((value & (MASK << shift)) >> shift) | 0x80);
            shift -= 7;
        }
        data[off] = (byte) (value & MASK);
    }

}
