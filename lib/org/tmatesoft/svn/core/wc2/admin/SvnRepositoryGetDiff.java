package org.tmatesoft.svn.core.wc2.admin;

import java.io.OutputStream;

import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

public class SvnRepositoryGetDiff extends SvnRepositoryOperation<Long> {
    
    private String transactionName;
    private boolean diffDeleted;
    private boolean diffAdded;
    private boolean diffCopyFrom; 
    private OutputStream outputStream;
        
    public SvnRepositoryGetDiff(SvnOperationFactory factory) {
        super(factory);
    }

    public String getTransactionName() {
		return transactionName;
	}

	public void setTransactionName(String transactionName) {
		this.transactionName = transactionName;
	}

	public boolean isDiffDeleted() {
		return diffDeleted;
	}

	public void setDiffDeleted(boolean diffDeleted) {
		this.diffDeleted = diffDeleted;
	}

	public boolean isDiffAdded() {
		return diffAdded;
	}

	public void setDiffAdded(boolean diffAdded) {
		this.diffAdded = diffAdded;
	}

	public boolean isDiffCopyFrom() {
		return diffCopyFrom;
	}

	public void setDiffCopyFrom(boolean diffCopyFrom) {
		this.diffCopyFrom = diffCopyFrom;
	}

	public OutputStream getOutputStream() {
		return outputStream;
	}

	public void setOutputStream(OutputStream outputStream) {
		this.outputStream = outputStream;
	}
}
