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

package org.tmatesoft.svn.core.internal.util;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.tmatesoft.svn.core.ISVNCanceller;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNClassLoader;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * <code>SVNSocketFactory</code> is a utility class that represents a custom
 * socket factory which provides creating either a plain socket or a secure one
 * to encrypt data transmitted over network.
 *
 * <p>
 * The created socket then used by the inner engine of <b><i>SVNKit</i></b>
 * library to communicate with a Subversion repository.
 *
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNSocketFactory {

    private static boolean ourIsSocketStaleCheck = false;
    private static int ourSocketReceiveBufferSize = 0; // default
    private static ISVNThreadPool ourThreadPool = SVNClassLoader.getThreadPool(); 
    private static String ourSSLProtocols = System.getProperty("svnkit.http.sslProtocols");
    
    public static Socket createPlainSocket(String host, int port, int connectTimeout, int readTimeout, ISVNCanceller cancel) throws IOException, SVNException {
        InetAddress address = createAddres(host);
        Socket socket = new Socket();
        int bufferSize = getSocketReceiveBufferSize();
        if (bufferSize > 0) {
            socket.setReceiveBufferSize(bufferSize);
        }
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);
        connect(socket, socketAddress, connectTimeout, cancel);
        socket.setReuseAddress(true);
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        socket.setSoLinger(true, 0);
        socket.setSoTimeout(readTimeout);
        return socket;
    }
    
    public static synchronized void setSSLProtocols(String sslProtocols) {
        ourSSLProtocols = sslProtocols;
    }

    public static synchronized String getSSLProtocols() {
        return ourSSLProtocols;
    }

    public static Socket createSSLSocket(KeyManager[] keyManagers, TrustManager trustManager, String host, int port, int connectTimeout, int readTimeout, ISVNCanceller cancel) throws IOException, SVNException {
        InetAddress address = createAddres(host);
        Socket sslSocket = createSSLContext(keyManagers, trustManager).getSocketFactory().createSocket();
        int bufferSize = getSocketReceiveBufferSize();
        if (bufferSize > 0) {
            sslSocket.setReceiveBufferSize(bufferSize);
        }
        sslSocket = setSSLSocketHost(sslSocket, host);
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);
        sslSocket.setReuseAddress(true);
        sslSocket.setTcpNoDelay(true);
        sslSocket.setKeepAlive(true);
        sslSocket.setSoLinger(true, 0);
        sslSocket.setSoTimeout(readTimeout);
        sslSocket = configureSSLSocket(sslSocket);

        connect(sslSocket, socketAddress, connectTimeout, cancel);

        return sslSocket;
    }

    public static Socket createSSLSocket(KeyManager[] keyManagers, TrustManager trustManager, String host, int port, Socket socket, int readTimeout) throws IOException {
        Socket sslSocket = createSSLContext(keyManagers, trustManager).getSocketFactory().createSocket(socket, host, port, true);
        sslSocket = setSSLSocketHost(sslSocket, host);
        sslSocket.setReuseAddress(true);
        sslSocket.setTcpNoDelay(true);
        sslSocket.setKeepAlive(true);
        sslSocket.setSoLinger(true, 0);
        sslSocket.setSoTimeout(readTimeout);
        sslSocket = configureSSLSocket(sslSocket);
        
        return sslSocket;
    }

    private static Socket setSSLSocketHost(Socket sslSocket, String host) {
        try {
            Method m = sslSocket.getClass().getMethod("setHost", String.class);
            if (m != null) {
                m.invoke(sslSocket, host);
                SVNDebugLog.getDefaultLog().logFinest(SVNLogType.NETWORK, "Host set on an SSL socket"); 
            } 
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }
        return sslSocket;
    }

    public static ISVNThreadPool getThreadPool() {
        return ourThreadPool;
    }
    
    public static void connect(Socket socket, InetSocketAddress address, int timeout, ISVNCanceller cancel) throws IOException, SVNException {
        if (cancel == null || cancel == ISVNCanceller.NULL) {
            socket.connect(address, timeout);
            return;
        }

        SVNSocketConnection socketConnection = new SVNSocketConnection(socket, address, timeout);
        ISVNTask task = ourThreadPool.run(socketConnection, true);

        while (!socketConnection.isSocketConnected()) {
            try {
                cancel.checkCancelled();
            } catch (SVNCancelException e) {
                task.cancel(true);
                throw e;
            }
        }
        
        if (socketConnection.getError() != null) {
            throw socketConnection.getError();           
        }
    }

    private static InetAddress createAddres(String hostName) throws UnknownHostException {
        byte[] bytes = new byte[4];
        int index = 0;
        for (StringTokenizer tokens = new StringTokenizer(hostName, "."); tokens.hasMoreTokens();) {
            String token = tokens.nextToken();
            try {
                byte b = (byte) Integer.parseInt(token);
                if (index < bytes.length) {
                    bytes[index] = b;
                    index++;
                } else {
                    bytes = null;
                    break;
                }
            } catch (NumberFormatException e) {
                bytes = null;
                break;
            }
        }
        if (bytes != null && index == 4) {
            return InetAddress.getByAddress(hostName, bytes);
        }
        return InetAddress.getByName(hostName);
    }
    
    public static synchronized void setSocketReceiveBufferSize(int size) {
        ourSocketReceiveBufferSize = size;
    }

    public static synchronized int getSocketReceiveBufferSize() {
        return ourSocketReceiveBufferSize;
    }
    
    public static void setSocketStaleCheckEnabled(boolean enabled) {
        ourIsSocketStaleCheck = enabled;
    }

    public static boolean isSocketStaleCheckEnabled() {
        return ourIsSocketStaleCheck;
    }

    public static boolean isSocketStale(Socket socket) throws IOException {
        if (!isSocketStaleCheckEnabled()) {
            return socket == null || socket.isClosed() || !socket.isConnected();
        }
        
        boolean isStale = true;
        if (socket != null) {
            isStale = false;
            try {
                if (socket.getInputStream().available() == 0) {
                    int timeout = socket.getSoTimeout();
                    try {
                        socket.setSoTimeout(1);
                        socket.getInputStream().mark(1);
                        int byteRead = socket.getInputStream().read();
                        if (byteRead == -1) {
                            isStale = true;
                        } else {
                            socket.getInputStream().reset();
                        }
                    } finally {
                        socket.setSoTimeout(timeout);
                    }
                }
            } catch (InterruptedIOException e) {
                if (!SocketTimeoutException.class.isInstance(e)) {
                    throw e;
                }
            } catch (IOException e) {
                isStale = true;
            }
        }
        return isStale;
    }
    
    private static X509TrustManager EMPTY_TRUST_MANAGER = new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
        public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
        }
        public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
        }
    }; 

    private static KeyManager[] EMPTY_KEY_MANAGERS = new KeyManager[0];

	public static SSLContext createSSLContext(KeyManager[] keyManagers, TrustManager trustManager) throws IOException {
        final TrustManager[] trustManagers = new TrustManager[] {trustManager != null ? trustManager : EMPTY_TRUST_MANAGER};
        keyManagers = keyManagers != null ? keyManagers : EMPTY_KEY_MANAGERS;
        
        try {
            return createSSLContext(keyManagers, trustManagers, getEnabledSSLProtocols(true));
        } catch (NoSuchAlgorithmException e) {
            try {
                return createSSLContext(keyManagers, trustManagers, getEnabledSSLProtocols(false));
            } catch (NoSuchAlgorithmException e1) {
                throw new IOException(e1.getMessage());
            }
        }
	}

    private static SSLContext createSSLContext(KeyManager[] keyManagers, final TrustManager[] trustManagers, final List<String> sslProtocols) throws IOException, NoSuchAlgorithmException {
        SSLContext context = null;
        NoSuchAlgorithmException missingAlgorithm = null;
        for (String sslProtocol : sslProtocols) {
            try {
                context = SSLContext.getInstance(sslProtocol);
                if (context == null) {
                    continue;
                }
                context.init(keyManagers, trustManagers, null);
                return context;
            } catch (NoSuchAlgorithmException e) {
                missingAlgorithm = e;
            } catch (KeyManagementException e) {
                throw new IOException(e.getMessage());
            }
        }
        if (missingAlgorithm != null) {
            throw missingAlgorithm;
        }
        throw new NoSuchAlgorithmException();
    }
	
	private static final List<String> getEnabledSSLProtocols(boolean includeUserDefined) {
	    // in case there is a user-defined list of protocols, 
	    // use it. if all failed or was not provided, use TLS, then SSLv3.
        final String sslProtocols;
        synchronized (SVNSocketFactory.class) {
            sslProtocols = ourSSLProtocols;
        }

        List<String> protocolsToUse = new ArrayList<String>();
	    if (includeUserDefined && sslProtocols != null) {
            for(StringTokenizer tokens = new StringTokenizer(sslProtocols, ","); tokens.hasMoreTokens();) {
                String userProtocol = tokens.nextToken().trim();
                if (!"".equals(userProtocol)) {
                    protocolsToUse.add(userProtocol);
                }
            }
	    }
	    if (protocolsToUse.isEmpty()) {
	        protocolsToUse.add("TLS");
	        protocolsToUse.add("SSLv3");
	    }
	    return protocolsToUse;
	}

    public static Socket configureSSLSocket(Socket socket) {
        if (socket == null || !(socket instanceof SSLSocket)) {
            return null;
        }
        final SSLSocket sslSocket = (SSLSocket) socket;
        // configure enabled protocols enabling those supported.
        final List<String> enabledProtocols = getEnabledSSLProtocols(true);
        final List<String> defaultEnabledProtocols = Arrays.asList(sslSocket.getEnabledProtocols());
        final List<String> supportedProtocols = Arrays.asList(sslSocket.getSupportedProtocols());
        final List<String> protocolsToEnable = new ArrayList<String>();
        
        for (String enabledProtocol : enabledProtocols) {
            for (String supportedProtocol : supportedProtocols) {
                if (supportedProtocol.startsWith(enabledProtocol)) {
                    protocolsToEnable.add(supportedProtocol);
                }
            }
        }

        if (protocolsToEnable.isEmpty()) {
            // fall back to default.
            protocolsToEnable.addAll(defaultEnabledProtocols);
        }
        sslSocket.setEnabledProtocols(protocolsToEnable.toArray(new String[protocolsToEnable.size()]));
        SVNDebugLog.getDefaultLog().logFinest(SVNLogType.NETWORK, "SSL protocols explicitly enabled: " + protocolsToEnable);
        return sslSocket;
    }
}
