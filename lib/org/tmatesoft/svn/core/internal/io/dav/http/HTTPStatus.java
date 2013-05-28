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

import java.text.ParseException;

import org.tmatesoft.svn.core.SVNErrorMessage;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class HTTPStatus {
    
    private String myStatusLine;
    private String myVersion;
    private int myCode;
    private String myReason;
    private HTTPHeader myHeader;
    private SVNErrorMessage myError;
    
    public static boolean isHTTPStatusLine(String statusLine) {
        try {
            HTTPStatus status = createHTTPStatus(statusLine);
            return status != null;
        } catch (ParseException pe) {
        }
        return false;
    }
    
    public static HTTPStatus createHTTPStatus(String statusLine) throws ParseException {
        int length = statusLine.length();
        int at = 0;
        int start = 0;
        
        String reason = null;
        String version = null;
        int code = -1;
        
        try {
            while (Character.isWhitespace(statusLine.charAt(at))) {
                ++at;
                ++start;
            }
            if (!"HTTP".equals(statusLine.substring(at, at += 4))) {
                throw new ParseException("Status-Line '" + statusLine + "' does not start with HTTP", 0);
            }
            //handle the HTTP-Version
            while (!Character.isWhitespace(statusLine.charAt(at)) && at < statusLine.length()) {
                at++;
            }
            if (at == statusLine.length()) {
                throw new ParseException("Unable to parse HTTP-Version from the status line: '" + statusLine + "'", 0);
            }
            version = (statusLine.substring(start, at)).toUpperCase();

            //advance through spaces
            while (Character.isWhitespace(statusLine.charAt(at)) && at < statusLine.length()) {
                at++;
            }
            if (at == statusLine.length()) {
                throw new ParseException("Status-Line '" + statusLine + "' is not valid", 0); 
            }
            //handle the Status-Code
            int to = at;
            while(!Character.isWhitespace(statusLine.charAt(to)) && to < statusLine.length()) {
                to++;
            }
            try {
                code = Integer.parseInt(statusLine.substring(at, to));
            } catch (NumberFormatException e) {
                throw new ParseException("Unable to parse status code from status line: '" + statusLine + "'", 0);
            }
            //handle the Reason-Phrase
            at = to + 1;
            if (at < length) {
                reason = statusLine.substring(at).trim();
            } else {
                reason = "";
            }
        } catch (StringIndexOutOfBoundsException e) {
            throw new ParseException("Status-Line '" + statusLine + "' is not valid", 0); 
        }
        return new HTTPStatus(version, reason, code, statusLine); 
    }

    private HTTPStatus(String version, String reason, int code, String statusLine) {
        myVersion = version;
        myStatusLine = statusLine;
        myReason = reason;
        myCode = code;         
    }

    public String getReason() {
        return myReason;
    }
    
    public int getCode() {
        return myCode;
    }

    public int getCodeClass() {
        return myCode / 100;
    }
    
    public String getStatusLine() {
        return myStatusLine;
    }
    
    public String getVersion() {
        return myVersion;
    }
    
    public boolean isHTTP11() {
        return "HTTP/1.1".equals(myVersion);        
    }

    public void setHeader(HTTPHeader header) {
        myHeader = header;
    }
    
    public HTTPHeader getHeader() {
        return myHeader;
    }

    public void setError(SVNErrorMessage error) {
        myError = error;
    }
    
    public SVNErrorMessage getError() {
        return myError;
    }

}
