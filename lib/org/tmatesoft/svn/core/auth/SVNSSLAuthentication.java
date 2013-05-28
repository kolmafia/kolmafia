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
package org.tmatesoft.svn.core.auth;

import java.io.File;

import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;

/**
 * The <b>SVNSSLAuthentication</b> class represents user's credentials used 
 * to authenticate a user in secure connections. Used along with the 
 * {@link ISVNAuthenticationManager#SSL SSL} credential kind. 
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public class SVNSSLAuthentication extends SVNAuthentication {

    public static final String MSCAPI = "MSCAPI";
    public static final String SSL = "SSL";
    
    private File myCertificate;
    private String myPassword;
    private String mySSLKind;
    private String myAlias;
    private String myCertificatePath;

    /**
     * Creates an SSL credentials object. 
     * 
     * @deprecated use constructor with SVNURL parameter instead
     * 
     * @param certFile         user's certificate file
     * @param password         user's password 
     * @param storageAllowed   to store or not this credential in a 
     *                         credentials cache    
     */
    public SVNSSLAuthentication(File certFile, String password, boolean storageAllowed) {
        this(certFile, password, storageAllowed, null, false);
    }

    /**
     * Creates an SSL credentials object. 
     * 
     * @param certFile         user's certificate file
     * @param password         user's password 
     * @param storageAllowed   to store or not this credential in a 
     *                         credentials cache
     * @param url              url these credentials are applied to
     * @since 1.3.1
     */
    public SVNSSLAuthentication(File certFile, String password, boolean storageAllowed, SVNURL url, boolean isPartial) {
        super(ISVNAuthenticationManager.SSL, null, storageAllowed, url, isPartial);
        myCertificate = certFile;
        myPassword = password;
        mySSLKind = SSL;
    }

    public SVNSSLAuthentication(String sslKind, String alias, boolean storageAllowed, SVNURL url, boolean isPartial) {
        this((File) null, null, storageAllowed, url, isPartial);
        mySSLKind = sslKind;
        myAlias = alias;
    }

    /**
     * Return a user's password. 
     * 
     * @return a password
     */
    public String getPassword() {
        return myPassword;
    }

    /**
     * Returns a user's certificate file. 
     * 
     * @return certificate file
     */
    public File getCertificateFile() {
        return myCertificate;
    }

    public String getSSLKind() {
        return mySSLKind;
    }
    
    /**
     * Only used for MSCAPI
     */
    public String getAlias() {
        return myAlias;
    }
    
    public String getCertificatePath() {
        if (myCertificatePath != null) {
            return myCertificatePath;
        }
        return myCertificate.getAbsolutePath();
    }
    
    public void setCertificatePath(String path) {
        myCertificatePath = path;
    }

    public static boolean isCertificatePath(String path) {
        return SVNFileType.getType(new File(path)) == SVNFileType.FILE;
    }
}
