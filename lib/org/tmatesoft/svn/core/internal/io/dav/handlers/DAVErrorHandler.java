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

package org.tmatesoft.svn.core.internal.io.dav.handlers;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.io.dav.DAVElement;

import org.xml.sax.Attributes;


/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class DAVErrorHandler extends BasicDAVHandler {

    private static final DAVElement SVN_ERROR = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "error");
    private static final DAVElement ERROR_DATA = DAVElement.getElement(DAVElement.SVN_APACHE_PROPERTY_NAMESPACE, "human-readable");

    private SVNErrorMessage myError;
    private SVNErrorCode myErrorCode;
    private String myErrorMessage;

    public DAVErrorHandler() {
        init();
        myErrorCode = SVNErrorCode.RA_DAV_REQUEST_FAILED;
        myErrorMessage = "General svn error from server";
    }

    public SVNErrorMessage getErrorMessage() {
        return myError;
    }

    protected void startElement(DAVElement parent, DAVElement element, Attributes attrs) throws SVNException {
        if (element == SVN_ERROR) {
            myErrorMessage = "General svn error from server";
            myError = SVNErrorMessage.create(myErrorCode, myErrorMessage);
        } else if (element == ERROR_DATA) {
            String errCode = attrs.getValue("errcode");
            if (errCode != null) {
                try {
                    myErrorCode = SVNErrorCode.getErrorCode(Integer.parseInt(errCode));
                    myError = SVNErrorMessage.create(myErrorCode, myErrorMessage);
                } catch (NumberFormatException nfe) {
                }
            }
        }
    }

    protected void endElement(DAVElement parent, DAVElement element, StringBuffer cdata) throws SVNException {
        if (element == ERROR_DATA && cdata != null) {
            String errorMessage = cdata.toString();
            while (errorMessage.endsWith("\n")) {
                errorMessage = errorMessage.substring(0, errorMessage.length() - 1);
            }
            while (errorMessage.startsWith("\n")) {
                errorMessage = errorMessage.substring(1);
            }
            myErrorMessage = errorMessage;
            myError = SVNErrorMessage.create(myErrorCode, myErrorMessage);
        }
    }
}
