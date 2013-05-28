package org.tmatesoft.svn.core.wc2.admin;

import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.admin.SVNAdminEvent;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

public class SvnRepositoryVerify extends SvnRepositoryReceivingOperation<SVNAdminEvent> {

    private SVNRevision startRevision;
    private SVNRevision endRevision;
    
    public SvnRepositoryVerify(SvnOperationFactory factory) {
        super(factory);
    }
    
    @Override
    protected void initDefaults() {
    	super.initDefaults();
    	startRevision = SVNRevision.create(0);
        endRevision = SVNRevision.HEAD;
    }
	
	public SVNRevision getStartRevision() {
        return startRevision;
    }

    public void setStartRevision(SVNRevision startRevision) {
        this.startRevision = startRevision;
    }

    public SVNRevision getEndRevision() {
        return endRevision;
    }

    public void setEndRevision(SVNRevision endRevision) {
        this.endRevision = endRevision;
    }
}
