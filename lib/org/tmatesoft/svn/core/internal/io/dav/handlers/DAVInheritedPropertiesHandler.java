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

import java.util.HashMap;
import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.internal.io.dav.DAVElement;
import org.tmatesoft.svn.core.internal.util.SVNXMLUtil;
import org.xml.sax.Attributes;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class DAVInheritedPropertiesHandler extends BasicDAVHandler {

    public static StringBuffer generateReport(StringBuffer xmlBuffer, String path, long revision) {
        xmlBuffer = xmlBuffer == null ? new StringBuffer() : xmlBuffer;
        SVNXMLUtil.addXMLHeader(xmlBuffer);
        SVNXMLUtil.openNamespaceDeclarationTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "inherited-props-report", SVN_DAV_NAMESPACES_LIST, 
                SVNXMLUtil.PREFIX_MAP, xmlBuffer);
        SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "revision", String.valueOf(revision), xmlBuffer);
        SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "path", path, xmlBuffer);
        SVNXMLUtil.closeXMLTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "inherited-props-report", xmlBuffer);
        return xmlBuffer;
    }
    
    private Map<String, SVNProperties> inhertiedProperties;
    private String currentValueEncoding;
    private String currentPropertyName;
    private SVNPropertyValue currentPropertyValue;
    private SVNProperties currentProperties;
    
    private static final DAVElement IPROPNAME = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "iprop-propname");
    private static final DAVElement IPROPVALUE = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "iprop-propval");
    private static final DAVElement IPROPITEM = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "iprop-item");
    private static final DAVElement IPROPPATH = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "iprop-path");

    
    public DAVInheritedPropertiesHandler() {
        init();
        inhertiedProperties = new HashMap<String, SVNProperties>();
    }

    protected void startElement(DAVElement parent, DAVElement element, Attributes attrs) throws SVNException {
        if (element == IPROPVALUE) {
            currentValueEncoding = attrs.getValue("encoding");
        } else if (element == IPROPITEM) {
            currentProperties = new SVNProperties();
            currentPropertyName = null;
            currentPropertyValue = null;
        }
    }
    
    protected void endElement(DAVElement parent, DAVElement element, StringBuffer cdata) throws SVNException {
        if (element == IPROPNAME) {
            currentPropertyName = cdata.toString();
        } else if (element == IPROPVALUE) {
            currentPropertyValue = createPropertyValue(element, currentPropertyName, cdata, currentValueEncoding);
            currentProperties.put(currentPropertyName, currentPropertyValue);
            currentValueEncoding = null;
            currentPropertyName = null;
            currentPropertyValue = null;
        } else if (element == IPROPITEM) {
            currentPropertyName = null;
            currentPropertyValue = null;
        } else if (element == IPROPPATH) {
            String repositoryPath = cdata.toString();
            if (!repositoryPath.startsWith("/")) {
                repositoryPath = "/" + repositoryPath;
            }
            inhertiedProperties.put(repositoryPath, currentProperties);
        }
    }

    public Map<String, SVNProperties> getInheritedProperties() {
        return inhertiedProperties;
    }

}
