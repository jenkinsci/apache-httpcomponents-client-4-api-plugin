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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.URL;
import org.junit.jupiter.api.Test;

class RobustHTTPClientTest {

    @Test
    void sanitize() throws Exception {
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
    void sanitizeReturnsInvalidURLs() throws Exception {
        assumeFalse(RobustHTTPClient.class.desiredAssertionStatus());
        /* URLs that cannot be converted to URIs are not sanitized.
         * They are returned "as-is" when assertions are disabled.
         */
        String invalidURI = "https://user:s3cr3t@example.com/ /has/space/in/url/";
        assertThat(RobustHTTPClient.sanitize(new URL(invalidURI)), is(invalidURI));
    }

    @Test
    void sanitizeExceptionOnInvalidURLs() {
        assumeTrue(RobustHTTPClient.class.desiredAssertionStatus());
        /* URLs that cannot be converted to URIs are not sanitized.
         * An assertion exception is thrown when assertions are enabled.
         */
        String invalidURI = "https://user:s3cr3t@example.com/ /has/space/in/url/";
        final AssertionError e =
                assertThrows(AssertionError.class, () -> RobustHTTPClient.sanitize(new URL(invalidURI)));
        assertThat(e.getCause().getMessage(), containsString("Illegal character in path at index 32: " + invalidURI));
    }
}
