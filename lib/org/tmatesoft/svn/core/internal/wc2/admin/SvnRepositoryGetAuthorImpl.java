package org.tmatesoft.svn.core.internal.wc2.admin;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.admin.SVNLookClient;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryGetAuthor;


public class SvnRepositoryGetAuthorImpl extends SvnRepositoryOperationRunner<String, SvnRepositoryGetAuthor> {

    @Override
    protected String run() throws SVNException {
        SVNLookClient lc = new SVNLookClient(getOperation().getAuthenticationManager(), getOperation().getOptions());
        lc.setEventHandler(this);
        
        String author;
             
        if (getOperation().getTransactionName() == null)
        	author = lc.doGetAuthor(getOperation().getRepositoryRoot(), getOperation().getRevision());
        else
        	author = lc.doGetAuthor(getOperation().getRepositoryRoot(), getOperation().getTransactionName());
        
        return author;
    }
}
