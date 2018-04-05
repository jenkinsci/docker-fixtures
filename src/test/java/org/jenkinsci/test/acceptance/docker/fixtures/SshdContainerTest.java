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

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import org.jenkinsci.test.acceptance.docker.DockerRule;
import org.junit.Rule;
import org.junit.Test;

import org.jenkinsci.utils.process.CommandBuilder;
import static org.junit.Assert.assertThat;

public class SshdContainerTest {

    @Rule
    public DockerRule<SshdContainer> container = new DockerRule<>(SshdContainer.class);
    @Rule
    public DockerRule<JavaContainer> javaContainerRule = new DockerRule<>(JavaContainer.class);

    @Test
    public void smokes() throws Exception {
        String version = container.get().ssh().add("echo \"$(id -nu):$(id -ng)\"").popen().verifyOrDieWith("Unable to query test user");
        assertThat(version, containsString("test:test"));
    }

    @Test
    public void locale() throws Exception {
        JavaContainer c = javaContainerRule.get();
        // cf. https://stackoverflow.com/a/4384506/12916
        assertThat(c.popen(new CommandBuilder("jrunscript", "-e", "'println(java.lang.System.getProperty(\"file.encoding\") + \" vs. \" + java.lang.System.getProperty(\"sun.jnu.encoding\"))'")).verifyOrDieWith("could not run jrunscript"),
            containsString("UTF-8 vs. UTF-8"));
        assertThat(c.popen(new CommandBuilder("jrunscript", "-e", "'var f = new java.io.File(\"hello\\u010d\\u0950\"); new java.io.FileOutputStream(f).close(); println(\"name: \" + java.net.URLEncoder.encode(f.name, \"UTF-8\")); println(\"exists: \" + f.file)'")).verifyOrDieWith("could not run jrunscript"),
            allOf(containsString("exists: true"), containsString("name: hello%C4%8D%E0%A5%90")));
        assertThat(c.popen(new CommandBuilder("sh", "-c", "'ls | native2ascii -encoding UTF-8'")).verifyOrDieWith("could not run ls"),
            containsString("hello\\u010d\\u0950"));
    }

}
