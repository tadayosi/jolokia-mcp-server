package org.jolokia.mcp;

import jakarta.inject.Inject;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(JettyTestResource.class)
@QuarkusTestResource(JettyTestResource.class)
class JolokiaMcpServerTest {

    @Inject
    JolokiaMcpServer jolokiaMcpServer;

    @Test
    void toolName() {
        assertEquals("javalang-Memory-gc",
                     jolokiaMcpServer.toolName("java.lang", "type=Memory", "gc", 0));
        assertEquals("javalang-Threading-dumpAllThreads-1",
                     jolokiaMcpServer.toolName("java.lang", "type=Threading", "dumpAllThreads", 1));
        assertEquals("jolokia-123456-jvm-Config-debugInfo",
                     jolokiaMcpServer.toolName("jolokia", "agent=123456-jvm,type=Config", "debugInfo", 0));
    }
}
