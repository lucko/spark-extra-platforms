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

import me.lucko.spark.common.platform.PlatformInfo;
import org.allaymc.api.network.ProtocolInfo;

import java.lang.reflect.InvocationTargetException;

/**
 * @author IWareQ
 */
public class AllayPlatformInfo implements PlatformInfo {

    @Override
    public Type getType() {
        return Type.SERVER;
    }

    @Override
    public String getName() {
        return "Allay";
    }

    @Override
    public String getBrand() {
        return "Allay";
    }

    @Override
    public String getVersion() {
        try {
            var gitProperties = Class.forName("org.allaymc.server.utils.GitProperties");
            var getBuildApiVersion = gitProperties.getMethod("getBuildApiVersion");
            return String.valueOf(getBuildApiVersion.invoke(null));
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getMinecraftVersion() {
        return ProtocolInfo.getMinecraftVersionStr();
    }
}
