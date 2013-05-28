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
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.internal.io.dav.DAVElement;
import org.tmatesoft.svn.core.internal.io.dav.DAVProperties;
import org.tmatesoft.svn.core.internal.io.dav.DAVUtil;
import org.tmatesoft.svn.core.internal.io.dav.http.HTTPStatus;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.util.SVNHashSet;
import org.tmatesoft.svn.core.internal.util.SVNXMLUtil;
import org.xml.sax.Attributes;

import java.text.ParseException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class DAVPropertiesHandler extends BasicDAVHandler {

    public static StringBuffer generatePropertiesRequest(StringBuffer xmlBuffer, DAVElement[] properties) {
        xmlBuffer = xmlBuffer == null ? new StringBuffer() : xmlBuffer;
        SVNXMLUtil.addXMLHeader(xmlBuffer);
        SVNXMLUtil.openNamespaceDeclarationTag(null, "propfind", DAV_NAMESPACES_LIST, null, xmlBuffer);
        if (properties != null) {
            xmlBuffer.append("<prop>");
            for (int i = 0; i < properties.length; i++) {
                SVNXMLUtil.openXMLTag(null, properties[i].getName(), SVNXMLUtil.XML_STYLE_SELF_CLOSING, "xmlns", properties[i].getNamespace(), xmlBuffer);
            }
            SVNXMLUtil.closeXMLTag(null, "prop", xmlBuffer);
        } else {
            SVNXMLUtil.openXMLTag(null, "allprop", SVNXMLUtil.XML_STYLE_SELF_CLOSING, null, xmlBuffer);
        }
        SVNXMLUtil.addXMLFooter(null, "propfind", xmlBuffer);
        return xmlBuffer;
    }

    private static final Set PROP_ELEMENTS = new SVNHashSet();

    static {
        PROP_ELEMENTS.add(DAVElement.HREF);
        PROP_ELEMENTS.add(DAVElement.STATUS);
        PROP_ELEMENTS.add(DAVElement.BASELINE);
        PROP_ELEMENTS.add(DAVElement.BASELINE_COLLECTION);
        PROP_ELEMENTS.add(DAVElement.COLLECTION);
        PROP_ELEMENTS.add(DAVElement.VERSION_NAME);
        PROP_ELEMENTS.add(DAVElement.GET_CONTENT_LENGTH);
        PROP_ELEMENTS.add(DAVElement.CREATION_DATE);
        PROP_ELEMENTS.add(DAVElement.CREATOR_DISPLAY_NAME);
        PROP_ELEMENTS.add(DAVElement.BASELINE_RELATIVE_PATH);
        PROP_ELEMENTS.add(DAVElement.MD5_CHECKSUM);
        PROP_ELEMENTS.add(DAVElement.REPOSITORY_UUID);
    }

    private DAVProperties myCurrentResource;
    private int myStatusCode;
    private String myEncoding;
    private Map myResources;
    private Map myCurrentProperties;

    public DAVPropertiesHandler() {
        init();
    }

    public Map getDAVProperties() {
        return myResources;
    }

    protected void startElement(DAVElement parent, DAVElement element, Attributes attrs) throws SVNException {
        if (element == DAVElement.RESPONSE) {
            if (myCurrentResource != null) {
                invalidXML();
            }
            myCurrentResource = new DAVProperties();
            myCurrentProperties = new SVNHashMap();
            myStatusCode = 0;
        } else if (element == DAVElement.PROPSTAT) {
            myStatusCode = 0;
        } else if (element == DAVElement.COLLECTION) {
            myCurrentResource.setCollection(true);
        } else {
            myEncoding = attrs.getValue(DAVElement.SVN_DAV_PROPERTY_NAMESPACE, "encoding");
        }
    }

    protected void endElement(DAVElement parent, DAVElement element, StringBuffer cdata) throws SVNException {
        DAVElement name = null;
        SVNPropertyValue value = null;
        if (myCurrentProperties == null) {
            invalidXML();
        }
        if (element == DAVElement.RESPONSE) {
            if (myCurrentResource.getURL() == null) {
                invalidXML();
            }
            myResources.put(myCurrentResource.getURL(), myCurrentResource);
            myCurrentResource = null;
            return;
        } else if (element == DAVElement.PROPSTAT) {
            if (myStatusCode != 0) {
                for (Iterator entries = myCurrentProperties.entrySet().iterator(); entries.hasNext();) {
                    Map.Entry entry = (Map.Entry) entries.next();
                    DAVElement propName = (DAVElement) entry.getKey();
                    SVNPropertyValue propValue = (SVNPropertyValue) entry.getValue();
                    if (myStatusCode == 200) {
                        myCurrentResource.setProperty(propName, propValue);
                    }
                }
                myCurrentProperties.clear();
            } else {
                invalidXML();
            }
            return;
        } else if (element == DAVElement.STATUS) {
            if (cdata == null) {
                invalidXML();
            }
            try {
                HTTPStatus status = HTTPStatus.createHTTPStatus(cdata.toString());
                if (status == null) {
                    invalidXML();
                }
                myStatusCode = status.getCode();
            } catch (ParseException e) {
                invalidXML();
            }
            return;
        } else if (element == DAVElement.HREF) {
            if (parent == DAVElement.RESPONSE) {
                // set resource url
                String path = cdata.toString();
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                myCurrentResource.setURL(path);
                return;
            }
            name = parent;
            if (name == null) {
                return;
            }
            value = SVNPropertyValue.create(cdata.toString());
        } else if (cdata != null) {
            if (myCurrentProperties.containsKey(element)) {
                // was already set with href.
                return;
            }
            name = element;
            String propertyName = DAVUtil.getPropertyNameByElement(name);
            value = createPropertyValue(name, propertyName, cdata, myEncoding);
            myEncoding = null;
        }
        if (name != null && value != null) {
            myCurrentProperties.put(name, value);
        }
    }

    public void setDAVProperties(Map result) {
        myResources = result;
    }

}