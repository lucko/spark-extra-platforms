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

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.ParserContext;
import me.lucko.spark.common.SparkPlatform;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class HytaleSparkCommand extends AbstractCommand {
    private final SparkPlatform platform;

    public HytaleSparkCommand(SparkPlatform platform) {
        super("spark", "Spark command");
        setAllowsExtraArguments(true);
        requirePermission("spark");
        this.platform = platform;
    }

    // we override acceptCall to handle parsing directly, spark arguments are not compatible with Hytale's command parser.
    @Override
    public @Nullable CompletableFuture<Void> acceptCall(@NonNull CommandSender sender, @NonNull ParserContext parserContext, @NonNull ParseResult parseResult) {
        String inputString = parserContext.getInputString();
        String[] args = inputString.split(" ");
        return this.platform.executeCommand(HytaleCommandSender.of(sender), Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    protected @Nullable CompletableFuture<Void> execute(@NonNull CommandContext ctx) {
        // This method is never called because we override acceptCall above.
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasPermission(@NonNull CommandSender sender) {
        return this.platform.hasPermissionForAnyCommand(HytaleCommandSender.of(sender));
    }
}
