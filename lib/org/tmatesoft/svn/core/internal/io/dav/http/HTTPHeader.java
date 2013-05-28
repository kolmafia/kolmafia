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
package org.tmatesoft.svn.core.internal.io.dav.http;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class HTTPHeader {

    public static final String CONNECTION_HEADER = "Connection";
    public static final String PROXY_CONNECTION_HEADER = "Proxy-Connection";
    public static final String TRANSFER_ENCODING_HEADER = "Transfer-Encoding";
    public static final String CONTENT_LENGTH_HEADER = "Content-Length";
    public static final String CONTENT_ENCODING_HEADER = "Content-Encoding";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String AUTHENTICATE_HEADER = "WWW-Authenticate";
    public static final String PROXY_AUTHENTICATE_HEADER = "Proxy-Authenticate";
    public static final String LOCATION_HEADER = "Location";
    public static final String LOCK_OWNER_HEADER = "X-SVN-Lock-Owner";
    public static final String CREATION_DATE_HEADER = "X-SVN-Creation-Date";
    public static final String SVN_VERSION_NAME_HEADER = "X-SVN-Version-Name";
    public static final String SVN_OPTIONS_HEADER = "X-SVN-Options";
    public static final String TEXT_MD5 = "X-SVN-Result-Fulltext-MD5";
    public static final String BASE_MD5 = "X-SVN-Base-Fulltext-MD5";
    public static final String LOCK_TOKEN_HEADER = "Lock-Token";
    public static final String IF_HEADER = "If";
    public static final String DEPTH_HEADER = "Depth";
    public static final String LABEL_HEADER = "Label";
    public static final String DESTINATION_HEADER = "Destination";
    public static final String TIMEOUT_HEADER = "Timeout";
    public static final String DAV_HEADER = "DAV";
    public static final String SVN_DELTA_BASE_HEADER = "X-SVN-VR-Base";
    public static final String ACCEPT_ENCODING_HEADER = "Accept-Encoding";
    public static final String CONTENT_RANGE_HEADER = "content-range";
    public static final String HOST_HEADER = "Host";
    public static final String NEW_URI_HEADER = "New-uri";
    public static final String OVERWRITE_HEADER = "Overwrite";
    
    public static final String SVNDIFF_MIME_TYPE = "application/vnd.svn-svndiff";
    public static final String SET_COOKIE = "Set-Cookie";
    public static final String COOKIE = "Cookie";
    
    private Map<String, Collection<String>> myHeaders;

    public HTTPHeader() {
    }

    public String toString() {
        if (myHeaders == null) {
            return "";
        }
        
        StringBuffer representation = new StringBuffer();        
        for(Iterator<String> headers = myHeaders.keySet().iterator(); headers.hasNext();){
            String headerName = (String) headers.next();
            Collection<String> headerValues = myHeaders.get(headerName);
            for(Iterator<String> values = headerValues.iterator(); values.hasNext();){
                String value = (String) values.next();
                representation.append(headerName);
                representation.append(": ");
                representation.append(value);
                representation.append(HTTPRequest.CRLF);
            }
        }
        return representation.toString();
    }

    public void addHeaderValue(String name, String value) {
        Map<String, Collection<String>> headers = getHeaders();
        Collection<String> values = headers.get(name);
        if (values == null) {
            values = new LinkedList<String>();
            headers.put(name, values);
        }
        values.add(value);
    }
    
    public Map<String, Collection<String>> getRawHeaders() {
        return getHeaders();
    }

    public Collection<String> getHeaderValues(String name) {
        if (myHeaders == null) {
            return null;
        }
        List<String> values = new LinkedList<String>();
        for (Iterator<String> names = myHeaders.keySet().iterator(); names.hasNext();) {
            String headerName = (String) names.next();
            if (name.equalsIgnoreCase(headerName)) {
                values.addAll(myHeaders.get(headerName));
            }
        }        
        return values.isEmpty() ? null : values;
    }
    
    public String getFirstHeaderValue(String name){
        LinkedList<String> values = (LinkedList<String>) getHeaderValues(name);
        return values != null ? (String) values.getFirst() : null;
    }
    
    public boolean hasHeader(String name){
        LinkedList<String> values = (LinkedList<String>) getHeaderValues(name);
        return values != null && !values.isEmpty();
    }
    
    public void setHeaderValue(String name, String value){
        Map<String, Collection<String>> headers = getHeaders();
        Collection<String> values = headers.get(name);
        if (values == null) {
            values = new LinkedList<String>();
            headers.put(name, values);
        } else {
            values.clear();
        }
        values.add(value);
    }
    
    private Map<String, Collection<String>> getHeaders() {
        if (myHeaders == null) {
            myHeaders = new TreeMap<String, Collection<String>>();
        }
        return myHeaders;
    }

    public static HTTPHeader parseHeader(InputStream is, String charset) throws IOException, ParseException {
        HTTPHeader headers = new HTTPHeader();
        String name = null;
        StringBuffer value = null;
        for (; ;) {
            String line = HTTPParser.readLine(is, charset);
            if ((line == null) || (line.trim().length() < 1)) {
                break;
            }
            if ((line.charAt(0) == ' ') || (line.charAt(0) == '\t')) {
                if (value != null) {
                    value.append(' ');
                    value.append(line.trim());
                }
            } else {
                if (name != null) {
                    headers.addHeaderValue(name, value != null ? value.toString() : "");
                }
                
                int colon = line.indexOf(":");
                if (colon < 0) {
                    throw new ParseException("Unable to parse header: " + line, 0);
                }
                name = line.substring(0, colon).trim();
                value = new StringBuffer(line.substring(colon + 1).trim());
            }
    
        }
    
        if (name != null) {
            headers.addHeaderValue(name, value != null ? value.toString() : "");
        }
        return headers;
    }
}
