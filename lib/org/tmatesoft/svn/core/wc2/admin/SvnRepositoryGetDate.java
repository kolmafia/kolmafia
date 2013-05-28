package org.tmatesoft.svn.core.wc2.admin;

import java.util.Date;

import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

public class SvnRepositoryGetDate extends SvnRepositoryOperation<Date> {
    
    private String transactionName;
    
    public SvnRepositoryGetDate(SvnOperationFactory factory) {
        super(factory);
    }
	
	public String getTransactionName() {
		return transactionName;
	}

	public void setTransactionName(String transactionName) {
		this.transactionName = transactionName;
	}
}
