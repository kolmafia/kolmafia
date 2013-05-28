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

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.io.dav.DAVElement;
import org.tmatesoft.svn.core.internal.util.SVNXMLUtil;

import org.xml.sax.Attributes;


/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class DAVOptionsHandler extends BasicDAVHandler {


    public static final StringBuffer OPTIONS_REQUEST = new StringBuffer();

    static {
        SVNXMLUtil.addXMLHeader(OPTIONS_REQUEST);
        SVNXMLUtil.openNamespaceDeclarationTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "options", 
                DAV_NAMESPACES_LIST, SVNXMLUtil.PREFIX_MAP, OPTIONS_REQUEST);
        SVNXMLUtil.openXMLTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "activity-collection-set", 
                SVNXMLUtil.XML_STYLE_SELF_CLOSING, null, OPTIONS_REQUEST);
        SVNXMLUtil.addXMLFooter(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "options", OPTIONS_REQUEST);
    }

    private String myActivityCollectionURL = null;

    public DAVOptionsHandler() {
        init();
    }

    public String getActivityCollectionURL() {
        return myActivityCollectionURL;
    }

    protected void startElement(DAVElement parent, DAVElement element, Attributes attrs) throws SVNException {
    }

    protected void endElement(DAVElement parent, DAVElement element, StringBuffer cdata) throws SVNException {
        if (element == DAVElement.HREF) {
            myActivityCollectionURL = cdata.toString();
        }
    }
}