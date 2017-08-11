/*
 * The MIT License
 *
 * Copyright (c) 2009-2017, Sun Microsystems, Inc., CloudBees, Inc. and other contributors
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
package org.jenkinsci.plugins.apachehttpclient4api;

import hudson.FilePath;
import hudson.model.Node;
import hudson.tools.JDKInstaller;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Test copy of {@link JDKInstaller.FileSystem} implementation in {@link JDKInstaller}.
 * @see Client4JDKInstaller
 */
public final class ClientFilePathFileSystem implements JDKInstaller.FileSystem {

    private final Node node;

    public ClientFilePathFileSystem(Node node) {
        this.node = node;
    }

    public void delete(String file) throws IOException, InterruptedException {
        $(file).delete();
    }

    public void chmod(String file, int mode) throws IOException, InterruptedException {
        $(file).chmod(mode);
    }

    public InputStream read(String file) throws IOException, InterruptedException {
        return $(file).read();
    }

    public List<String> listSubDirectories(String dir) throws IOException, InterruptedException {
        List<String> r = new ArrayList<String>();
        for (FilePath f : $(dir).listDirectories()) {
            r.add(f.getName());
        }
        return r;
    }

    public void pullUp(String from, String to) throws IOException, InterruptedException {
        $(from).moveAllChildrenTo($(to));
    }

    private FilePath $(String file) {
        return node.createPath(file);
    }
}
