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

import me.lucko.spark.common.platform.world.AbstractChunkInfo;
import me.lucko.spark.common.platform.world.CountMap;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import org.allaymc.api.registry.Registries;
import org.allaymc.api.server.Server;
import org.allaymc.api.utils.Identifier;
import org.allaymc.api.world.chunk.Chunk;
import org.allaymc.api.world.service.EntityService;

import java.util.*;

/**
 * @author IWareQ
 */
public class AllayWorldInfoProvider implements WorldInfoProvider {
    @Override
    public CountsResult pollCounts() {
        var server = Server.getInstance();
        var entities = 0;
        var blockEntities = 0;
        var chunks = 0;

        for (var world : server.getWorldPool().getWorlds().values()) {
            for (var dimension : world.getDimensions().values()) {
                entities += dimension.getEntities().size();
                blockEntities += dimension.getChunkService().getLoadedChunks()
                        .stream()
                        .mapToInt(chunk -> chunk.getBlockEntities().size())
                        .sum();
                chunks += dimension.getChunkService().getLoadedChunks().size();
            }
        }

        return new CountsResult(server.getPlayerService().getPlayers().size(), entities, blockEntities, chunks);
    }

    @Override
    public ChunksResult<AllayChunkInfo> pollChunks() {
        ChunksResult<AllayChunkInfo> result = new ChunksResult<>();

        for (var world : Server.getInstance().getWorldPool().getWorlds().values()) {
            for (var dimension : world.getDimensions().values()) {
                var entityService = dimension.getEntityService();
                var chunks = dimension.getChunkService().getLoadedChunks();
                var chunkInfos = chunks.stream().map(chunk -> new AllayChunkInfo(chunk, entityService)).toList();

                result.put(world.getWorldData().getDisplayName() + "_" + dimension.getDimensionInfo(), chunkInfos);
            }
        }

        return result;
    }

    @Override
    public GameRulesResult pollGameRules() {
        GameRulesResult data = new GameRulesResult();
        for (var world : Server.getInstance().getWorldPool().getWorlds().values()) {
            for (var gameRuleEntry : world.getWorldData().getGameRules().getGameRules().entrySet()) {
                var value = gameRuleEntry.getValue();
                data.put(gameRuleEntry.getKey().getName(), world.getWorldData().getDisplayName(), Objects.toString(value));
            }
        }

        return data;
    }

    @Override
    public Collection<DataPackInfo> pollDataPacks() {
        List<DataPackInfo> dataPacks = new ArrayList<>();
        Registries.PACKS.getContent().values().forEach(pack -> {
            dataPacks.add(new DataPackInfo(pack.getName(), pack.getManifest().getHeader().getDescription(), ""));
        });
        return dataPacks;
    }

    public static class AllayChunkInfo extends AbstractChunkInfo<Identifier> {
        private final CountMap<Identifier> entityCounts = new CountMap.Simple<>(new HashMap<>());

        protected AllayChunkInfo(Chunk chunk, EntityService entityService) {
            super(chunk.getX(), chunk.getZ());
            entityService.forEachEntitiesInChunk(chunk.getX(), chunk.getZ(), entity ->
                    this.entityCounts.increment(entity.getEntityType().getIdentifier())
            );
        }

        @Override
        public CountMap<Identifier> getEntityCounts() {
            return this.entityCounts;
        }

        @Override
        public String entityTypeName(Identifier identifier) {
            return identifier.toString();
        }
    }
}
