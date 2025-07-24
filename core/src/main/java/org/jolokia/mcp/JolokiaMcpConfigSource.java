/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jolokia.mcp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class JolokiaMcpConfigSource implements ConfigSource {

    private static final Map<String, String> configuration = new HashMap<>();

    public static void setup(String... args) {
        List<String> remains = Arrays.stream(args)
            .filter(arg -> {
                if (arg.startsWith("--") || arg.startsWith("-D")) {
                    String[] parts = arg.substring(2).split("=");
                    if (parts.length == 2) {
                        configuration.put(parts[0], parts[1]);
                    } else {
                        configuration.put(parts[0], "true");
                    }
                    return false;
                }
                return !arg.startsWith("-");
            }).
            toList();
        remains.stream()
            .findFirst()
            .ifPresent(arg -> configuration.put("jolokia.mcp.url", arg));
        setupSse();
    }

    public static void setup(Map<String, String> config, boolean setupSse) {
        configuration.putAll(config);
        if (setupSse) {
            setupSse();
        }
    }

    private static void setupSse() {
        boolean sse = Boolean.parseBoolean(configuration.get("sse"));
        if (sse) {
            configuration.put("quarkus.http.host-enabled", "true");
            configuration.put("quarkus.mcp.server.stdio.enabled", "false");
        } else {
            configuration.put("quarkus.http.host-enabled", "false");
            configuration.put("quarkus.mcp.server.stdio.enabled", "true");
        }
    }

    @Override
    public int getOrdinal() {
        return 275;
    }

    @Override
    public Set<String> getPropertyNames() {
        return configuration.keySet();
    }

    @Override
    public String getValue(String propertyName) {
        return configuration.get(propertyName);
    }

    @Override
    public String getName() {
        return JolokiaMcpConfigSource.class.getSimpleName();
    }
}
