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

import com.trilead.ssh2.auth.AgentProxy;

import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;

import java.io.File;

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

    /**
     * Creates a user credential object for authenticating over an ssh tunnel.
     *
     * @param userName         the name of a user to authenticate
     * @param password         the user's password
     * @param portNumber       the number of a port to establish an ssh tunnel over
     * @param storageAllowed   if <span class="javakeyword">true</span> then
     *                         this credential is allowed to be stored in the
     *                         global auth cache, otherwise not
     * @param url              url these credentials are applied to
     *
     * @since 1.8.9
     */
    public static SVNSSHAuthentication newInstance(String userName, char[] password, int portNumber, boolean storageAllowed, SVNURL url, boolean isPartial) {
        return new SVNSSHAuthentication(userName, password, null, null, null, null, portNumber, storageAllowed, url, isPartial);
    }

    /**
     * Creates a user credential object for authenticating over an ssh tunnel.
     *
     * @param userName         the name of a user to authenticate
     * @param keyFile          the user's ssh private key file
     * @param passphrase       a password to the ssh private key
     * @param portNumber       the number of a port to establish an ssh tunnel over
     * @param storageAllowed   if <span class="javakeyword">true</span> then
     *                         this credential is allowed to be stored in the
     *                         global auth cache, otherwise not
     * @param url              url these credentials are applied to
     *
     * @since 1.8.9
     */
    public static SVNSSHAuthentication newInstance(String userName, File keyFile, char[] passphrase, int portNumber, boolean storageAllowed, SVNURL url, boolean isPartial) {
        return new SVNSSHAuthentication(userName, null, keyFile, null, passphrase, null, portNumber, storageAllowed, url, isPartial);
    }

    /**
     * Creates a user credential object for authenticating over an ssh tunnel.
     *
     * @param userName         the name of a user to authenticate
     * @param privateKey       the user's ssh private key
     * @param passphrase       a password to the ssh private key
     * @param portNumber       the number of a port to establish an ssh tunnel over
     * @param storageAllowed   if <span class="javakeyword">true</span> then
     *                         this credential is allowed to be stored in the
     *                         global auth cache, otherwise not
     * @param url              url these credentials are applied to
     *
     * @since 1.8.9
     */
    public static SVNSSHAuthentication newInstance(String userName, char[] keyValue, char[] passphrase, int portNumber, boolean storageAllowed, SVNURL url, boolean isPartial) {
        return new SVNSSHAuthentication(userName, null, null, keyValue, passphrase, null, portNumber, storageAllowed, url, isPartial);
    }

    /**
     * Creates a user credential object for authenticating over an ssh tunnel.
     *
     * @param userName         the name of a user to authenticate
     * @param agentProxy       SSH agent proxy
     * @param portNumber       the number of a port to establish an ssh tunnel over
     * @param url              url these credentials are applied to
     *
     * @since 1.8.9
     */
    public static SVNSSHAuthentication newInstance(String userName, AgentProxy agentProxy, int portNumber, SVNURL url, boolean isPartial) {
        return new SVNSSHAuthentication(userName, null, null, null, null, agentProxy, portNumber, false, url, isPartial);
    }

    private char[] myPassword;
    private char[] myPassphrase;
    private File myPrivateKeyFile;
    private AgentProxy myAgentProxy;
    private int myPortNumber;
    private char[] myPrivateKeyValue;

    /**
     * @deprecated Use {@link #newInstance(String, char[], int, boolean, SVNURL, boolean)} method
     */
    public SVNSSHAuthentication(String userName, String password, int portNumber, boolean storageAllowed) {
        this(userName, password == null ? new char[0] : password.toCharArray(), null, null, null, null, portNumber, storageAllowed, null, false);
    }

    /**
     * @deprecated Use {@link #newInstance(String, char[], int, boolean, SVNURL, boolean)} method
     *
     * @since 1.3.1
     */
    public SVNSSHAuthentication(String userName, String password, int portNumber, boolean storageAllowed, SVNURL url, boolean isPartial) {
        this(userName, password == null ? new char[0] : password.toCharArray(), null, null, null, null, portNumber, storageAllowed, url, isPartial);
    }

    /**
     * @deprecated Use {@link #newInstance(String, File, char[], int, boolean, SVNURL, boolean) method
     */
    public SVNSSHAuthentication(String userName, File keyFile, String passphrase, int portNumber, boolean storageAllowed) {
        this(userName, null, keyFile, passphrase != null ? passphrase.toCharArray() : null, null, null, portNumber, storageAllowed, null, false);
    }

    /**
     * @deprecated Use {@link #newInstance(String, File, char[], int, boolean, SVNURL, boolean) method
     *
     * @since 1.3.1
     */
    public SVNSSHAuthentication(String userName, File keyFile, String passphrase, int portNumber, boolean storageAllowed, SVNURL url, boolean isPartial) {
        this(userName, null, keyFile, null, passphrase != null ? passphrase.toCharArray() : null, null, portNumber, storageAllowed, url, isPartial);
    }

    /**
     * @deprecated Use {@link #newInstance(String, char[], char[], int, boolean, SVNURL, boolean) method
     */
    public SVNSSHAuthentication(String userName, char[] privateKey, String passphrase, int portNumber, boolean storageAllowed) {
        this(userName, null, null, privateKey, passphrase != null ? passphrase.toCharArray() : null, null, portNumber, storageAllowed, null, false);
    }

    /**
     * @deprecated Use {@link #newInstance(String, char[], char[], int, boolean, SVNURL, boolean) method
     *
     * @since 1.3.1
     */
    public SVNSSHAuthentication(String userName, char[] privateKey, String passphrase, int portNumber, boolean storageAllowed, SVNURL url, boolean isPartial) {
        this(userName, null, null, privateKey, passphrase != null ? passphrase.toCharArray() : null, null, portNumber, storageAllowed, url, isPartial);
    }

    /**
     * @deprecated Use {@link #newInstance(String, AgentProxy, int, SVNURL, boolean) method
     */
    public SVNSSHAuthentication(String userName, AgentProxy agentProxy, int portNumber, SVNURL url, boolean isPartial) {
        this(userName, null, null, null, null, agentProxy, portNumber, false, url, isPartial);
    }

    private SVNSSHAuthentication(String userName, char[] password, File keyFile, char[] keyValue, char[] passphrase, AgentProxy agentProxy, int portNumber, boolean storageAllowed, SVNURL url, boolean isPartial) {
        super(ISVNAuthenticationManager.SSH, userName, storageAllowed, url, isPartial);
        myAgentProxy = agentProxy;
        myPassword = password;
        myPassphrase = passphrase;
        myPrivateKeyFile = keyFile;
        myPrivateKeyValue = keyValue;
        myPortNumber = portNumber;
    }

    /**
     * Returns password. This is used when an ssh private key is not used.
     *
     * @deprecated Use {@link #getPasswordValue()} method
     * @return the user's password
     */
    public String getPassword() {
        return myPassword != null ? new String(myPassword) : null;
    }

    /**
     * Returns password. This is used when an ssh private key is not used.
     *
     * @since 1.8.9
     * @return password
     */
    public char[] getPasswordValue() {
        return myPassword;
    }

    /**
     * Returns the password to the ssh private key.
     *
     * @deprecated Use {@link #getPassphraseValue()} method
     *
     * @return the password to the private key
     * @see    #getPrivateKeyFile()
     */
    public String getPassphrase() {
        return myPassphrase != null ? new String(myPassphrase) : null;
    }

    /**
     * Returns the password to the ssh private key.
     *
     * @since 1.8.9
     * @return the password to the private key
     * @see    #getPrivateKeyFile()
     */
    public char[] getPassphraseValue() {
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

    /**
     * Tells whether this authentication object has a SSH agent connection
     */
    public AgentProxy getAgentProxy() {
        return myAgentProxy;
    }

    @Override
    public void dismissSensitiveData() {
        super.dismissSensitiveData();
        SVNEncodingUtil.clearArray(myPassphrase);
        SVNEncodingUtil.clearArray(myPassword);
        SVNEncodingUtil.clearArray(myPrivateKeyValue);
    }

    @Override
    public SVNAuthentication copy() {
        return new SVNSSHAuthentication(getUserName(),
                copyOf(myPassword), myPrivateKeyFile,
                copyOf(myPrivateKeyValue),
                copyOf(myPassphrase),
                myAgentProxy, myPortNumber, isStorageAllowed(), getURL(), isPartial());
    }


}
