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

import io.quarkiverse.mcp.server.TextContent;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolManager;
import io.quarkiverse.mcp.server.ToolResponse;
import io.quarkus.logging.Log;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jolokia.client.exception.J4pException;
import org.jolokia.json.JSONArray;
import org.jolokia.json.JSONObject;

public class JolokiaMcpServer {

    static final Pattern TOOL_NAME_NEGATIVE_PATTERN = Pattern.compile("[^a-zA-Z0-9_-]+");
    static final Set<String> DENYLIST_DOMAINS = Set.of(
        "java.util.logging",
        "JMImplementation",
        "jdk.management.jfr",
        "com.sun.management",
        "org.apache.logging.log4j2",
        "java.nio"
    );
    static final Set<String> DENYLIST_OBJECTNAMES = Set.of();
    static final Set<String> SPECIAL_PROP_KEYS = Set.of(
        "type",
        "name",
        "context",
        "component",
        "agent"
    );

    @Inject
    JolokiaClient jolokiaClient;

    @Inject
    ToolManager toolManager;

    @Tool(description = "List available MBeans from the JVM")
    ToolResponse listMBeans() {
        try {
            List<String> mbeans = jolokiaClient.listMBeans();
            return ToolResponse.success(mbeans.stream().map(TextContent::new).toList());
        } catch (J4pException e) {
            return ToolResponse.error(e.getMessage());
        }
    }

    @Tool(description = "List available operations for a given MBean")
    ToolResponse listOperations(@ToolArg(description = "MBean name") String mbean) {
        try {
            JSONObject ops = jolokiaClient.listOperations(mbean);
            return ToolResponse.success(ops.toJSONString());
        } catch (J4pException e) {
            return ToolResponse.error(e.getMessage());
        }
    }

    @Tool(description = "List available attributes for a given MBean")
    ToolResponse listAttributes(@ToolArg(description = "MBean name") String mbean) {
        try {
            JSONObject attrs = jolokiaClient.listAttributes(mbean);
            return ToolResponse.success(attrs.toJSONString());
        } catch (J4pException e) {
            return ToolResponse.error(e.getMessage());
        }
    }

    @Tool(description = "Read an attribute from a given MBean")
    ToolResponse read(
        @ToolArg(description = "MBean name") String mbean,
        @ToolArg(description = "Attribute name") String attribute) {
        try {
            Optional<Object> response = jolokiaClient.read(mbean, attribute);
            return ToolResponse.success(response.orElse("null").toString());
        } catch (MalformedObjectNameException | J4pException e) {
            return ToolResponse.error(e.getMessage());
        }
    }

    @Tool(description = "Set the value to an attribute of a given MBean")
    ToolResponse write(
        @ToolArg(description = "MBean name") String mbean,
        @ToolArg(description = "Attribute name") String attribute,
        @ToolArg(description = "Attribute value") Object value) {
        try {
            Optional<Object> response = jolokiaClient.write(mbean, attribute, value);
            return ToolResponse.success(response.orElse("null").toString());
        } catch (MalformedObjectNameException | J4pException e) {
            return ToolResponse.error(e.getMessage());
        }
    }

    @Tool(description = "Execute an operation on a given MBean")
    ToolResponse execute(
        @ToolArg(description = "MBean name") String mbean,
        @ToolArg(description = "Operation name") String operation,
        @ToolArg(description = "Arguments") Object... args) {
        try {
            Optional<Object> response = jolokiaClient.exec(mbean, operation, args);
            return ToolResponse.success(response.orElse("null").toString());
        } catch (MalformedObjectNameException | J4pException e) {
            return ToolResponse.error(e.getMessage());
        }
    }

    void register(MBeanTool tool) {
        String toolName = resolveToolName(tool);
        Log.infof("Registering tool: %s (%s)", toolName, toolName.length());
        ToolManager.ToolDefinition def = toolManager.newTool(toolName)
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
        tool.args().forEach(arg -> {
            def.addArgument(
                (String) arg.get("name"),
                (String) arg.get("desc"),
                true,
                toJavaType((String) arg.get("type")));
        });
        def.register();
    }

    String resolveToolName(MBeanTool tool) {
        // Tool name must be <= 64 chars for Claude Desktop
        String toolName = tool.name();
        if (toolName.length() > 64) {
            toolName = toolName.substring(0, 63);
        }
        int count = 1;
        while (toolManager.getTool(toolName) != null) {
            toolName = toolName.replaceFirst(".$", String.valueOf(count));
            count++;
        }
        return toolName;
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

    Object[] toolArgsToArgs(Map<String, Object> toolArgs, List<JSONObject> args) throws IllegalArgumentException {
        if (toolArgs.size() != args.size()) {
            throw new IllegalArgumentException("Invalid number of arguments");
        }
        return args.stream()
            .map(arg -> arg.get("name"))
            .map(toolArgs::get)
            .toArray();
    }

    List<MBeanTool> listTools() throws J4pException {
        JSONObject domains = jolokiaClient.list(null);
        return domains.entrySet().stream()
            .filter(d -> !DENYLIST_DOMAINS.contains(d.getKey()))
            .flatMap(d -> domain(d.getKey(), (JSONObject) d.getValue()))
            .collect(Collectors.toList());
    }

    Stream<MBeanTool> domain(String domain, JSONObject mbeans) {
        return mbeans.entrySet().stream()
            .filter(m -> !DENYLIST_OBJECTNAMES.contains(domain + ":" + m.getKey()))
            .flatMap(m -> props(domain, m.getKey(), (JSONObject) m.getValue()));
    }

    Stream<MBeanTool> props(String domain, String props, JSONObject mbeanInfo) {
        Log.debug(domain + ":" + props);
        return mbeanInfo.entrySet().stream()
            .filter(kv -> "op".equals(kv.getKey()))
            .flatMap(kv -> ops(domain, props, (JSONObject) kv.getValue()));
    }

    Stream<MBeanTool> ops(String domain, String props, JSONObject ops) {
        ops.forEach((k, v) -> Log.debugf("  %s = %s", k, v));
        return ops.entrySet().stream()
            .flatMap(kv -> {
                if (kv.getValue() instanceof JSONArray) {
                    JSONArray v = (JSONArray) kv.getValue();
                    return IntStream.range(0, v.size())
                        .mapToObj(i -> op(domain, props, kv.getKey(), (JSONObject) v.get(i), i + 1));
                }
                return Stream.of((op(domain, props, kv.getKey(), (JSONObject) kv.getValue(), 0)));
            });
    }

    MBeanTool op(String domain, String props, String opName, JSONObject opInfo, int index) {
        String name = toolName(domain, props, opName, index);
        String objectName = domain + ":" + props;
        String uniqueOpName = opName;
        @SuppressWarnings("unchecked")
        List<JSONObject> args = (List<JSONObject>) opInfo.get("args");
        if (index > 0) {
            // Overloaded operation
            uniqueOpName += "(" + args.stream().map(arg -> (String) arg.get("type")).collect(Collectors.joining(",")) + ")";
        }
        String desc = objectName + "/" + uniqueOpName + ": " + opInfo.get("desc");
        String ret = (String) opInfo.get("ret");
        return new MBeanTool(name, desc, objectName, uniqueOpName, ret, args);
    }

    String toolName(String domain, String props, String opName, int index) {
        String name = shortObjectName(domain, props) + "-" + opName + (index > 0 ? "-" + index : "");
        return TOOL_NAME_NEGATIVE_PATTERN.matcher(name).replaceAll("_");
    }

    String shortObjectName(String domain, String props) {
        // Shorten domain
        String shortDomain = domain;
        if (domain.startsWith("org.apache.")) {
            shortDomain = domain.substring("org.apache.".length());
        }
        shortDomain = shortDomain.replaceAll("[.]", "");

        // Shorten props
        Map<String, String> propMap = Arrays.stream(props.split(","))
            .map(prop -> prop.split("="))
            .collect(Collectors.toMap(kv -> kv[0], kv -> kv[1], (v1, v2) -> v2, TreeMap::new));
        StringBuilder shortProps = new StringBuilder();
        // Take at most 2 props
        int count = 0;
        for (String key : propMap.keySet()) {
            if (SPECIAL_PROP_KEYS.contains(key)) {
                if (!shortProps.isEmpty()) {
                    shortProps.append("_");
                }
                shortProps.append(sanitiseName(propMap.get(key)));
                count++;
            }
            if (count >= 2) {
                break;
            }
        }
        // If there are less than 2 special keys in prop list
        if (count < 2 && propMap.size() > count) {
            shortProps.append(
                propMap.values().stream()
                    .filter(value -> !SPECIAL_PROP_KEYS.contains(value))
                    .limit(2 - count)
                    .map(this::sanitiseName)
                    .collect(Collectors.joining("_")));
        }

        return shortDomain + "-" + shortProps;
    }

    /**
     * Drop all characters that are negative for a tool name, plus "_" and "-"
     * to earn extra margin for the tool name (<= 64 chars for Claude Desktop).
     * Also shrink the name to at most 20 chars if it's longer.
     */
    String sanitiseName(String name) {
        String sanitised = TOOL_NAME_NEGATIVE_PATTERN.matcher(name)
            .replaceAll("")
            .replaceAll("[_-]", "");
        return sanitised.substring(0, Math.min(sanitised.length(), 20));
    }

    record MBeanTool(
        String name,
        String description,
        String objectName,
        String operation,
        String returnType,
        List<JSONObject> args) {
    }
}

