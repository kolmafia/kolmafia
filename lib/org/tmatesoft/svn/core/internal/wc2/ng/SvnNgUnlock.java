package org.tmatesoft.svn.core.internal.wc2.ng;

import java.util.Map;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLock;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgLockUtil.LockInfo;
import org.tmatesoft.svn.core.io.ISVNLockHandler;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc2.SvnUnlock;

public class SvnNgUnlock extends SvnNgOperationRunner<SVNLock, SvnUnlock> {

    @Override
    protected SVNLock run(SVNWCContext context) throws SVNException {
        
    	final Map entriesMap = new SVNHashMap();
        Map pathsRevisionsMap = new SVNHashMap();
        final SvnNgRepositoryAccess wcAccess = getRepositoryAccess();
        
            final SVNURL topURL = SvnNgLockUtil.collectLockInfo(this, getWcContext(), wcAccess, getOperation().getTargets(), entriesMap, pathsRevisionsMap, false, getOperation().isBreakLock());
            SVNRepository repository = getRepositoryAccess().createRepository(topURL, null, true);
            final SVNURL rootURL = repository.getRepositoryRoot(true);
            repository.unlock(pathsRevisionsMap, getOperation().isBreakLock(), new ISVNLockHandler() {

                public void handleLock(String path, SVNLock lock, SVNErrorMessage error) throws SVNException {
                }

                public void handleUnlock(String path, SVNLock lock, SVNErrorMessage error) throws SVNException {
                    SVNURL fullURL = rootURL.appendPath(path, false);
                    LockInfo lockInfo = (LockInfo) entriesMap.get(fullURL);
                    SVNEventAction action = null;
                    
                    if (error == null || (error != null && error.getErrorCode() != SVNErrorCode.FS_LOCK_OWNER_MISMATCH)) {
                    	getWcContext().getDb().removeLock(lockInfo.getFile());
                        if (getWcContext().getProperty(lockInfo.getFile(), SVNProperty.NEEDS_LOCK) != null) {
	                    	SVNFileUtil.setReadonly(lockInfo.getFile(), true);
	                    }
                        action = SVNEventAction.UNLOCKED;
                    }
                    if (error != null) {
                        action = SVNEventAction.UNLOCK_FAILED;
                    }
                    if (action != null) {
                        handleEvent(SVNEventFactory.createLockEvent(lockInfo.getFile(), action, lock, error), ISVNEventHandler.UNKNOWN);
                    }
                }
            });
        
        
    	return getOperation().first();
    }
}
