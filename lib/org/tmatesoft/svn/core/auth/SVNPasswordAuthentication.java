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
 * The <b>SVNPasswordAuthentication</b> class represents a simple 
 * user credential pair - a username and password.
 * 
 * <p> 
 * To obtain a password credential, specify the {@link ISVNAuthenticationManager#PASSWORD PASSWORD} 
 * kind to credentials getter method of <b>ISVNAuthenticationManager</b>: 
 * {@link ISVNAuthenticationManager#getFirstAuthentication(String, String, org.tmatesoft.svn.core.SVNURL) getFirstAuthentication()}, 
 * {@link ISVNAuthenticationManager#getNextAuthentication(String, String, org.tmatesoft.svn.core.SVNURL) getNextAuthentication()}.
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public class SVNPasswordAuthentication extends SVNAuthentication {

    private String myPassword;
    
    /**
     * Creates a password user credential object given a username and password. 
     * 
     * @deprecated use constructor with SVNURL parameter instead
     * 
     * @param userName         the name of a user to authenticate 
     * @param password         the user's password
     * @param storageAllowed   if <span class="javakeyword">true</span> then
     *                         this credential is allowed to be stored in the 
     *                         global auth cache, otherwise not
     */
    public SVNPasswordAuthentication(String userName, String password, boolean storageAllowed) {
        this(userName, password, storageAllowed, null, false);
    }

    /**
     * Creates a password user credential object given a username and password. 
     * 
     * @param userName         the name of a user to authenticate 
     * @param password         the user's password
     * @param storageAllowed   if <span class="javakeyword">true</span> then
     *                         this credential is allowed to be stored in the 
     *                         global auth cache, otherwise not
     * @param url              url these credentials are applied to
     * @since 1.3.1
     */
    public SVNPasswordAuthentication(String userName, String password, boolean storageAllowed, SVNURL url, boolean isPartial) {
        super(ISVNAuthenticationManager.PASSWORD, userName, storageAllowed, url, isPartial);
        myPassword = password == null ? "" : password;
    }

    /**
     * Returns this user credential's password. 
     * 
     * @return the user's password
     */
    public String getPassword() {
        return myPassword;
    }
}
