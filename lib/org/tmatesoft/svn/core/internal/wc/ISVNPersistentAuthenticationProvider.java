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

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.auth.SVNAuthentication;

/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public interface ISVNPersistentAuthenticationProvider {

    void saveAuthentication(SVNAuthentication auth, String kind, String realm) throws SVNException;

    void saveFingerprints(String realm, byte[] fingerprints);

    byte[] loadFingerprints(String realm);
}
