package org.tmatesoft.svn.core.internal.wc2.admin;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.admin.SVNAdminClient;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryHotCopy;

public class SvnRepositoryHotCopyImpl extends SvnRepositoryOperationRunner<Long, SvnRepositoryHotCopy> {

    @Override
    protected Long run() throws SVNException {
        SVNAdminClient ac = new SVNAdminClient(getOperation().getAuthenticationManager(), getOperation().getOptions());
        ac.setEventHandler(this);
                
        ac.doHotCopy(getOperation().getSrcRepositoryRoot(), getOperation().getNewRepositoryRoot());
        
        return 1l;
    }

    
}
