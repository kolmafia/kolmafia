package org.tmatesoft.svn.core.internal.wc2.admin;

import java.util.Date;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.admin.SVNLookClient;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetDate;


public class SvnRepositoryGetDateImpl extends SvnRepositoryOperationRunner<Date, SvnRepositoryGetDate> {

    @Override
    protected Date run() throws SVNException {
        SVNLookClient lc = new SVNLookClient(getOperation().getAuthenticationManager(), getOperation().getOptions());
        lc.setEventHandler(this);
        
        Date date;
             
        if (getOperation().getTransactionName() == null)
        	date = lc.doGetDate(getOperation().getRepositoryRoot(), getOperation().getRevision());
        else
        	date = lc.doGetDate(getOperation().getRepositoryRoot(), getOperation().getTransactionName());
        
        return date;
    }
}
