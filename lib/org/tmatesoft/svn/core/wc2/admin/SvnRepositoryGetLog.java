package org.tmatesoft.svn.core.wc2.admin;

import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

public class SvnRepositoryGetLog extends SvnRepositoryOperation<String> {
    
    private String transactionName;
    
    public SvnRepositoryGetLog(SvnOperationFactory factory) {
        super(factory);
    }
	
	public String getTransactionName() {
		return transactionName;
	}

	public void setTransactionName(String transactionName) {
		this.transactionName = transactionName;
	}
}
