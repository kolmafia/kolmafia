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
import org.tmatesoft.svn.core.internal.util.SVNXMLUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.ISVNLocationEntryHandler;
import org.tmatesoft.svn.core.io.SVNLocationEntry;
import org.tmatesoft.svn.util.SVNLogType;

import org.xml.sax.Attributes;


/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class DAVLocationsHandler extends BasicDAVHandler {

    public static StringBuffer generateLocationsRequest(StringBuffer xmlBuffer, String path, long pegRevision, long[] revisions) {
        xmlBuffer = xmlBuffer == null ? new StringBuffer() : xmlBuffer;
        SVNXMLUtil.addXMLHeader(xmlBuffer);
        SVNXMLUtil.openNamespaceDeclarationTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "get-locations", 
                SVN_DAV_NAMESPACES_LIST, SVNXMLUtil.PREFIX_MAP, xmlBuffer);
        SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "path", path, xmlBuffer);
        SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "peg-revision", String.valueOf(pegRevision), xmlBuffer);
        for (int i = 0; i < revisions.length; i++) {
            SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "location-revision", String.valueOf(revisions[i]), xmlBuffer);
        }
        SVNXMLUtil.addXMLFooter(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "get-locations", xmlBuffer);
        return xmlBuffer;
    }

    private static final DAVElement LOCATION_REPORT = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "get-locations-report");
    private static final DAVElement LOCATION = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "location");

    private ISVNLocationEntryHandler myLocationEntryHandler;
    private int myCount;


    public DAVLocationsHandler(ISVNLocationEntryHandler handler) {
        myLocationEntryHandler = handler;
        init();
    }

    protected void startElement(DAVElement parent, DAVElement element, Attributes attrs) throws SVNException {
        if (parent == LOCATION_REPORT && element == LOCATION) {
            String revStr = attrs.getValue("rev");
            if (revStr != null) {
                String path = attrs.getValue("path");
                if (path != null && myLocationEntryHandler != null) {
                    if (!path.startsWith("/")) {
                        path = "/" + path; 
                    }
                    try {
                        myLocationEntryHandler.handleLocationEntry(new SVNLocationEntry(Long.parseLong(revStr), path));
                    } catch (NumberFormatException nfe) {
                        SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, nfe), SVNLogType.NETWORK);
                    }
                    myCount++;
                }
            }
        }
    }

    protected void endElement(DAVElement parent, DAVElement element, StringBuffer cdata) throws SVNException {
    }

    public int getEntriesCount() {
        return myCount;
    }
}
