package org.tmatesoft.svn.core.internal.io.svn.ssh;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.InteractiveCallback;
import com.trilead.ssh2.ServerHostKeyVerifier;

public class SshHost {
    
    private static final long CONNECTION_INACTIVITY_TIMEOUT = 60*1000*10; // 10 minutes
    private static final long MAX_CONCURRENT_OPENERS = 3;
    private static final int MAX_SESSIONS_PER_CONNECTION = 8;
    
    private String myHost;
    private int myPort;
    private ServerHostKeyVerifier myHostVerifier;
    
    private char[] myPrivateKey;
    private char[] myPassphrase;
    private char[] myPassword;
    private String myUserName;
    
    private int myConnectTimeout;
    private boolean myIsLocked;
    private boolean myIsDisposed;
    
    private List<SshConnection> myConnections;
    private Object myOpenerLock = new Object();
    private int myOpenersCount;
    private int myReadTimeout;
    
    public SshHost(String host, int port) {
        myConnections = new LinkedList<SshConnection>();
        myHost = host;
        myPort = port;
    }
    
    public void setHostVerifier(ServerHostKeyVerifier verifier) {
        myHostVerifier = verifier;
    }
    
    public void setConnectionTimeout(int timeout) {
        myConnectTimeout = timeout;
    }

    public void setReadTimeout(int readTimeout) {
        myReadTimeout = readTimeout;
    }

    public void setCredentials(String userName, char[] key, char[] passphrase, char[] password) {
        myUserName = userName;
        myPrivateKey = key;
        myPassphrase = passphrase;
        myPassword = password;
    }
    
    public boolean purge() {
        try {
            lock();
            int size = myConnections.size();
            long time = System.currentTimeMillis();
            for (Iterator<SshConnection> connections = myConnections.iterator(); connections.hasNext();) {
                SshConnection connection = connections.next();
                if (connection.getSessionsCount() == 0) {
                    if (myConnections.size() == 1) {
                        long timeout = time - connection.lastAcccessTime();
                        if (timeout >= CONNECTION_INACTIVITY_TIMEOUT) {
                            connection.close();
                            connections.remove();
                        } 
                    } else {
                        connection.close();
                        connections.remove();
                    }
                }
            }
            if (myConnections.size() == 0 && size > 0) {
                setDisposed(true);
            }
            return isDisposed();
        } finally {
            unlock();
        }
        
    }
    
    public boolean isDisposed() {
        return myIsDisposed;
    }
    
    public void setDisposed(boolean disposed) {
        myIsDisposed = disposed;
        if (disposed) {
            for (SshConnection connection : myConnections) {
                connection.close();
            }
            myConnections.clear();
        }
    }
    
    public String getKey() {
        String key = myUserName + ":" + myHost + ":" + myPort;
        if (myPrivateKey != null) {
            key += ":" + new String(myPrivateKey);
        }
        if (myPassphrase != null) {
            key += ":" + new String(myPassphrase);
        }
        if (myPassword != null) {
            key += ":" + new String(myPassword);
        }
        return key;
    }
    
    void lock() {
        synchronized (myConnections) {
            while(myIsLocked) {
                try {
                    myConnections.wait();
                } catch (InterruptedException e) {
                }
            }
            myIsLocked = true;
        }
    }
    
    void unlock() {
        synchronized (myConnections) {
            myIsLocked = false;
            myConnections.notifyAll();
        }
    }
    
    public SshSession openSession() throws IOException {
        SshSession session = useExistingConnection();
        if (session != null) {
            return session;
        }        
        SshConnection newConnection = null;
        addOpener();
        try {
            session = useExistingConnection();
            if (session != null) {
                return session;
            }
            newConnection = openConnection();
        } finally {
            removeOpener();
        }
        
        if (newConnection != null) {
            lock();
            try {
                if (isDisposed()) {
                    newConnection.close();
                    throw new SshHostDisposedException();
                }                
                myConnections.add(newConnection);
                return newConnection.openSession();
            } finally {
                unlock();
            }
        }
        throw new IOException("Cannot establish SSH connection with " + myHost + ":" + myPort);
    }

    private SshSession useExistingConnection() throws IOException {
        lock();
        try {
            if (isDisposed()) {
                throw new SshHostDisposedException();
            }
            for (Iterator<SshConnection> connections = myConnections.iterator(); connections.hasNext();) {
                final SshConnection connection = connections.next();

                if (connection.getSessionsCount() < MAX_SESSIONS_PER_CONNECTION) {
                    try {
                        return connection.openSession();
                    } catch (IOException e) {
                        // this connection has been closed by server.
                        if (e.getMessage() != null && e.getMessage().contains("connection is closed")) {
                            connection.close();
                            connections.remove();
                        } else {
                            throw e;
                        }
                    }
                }
            }
        } finally {
            unlock();
        }
        return null;
    }

    
    private void removeOpener() {
        synchronized (myOpenerLock) {
            myOpenersCount--;
            myOpenerLock.notifyAll();
        }
    }

    private void addOpener() {
        synchronized(myOpenerLock) {
            while(myOpenersCount >= MAX_CONCURRENT_OPENERS) {
                try {
                    myOpenerLock.wait();
                } catch (InterruptedException e) {
                }
            }
            myOpenersCount++;
        }
    }

    private SshConnection openConnection() throws IOException {
        Connection connection = new Connection(myHost, myPort);
        connection.connect(new ServerHostKeyVerifier() {
            public boolean verifyServerHostKey(String hostname, int port, String serverHostKeyAlgorithm, byte[] serverHostKey) throws Exception {
                if (myHostVerifier != null) {
                    myHostVerifier.verifyServerHostKey(hostname, port, serverHostKeyAlgorithm, serverHostKey);
                }                    
                return true;
            }
        }, myConnectTimeout, myReadTimeout, myConnectTimeout);
        
        boolean authenticated = false;        
        
        final String password = myPassword != null ? new String(myPassword) : null;
        final String passphrase = myPassphrase != null ? new String(myPassphrase) : null;
        
        if (myPrivateKey != null) {
            authenticated = connection.authenticateWithPublicKey(myUserName, myPrivateKey, passphrase);
        } else if (myPassword != null) {
            String[] methods = connection.getRemainingAuthMethods(myUserName);
            authenticated = false;
            for (int i = 0; i < methods.length; i++) {
                if ("password".equals(methods[i])) {
                    authenticated = connection.authenticateWithPassword(myUserName, password);                    
                } else if ("keyboard-interactive".equals(methods[i])) {
                    authenticated = connection.authenticateWithKeyboardInteractive(myUserName, new InteractiveCallback() {
                        public String[] replyToChallenge(String name, String instruction, int numPrompts, String[] prompt, boolean[] echo) throws Exception {
                            String[] reply = new String[numPrompts];
                            for (int i = 0; i < reply.length; i++) {
                                reply[i] = password;
                            }
                            return reply;
                        }
                    });
                }
                if (authenticated) {
                    break;
                }
            }
        } else {
            connection.close();
            throw new SshAuthenticationException("No supported authentication methods left.");
        }
        if (!authenticated) {
            connection.close();
            throw new SshAuthenticationException("Credentials rejected by SSH server.");
        }
        return new SshConnection(this, connection);
    }

    public String toString() {
        return myUserName + "@" + myHost + ":" + myPort + ":" + myConnections.size();
    }

}
