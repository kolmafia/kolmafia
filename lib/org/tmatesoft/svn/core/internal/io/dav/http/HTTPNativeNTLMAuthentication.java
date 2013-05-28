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
package org.tmatesoft.svn.core.internal.io.dav.http;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.util.jna.SVNWinSecurity;
import org.tmatesoft.svn.core.internal.util.jna.SVNWinSecurity.SVNNTSecurityParameters;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @version 1.1.2
 * @author  TMate Software Ltd.
 */
public class HTTPNativeNTLMAuthentication extends HTTPNTLMAuthentication {

    private static final String NTLM_PROMPT_USER_PROPERTY = "svnkit.http.ntlm.promptUser";

    private SVNNTSecurityParameters myNTSecurityParameters;
    private String myLastToken;
    
    protected HTTPNativeNTLMAuthentication(String charset) {
        super(charset);
    }

    public static HTTPNativeNTLMAuthentication newInstance(String charset) {
        if (!SVNWinSecurity.isNativeLibraryAvailable()) {
            return null;
        }
        return new HTTPNativeNTLMAuthentication(charset);
    }
    
    public String authenticate() throws SVNException {
        if (myState != TYPE1 && myState != TYPE3) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, 
                    "Unsupported message type in HTTP NTLM authentication");
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        
        if (myNTSecurityParameters == null) {
            myNTSecurityParameters = SVNWinSecurity.getSecurityParams(getUserName(), getPassword(), getDomain());
        }
        
        String response = "NTLM " + SVNWinSecurity.getAuthHeader(myLastToken, myNTSecurityParameters);
        
        if (myNTSecurityParameters.myCrdHandle == null) {
            myNTSecurityParameters = null;
        }
        if (isInType3State()) {
            setType1State();
            if (myLastToken != null) {
                myLastToken = null;
            }
        }
        return response;
    }
    
    public void parseChallenge(String challenge) throws SVNException {
        myLastToken = challenge;
    }
    
    public boolean isNative() {
        return true;
    }

    @Override
    public boolean allowPropmtForCredentials() {
        final String prompt = System.getProperty(NTLM_PROMPT_USER_PROPERTY, "false");
        return Boolean.valueOf(prompt).booleanValue();
    }

}
