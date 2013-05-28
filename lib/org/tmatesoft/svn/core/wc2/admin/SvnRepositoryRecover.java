package org.tmatesoft.svn.core.wc2.admin;

import org.tmatesoft.svn.core.wc.admin.SVNAdminEvent;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

public class SvnRepositoryRecover extends SvnRepositoryReceivingOperation<SVNAdminEvent> {
    
    public SvnRepositoryRecover(SvnOperationFactory factory) {
        super(factory);
    }
}
