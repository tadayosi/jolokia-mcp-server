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

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pExecResponse;
import org.jolokia.client.request.J4pListRequest;
import org.jolokia.client.request.J4pListResponse;
import org.jolokia.json.JSONObject;

@ApplicationScoped
public class JolokiaClient {

    J4pClient jolokiaClient;

    public JolokiaClient(@ConfigProperty(name = "jolokia.mcp.url", defaultValue = "http://localhost:8778/jolokia")
                         String jolokiaUrl) {
        jolokiaClient = new J4pClient(jolokiaUrl);
    }

    public JSONObject list() throws J4pException {
        J4pListRequest req = new J4pListRequest((String) null);
        J4pListResponse resp = jolokiaClient.execute(req);
        return resp.getValue();
    }

    public Optional<Object> exec(String mbean, String op, Object... args) throws J4pException, MalformedObjectNameException {
        J4pExecRequest req = new J4pExecRequest(mbean, op, args);
        J4pExecResponse resp = jolokiaClient.execute(req);
        return Optional.ofNullable(resp.getValue());
    }
}

