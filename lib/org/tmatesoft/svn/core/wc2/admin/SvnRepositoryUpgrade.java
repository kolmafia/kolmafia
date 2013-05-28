package org.tmatesoft.svn.core.wc2.admin;

import org.tmatesoft.svn.core.wc.admin.SVNAdminEvent;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

public class SvnRepositoryUpgrade extends SvnRepositoryReceivingOperation<SVNAdminEvent> {
    
    public SvnRepositoryUpgrade(SvnOperationFactory factory) {
        super(factory);
    }
}
