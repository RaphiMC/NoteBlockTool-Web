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
            "banjo.ogg", "bass.ogg", "bassattack.ogg", "bd.ogg", "bell.ogg", "bit.ogg", "cow_bell.ogg", "didgeridoo.ogg", "flute.ogg", "guitar.ogg", "harp.ogg", "harp2.ogg", "hat.ogg",
            "icechime.ogg", "iron_xylophone.ogg", "pling.ogg", "snare.ogg", "trumpet.ogg", "trumpet_exposed.ogg", "trumpet_oxidized.ogg", "trumpet_weathered.ogg", "xylobone.ogg"
        };
    }

}
