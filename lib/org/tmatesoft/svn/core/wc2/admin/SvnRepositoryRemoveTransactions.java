package org.tmatesoft.svn.core.wc2.admin;

import org.tmatesoft.svn.core.wc.admin.SVNAdminEvent;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

public class SvnRepositoryRemoveTransactions extends SvnRepositoryReceivingOperation<SVNAdminEvent> {
    
    private String[] transactions;
    
    public SvnRepositoryRemoveTransactions(SvnOperationFactory factory) {
        super(factory);
    }

    public String[] getTransactions() {
		return transactions;
	}

	public void setTransactions(String[] transactions) {
		this.transactions = transactions;
	}
}
