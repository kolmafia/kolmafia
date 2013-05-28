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
package org.tmatesoft.svn.core.internal.io.dav.http;

import java.io.UnsupportedEncodingException;

import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
import org.tmatesoft.svn.core.internal.util.SVNBase64;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
class HTTPBasicAuthentication extends HTTPAuthentication {

    private String myCharset;

    public HTTPBasicAuthentication (SVNPasswordAuthentication credentials, String charset) {
        super(credentials);
        myCharset = charset;
    }

    protected HTTPBasicAuthentication (String name, String password, String charset) {
        super(name, password);
        myCharset = charset;
    }

    protected HTTPBasicAuthentication (String charset) {
        myCharset = charset;
    }

    public String authenticate() {
        if (getUserName() == null || getPassword() == null) {
            return null;
        }
        
        StringBuffer result = new StringBuffer();
        String authStr = getUserName() + ":" + getPassword();
        try {
            authStr = SVNBase64.byteArrayToBase64(authStr.getBytes(myCharset));
        } catch (UnsupportedEncodingException e) {
            authStr = SVNBase64.byteArrayToBase64(authStr.getBytes());
        }
        result.append("Basic ");
        result.append(authStr);
        return result.toString();
    }

    public String getAuthenticationScheme(){
        return "Basic";
    }

}
