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
package org.tmatesoft.svn.core.internal.io.dav.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.X509KeyManager;

import org.tmatesoft.svn.core.SVNAuthenticationException;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
import org.tmatesoft.svn.core.auth.SVNSSLAuthentication;
import org.tmatesoft.svn.core.internal.wc.ISVNSSLPasspharsePromptSupport;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public final class HTTPSSLKeyManager implements X509KeyManager {


    public static KeyManager[] loadClientCertificate() throws SVNException {
        Provider pjacapi = Security.getProvider("CAPI");
        Provider pmscapi = Security.getProvider("SunMSCAPI");

        KeyManager[] result = null;
        SVNDebugLog.getDefaultLog().logError(SVNLogType.CLIENT,"using mscapi");
        // get key store
        KeyStore keyStore = null;
        // Note: When a security manager is installed,
        // the following call requires SecurityPermission
        // "authProvider.SunMSCAPI".
        try {
            if (pmscapi != null) {
                pmscapi.setProperty("Signature.SHA1withRSA","sun.security.mscapi.RSASignature$SHA1");
                keyStore = KeyStore.getInstance("Windows-MY",pmscapi);
            } else if (pjacapi != null) {
                keyStore = KeyStore.getInstance("CAPI");
            }
            if (keyStore != null) {
                keyStore.load(null, null);
            }
        } catch (Throwable th) {
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, th);
            throw new SVNException(SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, "Problems, when connecting with ms capi! "+th.getMessage(), null, SVNErrorMessage.TYPE_ERROR, th), th);
        }

        KeyManagerFactory kmf = null;

        if (keyStore != null) {
            try {
                kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                if (kmf != null) {
                    kmf.init(keyStore, null);
                    result = kmf.getKeyManagers();
                }
            }
            catch (Throwable th) {
                SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, th);
                throw new SVNException(SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, "MS Capi error: "+th.getMessage()), th);
            }
        }
        return result;
    }

    public static KeyManager[] loadClientCertificate(File clientCertFile, String clientCertPassword) throws SVNException {
        char[] passphrase = null;
        if (clientCertPassword == null || clientCertPassword.length() == 0) {
            // Client certificates without an passphrase can't be received from Java Keystores. 
            throw new SVNException(SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, "No client certificate passphrase supplied (did you forget to specify?).\n" +
                    "Note that client certificates with empty passphrases can''t be used. In this case please re-create the certificate with a passphrase."));
        }
        if (clientCertPassword != null) {
            passphrase = clientCertPassword.toCharArray();
        }
        KeyManager[] result = null;
        KeyStore keyStore = null;
        if (clientCertFile != null && clientCertFile.getName().startsWith(SVNSSLAuthentication.MSCAPI)) {
            SVNDebugLog.getDefaultLog().logError(SVNLogType.CLIENT,"using mscapi");
            try {
                keyStore = KeyStore.getInstance("Windows-MY");
                SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "using my windows store");
                if (keyStore != null) {
                    keyStore.load(null, null);
                }
                KeyManagerFactory kmf = null;
                if (keyStore != null) {
                    kmf = KeyManagerFactory.getInstance("SunX509");
                    if (kmf != null) {
                        kmf.init(keyStore, passphrase);
                        result = kmf.getKeyManagers();
                    }
                }
                return result;
            }
            catch (Throwable th) {
                SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, th);
                throw new SVNException(SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, "loadClientCertificate ms capi with file - should not be called: "+th.getMessage(), null, SVNErrorMessage.TYPE_ERROR, th), th);
            }

        }
        final InputStream is = SVNFileUtil.openFileForReading(clientCertFile, SVNLogType.NETWORK);
        try {
            keyStore = KeyStore.getInstance("PKCS12");
            if (keyStore != null) {
                keyStore.load(is, passphrase);
            }
        }
        catch (Throwable th) {
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, th);
            throw new SVNException(SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, th.getMessage(), null, SVNErrorMessage.TYPE_ERROR, th), th);
        }
        finally {
            SVNFileUtil.closeFile(is);
        }
        KeyManagerFactory kmf = null;

        if (keyStore != null) {
            try {
                kmf = KeyManagerFactory.getInstance("SunX509");
                if (kmf != null) {
                    kmf.init(keyStore, passphrase);
                    result = kmf.getKeyManagers();
                }
            }
            catch (Throwable th) {
                SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, th);
                throw new SVNException(SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, th.getMessage()), th);
            }
        }

        return result;
    }

    public KeyManager[] loadClientCertificate(SVNSSLAuthentication sslAuthentication) throws SVNException {
        String clientCertPassword = sslAuthentication.getPassword();
        String clientCertPath = sslAuthentication.getCertificatePath();
        File clientCertFile = sslAuthentication.getCertificateFile();
        
        char[] passphrase = clientCertPassword == null || clientCertPassword.length() == 0 ? new char[0] : clientCertPassword.toCharArray(); 
        String realm = clientCertPath;
        SVNAuthentication auth = null;

        KeyManager[] result = null;
        KeyStore keyStore = null;
        if (clientCertFile != null && clientCertFile.getName().startsWith(SVNSSLAuthentication.MSCAPI)) {
            SVNDebugLog.getDefaultLog().logError(SVNLogType.CLIENT,"using mscapi");
            try {
                keyStore = KeyStore.getInstance("Windows-MY");
                SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "using my windows store");
                if (keyStore != null) {
                    keyStore.load(null, null);
                }
                KeyManagerFactory kmf = null;
                if (keyStore != null) {
                    kmf = KeyManagerFactory.getInstance("SunX509");
                    if (kmf != null) {
                        kmf.init(keyStore, passphrase);
                        result = kmf.getKeyManagers();
                    }
                }
                return result;
            } catch (Throwable th) {
                SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, th);
                throw new SVNAuthenticationException(SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, "loadClientCertificate ms capi with file - should not be called: "+th.getMessage(), null, SVNErrorMessage.TYPE_ERROR, th), th);
            }

        }

        while(true) {
            try {
                final InputStream is = SVNFileUtil.openFileForReading(clientCertFile, SVNLogType.NETWORK);
                try {
                    keyStore = KeyStore.getInstance("PKCS12");
                    if (keyStore != null) {
                        keyStore.load(is, passphrase);
                    }
                } finally {
                    SVNFileUtil.closeFile(is);
                }
                if (auth != null) {
                    BasicAuthenticationManager.acknowledgeAuthentication(true, ISVNAuthenticationManager.SSL, realm, null, auth, url, authenticationManager);
                } else {
                    BasicAuthenticationManager.acknowledgeAuthentication(true, ISVNAuthenticationManager.SSL, clientCertPath, null,
                            new SVNPasswordAuthentication("", clientCertPassword, sslAuthentication.isStorageAllowed(), sslAuthentication.getURL(), false), url, authenticationManager);
                }
                break;
            } catch (IOException io) {
                SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, io);
                if (auth != null) {
                    BasicAuthenticationManager.acknowledgeAuthentication(false, ISVNAuthenticationManager.SSL, realm, SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, io.getMessage()), auth, url, authenticationManager);
                    auth = authenticationManager.getNextAuthentication(ISVNAuthenticationManager.SSL, realm, sslAuthentication.getURL());
                } else {
                    auth = authenticationManager.getFirstAuthentication(ISVNAuthenticationManager.SSL, realm, sslAuthentication.getURL());
                }
                if (auth instanceof SVNPasswordAuthentication) {
                    passphrase = ((SVNPasswordAuthentication) auth).getPassword().toCharArray();
                } else {
                    auth = null;
                    SVNErrorManager.cancel("authentication cancelled", SVNLogType.NETWORK);
                    break;
                }
                continue;
            } catch (Throwable th) {                
                SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, th);
                throw new SVNException(SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, th.getMessage(), null, SVNErrorMessage.TYPE_ERROR, th), th);
            }
        }
        
        KeyManagerFactory kmf = null;
        if (keyStore != null) {
            try {
                kmf = KeyManagerFactory.getInstance("SunX509");
                if (kmf != null) {
                    kmf.init(keyStore, passphrase);
                    result = kmf.getKeyManagers();
                }
            }
            catch (Throwable th) {
                SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, th);
                throw new SVNAuthenticationException(SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, th.getMessage()), th);
            }
        }
        return result;
    }

    private final ISVNAuthenticationManager authenticationManager;
    private final String realm;
    private final SVNURL url;

    private KeyManager[] myKeyManagers;
    private SVNSSLAuthentication myAuthentication;
    private Exception myException;
    private String chooseAlias = null;
    
    private boolean myIsFirstRequest = true;

    public HTTPSSLKeyManager(ISVNAuthenticationManager authenticationManager, String realm, SVNURL url) {
        this.authenticationManager = authenticationManager;
        this.realm = realm;
        this.url = url;
    }

    public String[] getClientAliases(String location, Principal[] principals) {
        if (!initializeNoException()) {
            return null;
        }

        for (Iterator<X509KeyManager> it = getX509KeyManagers(myKeyManagers).iterator(); it.hasNext();) {
            final X509KeyManager keyManager = it.next();
            final String[] clientAliases = keyManager.getClientAliases(location, principals);
            if (clientAliases != null) {
                return clientAliases;
            }
        }

        return null;
    }

    public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket) {
        if (!initializeNoException()) {
            return null;
        }
        if (chooseAlias != null) {
            return chooseAlias;
        }
        for (Iterator<X509KeyManager> it = getX509KeyManagers(myKeyManagers).iterator(); it.hasNext();) {
            final X509KeyManager keyManager = it.next();
            final String clientAlias = keyManager.chooseClientAlias(strings, principals, socket);
            if (clientAlias != null) {
                return clientAlias;
            }
        }
        return null;
    }

    public String[] getServerAliases(String location, Principal[] principals) {
        if (!initializeNoException()) {
            return null;
        }

        for (Iterator<X509KeyManager> it = getX509KeyManagers(myKeyManagers).iterator(); it.hasNext();) {
            final X509KeyManager keyManager = it.next();
            final String[] serverAliases = keyManager.getServerAliases(location, principals);
            if (serverAliases != null) {
                return serverAliases;
            }
        }

        return null;
    }

    public String chooseServerAlias(String location, Principal[] principals, Socket socket) {
        if (!initializeNoException()) {
            return null;
        }

        for (Iterator<X509KeyManager> it = getX509KeyManagers(myKeyManagers).iterator(); it.hasNext();) {
            final X509KeyManager keyManager = it.next();
            final String serverAlias = keyManager.chooseServerAlias(location, principals, socket);
            if (serverAlias != null) {
                return serverAlias;
            }
        }

        return null;
    }

    public X509Certificate[] getCertificateChain(String location) {
        if (!initializeNoException()) {
            return null;
        }
        for (Iterator<X509KeyManager> it = getX509KeyManagers(myKeyManagers).iterator(); it.hasNext();) {
            final X509KeyManager keyManager = it.next();
            final X509Certificate[] certificateChain = keyManager.getCertificateChain(location);
            if (certificateChain != null) {
                return certificateChain;
            }
        }
        return null;
    }

    public PrivateKey getPrivateKey(String string) {
        if (!initializeNoException()) {
            return null;
        }

        for (Iterator<X509KeyManager> it = getX509KeyManagers(myKeyManagers).iterator(); it.hasNext();) {
            final X509KeyManager keyManager = it.next();
            final PrivateKey privateKey = keyManager.getPrivateKey(string);
            if (privateKey != null) {
                return privateKey;
            }
        }
        return null;
    }

    public Exception getException() {
        return myException;
    }

    public void acknowledgeAndClearAuthentication(SVNErrorMessage errorMessage) throws SVNException {
        if (myAuthentication != null) {
            BasicAuthenticationManager.acknowledgeAuthentication(errorMessage == null, ISVNAuthenticationManager.SSL, realm, errorMessage, myAuthentication, url, authenticationManager);
        }
        if (errorMessage != null) {
            myKeyManagers = null;
            chooseAlias = null;
        } else {
            myAuthentication = null;
            myIsFirstRequest = true;
        }

        final Exception exception = myException;
        myException = null;
        if (exception instanceof SVNException) {
            throw (SVNException)exception;
        } else if (exception != null) {
            throw new SVNException(SVNErrorMessage.UNKNOWN_ERROR_MESSAGE, exception);
        }
    }

    private boolean initializeNoException() {
        try {
            final boolean result = initialize();
            myException = null;
            return result;
        }
        catch (Exception exception) {
            myException = exception;
            return false;
        }
    }

    private boolean initialize() throws SVNException {
        if (myKeyManagers != null) {
            return true;
        }

        for (; ;) {
            if (myIsFirstRequest) {
                myAuthentication = (SVNSSLAuthentication)authenticationManager.getFirstAuthentication(ISVNAuthenticationManager.SSL, realm, url);
                myIsFirstRequest = false;
            } else {
                myAuthentication = (SVNSSLAuthentication)authenticationManager.getNextAuthentication(ISVNAuthenticationManager.SSL, realm, url);
            }

            if (myAuthentication == null) {
                SVNErrorManager.cancel("SSL authentication with client certificate cancelled", SVNLogType.NETWORK);
            }

            final KeyManager[] keyManagers;
            try {
                if (isMSCAPI(myAuthentication)) {
                    keyManagers = loadClientCertificate();
                    chooseAlias = myAuthentication.getAlias();
                } else {
                    if (authenticationManager instanceof ISVNSSLPasspharsePromptSupport &&
                            ((ISVNSSLPasspharsePromptSupport) authenticationManager).isSSLPassphrasePromtSupported()) {
                        keyManagers = loadClientCertificate(myAuthentication);
                    } else {
                        keyManagers = loadClientCertificate(myAuthentication.getCertificateFile(), myAuthentication.getPassword());
                    }
                }
                
            } catch (SVNAuthenticationException authenticationException) {
                throw authenticationException;
            } catch (SVNCancelException cancel) {
                throw cancel;
            } catch (SVNException ex) {
                final SVNErrorMessage sslErr = SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, "Failed to load SSL client certificate: ''{0}''", new Object[] { ex.getMessage() }, SVNErrorMessage.TYPE_ERROR, ex.getCause());
                BasicAuthenticationManager.acknowledgeAuthentication(false, ISVNAuthenticationManager.SSL, realm, sslErr, myAuthentication, url, authenticationManager);
                continue;
            }

            myKeyManagers = keyManagers;
            return true;
        }
    }

    private static List<X509KeyManager> getX509KeyManagers(KeyManager[] keyManagers) {
        final List<X509KeyManager> x509KeyManagers = new ArrayList<X509KeyManager>();
        for (int index = 0; index < keyManagers.length; index++) {
            final KeyManager keyManager = keyManagers[index];
            if (keyManager instanceof X509KeyManager) {
                x509KeyManagers.add((X509KeyManager) keyManager);
            }
        }
        return x509KeyManagers;
    }
    
    private static boolean isMSCAPI(SVNSSLAuthentication sslAuthentication) {
        return sslAuthentication != null && SVNSSLAuthentication.MSCAPI.equals(sslAuthentication.getSSLKind());
    }
}