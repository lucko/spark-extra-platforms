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

import me.lucko.spark.common.SparkPlatform;
import org.allaymc.api.command.CommandResult;
import org.allaymc.api.command.CommandSender;
import org.allaymc.api.command.SimpleCommand;
import org.allaymc.api.command.tree.CommandTree;

/**
 * @author IWareQ
 */
public class AllaySparkCommand extends SimpleCommand {
    private final SparkPlatform platform;

    public AllaySparkCommand(SparkPlatform platform) {
        super("spark", "spark");
        this.platform = platform;
    }

    // only for game overloads
    @Override
    public void prepareCommandTree(CommandTree tree) {
        tree.getRoot()
                .key("help").root()
                .key("profiler")
                .key("info").up()
                .key("open").up()
                .key("start").enums("startFlags",
                        "--timeout",
                        "--thread",
                        "--only-ticks-over",
                        "--interval",
                        "--alloc"
                ).str("value").optional().up(3)
                .key("stop").up()
                .key("cancel").root()
                .key("tps").root()
                .key("ping").enums("pingFlags", "--player").optional().str("value").root()
                .key("healthreport").enums("healthreportFlags", "--memory", "--network").optional().str("value").optional().root()
                .key("tickmonitor").enums("tickmonitorFlags",
                        "--threshold",
                        "--threshold-tick",
                        "--without-gc"
                ).optional().str("value").optional().optional().root()
                .key("gc").root()
                .key("gcmonitor").root()
                .key("heapsummary").enums("heapsummaryFlags", "--save-to-file").optional().str("value").root()
                .key("heapdump").enums("heapdumpFlags", "--compress").str("value").root()
                .key("activity").enums("activityFlags", "--page").str("value");
    }

    @Override
    public CommandResult execute(CommandSender sender, String[] args) {
        this.platform.executeCommand(new AllayCommandSender(sender), args);
        return CommandResult.success(null);
    }
}
