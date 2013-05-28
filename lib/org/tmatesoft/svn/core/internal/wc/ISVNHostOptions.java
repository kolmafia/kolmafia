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

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.auth.SVNAuthentication;

/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public interface ISVNHostOptions {

    Collection<String> getAuthTypes();

    boolean isAuthStorageEnabled();

    boolean isStorePasswords();

    boolean isStoreSSLClientCertificatePassphrases();

    boolean isStorePlainTextPasswords(String realm, SVNAuthentication auth) throws SVNException;

    boolean isStorePlainTextPassphrases(String realm, SVNAuthentication auth) throws SVNException;

    String getUserName();

    String getSSLClientCertFile();

    String getSSLClientCertPassword();

    boolean trustDefaultSSLCertificateAuthority();

    File[] getSSLAuthorityFiles();

    String getProxyHost();

    String getProxyPort();

    String getProxyUserName();

    String getProxyPassword();

    int getReadTimeout();

    int getConnectTimeout();
}
