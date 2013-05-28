package org.tmatesoft.svn.core.wc2.admin;

import org.tmatesoft.svn.core.SVNLock;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

public class SvnRepositoryGetLock extends SvnRepositoryOperation<SVNLock> {
    
    private String path;
    
    public SvnRepositoryGetLock(SvnOperationFactory factory) {
        super(factory);
    }

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
}
