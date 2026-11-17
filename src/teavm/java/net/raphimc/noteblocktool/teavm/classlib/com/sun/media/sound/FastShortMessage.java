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
package net.raphimc.noteblocktool.teavm.classlib.com.sun.media.sound;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

final class FastShortMessage extends ShortMessage {

    private int packedMsg;

    FastShortMessage(final int packedMsg) throws InvalidMidiDataException {
        this.packedMsg = packedMsg;
        this.getDataLength(packedMsg & 0xFF);
    }

    @Override
    public byte[] getMessage() {
        final byte[] returnedArray = new byte[this.getLength()];
        for (int i = 0; i < returnedArray.length; i++) {
            returnedArray[i] = (byte) (this.packedMsg >>> (i * 8));
        }
        return returnedArray;
    }

    @Override
    public int getLength() {
        try {
            return this.getDataLength(this.packedMsg & 0xFF) + 1;
        } catch (final InvalidMidiDataException _) {
        }
        return 0;
    }

    @Override
    public void setMessage(final int status) throws InvalidMidiDataException {
        final int dataLength = this.getDataLength(status);
        if (dataLength != 0) {
            super.setMessage(status);
        }
        this.packedMsg = (this.packedMsg & 0xFFFF00) | (status & 0xFF);
    }

    @Override
    public void setMessage(final int status, final int data1, final int data2) throws InvalidMidiDataException {
        this.getDataLength(status);
        this.packedMsg = (status & 0xFF) | ((data1 & 0xFF) << 8) | ((data2 & 0xFF) << 16);
    }

    @Override
    public void setMessage(final int command, final int channel, final int data1, final int data2) throws InvalidMidiDataException {
        this.getDataLength(command);
        this.packedMsg = (command & 0xF0) | (channel & 0x0F) | ((data1 & 0xFF) << 8) | ((data2 & 0xFF) << 16);
    }

    @Override
    public int getChannel() {
        return this.packedMsg & 0x0F;
    }

    @Override
    public int getCommand() {
        return this.packedMsg & 0xF0;
    }

    @Override
    public int getData1() {
        return (this.packedMsg & 0xFF00) >> 8;
    }

    @Override
    public int getData2() {
        return (this.packedMsg & 0xFF0000) >> 16;
    }

    @Override
    public int getStatus() {
        return this.packedMsg & 0xFF;
    }

}
