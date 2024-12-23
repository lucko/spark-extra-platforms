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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import me.lucko.spark.common.platform.serverconfig.ConfigParser;
import me.lucko.spark.common.platform.serverconfig.ExcludedConfigFilter;
import me.lucko.spark.common.platform.serverconfig.ServerConfigProvider;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author IWareQ
 */
public class AllayServerConfigProvider extends ServerConfigProvider {
    private static final Map<String, ConfigParser> FILES = new HashMap<>();
    private static final Set<String> HIDDEN_PATHS = new HashSet<>();

    static {
        FILES.put("server-settings.yml", YamlConfigParser.INSTANCE);
        FILES.put("worlds/world-settings.yml", YamlConfigParser.INSTANCE);

        HIDDEN_PATHS.add("network-settings.ip");
        HIDDEN_PATHS.add("network-settings.port");
    }

    public AllayServerConfigProvider() {
        super(FILES, HIDDEN_PATHS);
    }

    private static class YamlConfigParser implements ConfigParser {
        public static final YamlConfigParser INSTANCE = new YamlConfigParser();

        private static final Gson GSON = new Gson();
        private static final Yaml YAML = new Yaml();

        @Override
        public JsonElement load(String file, ExcludedConfigFilter filter) throws IOException {
            var values = this.parse(Paths.get(file));
            if (values == null) {
                return null;
            }

            return filter.apply(GSON.toJsonTree(values));
        }

        @Override
        public Map<String, Object> parse(BufferedReader reader) {
            return YAML.load(reader);
        }
    }
}
