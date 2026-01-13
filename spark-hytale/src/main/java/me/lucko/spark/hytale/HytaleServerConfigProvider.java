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
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import me.lucko.spark.common.platform.serverconfig.ConfigParser;
import me.lucko.spark.common.platform.serverconfig.ExcludedConfigFilter;
import me.lucko.spark.common.platform.serverconfig.ServerConfigProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;

public class HytaleServerConfigProvider extends ServerConfigProvider {

    /** A map of provided files and their type */
    private static final Map<String, ConfigParser> FILES;
    /** A collection of paths to be excluded from the files */
    private static final Collection<String> HIDDEN_PATHS;

    public HytaleServerConfigProvider() {
        super(FILES, HIDDEN_PATHS);
    }

    static {
        ImmutableSet.Builder<String> hiddenPaths = ImmutableSet.<String>builder()
                .add("Password")
                .addAll(getSystemPropertyList("spark.serverconfigs.hiddenpaths"));

        FILES = ImmutableMap.of("config.json", JsonConfigParser.INSTANCE);
        HIDDEN_PATHS = hiddenPaths.build();
    }

    private enum JsonConfigParser implements ConfigParser {
        INSTANCE;

        private static final Gson GSON = new Gson();

        @Override
        public JsonElement load(String file, ExcludedConfigFilter filter) throws IOException {
            Path path = Paths.get(file);
            if (!Files.exists(path)) {
                return null;
            }

            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonElement element = GSON.fromJson(reader, JsonElement.class);
                if (element == null) {
                    return null;
                }
                return filter.apply(element);
            }
        }

        @Override
        public Map<String, Object> parse(BufferedReader reader) {
            // can more easily convert directly to JsonElement - only called internally
            throw new UnsupportedOperationException();
        }
    }

}
