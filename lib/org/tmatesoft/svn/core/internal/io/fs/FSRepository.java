/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.io.fs;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.tmatesoft.svn.core.ISVNDirEntryHandler;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNAuthenticationException;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLock;
import org.tmatesoft.svn.core.SVNMergeInfoInheritance;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNRevisionProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNUserNameAuthentication;
import org.tmatesoft.svn.core.internal.delta.SVNDeltaCombiner;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.SVNMergeInfoManager;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.ISVNFileRevisionHandler;
import org.tmatesoft.svn.core.io.ISVNLocationEntryHandler;
import org.tmatesoft.svn.core.io.ISVNLocationSegmentHandler;
import org.tmatesoft.svn.core.io.ISVNLockHandler;
import org.tmatesoft.svn.core.io.ISVNReplayHandler;
import org.tmatesoft.svn.core.io.ISVNReporter;
import org.tmatesoft.svn.core.io.ISVNReporterBaton;
import org.tmatesoft.svn.core.io.ISVNSession;
import org.tmatesoft.svn.core.io.ISVNWorkspaceMediator;
import org.tmatesoft.svn.core.io.SVNCapability;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSRepository extends SVNRepository implements ISVNReporter {

    private File myReposRootDir;
    private FSUpdateContext myReporterContext;
    private FSLocationsFinder myLocationsFinder;
    private FSFS myFSFS;
    private SVNMergeInfoManager myMergeInfoManager;
    private FSLog myLogDriver;
    private boolean myIsHooksEnabled;
    
    protected FSRepository(SVNURL location, ISVNSession options) {
        super(location, options);
        setHooksEnabled(true);
    }

    public void setHooksEnabled(boolean enabled) {
        myIsHooksEnabled = enabled;
        if (getFSFS() != null) {
            getFSFS().setHooksEnabled(isHooksEnabled());
        }
    }
    
    public boolean isHooksEnabled() {
        return myIsHooksEnabled;
    }

    public FSFS getFSFS() {
        return myFSFS;
    }

    public void testConnection() throws SVNException {
        // try to open and close a repository
        try {
            openRepository();
        } finally {
            closeRepository();
        }
    }

    public File getRepositoryRootDir() {
        return myReposRootDir;
    }

    public int getReposFormat() {
        return myFSFS.getReposFormat();
    }

    public long getLatestRevision() throws SVNException {
        try {
            openRepository();
            return myFSFS.getYoungestRevision();
        } finally {
            closeRepository();
        }
    }

    public long getDatedRevision(Date date) throws SVNException {
        if (date == null) {
            return getLatestRevision();
        }

        try {
            openRepository();
            return myFSFS.getDatedRevision(date);
        } finally {
            closeRepository();
        }
    }

    public SVNProperties getRevisionProperties(long revision, SVNProperties properties) throws SVNException {
        assertValidRevision(revision);
        try {
            openRepository();
            properties = properties == null ? new SVNProperties() : properties;
            properties.putAll(myFSFS.getRevisionProperties(revision));
        } finally {
            closeRepository();
        }
        return properties;
    }

    public void setRevisionPropertyValue(long revision, String propertyName, SVNPropertyValue propertyValue) throws SVNException {
        setRevisionPropertyValue(revision, propertyName, propertyValue, false);
    }

    public void setRevisionPropertyValue(long revision, String propertyName, SVNPropertyValue propertyValue, boolean bypassHooks) throws SVNException {
        setRevisionPropertyValue(revision, propertyName, propertyValue, bypassHooks, bypassHooks);
    }

    public void setRevisionPropertyValue(long revision, String propertyName, SVNPropertyValue propertyValue, boolean bypassPreRevpropHook, boolean bypassPostRevpropHook) throws SVNException {
         assertValidRevision(revision);
        try {
            openRepository();
            FSRepositoryUtil.validateProperty(propertyName, propertyValue);
            String userName = getUserName();
            SVNProperties revProps = myFSFS.getRevisionProperties(revision);
            SVNPropertyValue oldValue = revProps.getSVNPropertyValue(propertyName);
            String action = null;
            if (propertyValue == null) {
                action = FSHooks.REVPROP_DELETE;
            } else if (oldValue == null) {
                action = FSHooks.REVPROP_ADD;
            } else {
                action = FSHooks.REVPROP_MODIFY;
            }

            byte[] bytes = SVNPropertyValue.getPropertyAsBytes(propertyValue);
            if (isHooksEnabled() && FSHooks.isHooksEnabled() && !bypassPreRevpropHook) {
                FSHooks.runPreRevPropChangeHook(myReposRootDir, propertyName, bytes, userName, revision, action);
            }
            myFSFS.setRevisionProperty(revision, propertyName, propertyValue);
            if (isHooksEnabled() && FSHooks.isHooksEnabled() && !bypassPostRevpropHook) {
                FSHooks.runPostRevPropChangeHook(myReposRootDir, propertyName, bytes, userName, revision, action);
            }
        } finally {
            closeRepository();
        }
    }

    public SVNPropertyValue getRevisionPropertyValue(long revision, String propertyName) throws SVNException {
        assertValidRevision(revision);
        if (propertyName == null) {
            return null;
        }
        try {
            openRepository();
            return myFSFS.getRevisionProperties(revision).getSVNPropertyValue(propertyName);
        } finally {
            closeRepository();
        }
    }

    public SVNNodeKind checkPath(String path, long revision) throws SVNException {
        try {
            openRepository();
            if (!SVNRepository.isValidRevision(revision)) {
                revision = myFSFS.getYoungestRevision();
            }
            String repositoryPath = getRepositoryPath(path);
            FSRevisionRoot root = myFSFS.createRevisionRoot(revision);
            return root.checkNodeKind(repositoryPath);
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.FS_NOT_DIRECTORY) {
                return SVNNodeKind.NONE;
            }
            throw e;
        } finally {
            closeRepository();
        }
    }

    public long getFile(String path, long revision, SVNProperties properties, OutputStream contents) throws SVNException {
        try {
            openRepository();
            if (!SVNRepository.isValidRevision(revision)) {
                revision = myFSFS.getYoungestRevision();
            }

            String repositoryPath = getRepositoryPath(path);
            FSRevisionRoot root = myFSFS.createRevisionRoot(revision);

            if (contents != null) {
                InputStream fileStream = null;
                try {
                    fileStream = root.getFileStreamForPath(new SVNDeltaCombiner(), repositoryPath);
                    FSRepositoryUtil.copy(fileStream, contents, getCanceller());
                } finally {
                    SVNFileUtil.closeFile(fileStream);
                }
            }
            if (properties != null) {
                FSRevisionNode revNode = root.getRevisionNode(repositoryPath);
                if (revNode.getFileMD5Checksum() != null) {
                    properties.put(SVNProperty.CHECKSUM, revNode.getFileMD5Checksum());
                }
                if (revision >= 0) {
                    properties.put(SVNProperty.REVISION, Long.toString(revision));
                }
                properties.putAll(collectProperties(revNode));
            }
            return revision;
        } finally {
            closeRepository();
        }
    }

    public long getDir(String path, long revision, SVNProperties properties, ISVNDirEntryHandler handler) throws SVNException {
        return getDir(path, revision, properties, SVNDirEntry.DIRENT_ALL, handler);
    }

    public SVNDirEntry getDir(String path, long revision, boolean includeCommitMessages, Collection entries) throws SVNException {
        try {
            openRepository();
            if (!SVNRepository.isValidRevision(revision)) {
                revision = myFSFS.getYoungestRevision();
            }

            String repositoryPath = getRepositoryPath(path);
            SVNURL parentURL = getLocation().appendPath(path, false);

            FSRevisionRoot root = myFSFS.createRevisionRoot(revision);
            FSRevisionNode parent = root.getRevisionNode(repositoryPath);

            if (entries != null) {
                entries.addAll(getDirEntries(parent, parentURL, SVNDirEntry.DIRENT_ALL));
            }

            SVNDirEntry parentDirEntry = buildDirEntry(new FSEntry(parent.getId(), parent.getType(), ""), parentURL, parent, SVNDirEntry.DIRENT_ALL);
            return parentDirEntry;
        } finally {
            closeRepository();
        }
    }

    public long getDir(String path, long revision, SVNProperties properties, int entryFields, ISVNDirEntryHandler handler) throws SVNException {
        try {
            openRepository();
            if (!SVNRepository.isValidRevision(revision)) {
                revision = myFSFS.getYoungestRevision();
            }
            String repositoryPath = getRepositoryPath(path);
            FSRevisionRoot root = myFSFS.createRevisionRoot(revision);

            FSRevisionNode parent = root.getRevisionNode(repositoryPath);
            if (handler != null) {
                SVNURL parentURL = getLocation().appendPath(path, false);
                Collection entriesCollection = getDirEntries(parent, parentURL, entryFields);
                Iterator entries = entriesCollection.iterator();
                while (entries.hasNext()) {
                    SVNDirEntry entry = (SVNDirEntry) entries.next();
                    handler.handleDirEntry(entry);
                }
            }
            if (properties != null) {
                properties.putAll(collectProperties(parent));
            }
            return revision;
        } finally {
            closeRepository();
        }
    }

    protected int getFileRevisionsImpl(String path, long startRevision, long endRevision, boolean includeMergedRevisions, ISVNFileRevisionHandler handler) throws SVNException {
        try {
            openRepository();
            path = getRepositoryPath(path);
            long latestRevision = INVALID_REVISION;
            if (isInvalidRevision(startRevision)) {
                latestRevision = myFSFS.getYoungestRevision();
                startRevision = latestRevision;
            }

            if (isInvalidRevision(endRevision)) {
                if (isInvalidRevision(latestRevision)) {
                    latestRevision = myFSFS.getYoungestRevision();
                }
                endRevision = latestRevision;
            }

            FSFileRevisionsFinder finder = new FSFileRevisionsFinder(myFSFS);
            int fileRevsNumber = finder.getFileRevisions(path, startRevision, endRevision,
                                                         includeMergedRevisions, handler);
            return fileRevsNumber;
        } finally {
            closeRepository();
        }
    }

    protected long logImpl(String[] targetPaths, long startRevision, long endRevision, boolean
                    discoverChangedPaths, boolean strictNode, long limit,
                    boolean includeMergedRevisions, String[] revPropNames,
                    ISVNLogEntryHandler handler) throws SVNException {
        try {
            openRepository();
            if (targetPaths == null || targetPaths.length == 0) {
                targetPaths = new String[] {""};
            }
            String[] absPaths = new String[targetPaths.length];
            for (int i = 0; i < targetPaths.length; i++) {
                absPaths[i] = getRepositoryPath(targetPaths[i]);
            }
            long youngestRev = myFSFS.getYoungestRevision();

            if (isInvalidRevision(startRevision)) {
                startRevision = youngestRev;
            }
            if (isInvalidRevision(endRevision)) {
                endRevision = youngestRev;
            }

            long histStart = startRevision;
            long histEnd = endRevision;

            if (startRevision > youngestRev) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NO_SUCH_REVISION, "No such revision {0}", String.valueOf(startRevision));
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
            if (endRevision > youngestRev) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NO_SUCH_REVISION, "No such revision {0}", String.valueOf(endRevision));
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }

            boolean isDescendingOrder = startRevision >= endRevision;
            if (isDescendingOrder) {
                histStart = endRevision;
                histEnd = startRevision;
            }

            FSLog logDriver = getLogDriver(absPaths, limit, histStart, histEnd, isDescendingOrder, 
                    discoverChangedPaths, strictNode, includeMergedRevisions, revPropNames, handler);
            return logDriver.runLog();
        } finally {
            closeRepository();
        }
    }

    protected int getLocationsImpl(String path, long pegRevision, long[] revisions, 
            ISVNLocationEntryHandler handler) throws SVNException {
        assertValidRevision(pegRevision);
        for (int i = 0; i < revisions.length; i++) {
            assertValidRevision(revisions[i]);
        }
        try {
            openRepository();
            path = getRepositoryPath(path);
            FSLocationsFinder locationsFinder = getLocationsFinder();
            return locationsFinder.traceNodeLocations(path, pegRevision, revisions, handler);
        } finally {
            closeRepository();
        }
    }

    protected long getLocationSegmentsImpl(String path, long pegRevision, long startRevision, long endRevision, 
            ISVNLocationSegmentHandler handler) throws SVNException {
        try {
            openRepository();
            path = getRepositoryPath(path);
            FSLocationsFinder locationsFinder = getLocationsFinder();
            return locationsFinder.getNodeLocationSegments(path, pegRevision, startRevision, endRevision, handler);
        } finally {
            closeRepository();
        }
    }

    public void replay(long lowRevision, long highRevision, boolean sendDeltas, ISVNEditor editor) throws SVNException {
        try {
            openRepository();
            FSRevisionRoot root = myFSFS.createRevisionRoot(highRevision);
            String basePath = getRepositoryPath("");
            FSRepositoryUtil.replay(myFSFS, root, basePath, lowRevision, sendDeltas, editor);
        } finally {
            closeRepository();
        }
    }

    public SVNDirEntry info(String path, long revision) throws SVNException {
        try {
            openRepository();
            path = getRepositoryPath(path);
            if (FSRepository.isInvalidRevision(revision)) {
                revision = myFSFS.getYoungestRevision();
            }
            FSRevisionRoot root = myFSFS.createRevisionRoot(revision);

            if (root.checkNodeKind(path) == SVNNodeKind.NONE) {
                return null;
            }

            FSRevisionNode revNode = root.getRevisionNode(path);
            String fullPath = getFullPath(path);
            String parentFullPath = "/".equals(path) ? fullPath : SVNPathUtil.removeTail(fullPath);
            SVNURL url = getLocation().setPath(parentFullPath, false);
            String name = "/".equals(path) ? "" : SVNPathUtil.tail(path);
            FSEntry fsEntry = new FSEntry(revNode.getId(), revNode.getType(), name);
            SVNDirEntry entry = buildDirEntry(fsEntry, url, revNode, SVNDirEntry.DIRENT_ALL);
            return entry;
        } finally {
            closeRepository();
        }
    }

    public ISVNEditor getCommitEditor(String logMessage, Map locks, boolean keepLocks, ISVNWorkspaceMediator mediator) throws SVNException {
        try {
            openRepository();
        } catch (SVNException svne) {
            closeRepository();
            throw svne;
        }
        // fetch user name!
        String author = getUserName();
        return new FSCommitEditor(getRepositoryPath(""), logMessage, author, locks, keepLocks, null, myFSFS, this);
    }

    public SVNLock getLock(String path) throws SVNException {
        try {
            openRepository();
            path = getRepositoryPath(path);
            SVNLock lock = myFSFS.getLockHelper(path, false);
            return lock;
        } finally {
            closeRepository();
        }
    }

    public SVNLock[] getLocks(String path) throws SVNException {
        try {
            openRepository();
            path = getRepositoryPath(path);

            File digestFile = myFSFS.getDigestFileFromRepositoryPath(path);
            final ArrayList locks = new ArrayList();
            ISVNLockHandler handler = new ISVNLockHandler() {

                public void handleLock(String path, SVNLock lock, SVNErrorMessage error) throws SVNException {
                    locks.add(lock);
                }

                public void handleUnlock(String path, SVNLock lock, SVNErrorMessage error) throws SVNException {
                }
            };

            myFSFS.walkDigestFiles(digestFile, handler, false);

            return (SVNLock[]) locks.toArray(new SVNLock[locks.size()]);
        } finally {
            closeRepository();
        }
    }

    public void lock(Map pathsToRevisions, String comment, boolean force, ISVNLockHandler handler) throws SVNException {
        lock(pathsToRevisions, comment, force, false, handler);
    }

    public void lock(Map pathsToRevisions, String comment, boolean force, boolean isDAVComment, ISVNLockHandler handler) throws SVNException {
        try {
            openRepository();
            for (Iterator paths = pathsToRevisions.keySet().iterator(); paths.hasNext();) {
                String path = (String) paths.next();
                Long revision = (Long) pathsToRevisions.get(path);
                String reposPath = getRepositoryPath(path);
                long curRevision = (revision == null || isInvalidRevision(revision.longValue())) ? myFSFS.getYoungestRevision() : revision.longValue();
                SVNLock lock = null;
                SVNErrorMessage error = null;
                try {
                    lock = myFSFS.lockPath(reposPath, null, getUserName(), comment, null, curRevision, force, isDAVComment);
                } catch (SVNException svne) {
                    error = svne.getErrorMessage();
                    if (!FSErrors.isLockError(error)) {
                        throw svne;
                    }
                }
                if (handler != null) {
                    handler.handleLock(reposPath, lock, error);
                }
            }
        } finally {
            closeRepository();
        }
    }

    public void unlock(Map pathToTokens, boolean force, ISVNLockHandler handler) throws SVNException {
        try {
            openRepository();
            for (Iterator paths = pathToTokens.keySet().iterator(); paths.hasNext();) {
                String path = (String) paths.next();
                String token = (String) pathToTokens.get(path);
                String reposPath = getRepositoryPath(path);
                SVNErrorMessage error = null;
                try {
                    myFSFS.unlockPath(reposPath, token, getUserName(), force, true);
                } catch (SVNException svne) {
                    error = svne.getErrorMessage();
                    if (!FSErrors.isUnlockError(error)) {
                        throw svne;
                    }
                }
                if (handler != null) {
                    handler.handleUnlock(reposPath, new SVNLock(reposPath, token, null, null, null, null), error);
                }
            }
        } finally {
            closeRepository();
        }
    }

    public void finishReport() throws SVNException {
        try {
            myReporterContext.drive();
        } finally {
            myReporterContext.dispose();
        }
    }

    public void abortReport() throws SVNException {
        if(myReporterContext != null){
            myReporterContext.dispose();
        }
    }

    public void closeSession() {
    }

    public static boolean isInvalidRevision(long revision) {
        return SVNRepository.isInvalidRevision(revision);
    }

    public static boolean isValidRevision(long revision) {
        return SVNRepository.isValidRevision(revision);
    }

    public void setPath(String path, String lockToken, long revision, boolean startEmpty) throws SVNException {
        setPath(path, lockToken, revision, SVNDepth.INFINITY, startEmpty);
    }

    public void deletePath(String path) throws SVNException {
        myReporterContext.writePathInfoToReportFile(path, null, null, SVNRepository.INVALID_REVISION, false, SVNDepth.INFINITY);
    }

    public void linkPath(SVNURL url, String path, String lockToken, long revision, boolean startEmpty) throws SVNException {
        linkPath(url, path, lockToken, revision, SVNDepth.INFINITY, startEmpty);
    }

    public void linkPath(SVNURL url, String path, String lockToken, long revision, SVNDepth depth, boolean startEmpty) throws SVNException {
        assertValidRevision(revision);
        if (depth == SVNDepth.EXCLUDE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.REPOS_BAD_ARGS, "Depth 'exclude' not supported for link");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        
        SVNURL reposRootURL = getRepositoryRoot(false);
        if (url.toDecodedString().indexOf(reposRootURL.toDecodedString()) == -1) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_ILLEGAL_URL, "''{0}''\nis not the same repository as\n''{1}''", new Object[] {
                    url, reposRootURL
            });
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        String reposLinkPath = url.toDecodedString().substring(reposRootURL.toDecodedString().length());
        if ("".equals(reposLinkPath)) {
            reposLinkPath = "/";
        }
        myReporterContext.writePathInfoToReportFile(path, reposLinkPath, lockToken, revision, startEmpty, depth);
    }

    public void setPath(String path, String lockToken, long revision, SVNDepth depth, boolean startEmpty) throws SVNException {
        assertValidRevision(revision);
        myReporterContext.writePathInfoToReportFile(path, null, lockToken, revision, startEmpty, depth);
    }

    public FSTranslateReporter beginReport(long revision, SVNURL url, String target, boolean ignoreAncestry,
            boolean sendTextDeltas, boolean sendCopyFromArgs, SVNDepth depth, ISVNEditor editor) throws SVNException {
        openRepository();
        makeReporterContext(revision, target, url, depth, ignoreAncestry, sendTextDeltas, sendCopyFromArgs, editor);
        return new FSTranslateReporter(this);
    }

    public void update(long revision, String target, SVNDepth depth, boolean sendCopyFromArgs,
            ISVNReporterBaton reporter, ISVNEditor editor) throws SVNException {
        try {
            openRepository();
            makeReporterContext(revision, target, null, depth, false, true, sendCopyFromArgs,
                    editor);
            reporter.report(this);
        } finally {
            closeRepository();
        }
    }

    public void update(SVNURL url, long revision, String target, SVNDepth depth,
            ISVNReporterBaton reporter, ISVNEditor editor) throws SVNException {
        try {
            openRepository();
            makeReporterContext(revision, target, url, depth, true, true, false, editor);
            reporter.report(this);
        } finally {
            closeRepository();
        }
    }

    public void diff(SVNURL url, long targetRevision, long revision, String target, boolean ignoreAncestry,
                     SVNDepth depth, boolean getContents, ISVNReporterBaton reporter, ISVNEditor editor) throws SVNException {
        try {
            openRepository();
            makeReporterContext(targetRevision, target, url, depth, ignoreAncestry, getContents,
                    false, editor);
            reporter.report(this);
        } finally {
            closeRepository();
        }
    }

    public void status(long revision, String target, SVNDepth depth, ISVNReporterBaton reporter,
            ISVNEditor editor) throws SVNException {
        try {
            openRepository();
            makeReporterContext(revision, target, null, depth, false, false, false, editor);
            reporter.report(this);
        } finally {
            closeRepository();
        }
    }

	public boolean hasCapability(SVNCapability capability) throws SVNException {
		if (capability == SVNCapability.DEPTH || 
		        capability == SVNCapability.LOG_REVPROPS ||
				capability == SVNCapability.PARTIAL_REPLAY || 
				capability == SVNCapability.COMMIT_REVPROPS) {
			return true;
		} else if (capability == SVNCapability.ATOMIC_REVPROPS) {
		    return false;		
		} else if (capability == SVNCapability.MERGE_INFO) {		
		    try {
		        getMergeInfoImpl(new String[] { "" }, 0, SVNMergeInfoInheritance.EXPLICIT, false);
		    } catch (SVNException svne) {
		        SVNErrorCode code = svne.getErrorMessage().getErrorCode();
		        if (code == SVNErrorCode.UNSUPPORTED_FEATURE) {
		            return false;
		        } else if (code == SVNErrorCode.FS_NOT_FOUND) {
		            return true;
		        }
		        throw svne;
		    }
		    return true;
		}
		SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN_CAPABILITY, 
				"Don''t know anything about capability ''{0}''", capability);
		SVNErrorManager.error(err, SVNLogType.FSFS);
		return false;
	}

    void closeRepository() throws SVNException {
        if (myFSFS != null) {
            myFSFS.close();
        }
        unlock();
    }

    protected Map getMergeInfoImpl(String[] paths, long revision, SVNMergeInfoInheritance inherit, 
            boolean includeDescendants) throws SVNException {
        try {
            openRepository();
            if (!isValidRevision(revision)) {
                revision = myFSFS.getYoungestRevision();
            }
            FSRevisionRoot root = myFSFS.createRevisionRoot(revision);
            String[] absPaths = new String[paths.length];
            for (int i = 0; i < paths.length; i++) {
                absPaths[i] = getRepositoryPath(paths[i]);
            }
            SVNMergeInfoManager mergeInfoManager = getMergeInfoManager();
            return mergeInfoManager.getMergeInfo(absPaths, root, inherit, includeDescendants);
        } finally {
            closeRepository();
        }
    }

    protected ISVNEditor getCommitEditorInternal(Map locks, boolean keepLocks, SVNProperties revProps, ISVNWorkspaceMediator mediator) throws SVNException {
        try {
            openRepository();
        } catch (SVNException svne) {
            closeRepository();
            throw svne;
        }
        revProps = revProps == null ? new SVNProperties() : revProps;
        if (!revProps.containsName(SVNRevisionProperty.AUTHOR)) {
            revProps.put(SVNRevisionProperty.AUTHOR, getUserName());
        }
        return new FSCommitEditor(getRepositoryPath(""), locks, keepLocks, null, myFSFS, this, revProps);
    }

    protected void replayRangeImpl(long startRevision, long endRevision, long lowRevision, boolean sendDeltas, 
            ISVNReplayHandler handler) throws SVNException {
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_NOT_IMPLEMENTED);
        SVNErrorManager.error(err, SVNLogType.FSFS);
    }

    protected long getDeletedRevisionImpl(String path, long pegRevision, long endRevision) throws SVNException {
        try {
            openRepository();
            path = getRepositoryPath(path);
            return myFSFS.getDeletedRevision(path, pegRevision, endRevision);
        } finally {
            closeRepository();
        }
    }

    private void openRepository() throws SVNException {
        try {
            openRepositoryRoot();
        } catch (SVNException svne) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_LOCAL_REPOS_OPEN_FAILED, "Unable to open repository ''{0}''", getLocation().toString());
            err.setChildErrorMessage(svne.getErrorMessage());
            SVNErrorManager.error(err.wrap("Unable to open an ra_local session to URL"), SVNLogType.FSFS);
        }
    }

    private void openRepositoryRoot() throws SVNException {
        lock();

        String hostName = getLocation().getHost();
        boolean hasCustomHostName = !"".equals(hostName) &&
                                    !"localhost".equalsIgnoreCase(hostName);

        if (!SVNFileUtil.isWindows && hasCustomHostName) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_ILLEGAL_URL, "Local URL ''{0}'' contains unsupported hostname", getLocation().toString());
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        String startPath = SVNEncodingUtil.uriDecode(getLocation().getURIEncodedPath());
        String rootPath = FSFS.findRepositoryRoot(hasCustomHostName ? hostName : null, startPath);
        if (rootPath == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_LOCAL_REPOS_OPEN_FAILED, "Unable to open repository ''{0}''", getLocation().toString());
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        
        String dirPath = rootPath.replaceFirst("\\|", "\\:");

        myReposRootDir = hasCustomHostName ? new File("\\\\" + hostName, dirPath).getAbsoluteFile() :
                                             new File(dirPath).getAbsoluteFile();
        myFSFS = new FSFS(myReposRootDir);
        myFSFS.setHooksEnabled(isHooksEnabled());
        myFSFS.open();
        setRepositoryCredentials(myFSFS.getUUID(), getLocation().setPath(rootPath, false));
    }

    private Collection getDirEntries(FSRevisionNode parent, SVNURL parentURL, int entryFields) throws SVNException {
        Map entries = parent.getDirEntries(myFSFS);
        Set keys = entries.keySet();
        Iterator dirEntries = keys.iterator();
        Collection dirEntriesList = new LinkedList();
        while (dirEntries.hasNext()) {
            String name = (String) dirEntries.next();
            FSEntry repEntry = (FSEntry) entries.get(name);
            if (repEntry != null) {
                dirEntriesList.add(buildDirEntry(repEntry, parentURL, null, entryFields));
            }
        }
        return dirEntriesList;
    }

    private SVNProperties collectProperties(FSRevisionNode revNode) throws SVNException {
        SVNProperties properties = new SVNProperties();
        SVNProperties versionedProps = revNode.getProperties(myFSFS);
        if (versionedProps != null && versionedProps.size() > 0) {
            properties.putAll(versionedProps);
        }
        SVNProperties metaprops = null;
        try {
            metaprops = myFSFS.compoundMetaProperties(revNode.getCreatedRevision());
        } catch (SVNException svne) {
            //
        }
        if (metaprops != null && metaprops.size() > 0) {
            properties.putAll(metaprops);
        }
        return properties;
    }

    private SVNDirEntry buildDirEntry(FSEntry repEntry, SVNURL parentURL, FSRevisionNode entryNode, int entryFields) throws SVNException {
        entryNode = entryNode == null ? myFSFS.getRevisionNode(repEntry.getId()) : entryNode;

        SVNNodeKind kind = null;
        if ((entryFields & SVNDirEntry.DIRENT_KIND) != 0) {
            kind = repEntry.getType();
        }

        long size = 0;
        if ((entryFields & SVNDirEntry.DIRENT_SIZE) != 0) {
            if (entryNode.getType() == SVNNodeKind.FILE) {
                size = entryNode.getFileLength();
            }
        }

        boolean hasProps = false;
        if ((entryFields & SVNDirEntry.DIRENT_HAS_PROPERTIES) != 0) {
            SVNProperties props = entryNode.getProperties(myFSFS);
            hasProps = props != null && props.size() > 0;
        }

        String lastAuthor = null;
        String log = null;
        Date lastCommitDate = null;
        long revision = SVNRepository.INVALID_REVISION;
        if ((entryFields & SVNDirEntry.DIRENT_TIME) != 0 ||
                (entryFields & SVNDirEntry.DIRENT_LAST_AUTHOR) != 0 ||
                (entryFields & SVNDirEntry.DIRENT_CREATED_REVISION) != 0 ||
                (entryFields & SVNDirEntry.DIRENT_COMMIT_MESSAGE) != 0) {
            revision = repEntry.getId().getRevision();
            SVNProperties revProps = myFSFS.getRevisionProperties(repEntry.getId().getRevision());
            if (revProps != null && revProps.size() > 0) {
                lastAuthor = revProps.getStringValue(SVNRevisionProperty.AUTHOR);
                log = revProps.getStringValue(SVNRevisionProperty.LOG);
                String timeString = revProps.getStringValue(SVNRevisionProperty.DATE);
                lastCommitDate = timeString != null ? SVNDate.parseDateString(timeString) : null;
            }
        }

        SVNURL entryURL = parentURL.appendPath(repEntry.getName(), false);
        SVNDirEntry dirEntry = new SVNDirEntry(entryURL, getRepositoryRoot(false), repEntry.getName(), kind, size, hasProps, revision, lastCommitDate, lastAuthor, log);
        dirEntry.setRelativePath(repEntry.getName());
        return dirEntry;
    }

    private void makeReporterContext(long targetRevision, String target, SVNURL switchURL,
            SVNDepth depth, boolean ignoreAncestry, boolean textDeltas, boolean sendCopyFromArgs,
            ISVNEditor editor) throws SVNException {
        if (depth == SVNDepth.EXCLUDE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.REPOS_BAD_ARGS, "Request depth 'exclude' not supported");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        
        target = target == null ? "" : target;

        if (!isValidRevision(targetRevision)) {
            targetRevision = myFSFS.getYoungestRevision();
        }

        String switchPath = null;

        if (switchURL != null) {
            SVNURL reposRootURL = getRepositoryRoot(false);

            if (switchURL.toDecodedString().indexOf(reposRootURL.toDecodedString()) == -1) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_ILLEGAL_URL, "''{0}''\nis not the same repository as\n''{1}''", new Object[] {
                        switchURL, getRepositoryRoot(false)
                });
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }

            switchPath = switchURL.toDecodedString().substring(reposRootURL.toDecodedString().length());

            if ("".equals(switchPath)) {
                switchPath = "/";
            }
        }

        String anchor = getRepositoryPath("");
        String fullTargetPath = switchPath != null ? switchPath : SVNPathUtil.getAbsolutePath(SVNPathUtil.append(anchor, target));

        if (myReporterContext == null) {
            myReporterContext = new FSUpdateContext(this, myFSFS, targetRevision,
                                                    SVNFileUtil.createTempFile("report", ".tmp"),
                                                    target, fullTargetPath,
                                                    switchURL == null ? false : true,
                                                    depth, ignoreAncestry, textDeltas,
                                                    sendCopyFromArgs, editor);
        } else {
            myReporterContext.reset(this, myFSFS, targetRevision, SVNFileUtil.createTempFile("report", ".tmp"),
                                    target, fullTargetPath, switchURL == null ? false : true, depth,
                                    ignoreAncestry, textDeltas, sendCopyFromArgs, editor);
        }
    }

    private String getUserName() throws SVNException {
        if (getLocation().getUserInfo() != null && getLocation().getUserInfo().trim().length() > 0) {
            return getLocation().getUserInfo();
        }
        if (getAuthenticationManager() != null) {
            try {
                String realm = getRepositoryUUID(true);
                ISVNAuthenticationManager authManager = getAuthenticationManager();
                SVNAuthentication auth = authManager.getFirstAuthentication(ISVNAuthenticationManager.USERNAME, realm, getLocation());

                while (auth != null) {
                    String userName = auth.getUserName();
                    if (userName == null) {
                        // anonymous.
                        return null;
                    }
                    if ("".equals(userName.trim())) {
                        userName = System.getProperty("user.name");
                    }
                    auth = new SVNUserNameAuthentication(userName, auth.isStorageAllowed(), getLocation(), false);
                    if (userName != null && !"".equals(userName.trim())) {
                        BasicAuthenticationManager.acknowledgeAuthentication(true, ISVNAuthenticationManager.USERNAME, realm, null, auth, myLocation, authManager);
                        return auth.getUserName();
                    }
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.AUTHN_CREDS_UNAVAILABLE, "Empty user name is not allowed");
                    BasicAuthenticationManager.acknowledgeAuthentication(false, ISVNAuthenticationManager.USERNAME, realm, err, auth, myLocation, authManager);
                    auth = authManager.getNextAuthentication(ISVNAuthenticationManager.USERNAME, realm, getLocation());
                }
                // auth manager returned null - that is cancellation.
                SVNErrorManager.cancel("Authentication cancelled", SVNLogType.FSFS);
            } catch (SVNCancelException e) {
                throw e;
            } catch (SVNAuthenticationException e) {
                // no more credentials, use system user name.
            } catch (SVNException e) {
                // generic error.
                throw e;
            }
        }
        // anonymous
        return null;
    }

    private FSLocationsFinder getLocationsFinder() {
        if (myLocationsFinder == null) {
            myLocationsFinder = new FSLocationsFinder(getFSFS());
        } else {
            myLocationsFinder.reset(getFSFS());
        }

        return myLocationsFinder;
    }

    private SVNMergeInfoManager getMergeInfoManager() {
        if (myMergeInfoManager == null) {
            myMergeInfoManager = new SVNMergeInfoManager();
        }
        return myMergeInfoManager;
    }

    private FSLog getLogDriver(String[] absPaths, long limit, long histStart, long histEnd, 
            boolean isDescendingOrder, boolean discoverChangedPaths, boolean strictNode, 
            boolean includeMergedRevisions, String[] revPropNames, ISVNLogEntryHandler handler) {
        if (myLogDriver == null) {
            myLogDriver = new FSLog(myFSFS, absPaths, limit, histStart, histEnd, isDescendingOrder, 
                    discoverChangedPaths, strictNode, includeMergedRevisions, revPropNames, handler);
        } else {
            myLogDriver.reset(myFSFS, absPaths, limit, histStart, histEnd, isDescendingOrder, 
                    discoverChangedPaths, strictNode, includeMergedRevisions, revPropNames, handler);
        }
        return myLogDriver;
    }

}