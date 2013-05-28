package org.tmatesoft.svn.core.internal.wc2.admin;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.admin.SVNLookClient;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetYoungest;


public class SvnRepositoryGetYoungestImpl extends SvnRepositoryOperationRunner<Long, SvnRepositoryGetYoungest> {

    @Override
    protected Long run() throws SVNException {
        SVNLookClient lc = new SVNLookClient(getOperation().getAuthenticationManager(), getOperation().getOptions());
        lc.setEventHandler(this);
        
        return lc.doGetYoungestRevision(getOperation().getRepositoryRoot());
    }
}
