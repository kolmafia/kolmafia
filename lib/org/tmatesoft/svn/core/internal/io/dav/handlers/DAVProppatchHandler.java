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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.text.ParseException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.internal.io.dav.DAVElement;
import org.tmatesoft.svn.core.internal.io.dav.http.HTTPStatus;
import org.tmatesoft.svn.core.internal.util.SVNBase64;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.util.SVNXMLUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;

import org.xml.sax.Attributes;


/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class DAVProppatchHandler extends BasicDAVHandler {

    private static final Collection NAMESPACES = new LinkedList();

    static {
        NAMESPACES.add(DAVElement.DAV_NAMESPACE);
        NAMESPACES.add(DAVElement.SVN_DAV_PROPERTY_NAMESPACE);
        NAMESPACES.add(DAVElement.SVN_SVN_PROPERTY_NAMESPACE);
        NAMESPACES.add(DAVElement.SVN_CUSTOM_PROPERTY_NAMESPACE);
    }    

    public static StringBuffer generatePropertyRequest(StringBuffer buffer, String name, SVNPropertyValue value) {
        SVNProperties props = new SVNProperties();
        props.put(name, value);
        return generatePropertyRequest(buffer, props);
    }

    public static StringBuffer generatePropertyRequest(StringBuffer buffer, String name, byte[] value) {
        SVNProperties props = new SVNProperties();
        props.put(name, value);
        return generatePropertyRequest(buffer, props);
    }

    public static StringBuffer generatePropertyRequest(StringBuffer xmlBuffer, SVNProperties properties) {
        xmlBuffer = xmlBuffer == null ? new StringBuffer() : xmlBuffer;
        SVNXMLUtil.addXMLHeader(xmlBuffer);
        SVNXMLUtil.openNamespaceDeclarationTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "propertyupdate", NAMESPACES,
                SVNXMLUtil.PREFIX_MAP, xmlBuffer);

        // if there are non-null values
        if (hasNotNullValues(properties)) {
            SVNXMLUtil.openXMLTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "set", SVNXMLUtil.XML_STYLE_NORMAL, null,
                    xmlBuffer);
            SVNXMLUtil.openXMLTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "prop", SVNXMLUtil.XML_STYLE_NORMAL, null,
                    xmlBuffer);
            for (Iterator names = properties.nameSet().iterator(); names.hasNext();) {
                String name = (String) names.next();
                SVNPropertyValue value = properties.getSVNPropertyValue(name);
                if (value != null) {
                    xmlBuffer = appendProperty(xmlBuffer, name, value);
                }
            }
            SVNXMLUtil.closeXMLTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "prop", xmlBuffer);
            SVNXMLUtil.closeXMLTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "set", xmlBuffer);
        }

        // if there are null values
        if (hasNullValues(properties)) {
            SVNXMLUtil.openXMLTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "remove", SVNXMLUtil.XML_STYLE_NORMAL, null,
                    xmlBuffer);
            SVNXMLUtil.openXMLTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "prop", SVNXMLUtil.XML_STYLE_NORMAL, null,
                    xmlBuffer);
            for (Iterator names = properties.nameSet().iterator(); names.hasNext();) {
                String name = (String) names.next();
                SVNPropertyValue value = properties.getSVNPropertyValue(name);
                if (value == null) {
                    xmlBuffer = appendProperty(xmlBuffer, name, value);
                }
            }
            SVNXMLUtil.closeXMLTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "prop", xmlBuffer);
            SVNXMLUtil.closeXMLTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "remove", xmlBuffer);
        }

        SVNXMLUtil.addXMLFooter(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "propertyupdate", xmlBuffer);
        return xmlBuffer;
    }

    private static StringBuffer appendProperty(StringBuffer xmlBuffer, String name, SVNPropertyValue value) {
        String prefix = SVNProperty.isSVNProperty(name) && !SVNProperty.isSVNKitProperty(name) ?
                SVNXMLUtil.SVN_SVN_PROPERTY_PREFIX : SVNXMLUtil.SVN_CUSTOM_PROPERTY_PREFIX;
        String tagName = SVNProperty.shortPropertyName(name);
        if (value == null){
            return SVNXMLUtil.openXMLTag(prefix, tagName, SVNXMLUtil.XML_STYLE_SELF_CLOSING, null, xmlBuffer);            
        }
        Map attrs = null;
        String stringValue = value.getString();
        boolean isXMLSafe = true;
        if (value.isBinary()) {
            CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
            decoder.onMalformedInput(CodingErrorAction.REPORT);
            decoder.onMalformedInput(CodingErrorAction.REPORT);
            try {
                stringValue = decoder.decode(ByteBuffer.wrap(value.getBytes())).toString();
            } catch (CharacterCodingException e) {
                isXMLSafe = false;
            }
        }
        if (stringValue != null) {
            isXMLSafe = SVNEncodingUtil.isXMLSafe(stringValue);
        }

        if (!isXMLSafe) {
            attrs = new SVNHashMap();
            String attrPrefix = (String) SVNXMLUtil.PREFIX_MAP.get(DAVElement.SVN_DAV_PROPERTY_NAMESPACE);
            attrs.put(attrPrefix + ":encoding", "base64");
            byte[] toDecode = null;
            if (stringValue != null) {
                try {
                    toDecode = stringValue.getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    toDecode = stringValue.getBytes();
                }
            } else {
                toDecode = value.getBytes();
            }
            stringValue = SVNBase64.byteArrayToBase64(toDecode);
        }
        return SVNXMLUtil.openCDataTag(prefix, tagName, stringValue, attrs, xmlBuffer);
    }


    private static boolean hasNullValues(SVNProperties props) {
        if (props.isEmpty()) {
            return false;
        }
        return props.containsValue(null);
    }

    private static boolean hasNotNullValues(SVNProperties props) {
        if (props.isEmpty()) {
            return false;
        }
        if (!hasNullValues(props)) {
            return true;
        }
        for (Iterator entries = props.nameSet().iterator(); entries.hasNext();) {
            String propName = (String) entries.next();
            if (props.getSVNPropertyValue(propName) != null) {
                return true;
            }
        }
        return false;
    }

    //fields for multistatus response handling
    private StringBuffer myPropertyName;
    private StringBuffer myPropstatDescription;
    private StringBuffer myDescription;
    private boolean myPropstatContainsError;
    private boolean myResponseContainsError;
    private SVNErrorMessage myError;


    public DAVProppatchHandler() {
        init();
    }

    public SVNErrorMessage getError(){
        return myError;
    }

    private StringBuffer getPropertyName() {
        if (myPropertyName == null){
            myPropertyName = new StringBuffer();            
        }
        return myPropertyName;
    }

    private StringBuffer getPropstatDescription() {
        if (myPropstatDescription == null){
            myPropstatDescription = new StringBuffer();            
        }
        return myPropstatDescription;
    }

    private StringBuffer getDescription() {
        if (myDescription == null){
            myDescription = new StringBuffer();            
        }
        return myDescription;
    }

    protected void startElement(DAVElement parent, DAVElement element, Attributes attrs) throws SVNException {
        if (parent == DAVElement.PROP) {
            getPropertyName().setLength(0);
            if (DAVElement.SVN_DAV_PROPERTY_NAMESPACE.equals(element.getNamespace())) {
                getPropertyName().append(SVNProperty.SVN_PREFIX);
            } else if (DAVElement.DAV_NAMESPACE.equals(element.getNamespace())) {
                getPropertyName().append(DAVElement.DAV_NAMESPACE);
            }
            getPropertyName().append(element.getName());
        } else if (element == DAVElement.PROPSTAT) {
            myPropstatContainsError = false;
        }
    }

    protected void endElement(DAVElement parent, DAVElement element, StringBuffer cdata) throws SVNException {
        if (element == DAVElement.MULTISTATUS) {
            if (myResponseContainsError) {
                String description = null;
                if (getDescription().length() == 0) {
                    description = "The request response contained at least one error";
                } else {
                    description = getDescription().toString();
                }
                myError = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, description);
            }
        } else if (element == DAVElement.RESPONSE_DESCRIPTION) {
            if (parent == DAVElement.PROPSTAT) {
                getPropstatDescription().append(cdata);
            } else {
                if (getDescription().length() != 0) {
                    getDescription().append('\n');
                }
                getDescription().append(cdata);
            }
        } else if (element == DAVElement.STATUS) {
            try {
                HTTPStatus status = HTTPStatus.createHTTPStatus(cdata.toString());
                if (parent != DAVElement.PROPSTAT) {
                    myResponseContainsError |= status.getCodeClass() != 2;
                } else {
                    myPropstatContainsError = status.getCodeClass() != 2;
                }
            } catch (ParseException e) {
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED,
                        "The response contains a non-conforming HTTP status line"), SVNLogType.NETWORK);

            }
        } else if (element == DAVElement.PROPSTAT) {
            myResponseContainsError |= myPropstatContainsError;
            getDescription().append("Error setting property ");
            getDescription().append(getPropertyName());
            getDescription().append(":");
            getDescription().append(getPropstatDescription());
        }
    }
}
