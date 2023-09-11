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
import hudson.model.UnprotectedRootAction;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;

public class RobustHTTPClientIntegrationTest {

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private RobustHTTPClient client;
    private File f;

    @Before
    public void createRobustHTTPClient() throws Exception {
        client = new RobustHTTPClient();
        f = new File(tempFolder.newFolder(), "jenkins-index.html");
    }

    @Test
    public void testDownloadFile() throws Exception {
        client.downloadFile(f, j.getURL(), TaskListener.NULL);
        assertThat(Files.readString(f.toPath()), containsString("<title>Dashboard [Jenkins]</title>"));
    }

    /**
     * Wait no more than TIMEOUT seconds for response
     */
    private static final int TIMEOUT = 2;

    @Test
    public void testDownloadFileWithTimeoutException() throws Exception {
        client.setStopAfterAttemptNumber(1);
        client.setTimeout(TIMEOUT, TimeUnit.SECONDS);
        URL hangingURL = new URL(j.getURL().toString() + "intentionally-hangs-always");
        final IOException e = assertThrows(IOException.class, () -> {
            client.downloadFile(f, hangingURL, TaskListener.NULL);
        });
        assertThat(e.getCause(), isA(TimeoutException.class));
    }

    @Test
    public void testDownloadFileWithRetry() throws Exception {
        // Try up to three times, should succeed on second try
        client.setStopAfterAttemptNumber(3);
        // Wait 213 milliseconds before retry (no need for a long wait)
        client.setWaitMultiplier(213, TimeUnit.MILLISECONDS);
        // Wait no more than 3x the TIMEOUT between retries (upper bound)
        client.setWaitMaximum(TIMEOUT * 3, TimeUnit.SECONDS);
        // Retry if no response in TIMEOUT seconds
        client.setTimeout(TIMEOUT, TimeUnit.SECONDS);
        URL hangsOnceURL = new URL(j.getURL().toString() + "intentionally-hangs-once");
        client.downloadFile(f, hangsOnceURL, TaskListener.NULL);
        assertThat(Files.readString(f.toPath()), containsString("a-dubious-response-from-hangs-once"));
    }

    @Test
    public void testDownloadFileFromNonExistentLocation() throws Exception {
        URL badURL = new URL(j.getURL().toString() + "/page/does/not/exist");
        final AbortException e = assertThrows(AbortException.class, () -> {
            client.downloadFile(f, badURL, TaskListener.NULL);
        });
        assertThat(e.getMessage(), containsString("Failed to download "));
    }

    @Test
    public void testDownloadFileStopAfterOneAttempt() throws Exception {
        client.setStopAfterAttemptNumber(1);
        URL badURL = new URL(j.getURL().toString() + "/page/does/not/exist");
        final AbortException e = assertThrows(AbortException.class, () -> {
            client.downloadFile(f, badURL, TaskListener.NULL);
        });
        assertThat(e.getMessage(), containsString("Failed to download "));
    }

    @TestExtension
    public static class IntentionallyHangsAlwaysAction implements UnprotectedRootAction {

        @Override
        public String getIconFileName() {
            return null;
        }

        @Override
        public String getDisplayName() {
            return null;
        }

        @Override
        public String getUrlName() {
            return "intentionally-hangs-always";
        }

        public HttpResponse doIndex() {
            try {
                // Sleep 3x longer than the timeout
                Thread.sleep(TIMEOUT * 3L * 1000L);
            } catch (InterruptedException ie) {
                // Ignore the exception
            }
            return HttpResponses.text("a-dubious-response-from-hangs-always");
        }
    }

    @TestExtension
    public static class IntentionallyHangsOnceAction implements UnprotectedRootAction {

        private int requestCount = 0;

        @Override
        public String getIconFileName() {
            return null;
        }

        @Override
        public String getDisplayName() {
            return null;
        }

        @Override
        public String getUrlName() {
            return "intentionally-hangs-once";
        }

        public HttpResponse doIndex() {
            requestCount++;
            if (requestCount <= 1) {
                /* Wait 3x longer than timeout on first request */
                try {
                    // Sleep 3x longer than the timeout
                    Thread.sleep(TIMEOUT * 3L * 1000L);
                } catch (InterruptedException ie) {
                    // Ignore the exception
                }
            }
            return HttpResponses.text("a-dubious-response-from-hangs-once");
        }
    }
}
