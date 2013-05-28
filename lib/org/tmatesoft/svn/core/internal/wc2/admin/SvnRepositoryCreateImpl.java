package org.tmatesoft.svn.core.internal.wc2.admin;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.admin.SVNAdminClient;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryCreate;

public class SvnRepositoryCreateImpl extends SvnRepositoryOperationRunner<SVNURL, SvnRepositoryCreate> {

    @Override
    protected SVNURL run() throws SVNException {
        SVNAdminClient ac = new SVNAdminClient(getOperation().getAuthenticationManager(), getOperation().getOptions());
        ac.setEventHandler(this);
                
        return ac.doCreateRepository(
        		getOperation().getRepositoryRoot(),
        		getOperation().getUuid(), 
        		getOperation().isEnableRevisionProperties(),  
                getOperation().isForce(), 
                getOperation().isPre14Compatible(), 
                getOperation().isPre15Compatible(), 
                getOperation().isPre16Compatible(), 
                getOperation().isPre17Compatible(),
                getOperation().isWith17Compatible());
    }

    
}
