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

import com.google.common.collect.ImmutableSet;
import com.hypixel.hytale.common.plugin.AuthorInfo;
import com.hypixel.hytale.server.core.command.system.CommandRegistration;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.Universe;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import me.lucko.spark.common.monitor.tick.TickStatistics;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.serverconfig.ServerConfigProvider;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.sampler.source.SourceMetadata;
import me.lucko.spark.common.util.SparkThreadFactory;
import org.jspecify.annotations.NonNull;

import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HytaleSparkPlugin extends JavaPlugin implements SparkPlugin {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4, new SparkThreadFactory());
    private final ThreadDumper gameThreadDumper = new ThreadDumper.Regex(ImmutableSet.of(
            "WorldThread - .*",        // Main world threads (WorldThread - WorldName)
            "WorldMap - .*",           // World map IO threads (WorldMap - WorldName)
            "ChunkLighting - .*",      // Chunk lighting threads (ChunkLighting - WorldName)
            "ChunkGenerator-.*",       // World generation workers (ChunkGenerator-N-Worker-M)
            "ServerWorkerGroup.*",     // Netty IO worker threads (ServerWorkerGroup - N)
            "ServerBossGroup.*",       // Netty IO boss threads (ServerBossGroup - N)
            "Scheduler"                // Main scheduled executor
    ));

    private SparkPlatform platform;
    private CommandRegistration command;

    public HytaleSparkPlugin(@NonNull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        this.platform = new SparkPlatform(this);
        this.platform.enable();

        this.command = getCommandRegistry().registerCommand(new HytaleSparkCommand(this.platform));
    }

    @Override
    protected void shutdown() {
        if (this.command != null) {
            this.command.unregister();
            this.command = null;
        }

        if (this.platform != null) {
            this.platform.disable();
            this.platform = null;
        }

        this.scheduler.shutdown();
    }

    @Override
    public String getVersion() {
        return getManifest().getVersion().toString();
    }

    @Override
    public Path getPluginDirectory() {
        return getDataDirectory();
    }

    @Override
    public String getCommandName() {
        return "spark";
    }

    @Override
    public Stream<HytaleCommandSender<?>> getCommandSenders() {
        return Stream.concat(
                Universe.get().getPlayers().stream().map(HytaleCommandSender::of),
                Stream.of(HytaleCommandSender.of(ConsoleSender.INSTANCE))
        );
    }

    @Override
    public void executeAsync(Runnable runnable) {
        this.scheduler.execute(runnable);
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new HytalePlatformInfo();
    }

    @Override
    public ClassSourceLookup createClassSourceLookup() {
        return new HytaleClassSourceLookup();
    }

    @Override
    public Collection<SourceMetadata> getKnownSources() {
        return SourceMetadata.gather(
                PluginManager.get().getPlugins(),
                PluginBase::getName,
                plugin -> plugin.getManifest().getVersion().toString(),
                plugin -> plugin.getManifest().getAuthors().stream().map(AuthorInfo::getName).collect(Collectors.joining(", ")),
                plugin -> plugin.getManifest().getDescription(),
                plugin -> plugin.getIdentifier().getGroup().equals("Hytale")
        );
    }

    @Override
    public ThreadDumper getDefaultThreadDumper() {
        return this.gameThreadDumper;
    }

    @Override
    public PlayerPingProvider createPlayerPingProvider() {
        return new HytalePlayerPingProvider();
    }

    @Override
    public ServerConfigProvider createServerConfigProvider() {
        return new HytaleServerConfigProvider();
    }

    @Override
    public TickStatistics createTickStatistics() {
        return new HytaleTickStatistics();
    }

    @Override
    public WorldInfoProvider createWorldInfoProvider() {
        return new HytaleWorldInfoProvider(this);
    }

    @Override
    public void log(Level level, String msg) {
        getLogger().at(level).log(msg);
    }

    @Override
    public void log(Level level, String msg, Throwable throwable) {
        getLogger().at(level).withCause(throwable).log(msg);
    }
}
