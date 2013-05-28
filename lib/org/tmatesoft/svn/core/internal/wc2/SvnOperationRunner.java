package org.tmatesoft.svn.core.internal.wc2;

import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc2.ISvnOperationRunner;
import org.tmatesoft.svn.core.wc2.SvnOperation;
import org.tmatesoft.svn.util.SVNLogType;


public abstract class SvnOperationRunner<V, T extends SvnOperation<V>> implements ISvnOperationRunner<V, T>, ISVNEventHandler {
    private T operation;
    private SVNWCContext wcContext;
    
    public V run(T operation) throws SVNException {
        setOperation(operation);
        return run();
    }
    
    public void reset(SvnWcGeneration wcGeneration) {
        setOperation(null);
        setWcContext(null);
    }
    
    public void setWcContext(SVNWCContext context) {
        this.wcContext = context;
    }
    
    protected SVNWCContext getWcContext() {
        return this.wcContext;
    }

    protected abstract V run() throws SVNException;

    public void setOperation(T operation) {
        this.operation = operation;
    }

    protected T getOperation() {
        return this.operation;
    }
    
    public void checkCancelled() throws SVNCancelException {
        if (getOperation() != null && getOperation().isCancelled()) {
            SVNErrorManager.cancel("Operation cancelled", SVNLogType.WC);
        }
        if (getOperation() != null && getOperation().getCanceller() != null) {
            getOperation().getCanceller().checkCancelled();
        }
    }
    
    public void handleEvent(SVNEvent event, double progress) throws SVNException {
        if (getOperation() != null && getOperation().getEventHandler() != null) {
            getOperation().getEventHandler().handleEvent(event, progress);
        }
    }
    
    protected void handleEvent(SVNEvent event) throws SVNException {
        handleEvent(event, -1);
    }
}