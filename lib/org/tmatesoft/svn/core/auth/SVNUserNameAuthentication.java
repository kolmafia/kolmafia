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

import org.tmatesoft.svn.core.SVNURL;


/**
 * The <b>SVNUserNameAuthentication</b> class represents a simple 
 * authentication credential class that uses only a username to 
 * authenticate a user. Used along with the 
 * {@link ISVNAuthenticationManager#USERNAME} credential kind.
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public class SVNUserNameAuthentication extends SVNAuthentication {
    
    /**
     * Creates a username authentication credential.
     * 
     * @param userName         a user name
     * @param storageAllowed   if <span class="javakeyword">true</span> then
     *                         this credential is allowed to be stored in the 
     *                         global auth cache, otherwise not
     * @param url              url these credentials are applied to
     * 
     * @since 1.8.9
     */
    public static SVNUserNameAuthentication newInstance(String userName, boolean storageAllowed, SVNURL url, boolean isPartial) {
        return new SVNUserNameAuthentication(userName, storageAllowed, url, isPartial);        
    }
    
    /**
     * @deprecated Use {@link #newInstance(String, boolean, SVNURL, boolean)} method
     */
    public SVNUserNameAuthentication(String userName, boolean storageAllowed) {
        this(userName, storageAllowed, null, false);
    }

    /**
     * @deprecated Use {@link #newInstance(String, boolean, SVNURL, boolean)} method
     * 
     * @since 1.3.1
     */
    public SVNUserNameAuthentication(String userName, boolean storageAllowed, SVNURL url, boolean isPartial) {
        super(ISVNAuthenticationManager.USERNAME, userName, storageAllowed, url, isPartial);
    }

    @Override
    public SVNAuthentication copy() {
        return SVNUserNameAuthentication.newInstance(getUserName(), isStorageAllowed(), getURL(), isPartial());
    }
}
