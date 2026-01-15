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

package me.lucko.spark.hytale;

import com.google.common.collect.ImmutableMap;
import com.hypixel.hytale.protocol.packets.connection.PongType;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import me.lucko.spark.common.monitor.ping.PlayerPingProvider;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HytalePlayerPingProvider implements PlayerPingProvider {

    @Override
    public Map<String, Integer> poll() {
        ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
        for (PlayerRef player : Universe.get().getPlayers()) {
            PacketHandler.PingInfo pingInfo = player.getPacketHandler().getPingInfo(PongType.Tick);
            long pingValue = pingInfo.getPingMetricSet().getLastValue();
            int pingMillis = (int) TimeUnit.MILLISECONDS.convert(pingValue, PacketHandler.PingInfo.TIME_UNIT);
            builder.put(player.getUsername(), pingMillis);
        }
        return builder.build();
    }
}
