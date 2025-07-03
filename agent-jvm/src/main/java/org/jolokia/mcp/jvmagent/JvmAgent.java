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

import java.lang.instrument.Instrumentation;
import java.util.Set;

import org.jolokia.server.core.detector.ServerDetector;
import org.jolokia.server.core.detector.ServerDetectorLookup;
import org.jolokia.server.core.service.impl.CachingServerDetectorLookup;
import org.jolokia.server.core.service.impl.ClasspathServerDetectorLookup;
import org.jolokia.server.core.service.impl.StdoutLogHandler;

public class JvmAgent {

    /**
     * System property used for communicating the agent's state
     */
    public static final String JOLOKIA_MCP_AGENT_URL = "jolokia.mcp.agent";

    private static JvmAgent agent;

    private JolokiaServer server;

    // info to preserve on server restart without restarting entire JVM
    private final Instrumentation instrumentation;
    private final JvmAgentConfig config;
    private final boolean lazy;

    private JvmAgent(JvmAgentConfig config, boolean lazy, Instrumentation inst) {
        this.instrumentation = inst;
        this.config = config;
        this.lazy = lazy;
    }

    /**
     * Entry point for the agent, using command line attach
     * (that is via -javaagent command line argument)
     *
     * @param agentArgs arguments as given on the command line
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        JvmAgentConfig config = new JvmAgentConfig(agentArgs);
        agent = new JvmAgent(config, config.isLazy(), inst);
        agent.start();
    }

    /**
     * Entry point for the agent, using dynamic attach.
     * (this is a post VM initialisation attachment, via com.sun.attach)
     *
     * @param agentArgs arguments as given on the command line
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        JvmAgentConfig config = new JvmAgentConfig(agentArgs);
        if (config.isModeStop()) {
            if (agent != null) {
                agent.stop(true);
            }
        } else {
            agent = new JvmAgent(config, config.isLazy(), inst);
            agent.start();
        }
    }

    private void start() {
        Thread jolokiaStartThread = new Thread("JolokiaMcpStart") {
            public void run() {
                try {
                    // block until the server supporting early detection is initialized
                    ServerDetectorLookup lookup = new CachingServerDetectorLookup(new ClasspathServerDetectorLookup());
                    ClassLoader loader = awaitServerInitialization(lookup);
                    config.setClassLoader(loader);

                    server = new JolokiaServer(config, lookup);
                    synchronized (this) {
                        server.start(lazy);
                        setStateMarker();
                        //configureWatcher(server, config);
                    }

                    System.out.println("Jolokia MCP: Agent started with URL " + server.getUrl());
                } catch (RuntimeException e) {
                    System.err.println("Could not start Jolokia MCP agent: " + e);
                }
            }
        };
        jolokiaStartThread.setDaemon(true);
        jolokiaStartThread.start();
    }

    private ClassLoader awaitServerInitialization(ServerDetectorLookup lookup) {
        Set<ServerDetector> detectors = lookup.lookup(new StdoutLogHandler());

        // if some detector (only one!) gives us a ClassLoader, we can use it instead of getClass().getClassLoader()
        // to perform Jolokia Service Manager initialization
        ServerDetector activeDetector = null;
        ClassLoader highOrderClassLoader = null;
        for (ServerDetector detector : detectors) {
            ClassLoader cl = detector.jvmAgentStartup(instrumentation);
            if (cl != null) {
                if (highOrderClassLoader != null) {
                    System.err.printf("Invalid ServerDetector configuration. Detector \"%s\" already provided" +
                                          " a classloader and different detector (\"%s\") overrides it.",
                                      activeDetector, detector);
                    throw new RuntimeException("Invalid ServerDetector configuration");
                } else {
                    highOrderClassLoader = cl;
                    activeDetector = detector;
                }
            }
        }

        return highOrderClassLoader;
    }

    private void stop(boolean waitForWatcher) {
        if (server == null) {
            return;
        }
        try {
            synchronized (JvmAgent.class) {
                server.stop();
                clearStateMarker();
                //stopWatcher(waitForWatcher);
                server = null;
            }
        } catch (RuntimeException e) {
            System.err.println("Could not stop Jolokia agent: " + e);
            e.printStackTrace();
        }
    }

    private void setStateMarker() {
        String url = server.getUrl();
        System.setProperty(JOLOKIA_MCP_AGENT_URL, url);
    }

    private void clearStateMarker() {
        System.clearProperty(JOLOKIA_MCP_AGENT_URL);
        System.out.println("Jolokia MCP: Agent stopped");
    }
}
