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

import org.jenkinsci.test.acceptance.docker.fixtures.JavaContainer;
import org.jenkinsci.test.acceptance.docker.fixtures.SshdContainer;
import org.jenkinsci.test.acceptance.docker.junit.jupiter.DockerExtension;
import org.jenkinsci.utils.process.CommandBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

class SshdContainerTest {

    @RegisterExtension
    private static final DockerExtension<SshdContainer> SSHD_CONTAINER = new DockerExtension<>(SshdContainer.class);
    @RegisterExtension
    private static final DockerExtension<JavaContainer> JAVA_CONTAINER = new DockerExtension<>(JavaContainer.class);

    @Test
    void smokes() throws Exception {
        String version = SSHD_CONTAINER.get().ssh().add("echo \"$(id -nu):$(id -ng)\"").popen().verifyOrDieWith("Unable to query test user");
        assertThat(version, containsString("test:test"));
    }

    @Test
    void locale() throws Exception {
        JavaContainer c = JAVA_CONTAINER.get();
        // cf. https://stackoverflow.com/a/4384506/12916
        assertThat(c.popen(new CommandBuilder("echo", "'System.out.println(System.getProperty(\"file.encoding\") + \" vs. \" + System.getProperty(\"sun.jnu.encoding\"))'", "| jshell -")).verifyOrDieWith("could not run jshell"),
                containsString("UTF-8 vs. UTF-8"));
        assertThat(c.popen(new CommandBuilder("echo", "'var f = new java.io.File(\"hello\\u010d\\u0950\"); new java.io.FileOutputStream(f).close(); System.out.println(\"name: \" + java.net.URLEncoder.encode(f.getName(), \"UTF-8\")); System.out.println(\"exists: \" + f.exists())'", "| jshell -")).verifyOrDieWith("could not run jshell"),
                allOf(containsString("exists: true"), containsString("name: hello%C4%8D%E0%A5%90")));
    }

}
