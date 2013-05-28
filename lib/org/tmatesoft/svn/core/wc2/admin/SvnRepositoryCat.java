package org.tmatesoft.svn.core.wc2.admin;

import java.io.OutputStream;

import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

public class SvnRepositoryCat extends SvnRepositoryOperation<Long> {
    
    private String transactionName;
    private String path;
    private OutputStream outputStream;
        
    public SvnRepositoryCat(SvnOperationFactory factory) {
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

	public OutputStream getOutputStream() {
		return outputStream;
	}

	public void setOutputStream(OutputStream outputStream) {
		this.outputStream = outputStream;
	}
}
