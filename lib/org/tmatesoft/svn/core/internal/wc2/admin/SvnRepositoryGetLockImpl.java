package org.tmatesoft.svn.core.internal.wc2.admin;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLock;
import org.tmatesoft.svn.core.wc.admin.SVNLookClient;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetLock;


public class SvnRepositoryGetLockImpl extends SvnRepositoryOperationRunner<SVNLock, SvnRepositoryGetLock> {

    @Override
    protected SVNLock run() throws SVNException {
        SVNLookClient lc = new SVNLookClient(getOperation().getAuthenticationManager(), getOperation().getOptions());
        lc.setEventHandler(this);
        
        return lc.doGetLock(getOperation().getRepositoryRoot(), getOperation().getPath());
    }
}
