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

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class TestUtil {

    private TestUtil() {
    }

    /**
     * Get an arbitrary free port on the localhost
     *
     * @return free port
     * @throws IllegalArgumentException if no free port could be found
     */
    public static int getFreePort() throws IOException {
        for (int port = 22332; port < 22500; port++) {
            if (trySocket(port)) {
                return port;
            }
        }
        throw new IllegalStateException("Cannot find a single free port");
    }

    /**
     * Try a given port on the localhost and check whether it is free
     *
     * @param port port to check
     * @return true if the port is still free
     */
    @SuppressWarnings({ "PMD.SystemPrintln" })
    public static boolean trySocket(int port) throws IOException {
        InetAddress address = Inet4Address.getByName("localhost");
        try (ServerSocket s = new ServerSocket()) {
            s.bind(new InetSocketAddress(address, port));
            return true;
        } catch (IOException exp) {
            System.err.println("Port " + port + " already in use, trying next ...");
            // exp.printStackTrace();
            // next try ....
        }
        return false;
    }
}
