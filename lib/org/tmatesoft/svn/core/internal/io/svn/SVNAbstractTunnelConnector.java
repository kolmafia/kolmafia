package org.tmatesoft.svn.core.internal.io.svn;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNLogType;

import com.trilead.ssh2.StreamGobbler;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public abstract class SVNAbstractTunnelConnector implements ISVNConnector {

    private OutputStream myOutputStream;
    private InputStream myInputStream;
    private Process myProcess;

    public void open(SVNRepositoryImpl repository, String process) throws SVNException {
        // 4. launch process.
        try {
            myProcess = Runtime.getRuntime().exec(process);
            myInputStream = new BufferedInputStream(myProcess.getInputStream());
            myOutputStream = new BufferedOutputStream(myProcess.getOutputStream());
            new StreamGobbler(myProcess.getErrorStream());
        } catch (IOException e) {
            try {
                close(repository);
            } catch (SVNException inner) {
            }
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.EXTERNAL_PROGRAM, "Cannot create tunnel: ''{0}''", e.getMessage());
            SVNErrorManager.error(err, e, SVNLogType.NETWORK);
        }
    }

    public InputStream getInputStream() throws IOException {
        return myInputStream;
    }

    public OutputStream getOutputStream() throws IOException {
        return myOutputStream;
    }

    public boolean isConnected(SVNRepositoryImpl repos) throws SVNException {
        return myInputStream != null;
    }
    
    public boolean isStale() {
        return false;
    }

    public void close(SVNRepositoryImpl repository) throws SVNException {
        if (myProcess != null) {
            if (myInputStream != null) {
                repository.getDebugLog().flushStream(myInputStream);
                SVNFileUtil.closeFile(myInputStream);
            }
            if (myOutputStream != null) {
                repository.getDebugLog().flushStream(myOutputStream);
                SVNFileUtil.closeFile(myOutputStream);
            }
            myProcess.destroy();
            myInputStream = null;
            myOutputStream = null;
            myProcess = null;
        }
    }

    public void free() {
    }

    public boolean occupy() {
        return true;
    }

}
