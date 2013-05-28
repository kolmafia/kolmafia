package org.tmatesoft.svn.core.wc2.admin;

import java.io.File;

import org.tmatesoft.svn.core.wc2.SvnOperation;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public abstract class SvnRepositoryOperation<V> extends SvnOperation<V> {
    
    protected SvnRepositoryOperation(SvnOperationFactory factory) {
        super(factory);
    }

    private File repositoryRoot;

    public void setRepositoryRoot(File repositoryRoot) {
        this.repositoryRoot = repositoryRoot;
        setSingleTarget(repositoryRoot != null ? SvnTarget.fromFile(repositoryRoot) : null);
    }
    
    public File getRepositoryRoot() {
        return this.repositoryRoot;
    }

    /**
     * Gets whether the operation changes working copy
     * @return <code>true</code> if the operation changes the working copy, otherwise <code>false</code>
     */
    @Override
    public boolean isChangesWorkingCopy() {
        return false;
    }
}
