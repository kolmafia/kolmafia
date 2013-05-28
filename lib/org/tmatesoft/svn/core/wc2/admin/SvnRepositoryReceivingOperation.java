package org.tmatesoft.svn.core.wc2.admin;

import java.io.File;

import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnReceivingOperation;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public abstract class SvnRepositoryReceivingOperation<T> extends SvnReceivingOperation<T> {
    
    protected SvnRepositoryReceivingOperation(SvnOperationFactory factory) {
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

}
