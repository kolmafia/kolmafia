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
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;

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
    
    /**
     * Creates a password user credential object given a username and password.
     * 
     * @param userName         the name of a user to authenticate 
     * @param password         the user's password
     * @param storageAllowed   if <span class="javakeyword">true</span> then
     *                         this credential is allowed to be stored in the 
     *                         global auth cache, otherwise not
     * @param url              url these credentials are applied to
     * @param isPartial        whether this object only contains part of credentials information
     * 
     * @since 1.8.9
     */
    public static SVNPasswordAuthentication newInstance(String userName, char[] password, boolean storageAllowed, SVNURL url, boolean isPartial) {
        return new SVNPasswordAuthentication(userName, password, storageAllowed, url, isPartial);
    }

    private char[] myPassword;
    
    /**
     * Creates a password user credential object given a username and password. 
     * 
     * @deprecated use {@link #newInstance(String, char[], boolean, SVNURL, boolean)}
     * 
     * @param userName         the name of a user to authenticate 
     * @param password         the user's password
     * @param storageAllowed   if <span class="javakeyword">true</span> then
     *                         this credential is allowed to be stored in the 
     *                         global auth cache, otherwise not
     */
    public SVNPasswordAuthentication(String userName, String password, boolean storageAllowed) {
        this(userName, password == null ? new char[0] : password.toCharArray(), storageAllowed, null, false);
    }

    /**
     * Creates a password user credential object given a username and password.
     * 
     * @deprecated use {@link #newInstance(String, char[], boolean, SVNURL, boolean)}
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
        this(userName, password == null ? new char[0] : password.toCharArray(), storageAllowed, url, isPartial);
    }

    private SVNPasswordAuthentication(String userName, char[] password, boolean storageAllowed, SVNURL url, boolean isPartial) {
        super(ISVNAuthenticationManager.PASSWORD, userName, storageAllowed, url, isPartial);
        myPassword = password == null ? new char[0] : password;
    }

    /**
     * Returns password. 
     *
     * @deprecated Use {@link #getPasswordValue()} method
     * 
     * @return password
     */
    public String getPassword() {
        return new String(myPassword);
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

    @Override
    public void dismissSensitiveData() {
        super.dismissSensitiveData();
        SVNEncodingUtil.clearArray(myPassword);
    }

    @Override
    public SVNAuthentication copy() {
        return new SVNPasswordAuthentication(getUserName(), copyOf(myPassword), isStorageAllowed(), getURL(), isPartial());
    }
}
