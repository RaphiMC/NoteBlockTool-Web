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
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public final class StandardMidiFileReader {

    private static final int MTHD_MAGIC = 0x4D546864;

    private MidiFileFormat getMidiFileFormatFromStream(final InputStream stream, final int fileLength, final SmfParser smfParser) throws InvalidMidiDataException, IOException {
        final int maxReadLength = 16;
        final int duration = MidiFileFormat.UNKNOWN_LENGTH;
        final DataInputStream dis;

        if (stream instanceof DataInputStream dataInputStream) {
            dis = dataInputStream;
        } else {
            dis = new DataInputStream(stream);
        }
        if (smfParser == null) {
            dis.mark(maxReadLength);
        } else {
            smfParser.stream = dis;
        }

        final int type;
        final int numTracks;
        final float divisionType;
        final int resolution;
        try {
            final int magic = dis.readInt();
            if (magic != MTHD_MAGIC) {
                throw new InvalidMidiDataException("not a valid MIDI file");
            }

            final int bytesRemaining = dis.readInt() - 6;
            type = dis.readShort();
            numTracks = dis.readShort();
            final int timing = dis.readShort();
            if (timing > 0) {
                divisionType = Sequence.PPQ;
                resolution = timing;
            } else {
                final int frameCode = -(timing >> 8);
                divisionType = switch (frameCode) {
                    case 24 -> Sequence.SMPTE_24;
                    case 25 -> Sequence.SMPTE_25;
                    case 29 -> Sequence.SMPTE_30DROP;
                    case 30 -> Sequence.SMPTE_30;
                    default -> throw new InvalidMidiDataException("Unknown frame code: " + frameCode);
                };
                resolution = timing & 0xFF;
            }
            if (smfParser != null) {
                dis.skip(bytesRemaining);
                smfParser.tracks = numTracks;
            }
        } finally {
            if (smfParser == null) {
                dis.reset();
            }
        }
        return new MidiFileFormat(type, divisionType, resolution, fileLength, duration);
    }

    public Sequence getSequence(final InputStream stream) throws InvalidMidiDataException, IOException {
        final SmfParser smfParser = new SmfParser();
        final MidiFileFormat format = this.getMidiFileFormatFromStream(stream, MidiFileFormat.UNKNOWN_LENGTH, smfParser);
        if ((format.getType() != 0) && (format.getType() != 1)) {
            throw new InvalidMidiDataException("Invalid or unsupported file type: " + format.getType());
        }

        final Sequence sequence = new Sequence(format.getDivisionType(), format.getResolution());
        for (int i = 0; i < smfParser.tracks; i++) {
            if (smfParser.nextTrack()) {
                smfParser.readTrack(sequence.createTrack());
            } else {
                break;
            }
        }
        return sequence;
    }

    private static final class SmfParser {

        private static final int MTRK_MAGIC = 0x4D54726B;

        int tracks;
        DataInputStream stream;

        private int trackLength = 0;
        private byte[] trackData = null;
        private int pos = 0;

        SmfParser() {
        }

        private int readUnsigned() {
            return this.trackData[this.pos++] & 0xFF;
        }

        private void read(final byte[] data) {
            System.arraycopy(this.trackData, this.pos, data, 0, data.length);
            this.pos += data.length;
        }

        private long readVarInt() {
            long value = 0;
            int currentByte;
            do {
                currentByte = this.trackData[this.pos++] & 0xFF;
                value = (value << 7) + (currentByte & 0x7F);
            } while ((currentByte & 0x80) != 0);
            return value;
        }

        private int readIntFromStream() throws IOException {
            try {
                return this.stream.readInt();
            } catch (final EOFException _) {
                throw new EOFException("invalid MIDI file");
            }
        }

        boolean nextTrack() throws IOException {
            int magic;
            this.trackLength = 0;
            do {
                if (this.stream.skipBytes(this.trackLength) != this.trackLength) {
                    return false;
                }
                magic = this.readIntFromStream();
                this.trackLength = this.readIntFromStream();
            } while (magic != MTRK_MAGIC);
            if (this.trackLength < 0) {
                return false;
            }

            try {
                this.trackData = new byte[this.trackLength];
            } catch (final OutOfMemoryError oom) {
                throw new IOException("Track length too big", oom);
            }
            try {
                this.stream.readFully(this.trackData);
            } catch (final EOFException _) {
                return false;
            }
            this.pos = 0;
            return true;
        }

        private boolean trackFinished() {
            return this.pos >= this.trackLength;
        }

        void readTrack(final Track track) throws IOException, InvalidMidiDataException {
            try {
                long tick = 0;
                int runningStatus = 0;
                boolean endOfTrackFound = false;
                while (!this.trackFinished() && !endOfTrackFound) {
                    final MidiMessage message;
                    int data1 = -1;

                    tick += this.readVarInt();
                    final int byteValue = this.readUnsigned();
                    final int status;
                    if (byteValue >= 0x80) {
                        status = byteValue;
                        if ((status & 0xF0) != 0xF0) {
                            runningStatus = status;
                        }
                    } else {
                        status = runningStatus;
                        data1 = byteValue;
                    }

                    switch (status & 0xF0) {
                        case 0x80, 0x90, 0xA0, 0xB0, 0xE0 -> {
                            if (data1 == -1) {
                                data1 = this.readUnsigned();
                            }
                            final int data2 = this.readUnsigned();
                            message = new FastShortMessage(status | (data1 << 8) | (data2 << 16));
                        }
                        case 0xC0, 0xD0 -> {
                            if (data1 == -1) {
                                data1 = this.readUnsigned();
                            }
                            message = new FastShortMessage(status | (data1 << 8));
                        }
                        case 0xF0 -> {
                            switch (status) {
                                case 0xF0, 0xF7 -> {
                                    final int sysexLength = (int) this.readVarInt();
                                    if (sysexLength < 0 || sysexLength > this.trackLength - this.pos) {
                                        throw new InvalidMidiDataException("Message length is out of bounds: " + sysexLength);
                                    }
                                    final byte[] sysexData = new byte[sysexLength];
                                    this.read(sysexData);

                                    final SysexMessage sysexMessage = new SysexMessage();
                                    sysexMessage.setMessage(status, sysexData, sysexLength);
                                    message = sysexMessage;
                                }
                                case 0xFF -> {
                                    final int metaType = this.readUnsigned();
                                    final int metaLength = (int) this.readVarInt();
                                    if (metaLength < 0 || metaLength > this.trackLength - this.pos) {
                                        throw new InvalidMidiDataException("Message length is out of bounds: " + metaLength);
                                    }
                                    final byte[] metaData;
                                    try {
                                        metaData = new byte[metaLength];
                                    } catch (final OutOfMemoryError oom) {
                                        throw new IOException("Meta length too big", oom);
                                    }
                                    this.read(metaData);

                                    final MetaMessage metaMessage = new MetaMessage();
                                    metaMessage.setMessage(metaType, metaData, metaLength);
                                    message = metaMessage;
                                    if (metaType == 0x2F) {
                                        endOfTrackFound = true;
                                    }
                                }
                                default -> throw new InvalidMidiDataException("Invalid status byte: " + status);
                            }
                        }
                        default -> throw new InvalidMidiDataException("Invalid status byte: " + status);
                    }
                    track.add(new MidiEvent(message, tick));
                }
            } catch (final ArrayIndexOutOfBoundsException _) {
                throw new EOFException("invalid MIDI file");
            }
        }

    }

}
