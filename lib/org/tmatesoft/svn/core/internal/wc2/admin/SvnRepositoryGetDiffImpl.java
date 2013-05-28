package org.tmatesoft.svn.core.internal.wc2.admin;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.admin.SVNLookClient;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetDiff;


public class SvnRepositoryGetDiffImpl extends SvnRepositoryOperationRunner<Long, SvnRepositoryGetDiff> {

    @Override
    protected Long run() throws SVNException {
        SVNLookClient lc = new SVNLookClient(getOperation().getAuthenticationManager(), getOperation().getOptions());
        lc.setEventHandler(this);
        
        if (getOperation().getTransactionName() == null)
        	lc.doGetDiff(
        			getOperation().getRepositoryRoot(), 
        			getOperation().getRevision(), 
        			getOperation().isDiffDeleted(), 
        			getOperation().isDiffAdded(), 
        			getOperation().isDiffCopyFrom(), 
        			getOperation().getOutputStream());
        else
        	lc.doGetDiff(
        			getOperation().getRepositoryRoot(), 
        			getOperation().getTransactionName(), 
        			getOperation().isDiffDeleted(), 
        			getOperation().isDiffAdded(), 
        			getOperation().isDiffCopyFrom(), 
        			getOperation().getOutputStream());
        
        return 1l;
    }
}
