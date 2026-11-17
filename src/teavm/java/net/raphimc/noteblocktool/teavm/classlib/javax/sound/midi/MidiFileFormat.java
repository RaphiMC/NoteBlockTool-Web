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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MidiFileFormat {

    public static final int UNKNOWN_LENGTH = -1;

    protected int type;
    protected float divisionType;
    protected int resolution;
    protected int byteLength;
    protected long microsecondLength;
    private HashMap<String, Object> properties;

    public MidiFileFormat(final int type, final float divisionType, final int resolution, final int bytes, final long microseconds) {
        this.type = type;
        this.divisionType = divisionType;
        this.resolution = resolution;
        this.byteLength = bytes;
        this.microsecondLength = microseconds;
        this.properties = null;
    }

    public MidiFileFormat(final int type, final float divisionType, final int resolution, final int bytes, final long microseconds, final Map<String, Object> properties) {
        this(type, divisionType, resolution, bytes, microseconds);
        this.properties = new HashMap<>(properties);
    }

    public int getType() {
        return this.type;
    }

    public float getDivisionType() {
        return this.divisionType;
    }

    public int getResolution() {
        return this.resolution;
    }

    public int getByteLength() {
        return this.byteLength;
    }

    public long getMicrosecondLength() {
        return this.microsecondLength;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> properties() {
        if (this.properties == null) {
            return Collections.emptyMap();
        } else {
            return Collections.unmodifiableMap((Map<String, Object>) this.properties.clone());
        }
    }

    public Object getProperty(final String key) {
        if (this.properties == null) {
            return null;
        }
        return this.properties.get(key);
    }

}
