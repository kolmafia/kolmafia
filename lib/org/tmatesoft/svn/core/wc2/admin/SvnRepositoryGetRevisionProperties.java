package org.tmatesoft.svn.core.wc2.admin;

import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

public class SvnRepositoryGetRevisionProperties extends SvnRepositoryOperation<SVNProperties> {
    
    private String transactionName;
        
    public SvnRepositoryGetRevisionProperties(SvnOperationFactory factory) {
        super(factory);
    }
	
	public String getTransactionName() {
		return transactionName;
	}

	public void setTransactionName(String transactionName) {
		this.transactionName = transactionName;
	}
}
