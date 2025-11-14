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

import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import org.allaymc.api.server.Server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author IWareQ
 */
public class AllayPlayerPingProvider implements PlayerPingProvider {
    private final Server server;

    public AllayPlayerPingProvider(Server server) {
        this.server = server;
    }

    @Override
    public Map<String, Integer> poll() {
        Map<String, Integer> result = new HashMap<>();
        for (var player : this.server.getPlayerManager().getPlayers().values()) {
            result.put(player.getOriginName(), player.getPing());
        }
        return Collections.unmodifiableMap(result);
    }
}
