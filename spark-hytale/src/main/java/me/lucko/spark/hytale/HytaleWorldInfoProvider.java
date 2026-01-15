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

import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.platform.world.ChunkInfo;
import me.lucko.spark.common.platform.world.WorldInfoProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

public class HytaleWorldInfoProvider implements WorldInfoProvider {
    private static final long TIMEOUT_SECONDS = 5;

    private final SparkPlugin plugin;

    public HytaleWorldInfoProvider(SparkPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CountsResult pollCounts() {
        Universe universe = Universe.get();

        List<CompletableFuture<CountsResult>> futures = new ArrayList<>(universe.getWorlds().size());
        for (World world : universe.getWorlds().values()) {
            if (!world.isStarted()) {
                continue;
            }

            futures.add(CompletableFuture.supplyAsync(() -> {
                int entities = world.getEntityStore().getStore().getEntityCount();
                int chunks = world.getChunkStore().getLoadedChunksCount();
                return new CountsResult(0, entities, 0, chunks);
            }, world));
        }

        int players = universe.getPlayerCount();
        int entities = 0;
        int chunks = 0;

        // Wait for all worlds and aggregate results
        for (CompletableFuture<CountsResult> future : futures) {
            CountsResult counts = getWithTimeout(future);
            if (counts == null) {
                continue;
            }

            entities += counts.entities();
            chunks += counts.chunks();
        }

        return new CountsResult(players, entities, -1, chunks);
    }

    @Override
    public ChunksResult<? extends ChunkInfo<?>> pollChunks() {
        return null; // TODO
    }

    @Override
    public GameRulesResult pollGameRules() {
        // No equivalent in Hytale
        return null;
    }

    @Override
    public Collection<DataPackInfo> pollDataPacks() {
        // No equivalent in Hytale
        return null;
    }

    @Override
    public boolean mustCallSync() {
        // Hytale's world operations must be called on their respective world threads
        return false;
    }

    private <T> T getWithTimeout(CompletableFuture<T> future) {
        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            this.plugin.log(Level.WARNING, "Timed out waiting for world statistics");
            return null;
        }
    }


}
