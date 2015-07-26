package org.tmatesoft.svn.core.wc2.admin;

import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

public class SvnRepositoryGetFileSize extends SvnRepositoryOperation<Long> {

    private String transactionName;
    private String path;

    public SvnRepositoryGetFileSize(SvnOperationFactory factory) {
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
