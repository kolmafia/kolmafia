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

package org.tmatesoft.svn.core;

import org.tmatesoft.svn.core.auth.SVNAuthentication;

/**
 * The <b>SVNAuthenticationCancelkedException</b> is used to signal on
 * authentication being cancelled by the user.
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.9
 * @see		SVNCancelException
 */
public class SVNAuthenticationCancelledException extends SVNCancelException {

    private static final long serialVersionUID = 4846L;
    private final SVNErrorMessage myLastError;
    private final SVNAuthentication myLastAuthentication;

    public SVNAuthenticationCancelledException(SVNErrorMessage lastError, SVNAuthentication lastAuthentication) {
        super(SVNErrorMessage.create(SVNErrorCode.CANCELLED, "Authentication cancelled"));

        myLastError = lastError;
        myLastAuthentication = lastAuthentication;
    }

    public SVNErrorMessage getLastAuthenticationError() {
        return myLastError;
    }

    public SVNAuthentication getLastAuthentication() {
        return myLastAuthentication;
    }

}
