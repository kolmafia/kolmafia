package org.tmatesoft.svn.core.wc2.admin;

import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

public class SvnRepositoryGetRevisionProperty extends SvnRepositoryOperation<SVNPropertyValue> {
    
    private String transactionName;
    private String propName;
        
    public SvnRepositoryGetRevisionProperty(SvnOperationFactory factory) {
        super(factory);
    }
	
	public String getTransactionName() {
		return transactionName;
	}

	public void setTransactionName(String transactionName) {
		this.transactionName = transactionName;
	}

	public String getPropName() {
		return propName;
	}

	public void setPropName(String propName) {
		this.propName = propName;
	}
}
