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
package net.raphimc.noteblocktool.teavm.extension;

import org.teavm.extension.Autoregistered;
import org.teavm.extension.spi.resources.DefaultResourcesPolicy;

import java.util.Collection;

@Autoregistered
public class ResourcesPolicyImpl extends DefaultResourcesPolicy {

    @Override
    public String[] supplyResources(final Collection<? extends String> availableClassNames) {
        return new String[]{
            "minecraft/note/banjo.ogg", "minecraft/note/bass.ogg", "minecraft/note/bassattack.ogg", "minecraft/note/bd.ogg", "minecraft/note/bell.ogg", "minecraft/note/bit.ogg",
            "minecraft/note/cow_bell.ogg", "minecraft/note/didgeridoo.ogg", "minecraft/note/flute.ogg", "minecraft/note/guitar.ogg", "minecraft/note/harp.ogg",
            "minecraft/note/harp2.ogg", "minecraft/note/hat.ogg", "minecraft/note/icechime.ogg", "minecraft/note/iron_xylophone.ogg", "minecraft/note/pling.ogg",
            "minecraft/note/snare.ogg", "minecraft/note/trumpet.ogg", "minecraft/note/trumpet_exposed.ogg", "minecraft/note/trumpet_oxidized.ogg", "minecraft/note/trumpet_weathered.ogg",
            "minecraft/note/xylobone.ogg"
        };
    }

}
