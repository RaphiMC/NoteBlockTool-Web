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
package net.raphimc.noteblocktool.teavm.classlib.java.util;

public class UUID {

    private String value;

    public UUID(final long mostSigBits, final long leastSigBits) {
        this.value = digits(mostSigBits >>> 32, 8) + "-"
            + digits(mostSigBits >>> 16, 4) + "-"
            + digits(mostSigBits, 4) + "-"
            + digits(leastSigBits >>> 48, 4) + "-"
            + digits(leastSigBits, 12);
    }

    private static String digits(final long value, final int count) {
        final long leadingBit = 1L << (count * 4);
        return Long.toHexString(leadingBit | (value & (leadingBit - 1))).substring(1);
    }

}
