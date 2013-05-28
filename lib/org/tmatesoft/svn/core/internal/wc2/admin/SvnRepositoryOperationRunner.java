package org.tmatesoft.svn.core.internal.wc2.admin;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldRunner;
import org.tmatesoft.svn.core.wc2.SvnOperation;

public abstract class SvnRepositoryOperationRunner<V,T extends SvnOperation<V>> extends SvnOldRunner<V, T> {

    public boolean isApplicable(T operation, SvnWcGeneration wcGeneration) throws SVNException {
        return true;
    }

    public SvnWcGeneration getWcGeneration() {
        return SvnWcGeneration.NOT_DETECTED;
    }

}
