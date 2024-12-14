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

import me.lucko.spark.common.tick.AbstractTickHook;
import me.lucko.spark.common.tick.TickHook;
import org.allaymc.api.scheduler.Task;
import org.allaymc.api.server.Server;

import java.util.concurrent.atomic.AtomicBoolean;

/*
 * @author IWareQ
 */
public class AllayTickHook extends AbstractTickHook implements TickHook, Task {

    private final AtomicBoolean isEnabled = new AtomicBoolean(true);

    private final AllaySparkPlugin plugin;

    public AllayTickHook(AllaySparkPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onRun() {
        this.onTick();
        return this.isEnabled.get();
    }

    @Override
    public void start() {
        Server.getInstance().getScheduler().scheduleRepeating(this.plugin, this, 1);
    }

    @Override
    public void close() {
        this.isEnabled.compareAndSet(true, false);
    }
}
