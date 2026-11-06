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

import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import net.raphimc.noteblocklib.format.minecraft.MinecraftInstrument;
import net.raphimc.noteblocklib.format.nbs.model.NbsCustomInstrument;
import net.raphimc.noteblocklib.model.note.Instrument;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public final class StringUtil {

    private StringUtil() {
    }

    public static String getInstrumentDisplayName(final Instrument instrument) {
        return switch (instrument) {
            case MinecraftInstrument minecraftInstrument -> Arrays.stream(minecraftInstrument.name().split("_"))
                .map(word -> word.charAt(0) + word.substring(1).toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));
            case NbsCustomInstrument customInstrument -> customInstrument.getNameOr("No Name") + " (" + customInstrument.getSoundFilePathOr("No Sound File") + ")";
            default -> throw new IllegalArgumentException("Unsupported instrument type: " + instrument.getClass().getName());
        };
    }

    public static RangeSet<Integer> parseRange(final String input, final boolean defaultToAll) {
        if (!input.isBlank()) {
            final RangeSet<Integer> result = TreeRangeSet.create();
            for (String subRange : input.split(",")) {
                final String[] bounds = subRange.split("-");
                final int start = Integer.parseInt(bounds[0].trim());
                final int end = bounds.length == 2 ? Integer.parseInt(bounds[1].trim()) : start;
                result.add(Range.closed(start, end).canonical(DiscreteDomain.integers()));
            }
            return ImmutableRangeSet.copyOf(result);
        } else {
            return defaultToAll ? ImmutableRangeSet.of(Range.all()) : ImmutableRangeSet.of();
        }
    }

    public static String formatByteCount(final long byteCount) {
        if (byteCount < 1024) {
            return byteCount + " B";
        }
        final int exp = (63 - Long.numberOfLeadingZeros(byteCount)) / 10;
        return String.format(Locale.ROOT, "%.1f %ciB", byteCount / Math.pow(1024, exp), "KMGTPE".charAt(exp - 1));
    }

}
