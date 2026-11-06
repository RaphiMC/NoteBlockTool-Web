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
package net.raphimc.noteblocktool.teavm.plugin.transformer;

import net.raphimc.noteblocktool.teavm.extension.SubstitutionPolicyImpl;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReader;
import org.teavm.model.ReferenceCache;
import org.teavm.model.util.ModelUtils;
import org.teavm.parsing.ClassRefsRenamer;

import java.util.HashMap;
import java.util.Map;

public class MethodSubstitutionTransformer extends AbstractDependencyListener implements ClassHolderTransformer {

    private static final String CLASSLIB_PREFIX = SubstitutionPolicyImpl.CLASSLIB_PACKAGE + ".";
    private final ReferenceCache referenceCache = new ReferenceCache();
    private final Map<String, ClassReader> preparedReplacements = new HashMap<>();
    private ClassReaderSource processedClasses;

    @Override
    public void started(final DependencyAgent agent) {
        this.processedClasses = agent.getClassSource();
    }

    @Override
    public void transformClass(final ClassHolder cls, final ClassHolderTransformerContext context) {
        final ClassReaderSource classes = context.getHierarchy().getClassSource();
        final ClassRefsRenamer renamer = new ClassRefsRenamer(this.referenceCache, name -> {
            return name.startsWith(CLASSLIB_PREFIX) && classes.get(name) != null ? name.substring(CLASSLIB_PREFIX.length()) : name;
        });
        if (cls.getName().startsWith(CLASSLIB_PREFIX)) {
            if (classes.get(cls.getName()) == null) {
                renamer.rename(cls);
            } else {
                this.preparedReplacements.put(cls.getName(), ModelUtils.copyClass(cls));
            }
            return;
        }

        final String replacementName = CLASSLIB_PREFIX + cls.getName();
        context.getIncrementalCache().addDependencies(cls.getName(), replacementName);
        if (classes.get(replacementName) == null) {
            return;
        }
        this.processedClasses.get(replacementName);
        final ClassReader replacement = this.preparedReplacements.get(replacementName);
        for (MethodReader method : replacement.getMethods()) {
            if (!method.getName().equals("<clinit>") && !(method.getName().equals("<init>") && method.parameterCount() == 0)) {
                cls.addMethod(renamer.rename(ModelUtils.copyMethod(method)));
            }
        }
    }

}
