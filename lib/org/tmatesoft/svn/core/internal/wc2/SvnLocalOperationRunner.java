package org.tmatesoft.svn.core.internal.wc2;

import java.io.File;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.wc2.SvnOperation;

public abstract class SvnLocalOperationRunner<V, T extends SvnOperation<V>> extends SvnOperationRunner<V, T> {
    
    protected SvnLocalOperationRunner() {
    }

    public boolean isApplicable(T operation, SvnWcGeneration wcGeneration) throws SVNException {
        return wcGeneration != null && operation.hasLocalTargets();
    }
    
    protected File getFirstTarget() {
        return getOperation().getFirstTarget() != null ? getOperation().getFirstTarget().getFile() : null;
    }

    protected void sleepForTimestamp() {
        if (getOperation().isSleepForTimestamp()) {
            SVNFileUtil.sleepForTimestamp();
        }
    }

}