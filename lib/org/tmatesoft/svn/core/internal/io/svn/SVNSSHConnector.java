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
package org.tmatesoft.svn.core.internal.io.svn;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.tmatesoft.svn.core.SVNAuthenticationException;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNSSHHostVerifier;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNSSHAuthentication;
import org.tmatesoft.svn.core.auth.SVNUserNameAuthentication;
import org.tmatesoft.svn.core.internal.io.svn.ssh.SshAuthenticationException;
import org.tmatesoft.svn.core.internal.io.svn.ssh.SshSession;
import org.tmatesoft.svn.core.internal.io.svn.ssh.SshSessionPool;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

import com.trilead.ssh2.ServerHostKeyVerifier;
import com.trilead.ssh2.StreamGobbler;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNSSHConnector implements ISVNConnector {

    private static final String SVNSERVE_COMMAND = "svnserve -t";
    private static final String SVNSERVE_COMMAND_WITH_USER_NAME = "svnserve -t --tunnel-user ";
    
    private static final boolean ourIsUseSessionPing = Boolean.getBoolean("svnkit.ssh2.ping");
    private static SshSessionPool ourSessionPool = new SshSessionPool();
    
    private SshSession mySession;
    private InputStream myInputStream;
    private OutputStream myOutputStream;
    private boolean myIsUseSessionPing;
    
    public SVNSSHConnector() {
        this(true, true);
    }
    
    public SVNSSHConnector(boolean useConnectionPing, boolean useSessionPing) {
        myIsUseSessionPing = useSessionPing;
    }

    public void open(SVNRepositoryImpl repository) throws SVNException {
        ISVNAuthenticationManager authManager = repository.getAuthenticationManager();
        if (authManager == null) {
            SVNErrorManager.authenticationFailed("Authentication required for ''{0}''", repository.getLocation());
            return;
        }

        String realm = repository.getLocation().getProtocol() + "://" + repository.getLocation().getHost();
        if (repository.getLocation().hasPort()) {
            realm += ":" + repository.getLocation().getPort();
        }
        if (repository.getLocation().getUserInfo() != null && !"".equals(repository.getLocation().getUserInfo())) {
            realm = repository.getLocation().getUserInfo() + "@" + realm;
        }

        int reconnect = 1;
        while(true) {
            SVNSSHAuthentication authentication = (SVNSSHAuthentication) authManager.getFirstAuthentication(ISVNAuthenticationManager.SSH, realm, repository.getLocation());
            SshSession connection = null;
            
                while (authentication != null) {
                    try {
                        final ISVNSSHHostVerifier verifier = (ISVNSSHHostVerifier) (authManager instanceof ISVNSSHHostVerifier ? authManager : null);
                        String host = repository.getLocation().getHost();
                        int port = repository.getLocation().hasPort() ? repository.getLocation().getPort() : authentication.getPortNumber();
                        if (port < 0) {
                            port = 22;
                        }
                        String userName = authentication.getUserName();
                        char[] privateKey = authentication.getPrivateKey() != null ? authentication.getPrivateKey() : null;
                        if (privateKey == null && authentication.getPrivateKeyFile() != null) {
                            privateKey = SVNSSHPrivateKeyUtil.readPrivateKey(authentication.getPrivateKeyFile());
                        }
                        char[] passphrase = authentication.getPassphrase() != null ? authentication.getPassphrase().toCharArray() : null;
                        if (passphrase != null && passphrase.length == 0) {
                            passphrase = null;
                        }
                        char[] password = authentication.getPassword() != null ? authentication.getPassword().toCharArray() : null;
                        if (password != null && password.length == 0) {
                            password = null;
                        }
                        if (privateKey != null && !SVNSSHPrivateKeyUtil.isValidPrivateKey(privateKey, authentication.getPassphrase())) {
                            if (password == null) {
                                SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, "File ''{0}'' is not valid OpenSSH DSA or RSA private key file", authentication.getPrivateKeyFile());
                                SVNErrorManager.error(error, SVNLogType.NETWORK);
                            } 
                            privateKey = null;
                        }
                        final int connectTimeout = authManager.getConnectTimeout(repository);
                        final int readTimeout = authManager.getReadTimeout(repository);
                        
                        ServerHostKeyVerifier v = new ServerHostKeyVerifier() {
                            public boolean verifyServerHostKey(String hostname, int port,
                                    String serverHostKeyAlgorithm, byte[] serverHostKey)
                                    throws Exception {
                                if (verifier != null) {
                                    verifier.verifyHostKey(hostname, port, serverHostKeyAlgorithm, serverHostKey);
                                }
                                return true;
                            }
                        };
                        connection = ourSessionPool.openSession(host, port, userName, privateKey, passphrase, password, v, connectTimeout, readTimeout);
                        
                        if (connection == null) {
                            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_CONNECTION_CLOSED, "Cannot connect to ''{0}''", repository.getLocation().setPath("", false));
                            SVNErrorManager.error(err, SVNLogType.NETWORK);
                        }
                        
                        BasicAuthenticationManager.acknowledgeAuthentication(true, ISVNAuthenticationManager.SSH, realm, null, authentication, repository.getLocation(), authManager);
                        break;
                    } catch (SVNAuthenticationException e) {
                        SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, e);
                        BasicAuthenticationManager.acknowledgeAuthentication(false, ISVNAuthenticationManager.SSH, realm, e.getErrorMessage(), authentication, repository.getLocation(), authManager);
                        authentication = (SVNSSHAuthentication) authManager.getNextAuthentication(ISVNAuthenticationManager.SSH, realm, repository.getLocation());
                        connection = null;
                    } catch (SshAuthenticationException auth) {
                        SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, auth);
                        SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, auth.getMessage());
                        BasicAuthenticationManager.acknowledgeAuthentication(false, ISVNAuthenticationManager.SSH, realm, error, authentication, repository.getLocation(), authManager);
                        authentication = (SVNSSHAuthentication) authManager.getNextAuthentication(ISVNAuthenticationManager.SSH, realm, repository.getLocation());
                        connection = null;
                    } catch (IOException e) {
                        connection = null;
                        SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.RA_SVN_CONNECTION_CLOSED, e);
                        SVNErrorManager.error(error, SVNLogType.NETWORK);
                    }
                }
                if (authentication == null) {
                    SVNErrorManager.cancel("authentication cancelled", SVNLogType.NETWORK);
                } else if (connection == null) {
                    SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_SVN_CONNECTION_CLOSED, "Can not establish connection to ''{0}''", realm), SVNLogType.NETWORK);
                }
                
                try {
                    mySession = connection;
                    SVNAuthentication author = authManager.getFirstAuthentication(ISVNAuthenticationManager.USERNAME, realm, repository.getLocation());
                    if (author == null) {
                        SVNErrorManager.cancel("authentication cancelled", SVNLogType.NETWORK);
                    }
                    String userName = author.getUserName();
                    if (userName == null || "".equals(userName.trim())) {
                        userName = authentication.getUserName();
                    }
                    if (author.getUserName() == null || author.getUserName().equals(authentication.getUserName()) || 
                            "".equals(author.getUserName())) {
                        repository.setExternalUserName("");
                    } else {
                        repository.setExternalUserName(author.getUserName()); 
                    }
                    author = new SVNUserNameAuthentication(userName, author.isStorageAllowed(), repository.getLocation(), false);
                    BasicAuthenticationManager.acknowledgeAuthentication(true, ISVNAuthenticationManager.USERNAME, realm, null, author, repository.getLocation(), authManager);
    
                    if ("".equals(repository.getExternalUserName())) {
                        mySession.execCommand(SVNSERVE_COMMAND);
                    } else {
                        mySession.execCommand(SVNSERVE_COMMAND_WITH_USER_NAME + "\"" + repository.getExternalUserName() + "\"");
                    }
                    myOutputStream = mySession.getIn();
                    myOutputStream = new BufferedOutputStream(myOutputStream, 16*1024);
                    myInputStream = mySession.getOut();
                    myInputStream = new BufferedInputStream(myInputStream, 16*1024);
                    new StreamGobbler(mySession.getErr());
                    return;
                } catch (SocketTimeoutException e) {
	                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_IO_ERROR, "timed out waiting for server", null, SVNErrorMessage.TYPE_ERROR, e);
                    SVNErrorManager.error(err, e, SVNLogType.NETWORK);
                } catch (UnknownHostException e) {
	                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_IO_ERROR, "Unknown host " + e.getMessage(), null, SVNErrorMessage.TYPE_ERROR, e);
                    SVNErrorManager.error(err, e, SVNLogType.NETWORK);
                } catch (ConnectException e) {
	                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_IO_ERROR, "connection refused by the server", null, SVNErrorMessage.TYPE_ERROR, e);
                    SVNErrorManager.error(err, e, SVNLogType.NETWORK);
                } catch (IOException e) {
                    reconnect--;
                    if (reconnect >= 0) {
                        // try again, but close session first.
                        mySession.close();
                        continue;
                    }
                    repository.getDebugLog().logFine(SVNLogType.NETWORK, e);
                    close(repository);
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_CONNECTION_CLOSED, "Cannot connect to ''{0}'': {1}", new Object[] {repository.getLocation().setPath("", false), e.getMessage()});
                    SVNErrorManager.error(err, e, SVNLogType.NETWORK);
                }
//            } finally {
//                SVNSSHSession.unlock();
//            }
        }
    }

    public void close(SVNRepositoryImpl repository) throws SVNException {
        SVNFileUtil.closeFile(myOutputStream);
        SVNFileUtil.closeFile(myInputStream);
        if (mySession != null) {
            if (mySession != null) {
                mySession.close();
                mySession = null;
            }
        }
        mySession = null;
        myOutputStream = null;
        myInputStream = null;
    }

    public InputStream getInputStream() throws IOException {
        return myInputStream;
    }

    public OutputStream getOutputStream() throws IOException {
        return myOutputStream;
    }

    public boolean isConnected(SVNRepositoryImpl repos) throws SVNException {
        return mySession != null && !isStale();
    }
    
    public boolean isStale() {
        if (mySession == null) {
            return true;
        }
        if (!ourIsUseSessionPing) {
            return false;
        }
        if (!myIsUseSessionPing) {
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "SKIPPING CHANNEL PING, IT HAS BEEN DISABLED");
            return false;
        }
        try {
            mySession.ping();
        } catch (IOException e) {
            return true;
        }
        return false;
    }

    public static void shutdown() {
        ourSessionPool.shutdown();
    }

    public void handleExceptionOnOpen(SVNRepositoryImpl repository, SVNException exception) throws SVNException {
        throw exception;
    }
}