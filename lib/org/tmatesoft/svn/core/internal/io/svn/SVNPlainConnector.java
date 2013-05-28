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
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNSocketFactory;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNPlainConnector implements ISVNConnector {

    private static final int DEFAULT_SVN_TIMEOUT = 0;

    private Socket mySocket;
    private OutputStream myOutputStream;
    private InputStream myInputStream;

    public void open(SVNRepositoryImpl repository) throws SVNException {
        if (mySocket != null) {
            return;
        }
        SVNURL location = repository.getLocation();
        try {
            int connectTimeout = repository.getAuthenticationManager() != null ? repository.getAuthenticationManager().getConnectTimeout(repository) : DEFAULT_SVN_TIMEOUT;
            int readTimeout = repository.getAuthenticationManager() != null ? repository.getAuthenticationManager().getReadTimeout(repository) : DEFAULT_SVN_TIMEOUT;
            mySocket = SVNSocketFactory.createPlainSocket(location.getHost(), location.getPort(), connectTimeout, readTimeout, repository.getCanceller());
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
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_IO_ERROR, e.getLocalizedMessage());
            SVNErrorManager.error(err, e, SVNLogType.NETWORK);
        }
    }

    public void close(SVNRepositoryImpl repository) throws SVNException {
        if (mySocket != null) {
            try {
                mySocket.shutdownInput();
            } catch (IOException e) {
                //  
            }
            try {
                mySocket.shutdownOutput();
            } catch (IOException e) {
                //  
            }
            try {
                mySocket.close();
            } catch (IOException ex) {
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_SVN_IO_ERROR, ex.getMessage(), ex), SVNLogType.NETWORK);
            } finally {
                mySocket = null;
                myInputStream = null;
                myOutputStream = null;
            }
        }
    }
    
    public boolean isStale() {
        try {
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "checking whether connection is stale.");
            boolean result = mySocket != null && SVNSocketFactory.isSocketStale(mySocket);
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "connection is stale: " + result);
            return result;
        } catch (IOException e) {
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "failure during stale check");
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, e);
            return true;
        }
    }
    
    public boolean isConnected(SVNRepositoryImpl repos) throws SVNException {
        return mySocket != null && mySocket.isConnected();
    }

    public InputStream getInputStream() throws IOException {
        if (myInputStream == null) {
            myInputStream = mySocket.getInputStream();
            myInputStream = new BufferedInputStream(myInputStream);
        }
        return myInputStream;
    }

    public OutputStream getOutputStream() throws IOException {
        if (myOutputStream == null) {
            myOutputStream = new BufferedOutputStream(mySocket.getOutputStream());
        }
        return myOutputStream;
    }

    public void free() {
    }

    public boolean occupy() {
        return true;
    }

    public void handleExceptionOnOpen(SVNRepositoryImpl repository, SVNException exception) throws SVNException {
        throw exception;
    }
}