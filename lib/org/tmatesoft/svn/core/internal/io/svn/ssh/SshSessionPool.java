package org.tmatesoft.svn.core.internal.io.svn.ssh;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

import com.trilead.ssh2.ServerHostKeyVerifier;

public class SshSessionPool {
    
    private static final long PURGE_INTERVAL = 10*1000;
    
    private Map<String, SshHost> myPool;
    private Timer myTimer;
    
    public SshSessionPool() {
        myPool = new HashMap<String, SshHost>();
        myTimer = new Timer(true);
        
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (myPool) {
                    Collection<SshHost> hosts = new ArrayList<SshHost>(myPool.values());
                    for (SshHost host : hosts) {
                        if (host.purge()) {
                            myPool.remove(host.getKey());
                        }
                        SVNDebugLog.getDefaultLog().logFinest(SVNLogType.NETWORK, "SSH pool, purged: " + host);
                    }
                }
            }
        }, PURGE_INTERVAL, PURGE_INTERVAL);
        
    }
    
    public void shutdown() {
        synchronized (myPool) {
            Collection<SshHost> hosts = new ArrayList<SshHost>(myPool.values());
            for (SshHost host : hosts) {
                try {
                    host.lock();
                    host.setDisposed(true);
                    
                    myPool.remove(host.getKey());
                } finally {
                    host.unlock();
                }
            }
        }
    }
    
    public SshSession openSession(String host, int port, String userName,
            char[] privateKey, char[] passphrase, char[] password,
            ServerHostKeyVerifier verifier, int connectTimeout, int readTimeout) throws IOException {

        final SshHost newHost = new SshHost(host, port);
        newHost.setCredentials(userName, privateKey, passphrase, password);
        newHost.setConnectionTimeout(connectTimeout);
        newHost.setHostVerifier(verifier);
        newHost.setReadTimeout(readTimeout);
        
        SshSession session = null;
        final String hostKey = newHost.getKey();

        while(session == null) {
            SshHost sshHost;
            synchronized (myPool) {
                sshHost = myPool.get(hostKey);
                if (sshHost == null) {
                    sshHost = newHost;
                    myPool.put(hostKey, newHost);
                }
            }
            
            try {
                session = sshHost.openSession();
            } catch (SshHostDisposedException e) {
                // host has been removed from the pool.
                synchronized (myPool) {
                  myPool.remove(hostKey);
                }
                continue;
            }
            break;
        }
        
        return session;
    }

}
