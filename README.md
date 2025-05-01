# Jolokia MCP Server

[![Test](https://github.com/jolokia/jolokia-mcp-server/actions/workflows/test.yaml/badge.svg)](https://github.com/jolokia/jolokia-mcp-server/actions/workflows/test.yaml)

MCP server for [Jolokia](https://jolokia.org/), a JMX-HTTP bridge for Java applications. This MCP server enables an LLM to manage a Java application using JMX API via Jolokia.

## Download

- [jolokia-mcp-server-0.3.3-runner.jar](https://github.com/jolokia/jolokia-mcp-server/releases/download/v0.3.3/jolokia-mcp-server-0.3.3-runner.jar)

## Install

To install the Jolokia MCP server to a MCP host, add the following entry to the MCP settings:

```json
{
  "mcpServers": {
    "jolokia": {
      "command": "java",
      "args": [
        "-jar",
        "<path-to-the-runner-jar>/jolokia-mcp-0.3.3-runner.jar"
      ]
    }
  }
}
```

## Run

Run it with `java -jar`:

```console
java -jar jolokia-mcp-server-0.3.3-runner.jar [Jolokia URL]
```

Using [JBang](https://www.jbang.dev/), you don't need to download the jar. You can directly run it with the Maven GAV (`org.jolokia.mcp:jolokia-mcp-server:0.3.3:runner`):

```console
jbang run org.jolokia.mcp:jolokia-mcp-server:0.3.3:runner
```

## Build

```console
mvn clean install
```
