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

package org.jenkinsci.test.acceptance.docker;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import org.apache.commons.io.FileUtils;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Starts a Docker container for a test.
 */
public final class DockerRule<T extends DockerContainer> implements TestRule {

    private final Class<T> type;
    private boolean localOnly;
    private T container;
    private File buildlog, runlog;

    public DockerRule(Class<T> type) {
        this.type = type;
    }

    public DockerRule<T> localOnly() {
        localOnly = true;
        return this;
    }

    public T get() throws IOException, InterruptedException {
        if (container == null) {
            // Adapted from WithDocker:
            Docker docker = new Docker();
            if (!docker.isAvailable()) {
                throw new AssumptionViolatedException("Docker is needed for the test");
            }
            if (localOnly) {
                String host = DockerImage.getDockerHost();
                if (!InetAddress.getByName(host).isLoopbackAddress()) {
                    throw new AssumptionViolatedException("Docker is needed locally for the test but is running on " + host);
                }
            }
            // Adapted from DockerContainerHolder:
            buildlog = File.createTempFile("docker-" + type.getSimpleName() + "-build", ".log");
            runlog = File.createTempFile("docker-" + type.getSimpleName() + "-run", ".log");
            DockerImage.Starter<T> containerStarter = docker.build(type, buildlog).start(type);
            container = containerStarter.withLog(runlog).start();
        }
        return container;
    }

    // Mixture of logic from ExternalResource and TestWatcher
    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    base.evaluate();
                } catch (AssumptionViolatedException e) {
                    throw e;
                } catch (Throwable t) {
                    dump(buildlog);
                    dump(runlog);
                    throw t;
                } finally {
                    if (buildlog != null) {
                        buildlog.delete();
                        buildlog = null;
                    }
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
            private void dump(File log) throws IOException {
                if (log != null) {
                    System.out.println("---%<--- " + log.getName());
                    FileUtils.copyFile(log, System.out);
                    System.out.println("--->%---");
                }
            }
        };
    }

}
