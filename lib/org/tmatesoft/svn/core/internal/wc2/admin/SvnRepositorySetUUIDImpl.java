package org.tmatesoft.svn.core.internal.wc2.admin;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.admin.SVNAdminClient;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositorySetUUID;


public class SvnRepositorySetUUIDImpl extends SvnRepositoryOperationRunner<Long, SvnRepositorySetUUID> {

    @Override
    protected Long run() throws SVNException {
        SVNAdminClient ac = new SVNAdminClient(getOperation().getAuthenticationManager(), getOperation().getOptions());
        ac.setEventHandler(this);
                
        ac.doSetUUID(getOperation().getRepositoryRoot(), getOperation().getUUID());
        
        return 1l;
    }
}
