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
package net.raphimc.noteblocktool.util;

public final class Logger {

    private Logger() {
    }

    public static void info(final String message) {
        System.out.println(getRelativeTimestamp() + ' ' + message);
    }

    public static void error(final String message) {
        System.err.println(getRelativeTimestamp() + ' ' + message);
    }

    private static String getRelativeTimestamp() {
        return '[' + formatNanos(System.nanoTime()) + "]";
    }

    private static String formatNanos(final long nanos) {
        final long millis = nanos / 1_000_000;
        final long hours = millis / 3_600_000;
        final long minutes = (millis / 60_000) % 60;
        final long seconds = (millis / 1_000) % 60;
        final long milliseconds = millis % 1_000;
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds);
    }

}
