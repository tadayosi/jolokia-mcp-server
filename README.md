# Jolokia MCP Server

[![Test](https://github.com/jolokia/jolokia-mcp-server/actions/workflows/test.yaml/badge.svg)](https://github.com/jolokia/jolokia-mcp-server/actions/workflows/test.yaml)

MCP server for [Jolokia](https://jolokia.org/), a JMX-HTTP bridge for Java applications. This MCP server enables an LLM to manage a Java application using JMX API via Jolokia.

<https://github.com/user-attachments/assets/624ec93b-da69-49b5-be8f-02f2ff14bd2e>

## Distributions

Since version 0.4, there are two distributions of Jolokia MCP Server:

- [Standalone MCP Server](#standalone-mcp-server)
- [JVM Agent MCP Server](#jvm-agent-mpc-server)

### Standalone MCP Server

The standalone MCP Server acts as a typical MCP server: it registers with the MCP host via stdio or HTTP, and the MCP server itself communicates with the Java application to which the Jolokia agent is attached via JMX over HTTP.

Therefore, it requires to attach a Jolokia agent to your Java application in order to use it to operate your Java application. Read the Jolokia manual for how to attach a Jolokia agent to a Java application:

<https://jolokia.org/reference/html/manual/agents.html>

### JVM Agent MPC Server

On the other hand, the JVM Agent MCP Server works as a drop-in replacement for the [standard Jolokia JVM Agent](https://jolokia.org/reference/html/manual/agents.html#agents-jvm).

## Features

This MCP server connects to a single JVM at startup and provides the following features on the connected JVM:

- List MBeans from the connected JVM
- List operations for a MBean
- List attributes for a MBean
- Read/write attributes of a MBean
- Execute operations on a MBean

### Tools

This MCP server provides 6 tools.

- **listMBeans**
  - List available MBeans from the JVM
  - Output (`List<String>`): List of all MBean object names in the JVM
- **listMBeanOperations**
  - List available operations for a given MBean
  - Inputs:
    - `mbean` (`String`): MBean name
  - Output (`String`): JSON-formatted definitions of all available operations for the given MBean
- **listMBeanAttributes**
  - List available attributes for a given MBean
  - Inputs:
    - `mbean` (`String`): MBean name
  - Output (`String`): JSON-formatted definitions of all available attributes for the given MBean
- **readMBeanAttribute**
  - Read an attribute from a given MBean
  - Inputs:
    - `mbean` (`String`): MBean name
    - `attribute` (`String`): Attribute name
  - Output (`String`): String representation of the given attribute's value or "null"
- **writeMBeanAttribute**
  - Set the value to an attribute of a given MBean
  - Inputs:
    - `mbean` (`String`): MBean name
    - `attribute` (`String`): Attribute name
    - `value` (`Object`): Attribute value
  - Output (`String`): String representation of the given attribute's previous value or "null"
- **executeMBeanOperation**
  - Execute an operation on a given MBean
  - Inputs:
    - `mbean` (`String`): MBean name
    - `operation` (`String`): Operation name
    - `args` (`Object[]`): Arguments
  - Output (`String`): String representation of the return value of the operation or "null"

## Install

Download the MCP server runner jar:

- [jolokia-mcp-server-0.4.1-runner.jar](https://github.com/jolokia/jolokia-mcp-server/releases/download/v0.4.1/jolokia-mcp-server-0.4.1-runner.jar)

To install the Jolokia MCP server to a MCP host, add the following entry to the MCP settings:

```json
{
  "mcpServers": {
    "jolokia": {
      "command": "java",
      "args": [
        "-jar",
        "<path-to-the-runner-jar>/jolokia-mcp-0.4.1-runner.jar"
      ]
    }
  }
}
```

Or if you prefer using [JBang](https://www.jbang.dev/) (no need for downloading the jar with this method):

```json
{
  "mcpServers": {
    "jolokia": {
      "command": "jbang",
      "args": [
        "org.jolokia.mcp:jolokia-mcp-server:0.4.1:runner"
      ]
    }
  }
}
```

## Run

Run it with `java -jar`:

```console
java -jar jolokia-mcp-server-0.4.1-runner.jar [Jolokia URL]
```

Using JBang, you can directly run it with the Maven GAV (`org.jolokia.mcp:jolokia-mcp-server:0.4.1:runner`):

```console
jbang org.jolokia.mcp:jolokia-mcp-server:0.4.1:runner
```

### HTTP/SSE Transport

By default, this MCP server runs with stdio transport. To switch it to HTTP/SSE transport, use the `--sse` option:

```console
java -jar jolokia-mcp-server-0.4.1-runner.jar --sse
```

The HTTP/SSE transport endpoint by default launches at <http://localhost:8080/mcp/sse>.

## Config Options

| Parameter/Option | Default | Description |
| ---------------- | ------- | ----------- |
| Positional parameter | `http://localhost:8778/jolokia` | The Jolokia endpoint URL the MCP server connects to |
| `--sse` | `false` (stdio) | Enable HTTP/SSE transport |
| `-D*=*` | | System properties |

The system properties that are relevant to the MCP server:

| System property | Default | Description |
| --------------- | ------- | ----------- |
| `quarkus.http.port` | `8080` | (SSE) The port for the SSE endpoint |
| `quarkus.mcp.server.sse.root-path` | `mcp` | (SSE) The root path for the SSE endpoint (`http://localhost:8080/mcp/sse`) |
| `jolokia.mcp.url` | `http://localhost:8778/jolokia` | Equivalent to the positional parameter |

## Build

```console
mvn clean install
```
