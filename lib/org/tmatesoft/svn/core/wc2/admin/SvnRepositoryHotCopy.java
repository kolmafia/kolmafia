package org.tmatesoft.svn.core.wc2.admin;

import java.io.File;

import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

public class SvnRepositoryHotCopy extends SvnRepositoryOperation<Long> {
    
    private File srcRepositoryRoot;

    public SvnRepositoryHotCopy(SvnOperationFactory factory) {
        super(factory);
    }

	public File getSrcRepositoryRoot() {
		return srcRepositoryRoot;
	}

	public void setSrcRepositoryRoot(File srcRepositoryRoot) {
		this.srcRepositoryRoot = srcRepositoryRoot;
	}

	public File getNewRepositoryRoot() {
		return getRepositoryRoot();
	}

	public void setNewRepositoryRoot(File newRepositoryRoot) {
	    setRepositoryRoot(newRepositoryRoot);
	}
}
