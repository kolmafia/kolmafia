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
import java.util.Collection;
import java.util.Map;

import javax.net.ssl.TrustManager;

import org.tmatesoft.svn.core.SVNAuthenticationException;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationProvider;
import org.tmatesoft.svn.core.auth.ISVNProxyManager;
import org.tmatesoft.svn.core.auth.ISVNSSHHostVerifier;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
import org.tmatesoft.svn.core.auth.SVNSSHAuthentication;
import org.tmatesoft.svn.core.auth.SVNUserNameAuthentication;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class DefaultSVNAuthenticationManager implements ISVNAuthenticationManager, ISVNSSLPasspharsePromptSupport, ISVNSSHHostVerifier {

    private boolean myIsStoreAuth;
    private File myConfigDirectory;
    private ISVNAuthenticationStorageOptions myAuthOptions;
    private DefaultSVNOptions myDefaultOptions;
    private ISVNHostOptionsProvider myHostOptionsProvider;

    private ISVNAuthenticationStorage myRuntimeAuthStorage;
    private ISVNAuthenticationProvider[] myProviders;

    private SVNAuthentication myPreviousAuthentication;
    private SVNErrorMessage myPreviousErrorMessage;
    private int myLastProviderIndex;
    private SVNAuthentication myLastLoadedAuth;
    private boolean myIsAuthenticationForced;

    public DefaultSVNAuthenticationManager(File configDirectory, boolean storeAuth, String userName, String password) {
        this(configDirectory, storeAuth, userName, password, null, null);
    }

    public DefaultSVNAuthenticationManager(File configDirectory, boolean storeAuth, String userName, String password, File privateKey, String passphrase) {
        //password = password == null ? "" : password;

        myIsStoreAuth = storeAuth;
        myConfigDirectory = configDirectory;
        if (myConfigDirectory == null) {
            myConfigDirectory = SVNWCUtil.getDefaultConfigurationDirectory();
        }

        myProviders = new ISVNAuthenticationProvider[4];
        myProviders[0] = createDefaultAuthenticationProvider(userName, password, privateKey, passphrase, myIsStoreAuth);
        myProviders[1] = createRuntimeAuthenticationProvider();
        myProviders[2] = createCacheAuthenticationProvider(new File(myConfigDirectory, "auth"), userName);
    }

    public void setInMemoryServersOptions(Map serversOptions) {
        if (getHostOptionsProvider() instanceof DefaultSVNHostOptionsProvider) {
            DefaultSVNHostOptionsProvider defaultHostOptionsProvider = (DefaultSVNHostOptionsProvider) getHostOptionsProvider();
            defaultHostOptionsProvider.setInMemoryServersOptions(serversOptions);
        }
    }

    public void setInMemoryConfigOptions(Map configOptions) {
        getDefaultOptions().setInMemoryConfigOptions(configOptions);
    }

    public ISVNAuthenticationStorageOptions getAuthenticationStorageOptions() {
        if (myAuthOptions == null) {
            return ISVNAuthenticationStorageOptions.DEFAULT;
        }
        return myAuthOptions;
    }

    public void setAuthenticationStorageOptions(ISVNAuthenticationStorageOptions authOptions) {
        myAuthOptions = authOptions;
    }

    public void setAuthenticationProvider(ISVNAuthenticationProvider provider) {
        // add provider to list
        myProviders[3] = provider; 
    }

    protected File getConfigDirectory() {
	    return myConfigDirectory;
    }

    public DefaultSVNOptions getDefaultOptions() {
        if (myDefaultOptions == null) {
            myDefaultOptions = new DefaultSVNOptions(myConfigDirectory, true);
        }
        return myDefaultOptions;
    }

    public ISVNHostOptionsProvider getHostOptionsProvider() {
        if (myHostOptionsProvider == null) {
            myHostOptionsProvider = new ExtendedHostOptionsProvider();
        }
        return myHostOptionsProvider;
    }

    protected void setHostOptionsProvider(ISVNHostOptionsProvider hostOptionsProvider) {
        myHostOptionsProvider = hostOptionsProvider;
    }

    public Collection<String> getAuthTypes(SVNURL url) {
        return getHostOptionsProvider().getHostOptions(url).getAuthTypes();
    }
    
    public ISVNProxyManager getProxyManager(SVNURL url) throws SVNException {
        final ISVNHostOptions hostOptions = getHostOptionsProvider().getHostOptions(url);
        String proxyHost = hostOptions.getProxyHost();
        if (proxyHost == null) {
            return null;
        }
        String proxyPort = hostOptions.getProxyPort();
        String proxyUser = hostOptions.getProxyUserName();
        String proxyPassword = hostOptions.getProxyPassword();
        return new SimpleProxyManager(proxyHost, proxyPort, proxyUser, proxyPassword);
    }

	public TrustManager getTrustManager(SVNURL url) throws SVNException {
        final ISVNHostOptions hostOptions = getHostOptionsProvider().getHostOptions(url);
        boolean trustAll = hostOptions.trustDefaultSSLCertificateAuthority();
		File[] serverCertFiles = hostOptions.getSSLAuthorityFiles();
		File authDir = new File(myConfigDirectory, "auth/svn.ssl.server");
		return new DefaultSVNSSLTrustManager(authDir, url, serverCertFiles, trustAll, this);
	}

    public SVNAuthentication getFirstAuthentication(String kind, String realm, SVNURL url) throws SVNException {
        myPreviousAuthentication = null;
        myPreviousErrorMessage = null;
        myLastProviderIndex = 0;
        myLastLoadedAuth = null;
        // iterate over providers and ask for auth till it is found.
        for (int i = 0; i < myProviders.length; i++) {
            if (myProviders[i] == null) {
                continue;
            }
            SVNAuthentication auth = myProviders[i].requestClientAuthentication(kind, url, realm, null, myPreviousAuthentication, myIsStoreAuth);
            if (auth != null) {
                if (i == 2) {
                    myLastLoadedAuth = auth;
                }

                myPreviousAuthentication = auth;
                myLastProviderIndex = i;

                if (auth.isPartial()) {
                    continue;
                }
                return auth;
            }
            if (i == 3) {
                SVNErrorManager.cancel("authentication cancelled", SVNLogType.WC);
            }
        }
        // end of probe. if we were asked for username for ssh and didn't find anything 
        // report something default.
        if (ISVNAuthenticationManager.USERNAME.equals(kind)) {
            // user auth shouldn't be null.
            return new SVNUserNameAuthentication("", getHostOptionsProvider().getHostOptions(url).isAuthStorageEnabled(), url, false);
        }
        SVNErrorManager.authenticationFailed("Authentication required for ''{0}''", realm);
        return null;
    }

    public SVNAuthentication getNextAuthentication(String kind, String realm, SVNURL url) throws SVNException {
        int index = Math.min(myLastProviderIndex + 1, 3);
        for(int i = index; i < myProviders.length; i++) {
            if (myProviders[i] == null) {
                continue;
            }
            if ((i == 1 || i == 2) && hasExplicitCredentials(kind)) {
                continue;
            }
            SVNAuthentication auth = myProviders[i].requestClientAuthentication(kind, url, realm, myPreviousErrorMessage, myPreviousAuthentication, myIsStoreAuth);
            if (auth != null) {
                if (i == 2) {
                    myLastLoadedAuth = auth;
                }
                
                myPreviousAuthentication = auth;
                myLastProviderIndex = i;

                if (auth.isPartial()) {
                    continue;
                }

                return auth;
            }
            if (i == 3) {
                SVNErrorManager.cancel("authentication cancelled", SVNLogType.WC);
            }
        }
        SVNErrorManager.authenticationFailed("Authentication required for ''{0}''", realm);
        return null;
    }

    public void acknowledgeAuthentication(boolean accepted, String kind, String realm, SVNErrorMessage errorMessage, SVNAuthentication authentication) throws SVNException {
        if (!accepted) {
            myPreviousErrorMessage = errorMessage;
            myPreviousAuthentication = authentication;
            myLastLoadedAuth = null;
            return;
        }
        if (myIsStoreAuth && authentication.isStorageAllowed() && myProviders[2] instanceof ISVNPersistentAuthenticationProvider) {
            // compare this authentication with last loaded from provider[2].
            if (myLastLoadedAuth == null || myLastLoadedAuth != authentication) {
                ((ISVNPersistentAuthenticationProvider) myProviders[2]).saveAuthentication(authentication, kind, realm);
            }
        }
        myLastLoadedAuth = null;
        if (!hasExplicitCredentials(kind)) {
            // do not cache explicit credentials in runtime cache?
            ((CacheAuthenticationProvider) myProviders[1]).saveAuthentication(authentication, realm);
        }
    }

	public void acknowledgeTrustManager(TrustManager manager) {
	}

    private boolean hasExplicitCredentials(String kind) {
        if (ISVNAuthenticationManager.PASSWORD.equals(kind) || ISVNAuthenticationManager.USERNAME.equals(kind) || ISVNAuthenticationManager.SSH.equals(kind)) {
            if (myProviders[0] instanceof DumbAuthenticationProvider) {
                DumbAuthenticationProvider authProvider = (DumbAuthenticationProvider) myProviders[0];
                // for user name has to be user
                String userName = authProvider.myUserName;
                String password = authProvider.myPassword;
                if (ISVNAuthenticationManager.USERNAME.equals(kind)) {
                    return userName != null && !"".equals(userName);
                }
                // do not look into cache when both password and user name specified
                // if only username is specified, then do look, but only for that username
                return password != null && !"".equals(password) && userName != null && !"".equals(userName);
            }
        }
        return false;
    }

    /**
     * Sets a specific runtime authentication storage manager. This storage
     * manager will be asked by this auth manager for cached credentials as
     * well as used to cache new ones accepted recently.
     *
     * @param storage a custom auth storage manager
     */
    public void setRuntimeStorage(ISVNAuthenticationStorage storage) {
        myRuntimeAuthStorage = storage;
    }
    
    protected ISVNAuthenticationStorage getRuntimeAuthStorage() {
        if (myRuntimeAuthStorage == null) {
            myRuntimeAuthStorage = new ISVNAuthenticationStorage() {
                private Map myData = new SVNHashMap(); 

                public void putData(String kind, String realm, Object data) {
                    myData.put(kind + "$" + realm, data);
                }
                public Object getData(String kind, String realm) {
                    return myData.get(kind + "$" + realm);
                }
            };
        }
        return myRuntimeAuthStorage;
    }
    
    protected ISVNAuthenticationProvider getAuthenticationProvider() {
        return myProviders[3];
    }
    
    protected SVNSSHAuthentication getDefaultSSHAuthentication(SVNURL url) {
        String userName = getDefaultOptions().getDefaultSSHUserName();
        String password = getDefaultOptions().getDefaultSSHPassword();
        String keyFile = getDefaultOptions().getDefaultSSHKeyFile();
        int port = getDefaultOptions().getDefaultSSHPortNumber();
        String passphrase = getDefaultOptions().getDefaultSSHPassphrase();

        if (userName != null && password != null) {
            return new SVNSSHAuthentication(userName, password, port, getHostOptionsProvider().getHostOptions(url).isAuthStorageEnabled(), url, false);
        } else if (userName != null && keyFile != null) {
            return new SVNSSHAuthentication(userName, new File(keyFile), passphrase, port, getHostOptionsProvider().getHostOptions(url).isAuthStorageEnabled(), url, false);
        }
        return null;
    }
    
    protected ISVNAuthenticationProvider createDefaultAuthenticationProvider(String userName, String password, File privateKey, String passphrase, boolean allowSave) {
        return new DumbAuthenticationProvider(userName, password, privateKey, passphrase, allowSave);
    }

    protected ISVNAuthenticationProvider createRuntimeAuthenticationProvider() {
        return new CacheAuthenticationProvider();
    }

    protected ISVNAuthenticationProvider createCacheAuthenticationProvider(File authDir, String userName) {
        ISVNAuthenticationStorageOptions delegatingOptions = createAuthenticationStorageOptions();
        return new DefaultSVNPersistentAuthenticationProvider(authDir, userName, delegatingOptions, getDefaultOptions(), getHostOptionsProvider());
    }

    public ISVNAuthenticationStorageOptions createAuthenticationStorageOptions() {
        return new ISVNAuthenticationStorageOptions() {
            public boolean isNonInteractive() throws SVNException {
                return getAuthenticationStorageOptions().isNonInteractive();
            }

            public ISVNAuthStoreHandler getAuthStoreHandler() throws SVNException {
                return getAuthenticationStorageOptions().getAuthStoreHandler();
            }

            public boolean isSSLPassphrasePromptSupported() {
                if (getAuthenticationStorageOptions() == ISVNAuthenticationStorageOptions.DEFAULT) {
                    return DefaultSVNAuthenticationManager.this.isSSLPassphrasePromtSupported();
                }
                return getAuthenticationStorageOptions().isSSLPassphrasePromptSupported();
            }

            public ISVNGnomeKeyringPasswordProvider getGnomeKeyringPasswordProvider() {
                return getAuthenticationStorageOptions().getGnomeKeyringPasswordProvider();
            }
        };
    }

    protected class DumbAuthenticationProvider implements ISVNAuthenticationProvider {
        
        private String myUserName;
        private String myPassword;
        private boolean myIsStore;
        private String myPassphrase;
        private File myPrivateKey;

        public DumbAuthenticationProvider(String userName, String password, File privateKey, String passphrase, boolean store) {
            myUserName = userName;
            myPassword = password;
            myPrivateKey = privateKey;
            myPassphrase = passphrase;
            myIsStore = store;
        }

        public SVNAuthentication requestClientAuthentication(String kind, SVNURL url, String realm, SVNErrorMessage errorMessage,
                SVNAuthentication previousAuth, boolean authMayBeStored) {
            if (previousAuth == null) {
                if (ISVNAuthenticationManager.SSH.equals(kind)) {
                    SVNSSHAuthentication sshAuth = getDefaultSSHAuthentication(url);
                    if (myUserName == null || "".equals(myUserName.trim())) {
                        return sshAuth;
                    }
                    if (myPrivateKey != null) {
                        return new SVNSSHAuthentication(myUserName, myPrivateKey, myPassphrase, sshAuth != null ? sshAuth.getPortNumber() : -1, 
                                myIsStore, url, false);
                    }
                    return new SVNSSHAuthentication(myUserName, myPassword, sshAuth != null ? sshAuth.getPortNumber() : -1, myIsStore, url, false);
                } else if (ISVNAuthenticationManager.PASSWORD.equals(kind)) {
                    if (myUserName == null || "".equals(myUserName.trim())) {
                        String defaultUserName = getHostOptionsProvider().getHostOptions(url).getUserName();
                        defaultUserName = defaultUserName == null ? System.getProperty("user.name") : defaultUserName; 
                        if (defaultUserName != null) {
                            //return new SVNUserNameAuthentication(defaultUserName, false);
                            SVNPasswordAuthentication partialAuth = new SVNPasswordAuthentication(defaultUserName, null, false, url, true);
                            return partialAuth;
                        } 
                        return null;
                    }
                    
                    if (myPassword == null) {
                        return new SVNPasswordAuthentication(myUserName, null, false, url, true);
                    }
                    return new SVNPasswordAuthentication(myUserName, myPassword, myIsStore, url, false);
                } else if (ISVNAuthenticationManager.USERNAME.equals(kind)) {
                    if (myUserName == null || "".equals(myUserName)) {
                        String userName = System.getProperty("svnkit.ssh2.author", System.getProperty("javasvn.ssh2.author"));
                        if (userName != null) {
                            return new SVNUserNameAuthentication(userName, myIsStore, url, false);
                        }
                        return null;
                    }
                    return new SVNUserNameAuthentication(myUserName, myIsStore, url, false);
                }
            }
            return null;
        }
        public int acceptServerAuthentication(SVNURL url, String r, Object serverAuth, boolean resultMayBeStored) {
            return ACCEPTED;
        }
    }

    private class CacheAuthenticationProvider implements ISVNAuthenticationProvider {        

        public SVNAuthentication requestClientAuthentication(String kind, SVNURL url, String realm, SVNErrorMessage errorMessage, SVNAuthentication previousAuth, boolean authMayBeStored) {
            String actualRealm = realm;
            if (url != null && url.getUserInfo() != null) {
                actualRealm = url.getUserInfo() + "$" + actualRealm;
            }
            return (SVNAuthentication) getRuntimeAuthStorage().getData(kind, actualRealm);
        }
        
        public void saveAuthentication(SVNAuthentication auth, String realm) {
            if (auth == null || realm == null) {
                return;
            }
            final String kind = auth.getKind();
            String actualRealm = realm;
            if (auth.getURL() != null && auth.getURL().getUserInfo() != null) {
                actualRealm = auth.getURL().getUserInfo() + "$" + actualRealm;
            }
            getRuntimeAuthStorage().putData(kind, actualRealm, auth);
        }
        
        public int acceptServerAuthentication(SVNURL url, String r, Object serverAuth, boolean resultMayBeStored) {
            return ACCEPTED;
        }
    }

    public class ExtendedHostOptionsProvider extends DefaultSVNHostOptionsProvider {

        public ExtendedHostOptionsProvider() {
            super(myConfigDirectory);
        }

        @Override
        public ISVNHostOptions getHostOptions(SVNURL url) {
            return new ExtendedHostOptions(getServersFile(), url);
        }
    }

    public class ExtendedHostOptions extends DefaultSVNHostOptions {

        public ExtendedHostOptions(SVNCompositeConfigFile serversFile, SVNURL url) {
            super(serversFile, url);
        }

        @Override
        public boolean isAuthStorageEnabled() {
            if (!super.hasAuthStorageEnabledOption()) {
                return myIsStoreAuth;
            }
            return super.isAuthStorageEnabled();
        }

        public boolean isStorePlainTextPasswords(String realm, SVNAuthentication auth) throws SVNException {
            if (!super.hasStorePlainTextPasswordsOption()) {
                ISVNAuthStoreHandler handler = getAuthenticationStorageOptions().getAuthStoreHandler();
                if (handler != null) {
                    return handler.canStorePlainTextPasswords(realm, auth);
                }
                return false;
            }
            return super.isStorePlainTextPasswords(realm, auth);
        }

        public boolean isStorePlainTextPassphrases(String realm, SVNAuthentication auth) throws SVNException {
            if (!super.hasStorePlainTextPassphrasesOption()) {
                ISVNAuthStoreHandler handler = getAuthenticationStorageOptions().getAuthStoreHandler();
                if (handler != null) {
                    return handler.canStorePlainTextPassphrases(realm, auth);
                }
                return false;
            }
            return super.isStorePlainTextPassphrases(realm, auth);
        }
    }
    
    private static final class SimpleProxyManager implements ISVNProxyManager {

        private String myProxyHost;
        private String myProxyPort;
        private String myProxyUser;
        private String myProxyPassword;

        public SimpleProxyManager(String host, String port, String user, String password) {
            myProxyHost = host;
            myProxyPort = port == null ? "3128" : port;
            myProxyUser = user;
            myProxyPassword = password;
        }
        
        public String getProxyHost() {
            return myProxyHost;
        }

        public int getProxyPort() {
            try {
                return Integer.parseInt(myProxyPort);
            } catch (NumberFormatException nfe) {
                //
            }
            return 3128;
        }

        public String getProxyUserName() {
            return myProxyUser;
        }

        public String getProxyPassword() {
            return myProxyPassword;
        }

        public void acknowledgeProxyContext(boolean accepted, SVNErrorMessage errorMessage) {
        }
    }

    public boolean isAuthenticationForced() {
        return myIsAuthenticationForced;
    }

    /**
     * Specifies the way how credentials are to be supplied to a
     * repository server.
     *
     * @param forced  <span class="javakeyword">true</span> to force
     *                credentials sending; <span class="javakeyword">false</span>
     *                to put off sending credentials till a server challenge
     * @see           #isAuthenticationForced()
     */
    public void setAuthenticationForced(boolean forced) {
        myIsAuthenticationForced = forced;
    }

    public int getReadTimeout(SVNRepository repository) {
        return getHostOptionsProvider().getHostOptions(repository.getLocation()).getReadTimeout();
    }

    public int getConnectTimeout(SVNRepository repository) {
        return getHostOptionsProvider().getHostOptions(repository.getLocation()).getConnectTimeout();
    }

    public void verifyHostKey(String hostName, int port, String keyAlgorithm, byte[] hostKey) throws SVNException {
        String realm = hostName + ":" + port + " <" + keyAlgorithm + ">";
        
        byte[] existingFingerprints = (byte[]) getRuntimeAuthStorage().getData("svn.ssh.server", realm);
        if (existingFingerprints == null && myProviders[2] instanceof ISVNPersistentAuthenticationProvider) {
            existingFingerprints = ((ISVNPersistentAuthenticationProvider) myProviders[2]).loadFingerprints(realm);
        }

        if (existingFingerprints == null || !equals(existingFingerprints, hostKey)) {
            SVNURL url = SVNURL.create("svn+ssh", null, hostName, port, "", true);
            final ISVNHostOptions hostOptions = getHostOptionsProvider().getHostOptions(url);
            boolean storageEnabled = hostOptions.isAuthStorageEnabled();
            if (getAuthenticationProvider() != null) {
                int accepted = getAuthenticationProvider().acceptServerAuthentication(url, realm, hostKey, storageEnabled);
                if (accepted == ISVNAuthenticationProvider.ACCEPTED && storageEnabled) {
                    if (storageEnabled && hostKey != null && myProviders[2] instanceof ISVNPersistentAuthenticationProvider) {
                        ((ISVNPersistentAuthenticationProvider) myProviders[2]).saveFingerprints(realm, hostKey);
                    }
                } else if (accepted == ISVNAuthenticationProvider.REJECTED) {
                    throw new SVNAuthenticationException(SVNErrorMessage.create(SVNErrorCode.AUTHN_CREDS_NOT_SAVED, "Host key ('" + realm + "') can not be verified."));
                }
                if (hostKey != null) {
                    getRuntimeAuthStorage().putData("svn.ssh.server", realm, hostKey);
                }
            }
        }
    }
    
    private static boolean equals(byte[] b1, byte[] b2) {
        if (b1 == null && b2 == b1) {
            return true;
        }
        if (b1 == null || b2 == null) {
            return false;
        }
        if (b1.length != b2.length) {
            return false;
        }
        for (int i = 0; i < b2.length; i++) {
            if (b1[i] != b2[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean isSSLPassphrasePromtSupported() {
        if (getAuthenticationProvider() == null) {
            return true;
        } else if (getAuthenticationProvider() instanceof ISVNSSLPasspharsePromptSupport) {
            return ((ISVNSSLPasspharsePromptSupport) getAuthenticationProvider()).isSSLPassphrasePromtSupported();
        }
        return false;
     }
}
