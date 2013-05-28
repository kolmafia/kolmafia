package org.tmatesoft.svn.core.internal.wc2.remote;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import org.tmatesoft.svn.core.ISVNDirEntryHandler;
import org.tmatesoft.svn.core.SVNAuthenticationException;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLock;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNRevisionProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc2.SvnRemoteOperationRunner;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess.RepositoryInfo;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc2.SvnList;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnRemoteList extends SvnRemoteOperationRunner<SVNDirEntry, SvnList> implements ISVNDirEntryHandler {

    public boolean isApplicable(SvnList operation, SvnWcGeneration wcGeneration) throws SVNException {
        return true;
    }

    protected SVNDirEntry run() throws SVNException {
        SvnTarget infoTarget = getOperation().getFirstTarget();
        Structure<RepositoryInfo> repositoryInfo = 
            getRepositoryAccess().createRepositoryFor(infoTarget, getOperation().getRevision(), infoTarget.getResolvedPegRevision(), null);
        
        SVNRepository repository = repositoryInfo.<SVNRepository>get(RepositoryInfo.repository);
        long revNum = repositoryInfo.lng(RepositoryInfo.revision);                
        
        repositoryInfo.release();
        
        doList(repository, revNum, this, getOperation().isFetchLocks(), getOperation().getDepth(), getOperation().getEntryFields());
        
        return getOperation().first();
    }

    public void handleDirEntry(SVNDirEntry dirEntry) throws SVNException {
        getOperation().receive(SvnTarget.fromURL(dirEntry.getURL()), dirEntry);
    }

    private void doList(SVNRepository repos, long rev, final ISVNDirEntryHandler handler, boolean fetchLocks, SVNDepth depth, int entryFields) throws SVNException {
        SVNURL url = repos.getLocation();
        SVNURL reposRoot = repos.getRepositoryRoot(false);
        SVNDirEntry entry = null;
        SVNException error = null;
        try {
            entry = repos.info("", rev);
        } catch (SVNException svne) {
            if (svne.getErrorMessage().getErrorCode() == SVNErrorCode.RA_NOT_IMPLEMENTED) {
                error = svne;
            } else {
                throw svne;
            }
        }
        if (error != null) {
            SVNNodeKind kind = repos.checkPath("", rev);
            if (kind != SVNNodeKind.NONE) {
                if (!url.equals(reposRoot)) {
                    String name = SVNPathUtil.tail(repos.getLocation().getPath());
                    repos.setLocation(repos.getLocation().removePathTail(), false);
                    Collection<SVNDirEntry> dirEntries = repos.getDir("", rev, null, entryFields, (Collection) null);
                    repos.setLocation(url, false);
                    for (Iterator<SVNDirEntry> ents = dirEntries.iterator(); ents.hasNext();) {
                        SVNDirEntry dirEntry = (SVNDirEntry) ents.next();
                        if (name.equals(dirEntry.getName())) {
                            entry = dirEntry;
                            break;
                        }
                    }
                    if (entry != null) {
                        entry.setRelativePath(kind == SVNNodeKind.FILE ? name : "");
                    }
                } else {
                    SVNProperties props = new SVNProperties();
                    repos.getDir("", rev, props, entryFields, (Collection<SVNDirEntry>) null);
                    SVNProperties revProps = repos.getRevisionProperties(rev, null);
                    String author = revProps.getStringValue(SVNRevisionProperty.AUTHOR);
                    String dateStr = revProps.getStringValue(SVNRevisionProperty.DATE);
                    Date datestamp = null;
                    if (dateStr != null) {
                        datestamp = SVNDate.parseDateString(dateStr);
                    }
                    entry = new SVNDirEntry(url, reposRoot, "", kind, 0, !props.isEmpty(), rev, datestamp, author);
                    entry.setRelativePath("");
                }
            }
        } else if (entry != null) {
            if (entry.getKind() == SVNNodeKind.DIR) {
                entry.setName("");
            }
            entry.setRelativePath(entry.getKind() == SVNNodeKind.DIR ? "" : entry.getName());
        }
        if (entry == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_FOUND, "URL ''{0}'' non-existent in that revision", url);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        final Map locksMap = new SVNHashMap();
        if (fetchLocks) {
            SVNLock[] locks = new SVNLock[0];
            try {
                locks = repos.getLocks("");
            } catch (SVNException e) {
                if (!(e.getErrorMessage() != null && e.getErrorMessage().getErrorCode() == SVNErrorCode.RA_NOT_IMPLEMENTED)) {
                    throw e;
                }
            }
            if (locks != null && locks.length > 0) {
                SVNURL root = repos.getRepositoryRoot(true);
                for (int i = 0; i < locks.length; i++) {
                    String repositoryPath = locks[i].getPath();
                    locksMap.put(root.appendPath(repositoryPath, false), locks[i]);
                }
            }
        }
        ISVNDirEntryHandler nestedHandler = new ISVNDirEntryHandler() {

            public void handleDirEntry(SVNDirEntry dirEntry) throws SVNException {
                dirEntry.setLock((SVNLock) locksMap.get(dirEntry.getURL()));
                handler.handleDirEntry(dirEntry);
            }
        };
        nestedHandler.handleDirEntry(entry);
        if (entry.getKind() == SVNNodeKind.DIR && (depth == SVNDepth.FILES || depth == SVNDepth.IMMEDIATES || depth == SVNDepth.INFINITY)) {
            list(repos, "", rev, depth, entryFields, nestedHandler);
        }
    }

    private static void list(SVNRepository repository, String path, long rev, SVNDepth depth, int entryFields, ISVNDirEntryHandler handler) throws SVNException {
        if (depth == SVNDepth.EMPTY) {
            return;
        }
        Collection entries = new TreeSet();
        try {
            entries = repository.getDir(path, rev, null, entryFields, entries);
        } catch (SVNAuthenticationException e) {
            return;
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.RA_NOT_AUTHORIZED) {
                return;
            }
            throw e;
        }
        
        for (Iterator iterator = entries.iterator(); iterator.hasNext();) {
            SVNDirEntry entry = (SVNDirEntry) iterator.next();
            String childPath = SVNPathUtil.append(path, entry.getName());
            entry.setRelativePath(childPath);
            if (entry.getKind() == SVNNodeKind.FILE || depth == SVNDepth.IMMEDIATES || depth == SVNDepth.INFINITY) {
                handler.handleDirEntry(entry);
            }
            if (entry.getKind() == SVNNodeKind.DIR && entry.getDate() != null && depth == SVNDepth.INFINITY) {
                list(repository, childPath, rev, depth, entryFields, handler);
            }
        }
    }
}
