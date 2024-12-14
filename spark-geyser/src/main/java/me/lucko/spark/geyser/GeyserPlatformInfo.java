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

package me.lucko.spark.geyser;

import me.lucko.spark.common.platform.PlatformInfo;
import org.geysermc.api.util.ApiVersion;
import org.geysermc.geyser.api.GeyserApi;

public class GeyserPlatformInfo implements PlatformInfo {
    private final GeyserApi geyserApi;

    public GeyserPlatformInfo(GeyserApi geyserApi) {
        this.geyserApi = geyserApi;
    }

    @Override
    public Type getType() {
        return Type.SERVER;
    }

    @Override
    public String getName() {
        return "Geyser";
    }

    @Override
    public String getBrand() {
        return "Geyser";
    }

    @Override
    public String getVersion() {
        ApiVersion version = this.geyserApi.geyserApiVersion();
        return version.human() + "." + version.major() + "." + version.minor();
    }

    @Override
    public String getMinecraftVersion() {
        return this.geyserApi.supportedJavaVersion().versionString();
    }
}
