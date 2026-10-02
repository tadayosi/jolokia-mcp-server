# AGENTS.md

Guidelines for AI coding agents working on this repository.

## Project Info

- Language: Java 17+
- Build tool/Package manager: Maven
- Key dependencies: Jolokia, Quarkus, Quarkus MCP Server
- Commit style: Conventional Commits (e.g., `feat:`, `fix:`, `chore:`, `docs:`, `ci:`)

## Project Structure

```text
.
├── agent-jvm/    # JVM agent distribution for in-VM MCP hosting
├── core/         # Common MCP server logic, tool definitions, and Jolokia service interface
├── server/       # Standalone MCP server CLI application
├── Makefile      # Development helper targets
└── pom.xml       # Root Maven multi-module POM
```

## Documentation Index

Read these documents **only when the task requires it** — do not load them all upfront.

| Document | When to read |
| --- | --- |
| [`README.md`](README.md) | Features, installation, usage, configuration, and tool specifications |
| [`Makefile`](Makefile) | Helper commands for testing with MCP Inspector / CLI and downloading binaries |
| [Quarkus MCP Server Documentation](https://docs.quarkiverse.io/quarkus-mcp-server/dev/index.html) | Quarkus MCP Server extension features, annotations (`@Tool`), and transport configuration |
| [Jolokia Documentation](https://jolokia.org/reference/html/manual/agents.html) | Jolokia agent concepts, HTTP-JMX bridging, and agent configuration options |

## Essential Commands

```bash
# Build & install all modules
mvn clean install

# Launch MCP Inspector (requires npm)
make mcp-inspector

# Run MCP CLI against built runner jar
make mcp-cli ARGS="http://localhost:8778/jolokia"
```
