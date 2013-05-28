/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.io.svn;

import java.io.IOException;
import java.util.List;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNPlainAuthenticator extends SVNAuthenticator {

    public SVNPlainAuthenticator(SVNConnection connection) throws SVNException {
        super(connection);
    }

    public SVNAuthentication authenticate(List mechs, String realm, SVNRepositoryImpl repos) throws SVNException {
        SVNErrorMessage failureReason = null;
        if (mechs == null || mechs.size() == 0) {
            return null;
        }
        ISVNAuthenticationManager authManager = repos.getAuthenticationManager();
        if (authManager != null && authManager.isAuthenticationForced() && mechs.contains("ANONYMOUS") && mechs.contains("CRAM-MD5")) {
            mechs.remove("ANONYMOUS");
        }
        SVNURL location = repos.getLocation();
        SVNPasswordAuthentication auth = null;
        if (repos.getExternalUserName() != null && mechs.contains("EXTERNAL")) {
            getConnection().write("(w(s))", new Object[]{"EXTERNAL", ""});
            failureReason = readAuthResponse();
        } else if (mechs.contains("ANONYMOUS")) {
            getConnection().write("(w(s))", new Object[]{"ANONYMOUS", ""});
            failureReason = readAuthResponse();
        } else if (mechs.contains("CRAM-MD5")) {
            while (true) {
                CramMD5 authenticator = new CramMD5();
                if (location != null) {
                    realm = "<" + location.getProtocol() + "://"
                            + location.getHost() + ":"
                            + location.getPort() + "> " + realm;
                }
                try {
                    if (auth == null && authManager != null) {
                        auth = (SVNPasswordAuthentication) authManager.getFirstAuthentication(ISVNAuthenticationManager.PASSWORD, realm, location);
                    } else if (authManager != null) {
                        BasicAuthenticationManager.acknowledgeAuthentication(false, ISVNAuthenticationManager.PASSWORD, realm, failureReason, auth, location, authManager);
                        auth = (SVNPasswordAuthentication) authManager.getNextAuthentication(ISVNAuthenticationManager.PASSWORD, realm, location);
                    }
                } catch (SVNException e) {
                    if (e.getErrorMessage().getErrorCode() == SVNErrorCode.CANCELLED) {
                        throw e;
                    } else if (getLastError() != null) {
                        SVNErrorManager.error(getLastError(), SVNLogType.NETWORK);
                    }
                    throw e;
                }
                if (auth == null) {
                    failureReason = SVNErrorMessage.create(SVNErrorCode.CANCELLED, "Authentication cancelled"); 
                    setLastError(failureReason);
                    break;
                    
                }
                if (auth.getUserName() == null || auth.getPassword() == null) {
                    failureReason = SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, "Can''t get password. Authentication is required for ''{0}''", realm);
                    break;
                }
                getConnection().write("(w())", new Object[]{"CRAM-MD5"});
                while (true) {
                    authenticator.setUserCredentials(auth);
                    List items = getConnection().readTuple("w(?s)", true);
                    String status = SVNReader.getString(items, 0);
                    if (SVNAuthenticator.SUCCESS.equals(status)) {
                        BasicAuthenticationManager.acknowledgeAuthentication(true, ISVNAuthenticationManager.PASSWORD, realm, null, auth, location, authManager);
                        return auth;
                    } else if (SVNAuthenticator.FAILURE.equals(status)) {                        
                        failureReason = SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, "Authentication error from server: {0}", SVNReader.getString(items, 1));
                        String message = SVNReader.getString(items, 1);
                        if (message != null) {
                            setLastError(SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, message));
                        }
                        break;
                    } else if (SVNAuthenticator.STEP.equals(status)) {
                        try {
                            byte[] response = authenticator.buildChallengeResponse(SVNReader.getBytes(items, 1));
                            getConnectionOutputStream().write(response);
                            getConnectionOutputStream().flush();
                        } catch (IOException e) {
                            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_SVN_IO_ERROR, e.getMessage()), e, SVNLogType.NETWORK);
                        }
                    }
                }
            }
        } else {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, "Cannot negotiate authentication mechanism"), SVNLogType.NETWORK);
        }
        if (failureReason != null) {
            if (getLastError() != null) {
                SVNErrorManager.error(getLastError(), SVNLogType.NETWORK);
            }
            SVNErrorManager.error(failureReason, SVNLogType.NETWORK);
        }

        return auth;
    }
    
    protected SVNErrorMessage readAuthResponse() throws SVNException {
        List items = getConnection().readTuple("w(?s)", true);
        if (SVNAuthenticator.SUCCESS.equals(SVNReader.getString(items, 0))) {
            return null;
        } else if (SVNAuthenticator.FAILURE.equals(SVNReader.getString(items, 0))) {
            return SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, "Authentication error from server: {0}", SVNReader.getString(items, 1));
        } 
        return SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, "Unexpected server response to authentication");
    }

}
