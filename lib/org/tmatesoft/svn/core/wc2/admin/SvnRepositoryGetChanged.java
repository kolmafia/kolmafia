package org.tmatesoft.svn.core.wc2.admin;

import org.tmatesoft.svn.core.wc.admin.SVNChangeEntry;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

public class SvnRepositoryGetChanged extends SvnRepositoryReceivingOperation<SVNChangeEntry> {

    private String transactionName;
    private boolean includeCopyInfo;
            
    public SvnRepositoryGetChanged(SvnOperationFactory factory) {
        super(factory);
    }

	public String getTransactionName() {
		return transactionName;
	}

	public void setTransactionName(String transactionName) {
		this.transactionName = transactionName;
	}

	public boolean isIncludeCopyInfo() {
		return includeCopyInfo;
	}

	public void setIncludeCopyInfo(boolean includeCopyInfo) {
		this.includeCopyInfo = includeCopyInfo;
	}
}
