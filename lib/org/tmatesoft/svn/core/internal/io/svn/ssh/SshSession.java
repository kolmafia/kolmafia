package org.tmatesoft.svn.core.internal.io.svn.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.trilead.ssh2.ChannelCondition;
import com.trilead.ssh2.Session;

public class SshSession {
    
    private SshConnection myOwner;
    private Session mySession;

    public SshSession(SshConnection owner, Session session) {
        mySession = session;
        myOwner = owner;
    }
    
    public void close() {
        mySession.close();
        waitForCondition(ChannelCondition.CLOSED, 0);        
        myOwner.sessionClosed(this);
    }    
    
    public InputStream getOut() {
        return mySession.getStdout();
    }

    public InputStream getErr() {
        return mySession.getStderr();        
    }
    
    public OutputStream getIn() {
        return mySession.getStdin();
    }
    
    public Integer getExitCode() {
        return mySession.getExitStatus();
    }
    
    public String getExitSignal() {
        return mySession.getExitSignal();
    }
    
    public void waitForCondition(int code, long timeout) {
        mySession.waitForCondition(code, timeout);
    }
    
    public void execCommand(String command) throws IOException {
        mySession.execCommand(command);
    }
    
    public void ping() throws IOException {
        mySession.ping();
    }
}
