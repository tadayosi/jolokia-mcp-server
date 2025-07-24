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
import jakarta.enterprise.context.Dependent;

import io.quarkus.arc.DefaultBean;
import org.jolokia.json.JSONObject;

/**
 * A default, dummy implementation of the {@link JolokiaService} interface
 * that does nothing and returns empty results.
 */
@DefaultBean
@Dependent
public class DefaultJolokiaService implements JolokiaService {

    @Override
    public List<String> listMBeans() {
        return List.of();
    }

    @Override
    public JSONObject listOperations(String mbean) {
        return null;
    }

    @Override
    public JSONObject listAttributes(String mbean) {
        return null;
    }

    @Override
    public Optional<Object> read(String mbean, String attr) {
        return Optional.empty();
    }

    @Override
    public Optional<Object> write(String mbean, String attr, Object value) {
        return Optional.empty();
    }

    @Override
    public Optional<Object> exec(String mbean, String op, Object... args) {
        return Optional.empty();
    }
}
