package org.tmatesoft.svn.core.internal.wc2.admin;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.admin.SVNLookClient;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetLog;


public class SvnRepositoryGetLogImpl extends SvnRepositoryOperationRunner<String, SvnRepositoryGetLog> {

    @Override
    protected String run() throws SVNException {
        SVNLookClient lc = new SVNLookClient(getOperation().getAuthenticationManager(), getOperation().getOptions());
        lc.setEventHandler(this);
        
        String log;
             
        if (getOperation().getTransactionName() == null)
        	log = lc.doGetLog(getOperation().getRepositoryRoot(), getOperation().getRevision());
        else
        	log = lc.doGetLog(getOperation().getRepositoryRoot(), getOperation().getTransactionName());
        
        return log;
    }
}
