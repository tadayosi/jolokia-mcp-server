# Jolokia MCP Server

[![Test](https://github.com/jolokia/jolokia-mcp-server/actions/workflows/test.yaml/badge.svg)](https://github.com/jolokia/jolokia-mcp-server/actions/workflows/test.yaml)

MCP server for [Jolokia](https://jolokia.org/), a JMX-HTTP bridge for Java applications. This MCP server enables an LLM to manage a Java application using JMX API via Jolokia.

## Build

```console
mvn clean install
```

## Run

```console
java -jar target/jolokia-mcp-server-0.3.0-SNAPSHOT.jar [Jolokia URL]
```

Using JBang:

```console
jbang run org.jolokia.mcp:jolokia-mcp-server:0.3.2:runner
```
