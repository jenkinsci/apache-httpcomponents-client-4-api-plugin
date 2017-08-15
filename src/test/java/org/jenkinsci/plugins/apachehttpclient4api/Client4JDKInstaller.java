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

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.ProxyConfiguration;
import hudson.Util;
import hudson.model.JDK;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.JDKInstaller;
import hudson.tools.JDKInstaller.CPU;
import hudson.tools.JDKInstaller.JDKFamily;
import hudson.tools.JDKInstaller.JDKFamilyList;
import hudson.tools.JDKInstaller.JDKFile;
import hudson.tools.JDKInstaller.JDKList;
import hudson.tools.JDKInstaller.JDKRelease;
import hudson.tools.JDKInstaller.Platform;
import hudson.util.Secret;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;

import java.net.URL;

import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static hudson.tools.JDKInstaller.Preference.*;
import hudson.tools.Messages;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolInstallerDescriptor;
import hudson.util.FormValidation;
import hudson.util.HttpResponses;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.CheckForNull;
import javax.servlet.ServletException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHttpRequest;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Install JDKs from java.sun.com using Apache HttpClient 4.x.
 *
 * @author Kohsuke Kawaguchi
 * @author Oleg Nenashev
 */
// TODO: Cannot extend JDKInstaller, because its descriptor is final and getDescriptor() is overridden
public class Client4JDKInstaller extends ToolInstaller {

    /**
     * The release ID that Sun assigns to each JDK, such as "jdk-6u13-oth-JPR@CDS-CDS_Developer"
     *
     * <p>
     * This ID can be seen in the "ProductRef" query parameter of the download page, like
     * https://cds.sun.com/is-bin/INTERSHOP.enfinity/WFS/CDS-CDS_Developer-Site/en_US/-/USD/ViewProductDetail-Start?ProductRef=jdk-6u13-oth-JPR@CDS-CDS_Developer
     */
    public final String id;

    /**
     * We require that the user accepts the license by clicking a checkbox, to make up for the part
     * that we auto-accept cds.sun.com license click through.
     */
    public final boolean acceptLicense;
    
    private final JDKInstaller installer;

    @DataBoundConstructor
    public Client4JDKInstaller(String id, boolean acceptLicense) {
        super(null);
        this.id = id;
        this.acceptLicense = acceptLicense;
        this.installer = new JDKInstaller(id, acceptLicense);
    }

    private boolean isJava15() {
        return id.contains("-1.5");
    }

    private boolean isJava14() {
        return id.contains("-1.4");
    }

    /**
     * This is where we locally cache this JDK.
     */
    private File getLocalCacheFile(Platform platform, CPU cpu) {
        return new File(Jenkins.getInstance().getRootDir(),"cache/jdks/"+platform+"/"+cpu+"/"+id);
    }
    
    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        FilePath expectedLocation = preferredLocation(tool, node);
        PrintStream out = log.getLogger();
        try {
            if(!acceptLicense) {
                out.println(Messages.JDKInstaller_UnableToInstallUntilLicenseAccepted());
                return expectedLocation;
            }
            // already installed?
            FilePath marker = expectedLocation.child(".installedByHudson");
            if (marker.exists() && marker.readToString().equals(id)) {
                return expectedLocation;
            }
            expectedLocation.deleteRecursive();
            expectedLocation.mkdirs();

            JDKInstaller.Platform p = JDKInstaller.Platform.of(node);
            URL url = locate(log, p, JDKInstaller.CPU.of(node), true);

            FilePath file = expectedLocation.child(p.bundleFileName);
            file.copyFrom(url);

            // JDK6u13 on Windows doesn't like path representation like "/tmp/foo", so make it a strict platform native format by doing 'absolutize'
            installer.install(node.createLauncher(log), p, new ClientFilePathFileSystem(node), log, expectedLocation.absolutize().getRemote(), file.getRemote());

            // successfully installed
            file.delete();
            marker.write(id, null);

        } catch (Exception e) {
            out.println("JDK installation skipped: "+e.getMessage());
            if (e instanceof IOException) {
                throw (IOException)e;
            }
            if (e instanceof InterruptedException) {
                throw (InterruptedException)e;
            }
        }

        return expectedLocation;
    }

    /**
     * Performs a license click through and obtains the one-time URL for downloading bits.
     * @return URL of the downloaded file or {@code null} if the download didn't happen
     */
    @CheckForNull
    public URL locate(TaskListener log, Platform platform, CPU cpu, boolean downloadMustHappen) throws IOException {
        File cache = getLocalCacheFile(platform, cpu);
        if (cache.exists() && cache.length()>1*1024*1024) return cache.toURL(); // if the file is too small, don't trust it. In the past, the download site served error message in 200 status code

        log.getLogger().println("Installing JDK "+id);
        JDKFamilyList families = JDKList.all().get(JDKList.class).toList();
        if (families.isEmpty())
            throw new IOException("JDK data is empty.");

        JDKRelease release = families.getRelease(id);
        if (release==null)
            throw new IOException("Unable to find JDK with ID="+id);

        JDKFile primary=null,secondary=null;
        for (JDKFile f : release.files) {
            String vcap = f.name.toUpperCase(Locale.ENGLISH);

            // JDK files have either 'windows', 'linux', or 'solaris' in its name, so that allows us to throw
            // away unapplicable stuff right away
            if(!platform.is(vcap))
                continue;

            switch (cpu.accept(vcap)) {
            case PRIMARY:   primary = f;break;
            case SECONDARY: secondary=f;break;
            case UNACCEPTABLE:  break;
            }
        }

        if(primary==null)   primary=secondary;
        if(primary==null)
            throw new AbortException("Couldn't find the right download for "+platform+" and "+ cpu +" combination");
        LOGGER.log(Level.FINE, "Platform choice:{0}", primary);

        log.getLogger().println("Downloading JDK from "+primary.filepath);

        HttpClientBuilder hcb = HttpClientBuilder.create();
        
        //TODO: Offer public utility method for that?
        ProxyConfiguration jpc = Jenkins.getActiveInstance().proxy;
        if(jpc != null) {
            HttpHost proxyHost = new HttpHost(jpc.name, jpc.port);
            hcb.setProxy(proxyHost);
            
            if(jpc.getUserName() != null) {
                BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
            
                UsernamePasswordCredentials creds = new UsernamePasswordCredentials(jpc.getUserName(), jpc.getPassword());
                credsProvider.setCredentials(new AuthScope(proxyHost), creds);      
                hcb.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());

            }
        }
        
        { // Cookies
            CookieStore cookies = new BasicCookieStore();
        
            BasicClientCookie gpw_e24 = new BasicClientCookie("gpw_e24", ".");
            gpw_e24.setSecure(false);
            gpw_e24.setDomain(".oracle.com");
            gpw_e24.setPath("/");
            cookies.addCookie(gpw_e24);
        
            BasicClientCookie oraclelicense = new BasicClientCookie("oraclelicense", "accept-securebackup-cookie");
            oraclelicense.setSecure(false);
            oraclelicense.setDomain("oraclelicense");
            oraclelicense.setPath("/");
            cookies.addCookie(oraclelicense);
        
            hcb.setDefaultCookieStore(cookies);
        }
        
        { // User Agent
            hcb.setUserAgent("Mozilla/5.0 (Windows; U; MSIE 9.0; Windows NT 9.0; en-US");
        }
        
        int totalPageCount=0;  // counters for avoiding infinite loop
        final HttpHost target;
        try {
            URI targetUri = new URI(primary.filepath);
            target = new HttpHost(targetUri.getHost());
        } catch(URISyntaxException ex) {
            throw new IOException(ex);
        }
        HttpRequest m = new HttpGet(primary.filepath);
        try(CloseableHttpClient hc = hcb.build()) {
            while (true) {
                if (totalPageCount++>16) {
                    if (downloadMustHappen) {
                        throw new IOException("Unable to find the login form");
                    } else {
                        LOGGER.warning("Reached the login attempts limit, aborting");
                        return null;
                    }
                }

                LOGGER.log(Level.FINE, "Requesting {0}", m);
                    try (CloseableHttpResponse response = hc.execute(target, m)) {
                    StatusLine statusLine = response.getStatusLine();
                    if (statusLine.getStatusCode()/100==3) {
                        // redirect?
                        String loc = response.getFirstHeader("Location").getValue();
                        m = new BasicHttpRequest("GET", loc);
                        continue;
                    }
                    if (statusLine.getStatusCode()!=200) {
                        throw new IOException("Failed to request " + m.getRequestLine().getUri() +" status=" + statusLine);
                    }
                
                
                    final URI mURI; 
                    try {
                        mURI = new URI(m.getRequestLine().getUri());
                    } catch(URISyntaxException ex) {
                        throw new IOException("Request line URI is corrupted", ex);
                    }

                    if (mURI.getHost().equals("login.oracle.com")) {
                        LOGGER.fine("Appears to be a login page");

                        BasicResponseHandler loginPageHandler= new BasicResponseHandler() {
                            @Override
                            public String handleResponse(org.apache.http.HttpResponse response) throws HttpResponseException, IOException {
                                return IOUtils.toString(
                                        response.getEntity().getContent(), 
                                        response.getEntity().getContentEncoding().getValue()); 
                            }  
                        };
                        String resp = loginPageHandler.handleResponse(response);
                        
                        Matcher pm = Pattern.compile("<form .*?action=\"([^\"]*)\" .*?</form>", Pattern.DOTALL).matcher(resp);
                        if (!pm.find())
                            throw new IllegalStateException("Unable to find a form in the response:\n"+resp);

                        String form = pm.group();
                        
                        String u = getDescriptor().getUsername();
                        Secret p = getDescriptor().getPassword();
                        if (u==null || p==null) {
                            log.hyperlink(getCredentialPageUrl(),"Oracle now requires Oracle account to download previous versions of JDK. Please specify your Oracle account username/password.\n");
                            throw new AbortException("Unable to install JDK unless a valid Oracle account username/password is provided in the system configuration.");
                        }
                        
                        
                        URIBuilder bldr = new URIBuilder(new URL(mURI.toURL(), pm.group(1)).toURI());
                        for (String fragment : form.split("<input")) {
                            String n = extractAttribute(fragment,"name");
                            String v = extractAttribute(fragment,"value");
                            if (n==null || v==null)     continue;
                            if (n.equals("ssousername"))
                                v = u;
                            if (n.equals("password")) {
                                v = p.getPlainText();
                                // Do not try to login with mock credentials
                                /*if (authCount++ > 3) {
                                    log.hyperlink(getCredentialPageUrl(),"Your Oracle account doesn't appear valid. Please specify a valid username/password\n");
                                    throw new AbortException("Unable to install JDK unless a valid username/password is provided.");
                                }*/
                            }
                            bldr.addParameter(n, v);
                        }
                        HttpPost post = new HttpPost(bldr.build());

                        m = post;
                    } else {
                        log.getLogger().println("Downloading " + response.getEntity().getContentLength() + " bytes");

                        // download to a temporary file and rename it in to handle concurrency and failure correctly,
                        File tmp = new File(cache.getPath()+".tmp");
                        try {
                            tmp.getParentFile().mkdirs();
                            try (OutputStream out = Files.newOutputStream(tmp.toPath())) {
                                IOUtils.copy(response.getEntity().getContent(), out);
                            } catch (InvalidPathException e) {
                                throw new IOException(e);
                            }

                            tmp.renameTo(cache);
                            return cache.toURL();
                        } finally {
                            tmp.delete();
                        }
                    }
                }
            }
        } catch(URISyntaxException ex) {
            throw new IOException(ex);
        } 
    }

    private static String extractAttribute(String s, String name) {
        String h = name + "=\"";
        int si = s.indexOf(h);
        if (si<0)   return null;
        int ei = s.indexOf('\"',si+h.length());
        return s.substring(si+h.length(),ei);
    }

    private String getCredentialPageUrl() {
        return "/"+getDescriptor().getDescriptorUrl()+"/enterCredential";
    }

    @Override
    public Client4DescriptorImpl getDescriptor() {
        return (Client4DescriptorImpl)super.getDescriptor();
    }
    
    private static final Logger LOGGER = Logger.getLogger(Client4JDKInstaller.class.getName());
    
    @Extension
    public static final class Client4DescriptorImpl extends ToolInstallerDescriptor<JDKInstaller> {
        private String username;
        private Secret password;

        public Client4DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return Messages.JDKInstaller_DescriptorImpl_displayName();
        }

        @Override
        public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
            return toolType==JDK.class;
        }

        public String getUsername() {
            return username;
        }

        public Secret getPassword() {
            return password;
        }

        public FormValidation doCheckId(@QueryParameter String value) {
            if (Util.fixEmpty(value) == null)
                return FormValidation.error(Messages.JDKInstaller_DescriptorImpl_doCheckId()); // improve message
            return FormValidation.ok();
        }

        /**
         * List of installable JDKs.
         * @return never null.
         */
        public List<JDKFamily> getInstallableJDKs() throws IOException {
            return Arrays.asList(JDKList.all().get(JDKList.class).toList().data);
        }

        public FormValidation doCheckAcceptLicense(@QueryParameter boolean value) {
            if (username==null || password==null)
                return FormValidation.errorWithMarkup(Messages.JDKInstaller_RequireOracleAccount(Stapler.getCurrentRequest().getContextPath()+'/'+getDescriptorUrl()+"/enterCredential"));
            if (value) {
                return FormValidation.ok();
            } else {
                return FormValidation.error(Messages.JDKInstaller_DescriptorImpl_doCheckAcceptLicense());
            }
        }

        /**
         * Submits the Oracle account username/password.
         */
        @RequirePOST
        public HttpResponse doPostCredential(@QueryParameter String username, @QueryParameter String password) throws IOException, ServletException {
            Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
            this.username = username;
            this.password = Secret.fromString(password);
            save();
            return HttpResponses.redirectTo("credentialOK");
        }
    }
}
