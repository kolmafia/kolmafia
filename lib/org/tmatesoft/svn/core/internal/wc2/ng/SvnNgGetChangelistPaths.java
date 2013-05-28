package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.ISVNWCNodeHandler;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbInfo.InfoField;
import org.tmatesoft.svn.core.wc.ISVNChangelistHandler;
import org.tmatesoft.svn.core.wc2.SvnGetChangelistPaths;
import org.tmatesoft.svn.core.wc2.SvnTarget;


public class SvnNgGetChangelistPaths extends SvnNgOperationRunner<String, SvnGetChangelistPaths> implements ISVNChangelistHandler, ISVNWCNodeHandler {

	private SVNWCContext context;
	
    @Override
    protected String run(SVNWCContext context) throws SVNException {
    	this.context = context;
    	for (SvnTarget target: getOperation().getTargets()) {
            doGetChangeLists(target.getFile());
        }
        return getOperation().first();
    }
    
    public void doGetChangeLists(File file) throws SVNException {
    	 context.nodeWalkChildren(file, this, false, getOperation().getDepth(), getOperation().getApplicableChangelists());
    }
    
    public void nodeFound(File localAbsPath, SVNWCDbKind kind) throws SVNException {
    	WCDbInfo info = context.getDb().readInfo(localAbsPath, InfoField.changelist);
    	if (context.matchesChangelist(localAbsPath, getOperation().getApplicableChangelists())) {
    		handle(localAbsPath, info.changelist);
    	}
    }
   
	public void handle(File path, String changelistName) {
		try {
            getOperation().receive(SvnTarget.fromFile(path), changelistName);
        } catch (SVNException e) {
        }
	}
}
