package org.tmatesoft.svn.core.internal.wc2.admin;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.wc.admin.SVNLookClient;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetInfo;


public class SvnRepositoryGetInfoImpl extends SvnRepositoryOperationRunner<SVNLogEntry, SvnRepositoryGetInfo> {

    @Override
    protected SVNLogEntry run() throws SVNException {
        SVNLookClient lc = new SVNLookClient(getOperation().getAuthenticationManager(), getOperation().getOptions());
        lc.setEventHandler(this);
        
        SVNLogEntry entry;
             
        if (getOperation().getTransactionName() == null)
        	entry = lc.doGetInfo(getOperation().getRepositoryRoot(), getOperation().getRevision());
        else
        	entry = lc.doGetInfo(getOperation().getRepositoryRoot(), getOperation().getTransactionName());
        
        return entry;
    }
}
