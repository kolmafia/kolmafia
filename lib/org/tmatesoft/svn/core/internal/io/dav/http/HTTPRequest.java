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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.Iterator;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVElement;
import org.tmatesoft.svn.util.Version;
import org.xml.sax.helpers.DefaultHandler;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
class HTTPRequest {

    public static final char[] CRLF = { '\r', '\n' };

    private boolean myIsSecured;
    private boolean myIsProxied;
    private HTTPConnection myConnection;

    private String myAuthentication;
    private String myProxyAuthentication;

    private HTTPHeader myResponseHeader;
    private HTTPStatus myStatus;

    private SVNErrorMessage myErrorMessage;
    private DefaultHandler myResponseHandler;
    private OutputStream myResponseStream;

    private byte[] myRequestBody;
    private InputStream myRequestStream;
    private boolean myIsProxyAuthForced;
    private boolean myIsKeepAlive;
    private String myCharset;

    private long myTimeout;

    private Collection<String> myCookies;

    public HTTPRequest(String charset) {
        myCharset = charset;
    }
    
    public void reset() {
        if (myRequestStream != null) {
            try {
                myRequestStream.reset();
            } catch (IOException e) {
            }
        }
        myAuthentication = null;
        myProxyAuthentication = null;
        myResponseHeader = null;
        myStatus = null;
        myErrorMessage = null;
    }
    
    public void setProxied(boolean proxied) {
        myIsProxied = proxied;
    }
    
    public void setSecured(boolean secured) {
        myIsSecured = secured;
    }
    
    public void setConnection(HTTPConnection connection) {
        myConnection = connection;
    }
    
    public void initCredentials(HTTPAuthentication authentication, String method, String path) {
        if (authentication != null) {
            authentication.setChallengeParameter("method", method);
            authentication.setChallengeParameter("uri", composeRequestURI(method, path));
            authentication.setChallengeParameter("host", myConnection.getHost().getHost());
        }
    }
    
    public void setAuthentication(String auth) {
        myAuthentication = auth;
    }

    public void setProxyAuthentication(String auth) {
        myProxyAuthentication = auth;
    }
    
    public void setForceProxyAuth(boolean force) {
        myIsProxyAuthForced = force;
    }
    
    public void setResponseHandler(DefaultHandler handler) {
        myResponseHandler = handler;
    }
    
    public void setResponseStream(OutputStream os) {
        myResponseStream = os;
    }
    
    public void setRequestBody(byte[] body) {
        myRequestBody = body;
    }
    
    public void setRequestBody(StringBuffer sb) {
        try {
            myRequestBody = sb.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            myRequestBody = sb.toString().getBytes();
        }
    }
    
    public void setRequestBody(InputStream is) {
        myRequestStream = is;
    }
    
    /**
     * heart of http engine.
     * 
     * features:
     *  // all this should be moved outside this method:
     *  - authentication callback to process 401 and 403 codes, failure results in returning error message.
     *  - another callback to process 301 and 302 codes, failure results in returning error message.
     *  - code that process ssl exceptions and re-prompts for client certificate when allowed.
     *  // auth error, ssl exception and "moved" errors should be processed by the caller.
     *  
     *  - code to send request body.
     *  - code to parse svn error response in case return code is not ok1 and ok2.
     *  - standard http error should be returned otherwise.
     *  
     *  - body may be resetable inputStream + length - IMeasurable.
     *  // this may throw IOException that will be converted to: timeout error, can't connect error, or ssl will re-prompt.
     */
    public void dispatch(String request, String path, HTTPHeader header, int ok1, int ok2, SVNErrorMessage context) throws IOException {
        long length = 0;
        if (myRequestBody != null) {
            length = myRequestBody.length;
        } else if (myRequestStream instanceof ByteArrayInputStream) {
            length = ((ByteArrayInputStream) myRequestStream).available();
        } else if (header != null && header.hasHeader(HTTPHeader.CONTENT_LENGTH_HEADER)) {
            try {
                length = Long.parseLong(header.getFirstHeaderValue(HTTPHeader.CONTENT_LENGTH_HEADER));
            } catch (NumberFormatException nfe) {
                throw new IOException(nfe.getMessage());
            }
        }
        StringBuffer headerText = composeHTTPHeader(request, path, header, length, myIsKeepAlive);
        myConnection.sendData(headerText.toString().getBytes(myCharset));
        if (myRequestBody != null && length > 0) {
            myConnection.sendData(myRequestBody);
        } else if (myRequestStream != null && length > 0) {
            myConnection.sendData(myRequestStream, length);
        }
        // if method is "CONNECT", then just return normal status 
        // only if there is nothing to read.
        // this may throw EOFException, then and only then we retry.
        myConnection.readHeader(this);
        // store last time for the next request in case it was keep-alive one.
        myTimeout = computeTimeout(myStatus, getResponseHeader());
        context = context == null ? SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, "{0} of ''{1}''", new Object[] {request, path}) : context; 
        
        // check status.
        if (myStatus.getCode() == HttpURLConnection.HTTP_MOVED_PERM || 
                myStatus.getCode() == HttpURLConnection.HTTP_MOVED_TEMP ||
                myStatus.getCode() == HttpURLConnection.HTTP_FORBIDDEN ||
                myStatus.getCode() == HttpURLConnection.HTTP_UNAUTHORIZED ||
                myStatus.getCode() == HttpURLConnection.HTTP_PROXY_AUTH) {
            // these errors are always processed by the caller, to allow retry.
            myErrorMessage = createDefaultErrorMessage(myConnection.getHost(), path, myStatus, 
                    context.getMessageTemplate(), context.getRelatedObjects());
            myConnection.skipData(this);
            return;
        } 
        
        boolean notExpected = false;        
        if (ok1 >= 0) {
            if (ok1 == 0) {
                ok1 = "PROPFIND".equals(request) ? 207 : 200;
            }
            if (ok2 <= 0) {
                ok2 = ok1;
            }
            notExpected = !(myStatus.getCode() == ok1 || myStatus.getCode() == ok2); 
        } else if ("CONNECT".equalsIgnoreCase(request) && myStatus.getCode() != HttpURLConnection.HTTP_OK) {
            notExpected = true;
        }
        if (notExpected) {
            // unexpected response code.
            myErrorMessage = readError(request, path, context);
        } else if (myStatus.getCode() == HttpURLConnection.HTTP_NO_CONTENT) {
            myConnection.skipData(this);
        } else if (myStatus.getCode() >= 300) {
            SVNErrorMessage error = readError(request, path, context);
            myStatus.setError(error);
        } else if (myResponseStream != null) {
            myErrorMessage = myConnection.readData(this, myResponseStream);
        } else if (myResponseHandler != null) {            
            myErrorMessage = myConnection.readData(this, request, path, myResponseHandler);
        } else {
            if (!"CONNECT".equalsIgnoreCase(request)) {
                myConnection.skipData(this);
            } 
        }
    }
    
    private static long computeTimeout(HTTPStatus status, HTTPHeader header) {
        if (header == null) {
            return -1;
        }
        String keepAlive = header.getFirstHeaderValue("Keep-Alive");
        if (keepAlive == null && status.isHTTP11()) {
            // HTTP/1.1
            // no keep-alive header, consider as infinite timeout.
            return Long.MAX_VALUE;
        } else if (keepAlive == null) {
            // HTTP/1.0
            // keep-alive only if there is Connection: keep-alive header.
            String value = header.getFirstHeaderValue(HTTPHeader.CONNECTION_HEADER);
            if (value != null && value.toLowerCase().indexOf("keep-alive") >= 0) {
                return Long.MAX_VALUE;
            }
            Collection<String> connectionHeaders = header.getHeaderValues(HTTPHeader.CONNECTION_HEADER);
            if (connectionHeaders != null) {
                for (Iterator<String> headers = connectionHeaders.iterator(); headers.hasNext();) {
                    value = (String) headers.next();
                    if (value != null && value.toLowerCase().indexOf("keep-alive") >= 0) {
                        return Long.MAX_VALUE;
                    }
                }
            }
            return -1;
        }
        // HTTP/1.1
        String[] fields = keepAlive.split(",");
        for (int i = 0; i < fields.length; i++) {
            int index = fields[i].indexOf('=');
            if (index < 0) {
                continue;
            }
            String name = fields[i].substring(0, index).trim();
            String value = fields[i].substring(index + 1).trim();
            if ("timeout".equalsIgnoreCase(name)) {
                try {
                    int seconds = Integer.parseInt(value);
                    if (seconds >= 1) {
                        return System.currentTimeMillis() + (seconds - 1)*1000;
                    }
                } catch (NumberFormatException nfe){                    
                }
                // error parsing timeout value, consider no keep-alive.
                return -1;
            }
        }
        return Long.MAX_VALUE;
    }

    private SVNErrorMessage readError(String request, String path, SVNErrorMessage context) {
        String contextMessage = context.getMessageTemplate();
        Object[] contextObjects = context.getRelatedObjects();
        if (myStatus.getCode() == HttpURLConnection.HTTP_NOT_FOUND) {
            contextMessage = "''{0}'' path not found";
            contextObjects = new Object[] {path};
        } 
        SVNErrorMessage error = createDefaultErrorMessage(myConnection.getHost(), path, myStatus, contextMessage, 
                contextObjects);
        SVNErrorMessage davError = myConnection.readError(this, request, path);
        if (davError != null) {
            if (error != null) {
                davError.setChildErrorMessage(error);
            }
            return davError; 
        }
        return error;
    }
    
    public HTTPHeader getResponseHeader() {
        return myResponseHeader;
    }
    
    public long getNextRequestTimeout() {
        return myTimeout;
    }
    
    public HTTPStatus getStatus() {
        return myStatus;
    }
    
    public void setStatus(HTTPStatus status) {
        myStatus = status;
    }
    
    public void setResponseHeader(HTTPHeader header) {
        myResponseHeader = header;
    }
    
    public SVNErrorMessage getErrorMessage() {
        return myErrorMessage;
    }

    private StringBuffer composeHTTPHeader(String request, String path, HTTPHeader header, long length, boolean keepAlive) {
        StringBuffer sb = new StringBuffer();
        sb.append(request);
        sb.append(' ');
        sb.append(composeRequestURI(request, path));
        sb.append(' ');
        sb.append("HTTP/1.1");
        sb.append(HTTPRequest.CRLF);
        sb.append("Host: ");
        sb.append(myConnection.getHost().getHost());
        // only append if URL has port indeed.
        int defaultPort = "https".equals(myConnection.getHost().getProtocol()) ? 443 : 80;
        if (myConnection.getHost().getPort() != defaultPort) {
            sb.append(":");
            sb.append(myConnection.getHost().getPort());
        }
        sb.append(HTTPRequest.CRLF);
        sb.append("User-Agent: ");
        sb.append(Version.getUserAgent());
        sb.append(HTTPRequest.CRLF);
        if (keepAlive) {
            sb.append("Keep-Alive:");
            sb.append(HTTPRequest.CRLF);
            sb.append("Connection: TE, Keep-Alive");
            sb.append(HTTPRequest.CRLF);
        }
        sb.append("TE: trailers");
        sb.append(HTTPRequest.CRLF);
        if (myAuthentication != null) {
            sb.append("Authorization: ");
            sb.append(myAuthentication);
            sb.append(HTTPRequest.CRLF);
        }
        if ((myIsProxyAuthForced || (myIsProxied && !myIsSecured)) && myProxyAuthentication != null) {
            sb.append("Proxy-Authorization: ");
            sb.append(myProxyAuthentication);
            sb.append(HTTPRequest.CRLF);
        }
        if (header == null || !header.hasHeader(HTTPHeader.CONTENT_LENGTH_HEADER)) {
            sb.append("Content-Length: ");
            sb.append(length);
            sb.append(HTTPRequest.CRLF);
        }
        sb.append("Accept-Encoding: gzip");
        sb.append(HTTPRequest.CRLF);
        if (header == null || !header.hasHeader(HTTPHeader.CONTENT_TYPE_HEADER)) {
            sb.append("Content-Type: text/xml; charset=\"utf-8\"");
            sb.append(HTTPRequest.CRLF);
        }
        // append capabilities
        sb.append(HTTPHeader.DAV_HEADER + ": ");
        sb.append(DAVElement.DEPTH_OPTION);
        sb.append(HTTPRequest.CRLF);
        sb.append(HTTPHeader.DAV_HEADER + ": ");
        sb.append(DAVElement.MERGE_INFO_OPTION);
        sb.append(HTTPRequest.CRLF);
        sb.append(HTTPHeader.DAV_HEADER + ": ");
        sb.append(DAVElement.LOG_REVPROPS_OPTION);
        sb.append(HTTPRequest.CRLF);
        if (header != null) {
            sb.append(header.toString());
        }

        if (myCookies != null) {
            for (Iterator<String> cookies = myCookies.iterator(); cookies.hasNext();) {
                String cookie = cookies.next();
                if (cookie != null) {
                    sb.append(HTTPHeader.COOKIE);
                    sb.append(": ");
                    sb.append(cookie);
                    sb.append(HTTPRequest.CRLF);
                }
            }
        }
        sb.append(HTTPRequest.CRLF);
        return sb;
    }
    
    private String composeRequestURI(String request, String path) {
        StringBuffer sb = new StringBuffer();
        if (myIsProxied && !myIsSecured) {
            // prepend path with host name.
            sb.append("http://");
            sb.append(myConnection.getHost().getHost());
            sb.append(":");
            sb.append(myConnection.getHost().getPort());
        }
        if (path == null) {
            path = "/";
        }
        if (!"CONNECT".equals(request) && (path.length() == 0 || path.charAt(0) != '/')) {
            path = "/" + path;
        }
        HTTPParser.getCanonicalPath(path, sb);
        return sb.toString();

    }
    
    public static SVNErrorMessage createDefaultErrorMessage(SVNURL host, String path, HTTPStatus status, String context, 
            Object[] contextObjects) {
        SVNErrorCode errorCode = SVNErrorCode.RA_DAV_REQUEST_FAILED;
        String message = status != null ? status.getCode() + " " + status.getReason() : "";
        if (status != null && status.getCode() == HttpURLConnection.HTTP_FORBIDDEN || status.getCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            errorCode = SVNErrorCode.RA_NOT_AUTHORIZED;
            message = status.getCode() + " " + status.getReason();
        } else if (status != null && status.getCode() == HttpURLConnection.HTTP_NOT_FOUND) {
            errorCode = SVNErrorCode.FS_NOT_FOUND;
        } else if (status != null && (status.getCode() == HttpURLConnection.HTTP_MOVED_PERM || 
                status.getCode() == HttpURLConnection.HTTP_MOVED_TEMP)) {
            final String location = status.getHeader() != null ? status.getHeader().getFirstHeaderValue("Location") : null;
            
            if (location != null) {
                message = status.getCode() == HttpURLConnection.HTTP_MOVED_PERM ? "Repository moved permanently to ''{0}''; please relocate" : 
                    "Repository moved temporarily to ''{0}''; please relocate";
                return SVNErrorMessage.create(SVNErrorCode.RA_DAV_RELOCATED, message, location);
            } else {
                message = status.getCode() == HttpURLConnection.HTTP_MOVED_PERM ? "Repository moved permanently; please relocate" : 
                    "Repository moved temporarily; please relocate";
                return SVNErrorMessage.create(SVNErrorCode.RA_DAV_RELOCATED, message);
            }
        }
        // extend context object to include host:port (empty location).
        Object[] messageObjects = contextObjects == null ? new Object[1] : new Object[contextObjects.length + 1];
        int index = messageObjects.length - 1;
        messageObjects[messageObjects.length - 1] = host;
        if (messageObjects.length > 1) {
            System.arraycopy(contextObjects, 0, messageObjects, 0, contextObjects.length);
        }
        return SVNErrorMessage.create(errorCode, context + ": " + message + " ({" + index + "})", messageObjects);
    }

    public void setKeepAlive(boolean isKeepAlive) {
        myIsKeepAlive = isKeepAlive;
    }

    public void setCookies(Collection<String> cookie) {
        myCookies = cookie;
    }
    
}
