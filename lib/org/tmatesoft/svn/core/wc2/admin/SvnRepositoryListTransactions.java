package org.tmatesoft.svn.core.wc2.admin;

import org.tmatesoft.svn.core.wc.admin.SVNAdminEvent;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

public class SvnRepositoryListTransactions extends SvnRepositoryReceivingOperation<SVNAdminEvent> {
    
    public SvnRepositoryListTransactions(SvnOperationFactory factory) {
        super(factory);
    }
}
