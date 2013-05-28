package org.tmatesoft.svn.core.internal.wc2.admin;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.wc.admin.SVNLookClient;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetRevisionProperty;


public class SvnRepositoryGetRevisionPropertyImpl extends SvnRepositoryOperationRunner<SVNPropertyValue, SvnRepositoryGetRevisionProperty> {

    @Override
    protected SVNPropertyValue run() throws SVNException {
        SVNLookClient lc = new SVNLookClient(getOperation().getAuthenticationManager(), getOperation().getOptions());
        lc.setEventHandler(this);
        
        SVNPropertyValue propvalue;
             
        if (getOperation().getTransactionName() == null)
        	propvalue = lc.doGetRevisionProperty(getOperation().getRepositoryRoot(), getOperation().getPropName(), getOperation().getRevision());
        else
        	propvalue = lc.doGetRevisionProperty(getOperation().getRepositoryRoot(), getOperation().getPropName(), getOperation().getTransactionName());
        
        return propvalue;
    }
}
