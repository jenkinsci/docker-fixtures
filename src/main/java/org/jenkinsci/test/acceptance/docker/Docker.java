package org.jenkinsci.test.acceptance.docker;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.utils.process.CommandBuilder;
import org.jvnet.hudson.annotation_indexer.Index;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Entry point to the docker support.
 * <p>
 * Use this subsystem by injecting this class into your test.
 *
 * @author Kohsuke Kawaguchi
 * @author asotobueno
 */
public class Docker {

    /**
     * Command to invoke docker.
     */
    private static final List<String> dockerCmd = Collections.singletonList("docker");

    public ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    public Docker() {
    }

    public static CommandBuilder cmd(String... cmd) {
        return new CommandBuilder(dockerCmd).add(cmd);
    }

    /**
     * Checks if docker is available on this system.
     */
    public boolean isAvailable() {
        try {
            return cmd("ps").popen().waitFor() == 0;
        } catch (InterruptedException | IOException e) {
            return false;
        }
    }

    /**
     * Checks if a given container is currently running.
     *
     * @param container Container id
     * @return false if the container is not running, true otherwise
     */
    public boolean isContainerRunning(String container) throws IOException, InterruptedException {
        ProcessBuilder pBuilder = cmd("ps").add("-q", "--filter", "\"id=" + container + '"').build();
        Process psProcess = pBuilder.start();

        String psOutput = IOUtils.toString(psProcess.getInputStream());
        int pExit = psProcess.waitFor();
        if (pExit == 0) {
            return psOutput.contains(container);
        }
        // docker command errored - and it does not do this if there is no match.
        System.err.println("docker ps failed with code: " + pExit +
                          (psOutput != null ? " and output: " + psOutput : " and provided no output"));
        return false;
    }

    /**
     * Builds a docker image.
     *
     * @param image Name of the image to be built.
     * @param dir   Directory that contains Dockerfile
     */
    private DockerImage build(String image, File dir) throws IOException, InterruptedException {
        return build(image, dir, null);
    }

    @SuppressFBWarnings(value = "MS_SHOULD_BE_FINAL", justification = "TODO needs triage")
    public static boolean NO_CACHE;

    /**
     * Builds a docker image.
     *
     * @param image Name of the image to be built.
     * @param dir   Directory that contains Dockerfile
     * @param log   Log file to store image building output
     */
    private DockerImage build(String image, File dir, /*@CheckForNull*/ File log) throws IOException, InterruptedException {
        // compute tag from the content of Dockerfile
        String tag = getDockerFileHash(dir);
        String fullTag = image + ":" + tag;

        CommandBuilder buildCmd = cmd("build").add("-t", fullTag);
        buildCmd.add(dir);
        ProcessBuilder processBuilder = buildCmd.build().redirectErrorStream(true);
        if (log != null) {
            processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
        } else {
            processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        }
        if (NO_CACHE) {
            buildCmd.add("--no-cache=true");
        }

        StringBuilder sb = new StringBuilder("Building Docker image `").append(buildCmd.toString()).append("`");
        if (log != null) {
            sb.append(": logfile is at ").append(log);
        }
        System.out.println(sb.toString());

        int exit = processBuilder.start().waitFor();
        if (exit != 0) {
            throw new Error("Failed to build image (" + exit + "): " + tag);
        }
        return new DockerImage(fullTag);
    }

    public DockerImage build(Class<? extends DockerContainer> fixture) throws IOException, InterruptedException {
        File buildlog = File.createTempFile("docker-" + fixture.getSimpleName() + "-build", ".log");
        DockerImage image = null;
        try {
            image = build(fixture, buildlog);
        } finally {
            if (image == null) {
                dump(buildlog);
            }
            buildlog.delete();
        }
        return image;
    }

    public static void dump(File log) throws IOException {
        if (log != null) {
            System.out.println("---%<--- " + log.getName());
            FileUtils.copyFile(log, System.out);
            System.out.println("--->%---");
        }
    }

    public DockerImage build(Class<? extends DockerContainer> fixture, File log) throws IOException, InterruptedException {
        if (fixture.getSuperclass() != DockerContainer.class && fixture.getSuperclass() != DynamicDockerContainer.class) {
            build((Class) fixture.getSuperclass(), log); // build the base image first
        }

        try {
            DockerFixture f = fixture.getAnnotation(DockerFixture.class);
            if (f == null) {
                throw new AssertionError(fixture + " is missing @DockerFixture");
            }

            File dir = Files.createTempDirectory("docker-build").toFile();
            try {
                copyDockerfileDirectory(fixture, f, dir);
                return build("jenkins/" + f.id(), dir, log);
            } finally {
                FileUtils.deleteDirectory(dir);
            }
        } catch (InterruptedException | IOException e) {
            throw new IOException("Failed to build image: " + fixture, e);
        }
    }

    //package scope for testing purposes. Ideally we should encapsulate Docker interactions so they can be mocked
    // and call public method.
    void copyDockerfileDirectory(Class<? extends DockerContainer> fixture, DockerFixture f, File dir)
            throws IOException {
        String dockerfilePath = resolveDockerfileLocation(fixture, f);
        copyDockerfileDirectoryFromClasspath(fixture, dockerfilePath, dir);
    }

    private String resolveDockerfileLocation(Class<? extends DockerContainer> fixture, DockerFixture f) {
        String prefix;
        if(isSpecificDockerfileLocationSet(f)) {
            prefix = f.dockerfileFolder();
        } else {
            prefix = fixture.getName();
        }
        return prefix.replace('.', '/').replace('$', '/');
    }

    private void copyDockerfileDirectoryFromClasspath(Class<? extends DockerContainer> fixture, String dockerfileLocation, File dir) throws IOException {
        File jar = null;
        try {
            jar = Which.jarFile(fixture);
        } catch (IllegalArgumentException e) {
            // fall through
        }

        if (jar!=null && jar.isFile()) {
            // files are packaged into a jar/war. extract them
            dockerfileLocation += "/";
            copyDockerfileDirectoryFromPackaged(jar, dockerfileLocation, dir);
        } else {
            // Dockerfile is not packaged into a jar file, so copy locally
            copyDockerfileDirectoryFromLocal(dockerfileLocation, dir);
        }
        // if the fixture is dynamic (needs to know something about our environment then process it.
        if (DynamicDockerContainer.class.isAssignableFrom(fixture)) {
            try {
                DynamicDockerContainer newInstance = (DynamicDockerContainer) fixture.newInstance();
                newInstance.process(new File(dir, "Dockerfile"));
            }
            catch (InstantiationException | IllegalAccessException ex) {
                throw new IOException("Could not transfrom Dockerfile", ex);
            }
        }
    }

    private boolean isSpecificDockerfileLocationSet(DockerFixture f) {
        return !f.dockerfileFolder().isEmpty();
    }

    private void copyDockerfileDirectoryFromPackaged(File jar, String fixtureLocation, File outputDirectory) throws IOException {
        try (JarFile j = new JarFile(jar)) {
            Enumeration<JarEntry> e = j.entries();
            while (e.hasMoreElements()) {
                JarEntry je = e.nextElement();
                if (je.getName().startsWith(fixtureLocation)) {
                    File dst = new File(outputDirectory, je.getName().substring(fixtureLocation.length()));
                    if (je.isDirectory()) {
                        dst.mkdirs();
                    } else {
                        try (InputStream in = j.getInputStream(je)) {
                            FileUtils.copyInputStreamToFile(in, dst);
                        }
                    }
                }
            }
        }
    }

    private void copyDockerfileDirectoryFromLocal(String fixtureLocation, File outputDirectory) throws IOException {
        URL resourceDir = classLoader.getResource(fixtureLocation);
        if (resourceDir == null) throw new Error("The fixture directory does not exist: " + fixtureLocation);
        copyFile(outputDirectory, resourceDir);
    }

    private void copyFile(File outputDirectory, URL resourceDir) throws IOException {
        File dockerFileDir;
        try {
            dockerFileDir = new File(resourceDir.toURI());
        } catch (URISyntaxException e) {
            dockerFileDir = new File(resourceDir.getPath());
        }
        FileUtils.copyDirectory(dockerFileDir, outputDirectory);
    }

    private String getDockerFileHash(File dockerFileDir) {
        File dockerFile = new File(dockerFileDir, "Dockerfile");
        SHA1Sum dockerFileHash = new SHA1Sum(dockerFile);
        return dockerFileHash.getSha1String().substring(0, 12);
    }

    /**
     * Finds a fixture class that has the specified ID.
     *
     * @see org.jenkinsci.test.acceptance.docker.DockerFixture#id()
     */
    public Class<? extends DockerContainer> findFixture(String id) throws IOException {
        for (Class<?> t : Index.list(DockerFixture.class, classLoader, Class.class)) {
            if (t.getAnnotation(DockerFixture.class).id().equals(id)) {
                return t.asSubclass(DockerContainer.class);
            }
        }
        throw new IllegalArgumentException("No such docker fixture found: " + id);
    }

}
