package org.tmatesoft.svn.core.internal.wc2.admin;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.admin.SVNAdminClient;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositorySyncInfo;

public class SvnRepositorySyncInfoImpl extends SvnRepositoryOperationRunner<Long, SvnRepositorySyncInfo> {

    @Override
    protected Long run() throws SVNException {
        SVNAdminClient ac = new SVNAdminClient(getOperation().getAuthenticationManager(), getOperation().getOptions());
        ac.setEventHandler(this);
                
        ac.doInfo(getOperation().getToUrl());
        
        return 1l;
    }
}
