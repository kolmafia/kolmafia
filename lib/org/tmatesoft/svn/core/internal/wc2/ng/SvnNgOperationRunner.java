package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.util.Collection;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNCommitter17;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc2.SvnLocalOperationRunner;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.wc2.ISvnCommitParameters;
import org.tmatesoft.svn.core.wc2.SvnOperation;

public abstract class SvnNgOperationRunner<V, T extends SvnOperation<V>> extends SvnLocalOperationRunner<V, T> {
    
    private SvnNgRepositoryAccess repositoryAccess;
    
    protected V run() throws SVNException {
        return run(getWcContext());
    }
    
    protected boolean matchesChangelist(File target) {
        return getWcContext().matchesChangelist(target, getOperation().getApplicableChangelists());
    }
    
    protected SvnNgRepositoryAccess getRepositoryAccess() throws SVNException {
        if (repositoryAccess == null) {
            repositoryAccess = new SvnNgRepositoryAccess(getOperation(), getWcContext());
        }
        return repositoryAccess;
    }
    
    protected void setRepositoryAccess(SvnNgRepositoryAccess repositoryAccess) {
        this.repositoryAccess = repositoryAccess;
    }
    
    protected abstract V run(SVNWCContext context) throws SVNException;

    @Override
    public void reset(SvnWcGeneration wcGeneration) {
        super.reset(wcGeneration);
        repositoryAccess = null;
    }

    public SvnWcGeneration getWcGeneration() {
        return SvnWcGeneration.V17;
    }

    protected void deleteDeleteFiles(SVNCommitter17 committer, ISvnCommitParameters parameters) {
        if (parameters == null) {
            return;
        }
        
        Collection<File> deletedPaths = committer.getDeletedPaths();
        for (File deletedPath : deletedPaths) {
            boolean delete = false;
            if (deletedPath.isFile()) {
                delete = parameters.onFileDeletion(deletedPath);
            } else if (deletedPath.isDirectory()) {
                delete = parameters.onDirectoryDeletion(deletedPath);
            }
            if (delete) {
                SVNFileUtil.deleteAll(deletedPath, true);
            }
        }
    }
    
    

}
