package org.tmatesoft.svn.core.internal.wc2.remote;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLock;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc2.SvnRemoteOperationRunner;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess.LocationsInfo;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess.RepositoryInfo;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.SvnGetInfo;
import org.tmatesoft.svn.core.wc2.SvnInfo;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnRemoteGetInfo extends SvnRemoteOperationRunner<SvnInfo, SvnGetInfo> {

    public boolean isApplicable(SvnGetInfo operation, SvnWcGeneration wcGeneration) throws SVNException {
        if (super.isApplicable(operation, wcGeneration)) {
            return true;
        }
        SVNRevision revision = operation.getRevision();
        if (!revision.isLocal()) {
            return true;
        }
        return false;
    }

    protected SvnInfo run() throws SVNException {
        SvnTarget infoTarget = getOperation().getFirstTarget();
        Structure<RepositoryInfo> repositoryInfo = 
            getRepositoryAccess().createRepositoryFor(infoTarget, getOperation().getRevision(), infoTarget.getResolvedPegRevision(), null);
        
        SVNRepository repository = repositoryInfo.<SVNRepository>get(RepositoryInfo.repository);
        SVNURL url = repositoryInfo.<SVNURL>get(RepositoryInfo.url);
        long revNum = repositoryInfo.lng(RepositoryInfo.revision);
        SVNURL repositoryRootUrl = repository.getRepositoryRoot(true);
        String repositoryUUID = repository.getRepositoryUUID(true);
        
        repositoryInfo.release();
        
        SVNURL parentURL = url.removePathTail();
        String baseName = SVNPathUtil.tail(url.getPath());
        SVNDirEntry rootEntry = null;
        
        SVNDepth depth = getOperation().getDepth();
        SVNRevision pegRevision = infoTarget.getResolvedPegRevision();
        
        try {
            rootEntry = repository.info("", revNum);
        } catch (SVNException e) {
            if (e.getErrorMessage() != null && e.getErrorMessage().getErrorCode() == SVNErrorCode.RA_NOT_IMPLEMENTED) {
                if (url.equals(repositoryRootUrl)) {
                    if (depth.compareTo(SVNDepth.EMPTY) > 0) {
                        SVNLock[] locks = null;
                        if (pegRevision == SVNRevision.HEAD) {
                            try {
                                locks = repository.getLocks("");
                            } catch (SVNException svne) {
                                SVNErrorCode code = svne.getErrorMessage().getErrorCode();
                                if (code == SVNErrorCode.RA_NOT_IMPLEMENTED || code == SVNErrorCode.UNSUPPORTED_FEATURE) {
                                    locks = new SVNLock[0];
                                } else {
                                    throw svne;
                                }
                            }
                        } else {
                            locks = new SVNLock[0];
                        }
                        locks = locks == null ? new SVNLock[0] : locks;
                        Map<String, SVNLock> locksMap = new HashMap<String, SVNLock>();
                        for (int i = 0; i < locks.length; i++) {
                            SVNLock lock = locks[i];
                            locksMap.put(lock.getPath(), lock);
                        }
                        pushDirInfo(repository, SVNRevision.create(revNum), "", repository.getRepositoryRoot(true), repositoryUUID, url, locksMap, depth);
                        return getOperation().first();
                    }
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "Server does not support retrieving information about the repository root");
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
                SVNNodeKind urlKind = repository.checkPath("", revNum);
                if (urlKind == SVNNodeKind.NONE) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_ILLEGAL_URL, "URL ''{0}'' non-existent in revision {1}", new Object[] { url, new Long(revNum) });
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
                
                SVNRepository parentRepository = getRepositoryAccess().createRepository(parentURL, null, false);
                
                Collection<SVNDirEntry> dirEntries = parentRepository.getDir("", revNum, null, SVNDirEntry.DIRENT_KIND | SVNDirEntry.DIRENT_CREATED_REVISION | SVNDirEntry.DIRENT_TIME
                        | SVNDirEntry.DIRENT_LAST_AUTHOR, (Collection<SVNDirEntry>) null);
                
                for (SVNDirEntry dirEntry : dirEntries) {
                    if (baseName.equals(dirEntry.getName())) {
                        rootEntry = dirEntry;
                        break;
                    }
                }
                if (rootEntry == null) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_ILLEGAL_URL, "URL ''{0}'' non-existent in revision {1}", new Object[] { url, new Long(revNum)});
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
            } else {
                throw e;
            }
        }
        if (rootEntry == null || rootEntry.getKind() == SVNNodeKind.NONE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_ILLEGAL_URL, "URL ''{0}'' non-existent in revision {1}", new Object[] { url, new Long(revNum) });
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        
        SVNLock lock = null;
        if (rootEntry.getKind() == SVNNodeKind.FILE) {
            try {
                Structure<LocationsInfo> locations = getRepositoryAccess().getLocations(null, SvnTarget.fromURL(url), SVNRevision.create(revNum), SVNRevision.HEAD, SVNRevision.UNDEFINED);
                if (locations != null && locations.hasValue(LocationsInfo.startUrl)) {
                    SVNURL headURL = locations.<SVNURL>get(LocationsInfo.startUrl);
                    locations.release();
                    if (headURL.equals(url)) {
                        try {
                            lock = repository.getLock("");
                        } catch (SVNException e) {
                            if (!(e.getErrorMessage() != null && e.getErrorMessage().getErrorCode() == SVNErrorCode.RA_NOT_IMPLEMENTED)) {
                                throw e;
                            }
                        }
                    }
                }
            } catch (SVNException e) {
                SVNErrorCode code = e.getErrorMessage().getErrorCode();
                if (code != SVNErrorCode.FS_NOT_FOUND && code != SVNErrorCode.CLIENT_UNRELATED_RESOURCES) {
                    throw e;
                }
            }
        }
        SvnInfo info = creatSvnInfoForEntry(repositoryRootUrl, repositoryUUID, rootEntry, url, revNum, lock);
        getOperation().receive(SvnTarget.fromURL(url), info);
        
        if (depth.compareTo(SVNDepth.EMPTY) > 0 && rootEntry.getKind() == SVNNodeKind.DIR) {
            SVNLock[] locks = null;
            if (pegRevision == SVNRevision.HEAD) {
                try {
                    locks = repository.getLocks("");
                } catch (SVNException svne) {
                    SVNErrorCode code = svne.getErrorMessage().getErrorCode();
                    if (code == SVNErrorCode.RA_NOT_IMPLEMENTED || code == SVNErrorCode.UNSUPPORTED_FEATURE) {
                        locks = new SVNLock[0];
                    } else {
                        throw svne;
                    }
                }
            } else {
                locks = new SVNLock[0];
            }
            locks = locks == null ? new SVNLock[0] : locks;
            Map<String, SVNLock> locksMap = new HashMap<String, SVNLock>();
            for (int i = 0; i < locks.length; i++) {
                lock = locks[i];
                locksMap.put(lock.getPath(), lock);
            }
            
            pushDirInfo(repository, SVNRevision.create(revNum), "", repository.getRepositoryRoot(true), repositoryUUID, url, locksMap, depth);
        }
        return getOperation().first();        
    }
    
    private void pushDirInfo(SVNRepository repos, SVNRevision rev, String path, SVNURL root, String uuid, SVNURL url, Map<String, SVNLock> locks, SVNDepth depth) throws SVNException {
        Collection<SVNDirEntry> children =  repos.getDir(path, rev.getNumber(), null, SVNDirEntry.DIRENT_SIZE | SVNDirEntry.DIRENT_KIND | SVNDirEntry.DIRENT_CREATED_REVISION | SVNDirEntry.DIRENT_TIME | SVNDirEntry.DIRENT_LAST_AUTHOR,  new ArrayList<SVNDirEntry>());
        
        for (SVNDirEntry child : children) {
            SVNURL childURL = url.appendPath(child.getName(), false);
            SVNLock lock = locks.get(path);            
            
            if (depth.compareTo(SVNDepth.IMMEDIATES) >= 0 || (depth == SVNDepth.FILES && child.getKind() == SVNNodeKind.FILE)) {
                SvnInfo info = creatSvnInfoForEntry(root, uuid, child, childURL, rev.getNumber(), lock);
                
                getOperation().receive(SvnTarget.fromURL(childURL), info);
            }
            
            if (depth == SVNDepth.INFINITY && child.getKind() == SVNNodeKind.DIR) {
                pushDirInfo(repos, rev, SVNPathUtil.append(path, child.getName()), root, uuid, childURL, locks, depth);
            }
        }
    }

    private SvnInfo creatSvnInfoForEntry(SVNURL root, String uuid, SVNDirEntry entry, SVNURL entryURL, long revision, SVNLock lock) {
        SvnInfo info = new SvnInfo();
        info.setKind(entry.getKind());
        info.setLastChangedAuthor(entry.getAuthor());
        info.setLastChangedDate(SVNDate.fromDate(entry.getDate()));
        info.setLastChangedRevision(entry.getRevision());
        info.setLock(lock);
        info.setRepositoryRootURL(root);
        info.setRepositoryUuid(uuid);
        info.setSize(entry.getSize());
        info.setUrl(entryURL);
        info.setRevision(revision);
        return info;
    }

}
