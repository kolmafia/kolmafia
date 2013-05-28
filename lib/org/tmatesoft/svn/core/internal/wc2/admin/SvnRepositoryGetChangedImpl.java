package org.tmatesoft.svn.core.internal.wc2.admin;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.admin.ISVNChangeEntryHandler;
import org.tmatesoft.svn.core.wc.admin.SVNChangeEntry;
import org.tmatesoft.svn.core.wc.admin.SVNLookClient;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetChanged;


public class SvnRepositoryGetChangedImpl extends SvnRepositoryOperationRunner<SVNChangeEntry, SvnRepositoryGetChanged> implements ISVNChangeEntryHandler {

    @Override
    protected SVNChangeEntry run() throws SVNException {
        SVNLookClient lc = new SVNLookClient(getOperation().getAuthenticationManager(), getOperation().getOptions());
        lc.setEventHandler(this);
        
        if (getOperation().getTransactionName() == null)
        	lc.doGetChanged(getOperation().getRepositoryRoot(), getOperation().getRevision(), this, getOperation().isIncludeCopyInfo());
        else
        	lc.doGetChanged(getOperation().getRepositoryRoot(), getOperation().getTransactionName(), this, getOperation().isIncludeCopyInfo());
        
        return getOperation().first();
    }
    
    public void handleEntry(SVNChangeEntry entry) throws SVNException {
    	getOperation().receive(SvnTarget.fromFile(getOperation().getRepositoryRoot()), entry);
    }
}
