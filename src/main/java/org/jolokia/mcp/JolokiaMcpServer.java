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

import javax.management.MalformedObjectNameException;

import jakarta.inject.Inject;

import io.quarkiverse.mcp.server.ToolManager;
import io.quarkiverse.mcp.server.ToolResponse;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jolokia.client.exception.J4pException;
import org.jolokia.json.JSONArray;
import org.jolokia.json.JSONObject;

public class JolokiaMcpServer {

    @Inject
    JolokiaClient jolokiaClient;

    @Inject
    ToolManager toolManager;

    @Startup
    void registerTools() {
        try {
            listTools().forEach(tool -> {
                ToolManager.ToolDefinition def = toolManager.newTool(tool.name())
                    .setDescription(tool.description())
                    .setHandler(args -> {
                        try {
                            Optional<Object> response = jolokiaClient.exec(
                                tool.objectName(),
                                tool.operation(),
                                toolArgsToArgs(args.args(), tool.args()));
                            return ToolResponse.success(response.orElse("null").toString());
                        } catch (MalformedObjectNameException | J4pException | IllegalArgumentException e) {
                            return ToolResponse.error(e.getMessage());
                        }
                    });
                Arrays.stream(tool.args())
                    .map(arg -> (JSONObject) arg)
                    .forEach(arg -> {
                        def.addArgument(
                            (String) arg.get("name"),
                            (String) arg.get("desc"),
                            true,
                            toJavaType((String) arg.get("type")));
                    });
                def.register();
            });
        } catch (J4pException e) {
            Log.error(e.getMessage(), e);
        }
    }

    Type toJavaType(String type) {
        return switch (type) {
            case "boolean", "java.lang.Boolean" -> Boolean.class;
            case "int", "java.lang.Integer" -> Integer.class;
            case "long", "java.lang.Long" -> Long.class;
            case "short", "java.lang.Short" -> Short.class;
            case "double", "java.lang.Double" -> Double.class;
            case "float", "java.lang.Float" -> Float.class;
            case "byte", "java.lang.Byte" -> Byte.class;
            case "java.lang.String" -> String.class;
            default -> Object.class;
        };
    }

    Object[] toolArgsToArgs(Map<String, Object> toolArgs, Object[] args) throws IllegalArgumentException {
        if (toolArgs.size() != args.length) {
            throw new IllegalArgumentException("Invalid number of arguments");
        }
        return Arrays.stream(args)
            .map(arg -> (JSONObject) arg)
            .map(arg -> toolArgs.get(arg.get("name")))
            .toArray();
    }

    List<MBeanTool> listTools() throws J4pException {
        JSONObject domains = jolokiaClient.list();
        return domains.entrySet().stream()
            .flatMap(d -> domain(d.getKey(), (JSONObject) d.getValue()))
            .collect(Collectors.toList());
    }

    Stream<MBeanTool> domain(String domain, JSONObject mbeans) {
        return mbeans.entrySet().stream()
            .flatMap(m -> props(domain, m.getKey(), (JSONObject) m.getValue()));
    }

    Stream<MBeanTool> props(String domain, String props, JSONObject mbeanInfo) {
        String objectName = domain + ":" + props;
        Log.debug(objectName);
        return mbeanInfo.entrySet().stream()
            .filter(kv -> "op".equals(kv.getKey()))
            .flatMap(kv -> ops(objectName, (JSONObject) kv.getValue()));
    }

    Stream<MBeanTool> ops(String objectName, JSONObject ops) {
        ops.forEach((k, v) -> Log.debugf("  %s = %s", k, v));
        return ops.entrySet().stream()
            .flatMap(kv -> {
                if (kv.getValue() instanceof JSONArray) {
                    JSONArray v = (JSONArray) kv.getValue();
                    return IntStream.range(0, v.size())
                        .mapToObj(i -> op(objectName, kv.getKey(), (JSONObject) v.get(i), i + 1));
                }
                return Stream.of((op(objectName, kv.getKey(), (JSONObject) kv.getValue(), 0)));
            });
    }

    MBeanTool op(String objectName, String opName, JSONObject opInfo, int index) {
        String name = objectName + "/" + opName + (index > 0 ? index : "");
        String desc = (String) opInfo.get("desc");
        String ret = (String) opInfo.get("ret");
        JSONArray args = (JSONArray) opInfo.get("args");
        return new MBeanTool(name, desc, objectName, opName, ret, args.toArray(new Object[0]));
    }

    record MBeanTool(
        String name,
        String description,
        String objectName,
        String operation,
        String returnType,
        Object... args) {
    }
}

