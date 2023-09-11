/*
 * The MIT License
 *
 * Copyright 2018 CloudBees, Inc.
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

package io.jenkins.plugins.httpclient;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThrows;

import hudson.AbortException;
import hudson.model.TaskListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;

public class RobustHTTPClientTest {

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    @WithoutJenkins
    public void sanitize() throws Exception {
        assertThat(
                RobustHTTPClient.sanitize(new URL("http://example.com/some/long/path")),
                is("http://example.com/some/long/path"));
        assertThat(
                RobustHTTPClient.sanitize(new URL("https://example.com/some/path?auth=s3cr3t")),
                is("https://example.com/some/path?…"));
        assertThat(
                RobustHTTPClient.sanitize(new URL("https://user:s3cr3t@example.com/otherpath")),
                is("https://…@example.com/otherpath"));
    }

    @Test
    @WithoutJenkins
    public void sanitizeThrowsException() {
        final AssertionError e = assertThrows(AssertionError.class, () -> {
            RobustHTTPClient.sanitize(new URL("https://example.com/ /has/space/in/url/"));
        });
        assertThat(e.getCause().getMessage(), containsString("Illegal character in path at index 20"));
    }

    @Test
    public void testDownloadFile() throws Exception {
        JenkinsRule.WebClient wc = j.createWebClient();
        RobustHTTPClient client = wc.executeOnServer(() -> {
            return new RobustHTTPClient();
        });
        File f = new File(tempFolder.newFolder(), "jenkins-index.html");
        client.downloadFile(f, j.getURL(), TaskListener.NULL);
        assertThat(Files.readString(f.toPath()), containsString("<title>Dashboard [Jenkins]</title>"));
    }

    @Test
    public void testDownloadFileWithTimeoutException() throws Exception {
        JenkinsRule.WebClient wc = j.createWebClient();
        RobustHTTPClient client = wc.executeOnServer(() -> {
            RobustHTTPClient internalClient = new RobustHTTPClient();
            internalClient.setStopAfterAttemptNumber(1);
            internalClient.setTimeout(50, TimeUnit.MICROSECONDS);
            return internalClient;
        });
        File f = new File(tempFolder.newFolder(), "jenkins-index.html");
        final IOException e = assertThrows(IOException.class, () -> {
            client.downloadFile(f, j.getURL(), TaskListener.NULL);
        });
        assertThat(e.getCause(), isA(TimeoutException.class));
    }

    @Test
    public void testDownloadFileFromNonExistentLocation() throws Exception {
        JenkinsRule.WebClient wc = j.createWebClient();
        RobustHTTPClient client = wc.executeOnServer(() -> {
            return new RobustHTTPClient();
        });
        File f = new File(tempFolder.newFolder(), "jenkins-index.html");
        URL badURL = new URL(j.getURL().toString() + "/page/does/not/exist");
        final AbortException e = assertThrows(AbortException.class, () -> {
            client.downloadFile(f, badURL, TaskListener.NULL);
        });
        assertThat(e.getMessage(), containsString("Failed to download "));
    }

    @Test
    public void testDownloadFileStopAfterOneAttempt() throws Exception {
        JenkinsRule.WebClient wc = j.createWebClient();
        RobustHTTPClient client = wc.executeOnServer(() -> {
            RobustHTTPClient internalClient = new RobustHTTPClient();
            internalClient.setStopAfterAttemptNumber(1);
            return internalClient;
        });
        File f = new File(tempFolder.newFolder(), "jenkins-index.html");
        URL badURL = new URL(j.getURL().toString() + "/page/does/not/exist");
        final AbortException e = assertThrows(AbortException.class, () -> {
            client.downloadFile(f, badURL, TaskListener.NULL);
        });
        assertThat(e.getMessage(), containsString("Failed to download "));
    }
}
