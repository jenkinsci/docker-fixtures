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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.ClassRule;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Builds a Docker image before running tests, then starts containers on demand.
 * Use with {@link ClassRule}.
 * Compared to {@link DockerRule} this interacts better with {@link Timeout},
 * including rules like {@code JenkinsRule} which run that implicitly:
 * you do not want the timeout applied to a potentially lengthy {@code docker-build} command.
 */
public class DockerClassRule<T extends DockerContainer> implements TestRule {

    private final DockerRule<T> delegate;
    private DockerImage image;
    private List<T> containers = new ArrayList<>();

    public DockerClassRule(Class<T> type) {
        delegate = new DockerRule<>(type);
    }

    public DockerClassRule<T> localOnly() {
        delegate.localOnly();
        return this;
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                image = delegate.build();
                try {
                    base.evaluate();
                } finally {
                    containers.forEach(T::close);
                }
            }
        };
    }

    public T create() throws IOException, InterruptedException {
        T container = image.start(delegate.type).start();
        containers.add(container);
        return container;
    }

}
