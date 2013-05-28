package org.tmatesoft.svn.core.wc2.admin;

import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

public class SvnRepositoryGetInfo extends SvnRepositoryOperation<SVNLogEntry> {
    
    private String transactionName;
    
    public SvnRepositoryGetInfo(SvnOperationFactory factory) {
        super(factory);
    }
	
	public String getTransactionName() {
		return transactionName;
	}

	public void setTransactionName(String transactionName) {
		this.transactionName = transactionName;
	}
}
