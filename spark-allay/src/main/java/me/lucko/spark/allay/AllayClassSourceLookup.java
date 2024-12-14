/*
 * This file is part of spark.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.lucko.spark.allay;

import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import org.allaymc.api.plugin.PluginManager;

import java.util.HashMap;
import java.util.Map;

/**
 * @author IWareQ
 */
public class AllayClassSourceLookup extends ClassSourceLookup.ByFirstUrlSource {

    private final Map<ClassLoader, String> classLoaders2PluginName = new HashMap<>();

    public AllayClassSourceLookup(PluginManager manager) {
        manager.getEnabledPlugins().values().forEach(container -> classLoaders2PluginName.put(
                container.plugin().getClass().getClassLoader(),
                container.descriptor().getName())
        );
    }

    @Override
    public String identify(ClassLoader loader) {
        if (!this.classLoaders2PluginName.containsKey(loader)) {
            return null;
        }

        return this.classLoaders2PluginName.get(loader);
    }
}
