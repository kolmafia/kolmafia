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

import org.tmatesoft.svn.core.internal.io.dav.DAVElement;
import org.tmatesoft.svn.core.internal.util.SVNXMLUtil;

import org.xml.sax.Attributes;


/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class DAVLockHandler extends BasicDAVHandler {

    public static StringBuffer generateGetLockRequest(StringBuffer body) {
        return DAVPropertiesHandler.generatePropertiesRequest(body, new DAVElement[]{DAVElement.LOCK_DISCOVERY});
    }

    public static StringBuffer generateSetLockRequest(StringBuffer xmlBuffer, String comment) {
        xmlBuffer = xmlBuffer == null ? new StringBuffer() : xmlBuffer;
        SVNXMLUtil.addXMLHeader(xmlBuffer);
        SVNXMLUtil.openNamespaceDeclarationTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "lockinfo", DAV_NAMESPACES_LIST, 
                SVNXMLUtil.PREFIX_MAP, xmlBuffer);
        SVNXMLUtil.openXMLTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "lockscope", SVNXMLUtil.XML_STYLE_NORMAL, null, 
                xmlBuffer);
        SVNXMLUtil.openXMLTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "exclusive", SVNXMLUtil.XML_STYLE_SELF_CLOSING, 
                null, xmlBuffer);
        SVNXMLUtil.closeXMLTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "lockscope", xmlBuffer);
        SVNXMLUtil.openXMLTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "locktype", SVNXMLUtil.XML_STYLE_NORMAL, null, 
                xmlBuffer);
        SVNXMLUtil.openXMLTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "write", SVNXMLUtil.XML_STYLE_SELF_CLOSING, null, 
                xmlBuffer);
        SVNXMLUtil.closeXMLTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "locktype", xmlBuffer);
        comment = comment == null ? "" : comment;
        SVNXMLUtil.openCDataTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "owner", comment, xmlBuffer);
        SVNXMLUtil.addXMLFooter(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "lockinfo", xmlBuffer);
        return xmlBuffer;
    }

    private boolean myIsHandlingToken;
    private String myID;
    private String myComment;
    private String myExpiration;

    public DAVLockHandler() {
        init();
    }

    public String getComment() {
        return myComment;
    }

    public String getExpiration() {
        return myExpiration;
    }

    public String getID() {
        return myID;
    }

    protected void startElement(DAVElement parent, DAVElement element, Attributes attrs) {
        if (element == DAVElement.LOCK_TOKEN) {
            myIsHandlingToken = true;
        }
    }

    protected void endElement(DAVElement parent, DAVElement element, StringBuffer cdata) {
        if (element == DAVElement.HREF && myIsHandlingToken && cdata != null) {
            myID = cdata.toString();
        } else if (element == DAVElement.LOCK_TOKEN) {
            myIsHandlingToken = false;
        } else if (element == DAVElement.LOCK_OWNER && cdata != null) {
            myComment = cdata.toString();
        } else if (element == DAVElement.LOCK_TIMEOUT && cdata != null) {
            myExpiration = cdata.toString();
        }
    }
}
