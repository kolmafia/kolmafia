package org.tmatesoft.svn.core.internal.wc2;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgRepositoryAccess;
import org.tmatesoft.svn.core.internal.wc2.old.SvnOldRepositoryAccess;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.SvnOperation;

public abstract class SvnRemoteOperationRunner<V, T extends SvnOperation<V>> extends SvnOperationRunner<V, T> {
    
    private SvnRepositoryAccess repositoryAccess;
    private SvnWcGeneration detectedWcGeneration;
    
    public void reset(SvnWcGeneration wcGeneration) {
        super.reset(wcGeneration);
        repositoryAccess = null;
        detectedWcGeneration = wcGeneration;
    }

    public boolean isApplicable(T operation, SvnWcGeneration wcGeneration) throws SVNException {
        return operation.hasRemoteTargets() || !isRevisionLocalToWc(operation.getRevision());
    }
    
    protected SvnRepositoryAccess getRepositoryAccess() throws SVNException {
        if (repositoryAccess == null) {
            if (getDetectedWcGeneration() == SvnWcGeneration.V16) {
                repositoryAccess = new SvnOldRepositoryAccess(getOperation());
            } else {
                repositoryAccess = new SvnNgRepositoryAccess(getOperation(), getWcContext());
            }            
        }
        return repositoryAccess;
    }
    
    private SvnWcGeneration getDetectedWcGeneration() {
        return detectedWcGeneration;
    }

    protected boolean isRevisionLocalToWc(SVNRevision revision) {
        return revision == SVNRevision.BASE || revision == SVNRevision.WORKING || revision == SVNRevision.COMMITTED;
    }

    public SvnWcGeneration getWcGeneration() {
        return SvnWcGeneration.NOT_DETECTED;
    }
}
