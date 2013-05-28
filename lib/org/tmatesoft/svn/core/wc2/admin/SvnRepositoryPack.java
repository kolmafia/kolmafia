package org.tmatesoft.svn.core.wc2.admin;

import org.tmatesoft.svn.core.wc.admin.SVNAdminEvent;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

public class SvnRepositoryPack extends SvnRepositoryReceivingOperation<SVNAdminEvent> {
    
    public SvnRepositoryPack(SvnOperationFactory factory) {
        super(factory);
    }
}
