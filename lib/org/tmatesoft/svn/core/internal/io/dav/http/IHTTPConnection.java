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

import java.io.InputStream;
import java.io.OutputStream;

import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.auth.SVNAuthentication;

import org.xml.sax.helpers.DefaultHandler;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public interface IHTTPConnection {
    
    public void setSpoolResponse(boolean spoolResponse);

    public HTTPStatus request(String method, String path, HTTPHeader header, StringBuffer body, int ok1, int ok2, OutputStream dst, DefaultHandler handler) throws SVNException;

    public HTTPStatus request(String method, String path, HTTPHeader header, StringBuffer body, int ok1, int ok2, OutputStream dst, DefaultHandler handler, SVNErrorMessage context) throws SVNException;

    public HTTPStatus request(String method, String path, HTTPHeader header, InputStream body, int ok1, int ok2, OutputStream dst, DefaultHandler handler) throws SVNException;

    public HTTPStatus request(String method, String path, HTTPHeader header, InputStream body, int ok1, int ok2, OutputStream dst, DefaultHandler handler, SVNErrorMessage context) throws SVNException;

    public HTTPStatus getLastStatus();

    public SVNAuthentication getLastValidCredentials();

    public void clearAuthenticationCache();

    public void close();
}
