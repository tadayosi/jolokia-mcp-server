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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.mcp.server.TextContent;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import org.jolokia.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class JolokiaMcpServer {

    private static final Logger LOG = LoggerFactory.getLogger(JolokiaMcpServer.class);

    @Inject
    JolokiaService jolokiaService;

    public JolokiaMcpServer() {
        LOG.info("Start Jolokia MCP Server");
    }

    @Tool(description = "List available MBeans from the JVM")
    ToolResponse listMBeans() {
        try {
            List<String> mbeans = jolokiaService.listMBeans();
            return ToolResponse.success(mbeans.stream().map(TextContent::new).toList());
        } catch (Exception e) {
            LOG.error("listMBeans: " + e.getMessage(), e);
            return ToolResponse.error(e.getMessage());
        }
    }

    @Tool(description = "List available operations for a given MBean")
    ToolResponse listMBeanOperations(@ToolArg(description = "MBean name") String mbean) {
        try {
            JSONObject ops = jolokiaService.listOperations(mbean);
            return ToolResponse.success(ops.toJSONString());
        } catch (Exception e) {
            LOG.error("listMBeanOperations: " + e.getMessage(), e);
            return ToolResponse.error(e.getMessage());
        }
    }

    @Tool(description = "List available attributes for a given MBean")
    ToolResponse listMBeanAttributes(@ToolArg(description = "MBean name") String mbean) {
        try {
            JSONObject attrs = jolokiaService.listAttributes(mbean);
            return ToolResponse.success(attrs.toJSONString());
        } catch (Exception e) {
            LOG.error("listMBeanAttributes: " + e.getMessage(), e);
            return ToolResponse.error(e.getMessage());
        }
    }

    @Tool(description = "Read an attribute from a given MBean")
    ToolResponse readMBeanAttribute(
        @ToolArg(description = "MBean name") String mbean,
        @ToolArg(description = "Attribute name") String attribute) {
        try {
            Optional<Object> response = jolokiaService.read(mbean, attribute);
            return ToolResponse.success(response.orElse("null").toString());
        } catch (Exception e) {
            LOG.error("readMBeanAttribute: " + e.getMessage(), e);
            return ToolResponse.error(e.getMessage());
        }
    }

    @Tool(description = "Set the value to an attribute of a given MBean")
    ToolResponse writeMBeanAttribute(
        @ToolArg(description = "MBean name") String mbean,
        @ToolArg(description = "Attribute name") String attribute,
        @ToolArg(description = "Attribute value") Object value) {
        try {
            Optional<Object> response = jolokiaService.write(mbean, attribute, value);
            return ToolResponse.success(response.orElse("null").toString());
        } catch (Exception e) {
            LOG.error("writeMBeanAttribute: " + e.getMessage(), e);
            return ToolResponse.error(e.getMessage());
        }
    }

    @Tool(description = "Execute an operation on a given MBean")
    ToolResponse executeMBeanOperation(
        @ToolArg(description = "MBean name") String mbean,
        @ToolArg(description = "Operation name") String operation,
        @ToolArg(description = "Arguments") Object... args) {
        try {
            Optional<Object> response = jolokiaService.exec(mbean, operation, args);
            return ToolResponse.success(response.orElse("null").toString());
        } catch (Exception e) {
            LOG.error("executeMBeanOperation: " + e.getMessage(), e);
            return ToolResponse.error(e.getMessage());
        }
    }
}

