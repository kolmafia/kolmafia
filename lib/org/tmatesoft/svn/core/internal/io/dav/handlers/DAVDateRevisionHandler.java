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

import java.util.Date;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.io.dav.DAVElement;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNXMLUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;

import org.xml.sax.Attributes;


/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class DAVDateRevisionHandler extends BasicDAVHandler {

    public static StringBuffer generateDateRevisionRequest(StringBuffer xmlBuffer, Date date) {
        xmlBuffer = xmlBuffer == null ? new StringBuffer() : xmlBuffer;
        SVNXMLUtil.addXMLHeader(xmlBuffer);
        SVNXMLUtil.openNamespaceDeclarationTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "dated-rev-report", SVN_DAV_NAMESPACES_LIST, SVNXMLUtil.PREFIX_MAP, xmlBuffer);
        SVNXMLUtil.openCDataTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "creationdate", SVNDate.formatDate(date), xmlBuffer);
        SVNXMLUtil.addXMLFooter(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "dated-rev-report", xmlBuffer);
        return xmlBuffer;
    }

    private long myRevisionNumber;

    public DAVDateRevisionHandler() {
        init();
        myRevisionNumber = -1;
    }

    public long getRevisionNumber() {
        return myRevisionNumber;
    }

    protected void endElement(DAVElement parent, DAVElement element, StringBuffer cdata) throws SVNException {
        if (element == DAVElement.VERSION_NAME && cdata != null) {
            try {
                myRevisionNumber = Long.parseLong(cdata.toString());
            } catch (NumberFormatException nfe) {
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, nfe), SVNLogType.NETWORK);
            }
        }
    }

    protected void startElement(DAVElement parent, DAVElement element, Attributes attrs) {
    }

}
