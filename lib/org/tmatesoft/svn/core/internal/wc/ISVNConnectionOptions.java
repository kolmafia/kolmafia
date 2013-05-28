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
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.SVNAuthentication;

/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public interface ISVNConnectionOptions {

    Collection getAuthTypes(SVNURL url);

    String[] getPasswordStorageTypes();

    boolean isAuthStorageEnabled(SVNURL url);

    boolean isStorePasswords(SVNURL url);

    boolean isStorePlainTextPasswords(String realm, SVNAuthentication auth) throws SVNException;

    boolean isStoreSSLClientCertificatePassphrases(SVNURL url);

    boolean isStorePlainTextPassphrases(String realm, SVNAuthentication auth) throws SVNException;

    String getUserName(SVNURL url);

    int getDefaultSSHPortNumber();

    String getDefaultSSHUserName();

    String getDefaultSSHPassword();

    String getDefaultSSHKeyFile();

    String getDefaultSSHPassphrase();

    String getSSLClientCertFile(SVNURL url);

    String getSSLClientCertPassword(SVNURL url);

    boolean trustDefaultSSLCertificateAuthority(SVNURL url);

    File[] getSSLAuthorityFiles(SVNURL url);

    String getProxyHost(SVNURL url);

    String getProxyPort(SVNURL url);

    String getProxyUserName(SVNURL url);

    String getProxyPassword(SVNURL url);

    int getReadTimeout(SVNURL url);

    int getConnectTimeout(SVNURL url);
}
