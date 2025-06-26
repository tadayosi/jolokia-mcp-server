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

import java.util.List;
import java.util.Optional;
import javax.management.MalformedObjectNameException;
import jakarta.inject.Inject;

import io.quarkiverse.mcp.server.TextContent;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import org.jolokia.client.exception.J4pException;
import org.jolokia.json.JSONObject;

public class JolokiaMcpServer {

    @Inject
    JolokiaClient jolokiaClient;

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
    ToolResponse listMBeanOperations(@ToolArg(description = "MBean name") String mbean) {
        try {
            JSONObject ops = jolokiaClient.listOperations(mbean);
            return ToolResponse.success(ops.toJSONString());
        } catch (J4pException e) {
            return ToolResponse.error(e.getMessage());
        }
    }

    @Tool(description = "List available attributes for a given MBean")
    ToolResponse listMBeanAttributes(@ToolArg(description = "MBean name") String mbean) {
        try {
            JSONObject attrs = jolokiaClient.listAttributes(mbean);
            return ToolResponse.success(attrs.toJSONString());
        } catch (J4pException e) {
            return ToolResponse.error(e.getMessage());
        }
    }

    @Tool(description = "Read an attribute from a given MBean")
    ToolResponse readMBeanAttribute(
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
    ToolResponse writeMBeanAttribute(
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
    ToolResponse executeMBeanOperation(
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
}

