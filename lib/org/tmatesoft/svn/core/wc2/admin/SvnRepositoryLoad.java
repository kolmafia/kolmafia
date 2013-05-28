package org.tmatesoft.svn.core.wc2.admin;

import java.io.InputStream;

import org.tmatesoft.svn.core.wc.admin.SVNAdminEvent;
import org.tmatesoft.svn.core.wc.admin.SVNUUIDAction;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

public class SvnRepositoryLoad extends SvnRepositoryReceivingOperation<SVNAdminEvent> {
    
    private InputStream dumpStream;
    private boolean usePreCommitHook;
    private boolean usePostCommitHook;
    private SVNUUIDAction uuidAction;
    private String parentDir;
    
    
    public SvnRepositoryLoad(SvnOperationFactory factory) {
        super(factory);
    }
    
    @Override
    protected void initDefaults() {
    	super.initDefaults();
    	uuidAction = SVNUUIDAction.DEFAULT;
    }

    public InputStream getDumpStream() {
		return dumpStream;
	}

	public void setDumpStream(InputStream dumpStream) {
		this.dumpStream = dumpStream;
	}

	public boolean isUsePreCommitHook() {
		return usePreCommitHook;
	}

	public void setUsePreCommitHook(boolean usePreCommitHook) {
		this.usePreCommitHook = usePreCommitHook;
	}

	public boolean isUsePostCommitHook() {
		return usePostCommitHook;
	}

	public void setUsePostCommitHook(boolean usePostCommitHook) {
		this.usePostCommitHook = usePostCommitHook;
	}

	public SVNUUIDAction getUuidAction() {
		return uuidAction;
	}

	public void setUuidAction(SVNUUIDAction uuidAction) {
		this.uuidAction = uuidAction;
	}

	public String getParentDir() {
		return parentDir;
	}

	public void setParentDir(String parentDir) {
		this.parentDir = parentDir;
	}
}
