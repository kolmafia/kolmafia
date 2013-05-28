package org.tmatesoft.svn.core.wc2.admin;

import org.tmatesoft.svn.core.wc.admin.SVNAdminEvent;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

public class SvnRepositoryListLocks extends SvnRepositoryReceivingOperation<SVNAdminEvent> {
    
    public SvnRepositoryListLocks(SvnOperationFactory factory) {
        super(factory);
    }

}
