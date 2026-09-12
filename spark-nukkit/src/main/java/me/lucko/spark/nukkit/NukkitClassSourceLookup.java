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

package me.lucko.spark.nukkit;

import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginClassLoader;
import cn.nukkit.plugin.PluginManager;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;

import java.util.HashMap;
import java.util.Map;

public class NukkitClassSourceLookup extends ClassSourceLookup.ByClassLoader {

    private final Map<ClassLoader, String> pluginClassLoaders;

    public NukkitClassSourceLookup(PluginManager pluginManager) {
        this.pluginClassLoaders = new HashMap<>();
        for (Plugin plugin : pluginManager.getPlugins().values()) {
            this.pluginClassLoaders.put(plugin.getClass().getClassLoader(), plugin.getName());
        }
    }

    @Override
    public String identify(ClassLoader loader) {
        if (loader instanceof PluginClassLoader) {
            return this.pluginClassLoaders.get(loader);
        }
        return null;
    }
}
