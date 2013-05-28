package org.tmatesoft.svn.core.internal.wc2.admin;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.admin.ISVNChangedDirectoriesHandler;
import org.tmatesoft.svn.core.wc.admin.SVNLookClient;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetChangedDirectories;


public class SvnRepositoryGetChangedDirectoriesImpl extends SvnRepositoryOperationRunner<String, SvnRepositoryGetChangedDirectories> implements ISVNChangedDirectoriesHandler {

    @Override
    protected String run() throws SVNException {
        SVNLookClient lc = new SVNLookClient(getOperation().getAuthenticationManager(), getOperation().getOptions());
        lc.setEventHandler(this);
        
        if (getOperation().getTransactionName() == null)
        	lc.doGetChangedDirectories(getOperation().getRepositoryRoot(), getOperation().getRevision(), this);
        else
        	lc.doGetChangedDirectories(getOperation().getRepositoryRoot(), getOperation().getTransactionName(), this);
        
        return getOperation().first();
    }
    
    public void handleDir(String path) throws SVNException {
    	getOperation().receive(SvnTarget.fromFile(getOperation().getRepositoryRoot()), path);
    }
}
