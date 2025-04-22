# Jolokia MCP Server

[![Test](https://github.com/tadayosi/jolokia-mcp/actions/workflows/test.yaml/badge.svg)](https://github.com/tadayosi/jolokia-mcp/actions/workflows/test.yaml)

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
jbang run --repos=jitpack com.github.tadayosi:jolokia-mcp:main-SNAPSHOT:runner
```
