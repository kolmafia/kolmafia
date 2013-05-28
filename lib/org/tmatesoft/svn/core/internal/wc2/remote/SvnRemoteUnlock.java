package org.tmatesoft.svn.core.internal.wc2.remote;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLock;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.util.SVNHashSet;
import org.tmatesoft.svn.core.internal.util.SVNURLUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc2.SvnRemoteOperationRunner;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.io.ISVNLockHandler;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc2.SvnSetLock;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.core.wc2.SvnUnlock;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnRemoteUnlock extends SvnRemoteOperationRunner<SVNLock, SvnUnlock> implements ISVNLockHandler {

    @Override
    public boolean isApplicable(SvnUnlock operation, SvnWcGeneration wcGeneration) throws SVNException {
        return operation.hasRemoteTargets();
    }

    @Override
    protected SVNLock run() throws SVNException {
    	int i = 0;
    	SVNURL[] urls = new SVNURL[getOperation().getTargets().size()];
        for (SvnTarget target : getOperation().getTargets()) {
        	urls[i++] = target.getURL();
        }
        
    	Collection<String> paths = new SVNHashSet();
        SVNURL topURL = SVNURLUtil.condenceURLs(urls, paths, false);
        if (paths.isEmpty()) {
            paths.add("");
        }
        Map<String, String> pathsToTokens = new SVNHashMap();
        for (Iterator<String> p = paths.iterator(); p.hasNext();) {
            String path = (String) p.next();
            path = SVNEncodingUtil.uriDecode(path);
            pathsToTokens.put(path, null);
        }
        
        checkCancelled();        
        SVNRepository repository = getRepositoryAccess().createRepository(topURL, null, true);
        if (!getOperation().isBreakLock()) {
            pathsToTokens = fetchLockTokens(repository, pathsToTokens);
        }
        repository.unlock(pathsToTokens, getOperation().isBreakLock(), this);
        
        return getOperation().first();
    }
    
    private Map<String, String> fetchLockTokens(SVNRepository repository, Map<String, String> pathsTokensMap) throws SVNException {
        Map<String, String> tokens = new SVNHashMap();
        for (Iterator<String> paths = pathsTokensMap.keySet().iterator(); paths.hasNext();) {
            String path = paths.next();
            SVNLock lock = repository.getLock(path);
            if (lock == null || lock.getID() == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_MISSING_LOCK_TOKEN, "''{0}'' is not locked", path);
                SVNErrorManager.error(err, SVNLogType.WC);
                continue;
            }
            tokens.put(path, lock.getID());
        }
        return tokens;
    }

    public void handleLock(String path, SVNLock lock, SVNErrorMessage error) throws SVNException {
    }

    public void handleUnlock(String path, SVNLock lock, SVNErrorMessage error) throws SVNException {
        if (error != null) {
            handleEvent(SVNEventFactory.createLockEvent(new File(path), SVNEventAction.UNLOCK_FAILED, null, error), ISVNEventHandler.UNKNOWN);
        } else {
            handleEvent(SVNEventFactory.createLockEvent(new File(path), SVNEventAction.UNLOCKED, null, null), ISVNEventHandler.UNKNOWN);
        }
    }
}
