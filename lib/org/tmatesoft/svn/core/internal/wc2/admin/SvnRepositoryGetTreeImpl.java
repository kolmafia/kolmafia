package org.tmatesoft.svn.core.internal.wc2.admin;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.admin.ISVNTreeHandler;
import org.tmatesoft.svn.core.wc.admin.SVNAdminPath;
import org.tmatesoft.svn.core.wc.admin.SVNLookClient;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetTree;


public class SvnRepositoryGetTreeImpl extends SvnRepositoryOperationRunner<SVNAdminPath, SvnRepositoryGetTree> implements ISVNTreeHandler {

    @Override
    protected SVNAdminPath run() throws SVNException {
        SVNLookClient lc = new SVNLookClient(getOperation().getAuthenticationManager(), getOperation().getOptions());
        lc.setEventHandler(this);
        
        if (getOperation().getTransactionName() == null)
        	lc.doGetTree(getOperation().getRepositoryRoot(), getOperation().getPath(), getOperation().getRevision(), getOperation().isIncludeIDs(), getOperation().isRecursive(), this);
        else
        	lc.doGetTree(getOperation().getRepositoryRoot(), getOperation().getPath(), getOperation().getTransactionName(), getOperation().isIncludeIDs(), getOperation().isRecursive(), this);
        
        return getOperation().first();
    }
    
    public void handlePath(SVNAdminPath path) throws SVNException {
    	getOperation().receive(SvnTarget.fromFile(getOperation().getRepositoryRoot()), path);
    }
}
