package org.tmatesoft.svn.core.wc2.admin;

import org.tmatesoft.svn.core.wc.admin.SVNAdminPath;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

public class SvnRepositoryGetHistory extends SvnRepositoryReceivingOperation<SVNAdminPath> {
    
    private String path;
    private boolean includeIDs;
    private long limit;
    
               
    public SvnRepositoryGetHistory(SvnOperationFactory factory) {
        super(factory);
    }

    public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean isIncludeIDs() {
		return includeIDs;
	}

	public void setIncludeIDs(boolean includeIDs) {
		this.includeIDs = includeIDs;
	}

	public long getLimit() {
		return limit;
	}

	public void setLimit(long limit) {
		this.limit = limit;
	}

	

	

		
	

	

	    
}
