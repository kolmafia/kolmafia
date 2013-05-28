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
public class DefaultSVNConnectionOptions implements ISVNConnectionOptions {

    private static final String[] DEFAULT_PASSWORD_STORE_TYPES = new String[]{
            DefaultSVNPersistentAuthenticationProvider.WINDOWS_CRYPTO_API_PASSWORD_STORAGE,
            DefaultSVNPersistentAuthenticationProvider.MAC_OS_KEYCHAIN_PASSWORD_STORAGE
    };

    private final SVNCompositeConfigFile myServersFile;
    private final SVNCompositeConfigFile myConfigFile;

    public DefaultSVNConnectionOptions(SVNCompositeConfigFile serversFile, SVNCompositeConfigFile configFile) {
        myServersFile = serversFile;
        myConfigFile = configFile;
    }

    private SVNCompositeConfigFile getServersFile() {
        return myServersFile;
    }

    private SVNCompositeConfigFile getConfigFile() {
        return myConfigFile;
    }

    public Collection getAuthTypes(SVNURL url) {
        List schemes = new ArrayList();

        String host = url.getHost();
        Map hostProperties = getHostProperties(host);
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

    private String getAuthStorageEnabledOption(SVNURL url) {
        String host = url != null ? url.getHost() : null;
        Map properties = getHostProperties(host);
        return (String) properties.get("store-auth-creds");
    }

    public boolean hasAuthStorageEnabledOption(SVNURL url) {
        return getAuthStorageEnabledOption(url) != null;
    }

    public boolean isAuthStorageEnabled(SVNURL url) {
        String storeAuthCreds = getAuthStorageEnabledOption(url);
        if (storeAuthCreds == null) {
            return false;
        }
        return "yes".equalsIgnoreCase(storeAuthCreds) || "on".equalsIgnoreCase(storeAuthCreds) || "true".equalsIgnoreCase(storeAuthCreds);
    }

    public String[] getPasswordStorageTypes() {
        String storeTypesOption = getConfigFile().getPropertyValue("auth", "password-stores");
        if (storeTypesOption == null) {
            return DEFAULT_PASSWORD_STORE_TYPES;
        }
        List storeTypes = new ArrayList();
        for (StringTokenizer types = new StringTokenizer(storeTypesOption, " ,"); types.hasMoreTokens();) {
            String type = types.nextToken();
            type = type == null ? null : type.trim();
            if (type != null && !"".equals(type)) {
                storeTypes.add(type);
            }
        }
        return (String[]) storeTypes.toArray(new String[storeTypes.size()]);
    }

    public boolean isStorePasswords(SVNURL url) {
        boolean store = true;
        String value = getConfigFile().getPropertyValue("auth", "store-passwords");
        if (value != null) {
            store = "yes".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
        }

        String host = url != null ? url.getHost() : null;
        Map properties = getHostProperties(host);
        String storePasswords = (String) properties.get("store-passwords");
        if (storePasswords == null) {
            return store;
        }

        return "yes".equalsIgnoreCase(storePasswords) || "on".equalsIgnoreCase(storePasswords) || "true".equalsIgnoreCase(storePasswords);
    }

    private String getStorePlainTextPasswordOption(SVNAuthentication auth) {
        SVNURL url = auth.getURL();
        String host = url != null ? url.getHost() : null;
        Map properties = getHostProperties(host);
        return (String) properties.get("store-plaintext-passwords");
    }

    public boolean isStorePlainTextPasswords(String realm, SVNAuthentication auth) throws SVNException {
        String storePlainTextPasswords = getStorePlainTextPasswordOption(auth);
        if (storePlainTextPasswords == null) {
            return false;
        }
        return "yes".equalsIgnoreCase(storePlainTextPasswords) || "on".equalsIgnoreCase(storePlainTextPasswords) ||
                "true".equalsIgnoreCase(storePlainTextPasswords);
    }

    public boolean isStoreSSLClientCertificatePassphrases(SVNURL url) {
        String host = url != null ? url.getHost() : null;
        Map properties = getHostProperties(host);
        String storeCertPassphrases = (String) properties.get("store-ssl-client-cert-pp");

        if (storeCertPassphrases == null) {
            return true;
        }

        return "yes".equalsIgnoreCase(storeCertPassphrases) || "on".equalsIgnoreCase(storeCertPassphrases) ||
                "true".equalsIgnoreCase(storeCertPassphrases);
    }

    private String getStorePlainTextPassphraseOption(SVNAuthentication auth) {
        SVNURL url = auth.getURL();
        String host = url != null ? url.getHost() : null;
        Map properties = getHostProperties(host);
        return (String) properties.get("store-ssl-client-cert-pp-plaintext");
    }

    public boolean isStorePlainTextPassphrases(String realm, SVNAuthentication auth) throws SVNException {
        String storePlainTextPassphrases = getStorePlainTextPassphraseOption(auth);
        if (storePlainTextPassphrases == null) {
            return false;
        }
        return "yes".equalsIgnoreCase(storePlainTextPassphrases) || "on".equalsIgnoreCase(storePlainTextPassphrases) ||
                "true".equalsIgnoreCase(storePlainTextPassphrases);
    }

    public String getUserName(SVNURL url) {
        String host = url != null ? url.getHost() : null;
//        if (url != null && url.getUserInfo() != null) {
//            return url.getUserInfo();
//        }
        Map properties = getHostProperties(host);
        String userName = (String) properties.get("username");
        return userName;
    }

    private String getDefaultSSHCommandLine() {
        Map tunnels = getConfigFile().getProperties("tunnels");
        if (tunnels == null || !tunnels.containsKey("ssh")) {
            return null;
        }
        return (String) tunnels.get("ssh");
    }

    private String getDefaultSSHOptionValue(String optionName, String systemProperty, String fallbackSystemProperty) {
        if (optionName != null) {
            String sshCommandLine = getDefaultSSHCommandLine();
            if (sshCommandLine != null) {
                String value = getOptionValue(sshCommandLine, optionName);
                if (value != null) {
                    return value;
                }
            }
        }
        return System.getProperty(systemProperty, System.getProperty(fallbackSystemProperty));
    }

    public int getDefaultSSHPortNumber() {
        String sshCommandLine = getDefaultSSHCommandLine();
        if (sshCommandLine == null) {
            return -1;
        }
        final String portOption = sshCommandLine.toLowerCase().trim().startsWith("plink") ? "-p" : "-P";
        String port = getDefaultSSHOptionValue(portOption, "svnkit.ssh2.port", "javasvn.ssh2.port");
        if (port != null) {
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException e) {
                //
            }
        }
        return -1;
    }

    public String getDefaultSSHUserName() {
        String userName = getDefaultSSHOptionValue("-l", "svnkit.ssh2.username", "javasvn.ssh2.username");
        if (userName == null) {
            userName = System.getProperty("user.name");
        }
        return userName;
    }

    public String getDefaultSSHPassword() {
        return getDefaultSSHOptionValue("-pw", "svnkit.ssh2.password", "javasvn.ssh2.password");
    }

    public String getDefaultSSHKeyFile() {
        return getDefaultSSHOptionValue("-i", "svnkit.ssh2.key", "javasvn.ssh2.key");
    }

    public String getDefaultSSHPassphrase() {
        return getDefaultSSHOptionValue(null, "svnkit.ssh2.passphrase", "javasvn.ssh2.passphrase");
    }

    public String getSSLClientCertFile(SVNURL url) {
        Map properties = getHostProperties(url.getHost());
        return (String) properties.get("ssl-client-cert-file");
    }

    public String getSSLClientCertPassword(SVNURL url) {
        Map properties = getHostProperties(url.getHost());
        return (String) properties.get("ssl-client-cert-password");
    }

    public boolean trustDefaultSSLCertificateAuthority(SVNURL url) {
        String host = url.getHost();
        Map properties = getHostProperties(host);
        return !"no".equalsIgnoreCase((String) properties.get("ssl-trust-default-ca"));
    }

    public File[] getSSLAuthorityFiles(SVNURL url) {
        String host = url.getHost();
        Map properties = getHostProperties(host);
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

    public String getProxyHost(SVNURL url) {
        return getProxyOption(url, "http-proxy-host", "http.proxyHost");
    }

    public String getProxyPort(SVNURL url) {
        return getProxyOption(url, "http-proxy-port", "http.proxyPort");
    }

    public String getProxyUserName(SVNURL url) {
        return getProxyOption(url, "http-proxy-username", null);
    }

    public String getProxyPassword(SVNURL url) {
        return getProxyOption(url, "http-proxy-password", null);
    }

    private String getProxyOption(SVNURL url, String optionName, String systemProperty) {
        if (url == null || optionName == null) {
            return null;
        }
        String host = url.getHost();
        Map properties = getHostProperties(host);
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

    public int getReadTimeout(SVNURL url) {
        String protocol = url.getProtocol();
        if ("http".equals(protocol) || "https".equals(protocol)) {
            String host = url.getHost();
            Map properties = getHostProperties(host);
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

    public int getConnectTimeout(SVNURL url) {
        String protocol = url.getProtocol();
        if ("http".equals(protocol) || "https".equals(protocol)) {
            return 60 * 1000;
        }
        return 0;
    }

    private Map getHostProperties(String host) {
        Map globalProps = getServersFile().getProperties("global");
        String groupName = getGroupName(getServersFile().getProperties("groups"), host);
        if (groupName != null) {
            Map hostProps = getServersFile().getProperties(groupName);
            globalProps.putAll(hostProps);
        }
        return globalProps;
    }

    private static String getGroupName(Map groups, String host) {
        for (Iterator names = groups.keySet().iterator(); names.hasNext();) {
            String name = (String) names.next();
            String pattern = (String) groups.get(name);
            for (StringTokenizer tokens = new StringTokenizer(pattern, ","); tokens.hasMoreTokens();) {
                String token = tokens.nextToken();
                if (DefaultSVNOptions.matches(token, host)) {
                    return name;
                }
            }
        }
        return null;
    }

    private static String getOptionValue(String commandLine, String optionName) {
        if (commandLine == null || optionName == null) {
            return null;
        }
        for (StringTokenizer options = new StringTokenizer(commandLine, " \r\n\t"); options.hasMoreTokens();) {
            String option = options.nextToken().trim();
            if (optionName.equals(option) && options.hasMoreTokens()) {
                return options.nextToken();
            } else if (option.startsWith(optionName)) {
                return option.substring(optionName.length());
            }
        }
        return null;
    }
}
