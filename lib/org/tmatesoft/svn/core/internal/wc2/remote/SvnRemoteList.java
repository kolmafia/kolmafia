package org.tmatesoft.svn.core.internal.wc2.remote;

import java.io.File;
import java.util.*;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNExternal;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc2.SvnRemoteOperationRunner;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess.RepositoryInfo;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc2.SvnList;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnRemoteList extends SvnRemoteOperationRunner<SVNDirEntry, SvnList> implements ISVNDirEntryHandler {

    public boolean isApplicable(SvnList operation, SvnWcGeneration wcGeneration) throws SVNException {
        if (wcGeneration == SvnWcGeneration.V16 && operation.getFirstTarget().isFile()) {
            return false;
        }
        return true;
    }

    protected SVNDirEntry run() throws SVNException {
        SvnTarget infoTarget = getOperation().getFirstTarget();
        Structure<RepositoryInfo> repositoryInfo = 
            getRepositoryAccess().createRepositoryFor(infoTarget, getOperation().getRevision(), infoTarget.getResolvedPegRevision(), null);
        
        SVNRepository repository = repositoryInfo.<SVNRepository>get(RepositoryInfo.repository);
        long revNum = repositoryInfo.lng(RepositoryInfo.revision);                
        
        repositoryInfo.release();
        
        doList(repository, revNum, this, getOperation().isFetchLocks(), getOperation().getDepth(), getOperation().getEntryFields(), null, null);
        
        return getOperation().first();
    }

    public void handleDirEntry(SVNDirEntry dirEntry) throws SVNException {
        getOperation().receive(SvnTarget.fromURL(dirEntry.getURL()), dirEntry);
    }

    private void doList(SVNRepository repos, long rev, final ISVNDirEntryHandler handler, boolean fetchLocks, SVNDepth depth, int entryFields, SVNURL externalParentUrl, String externalTarget) throws SVNException {
        boolean includeExternals = !getOperation().isIgnoreExternals();
        Map<SVNURL, SVNPropertyValue> externals = includeExternals ? new HashMap<SVNURL, SVNPropertyValue>() : null;
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
        entry.setExternalParentUrl(externalParentUrl);
        entry.setExternalTarget(externalTarget);
        nestedHandler.handleDirEntry(entry);
        if (entry.getKind() == SVNNodeKind.DIR && (depth == SVNDepth.FILES || depth == SVNDepth.IMMEDIATES || depth == SVNDepth.INFINITY)) {
            list(repos, "", rev, depth, entryFields, externals, externalParentUrl, externalTarget, nestedHandler);
        }

        if (includeExternals && externals != null && externals.size() > 0) {
            listExternals(repos, externals, depth, entryFields, fetchLocks, handler);
        }
    }

    private static void list(SVNRepository repository, String path, long rev, SVNDepth depth, int entryFields, Map<SVNURL, SVNPropertyValue> externals, SVNURL externalParentUrl, String externalTarget, ISVNDirEntryHandler handler) throws SVNException {
        if (depth == SVNDepth.EMPTY) {
            return;
        }
        Collection entries = new TreeSet();
        SVNProperties properties;
        try {
            properties = externals == null ? null : new SVNProperties();
            entries = repository.getDir(path, rev, properties, entryFields, entries);
        } catch (SVNAuthenticationException e) {
            return;
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.RA_NOT_AUTHORIZED) {
                return;
            }
            throw e;
        }

        SVNPropertyValue svnExternalsVaule = properties == null ? null : properties.getSVNPropertyValue(SVNProperty.EXTERNALS);
        if (svnExternalsVaule != null) {
            SVNURL location = repository.getLocation();
            externals.put(location.appendPath(path, false), svnExternalsVaule);
        }

        for (Iterator iterator = entries.iterator(); iterator.hasNext(); ) {
            SVNDirEntry entry = (SVNDirEntry) iterator.next();
            String childPath = SVNPathUtil.append(path, entry.getName());
            entry.setRelativePath(childPath);
            if (entry.getKind() == SVNNodeKind.FILE || depth == SVNDepth.IMMEDIATES || depth == SVNDepth.INFINITY) {
                entry.setExternalParentUrl(externalParentUrl);
                entry.setExternalTarget(externalTarget);
                handler.handleDirEntry(entry);
            }
            if (entry.getKind() == SVNNodeKind.DIR && entry.getDate() != null && depth == SVNDepth.INFINITY) {
                list(repository, childPath, rev, depth, entryFields, externals, externalParentUrl, externalTarget, handler);
            }
        }
    }

    private void listExternals(SVNRepository repository, Map<SVNURL, SVNPropertyValue> externals, SVNDepth depth, int entryFields, boolean fetchLocks, ISVNDirEntryHandler handler) throws SVNException {
        for (Map.Entry<SVNURL, SVNPropertyValue> entry : externals.entrySet()) {
            SVNURL externalParentUrl = entry.getKey();
            SVNPropertyValue externalValue = entry.getValue();

            SVNExternal[] externalItems = SVNExternal.parseExternals(externalParentUrl, SVNPropertyValue.getPropertyAsString(externalValue));
            if (externalItems == null || externalItems.length == 0) {
                continue;
            }
            listExternalItems(repository, externalItems, externalParentUrl, depth, entryFields, fetchLocks, handler);
        }
    }

    private void listExternalItems(SVNRepository repository, SVNExternal[] externalItems, SVNURL externalParentUrl, SVNDepth depth, int entryFields, boolean fetchLocks, ISVNDirEntryHandler handler) throws SVNException {
        SVNURL rootUrl = repository.getRepositoryRoot(true);
        for (SVNExternal externalItem : externalItems) {
            SVNURL resolvedURL = externalItem.resolveURL(rootUrl, externalParentUrl);
            try {
                //TODO: peg revision
                repository.setLocation(resolvedURL, true);
                doList(repository, externalItem.getRevision().getNumber(), handler, fetchLocks, depth, entryFields, externalParentUrl, externalItem.getPath());
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.CANCELLED) {
                    SVNEvent event = SVNEventFactory.createSVNEvent(new File(externalItem.getPath()), SVNNodeKind.UNKNOWN, null, -1, SVNEventAction.FAILED_EXTERNAL, SVNEventAction.FAILED_EXTERNAL, e.getErrorMessage(), null);
                    getOperation().getEventHandler().handleEvent(event, UNKNOWN);
                } else {
                    throw e;
                }
            }
        }
    }
}
