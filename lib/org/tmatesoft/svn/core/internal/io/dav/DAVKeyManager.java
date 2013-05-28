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
package org.tmatesoft.svn.core.internal.io.dav;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;

import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
class DAVKeyManager {
    
    private static final String CERTIFICATE_FILE = "svnkit.ssl.client-cert-file";
    private static final String CERTIFICATE_PASSPHRASE = "svnkit.ssl.client-cert-password";
    private static final String OLD_CERTIFICATE_FILE = "javasvn.ssl.client-cert-file";
    private static final String OLD_CERTIFICATE_PASSPHRASE = "javasvn.ssl.client-cert-password";
    
    private static KeyManager[] ourKeyManagers;
    private static boolean ourIsInitialized;
    
    public static KeyManager[] getKeyManagers() {
        if (ourIsInitialized) {
            return ourKeyManagers;
        }
        ourIsInitialized = true;
        String certFileName = System.getProperty(CERTIFICATE_FILE, System.getProperty(OLD_CERTIFICATE_FILE));
        if (certFileName == null) {
            return null;
        }
        char[] passphrase = null;
        String pph = System.getProperty(CERTIFICATE_PASSPHRASE, System.getProperty(OLD_CERTIFICATE_PASSPHRASE));
        if (pph != null) {
            passphrase = pph.toCharArray();
        }
        KeyStore keyStore = null;            
        InputStream is = null;
        try {
            keyStore = KeyStore.getInstance("PKCS12");
            if (keyStore != null) {
                is = new FileInputStream(certFileName);
                keyStore.load(is, passphrase);                    
            }
        } catch (Throwable th) {
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.DEFAULT, th);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
        KeyManagerFactory kmf = null;
        if (keyStore != null) {
            try {
                kmf = KeyManagerFactory.getInstance("SunX509");
                if (kmf != null) {
                    kmf.init(keyStore, passphrase);
                    ourKeyManagers = kmf.getKeyManagers();
                }
            } catch (Throwable e) {
                SVNDebugLog.getDefaultLog().logFine(SVNLogType.DEFAULT, e);
            } 
        }
        return ourKeyManagers; 
    }

}
