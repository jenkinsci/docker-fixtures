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

package org.jenkinsci.test.acceptance.docker.fixtures.junit.jupiter;

import org.apache.commons.io.IOUtils;
import org.jenkinsci.test.acceptance.docker.Docker;
import org.jenkinsci.test.acceptance.docker.fixtures.JavaContainer;
import org.jenkinsci.test.acceptance.docker.junit.jupiter.DockerExtension;
import org.jenkinsci.utils.process.CommandBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.opentest4j.TestAbortedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

class JavaContainerTest {

    @RegisterExtension
    private static final DockerExtension<JavaContainer> CONTAINER = new DockerExtension<>(JavaContainer.class);

    @BeforeAll
    static void cleanOldImages() throws Exception {
        Process dockerImages;
        try {
            dockerImages = new ProcessBuilder("docker", "images", "--format", "{{.Repository}}:{{.Tag}}").start();
        } catch (IOException x) {
            throw new TestAbortedException("could not run docker", x);
        }
        Scanner scanner = new Scanner(dockerImages.getInputStream());
        List<String> toRemove = new ArrayList<>();
        while (scanner.hasNext()) {
            String image = scanner.next();
            if (image.startsWith("jenkins/")) {
                toRemove.add(image);
            }
        }
        dockerImages.waitFor();
        if (!toRemove.isEmpty()) {
            toRemove.add(0, "docker");
            toRemove.add(1, "rmi");
            Process dockerRmi = new ProcessBuilder(toRemove).redirectErrorStream(true).start();
            // Cannot use inheritIO from Surefire.
            IOUtils.copy(dockerRmi.getInputStream(), System.err);
            dockerRmi.waitFor();
        }
    }

    @BeforeAll
    static void noCache() {
        Docker.NO_CACHE = true;
    }

    @AfterAll
    static void backToCache() {
        Docker.NO_CACHE = false;
    }

    @Test
    void smokes() throws Exception {
        JavaContainer c = CONTAINER.get();
        assertThat(c.popen(new CommandBuilder("java", "-version")).verifyOrDieWith("could not launch Java"), containsString("openjdk version \"17"));
        c.sshWithPublicKey(new CommandBuilder("id"));
    }

}
