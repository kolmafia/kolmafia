package org.tmatesoft.svn.core.wc2.admin;

import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.admin.SVNAdminEvent;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnReceivingOperation;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SvnRepositoryInitialize extends SvnReceivingOperation<SVNAdminEvent> {
    
    private SVNURL fromURL;
    private SVNURL toURL;
    
    public SvnRepositoryInitialize(SvnOperationFactory factory) {
        super(factory);
    }

	public SVNURL getFromURL() {
		return fromURL;
	}

	public void setFromURL(SVNURL fromURL) {
		this.fromURL = fromURL;
	}

	public SVNURL getToURL() {
		return toURL;
	}

	public void setToURL(SVNURL toURL) {
		this.toURL = toURL;
        setSingleTarget(toURL != null ? SvnTarget.fromURL(toURL) : null);
	}

	

	    
}
