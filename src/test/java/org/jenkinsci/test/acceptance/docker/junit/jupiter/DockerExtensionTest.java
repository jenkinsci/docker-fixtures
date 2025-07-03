package org.jenkinsci.test.acceptance.docker.junit.jupiter;

import org.jenkinsci.test.acceptance.docker.DockerContainer;
import org.jenkinsci.test.acceptance.docker.DockerFixture;
import org.jenkinsci.test.acceptance.docker.fixtures.SshdContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class DockerExtensionTest {

    @RegisterExtension
    private static final DockerExtension<SshdContainer> STATIC_EXTENSION = new DockerExtension<>(SshdContainer.class);

    @RegisterExtension
    private static final DockerExtension<CustomContainer> CUSTOM_EXTENSION = new DockerExtension<>(CustomContainer.class);

    @RegisterExtension
    private final DockerExtension<SshdContainer> instanceExtension = new DockerExtension<>(SshdContainer.class);

    @BeforeAll
    static void beforeAll() throws Exception {
        STATIC_EXTENSION.get().assertRunning();
        // instanceExtension.get().assertRunning();
        CUSTOM_EXTENSION.get().assertRunning();
    }

    @BeforeEach
    void beforeEach() throws Exception {
        STATIC_EXTENSION.get().assertRunning();
        instanceExtension.get().assertRunning();
        CUSTOM_EXTENSION.get().assertRunning();
    }

    @AfterAll
    static void afterAll() throws Exception {
        STATIC_EXTENSION.get().assertRunning();
        // instanceExtension.get().assertRunning();
        CUSTOM_EXTENSION.get().assertRunning();
    }

    @AfterEach
    void afterEach() throws Exception {
        STATIC_EXTENSION.get().assertRunning();
        instanceExtension.get().assertRunning();
        CUSTOM_EXTENSION.get().assertRunning();
    }

    @Test
    void testContainers() throws Exception {
        STATIC_EXTENSION.get().assertRunning();
        // instanceExtension.get().assertRunning();
        CUSTOM_EXTENSION.get().assertRunning();
    }

    @Test
    @Disabled("Used to manually check behavior of DockerExtension#interceptTestMethod")
    void testAssumption() {
        assumeFalse(true, "Test is skipped");
    }

    @Test
    @Disabled("Used to manually check behavior of DockerExtension#interceptTestMethod")
    void testFailure() {
        assertTrue(false, "Test fails");
    }

    @DockerFixture(id = "custom", ports = 8080)
    public static class CustomContainer extends DockerContainer {

    }
}
