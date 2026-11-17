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

import java.util.Vector;

public class Sequence {

    public static final float PPQ = 0.0f;
    public static final float SMPTE_24 = 24.0f;
    public static final float SMPTE_25 = 25.0f;
    public static final float SMPTE_30DROP = 29.97f;
    public static final float SMPTE_30 = 30.0f;

    protected final Vector<Track> tracks = new Vector<>();
    protected float divisionType;
    protected int resolution;

    public Sequence(final float divisionType, final int resolution) throws InvalidMidiDataException {
        if (divisionType == PPQ) {
            this.divisionType = PPQ;
        } else if (divisionType == SMPTE_24) {
            this.divisionType = SMPTE_24;
        } else if (divisionType == SMPTE_25) {
            this.divisionType = SMPTE_25;
        } else if (divisionType == SMPTE_30DROP) {
            this.divisionType = SMPTE_30DROP;
        } else if (divisionType == SMPTE_30) {
            this.divisionType = SMPTE_30;
        } else {
            throw new InvalidMidiDataException("Unsupported division type: " + divisionType);
        }
        this.resolution = resolution;
    }

    public Sequence(final float divisionType, final int resolution, final int numTracks) throws InvalidMidiDataException {
        this(divisionType, resolution);
        for (int i = 0; i < numTracks; i++) {
            this.tracks.addElement(new Track());
        }
    }

    public float getDivisionType() {
        return this.divisionType;
    }

    public int getResolution() {
        return this.resolution;
    }

    public Track createTrack() {
        final Track track = new Track();
        this.tracks.addElement(track);
        return track;
    }

    public boolean deleteTrack(final Track track) {
        return this.tracks.removeElement(track);
    }

    public Track[] getTracks() {
        return this.tracks.toArray(new Track[0]);
    }

    public long getTickLength() {
        long length = 0;
        synchronized (this.tracks) {
            for (int i = 0; i < this.tracks.size(); i++) {
                length = Math.max(this.tracks.elementAt(i).ticks(), length);
            }
        }
        return length;
    }

    public Patch[] getPatchList() {
        return new Patch[0];
    }

}
