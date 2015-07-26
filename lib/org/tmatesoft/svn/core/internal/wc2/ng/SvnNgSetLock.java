package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.util.Map;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.util.SVNXMLUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbLock;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgLockUtil.LockInfo;
import org.tmatesoft.svn.core.io.ISVNLockHandler;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc2.SvnSetLock;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnNgSetLock extends SvnNgOperationRunner<SVNLock, SvnSetLock> implements ISVNLockHandler {

    @Override
    protected SVNLock run(SVNWCContext context) throws SVNException {

        String lockMessage = getOperation().getLockMessage();
        if (lockMessage != null) {
            if (!SVNEncodingUtil.isXMLSafe(lockMessage)) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.XML_UNESCAPABLE_DATA, "Lock comment contains illegal characters");
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
        }

    	final Map entriesMap = new SVNHashMap();
        Map pathsRevisionsMap = new SVNHashMap();
        final SvnNgRepositoryAccess repositoryAccess = getRepositoryAccess();
        
        final SVNURL topURL = SvnNgLockUtil.collectLockInfo(this, getWcContext(), repositoryAccess, getOperation().getTargets(), entriesMap, pathsRevisionsMap, true, getOperation().isStealLock());
            SVNRepository repository = getRepositoryAccess().createRepository(topURL, null, true);
            final SVNURL rootURL = repository.getRepositoryRoot(true);
            repository.lock(pathsRevisionsMap, getOperation().getLockMessage(), getOperation().isStealLock(), new ISVNLockHandler() {

                public void handleLock(String path, SVNLock lock, SVNErrorMessage error) throws SVNException {
                    SVNURL fullURL = rootURL.appendPath(path, false);
                    LockInfo lockInfo = (LockInfo) entriesMap.get(fullURL);
                    
                    if (error == null)
                    {
	                    SVNWCDbLock dbLock = new SVNWCDbLock();
	                    dbLock.token = lock.getID();
	                    dbLock.owner = lock.getOwner();
	                    dbLock.comment = lock.getComment();
	                    dbLock.date = SVNDate.fromDate(lock.getCreationDate());
	                    getWcContext().getDb().addLock(lockInfo.getFile(), dbLock);
	                    
	                    if (getWcContext().getProperty(lockInfo.getFile(), SVNProperty.NEEDS_LOCK) != null) {
	                    	SVNFileUtil.setReadonly(lockInfo.getFile(), false);
	                    }
	                    handleEvent(SVNEventFactory.createLockEvent(lockInfo.getFile(), SVNEventAction.LOCKED, lock, null), ISVNEventHandler.UNKNOWN);
                    }
                    else {
                    	handleEvent(SVNEventFactory.createLockEvent(lockInfo.getFile(), SVNEventAction.LOCK_FAILED, lock, error), ISVNEventHandler.UNKNOWN);
                    }
                }

                public void handleUnlock(String path, SVNLock lock, SVNErrorMessage error) {
                }
            });
        
        
    	return getOperation().first();
    }
    
    public void handleLock(String path, SVNLock lock, SVNErrorMessage error) throws SVNException {
        if (error != null) {
            handleEvent(SVNEventFactory.createLockEvent(new File(path), SVNEventAction.LOCK_FAILED, lock, error), ISVNEventHandler.UNKNOWN);
        } else {
            handleEvent(SVNEventFactory.createLockEvent(new File(path), SVNEventAction.LOCKED, lock, null), ISVNEventHandler.UNKNOWN);
        }
    }

    public void handleUnlock(String path, SVNLock lock, SVNErrorMessage error) throws SVNException {
    }
 
}
