package org.tmatesoft.svn.core.internal.wc2.admin;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.wc.admin.SVNLookClient;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetRevisionProperties;



public class SvnRepositoryGetRevisionPropertiesImpl extends SvnRepositoryOperationRunner<SVNProperties, SvnRepositoryGetRevisionProperties> {

    @Override
    protected SVNProperties run() throws SVNException {
        SVNLookClient lc = new SVNLookClient(getOperation().getAuthenticationManager(), getOperation().getOptions());
        lc.setEventHandler(this);
        
        SVNProperties props;
             
        if (getOperation().getTransactionName() == null)
        	props = lc.doGetRevisionProperties(getOperation().getRepositoryRoot(), getOperation().getRevision());
        else
        	props = lc.doGetRevisionProperties(getOperation().getRepositoryRoot(), getOperation().getTransactionName());
        
        return props;
    }
}
