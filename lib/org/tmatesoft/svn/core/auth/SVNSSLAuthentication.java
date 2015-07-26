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
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
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
    
    /**
     * @param certFile         user's certificate file
     * @param password         user's password 
     * @param storageAllowed   to store or not this credential in a 
     *                         credentials cache
     * @param url              url these credentials are applied to
     * @param isPartial
     * 
     * @return authentication object
     */
    public static SVNSSLAuthentication newInstance(File certFile, char[] password, boolean storageAllowed, SVNURL url, boolean isPartial) {
        return new SVNSSLAuthentication(SSL, null, certFile, password, storageAllowed, url, isPartial);
    }

    /**
     * @param cert             user's certificate
     * @param password         user's password 
     * @param storageAllowed   to store or not this credential in a 
     *                         credentials cache
     * @param url              url these credentials are applied to
     * @param isPartial
     * 
     * @return authentication object
     */
    public static SVNSSLAuthentication newInstance(byte[] cert, char[] password, boolean storageAllowed, SVNURL url, boolean isPartial) {
        return new SVNSSLAuthentication(SSL, null, cert, password, storageAllowed, url, isPartial);
    }

    /**
     * @param kind             authentication kind ({@link #MSCAPI} or {@link #SSL}
     * @param alias            alias 
     * @param storageAllowed   to store or not this credential in a 
     *                         credentials cache
     * @param url              url these credentials are applied to
     * @param isPartial
     * 
     * @return authentication object
     */
    public static SVNSSLAuthentication newInstance(String kind, String alias, boolean storageAllowed, SVNURL url, boolean isPartial) {
        return new SVNSSLAuthentication(kind, alias, (File) null, null, storageAllowed, url, isPartial);
    }

    
    private File myCertificate;
    private char[] myPassword;
    private String mySSLKind;
    private String myAlias;
    private String myCertificatePath;
    private byte[] myCertificateData;

    /**
     * @deprecated Use {@link #newInstance(File, char[], boolean, SVNURL, boolean) method
     */
    public SVNSSLAuthentication(File certFile, String password, boolean storageAllowed) {
        this(SSL, null, certFile, password != null ? password.toCharArray() : null, storageAllowed, null, false);
    }

    /**
     * @deprecated Use {@link #newInstance(File, char[], boolean, SVNURL, boolean) method
     */
    public SVNSSLAuthentication(File certFile, String password, boolean storageAllowed, SVNURL url, boolean isPartial) {
        this(SSL, null, certFile, password != null ? password.toCharArray() : null, storageAllowed, url, isPartial);
    }

    /**
     * @deprecated Use {@link #newInstance(String, String, boolean, SVNURL, boolean)} method
     */
    public SVNSSLAuthentication(String sslKind, String alias, boolean storageAllowed, SVNURL url, boolean isPartial) {
        this(sslKind, alias, (File) null, null, storageAllowed, url, isPartial);
    }

    private SVNSSLAuthentication(String sslKind, String alias, File certFile, char[] password, boolean storageAllowed, SVNURL url, boolean isPartial) {
        super(ISVNAuthenticationManager.SSL, null, storageAllowed, url, isPartial);
        mySSLKind = sslKind;
        myAlias = alias;
        myCertificate = certFile;
        myPassword = password;
    }

    private SVNSSLAuthentication(String sslKind, String alias, byte[] cert, char[] password, boolean storageAllowed, SVNURL url, boolean isPartial) {
        super(ISVNAuthenticationManager.SSL, null, storageAllowed, url, isPartial);
        mySSLKind = sslKind;
        myAlias = alias;
        myCertificateData = cert;
        myPassword = password;
    }

    /**
     * Returns password. 
     *
     * @deprecated Use {@link #getPasswordValue()} method
     * 
     * @return password
     */
    public String getPassword() {
        return myPassword != null ? new String(myPassword) : null;
    }
    
    /**
     * Returns certificate raw data
     * 
     * @return certificate data
     */
    public byte[] getCertificate() {
        return myCertificateData;
    }
    
    /**
     * Returns password. 
     *
     * @since 1.8.9
     * @return password
     */
    public char[] getPasswordValue() {
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

    @Override
    public void dismissSensitiveData() {
        super.dismissSensitiveData();
        SVNEncodingUtil.clearArray(myPassword);
    }
    
    @Override
    public SVNAuthentication copy() {
        return new SVNSSLAuthentication(mySSLKind, myAlias, myCertificate, copyOf(myPassword), isStorageAllowed(), getURL(), isPartial());
    }
    
}
