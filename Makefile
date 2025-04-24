RUNNER ?= $(shell pwd)/target/jolokia-mcp-0.3.0-SNAPSHOT-runner.jar

mcp-inspector:
	npx @modelcontextprotocol/inspector

mcp-cli:
	npx @wong2/mcp-cli java -jar ${RUNNER} $(ARGS)
