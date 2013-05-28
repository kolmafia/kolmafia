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
package org.tmatesoft.svn.core.internal.util;

import java.util.ArrayList;
import java.util.Collection;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import java.util.Iterator;
import java.util.Map;

import org.tmatesoft.svn.core.internal.io.dav.DAVElement;

/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class SVNXMLUtil {

    public static final Map PREFIX_MAP = new SVNHashMap();

    public static final String DAV_NAMESPACE_PREFIX = "D";
    public static final String SVN_NAMESPACE_PREFIX = "S";
    public static final String SVN_DAV_PROPERTY_PREFIX = "SD";
    public static final String SVN_CUSTOM_PROPERTY_PREFIX = "SC";
    public static final String SVN_SVN_PROPERTY_PREFIX = "SS";
    public static final String SVN_APACHE_PROPERTY_PREFIX = "SA";

    static {
        PREFIX_MAP.put(DAVElement.DAV_NAMESPACE, DAV_NAMESPACE_PREFIX);
        PREFIX_MAP.put(DAVElement.SVN_NAMESPACE, SVN_NAMESPACE_PREFIX);
        PREFIX_MAP.put(DAVElement.SVN_DAV_PROPERTY_NAMESPACE, SVN_DAV_PROPERTY_PREFIX);
        PREFIX_MAP.put(DAVElement.SVN_SVN_PROPERTY_NAMESPACE, SVN_SVN_PROPERTY_PREFIX);
        PREFIX_MAP.put(DAVElement.SVN_CUSTOM_PROPERTY_NAMESPACE, SVN_CUSTOM_PROPERTY_PREFIX);
        PREFIX_MAP.put(DAVElement.SVN_APACHE_PROPERTY_NAMESPACE, SVN_APACHE_PROPERTY_PREFIX);
    }

    private static final String FULL_XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    public static final String DEFAULT_XML_HEADER = "<?xml version=\"1.0\"?>\n";

    public static final int XML_STYLE_NORMAL = 1;
    public static final int XML_STYLE_PROTECT_CDATA = 2;
    public static final int XML_STYLE_SELF_CLOSING = 4;
    public static final int XML_STYLE_ATTRIBUTE_BREAKS_LINE = 8;

    public static StringBuffer addXMLHeader(StringBuffer target, boolean addUTFAttribute) {
        target = target == null ? new StringBuffer() : target;
        target.append(addUTFAttribute ? FULL_XML_HEADER : DEFAULT_XML_HEADER);
        return target;
    }

    public static StringBuffer addXMLHeader(StringBuffer target) {
        return addXMLHeader(target, true);
    }

    public static StringBuffer openNamespaceDeclarationTag(String prefix, String header, Collection namespaces, Map prefixMap, Map attrs, 
            StringBuffer target, boolean addEOL) {
        target = target == null ? new StringBuffer() : target;
        target.append("<");
        if (prefix != null) {
            target.append(prefix);
            target.append(":");
        }
        target.append(header);
        if (namespaces != null && !namespaces.isEmpty()) {
            Collection usedNamespaces = new ArrayList();
            for (Iterator iterator = namespaces.iterator(); iterator.hasNext();) {
                Object item = iterator.next();
                String currentNamespace = null;
                if (item instanceof DAVElement) {
                    DAVElement currentElement = (DAVElement) item;
                    currentNamespace = currentElement.getNamespace();
                } else if (item instanceof String) {
                    currentNamespace = (String) item;
                }
                if (currentNamespace != null && currentNamespace.length() > 0 && !usedNamespaces.contains(currentNamespace)) {
                    usedNamespaces.add(currentNamespace);
                    target.append(" xmlns");
                    if (prefixMap != null){
                        target.append(":");
                        target.append(prefixMap.get(currentNamespace));
                    }
                    target.append("=\"");
                    target.append(currentNamespace);
                    target.append("\"");
                }
            }
            usedNamespaces.clear();
        }
        if (attrs != null && !attrs.isEmpty()) {
            for (Iterator iterator = attrs.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                String name = (String) entry.getKey();
                String value = (String) entry.getValue();
                target.append(" ");
                target.append(name);
                target.append("=\"");
                target.append(SVNEncodingUtil.xmlEncodeAttr(value));
                target.append("\"");
            }
        }
        target.append(">");
        if (addEOL) {
            target.append('\n');
        }
        return target;
    }

    public static StringBuffer openNamespaceDeclarationTag(String prefix, String header, Collection namespaces, Map prefixMap, 
            StringBuffer target) {
        return openNamespaceDeclarationTag(prefix, header, namespaces, prefixMap, null, target, true);
    }

    public static StringBuffer addXMLFooter(String prefix, String header, StringBuffer target) {
        target = target == null ? new StringBuffer() : target;
        target.append("</");
        if (prefix != null) {
            target.append(prefix);
            target.append(":");
        }
        target.append(header);
        target.append(">");
        return target;
    }

    public static StringBuffer openCDataTag(String prefix, String tagName, String cdata, StringBuffer target) {
        return openCDataTag(prefix, tagName, cdata, null, target);
    }

    public static StringBuffer openCDataTag(String tagName, String cdata, StringBuffer target) {
        return openCDataTag(null, tagName, cdata, target);
    }

    public static StringBuffer openCDataTag(String prefix, String tagName, String cdata, String attr, String value, StringBuffer target) {
        Map attributes = new SVNHashMap();
        attributes.put(attr, value);
        return openCDataTag(prefix, tagName, cdata, attributes, target);
    }

    public static StringBuffer openCDataTag(String prefix, String tagName, String cdata, String attr, String value, boolean escapeQuotes, 
            boolean encodeCDATA, StringBuffer target) {
        Map attributes = new SVNHashMap();
        attributes.put(attr, value);
        return openCDataTag(prefix, tagName, cdata, attributes, escapeQuotes, encodeCDATA, target);
    }

    public static StringBuffer openCDataTag(String prefix, String tagName, String cdata, Map attributes, StringBuffer target) {
        return openCDataTag(prefix, tagName, cdata, attributes, false, true, target);
    }

    public static StringBuffer openCDataTag(String prefix, String tagName, String cdata, Map attributes, boolean escapeQuotes, 
            boolean encodeCDATA, StringBuffer target) {
        if (cdata == null) {
            return target;
        }
        target = openXMLTag(prefix, tagName, XML_STYLE_PROTECT_CDATA, attributes, target);
        if (encodeCDATA) {
            target.append(SVNEncodingUtil.xmlEncodeCDATA(cdata, escapeQuotes));
        } else {
            target.append(cdata);
        }
        target = closeXMLTag(prefix, tagName, target);
        return target;
    }
    
    public static StringBuffer openXMLTag(String prefix, String tagName, int style, String attr, String value, StringBuffer target) {
        Map attributes = new SVNHashMap();
        attributes.put(attr, value);
        return openXMLTag(prefix, tagName, style, attributes, target);
    }

    public static StringBuffer openXMLTag(String prefix, String tagName, int style, Map attributes, StringBuffer target) {
        target = target == null ? new StringBuffer() : target;
        target.append("<");
        if (prefix != null) {
            target.append(prefix);
            target.append(":");
        }
        target.append(tagName);
        if (attributes != null && !attributes.isEmpty()) {
            for (Iterator iterator = attributes.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                String name = (String) entry.getKey();
                String value = (String) entry.getValue();
                if ((style & XML_STYLE_ATTRIBUTE_BREAKS_LINE) != 0){
                    target.append("\n  ");
                }
                target.append(" ");
                target.append(name);
                target.append("=\"");
                target.append(SVNEncodingUtil.xmlEncodeAttr(value));
                target.append("\"");
            }
            attributes.clear();
        }
        if ((style & XML_STYLE_SELF_CLOSING) != 0) {
            target.append("/");
        }
        target.append(">");
        if ((style & XML_STYLE_PROTECT_CDATA) == 0) {
            target.append("\n");
        }
        return target;
    }

    public static StringBuffer closeXMLTag(String prefix, String tagName, StringBuffer target) {
        return closeXMLTag(prefix, tagName, target, true);
    }
    
    public static StringBuffer closeXMLTag(String prefix, String tagName, StringBuffer target, boolean addEOL) {
        target = target == null ? new StringBuffer() : target;
        target.append("</");
        if (prefix != null) {
            target.append(prefix);
            target.append(":");
        }
        target.append(tagName);
        target.append(">");
        if (addEOL) {
            target.append('\n');
        }
        return target;
    }
}
