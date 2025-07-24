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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.RuntimeMBeanException;

import org.jolokia.json.JSONArray;
import org.jolokia.json.JSONObject;
import org.jolokia.json.JSONStructure;
import org.jolokia.json.parser.JSONParser;
import org.jolokia.json.parser.ParseException;
import org.jolokia.server.core.backend.BackendManager;
import org.jolokia.server.core.config.ConfigKey;
import org.jolokia.server.core.request.BadRequestException;
import org.jolokia.server.core.request.EmptyResponseException;
import org.jolokia.server.core.request.JolokiaExecRequest;
import org.jolokia.server.core.request.JolokiaListRequest;
import org.jolokia.server.core.request.JolokiaReadRequest;
import org.jolokia.server.core.request.JolokiaRequest;
import org.jolokia.server.core.request.JolokiaRequestFactory;
import org.jolokia.server.core.request.JolokiaWriteRequest;
import org.jolokia.server.core.request.ProcessingParameters;
import org.jolokia.server.core.service.api.JolokiaContext;
import org.jolokia.server.core.util.MimeTypeUtil;
import org.jolokia.server.core.util.RequestType;

public class InVmRequestHandler {

    private final JolokiaContext jolokiaContext;
    private final BackendManager backendManager;
    private final boolean includeRequestGlobal;

    public InVmRequestHandler(JolokiaContext context) {
        jolokiaContext = context;
        backendManager = new BackendManager(context);
        includeRequestGlobal = context.getConfig(ConfigKey.INCLUDE_REQUEST) == null
            || Boolean.parseBoolean(context.getConfig(ConfigKey.INCLUDE_REQUEST));
    }

    /**
     * Handle a GET request
     *
     * @param pUri          URI leading to this request
     * @param pPathInfo     path of the request
     * @param pParameterMap parameters of the GET request  @return the response
     */
    public JSONStructure handleGetRequest(String pUri, String pPathInfo, Map<String, String[]> pParameterMap)
        throws EmptyResponseException {
        String pathInfo = extractPathInfo(pUri, pPathInfo);

        JolokiaRequest jmxReq =
            JolokiaRequestFactory.createGetRequest(pathInfo, getProcessingParameter(pParameterMap));

        if (jolokiaContext.isDebug()) {
            jolokiaContext.debug("URI: " + pUri);
            jolokiaContext.debug("Path-Info: " + pathInfo);
            jolokiaContext.debug("Request: " + jmxReq.toString());
        }
        return executeRequest(jmxReq);
    }

    public JSONObject handleList(String mbean) throws EmptyResponseException {
        JolokiaListRequest list = JolokiaRequestFactory.createGetRequest(
            RequestType.LIST.getName() + "/" + toPath(mbean),
            new ProcessingParameters(new HashMap<>()));
        return executeRequest(list);
    }

    private String toPath(String mbean) {
        if (mbean == null || mbean.isEmpty()) {
            return "";
        }
        return mbean.replaceAll("!", "!!").replaceAll("/", "!/").replaceFirst(":", "/");
    }

    public JSONObject handleRead(String mbean, String attribute) throws EmptyResponseException {
        JolokiaReadRequest read = JolokiaRequestFactory.createGetRequest(
            RequestType.READ.getName() + "/" + mbean + "/" + attribute,
            new ProcessingParameters(new HashMap<>()));
        return executeRequest(read);
    }

    public JSONObject handleWrite(String mbean, String attribute, Object value) throws EmptyResponseException {
        JolokiaWriteRequest write = JolokiaRequestFactory.createGetRequest(
            // TODO: handle object value
            RequestType.WRITE.getName() + "/" + mbean + "/" + attribute + "/" + value,
            new ProcessingParameters(new HashMap<>()));
        return executeRequest(write);
    }

    public JSONObject handleExec(String mbean, String operation, Object... args) throws EmptyResponseException {
        String path = RequestType.EXEC.getName() + "/" + mbean + "/" + operation;
        if (args != null && args.length > 0) {
            path += "/" + Arrays.stream(args).map(Object::toString).collect(Collectors.joining("/"));
        }
        JolokiaExecRequest exec = JolokiaRequestFactory.createGetRequest(
            path,
            new ProcessingParameters(new HashMap<>()));
        return executeRequest(exec);
    }

    /**
     * Get processing parameters from a string-string map
     *
     * @param pParameterMap params to extra. A parameter {@link ConfigKey#PATH_QUERY_PARAM} is used as extra path info
     * @return the processing parameters
     */
    ProcessingParameters getProcessingParameter(Map<String, String[]> pParameterMap) throws BadRequestException {
        Map<ConfigKey, String> config = new HashMap<>();
        if (pParameterMap != null) {
            extractRequestParameters(config, pParameterMap);
            validateRequestParameters(config);
            extractDefaultRequestParameters(config);
        }
        return new ProcessingParameters(config);
    }

    /**
     * Execute a single {@link JolokiaRequest}. If a checked exception occurs,
     * this gets translated into the appropriate JSON object which will get returned.
     * Note, that these exceptions gets *not* translated into an HTTP error, since they are
     * supposed <em>Jolokia</em> specific errors above the transport layer.
     *
     * @param pJmxReq the request to execute
     * @return the JSON representation of the answer.
     */
    private JSONObject executeRequest(JolokiaRequest pJmxReq) throws EmptyResponseException {
        // Call handler and retrieve return value
        try {
            return backendManager.handleRequest(pJmxReq);
        } catch (ReflectionException | InstanceNotFoundException | AttributeNotFoundException e) {
            return getErrorJSON(404, e, pJmxReq);
        } catch (MBeanException e) {
            return getErrorJSON(500, e.getTargetException(), pJmxReq);
        } catch (UnsupportedOperationException | JMException | IOException e) {
            return getErrorJSON(500, e, pJmxReq);
        } catch (IllegalArgumentException e) {
            return getErrorJSON(400, e, pJmxReq);
        } catch (SecurityException e) {
            // Wipe out stacktrace
            return getErrorJSON(403, new Exception(e.getMessage()), pJmxReq);
        } catch (RuntimeMBeanException e) {
            // Use wrapped exception
            return errorForUnwrappedException(e, pJmxReq);
        }
    }

    /**
     * Utility method for handling single runtime exceptions and errors. This method is called
     * in addition to and after {@link #executeRequest(JolokiaRequest)} to catch additional errors.
     * They are two different methods because of bulk requests, where each individual request can
     * lead to an error. So, each individual request is wrapped with the error handling of
     * {@link #executeRequest(JolokiaRequest)}
     * whereas the overall handling is wrapped with this method. It is hence more coarse grained,
     * leading typically to a status code of 500.
     * <p>
     * Summary: This method should be used as last security belt is some exception should escape
     * from a single request processing in {@link #executeRequest(JolokiaRequest)}.
     *
     * @param pThrowable exception to handle
     * @return its JSON representation
     */
    public JSONObject handleThrowable(Throwable pThrowable) {
        if (pThrowable instanceof IllegalArgumentException) {
            return getErrorJSON(400, pThrowable, null);
        } else if (pThrowable instanceof SecurityException) {
            // Wipe out stacktrace
            return getErrorJSON(403, new Exception(pThrowable.getMessage()), null);
        } else {
            return getErrorJSON(500, pThrowable, null);
        }
    }

    /**
     * Get the JSON representation for an exception.
     *
     * @param pErrorCode the HTTP error code to return
     * @param pExp       the exception or error occured
     * @param pJmxReq    request from where to get processing options
     * @return the json representation
     */
    public JSONObject getErrorJSON(int pErrorCode, Throwable pExp, JolokiaRequest pJmxReq) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", pErrorCode);
        jsonObject.put("error", getExceptionMessage(pExp));
        jsonObject.put("error_type", pExp.getClass().getName());
        addErrorInfo(jsonObject, pExp, pJmxReq);
        if (jolokiaContext.isDebug()) {
            jolokiaContext.error("Error " + pErrorCode, pExp);
        }
        if (pJmxReq != null) {
            String includeRequestLocal = pJmxReq.getParameter(ConfigKey.INCLUDE_REQUEST);
            if ((includeRequestGlobal && !"false".equals(includeRequestLocal))
                || (!includeRequestGlobal && "true".equals(includeRequestLocal))) {
                jsonObject.put("request", pJmxReq.toJSON());
            }
        }
        return jsonObject;
    }

    /**
     * Extract configuration parameters from the given HTTP request parameters
     */
    private void extractRequestParameters(Map<ConfigKey, String> pConfig, Map<String, String[]> pParameterMap) {
        for (Map.Entry<String, String[]> entry : pParameterMap.entrySet()) {
            String[] values = entry.getValue();
            if (values != null && values.length > 0) {
                ConfigKey cKey = ConfigKey.getRequestConfigKey(entry.getKey());
                if (cKey != null) {
                    Object value = values[0];
                    pConfig.put(cKey, value != null ? value.toString() : null);
                }
            }
        }
    }

    /**
     * Validation of parameters. Should be called for provided parameter values. Not necessary for built-in/default
     * values.
     */
    private void validateRequestParameters(Map<ConfigKey, String> config) throws BadRequestException {
        // parameters that may be passed with HTTP request:
        //  + callback
        //  + canonicalNaming
        //  + ifModifiedSince
        //  + ignoreErrors (validated in org.jolokia.server.core.request.JolokiaRequest.initParameters())
        //  + includeRequest
        //  + includeStackTrace
        //  + listCache
        //  + listKeys
        //  + maxCollectionSize
        //  + maxDepth
        //  + maxObjects
        //  + mimeType
        //  + p
        //  + serializeException
        //  + serializeLong
        for (Map.Entry<ConfigKey, String> e : config.entrySet()) {
            ConfigKey key = e.getKey();
            String value = e.getValue();
            Class<?> type = key.getType();

            if (type == null) {
                continue;
            }

            if (type == Boolean.class) {
                String v = value.trim().toLowerCase();
                if (!(ConfigKey.enabledValues.contains(v) || ConfigKey.disabledValues.contains(v))) {
                    throw new BadRequestException("Invalid value of " + key.getKeyValue() + " parameter");
                }
            } else if (type == Integer.class) {
                String v = value.trim();
                try {
                    Integer.parseInt(v);
                } catch (NumberFormatException ex) {
                    throw new BadRequestException("Invalid value of " + key.getKeyValue() + " parameter");
                }
            } else if (type == String.class) {
                // validate selected keys
                if (key == ConfigKey.INCLUDE_STACKTRACE) {
                    String v = value.trim().toLowerCase();
                    if (!(ConfigKey.enabledValues.contains(v) || ConfigKey.disabledValues.contains(v)
                        || v.equals("runtime"))) {
                        throw new BadRequestException("Invalid value of " + ConfigKey.INCLUDE_STACKTRACE.getKeyValue() + " parameter");
                    }
                } else if (key == ConfigKey.SERIALIZE_LONG) {
                    String v = value.trim().toLowerCase();
                    if (!("number".equals(v) || "string".equals(v))) {
                        throw new BadRequestException("Invalid value of " + ConfigKey.SERIALIZE_LONG.getKeyValue() + " parameter");
                    }
                } else if (key == ConfigKey.MIME_TYPE) {
                    String v = value.trim().toLowerCase();
                    boolean ok = false;
                    for (String accepted : MimeTypeUtil.ACCEPTED_MIME_TYPES) {
                        if (accepted.equals(v)) {
                            ok = true;
                            break;
                        }
                    }
                    if (!ok) {
                        throw new BadRequestException("Invalid value of " + ConfigKey.MIME_TYPE.getKeyValue() + " parameter");
                    }
                }
            }
        }
    }

    // Add from the global configuration all request relevant parameters which have not
    // already been set in the given map
    private void extractDefaultRequestParameters(Map<ConfigKey, String> pConfig) {
        Set<ConfigKey> globalRequestConfigKeys = jolokiaContext.getConfigKeys();
        for (ConfigKey key : globalRequestConfigKeys) {
            if (key.isRequestConfig() && !pConfig.containsKey(key)) {
                pConfig.put(key, jolokiaContext.getConfig(key));
            }
        }
    }

    private void addErrorInfo(JSONObject pErrorResp, Throwable pExp, JolokiaRequest pJmxReq) {
        if (Boolean.parseBoolean(jolokiaContext.getConfig(ConfigKey.ALLOW_ERROR_DETAILS))) {
            String includeStackTrace = pJmxReq != null ?
                pJmxReq.getParameter(ConfigKey.INCLUDE_STACKTRACE) : "false";
            if (includeStackTrace.equalsIgnoreCase("true") ||
                (includeStackTrace.equalsIgnoreCase("runtime") && pExp instanceof RuntimeException)) {
                StringWriter writer = new StringWriter();
                pExp.printStackTrace(new PrintWriter(writer));
                pErrorResp.put("stacktrace", writer.toString());
            }
            if (pJmxReq != null && pJmxReq.getParameterAsBool(ConfigKey.SERIALIZE_EXCEPTION)) {
                pErrorResp.put("error_value", backendManager.convertExceptionToJson(pExp, pJmxReq));
            }
        }
    }

    // Extract class and exception message for an error message
    private String getExceptionMessage(Throwable pException) {
        String message = pException.getLocalizedMessage();
        return pException.getClass().getName() + (message != null ? " : " + message : "");
    }

    // Unwrap an exception to get to the 'real' exception
    // and extract the error code accordingly
    private JSONObject errorForUnwrappedException(Exception e, JolokiaRequest pJmxReq) {
        Throwable cause = e.getCause();
        int code = cause instanceof IllegalArgumentException ? 400 : cause instanceof SecurityException ? 403 : 500;
        return getErrorJSON(code, cause, pJmxReq);
    }

    // Path info might need some special handling in case when the URL
    // contains two following slashes. These slashes get collapsed
    // when calling getPathInfo() but are still present in the URI.
    // This situation can happen, when slashes are escaped and the last char
    // of a path part is such an escaped slash
    // (e.g. "read/domain:type=name!//attribute")
    // In this case, we extract the path info on our own

    private static final Pattern PATH_PREFIX_PATTERN = Pattern.compile("^/?[^/]+/");

    private String extractPathInfo(String pUri, String pPathInfo) {
        if (pUri.contains("!//")) {
            // Special treatment for trailing slashes in paths
            Matcher matcher = PATH_PREFIX_PATTERN.matcher(pPathInfo);
            if (matcher.find()) {
                String prefix = matcher.group();
                String pathInfoEncoded = pUri.replaceFirst("^.*?" + prefix, prefix);
                return URLDecoder.decode(pathInfoEncoded, StandardCharsets.UTF_8);
            }
        }
        return pPathInfo;
    }
}
