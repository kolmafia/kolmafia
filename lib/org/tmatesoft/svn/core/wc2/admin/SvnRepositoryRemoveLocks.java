package org.tmatesoft.svn.core.wc2.admin;

import org.tmatesoft.svn.core.wc.admin.SVNAdminEvent;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

public class SvnRepositoryRemoveLocks extends SvnRepositoryReceivingOperation<SVNAdminEvent> {
    private String[] paths;
    
    public SvnRepositoryRemoveLocks(SvnOperationFactory factory) {
        super(factory);
    }

	public String[] getPaths() {
		return paths;
	}

	public void setPaths(String[] paths) {
		this.paths = paths;
	}
}
