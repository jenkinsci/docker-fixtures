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

package org.jenkinsci.test.acceptance.docker.fixtures;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.apache.commons.io.IOUtils;
import static org.hamcrest.CoreMatchers.*;
import org.jenkinsci.test.acceptance.docker.Docker;
import org.jenkinsci.test.acceptance.docker.DockerRule;
import org.jenkinsci.utils.process.CommandBuilder;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.AssumptionViolatedException;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class JavaContainerTest {

    @Rule
    public DockerRule<JavaContainer> rule = new DockerRule<>(JavaContainer.class);

    @BeforeClass
    public static void cleanOldImages() throws Exception {
        Process dockerImages;
        try {
            dockerImages = new ProcessBuilder("docker", "images", "--format", "{{.Repository}}:{{.Tag}}").start();
        } catch (IOException x) {
            throw new AssumptionViolatedException("could not run docker", x);
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

    @BeforeClass
    public static void noCache() {
        Docker.NO_CACHE = true;
    }

    @AfterClass
    public static void backToCache() {
        Docker.NO_CACHE = false;
    }

    @Test
    public void smokes() throws Exception {
        JavaContainer c = rule.get();
        assertThat(c.popen(new CommandBuilder("java", "-version")).verifyOrDieWith("could not launch Java"), containsString("openjdk version \"11"));
        c.sshWithPublicKey(new CommandBuilder("id"));
    }

}
