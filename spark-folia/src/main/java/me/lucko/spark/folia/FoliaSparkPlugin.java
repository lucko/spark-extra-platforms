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

package me.lucko.spark.folia;

import com.google.common.collect.ImmutableSet;
import me.lucko.spark.api.Spark;
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
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Stream;

public class FoliaSparkPlugin extends JavaPlugin implements SparkPlugin {
    private BukkitAudiences audienceFactory;
    private ThreadDumper gameThreadDumper;

    private SparkPlatform platform;

    @Override
    public void onEnable() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
        } catch (ClassNotFoundException e) {
            getLogger().severe("This version of spark requires Folia! Please use the regular Bukkit plugin instead.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.audienceFactory = BukkitAudiences.create(this);
        this.gameThreadDumper = new ThreadDumper.Regex(ImmutableSet.of("Region Scheduler Thread #\\d+"));

        this.platform = new SparkPlatform(this);
        this.platform.enable();
    }

    @Override
    public void onDisable() {
        if (this.platform != null) {
            this.platform.disable();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        this.platform.executeCommand(new FoliaCommandSender(sender, this.audienceFactory), args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return this.platform.tabCompleteCommand(new FoliaCommandSender(sender, this.audienceFactory), args);
    }

    @Override
    public String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public Path getPluginDirectory() {
        return getDataFolder().toPath();
    }

    @Override
    public String getCommandName() {
        return "spark";
    }

    @Override
    public Stream<FoliaCommandSender> getCommandSenders() {
        return Stream.concat(
                getServer().getOnlinePlayers().stream(),
                Stream.of(getServer().getConsoleSender())
        ).map(sender -> new FoliaCommandSender(sender, this.audienceFactory));
    }

    @Override
    public void executeAsync(Runnable task) {
        getServer().getAsyncScheduler().runNow(this, t -> task.run());
    }

    @Override
    public void executeSync(Runnable task) {
        getServer().getGlobalRegionScheduler().execute(this, task);
    }

    @Override
    public void log(Level level, String msg) {
        getLogger().log(level, msg);
    }

    @Override
    public void log(Level level, String msg, Throwable throwable) {
        getLogger().log(level, msg, throwable);
    }

    @Override
    public ThreadDumper getDefaultThreadDumper() {
        return this.gameThreadDumper;
    }

    @Override
    public TickStatistics createTickStatistics() {
        return new FoliaTickStatistics(getServer());
    }

    @Override
    public ClassSourceLookup createClassSourceLookup() {
        return new FoliaClassSourceLookup();
    }

    @Override
    public Collection<SourceMetadata> getKnownSources() {
        return SourceMetadata.gather(
                Arrays.asList(getServer().getPluginManager().getPlugins()),
                Plugin::getName,
                plugin -> plugin.getDescription().getVersion(),
                plugin -> String.join(", ", plugin.getDescription().getAuthors()),
                plugin -> plugin.getDescription().getDescription()
        );
    }

    @Override
    public PlayerPingProvider createPlayerPingProvider() {
        return new FoliaPlayerPingProvider(getServer());
    }

    @Override
    public ServerConfigProvider createServerConfigProvider() {
        return new FoliaServerConfigProvider();
    }

    @Override
    public WorldInfoProvider createWorldInfoProvider() {
        return new FoliaWorldInfoProvider(this);
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new FoliaPlatformInfo(getServer());
    }

    @Override
    public void registerApi(Spark api) {
        getServer().getServicesManager().register(Spark.class, api, this, ServicePriority.Normal);
    }

}
