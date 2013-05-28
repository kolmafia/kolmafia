package org.tmatesoft.svn.core.internal.wc2.admin;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.admin.SVNLookClient;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetUUID;


public class SvnRepositoryGetUUIDImpl extends SvnRepositoryOperationRunner<String, SvnRepositoryGetUUID> {

    @Override
    protected String run() throws SVNException {
        SVNLookClient lc = new SVNLookClient(getOperation().getAuthenticationManager(), getOperation().getOptions());
        lc.setEventHandler(this);
        
        return lc.doGetUUID(getOperation().getRepositoryRoot());
    }
}
