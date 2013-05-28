package org.tmatesoft.svn.core.internal.wc2.admin;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.admin.ISVNHistoryHandler;
import org.tmatesoft.svn.core.wc.admin.SVNAdminPath;
import org.tmatesoft.svn.core.wc.admin.SVNLookClient;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetHistory;


public class SvnRepositoryGetHistoryImpl extends SvnRepositoryOperationRunner<SVNAdminPath, SvnRepositoryGetHistory> implements ISVNHistoryHandler {

    @Override
    protected SVNAdminPath run() throws SVNException {
        SVNLookClient lc = new SVNLookClient(getOperation().getAuthenticationManager(), getOperation().getOptions());
        lc.setEventHandler(this);
        
        lc.doGetHistory(
        		getOperation().getRepositoryRoot(), 
        		getOperation().getPath(), 
        		getOperation().getRevision(), 
        		getOperation().isIncludeIDs(), 
        		getOperation().getLimit(), 
        		this);
        
        return getOperation().first();
    }
    
    public void handlePath(SVNAdminPath path) throws SVNException {
    	getOperation().receive(SvnTarget.fromFile(getOperation().getRepositoryRoot()), path);
    }
}
