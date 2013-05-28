package org.tmatesoft.svn.core.internal.wc2.admin;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.wc.admin.SVNLookClient;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetProperty;


public class SvnRepositoryGetPropertyImpl extends SvnRepositoryOperationRunner<SVNPropertyValue, SvnRepositoryGetProperty> {

    @Override
    protected SVNPropertyValue run() throws SVNException {
        SVNLookClient lc = new SVNLookClient(getOperation().getAuthenticationManager(), getOperation().getOptions());
        lc.setEventHandler(this);
        
        SVNPropertyValue propvalue;
             
        if (getOperation().getTransactionName() == null)
        	propvalue = lc.doGetProperty(getOperation().getRepositoryRoot(), getOperation().getPropName(), getOperation().getPath(), getOperation().getRevision());
        else
        	propvalue = lc.doGetProperty(getOperation().getRepositoryRoot(), getOperation().getPropName(), getOperation().getPath(), getOperation().getTransactionName());
        
        return propvalue;
    }
}
