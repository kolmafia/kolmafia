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

/**
 * The <b>SVNSSHAuthentication</b> class represents a kind of credentials used 
 * to authenticate a user over an SSH tunnel.
 * 
 * <p> 
 * To obtain an ssh user credential, specify the {@link ISVNAuthenticationManager#SSH SSH} 
 * kind to credentials getter method of <b>ISVNAuthenticationManager</b>: 
 * {@link ISVNAuthenticationManager#getFirstAuthentication(String, String, org.tmatesoft.svn.core.SVNURL) getFirstAuthentication()}, 
 * {@link ISVNAuthenticationManager#getNextAuthentication(String, String, org.tmatesoft.svn.core.SVNURL) getNextAuthentication()}.
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 * @see     ISVNAuthenticationManager  
 */
public class SVNSSHAuthentication extends SVNAuthentication {

    private String myPassword;
    private String myPassphrase;
    private File myPrivateKeyFile;
    private int myPortNumber;
    private char[] myPrivateKeyValue;
    
    /**
     * Creates a user credential object for authenticating over an ssh tunnel. 
     * This kind of credentials is used when an ssh connection requires 
     * a user password instead of an ssh private key.
     * 
     * @deprecated use constructor with SVNURL parameter instead
     * 
     * @param userName         the name of a user to authenticate 
     * @param password         the user's password
     * @param portNumber       the number of a port to establish an ssh tunnel over  
     * @param storageAllowed   if <span class="javakeyword">true</span> then
     *                         this credential is allowed to be stored in the 
     *                         global auth cache, otherwise not
     */
    public SVNSSHAuthentication(String userName, String password, int portNumber, boolean storageAllowed) {
        this(userName, password, portNumber, storageAllowed, null, false);
    }

    /**
     * Creates a user credential object for authenticating over an ssh tunnel. 
     * This kind of credentials is used when an ssh connection requires 
     * a user password instead of an ssh private key.
     * 
     * @param userName         the name of a user to authenticate 
     * @param password         the user's password
     * @param portNumber       the number of a port to establish an ssh tunnel over  
     * @param storageAllowed   if <span class="javakeyword">true</span> then
     *                         this credential is allowed to be stored in the 
     *                         global auth cache, otherwise not
     * @param url              url these credentials are applied to
     * @since 1.3.1
     */
    public SVNSSHAuthentication(String userName, String password, int portNumber, boolean storageAllowed, SVNURL url, boolean isPartial) {
        this(userName, portNumber, storageAllowed, url, isPartial);
        myPassword = password;
    }

    /**
     * Creates a user credential object for authenticating over an ssh tunnel. 
     * This kind of credentials is used when an ssh connection requires 
     * an ssh private key.
     * 
     * @deprecated use constructor with SVNURL parameter instead
     * 
     * @param userName         the name of a user to authenticate 
     * @param keyFile          the user's ssh private key file 
     * @param passphrase       a password to the ssh private key
     * @param portNumber       the number of a port to establish an ssh tunnel over  
     * @param storageAllowed   if <span class="javakeyword">true</span> then
     *                         this credential is allowed to be stored in the 
     *                         global auth cache, otherwise not
     */
    public SVNSSHAuthentication(String userName, File keyFile, String passphrase, int portNumber, boolean storageAllowed) {
        this(userName, keyFile, passphrase, portNumber, storageAllowed, null, false);
    }

    /**
     * Creates a user credential object for authenticating over an ssh tunnel. 
     * This kind of credentials is used when an ssh connection requires 
     * an ssh private key.
     * 
     * @param userName         the name of a user to authenticate 
     * @param keyFile          the user's ssh private key file 
     * @param passphrase       a password to the ssh private key
     * @param portNumber       the number of a port to establish an ssh tunnel over  
     * @param storageAllowed   if <span class="javakeyword">true</span> then
     *                         this credential is allowed to be stored in the 
     *                         global auth cache, otherwise not
     * @param url              url these credentials are applied to
     * @since 1.3.1
     */
    public SVNSSHAuthentication(String userName, File keyFile, String passphrase, int portNumber, boolean storageAllowed, SVNURL url, boolean isPartial) {
        this(userName, portNumber, storageAllowed, url, isPartial);
        myPrivateKeyFile = keyFile;
        myPassphrase = passphrase;
    }
    
    /**
     * Creates a user credential object for authenticating over an ssh tunnel. 
     * This kind of credentials is used when an ssh connection requires 
     * an ssh private key.
     * 
     * @deprecated use constructor with SVNURL parameter instead
     * 
     * @param userName         the name of a user to authenticate 
     * @param privateKey       the user's ssh private key 
     * @param passphrase       a password to the ssh private key
     * @param portNumber       the number of a port to establish an ssh tunnel over  
     * @param storageAllowed   if <span class="javakeyword">true</span> then
     *                         this credential is allowed to be stored in the 
     *                         global auth cache, otherwise not
     */
    public SVNSSHAuthentication(String userName, char[] privateKey, String passphrase, int portNumber, boolean storageAllowed) {
        this(userName, privateKey, passphrase, portNumber, storageAllowed, null, false);
    }

    /**
     * Creates a user credential object for authenticating over an ssh tunnel. 
     * This kind of credentials is used when an ssh connection requires 
     * an ssh private key.
     * 
     * @param userName         the name of a user to authenticate 
     * @param privateKey       the user's ssh private key 
     * @param passphrase       a password to the ssh private key
     * @param portNumber       the number of a port to establish an ssh tunnel over  
     * @param storageAllowed   if <span class="javakeyword">true</span> then
     *                         this credential is allowed to be stored in the 
     *                         global auth cache, otherwise not
     * @param url              url these credentials are applied to
     * @since 1.3.1
     */
    public SVNSSHAuthentication(String userName, char[] privateKey, String passphrase, int portNumber, boolean storageAllowed, SVNURL url, boolean isPartial) {
        this(userName, portNumber, storageAllowed, url, isPartial);
        myPrivateKeyValue = privateKey;
        myPassphrase = passphrase;
    }

    private SVNSSHAuthentication(String userName, int portNumber, boolean storageAllowed, SVNURL url, boolean isPartial) {
        super(ISVNAuthenticationManager.SSH, userName, storageAllowed, url, isPartial);
        myPortNumber = portNumber;
    }
    
    /**
     * Returns the user account's password. This is used when an  
     * ssh private key is not used. 
     * 
     * @return the user's password
     */
    public String getPassword() {
        return myPassword;
    }
    
    /**
     * Returns the password to the ssh private key. 
     * 
     * @return the password to the private key
     * @see    #getPrivateKeyFile()
     */
    public String getPassphrase() {
        return myPassphrase;
    }
    
    /**
     * Returns the File representation referring to the file with the 
     * user's ssh private key. If the private key is encrypted with a 
     * passphrase, it should have been provided to an appropriate constructor.
     * 
     * @return the user's private key file
     */
    public File getPrivateKeyFile() {
        return myPrivateKeyFile;
    }
    
    /**
     * Returns ssh private key. If the private key is encrypted with a 
     * passphrase, it should have been provided to an appropriate constructor.
     * 
     * @return the user's private key file
     */
    public char[] getPrivateKey() {
        return myPrivateKeyValue;
    }
    
    /**
     * Returns the number of the port across which an ssh tunnel 
     * is established. 
     * 
     * @return the port number to establish an ssh tunnel over
     */
    public int getPortNumber() {
        return myPortNumber;
    }
    
    /**
     * Tells whether this authentication object contains a user's private key.
     * @return <span class="javakeyword">true</span> if either {@link #getPrivateKey()} or
     *         {@link #getPrivateKeyFile()} returns non-<span class="javakeyword">null</span>
     * @since  1.2.0 
     */
    public boolean hasPrivateKey() {
        return myPrivateKeyFile != null || myPrivateKeyValue != null;
    }
}
