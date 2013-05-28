package org.tmatesoft.svn.core.wc2.admin;

import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

public class SvnRepositoryGetProperties extends SvnRepositoryOperation<SVNProperties> {
    
    private String transactionName;
    private String path;
    
    public SvnRepositoryGetProperties(SvnOperationFactory factory) {
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
}
