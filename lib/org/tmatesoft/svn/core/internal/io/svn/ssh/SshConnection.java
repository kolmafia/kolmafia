package org.tmatesoft.svn.core.internal.io.svn.ssh;

import java.io.IOException;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.Session;

public class SshConnection {
    
    private Connection myConnection;
    private volatile int mySessionCount;
    private SshHost myHost;
    private long myLastAccessTime;
    
    public SshConnection(SshHost host, Connection connection) {
        myHost = host;
        myConnection = connection;
        myLastAccessTime = System.currentTimeMillis();
    }
    
    public SshSession openSession() throws IOException {
        Session session = myConnection.openSession();
        if (session != null) {
            mySessionCount++;
            return new SshSession(this, session);
        }
        return null;
    }

    public void sessionClosed(SshSession sshSession) {
        myHost.lock();
        try {
            myLastAccessTime = System.currentTimeMillis();
            mySessionCount--;
        } finally {
            myHost.unlock();
        }
    }
    
    public int getSessionsCount() {
        return mySessionCount;
    }

    public void close() {
        myConnection.close();
        mySessionCount = 0;
    }

    public long lastAcccessTime() {
        return myLastAccessTime;
    }

}
