/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.wc;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.SVNAuthentication;

/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class DefaultSVNHostOptions implements ISVNHostOptions {

    private final SVNCompositeConfigFile myServersFile;
    private final SVNURL myURL;

    public DefaultSVNHostOptions(SVNCompositeConfigFile serversFile, SVNURL url) {
        myServersFile = serversFile;
        myURL = url;
    }

    private SVNCompositeConfigFile getServersFile() {
        return myServersFile;
    }

    public String getHost() {
        return myURL.getHost();
    }

    public String getProtocol() {
        return myURL.getProtocol();
    }

    public Collection getAuthTypes() {
        List schemes = new ArrayList();
        Map hostProperties = getHostProperties();
        String authTypes = (String) hostProperties.get("http-auth-types");
        if (authTypes == null || "".equals(authTypes.trim())) {
            return schemes;
        }

        for (StringTokenizer tokens = new StringTokenizer(authTypes, ";"); tokens.hasMoreTokens();) {
            String scheme = tokens.nextToken();
            if (!schemes.contains(scheme)) {
                schemes.add(scheme);
            }
        }
        return schemes;
    }

    private String getAuthStorageEnabledOption() {
        Map properties = getHostProperties();
        return (String) properties.get("store-auth-creds");
    }

    protected boolean hasAuthStorageEnabledOption() {
        return getAuthStorageEnabledOption() != null;
    }

    public boolean isAuthStorageEnabled() {
        String storeAuthCreds = getAuthStorageEnabledOption();
        if (storeAuthCreds == null) {
            return false;
        }
        return "yes".equalsIgnoreCase(storeAuthCreds) || "on".equalsIgnoreCase(storeAuthCreds) || "true".equalsIgnoreCase(storeAuthCreds);
    }

    public boolean isStorePasswords() {
        boolean store = true;
//        String value = getConfigFile().getPropertyValue("auth", "store-passwords");
//        if (value != null) {
//            store = "yes".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
//        }
        Map properties = getHostProperties();
        String storePasswords = (String) properties.get("store-passwords");
        if (storePasswords == null) {
            return store;
        }

        return "yes".equalsIgnoreCase(storePasswords) || "on".equalsIgnoreCase(storePasswords) || "true".equalsIgnoreCase(storePasswords);
    }

    private String getStorePlainTextPasswordOption() {
        Map properties = getHostProperties();
        return (String) properties.get("store-plaintext-passwords");
    }

    public boolean hasStorePlainTextPasswordsOption() {
        return getStorePlainTextPasswordOption() != null;
    }

    public boolean isStorePlainTextPasswords(String realm, SVNAuthentication auth) throws SVNException {
        String storePlainTextPasswords = getStorePlainTextPasswordOption();
        if (storePlainTextPasswords == null) {
            return false;
        }
        return "yes".equalsIgnoreCase(storePlainTextPasswords) || "on".equalsIgnoreCase(storePlainTextPasswords) ||
                "true".equalsIgnoreCase(storePlainTextPasswords);
    }

    public boolean isStoreSSLClientCertificatePassphrases() {
        Map properties = getHostProperties();
        String storeCertPassphrases = (String) properties.get("store-ssl-client-cert-pp");

        if (storeCertPassphrases == null) {
            return true;
        }

        return "yes".equalsIgnoreCase(storeCertPassphrases) || "on".equalsIgnoreCase(storeCertPassphrases) ||
                "true".equalsIgnoreCase(storeCertPassphrases);
    }

    private String getStorePlainTextPassphraseOption() {
        Map properties = getHostProperties();
        return (String) properties.get("store-ssl-client-cert-pp-plaintext");
    }

    protected boolean hasStorePlainTextPassphrasesOption() {
        String storePlainTextPassphrases = getStorePlainTextPassphraseOption();
        return storePlainTextPassphrases != null;
    }

    public boolean isStorePlainTextPassphrases(String realm, SVNAuthentication auth) throws SVNException {
        String storePlainTextPassphrases = getStorePlainTextPassphraseOption();
        if (storePlainTextPassphrases == null) {
            return false;
        }
        return "yes".equalsIgnoreCase(storePlainTextPassphrases) || "on".equalsIgnoreCase(storePlainTextPassphrases) ||
                "true".equalsIgnoreCase(storePlainTextPassphrases);
    }

    public String getUserName() {
//        if (url != null && url.getUserInfo() != null) {
//            return url.getUserInfo();
//        }
        Map properties = getHostProperties();
        String userName = (String) properties.get("username");
        return userName;
    }

    public String getSSLClientCertFile() {
        Map properties = getHostProperties();
        return (String) properties.get("ssl-client-cert-file");
    }

    public String getSSLClientCertPassword() {
        Map properties = getHostProperties();
        return (String) properties.get("ssl-client-cert-password");
    }

    public boolean trustDefaultSSLCertificateAuthority() {
        Map properties = getHostProperties();
        return !"no".equalsIgnoreCase((String) properties.get("ssl-trust-default-ca"));
    }

    public File[] getSSLAuthorityFiles() {
        Map properties = getHostProperties();
        String sslAuthorityFilePaths = (String) properties.get("ssl-authority-files"); // "pem" files
        Collection trustStorages = new ArrayList();
        if (sslAuthorityFilePaths != null) {
            for (StringTokenizer files = new StringTokenizer(sslAuthorityFilePaths, ";"); files.hasMoreTokens();) {
                String fileName = files.nextToken();
                fileName = fileName == null ? null : fileName.trim();
                if (fileName != null && !"".equals(fileName)) {
                    trustStorages.add(new File(fileName));
                }
            }
        }
        return (File[]) trustStorages.toArray(new File[trustStorages.size()]);
    }

    public String getProxyHost() {
        return getProxyOption("http-proxy-host", "http.proxyHost");
    }

    public String getProxyPort() {
        return getProxyOption("http-proxy-port", "http.proxyPort");
    }

    public String getProxyUserName() {
        return getProxyOption("http-proxy-username", null);
    }

    public String getProxyPassword() {
        return getProxyOption("http-proxy-password", null);
    }

    private String getProxyOption(String optionName, String systemProperty) {
        String host = getHost();
        Map properties = getHostProperties();
        String value = (String) properties.get(optionName);
        if ((value == null || "".equals(value.trim())) && systemProperty != null) {
            value = System.getProperty(systemProperty);
        }
        if (value == null || "".equals(value.trim())) {
            return null;
        }
        if (hostExceptedFromProxy(host, properties)) {
            return null;
        }
        return value;
    }

    private static boolean hostExceptedFromProxy(String host, Map properties) {
        String proxyExceptions = (String) properties.get("http-proxy-exceptions");
        String proxyExceptionsSeparator = ",";
        if (proxyExceptions == null) {
            proxyExceptions = System.getProperty("http.nonProxyHosts");
            proxyExceptionsSeparator = "|";
        }
        if (proxyExceptions != null) {
            for (StringTokenizer exceptions = new StringTokenizer(proxyExceptions, proxyExceptionsSeparator); exceptions.hasMoreTokens();) {
                String exception = exceptions.nextToken().trim();
                if (DefaultSVNOptions.matches(exception, host)) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getReadTimeout() {
        String protocol = getProtocol();
        if ("http".equals(protocol) || "https".equals(protocol)) {
            Map properties = getHostProperties();
            String timeout = (String) properties.get("http-timeout");
            if (timeout != null) {
                try {
                    return Integer.parseInt(timeout) * 1000;
                } catch (NumberFormatException nfe) {
                }
            }
            return 3600 * 1000;
        }
        return 0;
    }

    public int getConnectTimeout() {
        String protocol = getProtocol();
        if ("http".equals(protocol) || "https".equals(protocol)) {
            return 60 * 1000;
        }
        return 0;
    }

    private Map getHostProperties() {
        Map properties = getServersFile().getProperties("global");
        String groupName = getGroupName(getServersFile().getProperties("groups"));
        if (groupName != null) {
            Map hostProps = getServersFile().getProperties(groupName);
            properties.putAll(hostProps);
        }
        return properties;
    }

    private String getGroupName(Map groups) {
        for (Iterator names = groups.keySet().iterator(); names.hasNext();) {
            String name = (String) names.next();
            String pattern = (String) groups.get(name);
            for (StringTokenizer tokens = new StringTokenizer(pattern, ","); tokens.hasMoreTokens();) {
                String token = tokens.nextToken();
                token = token.trim();
                if (DefaultSVNOptions.matches(token, getHost())) {
                    return name;
                }
            }
        }
        return null;
    }
}
