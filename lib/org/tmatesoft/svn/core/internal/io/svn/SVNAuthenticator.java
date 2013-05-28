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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public abstract class SVNAuthenticator {
    
    protected static final String SUCCESS = "success";
    protected static final String FAILURE = "failure";
    protected static final String STEP = "step";

    private SVNConnection myConnection;
    private OutputStream myConnectionOutputStream;
    private InputStream myConnectionInputStream;
    private SVNErrorMessage myLastError;
    private InputStream myPlainInputStream;
    private OutputStream myPlainOutputStream;

    protected SVNAuthenticator(SVNConnection connection) throws SVNException {
        myConnection = connection;
        // these are logged streams.
        myConnectionInputStream = connection.getInputStream();
        myConnectionOutputStream = connection.getOutputStream();
        // plain streams.
        try {
            myPlainInputStream = connection.getConnector().getInputStream();
            myPlainOutputStream = connection.getConnector().getOutputStream();
        } catch (IOException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e.getMessage());
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
    }
    
    protected void setOutputStream(OutputStream os) {
        myConnection.setOutputStream(os);
    }

    protected void setInputStream(InputStream is) {
        myConnection.setInputStream(is);
    }
    
    protected InputStream getConnectionInputStream() {
        return myConnectionInputStream;
    }

    protected OutputStream getConnectionOutputStream() {
        return myConnectionOutputStream;
    }
    
    protected InputStream getPlainInputStream() {
        return myPlainInputStream;
    }

    protected OutputStream getPlainOutputStream() {
        return myPlainOutputStream;
    }
    
    protected SVNConnection getConnection() {
        return myConnection;
    }

    protected SVNErrorMessage getLastError() {
        return myLastError;
    }
    
    public void dispose() {
    }
    
    protected void setLastError(SVNErrorMessage err) {
        myLastError = err;
    }

    public abstract SVNAuthentication authenticate(List mechs, String realm, SVNRepositoryImpl repository) throws SVNException;
}
