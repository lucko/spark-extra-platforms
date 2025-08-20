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
import org.allaymc.api.permission.Permission;

import java.util.List;

/**
 * @author IWareQ
 */
public class AllaySparkCommand extends SimpleCommand {
    private final SparkPlatform platform;

    public AllaySparkCommand(SparkPlatform platform) {
        super("spark", "spark", List.of(Permission.createForCommand("spark", "spark")));
        this.platform = platform;
    }

    /**
     * Only for client overloads
     */
    @Override
    public void prepareCommandTree(CommandTree tree) {
        var root = tree.getRoot();
        root.key("help");

        var profiler = root.key("profiler");
        profiler.key("info");
        profiler.key("open");
        profiler.key("start").enums("startFlags",
                "--timeout",
                "--thread",
                "--only-ticks-over",
                "--interval",
                "--alloc"
        ).optional().str("value");
        profiler.key("stop");
        profiler.key("cancel");

        root.key("tps");
        root.key("ping")
                .enums("pingFlags", "--player").optional()
                .playerTarget("value");

        root.key("healthreport").enums("healthreportFlags", "--upload", "--memory", "--network").optional();

        root.key("tickmonitor").enums("tickmonitorFlags",
                "--threshold",
                "--threshold-tick",
                "--without-gc"
        ).optional().str("value");

        root.key("gc");
        root.key("gcmonitor");
        root.key("heapsummary").key("--save-to-file").optional();
        root.key("heapdump")
                .key("--compress").optional()
                .str("type");

        root.key("activity")
                .key("--page").optional()
                .str("page no");
    }

    @Override
    public CommandResult execute(CommandSender sender, String[] args) {
        this.platform.executeCommand(new AllayCommandSender(sender), args);
        return CommandResult.success(null);
    }
}
