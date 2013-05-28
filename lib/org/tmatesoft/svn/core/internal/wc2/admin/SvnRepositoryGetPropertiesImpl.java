package org.tmatesoft.svn.core.internal.wc2.admin;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.wc.admin.SVNLookClient;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetProperties;



public class SvnRepositoryGetPropertiesImpl extends SvnRepositoryOperationRunner<SVNProperties, SvnRepositoryGetProperties> {

    @Override
    protected SVNProperties run() throws SVNException {
        SVNLookClient lc = new SVNLookClient(getOperation().getAuthenticationManager(), getOperation().getOptions());
        lc.setEventHandler(this);
        
        SVNProperties props;
             
        if (getOperation().getTransactionName() == null)
        	props = lc.doGetProperties(getOperation().getRepositoryRoot(), getOperation().getPath(), getOperation().getRevision());
        else
        	props = lc.doGetProperties(getOperation().getRepositoryRoot(), getOperation().getPath(), getOperation().getTransactionName());
        
        return props;
    }
}
