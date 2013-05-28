package org.tmatesoft.svn.core.internal.wc2.remote;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLock;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.util.SVNHashSet;
import org.tmatesoft.svn.core.internal.util.SVNURLUtil;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc2.SvnRemoteOperationRunner;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.io.ISVNLockHandler;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc2.SvnSetLock;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SvnRemoteSetLock extends SvnRemoteOperationRunner<SVNLock, SvnSetLock> implements ISVNLockHandler {

    public boolean isApplicable(SvnSetLock operation, SvnWcGeneration wcGeneration) throws SVNException {
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

        Map<String, Long> pathsToRevisions = new SVNHashMap();
        for (Iterator<String> p = paths.iterator(); p.hasNext();) {
            String path = (String) p.next();
            path = SVNEncodingUtil.uriDecode(path);
            pathsToRevisions.put(path, null);
        }

        checkCancelled();
        SVNRepository repository = getRepositoryAccess().createRepository(topURL, null, true);
        repository.lock(pathsToRevisions, getOperation().getLockMessage(), getOperation().isStealLock(), this);

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
