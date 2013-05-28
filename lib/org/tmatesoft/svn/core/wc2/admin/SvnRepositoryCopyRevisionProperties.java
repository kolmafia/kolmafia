package org.tmatesoft.svn.core.wc2.admin;

import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.admin.SVNAdminEvent;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SvnRepositoryCopyRevisionProperties extends SvnRepositoryReceivingOperation<SVNAdminEvent> {
    
    private SVNURL toURL;
    private long startRevision;
    private long endRevision;
    
    public SvnRepositoryCopyRevisionProperties(SvnOperationFactory factory) {
        super(factory);
    }	

	public SVNURL getToURL() {
		return toURL;
	}

	public void setToURL(SVNURL toURL) {
		this.toURL = toURL;
		setSingleTarget(SvnTarget.fromURL(toURL));
	}
	
	public long getStartRevision() {
        return startRevision;
    }

    public void setStartRevision(long startRevision) {
        this.startRevision = startRevision;
    }

    public long getEndRevision() {
        return endRevision;
    }

    public void setEndRevision(long endRevision) {
        this.endRevision = endRevision;
    }
}
