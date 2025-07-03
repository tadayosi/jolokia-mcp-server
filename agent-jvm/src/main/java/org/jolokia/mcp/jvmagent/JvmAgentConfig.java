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
package org.jolokia.mcp.jvmagent;

import java.util.HashMap;
import java.util.Map;

import org.jolokia.server.core.util.EscapeUtil;

public class JvmAgentConfig {

    private boolean isStopMode;

    public JvmAgentConfig(String args) {
        this(split(args));
    }

    public JvmAgentConfig(Map<String, String> config) {
        initMode(config);
    }

    private void initMode(Map<String, String> agentConfig) {
        String mode = agentConfig.get("mode");
        if (mode != null && !mode.equals("start") && !mode.equals("stop")) {
            throw new IllegalArgumentException("Invalid running mode '" + mode + "'. Must be either 'start' or 'stop'");
        }
        isStopMode = "stop".equals(mode);
    }

    private static Map<String, String> split(String agentArgs) {
        Map<String, String> ret = new HashMap<>();
        if (agentArgs != null && !agentArgs.isEmpty()) {
            for (String arg : EscapeUtil.splitAsArray(agentArgs, EscapeUtil.CSV_ESCAPE, ",")) {
                String[] prop = arg.split("=", 2);
                if (prop.length != 2) {
                    throw new IllegalArgumentException("jolokia: Invalid option '" + arg + "'");
                } else {
                    ret.put(prop[0], prop[1]);
                }
            }
        }
        return ret;
    }

    public boolean isModeStop() {
        return false;
    }

    public boolean isLazy() {
        return true; // TODO: impl
    }

    public void setClassLoader(ClassLoader loader) {
    }
}
