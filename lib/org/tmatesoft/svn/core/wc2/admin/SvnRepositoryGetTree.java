package org.tmatesoft.svn.core.wc2.admin;

import org.tmatesoft.svn.core.wc.admin.SVNAdminPath;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

public class SvnRepositoryGetTree extends SvnRepositoryReceivingOperation<SVNAdminPath> {
    
    private String transactionName;
    private String path;
    private boolean includeIDs;
    private boolean recursive;
               
    public SvnRepositoryGetTree(SvnOperationFactory factory) {
        super(factory);
    }

    public String getTransactionName() {
		return transactionName;
	}

	public void setTransactionName(String transactionName) {
		this.transactionName = transactionName;
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

	public boolean isRecursive() {
		return recursive;
	}

	public void setRecursive(boolean recursive) {
		this.recursive = recursive;
	}

	

		
	

	

	    
}
