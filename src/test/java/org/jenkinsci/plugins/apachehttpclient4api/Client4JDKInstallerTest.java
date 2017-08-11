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

import static org.junit.Assert.assertEquals;
import hudson.tools.JDKInstaller.DescriptorImpl;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import hudson.model.JDK;
import hudson.util.StreamTaskListener;
import hudson.tools.JDKInstaller.Platform;
import hudson.tools.JDKInstaller.CPU;
import hudson.tools.InstallSourceProperty;
import hudson.tools.JDKInstaller;
import hudson.tools.ToolInstaller;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkinsci.plugins.apachehttpclient4api.Client4JDKInstaller.Client4DescriptorImpl;
import org.junit.Assert;
import org.junit.Assume;

import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.xml.sax.SAXException;

/**
 * Tests for {@link Client4JDKInstaller} using Apache HttpClient 4.x.
 * @author Kohsuke Kawaguchi
 * @author Oleg Nenashev
 */
public class Client4JDKInstallerTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();
    
    private boolean usingMockedCredentials;
    

    @Before
    public void setUp() throws Exception {
        File f = new File(new File(System.getProperty("user.home")),".jenkins-ci.org");
        String u = null;
        String p = null;
              
        if (!f.exists()) {
            LOGGER.log(Level.WARNING, "{0} doesn''t exist. Cannot load real credentials", f);
        } else {
            Properties prop = new Properties();
            try (InputStream in = Files.newInputStream(f.toPath())) {
                prop.load(in);
                u = prop.getProperty("oracle.userName");
                p = prop.getProperty("oracle.password");
                if (u==null || p==null) {
                    LOGGER.log(Level.WARNING, "{0} doesn''t contain oracle.userName and oracle.password. Falling back to mmock credentials", f);
                }
            }
        }
        
        if (u == null || p == null) {       
            LOGGER.log(Level.WARNING, "Adding mock credentials for the oracle website", f);
            u = "mockuser";
            p = "mockpassword";
            usingMockedCredentials = true;
        }
        
        
        DescriptorImpl d = j.jenkins.getDescriptorByType(DescriptorImpl.class);
        Client4DescriptorImpl d2 = j.jenkins.getDescriptorByType(Client4DescriptorImpl.class);
        d.doPostCredential(u,p);
        d2.doPostCredential(u, p);
    }

    @Test
    public void configRoundtrip() throws Exception {
        Client4JDKInstaller installer = new Client4JDKInstaller("jdk-6u13-oth-JPR@CDS-CDS_Developer", true);

        j.jenkins.getJDKs().add(new JDK("test",tmp.getRoot().getAbsolutePath(), Arrays.asList(
                new InstallSourceProperty(Arrays.<ToolInstaller>asList(installer)))));

        j.submit(j.createWebClient().goTo("configure").getFormByName("config"));

        JDK jdk = j.jenkins.getJDK("test");
        InstallSourceProperty isp = jdk.getProperties().get(InstallSourceProperty.class);
        assertEquals(1,isp.installers.size());
        j.assertEqualBeans(installer, isp.installers.get(Client4JDKInstaller.class), "id,acceptLicense");
    }

    @Test
    public void locateWithMockCredentials() throws Exception {
        Assume.assumeTrue("This test makes sense only with mock credentials", usingMockedCredentials);
        doTestLocate(false);
    }

    @Test
    public void locateStrict() throws Exception {
        Assume.assumeFalse("This test requires real credentialsto run correctly", usingMockedCredentials);
        doTestLocate(true);
    }
    
    private void doTestLocate(boolean downloadMustHappen) throws Exception {
        retrieveUpdateCenterData();

        Client4JDKInstaller i = new Client4JDKInstaller("jdk-7u3-oth-JPR", true);
        StreamTaskListener listener = StreamTaskListener.fromStdout();
        i.locate(listener, Platform.LINUX, CPU.i386, downloadMustHappen);
        i.locate(listener, Platform.WINDOWS, CPU.amd64, downloadMustHappen);
        i.locate(listener, Platform.SOLARIS, CPU.Sparc, downloadMustHappen);
    }
    
    private void retrieveUpdateCenterData() throws IOException, SAXException {
        j.createWebClient().goTo(""); // make sure data is loaded
        
        JDKInstaller.JDKList jdks = JDKInstaller.JDKList.all().get(JDKInstaller.JDKList.class);
        Assert.assertNotNull(jdks);
        FormValidation updateNow = jdks.updateNow();
        if (updateNow.kind != FormValidation.Kind.OK) {
            throw updateNow;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(Client4JDKInstallerTest.class.getName());
}
