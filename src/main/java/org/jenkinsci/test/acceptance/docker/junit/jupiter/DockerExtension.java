/*
 * The MIT License
 *
 * Copyright 2017 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.test.acceptance.docker.junit.jupiter;

import org.jenkinsci.test.acceptance.docker.Docker;
import org.jenkinsci.test.acceptance.docker.DockerClassRule;
import org.jenkinsci.test.acceptance.docker.DockerContainer;
import org.jenkinsci.test.acceptance.docker.DockerImage;
import org.jenkinsci.test.acceptance.docker.DockerRule;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.opentest4j.TestAbortedException;

import java.io.File;
import java.lang.reflect.Method;
import java.net.InetAddress;

/**
 * Provides a given {@link DockerContainer} for JUnit5 tests.
 * <p>
 * When registered as a {@code static} variable, the underlying container is started before all tests and stopped once all tests concluded.
 * <pre>{@code
 * @RegisterExtension
 * private static final DockerExtension<JavaContainer> container = new DockerExtension(JavaContainer.class);
 * }</pre>
 * When registered as an {@code instance} variable, the underlying container is started / stopped before / after each test.
 * <pre>{@code
 * @RegisterExtension
 * private final DockerExtension<JavaContainer> container = new DockerExtension(JavaContainer.class);
 * }</pre>
 * <p>
 * This is the JUnit5 implementation of {@link DockerRule} and {@link DockerClassRule}.
 *
 * @see DockerContainer
 * @see DockerRule
 * @see DockerClassRule
 */
public final class DockerExtension<T extends DockerContainer>
        implements BeforeAllCallback, BeforeEachCallback, InvocationInterceptor, AfterAllCallback, AfterEachCallback {

    final Class<T> type;
    private boolean localOnly;
    private T container;
    private File runlog;

    private boolean isNonStatic = false;

    /**
     * Construct an instance of the extension for the given {@link DockerContainer} type.
     *
     * @param type the {@link DockerContainer} class
     */
    public DockerExtension(Class<T> type) {
        this.type = type;
    }

    /**
     * Enforce that the docker host must be local.
     *
     * @return this
     */
    public DockerExtension<T> localOnly() {
        localOnly = true;
        return this;
    }

    /**
     * Returns the {@link DockerContainer} managed by this extension.
     *
     * @return the {@link DockerContainer} managed by this extension.
     */
    public T get() {
        if (container == null) {
            throw new IllegalStateException("Container was not initialized - make sure to register it via @RegisterExtension");
        }
        return container;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        start();
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (container == null) {
            isNonStatic = true;
            start();
        }
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        try {
            invocation.proceed();
        } catch (TestAbortedException e) {
            throw e;
        } catch (Throwable t) {
            Docker.dump(runlog);
            throw t;
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        stop();
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (isNonStatic) {
            stop();
        }
    }

    private void start() throws Exception {
        if (container == null) {
            // Adapted from WithDocker:
            Docker docker = new Docker();
            if (!docker.isAvailable()) {
                throw new TestAbortedException("Docker is needed for the test");
            }
            if (localOnly) {
                String host = DockerImage.getDockerHost();
                if (!InetAddress.getByName(host).isLoopbackAddress()) {
                    throw new TestAbortedException("Docker is needed locally for the test but is running on " + host);
                }
            }

            DockerImage image = docker.build(type);
            runlog = File.createTempFile("docker-" + type.getSimpleName() + "-run", ".log");
            container = image.start(type).withLog(runlog).start();
        }
    }

    private void stop() {
        if (runlog != null) {
            runlog.delete();
            runlog = null;
        }
        // From DockerContainerHolder:
        if (container != null) {
            container.close();
            container = null;
        }
    }

}
