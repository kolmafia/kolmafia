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
package org.tmatesoft.svn.core.internal.io.dav.handlers;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.io.dav.DAVElement;
import org.tmatesoft.svn.core.internal.util.SVNXMLUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.ISVNLocationSegmentHandler;
import org.tmatesoft.svn.core.io.SVNLocationSegment;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.util.SVNLogType;

import org.xml.sax.Attributes;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class DAVLocationSegmentsHandler extends BasicDAVHandler {
    private static final DAVElement LOCATION_SEGMENTS_REPORT = DAVElement.getElement(DAVElement.SVN_NAMESPACE, 
            "get-location-segments-report");
    private static final DAVElement LOCATION_SEGMENT = DAVElement.getElement(DAVElement.SVN_NAMESPACE, 
            "location-segment");


    public static StringBuffer generateGetLocationSegmentsRequest(StringBuffer xmlBuffer, String path, 
            long pegRevision, long startRevision, long endRevision) {
        xmlBuffer = xmlBuffer == null ? new StringBuffer() : xmlBuffer;
        SVNXMLUtil.addXMLHeader(xmlBuffer);
        SVNXMLUtil.openNamespaceDeclarationTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "get-location-segments", 
                SVN_DAV_NAMESPACES_LIST, SVNXMLUtil.PREFIX_MAP, xmlBuffer);
        SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "path", path, xmlBuffer);
        if (SVNRevision.isValidRevisionNumber(pegRevision)) {
            SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "peg-revision", 
                    String.valueOf(pegRevision), xmlBuffer);
        }
        if (SVNRevision.isValidRevisionNumber(startRevision)) {
            SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "start-revision", 
                    String.valueOf(startRevision), xmlBuffer);
        }
        if (SVNRevision.isValidRevisionNumber(endRevision)) {
            SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "end-revision", 
                    String.valueOf(endRevision), xmlBuffer);
        }
        SVNXMLUtil.addXMLFooter(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "get-location-segments", xmlBuffer);
        return xmlBuffer;
    }
    
    private ISVNLocationSegmentHandler myLocationSegmentHandler;
    private long myCount;

    public DAVLocationSegmentsHandler(ISVNLocationSegmentHandler handler) {
        myLocationSegmentHandler = handler;
        init();
    }

    public long getTotalRevisions() {
        return myCount;
    }
    
    protected void startElement(DAVElement parent, DAVElement element, Attributes attrs) throws SVNException {
        if (parent == LOCATION_SEGMENTS_REPORT && element == LOCATION_SEGMENT) {
            long rangeStart = SVNRepository.INVALID_REVISION;
            long rangeEnd = SVNRepository.INVALID_REVISION;
            String path = attrs.getValue("path");
            if (path != null && !path.startsWith("/")) {
                path = "/" + path;
            }
            String revStr = attrs.getValue("range-start");
            if (revStr != null) {
                try {
                    rangeStart = Long.parseLong(revStr);
                } catch (NumberFormatException nfe) {
                    SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, nfe), SVNLogType.NETWORK);
                }
            }
            revStr = attrs.getValue("range-end");
            if (revStr != null) {
                try {
                    rangeEnd = Long.parseLong(revStr);
                } catch (NumberFormatException nfe) {
                    SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, nfe), SVNLogType.NETWORK);
                }
            }
            
            if (SVNRevision.isValidRevisionNumber(rangeStart) && SVNRevision.isValidRevisionNumber(rangeEnd)) {
                if (myLocationSegmentHandler != null) {
                    myLocationSegmentHandler.handleLocationSegment(new SVNLocationSegment(rangeStart, rangeEnd, 
                            path));
                    myCount += rangeEnd - rangeStart + 1;
                }
            } else {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, 
                        "Expected valid revision range");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }
        }
    }

    protected void endElement(DAVElement parent, DAVElement element, StringBuffer cdata) throws SVNException {
    }

}
