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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManagerExt;
import org.tmatesoft.svn.core.auth.ISVNProxyManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
import org.tmatesoft.svn.core.internal.io.dav.handlers.DAVErrorHandler;
import org.tmatesoft.svn.core.internal.util.ChunkedInputStream;
import org.tmatesoft.svn.core.internal.util.FixedSizeInputStream;
import org.tmatesoft.svn.core.internal.util.SVNSSLUtil;
import org.tmatesoft.svn.core.internal.util.SVNSocketFactory;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.wc.IOExceptionWrapper;
import org.tmatesoft.svn.core.internal.wc.SVNCancellableOutputStream;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.util.ISVNDebugLog;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class HTTPConnection implements IHTTPConnection {
    
    private static final DefaultHandler DEFAULT_SAX_HANDLER = new DefaultHandler();
    private static EntityResolver NO_ENTITY_RESOLVER = new EntityResolver() {
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            return new InputSource(new ByteArrayInputStream(new byte[0]));
        }
    };
    
    
    private static final int requestAttempts; 
    private static final int DEFAULT_HTTP_TIMEOUT = 3600*1000;
    
    static {
        String attemptsString = System.getProperty("svnkit.http.requestAttempts", "1" );
        int attempts = 1;
        try {
            attempts = Integer.parseInt(attemptsString);
        } catch (NumberFormatException nfe) {
            attempts = 1;
        }
        if (attempts <= 0) {
            attempts = 1;
        }
        requestAttempts = attempts;
    }

    private static SAXParserFactory ourSAXParserFactory;
    private byte[] myBuffer;
    private SAXParser mySAXParser;
    private SVNURL myHost;
    private OutputStream myOutputStream;
    private InputStream myInputStream;
    private Socket mySocket;
    private SVNRepository myRepository;
    private boolean myIsSecured;
    private boolean myIsProxied;
    private SVNAuthentication myLastValidAuth;
    private HTTPAuthentication myChallengeCredentials;
    private HTTPAuthentication myProxyAuthentication;
    private boolean myIsSpoolResponse;
    private TrustManager myTrustManager;
    private HTTPSSLKeyManager myKeyManager;
    private String myCharset;
    private boolean myIsSpoolAll;
    private File mySpoolDirectory;
    private long myNextRequestTimeout;
    private Collection<String> myCookies;
    private int myRequestCount;
    private HTTPStatus myLastStatus;

    public HTTPConnection(SVNRepository repository, String charset, File spoolDirectory, boolean spoolAll) throws SVNException {
        myRepository = repository;
        myCharset = charset;
        myHost = repository.getLocation().setPath("", false);
        myIsSecured = "https".equalsIgnoreCase(myHost.getProtocol());
        myIsSpoolAll = spoolAll;
        mySpoolDirectory = spoolDirectory;
        myNextRequestTimeout = Long.MAX_VALUE;
    }

    public HTTPStatus getLastStatus() {
        return myLastStatus;
    }
    
    public SVNURL getHost() {
        return myHost;
    }

    private void connect(HTTPSSLKeyManager keyManager, TrustManager trustManager, ISVNProxyManager proxyManager) throws IOException, SVNException {
      SVNURL location = myRepository.getLocation();

	    if (mySocket == null || SVNSocketFactory.isSocketStale(mySocket)) {
            close();
            String host = location.getHost();
            int port = location.getPort();
            
	        ISVNAuthenticationManager authManager = myRepository.getAuthenticationManager();
	        int connectTimeout = authManager != null ? authManager.getConnectTimeout(myRepository) : 0;
            int readTimeout = authManager != null ? authManager.getReadTimeout(myRepository) : DEFAULT_HTTP_TIMEOUT;
            if (readTimeout < 0) {
                readTimeout = DEFAULT_HTTP_TIMEOUT;
            }
            if (proxyManager != null && proxyManager.getProxyHost() != null) {
                final ISVNDebugLog debugLog = myRepository.getDebugLog();
                debugLog.logFine(SVNLogType.NETWORK, "Using proxy " + proxyManager.getProxyHost() + " (secured=" + myIsSecured + ")");
                mySocket = SVNSocketFactory.createPlainSocket(proxyManager.getProxyHost(), proxyManager.getProxyPort(), connectTimeout, readTimeout, myRepository.getCanceller());
                myIsProxied = true;
                if (myIsSecured) {
                    int authAttempts = 0;
                    boolean credentialsUsed = false;
                    while(true) {
                        if (mySocket == null) {
                            mySocket = SVNSocketFactory.createPlainSocket(proxyManager.getProxyHost(), proxyManager.getProxyPort(), connectTimeout, readTimeout, myRepository.getCanceller());
                            debugLog.logFine(SVNLogType.NETWORK, "proxy connection reopened");
                        }
                        HTTPRequest connectRequest = new HTTPRequest(myCharset);
                        connectRequest.setConnection(this);
                        if (myProxyAuthentication != null) {
                            final String authToken = myProxyAuthentication.authenticate();
                            connectRequest.setProxyAuthentication(authToken);
                            debugLog.logFine(SVNLogType.NETWORK, "auth token set: " + authToken);
                        }
                        connectRequest.setForceProxyAuth(true);
                        connectRequest.dispatch("CONNECT", host + ":" + port, null, 0, 0, null);
                        HTTPStatus status = connectRequest.getStatus();
                        
                        if (status.getCode() == HttpURLConnection.HTTP_OK) {
                            myInputStream = null;
                            myOutputStream = null;
                            myProxyAuthentication = null;
                            mySocket = SVNSocketFactory.createSSLSocket(keyManager != null ? new KeyManager[] { keyManager } : new KeyManager[0], trustManager, host, port, mySocket, readTimeout);
                            proxyManager.acknowledgeProxyContext(true, null);
                            return;
                        } else if (status.getCode() == HttpURLConnection.HTTP_PROXY_AUTH) {
                            if (hasToCloseConnection(connectRequest.getResponseHeader())) {
                                close();
                                debugLog.logFine(SVNLogType.NETWORK, "Connection closed as requested by the response header");
                            }
                            authAttempts++;
                            debugLog.logFine(SVNLogType.NETWORK, "authentication attempt #" + authAttempts);
                            Collection<String> proxyAuthHeaders = connectRequest.getResponseHeader().getHeaderValues(HTTPHeader.PROXY_AUTHENTICATE_HEADER);
                            Collection<String> authTypes = Arrays.asList("Basic", "Digest", "Negotiate", "NTLM");
                            
                            debugLog.logFine(SVNLogType.NETWORK, "authentication methods supported: " + authTypes);
                            try {
                                myProxyAuthentication = HTTPAuthentication.parseAuthParameters(proxyAuthHeaders, myProxyAuthentication, myCharset, authTypes, null, myRequestCount); 
                            } catch (SVNException svne) {
                                myRepository.getDebugLog().logFine(SVNLogType.NETWORK, svne);
                                close();
                                throw svne;
                            }
                            debugLog.logFine(SVNLogType.NETWORK, "authentication type chosen: " + myProxyAuthentication.getClass().getSimpleName());
                            connectRequest.initCredentials(myProxyAuthentication, "CONNECT", host + ":" + port);
                            
                            HTTPNTLMAuthentication ntlmProxyAuth = null;
                            HTTPNegotiateAuthentication negotiateProxyAuth = null;
                            if (myProxyAuthentication instanceof HTTPNTLMAuthentication) {
                                ntlmProxyAuth = (HTTPNTLMAuthentication) myProxyAuthentication;
                                if (ntlmProxyAuth.isInType3State()) {
                                    debugLog.logFine(SVNLogType.NETWORK, "continuation of NTLM authentication");
                                    continue;
                                }
                            } else if (myProxyAuthentication instanceof HTTPNegotiateAuthentication) {
                                negotiateProxyAuth = (HTTPNegotiateAuthentication) myProxyAuthentication;
                                if (negotiateProxyAuth.isStarted()) {
                                    debugLog.logFine(SVNLogType.NETWORK, "continuation of Negotiate authentication");
                                    continue;
                                }
                            }
                            
                            if (ntlmProxyAuth != null && authAttempts == 1) {
                                if (!ntlmProxyAuth.allowPropmtForCredentials()) {
                                    continue;
                                }
                            }
                            if (negotiateProxyAuth != null && !negotiateProxyAuth.needsLogin()) {
                                debugLog.logFine(SVNLogType.NETWORK, "Negotiate will use existing credentials");
                                continue;
                            }

                            if (!credentialsUsed) {
                                myProxyAuthentication.setCredentials(new SVNPasswordAuthentication(proxyManager.getProxyUserName(), 
                                        proxyManager.getProxyPassword(), false, myRepository.getLocation(), false));
                                debugLog.logFine(SVNLogType.NETWORK, "explicit credentials set");
                                credentialsUsed = true;
                            } else {
                                debugLog.logFine(SVNLogType.NETWORK, "no more credentials to try");
                                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, "HTTP proxy authorization failed");
                                if (proxyManager != null) {
                                    proxyManager.acknowledgeProxyContext(false, err);
                                }
                                SVNErrorManager.error(err, SVNLogType.NETWORK);
                            }
                        } else {                        
                            SVNURL proxyURL = SVNURL.parseURIEncoded("http://" + proxyManager.getProxyHost() + ":" + proxyManager.getProxyPort());
                            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, "{0} request failed on ''{1}''", new Object[] {"CONNECT", proxyURL});
                            proxyManager.acknowledgeProxyContext(false, err);
                            SVNErrorManager.error(err, connectRequest.getErrorMessage(), SVNLogType.NETWORK);
                        }
                    }
                } else if (proxyManager.getProxyUserName() != null && proxyManager.getProxyPassword() != null ){
                    myProxyAuthentication = new HTTPBasicAuthentication("UTF-8");
                    myProxyAuthentication.setCredentials(new SVNPasswordAuthentication(proxyManager.getProxyUserName(), 
                            proxyManager.getProxyPassword(), false, myRepository.getLocation(), false));
                    debugLog.logFine(SVNLogType.NETWORK, "explicit credentials set");
                }
            } else {
                myIsProxied = false;
                myProxyAuthentication = null;
                mySocket = myIsSecured ? 
                        SVNSocketFactory.createSSLSocket(keyManager != null ? new KeyManager[] { keyManager } : new KeyManager[0], trustManager, host, port, connectTimeout, readTimeout, myRepository.getCanceller()) :
                        SVNSocketFactory.createPlainSocket(host, port, connectTimeout, readTimeout, myRepository.getCanceller());
            }
        }
    }
    
    public void readHeader(HTTPRequest request) throws IOException {
        InputStream is = myRepository.getDebugLog().createLogStream(SVNLogType.NETWORK, getInputStream());
        
        try {            
            // may throw EOF exception.
            HTTPStatus status = HTTPParser.parseStatus(is, myCharset);        
            HTTPHeader header = HTTPHeader.parseHeader(is, myCharset);
            request.setStatus(status);
            request.setResponseHeader(header);
        } catch (ParseException e) {
            // in case of parse exception:
            // try to read remaining and log it.
            String line = HTTPParser.readLine(is, myCharset);
            while(line != null && line.length() > 0) {
                line = HTTPParser.readLine(is, myCharset);
            }
            
            throw new IOException(e.getMessage());
        } finally {
            myRepository.getDebugLog().flushStream(is);
        }
    }
    
    public SVNErrorMessage readError(HTTPRequest request, String method, String path) {
        DAVErrorHandler errorHandler = new DAVErrorHandler();
        try {
            readData(request, method, path, errorHandler);
        } catch (IOException e) {
            return null;
        }
        return errorHandler.getErrorMessage();
    }
    
    public void sendData(byte[] body) throws IOException {
        try {
            getOutputStream().write(body, 0, body.length);
            getOutputStream().flush();
        } finally {
            myRepository.getDebugLog().flushStream(getOutputStream());
        }
    }
    
    public void sendData(InputStream source, long length) throws IOException {
        try {
            byte[] buffer = getBuffer(); 
            while(length > 0) {
                int read = source.read(buffer, 0, (int) Math.min(buffer.length, length));
                if (read > 0) {
                    length -= read;
                    getOutputStream().write(buffer, 0, read);
                } else if (read < 0) {
                    break;
                }
            }
            getOutputStream().flush();
        } finally {
            myRepository.getDebugLog().flushStream(getOutputStream());
        }
    }
    
    public SVNAuthentication getLastValidCredentials() {
        return myLastValidAuth;
    }
    
    public void clearAuthenticationCache() {
        myCookies = null;
        myLastValidAuth = null;
        myTrustManager = null;
        myKeyManager = null;
        myChallengeCredentials = null;
        myProxyAuthentication = null;
        myRequestCount = 0;
    }

    public HTTPStatus request(String method, String path, HTTPHeader header, StringBuffer body, int ok1, int ok2, OutputStream dst, DefaultHandler handler) throws SVNException {
        return request(method, path, header, body, ok1, ok2, dst, handler, null);
    }

    public HTTPStatus request(String method, String path, HTTPHeader header, StringBuffer body, int ok1, int ok2, OutputStream dst, DefaultHandler handler, SVNErrorMessage context) throws SVNException {
        byte[] buffer = null;
        if (body != null) {
            try {
                buffer = body.toString().getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                buffer = body.toString().getBytes();
            }
        } 
        return request(method, path, header, buffer != null ? new ByteArrayInputStream(buffer) : null, ok1, ok2, dst, handler, context);
    }

    public HTTPStatus request(String method, String path, HTTPHeader header, InputStream body, int ok1, int ok2, OutputStream dst, DefaultHandler handler) throws SVNException {
        return request(method, path, header, body, ok1, ok2, dst, handler, null);
    }
    
    public HTTPStatus request(String method, String path, HTTPHeader header, InputStream body, int ok1, int ok2, OutputStream dst, DefaultHandler handler, SVNErrorMessage context) throws SVNException {
        myLastStatus = null;
        myRequestCount++;
        
        if ("".equals(path) || path == null) {
            path = "/";
        }
        
        ISVNAuthenticationManager authManager = myRepository.getAuthenticationManager();
        // 1. prompt for ssl client cert if needed, if cancelled - throw cancellation exception.
        HTTPSSLKeyManager keyManager = myKeyManager == null && authManager != null ? createKeyManager() : myKeyManager;
        TrustManager trustManager = myTrustManager == null && authManager != null ? authManager.getTrustManager(myRepository.getLocation()) : myTrustManager;
        ISVNProxyManager proxyManager = authManager != null ? authManager.getProxyManager(myRepository.getLocation()) : null;

        String sslRealm = composeRealm("");
        SVNAuthentication httpAuth = myLastValidAuth;
        boolean isAuthForced = authManager != null ? authManager.isAuthenticationForced() : false;
        if (httpAuth == null && isAuthForced) {
            httpAuth = authManager.getFirstAuthentication(ISVNAuthenticationManager.PASSWORD, sslRealm, null);
            myChallengeCredentials = new HTTPBasicAuthentication((SVNPasswordAuthentication)httpAuth, myCharset);
        } 
        String realm = null;

        // 2. create request instance.
        HTTPRequest request = new HTTPRequest(myCharset);
        request.setConnection(this);
        request.setKeepAlive(true);
        request.setRequestBody(body);
        request.setResponseHandler(handler);
        request.setResponseStream(dst);
        
        SVNErrorMessage err = null;
        boolean ntlmAuthIsRequired = false;
        boolean ntlmProxyAuthIsRequired = false;
        boolean negoAuthIsRequired = false;
        int authAttempts = 0;
        while (true) {
            if (myNextRequestTimeout < 0 || System.currentTimeMillis() >= myNextRequestTimeout) {
                SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "Keep-Alive timeout detected");
                close();
                if (isClearCredentialsOnClose(myChallengeCredentials)) {
                    httpAuth = null;
                }
            }
            int retryCount = 1;
            try {
                err = null;
                String httpAuthResponse = null;
                String proxyAuthResponse = null;
                while(retryCount >= 0) {
                    connect(keyManager, trustManager, proxyManager);
                    request.reset();
                    request.setProxied(myIsProxied);
                    request.setSecured(myIsSecured);                    
                    if (myProxyAuthentication != null && (ntlmProxyAuthIsRequired || !"NTLM".equals(myProxyAuthentication.getAuthenticationScheme()))) {
                        if (proxyAuthResponse == null) {
                            request.initCredentials(myProxyAuthentication, method, path);
                            proxyAuthResponse = myProxyAuthentication.authenticate();
                        }
                        request.setProxyAuthentication(proxyAuthResponse);
                    }
                    
                    if (myChallengeCredentials != null && (ntlmAuthIsRequired || negoAuthIsRequired || ((!"NTLM".equals(myChallengeCredentials.getAuthenticationScheme())) && !"Negotiate".equals(myChallengeCredentials.getAuthenticationScheme())) && 
                            httpAuth != null)) {
                        if (httpAuthResponse == null) {
                            request.initCredentials(myChallengeCredentials, method, path);
                            httpAuthResponse = myChallengeCredentials.authenticate();
                        }
                        request.setAuthentication(httpAuthResponse);
                    }

                    if (myCookies != null && !myCookies.isEmpty()) {
                        request.setCookies(myCookies);
                    }
                    try {                        
                        request.dispatch(method, path, header, ok1, ok2, context);
                        break;
                    } catch (EOFException pe) {
                        // retry, EOF always means closed connection.
                        if (retryCount > 0) {
                            close();
                            continue;
                        }
                        throw (IOException) new IOException(pe.getMessage()).initCause(pe);
                    } finally {
                        retryCount--;
                    }
                }
                if (request.getResponseHeader().hasHeader(HTTPHeader.SET_COOKIE)) {
                    myCookies = request.getResponseHeader().getHeaderValues(HTTPHeader.COOKIE);
                }
                myNextRequestTimeout = request.getNextRequestTimeout();
                myLastStatus = request.getStatus();
            } catch (SSLHandshakeException ssl) {
                myRepository.getDebugLog().logFine(SVNLogType.NETWORK, ssl);
                close();
	            if (ssl.getCause() instanceof SVNSSLUtil.CertificateNotTrustedException) {
		            SVNErrorManager.cancel(ssl.getCause().getMessage(), SVNLogType.NETWORK);
	            }
                SVNErrorMessage sslErr = SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, "SSL handshake failed: ''{0}''", new Object[] { ssl.getMessage() }, SVNErrorMessage.TYPE_ERROR, ssl);
		            if (keyManager != null) {
			            keyManager.acknowledgeAndClearAuthentication(sslErr);
		            }
                err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, ssl);
	            continue;
            } catch (IOException e) {
                myRepository.getDebugLog().logFine(SVNLogType.NETWORK, e);
                if (e instanceof SocketTimeoutException) {
	                err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, 
	                        "timed out waiting for server", null, SVNErrorMessage.TYPE_ERROR, e);
                } else if (e instanceof UnknownHostException) {
	                err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, 
	                        "unknown host", null, SVNErrorMessage.TYPE_ERROR, e);
                } else if (e instanceof ConnectException) {
	                err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, 
	                        "connection refused by the server", null, 
	                        SVNErrorMessage.TYPE_ERROR, e);
                } else if (e instanceof SVNCancellableOutputStream.IOCancelException) {
                    SVNErrorManager.cancel(e.getMessage(), SVNLogType.NETWORK);
                } else if (e instanceof SSLException) {                   
                    err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, e);
                } else {
                    err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, e);
                }
            } catch (SVNException e) {
                myRepository.getDebugLog().logFine(SVNLogType.NETWORK, e);
                // force connection close on SVNException 
                // (could be thrown by user's auth manager methods).
                close();
                throw e;
            } finally {
                finishResponse(request);                
            }

            if (err != null) {
                if (proxyManager != null) {
                    proxyManager.acknowledgeProxyContext(false, err);
                }

                close();
                break;
            }

            if (proxyManager != null) {
                proxyManager.acknowledgeProxyContext(true, err);
            }

            if (keyManager != null) {
	            myKeyManager = keyManager;
	            myTrustManager = trustManager;
	            keyManager.acknowledgeAndClearAuthentication(null);
            }

            if (myLastStatus.getCode() == HttpURLConnection.HTTP_FORBIDDEN) {
                if (httpAuth != null && authManager != null) {
                    BasicAuthenticationManager.acknowledgeAuthentication(false, ISVNAuthenticationManager.PASSWORD, realm, request.getErrorMessage(), httpAuth, myRepository.getLocation(), authManager);
                }
                myLastValidAuth = null;
                close();
                err = request.getErrorMessage();
            } else if (myIsProxied && myLastStatus.getCode() == HttpURLConnection.HTTP_PROXY_AUTH) {
                Collection<String> proxyAuthHeaders = request.getResponseHeader().getHeaderValues(HTTPHeader.PROXY_AUTHENTICATE_HEADER);
                Collection<String> authTypes = null;
                if (authManager != null && authManager instanceof DefaultSVNAuthenticationManager) {
                    DefaultSVNAuthenticationManager defaultAuthManager = (DefaultSVNAuthenticationManager) authManager;
                    authTypes = defaultAuthManager.getAuthTypes(myRepository.getLocation());
                }
                try {
                    myProxyAuthentication = HTTPAuthentication.parseAuthParameters(proxyAuthHeaders, myProxyAuthentication, myCharset, authTypes, null, myRequestCount); 
                } catch (SVNException svne) {
                    myRepository.getDebugLog().logFine(SVNLogType.NETWORK, svne);
                    err = svne.getErrorMessage(); 
                    break;
                }

                if (myProxyAuthentication instanceof HTTPNTLMAuthentication) {
                    ntlmProxyAuthIsRequired = true;
                    HTTPNTLMAuthentication ntlmProxyAuth = (HTTPNTLMAuthentication) myProxyAuthentication;
                    if (ntlmProxyAuth.isInType3State()) {
                        continue;
                    }
                } 

                err = SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, "HTTP proxy authorization failed");
                if (proxyManager != null) {
                    proxyManager.acknowledgeProxyContext(false, err);
                }
                close();

                break;
            } else if (myLastStatus.getCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                authAttempts++;//how many times did we try?
                
                Collection<String> authHeaderValues = request.getResponseHeader().getHeaderValues(HTTPHeader.AUTHENTICATE_HEADER);
                if (authHeaderValues == null || authHeaderValues.size() == 0) {
                    err = request.getErrorMessage();
                    myLastStatus.setError(SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED, err.getMessageTemplate(), err.getRelatedObjects()));
                    if ("LOCK".equalsIgnoreCase(method)) {
                        myLastStatus.getError().setChildErrorMessage(SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE,
                                "Probably you are trying to lock file in repository that only allows anonymous access"));
                    }
                    SVNErrorManager.error(myLastStatus.getError(), SVNLogType.NETWORK);
                    return myLastStatus;
                }

                //we should work around a situation when a server
                //does not support Basic authentication while we're 
                //forcing it, credentials should not be immediately
                //thrown away
                boolean skip = false;
                isAuthForced = authManager != null ? authManager.isAuthenticationForced() : false;
                if (isAuthForced) {
                    if (httpAuth != null && myChallengeCredentials != null && !HTTPAuthentication.isSchemeSupportedByServer(myChallengeCredentials.getAuthenticationScheme(), authHeaderValues)) {
                        skip = true;
                    }
                }
                
                Collection<String> authTypes = null;
                if (authManager != null && authManager instanceof DefaultSVNAuthenticationManager) {
                    DefaultSVNAuthenticationManager defaultAuthManager = (DefaultSVNAuthenticationManager) authManager;
                    authTypes = defaultAuthManager.getAuthTypes(myRepository.getLocation());
                }
                
                try {
                    myChallengeCredentials = HTTPAuthentication.parseAuthParameters(authHeaderValues, myChallengeCredentials, myCharset, authTypes, authManager, myRequestCount); 
                } catch (SVNException svne) {
                    err = svne.getErrorMessage(); 
                    break;
                }

                myChallengeCredentials.setChallengeParameter("method", method);
                myChallengeCredentials.setChallengeParameter("uri", HTTPParser.getCanonicalPath(path, null).toString());
                
                if (skip) {
                    close();
                    continue;
                }
                
                HTTPNTLMAuthentication ntlmAuth = null;
                HTTPNegotiateAuthentication negoAuth = null;
                if (myChallengeCredentials instanceof HTTPNTLMAuthentication) {
                    ntlmAuthIsRequired = true;
                    ntlmAuth = (HTTPNTLMAuthentication)myChallengeCredentials;
                    if (ntlmAuth.isInType3State()) {
                        continue;
                    }
                } else if (myChallengeCredentials instanceof HTTPDigestAuthentication) {
                    // continue (retry once) if previous request was acceppted?
                    if (myLastValidAuth != null) {
                        myLastValidAuth = null;
                        continue;
                    }
                } else if (myChallengeCredentials instanceof HTTPNegotiateAuthentication) {
                    negoAuthIsRequired = true;
                    negoAuth = (HTTPNegotiateAuthentication)myChallengeCredentials;
                    if (negoAuth.isStarted()) {
                        continue;
                    }
                }

                myLastValidAuth = null;

                if (ntlmAuth != null && authAttempts == 1) {
                    /*
                     * if this is the first time we get HTTP_UNAUTHORIZED, NTLM is the target auth scheme
                     * and JNA is available, we should try a native auth mechanism first without calling 
                     * auth providers. 
                     */                
                    if (!ntlmAuth.allowPropmtForCredentials()) {
                        continue;
                    }
               }

                if (negoAuth != null && !negoAuth.needsLogin()) {
                    continue;
                }

                if (authManager == null) {
                    err = request.getErrorMessage();
                    break;
                }

                realm = myChallengeCredentials.getChallengeParameter("realm");
                realm = realm == null ? "" : " " + realm;
                realm = composeRealm(realm); 
                
                if (httpAuth == null) {
                    httpAuth = authManager.getFirstAuthentication(ISVNAuthenticationManager.PASSWORD, realm, myRepository.getLocation());
                } else if (authAttempts >= requestAttempts) {
                    BasicAuthenticationManager.acknowledgeAuthentication(false, ISVNAuthenticationManager.PASSWORD, realm, request.getErrorMessage(), httpAuth, myRepository.getLocation(), authManager);
                    httpAuth = authManager.getNextAuthentication(ISVNAuthenticationManager.PASSWORD, realm, myRepository.getLocation());
                }
                
                if (httpAuth == null) {
                    err = SVNErrorMessage.create(SVNErrorCode.CANCELLED, new SVNCancelException(SVNErrorMessage.create(SVNErrorCode.CANCELLED, "ISVNAuthentication provider did not provide credentials; HTTP authorization cancelled.")));
                    break;
                } 
                if (httpAuth != null) {
                    myChallengeCredentials.setCredentials((SVNPasswordAuthentication) httpAuth);
                }
                continue;
            } else if (myLastStatus.getCode() == HttpURLConnection.HTTP_MOVED_PERM || myLastStatus.getCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
                String newLocation = request.getResponseHeader().getFirstHeaderValue(HTTPHeader.LOCATION_HEADER);
                if (newLocation == null) {
                    err = request.getErrorMessage();
                    break;
                }
                int hostIndex = newLocation.indexOf("://");
                if (hostIndex > 0) {
                    hostIndex += 3;
                    hostIndex = newLocation.indexOf("/", hostIndex);
                }
                if (hostIndex > 0 && hostIndex < newLocation.length()) {
                    String newPath = newLocation.substring(hostIndex);
                    if (newPath.endsWith("/") &&
                            !newPath.endsWith("//") && !path.endsWith("/") &&
                            newPath.substring(0, newPath.length() - 1).equals(path)) {
                        path += "//";
                        continue;
                    }
                }
                err = request.getErrorMessage();
                close();
            } else if (request.getErrorMessage() != null) {
                err = request.getErrorMessage();
            } else {
                ntlmProxyAuthIsRequired = false;
                ntlmAuthIsRequired = false;
                negoAuthIsRequired = false;
            }
            
            if (err != null) {
                break;
            }
            
            if (myIsProxied) {
                if (proxyManager != null) {
                    proxyManager.acknowledgeProxyContext(true, null);
                }
            }
            
            if (httpAuth != null && realm != null && authManager != null) {
                BasicAuthenticationManager.acknowledgeAuthentication(true, ISVNAuthenticationManager.PASSWORD, realm, null, httpAuth, myRepository.getLocation(), authManager);
            }
	        if (trustManager != null && authManager != null) {
		        authManager.acknowledgeTrustManager(trustManager);
	        }
            
            if (httpAuth != null) {
                myLastValidAuth = httpAuth;
            }

            if (authManager instanceof ISVNAuthenticationManagerExt) {
                ((ISVNAuthenticationManagerExt)authManager).acknowledgeConnectionSuccessful(myRepository.getLocation());
            }

            myLastStatus.setHeader(request.getResponseHeader());
            return myLastStatus;
        }
        // force close on error that was not processed before.
        // these are errors that has no relation to http status (processing error or cancellation).
        close();
        if (err != null && err.getErrorCode().getCategory() != SVNErrorCode.RA_DAV_CATEGORY &&
            err.getErrorCode() != SVNErrorCode.UNSUPPORTED_FEATURE) {
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        // err2 is another default context...
        myRepository.getDebugLog().logFine(SVNLogType.NETWORK, new Exception(err.getMessage()));
        SVNErrorMessage err2 = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, "{0} request failed on ''{1}''", new Object[] {method, path}, err.getType(), err.getCause());
        SVNErrorManager.error(err, err2, SVNLogType.NETWORK);
        return null;
    }

    private String composeRealm(String realm) {
        final StringBuffer buffer = new StringBuffer();
        buffer.append("<");
        buffer.append(myHost.getProtocol());
        buffer.append("://");
        if (myHost.getUserInfo() != null && !"".equals(myHost.getUserInfo().trim())) {
            buffer.append(myHost.getUserInfo());
            buffer.append("@");
        }
        buffer.append(myHost.getHost());
        buffer.append(":");
        buffer.append(myHost.getPort());
        buffer.append(">");
        if (realm != null) {
            buffer.append(realm);
        }
        return buffer.toString();
    }

    private boolean isClearCredentialsOnClose(HTTPAuthentication auth) {
        return !(auth instanceof HTTPBasicAuthentication || auth instanceof HTTPDigestAuthentication 
                || auth instanceof HTTPNegotiateAuthentication);
    }

	private HTTPSSLKeyManager createKeyManager() {
		if (!myIsSecured) {
			return null;
		}

		SVNURL location = myRepository.getLocation();
		ISVNAuthenticationManager authManager = myRepository.getAuthenticationManager();
		String sslRealm = "<" + location.getProtocol() + "://" + location.getHost() + ":" + location.getPort() + ">";
		return new HTTPSSLKeyManager(authManager, sslRealm, location);
	}

	public SVNErrorMessage readData(HTTPRequest request, OutputStream dst) throws IOException {
        InputStream stream = createInputStream(request.getResponseHeader(), getInputStream());
        byte[] buffer = getBuffer();
        boolean willCloseConnection = false;
        try {
            while (true) {
                int count = stream.read(buffer);
                if (count < 0) {
                    break;
                }
                if (dst != null) {
                    dst.write(buffer, 0, count);
                }
            }
        } catch (IOException e) {
            willCloseConnection = true;
            if (e instanceof IOExceptionWrapper) {
                IOExceptionWrapper wrappedException = (IOExceptionWrapper) e; 
                return wrappedException.getOriginalException().getErrorMessage();
            }
            if (e.getCause() instanceof SVNException) {
                return ((SVNException) e.getCause()).getErrorMessage();
            }
            throw e;
        } finally {
            if (!willCloseConnection) {
                SVNFileUtil.closeFile(stream);
            }
            myRepository.getDebugLog().flushStream(stream);
        }
        return null;
    }
    
    public SVNErrorMessage readData(HTTPRequest request, String method, String path, DefaultHandler handler) throws IOException {
        InputStream is = null; 
        SpoolFile tmpFile = null; 
        SVNErrorMessage err = null;
        try {
            if (myIsSpoolResponse || myIsSpoolAll) {
                OutputStream dst = null;
                try {
                    tmpFile = new SpoolFile(mySpoolDirectory);
                    dst = tmpFile.openForWriting();
                    dst = new SVNCancellableOutputStream(dst, myRepository.getCanceller());
                    // this will exhaust http stream anyway.
                    err = readData(request, dst);
                    SVNFileUtil.closeFile(dst);
                    dst = null;
                    if (err != null) {
                        return err;
                    }
                    // this stream always have to be closed.
                    is = tmpFile.openForReading();
                } finally {
                    SVNFileUtil.closeFile(dst);
                }
            } else {
                is = createInputStream(request.getResponseHeader(), getInputStream());
            }
            // this will not close is stream.
            err = readData(is, method, path, handler);
        } catch (IOException e) {
            throw e;
        } finally {
            if (myIsSpoolResponse || myIsSpoolAll) {
                // always close spooled stream.
                SVNFileUtil.closeFile(is);
            } else if (err == null && !hasToCloseConnection(request.getResponseHeader())) {
                // exhaust stream if connection is not about to be closed.
                SVNFileUtil.closeFile(is);
            }
            if (tmpFile != null) {
                try {
                    tmpFile.delete();
                } catch (SVNException e) {
                    throw new IOException(e.getMessage());
                }
            }
            myIsSpoolResponse = false;
        }
        return err;
    }

    private SVNErrorMessage readData(InputStream is, String method, String path, DefaultHandler handler) throws FactoryConfigurationError, UnsupportedEncodingException, IOException {
        try {
            if (mySAXParser == null) {
                mySAXParser = getSAXParserFactory().newSAXParser();
            }
            XMLReader reader = new XMLReader(is);
            while (!reader.isClosed()) {
                org.xml.sax.XMLReader xmlReader = mySAXParser.getXMLReader();
                xmlReader.setContentHandler(handler);
                xmlReader.setDTDHandler(handler);
                xmlReader.setErrorHandler(handler);
                xmlReader.setEntityResolver(NO_ENTITY_RESOLVER);
                xmlReader.parse(new InputSource(reader));
            }
        } catch (SAXException e) {
            mySAXParser = null;
            if (e instanceof SAXParseException) {
                if (handler instanceof DAVErrorHandler) {
                    // failed to read svn-specific error, return null.
                    return null;
                }
            } else if (e.getException() instanceof SVNException) {
                return ((SVNException) e.getException()).getErrorMessage();
            } else if (e.getCause() instanceof SVNException) {
                return ((SVNException) e.getCause()).getErrorMessage();
            } 
            return SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, "Processing {0} request response failed: {1} ({2}) ",  new Object[] {method, e.getMessage(), path});
        } catch (ParserConfigurationException e) {
            mySAXParser = null;
            return SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, "XML parser configuration error while processing {0} request response: {1} ({2}) ",  new Object[] {method, e.getMessage(), path});
        } catch (EOFException e) {
            // skip it.
        } finally {
            if (mySAXParser != null) {
                // to avoid memory leaks when connection is cached.
                org.xml.sax.XMLReader xmlReader = null;
                try {
                    xmlReader = mySAXParser.getXMLReader();
                } catch (SAXException e) {
                }
                if (xmlReader != null) {
                    xmlReader.setContentHandler(DEFAULT_SAX_HANDLER);
                    xmlReader.setDTDHandler(DEFAULT_SAX_HANDLER);
                    xmlReader.setErrorHandler(DEFAULT_SAX_HANDLER);
                    xmlReader.setEntityResolver(NO_ENTITY_RESOLVER);
                }
            }
            myRepository.getDebugLog().flushStream(is);
        }
        return null;
    }
    
    public void skipData(HTTPRequest request) throws IOException {
        if (hasToCloseConnection(request.getResponseHeader())) {
            return;
        }
        InputStream is = createInputStream(request.getResponseHeader(), getInputStream());
        while(is.skip(2048) > 0);        
    }

    public void close() {
        if (isClearCredentialsOnClose(myChallengeCredentials)) {
            clearAuthenticationCache();
        }
        if (mySocket != null) {
            if (myInputStream != null) {
                try {
                    myInputStream.close();
                } catch (IOException e) {}
            }
            if (myOutputStream != null) {
                try {
                    myOutputStream.flush();
                } catch (IOException e) {}
            }
            if (myOutputStream != null) {
                try {
                    myOutputStream.close();
                } catch (IOException e) {}
            }
            try {
                mySocket.close();
            } catch (IOException e) {}
            mySocket = null;
            myOutputStream = null;
            myInputStream = null;
        }
    }

    private byte[] getBuffer() {
        if (myBuffer == null) {
            myBuffer = new byte[32*1024];
        }
        return myBuffer;
    }

    private InputStream getInputStream() throws IOException {
        if (myInputStream == null) {
            if (mySocket == null) {
                return null;
            }
//            myInputStream = new CancellableSocketInputStream(new BufferedInputStream(mySocket.getInputStream(), 2048), myRepository.getCanceller());
            myInputStream = new BufferedInputStream(mySocket.getInputStream(), 2048);

        }
        return myInputStream;
    }

    private OutputStream getOutputStream() throws IOException {
        SVNDebugLog.getDefaultLog().logFine(SVNLogType.DEFAULT, "socket output stream requested...");
        if (myOutputStream == null) {
            if (mySocket == null) {
                return null;
            }
            myOutputStream = new BufferedOutputStream(mySocket.getOutputStream(), 2048);
            myOutputStream = myRepository.getDebugLog().createLogStream(SVNLogType.NETWORK, myOutputStream);
        }
        return myOutputStream;
    }

    private void finishResponse(HTTPRequest request) {
        if (myOutputStream != null) {
            try {
                myOutputStream.flush();
            } catch (IOException ex) {
            }
        }
        HTTPHeader header = request != null ? request.getResponseHeader() : null;
        if (hasToCloseConnection(header)) {
            close();
        }
    }
    
    private static boolean hasToCloseConnection(HTTPHeader header) {
        if (header == null) {
            return true;
        }
        
        String connectionHeader = header.getFirstHeaderValue(HTTPHeader.CONNECTION_HEADER);
        String proxyHeader = header.getFirstHeaderValue(HTTPHeader.PROXY_CONNECTION_HEADER);
        
        if (connectionHeader != null && connectionHeader.toLowerCase().indexOf("close") >= 0) {
            return true;
        } else if (proxyHeader != null && proxyHeader.toLowerCase().indexOf("close") >= 0) {
            return true;
        } 
        return false;
    }
    
    private InputStream createInputStream(HTTPHeader readHeader, InputStream is) throws IOException {
        if ("chunked".equalsIgnoreCase(readHeader.getFirstHeaderValue(HTTPHeader.TRANSFER_ENCODING_HEADER))) {
            is = new ChunkedInputStream(is, myCharset);
        } else if (readHeader.getFirstHeaderValue(HTTPHeader.CONTENT_LENGTH_HEADER) != null) { 
            String lengthStr = readHeader.getFirstHeaderValue(HTTPHeader.CONTENT_LENGTH_HEADER);
            long length = 0;
            try {
                length = Long.parseLong(lengthStr);
            } catch (NumberFormatException nfe) {
                length = 0;
            }
            is = new FixedSizeInputStream(is, length);
        } else if (!hasToCloseConnection(readHeader)) {
            // no content length and no valid transfer-encoding!
            // consider as empty response.

            // but only when there is no "Connection: close" or "Proxy-Connection: close" header,
            // in that case just return "is". 
            // skipData will not read that as it should also analyze "close" instruction.
            
            // return empty stream. 
            // and force connection close? (not to read garbage on the next request).
            is = new FixedSizeInputStream(is, 0);
            // this will force connection to close.
            readHeader.setHeaderValue(HTTPHeader.CONNECTION_HEADER, "close");
        } 
        
        if ("gzip".equals(readHeader.getFirstHeaderValue(HTTPHeader.CONTENT_ENCODING_HEADER))) {
            is = new GZIPInputStream(is);
        }
        return myRepository.getDebugLog().createLogStream(SVNLogType.NETWORK, is);
    }

    private static synchronized SAXParserFactory getSAXParserFactory() throws FactoryConfigurationError {
        if (ourSAXParserFactory == null) {
            ourSAXParserFactory = createSAXParserFactory();
            Map<String, Object> supportedFeatures = new HashMap<String, Object>();
            try {
                ourSAXParserFactory.setFeature("http://xml.org/sax/features/namespaces", true);
                supportedFeatures.put("http://xml.org/sax/features/namespaces", Boolean.TRUE);
            } catch (SAXNotRecognizedException e) {
            } catch (SAXNotSupportedException e) {
            } catch (ParserConfigurationException e) {
            }
            try {
                ourSAXParserFactory.setFeature("http://xml.org/sax/features/validation", false);
                supportedFeatures.put("http://xml.org/sax/features/validation", Boolean.FALSE);
            } catch (SAXNotRecognizedException e) {
            } catch (SAXNotSupportedException e) {
            } catch (ParserConfigurationException e) {
            }
            try {
                ourSAXParserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                supportedFeatures.put("http://apache.org/xml/features/nonvalidating/load-external-dtd", Boolean.FALSE);
            } catch (SAXNotRecognizedException e) {
            } catch (SAXNotSupportedException e) {
            } catch (ParserConfigurationException e) {
            }
            if (supportedFeatures.size() < 3) {
                ourSAXParserFactory = createSAXParserFactory();
                for (Iterator<String> names = supportedFeatures.keySet().iterator(); names.hasNext();) {
                    String name = names.next();
                    try {
                        ourSAXParserFactory.setFeature(name, supportedFeatures.get(name) == Boolean.TRUE);
                    } catch (SAXNotRecognizedException e) {
                    } catch (SAXNotSupportedException e) {
                    } catch (ParserConfigurationException e) {
                    }
                }
            }
            ourSAXParserFactory.setNamespaceAware(true);
            ourSAXParserFactory.setValidating(false);
        }
        return ourSAXParserFactory;
    }
    
    public static SAXParserFactory createSAXParserFactory() {
        String legacy = System.getProperty("svnkit.sax.useDefault");
        if (legacy == null || !Boolean.valueOf(legacy).booleanValue()) {
            return SAXParserFactory.newInstance();
        }
        // instantiate JVM parser.
        String[] parsers = {"com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl", // 1.5, 1.6
                            "org.apache.crimson.jaxp.SAXParserFactoryImpl", // 1.4
                            };
        for (int i = 0; i < parsers.length; i++) {
            String className = parsers[i];
            ClassLoader loader = HTTPConnection.class.getClassLoader();
            try {
                Class<?> clazz = null;
                if (loader != null) {
                    clazz = loader.loadClass(className);
                } else {
                    clazz = Class.forName(className);
                }
                if (clazz != null) {
                    Object factory = clazz.newInstance();
                    if (factory instanceof SAXParserFactory) {
                        return (SAXParserFactory) factory;
                    }
                }
            } catch (ClassNotFoundException e) {
            } catch (InstantiationException e) {
            } catch (IllegalAccessException e) {
            }
        }
        return SAXParserFactory.newInstance();
    }

    public void setSpoolResponse(boolean spoolResponse) {
        myIsSpoolResponse = spoolResponse;
    }

}
