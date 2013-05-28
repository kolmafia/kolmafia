package org.tmatesoft.svn.core.internal.wc2.ng;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.util.SVNURLUtil;
import org.tmatesoft.svn.core.internal.wc.*;
import org.tmatesoft.svn.core.internal.wc17.*;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.SVNWCNodeReposInfo;
import org.tmatesoft.svn.core.internal.wc17.db.*;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.ExternalNodeInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeInfo;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess.RepositoryInfo;
import org.tmatesoft.svn.core.io.SVNCapability;
import org.tmatesoft.svn.core.io.SVNLocationSegment;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.AbstractSvnUpdate;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnRelocate;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SvnNgAbstractUpdate<V, T extends AbstractSvnUpdate<V>> extends SvnNgOperationRunner<V, T> {

    protected long update(SVNWCContext wcContext, File localAbspath, SVNRevision revision, SVNDepth depth, boolean depthIsSticky, boolean ignoreExternals, boolean allowUnversionedObstructions, boolean addsAsMoodifications, boolean makeParents, boolean innerUpdate, boolean sleepForTimestamp) throws SVNException {
        
        assert ! (innerUpdate && makeParents);
        
        File lockRootPath = null;
        File anchor;
        try {
            if (makeParents) {
                File parentPath = localAbspath;
                List<File> missingParents = new ArrayList<File>();
                while(true) {
                    try {
                        lockRootPath = getWcContext().acquireWriteLock(parentPath, !innerUpdate, true);
                        break;
                    } catch (SVNException e) {
                        if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_NOT_WORKING_COPY || SVNFileUtil.getParentFile(parentPath) == null) {
                            throw e;
                        }
                    }
                    parentPath = SVNFileUtil.getParentFile(parentPath);
                    missingParents.add(0, parentPath);
                }
                anchor = lockRootPath;
                for (File missingParent : missingParents) {
                    long revnum = updateInternal(
                            wcContext,
                            missingParent,
                            anchor, 
                            revision, 
                            SVNDepth.EMPTY, 
                            false, 
                            ignoreExternals, 
                            allowUnversionedObstructions,
                            addsAsMoodifications, 
                            sleepForTimestamp, 
                            false);
                    anchor = missingParent;
                    revision = SVNRevision.create(revnum);
                }
            } else {
                anchor = wcContext.acquireWriteLock(localAbspath, !innerUpdate, true);
                lockRootPath = anchor;
            }
            
            return updateInternal(wcContext, localAbspath, anchor, revision, depth, depthIsSticky, ignoreExternals, allowUnversionedObstructions, addsAsMoodifications, sleepForTimestamp, true);
            
        } finally {
            if (lockRootPath != null) {
                wcContext.releaseWriteLock(lockRootPath);
            }
        }
    }

    protected long updateInternal(SVNWCContext wcContext, File localAbspath, File anchorAbspath, SVNRevision revision, SVNDepth depth, boolean depthIsSticky, boolean ignoreExternals, boolean allowUnversionedObstructions, boolean addsAsMoodifications, boolean sleepForTimestamp, boolean notifySummary) throws SVNException {
        
        if (depth == SVNDepth.UNKNOWN) {
            depthIsSticky = false;
        }
        
        String target;
        if (!localAbspath.equals(anchorAbspath)) {
            target = SVNFileUtil.getFileName(localAbspath);
        } else {
            target = "";
        }
        final SVNURL anchorUrl = wcContext.getNodeUrl(anchorAbspath);
    
        if (anchorUrl == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_MISSING_URL, "'{0}' has no URL", anchorAbspath);
            SVNErrorManager.error(err, SVNLogType.WC);
            return SVNWCContext.INVALID_REVNUM;
        }
        
        long baseRevision = wcContext.getNodeBaseRev(anchorAbspath);
        SVNWCContext.ConflictInfo conflictInfo;
        boolean treeConflict = false;
        try {
            conflictInfo = wcContext.getConflicted(localAbspath, false, false, true);
            treeConflict = conflictInfo != null && conflictInfo.treeConflicted;
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                throw e;
            }
            treeConflict = false;
        }
        if (baseRevision == SVNWCContext.INVALID_REVNUM || treeConflict) {
            if (wcContext.getEventHandler() != null) {
                handleEvent(SVNEventFactory.createSVNEvent(localAbspath, SVNNodeKind.NONE, null, -1, 
                        treeConflict ? SVNEventAction.SKIP_CONFLICTED : SVNEventAction.UPDATE_SKIP_WORKING_ONLY, null, null, null, 0, 0));
                
            }
            return SVNWCContext.INVALID_REVNUM;
        }
        if (depthIsSticky && depth.compareTo(SVNDepth.INFINITY) < 0) {
            if (depth == SVNDepth.EXCLUDE) {
                wcContext.exclude(localAbspath);
                return SVNWCContext.INVALID_REVNUM;
            }
            final SVNNodeKind targetKind = wcContext.readKind(localAbspath, true);
            if (targetKind == SVNNodeKind.DIR) {
                wcContext.cropTree(localAbspath, depth);
            }
        }
    
        String[] preservedExts = getOperation().getOptions().getPreservedConflictFileExtensions();
        boolean useCommitTimes = getOperation().getOptions().isUseCommitTimes();
    
        if (notifySummary) {
            handleEvent(SVNEventFactory.createSVNEvent(localAbspath, SVNNodeKind.NONE, null, -1, SVNEventAction.UPDATE_STARTED, null, null, null, 0, 0));
        }
        boolean cleanCheckout = isEmptyWc(localAbspath, anchorAbspath);
        
        SVNRepository repos = getRepositoryAccess().createRepository(anchorUrl, anchorAbspath);
        boolean serverSupportsDepth = repos.hasCapability(SVNCapability.DEPTH);
        final SVNReporter17 reporter = new SVNReporter17(localAbspath, wcContext, true, !serverSupportsDepth, depth, 
                getOperation().isUpdateLocksOnDemand(), false, !depthIsSticky, useCommitTimes, null);
        final long revNumber = getWcContext().getRevisionNumber(revision, null, repos, localAbspath);
        final SVNURL reposRoot = repos.getRepositoryRoot(true);
        
    
        final SVNRepository[] repos2 = new SVNRepository[1];
        ISVNDirFetcher dirFetcher = new ISVNDirFetcher() {
            public Map<String, SVNDirEntry> fetchEntries(SVNURL reposRoot, File path) throws SVNException {
                SVNURL url = SVNWCUtils.join(reposRoot, path);
                if (repos2[0] == null) {
                    repos2[0] = getRepositoryAccess().createRepository(url, null, false);
                } else {
                    repos2[0].setLocation(url, false);
                }
                
                final Map<String, SVNDirEntry> entries = new HashMap<String, SVNDirEntry>();
                if (repos2[0].checkPath("", revNumber) == SVNNodeKind.DIR) {
                    repos2[0].getDir("", revNumber, null, new ISVNDirEntryHandler() {
                        public void handleDirEntry(SVNDirEntry dirEntry) throws SVNException {
                            if (dirEntry.getName() != null && !"".equals(dirEntry.getName())) {
                                entries.put(dirEntry.getName(), dirEntry);
                            }
                        }
                    });
                    return entries;
                } 
                return null;
            }
        };
    
        SVNExternalsStore externalsStore = new SVNExternalsStore();
        ISVNUpdateEditor editor = SVNUpdateEditor17.createUpdateEditor(wcContext, revNumber, 
                anchorAbspath, target, useCommitTimes,
                null,
                depth, 
                depthIsSticky,
                getOperation().isAllowUnversionedObstructions(),
                true,
                serverSupportsDepth,
                cleanCheckout,
                dirFetcher,
                externalsStore,
                preservedExts);
                
        try {
            repos.update(revNumber, target, depthIsSticky ? depth : SVNDepth.UNKNOWN, false, reporter, editor);
        } catch(SVNException e) {
            sleepForTimestamp();
            throw e;
        } finally {
            if (repos2[0] != null) {
                repos2[0].closeSession();
            }
        }
        
        long targetRevision = editor.getTargetRevision();
        
        if (targetRevision >= 0) {
            if ((depth == SVNDepth.INFINITY || depth == SVNDepth.UNKNOWN) && !getOperation().isIgnoreExternals()) {
                getWcContext().getDb().gatherExternalDefinitions(localAbspath, externalsStore);
                handleExternals(externalsStore.getNewExternals(), externalsStore.getDepths(), anchorUrl, localAbspath, reposRoot, depth, false);
            }
            if (sleepForTimestamp) {
                sleepForTimestamp();
            }
            if (notifySummary) {
                handleEvent(SVNEventFactory.createSVNEvent(localAbspath, SVNNodeKind.NONE, null, targetRevision, SVNEventAction.UPDATE_COMPLETED, null, null, null, reporter.getReportedFilesCount(),
                        reporter.getTotalFilesCount()));
            }
        }
        return targetRevision;
    }

    protected void handleExternals(Map<File, String> newExternals, Map<File, SVNDepth> ambientDepths, SVNURL anchorUrl, File targetAbspath, SVNURL reposRoot, SVNDepth requestedDepth, boolean sleepForTimestamp) throws SVNException {
        Map<File, File> oldExternals = getWcContext().getDb().getExternalsDefinedBelow(targetAbspath);
        
        for (File externalPath : newExternals.keySet()) {
            String externalDefinition = newExternals.get(externalPath);
            SVNDepth ambientDepth = SVNDepth.INFINITY;
            if (ambientDepths != null) {
                ambientDepth = ambientDepths.get(externalPath);
            }
            handleExternalsChange(reposRoot, externalPath, externalDefinition, oldExternals, ambientDepth, requestedDepth);
        }
        
        for(File oldExternalPath : oldExternals.keySet()) {
           File definingAbsPath = oldExternals.get(oldExternalPath);
           try {
               handleExternalItemRemoval(definingAbsPath, oldExternalPath);
           } catch (SVNCancelException cancel) {
               throw cancel;
           } catch (SVNException e) {
               handleEvent(SVNEventFactory.createSVNEvent(oldExternalPath, SVNNodeKind.NONE, null, -1, SVNEventAction.FAILED_EXTERNAL, SVNEventAction.UPDATE_EXTERNAL_REMOVED, 
                       e.getErrorMessage(), null));
           }
        }
    }


    private void handleExternalItemRemoval(File definingAbsPath, File localAbsPath) throws SVNException {
        SVNNodeKind kind = getWcContext().readKind(localAbsPath, false);
        if (kind == SVNNodeKind.NONE) {
            SvnWcDbExternals.removeExternalNode(getWcContext(), localAbsPath, definingAbsPath, null);
            return;
        }
        File lockRootAbsPath = null;
        boolean lockedHere = getWcContext().getDb().isWCLockOwns(localAbsPath, false);
        if (!lockedHere) {
            lockRootAbsPath = getWcContext().acquireWriteLock(localAbsPath, false, true);
        }
        SVNErrorMessage err = null;
        try {
            SvnWcDbExternals.removeExternal(getWcContext(), definingAbsPath, localAbsPath);
        } catch (SVNException e) {
            err = e.getErrorMessage();
        } 
        handleEvent(SVNEventFactory.createSVNEvent(localAbsPath, SVNNodeKind.NONE, null, -1, SVNEventAction.UPDATE_EXTERNAL_REMOVED, SVNEventAction.UPDATE_EXTERNAL_REMOVED, err, null, 1, 1));
        if (lockRootAbsPath != null) {
            try {
                getWcContext().releaseWriteLock(lockRootAbsPath);
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_NOT_LOCKED) {
                    throw e;
                }
            }
        }
        if (err != null && err.getErrorCode() == SVNErrorCode.WC_LEFT_LOCAL_MOD) {
            err = null;
        }
        if (err != null) {
            SVNErrorManager.error(err, SVNLogType.WC);
        }
    }

    private void handleExternalsChange(SVNURL reposRoot, File externalPath, String externalDefinition, Map<File, File> oldExternals, SVNDepth ambientDepth, SVNDepth requestedDepth) throws SVNException {
        if ((requestedDepth.compareTo(SVNDepth.INFINITY) < 0 && requestedDepth != SVNDepth.UNKNOWN) ||
                ambientDepth.compareTo(SVNDepth.INFINITY) < 0 && requestedDepth.compareTo(SVNDepth.INFINITY) < 0) {
            return;
        }
        if (externalDefinition != null) {
            SVNExternal[] externals = SVNExternal.parseExternals(externalPath, externalDefinition);
            SVNURL url = getWcContext().getNodeUrl(externalPath);
            for (int i = 0; i < externals.length; i++) {
                File targetAbsPath = SVNFileUtil.createFilePath(externalPath, externals[i].getPath());
                File oldExternalDefiningPath = oldExternals.get(targetAbsPath);
                try {
                    handleExternalItemChange(reposRoot, externalPath, url, targetAbsPath, oldExternalDefiningPath, externals[i]);
                } catch (SVNCancelException cancel) {
                    throw cancel;
                } catch (SVNException e) {
                    handleEvent(SVNEventFactory.createSVNEvent(targetAbsPath, SVNNodeKind.NONE, null, -1, SVNEventAction.FAILED_EXTERNAL, SVNEventAction.UPDATE_EXTERNAL, 
                            e.getErrorMessage(), null));
                }
                if (oldExternalDefiningPath != null) {
                    oldExternals.remove(targetAbsPath);
                }
            }
        }
    }

    private void handleExternalItemChange(SVNURL rootUrl, File parentPath, SVNURL parentUrl, File localAbsPath, File oldDefiningPath, SVNExternal newItem) throws SVNException {
        assert newItem != null;
        assert rootUrl != null && parentUrl != null;

        SVNURL newUrl = newItem.resolveURL(rootUrl, parentUrl);
        newUrl = SvnTarget.fromURL(newUrl).getURL();

        SVNRevision externalRevision  = newItem.getRevision();
        SVNRevision externalPegRevision = newItem.getPegRevision();

        if (getOperation().getExternalsHandler() != null) {
            SVNRevision[] revs = getOperation().getExternalsHandler().handleExternal(localAbsPath, newUrl, 
                    externalRevision, externalPegRevision, newItem.getRawValue(), 
                    SVNRevision.UNDEFINED);
            
            if (revs == null) {
                handleEvent(SVNEventFactory.createSVNEvent(localAbsPath, SVNNodeKind.DIR, null, SVNRepository.INVALID_REVISION, SVNEventAction.SKIP, SVNEventAction.UPDATE_EXTERNAL, null, null));
                return;
            }
            externalRevision = revs.length > 0 && revs[0] != null ? revs[0] : externalRevision;
            externalPegRevision = revs.length > 1 && revs[1] != null ? revs[1] : externalPegRevision;
        }

        Structure<RepositoryInfo> repositoryInfo = getRepositoryAccess().createRepositoryFor(SvnTarget.fromURL(newUrl), externalRevision, externalPegRevision, null);
        SVNRepository repository = repositoryInfo.<SVNRepository>get(RepositoryInfo.repository);
        long externalRevnum = repositoryInfo.lng(RepositoryInfo.revision);
        repositoryInfo.release();

        String repositoryUUID = repository.getRepositoryUUID(true);
        SVNURL repositoryRoot = repository.getRepositoryRoot(true);
        SVNNodeKind externalKind = repository.checkPath("", externalRevnum);
        
        if (externalKind == SVNNodeKind.NONE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_ILLEGAL_URL, "URL ''{0}'' at revision {1} doesn''t exist",
                    repository.getLocation(), externalRevnum);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        if (externalKind != SVNNodeKind.DIR && externalKind != SVNNodeKind.FILE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_ILLEGAL_URL, "URL ''{0}'' at revision {1} is not a file or a directory",
                    repository.getLocation(), externalRevnum);
            SVNErrorManager.error(err, SVNLogType.WC);
        }

        SVNNodeKind localKind = externalKind;

        handleEvent(SVNEventFactory.createSVNEvent(localAbsPath, externalKind, null, -1, SVNEventAction.UPDATE_EXTERNAL, null, null, null, 0, 0));
        if (oldDefiningPath == null) {
            SVNFileUtil.ensureDirectoryExists(SVNFileUtil.getParentFile(localAbsPath));
        }
        if (localKind == SVNNodeKind.DIR) {
            switchDirExternal(localAbsPath, newUrl, externalRevision, externalPegRevision, parentPath);
        } else if (localKind == SVNNodeKind.FILE) {
            if (!repositoryRoot.equals(rootUrl)) {
                SVNWCNodeReposInfo localReposInfo = getWcContext().getNodeReposInfo(parentPath);
                SVNURL localReposRootUrl = localReposInfo.reposRootUrl;
                String localReposUuid = localReposInfo.reposUuid;

                String externalRepositoryPath = SVNURLUtil.getRelativeURL(repositoryRoot, newUrl, false);

                if  (localReposUuid == null || localReposRootUrl == null ||
                        externalRepositoryPath == null || !localReposUuid.equals(repositoryUUID)) {
                    SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "Unsupported external: url of " +
                            "file external ''{0}'' is not in repository ''{0}''", newUrl, rootUrl);
                    SVNErrorManager.error(errorMessage, SVNLogType.WC);
                }

                newUrl = localReposRootUrl.appendPath(externalRepositoryPath, false);
                Structure<RepositoryInfo> repositoryInfoStructure = getRepositoryAccess().createRepositoryFor(SvnTarget.fromURL(newUrl), newItem.getRevision(), newItem.getPegRevision(), null);
                repository = repositoryInfoStructure.get(RepositoryInfo.repository);
                externalRevnum = repositoryInfoStructure.lng(RepositoryInfo.revision);
            }
            switchFileExternal(localAbsPath, newUrl, externalPegRevision, externalRevision, parentPath, repository, externalRevnum, repository.getRepositoryRoot(true));
        } else {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.ASSERTION_FAIL);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }
    }

    private void switchDirExternal(File localAbsPath, SVNURL url, SVNRevision revision, SVNRevision pegRevision, File definingPath) throws SVNException {
        SVNFileType fileKind = SVNFileType.getType(localAbsPath);
        if (fileKind == SVNFileType.DIRECTORY) {
            SVNURL nodeUrl = null;
            try {
                nodeUrl = getWcContext().getNodeUrl(localAbsPath);
                if (nodeUrl != null) {
                    if (url.equals(nodeUrl)) {
                        update(getWcContext(), localAbsPath, revision, SVNDepth.UNKNOWN, false, false, false, true, false, true, false);
                        return;
                    }
                    SVNWCNodeReposInfo nodeRepositoryInfo = getWcContext().getNodeReposInfo(localAbsPath);
                    if (nodeRepositoryInfo != null && nodeRepositoryInfo.reposRootUrl != null) {
                        if (!SVNURLUtil.isAncestor(nodeRepositoryInfo.reposRootUrl, url)) {
                            SvnRelocate relocate = getOperation().getOperationFactory().createRelocate();
                            relocate.setToUrl(nodeRepositoryInfo.reposRootUrl);
                            relocate.setFromUrl(nodeUrl);
                            relocate.setSingleTarget(SvnTarget.fromFile(localAbsPath));
                            try {
                                relocate.run();
                            } catch (SVNException e) {
                                if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_INVALID_RELOCATION 
                                        || e.getErrorMessage().getErrorCode() == SVNErrorCode.CLIENT_INVALID_RELOCATION) {
                                    relegateExternal(localAbsPath, url, revision, pegRevision, definingPath, fileKind);
                                    return;
                                }
                                throw e;
                            }
                        }
                        doSwitch(localAbsPath, url, revision, pegRevision, SVNDepth.INFINITY, true, false, false, true, false);
                        getWcContext().getDb().registerExternal(definingPath, localAbsPath, SVNNodeKind.DIR, 
                                nodeRepositoryInfo.reposRootUrl, nodeRepositoryInfo.reposUuid, 
                                SVNFileUtil.createFilePath(SVNPathUtil.getPathAsChild(nodeRepositoryInfo.reposRootUrl.getPath(), url.getPath())), 
                                SVNWCContext.INVALID_REVNUM, 
                                SVNWCContext.INVALID_REVNUM);
                        return;
                    }
                }
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                    throw e;
                }
            }
        }
        
        relegateExternal(localAbsPath, url, revision, pegRevision, definingPath, fileKind);
    }

    private void relegateExternal(File localAbsPath, SVNURL url, SVNRevision revision, SVNRevision pegRevision, File definingPath, SVNFileType fileKind) throws SVNException {
        if (fileKind == SVNFileType.DIRECTORY) {
            getWcContext().acquireWriteLock(localAbsPath, false, false);
            relegateExternalDir(definingPath, localAbsPath);
        } else {
            SVNFileUtil.ensureDirectoryExists(localAbsPath);
        }
        checkout(url, localAbsPath, pegRevision, revision, SVNDepth.INFINITY, false, false, false);
        
        SVNWCNodeReposInfo nodeRepositoryInfo = getWcContext().getNodeReposInfo(localAbsPath);
        getWcContext().getDb().registerExternal(definingPath, localAbsPath, SVNNodeKind.DIR, 
                nodeRepositoryInfo.reposRootUrl, nodeRepositoryInfo.reposUuid, 
                SVNFileUtil.createFilePath(SVNPathUtil.getPathAsChild(nodeRepositoryInfo.reposRootUrl.getPath(), url.getPath())), 
                SVNWCContext.INVALID_REVNUM, 
                SVNWCContext.INVALID_REVNUM);
    }

    private void relegateExternalDir(File wriAbsPath, File localAbsPath) throws SVNException {
        try {
            SvnWcDbExternals.removeExternal(getWcContext(), wriAbsPath, localAbsPath);
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_LEFT_LOCAL_MOD) {
                File oldName = SVNFileUtil.createUniqueFile(localAbsPath.getParentFile(), localAbsPath.getName(), ".OLD", false);
                SVNFileUtil.deleteFile(oldName);
                SVNFileUtil.rename(localAbsPath, oldName);
                return;
            }
            throw e;
        }
    }
    
    private void switchFileExternal(File localAbsPath, SVNURL url, SVNRevision pegRevision, SVNRevision revision, File defDirAbspath, SVNRepository repository, long repositoryRevision, SVNURL reposRootUrl) throws SVNException {
        File dirAbspath = SVNFileUtil.getParentFile(localAbsPath);
        boolean lockedHere = getWcContext().getDb().isWCLockOwns(dirAbspath, false); 
        if (!lockedHere) {
            File wcRoot = getWcContext().getDb().getWCRoot(dirAbspath);
            String rootPath = wcRoot.getAbsolutePath().replace(File.separatorChar, '/');
            String defPath = defDirAbspath.getAbsolutePath().replace(File.separatorChar, '/');
            if (!SVNPathUtil.isAncestor(rootPath, defPath)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_BAD_PATH, 
                        "Cannot insert a file external defined on ''{0}'' into the working copy ''{1}''", defDirAbspath, wcRoot);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        }
        SVNNodeKind kind = SVNNodeKind.NONE;
        SVNNodeKind externalKind = SVNNodeKind.NONE;
        
        try {
            kind = getWcContext().readKind(localAbsPath, false);
            Structure<ExternalNodeInfo> externalInfo = null;
            try {
                externalInfo = SvnWcDbExternals.readExternal(getWcContext(), localAbsPath, localAbsPath, ExternalNodeInfo.kind);
                ISVNWCDb.SVNWCDbKind eKind = externalInfo.<ISVNWCDb.SVNWCDbKind>get(ExternalNodeInfo.kind);
                if (eKind != null) {
                    externalKind = eKind.toNodeKind();
                }
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                    throw e;
                }
            } finally {
                if (externalInfo != null) {
                    externalInfo.release();
                }
            }
        } catch (SVNException e) {
            if (!lockedHere) {
                getWcContext().releaseWriteLock(dirAbspath);
            }
            throw e;
        }
        if (kind != SVNNodeKind.NONE && kind != SVNNodeKind.UNKNOWN) {
            if (externalKind != SVNNodeKind.FILE) {
                if (!lockedHere) {
                    getWcContext().releaseWriteLock(dirAbspath);
                }
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_FILE_EXTERNAL_OVERWRITE_VERSIONED, 
                        "The file external from ''{0}'' cannot overwrite the existing versioned item at ''{1}''",
                        url, localAbsPath);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        } else {
            SVNNodeKind diskKind = SVNFileType.getNodeKind(SVNFileType.getType(localAbsPath));
            if (diskKind == SVNNodeKind.FILE || diskKind == SVNNodeKind.DIR) {
                if (!lockedHere) {
                    getWcContext().releaseWriteLock(dirAbspath);
                }
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_FOUND, 
                        "The file external ''{0}'' can not be created because the node exists.",
                        localAbsPath);
                SVNErrorManager.error(err, SVNLogType.WC);
                return;
            }
        }
        // do file external update.
        SvnTarget target = SvnTarget.fromURL(url);
        Structure<RepositoryInfo> repositoryInfo = getRepositoryAccess().createRepositoryFor(target, revision, pegRevision, dirAbspath);
        repository = repositoryInfo.<SVNRepository>get(RepositoryInfo.repository);
        long revnum = repositoryInfo.lng(RepositoryInfo.revision);
        SVNURL swithUrl = repositoryInfo.<SVNURL>get(RepositoryInfo.url);
        repositoryInfo.release();
        
        String uuid = repository.getRepositoryUUID(true);
        String[] preservedExts = getOperation().getOptions().getPreservedConflictFileExtensions();
        boolean useCommitTimes = getOperation().getOptions().isUseCommitTimes();

        File definitionAbsPath = SVNFileUtil.getParentFile(localAbsPath);
        ISVNUpdateEditor updateEditor = SvnExternalUpdateEditor.createEditor(
                getWcContext(), 
                localAbsPath, 
                definitionAbsPath, 
                swithUrl, 
                reposRootUrl, 
                uuid, 
                useCommitTimes, 
                preservedExts, 
                definitionAbsPath, 
                url, 
                pegRevision, 
                revision);
        SvnExternalFileReporter reporter = new SvnExternalFileReporter(getWcContext(), localAbsPath, true, useCommitTimes);
        repository.update(swithUrl, revnum, SVNFileUtil.getFileName(localAbsPath), SVNDepth.UNKNOWN, reporter, updateEditor);

        handleEvent(SVNEventFactory.createSVNEvent(localAbsPath, SVNNodeKind.NONE, null, revnum, SVNEventAction.UPDATE_COMPLETED, null, null, null, 1, 1));

        if (!lockedHere) {
            getWcContext().releaseWriteLock(dirAbspath);
        }
    }
 
    protected long doSwitch(File localAbsPath, SVNURL switchUrl, SVNRevision revision, SVNRevision pegRevision, SVNDepth depth, boolean depthIsSticky, boolean ignoreExternals, boolean allowUnversionedObstructions, boolean ignoreAncestry, boolean sleepForTimestamp) throws SVNException {
        File anchor = null;
        boolean releaseLock = false;
        try {
            try {
                anchor = getWcContext().obtainAnchorPath(localAbsPath, true, true);
                getWcContext().getDb().obtainWCLock(anchor, -1, false);
                releaseLock = true;
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_LOCKED) {
                    throw e;
                }
                releaseLock = false;
            }
            getWcContext().getDb().clearDavCacheRecursive(localAbsPath);
            return switchInternal(localAbsPath, anchor, switchUrl, revision, pegRevision, depth, depthIsSticky, ignoreExternals, allowUnversionedObstructions, ignoreAncestry, sleepForTimestamp);
        } finally {
            if (anchor != null && releaseLock) {
                getWcContext().releaseWriteLock(anchor);
            }
        }
    }

    protected long switchInternal(File localAbsPath, File anchor, SVNURL switchUrl, SVNRevision revision, SVNRevision pegRevision, SVNDepth depth, boolean depthIsSticky, boolean ignoreExternals, boolean allowUnversionedObstructions, boolean ignoreAncestry, boolean sleepForTimestamp) throws SVNException {
        if (depth == SVNDepth.UNKNOWN) {
            depthIsSticky = false;
        }
        Structure<NodeInfo> nodeInfo = getWcContext().getDb().readInfo(localAbsPath, NodeInfo.haveWork);
        try {
            if (nodeInfo.is(NodeInfo.haveWork)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, 
                        "Cannot switch ''{0}'' because it is not in the repository yet", localAbsPath);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        } finally {
            nodeInfo.release();
        }
        
        String[] preservedExts = getOperation().getOptions().getPreservedConflictFileExtensions();
        boolean useCommitTimes = getOperation().getOptions().isUseCommitTimes();
        String target;
        
        if (!localAbsPath.equals(anchor)) {
            target = SVNFileUtil.getFileName(localAbsPath);
        } else {
            target = "";
        }
        final SVNURL anchorUrl = getWcContext().getNodeUrl(anchor);
        if (anchorUrl == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ENTRY_MISSING_URL, 
                    "Directory ''{0}'' has no URL", anchor);
            SVNErrorManager.error(err, SVNLogType.WC);            
        }
        
        if (depthIsSticky && depth.compareTo(SVNDepth.INFINITY) < 0) {
            if (depth == SVNDepth.EXCLUDE) {
                getWcContext().exclude(localAbsPath);
                return SVNWCContext.INVALID_REVNUM;
            }
            final SVNNodeKind targetKind = getWcContext().readKind(localAbsPath, true);
            if (targetKind == SVNNodeKind.DIR) {
                getWcContext().cropTree(localAbsPath, depth);
            }
        }
        
        Structure<RepositoryInfo> repositoryInfo = getRepositoryAccess().createRepositoryFor(SvnTarget.fromURL(switchUrl), revision, pegRevision, anchor);
        SVNRepository repository = repositoryInfo.<SVNRepository>get(RepositoryInfo.repository);
        final long revnum = repositoryInfo.lng(RepositoryInfo.revision);
        SVNURL switchRevUrl = repositoryInfo.<SVNURL>get(RepositoryInfo.url); 
        repositoryInfo.release();
    
        SVNURL switchRootUrl = repository.getRepositoryRoot(true);
        
        if (!anchorUrl.equals(switchRootUrl) && !anchorUrl.toString().startsWith(switchRootUrl.toString() + "/")) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_INVALID_SWITCH, 
                    "''{0}'' is not the same repository as ''{1}''", anchorUrl, switchRootUrl);
            SVNErrorManager.error(err, SVNLogType.WC);            
        }
        if (!ignoreAncestry) {
            SVNURL targetUrl = getWcContext().getNodeUrl(localAbsPath);
            long targetRev = getWcContext().getNodeBaseRev(localAbsPath);
            SVNLocationSegment ancestor = getRepositoryAccess().getYoungestCommonAncestor(switchRevUrl, revnum, targetUrl, targetRev);
            if (!(ancestor.getPath() != null && ancestor.getStartRevision() >= 0)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_UNRELATED_RESOURCES, 
                        "''{0}'' shares no common ancestry with ''{1}''", switchUrl, localAbsPath);
                SVNErrorManager.error(err, SVNLogType.WC);            
            }
        }
        
        repository.setLocation(anchorUrl, false);
        boolean serverSupportsDepth = repository.hasCapability(SVNCapability.DEPTH);
        SVNExternalsStore externalsStore = new SVNExternalsStore();
    
        final SVNRepository[] repos2 = new SVNRepository[1];
        ISVNDirFetcher dirFetcher = new ISVNDirFetcher() {
            public Map<String, SVNDirEntry> fetchEntries(SVNURL reposRoot, File path) throws SVNException {
                SVNURL url = SVNWCUtils.join(reposRoot, path);
                if (repos2[0] == null) {
                    repos2[0] = getRepositoryAccess().createRepository(url, null, false);
                } else {
                    repos2[0].setLocation(url, false);
                }
                
                final Map<String, SVNDirEntry> entries = new HashMap<String, SVNDirEntry>();
                SVNNodeKind kind = repos2[0].checkPath("", revnum);
                if (kind == SVNNodeKind.DIR) {
                    repos2[0].getDir("", revnum, null, new ISVNDirEntryHandler() {
                        public void handleDirEntry(SVNDirEntry dirEntry) throws SVNException {
                            if (dirEntry.getName() != null && !"".equals(dirEntry.getName())) {
                                entries.put(dirEntry.getName(), dirEntry);
                            }
                        }
                    });
                }
                return entries;
            }
        };
        final SVNReporter17 reporter = new SVNReporter17(localAbsPath, getWcContext(), true, !serverSupportsDepth, depth, 
                getOperation().isUpdateLocksOnDemand(), false, !depthIsSticky, useCommitTimes, null);
        ISVNUpdateEditor editor = SVNUpdateEditor17.createUpdateEditor(getWcContext(), 
                revnum, anchor, target, useCommitTimes, switchRevUrl, depth, depthIsSticky, allowUnversionedObstructions, 
                false, serverSupportsDepth, false, dirFetcher, externalsStore, preservedExts);
        
        try {
            repository.update(switchRevUrl, revnum, target, depthIsSticky ? depth : SVNDepth.UNKNOWN, reporter, editor);
        } catch (SVNException e) {
            sleepForTimestamp();
            throw e;
        }
        if (depth.isRecursive() && !getOperation().isIgnoreExternals()) {
            getWcContext().getDb().gatherExternalDefinitions(localAbsPath, externalsStore);
            handleExternals(externalsStore.getNewExternals(), externalsStore.getDepths(), anchorUrl, localAbsPath, switchRootUrl, depth, true);
        }
        if (sleepForTimestamp) {
            sleepForTimestamp();
        }
        handleEvent(SVNEventFactory.createSVNEvent(localAbsPath, SVNNodeKind.NONE, null, revnum, SVNEventAction.UPDATE_COMPLETED, null, null, null, reporter.getReportedFilesCount(),
                reporter.getTotalFilesCount()));
        
        return editor.getTargetRevision();
    }

    protected long checkout(SVNURL url, File localAbspath, SVNRevision pegRevision, SVNRevision revision, SVNDepth depth, boolean ignoreExternals, boolean allowUnversionedObstructions, boolean sleepForTimestamp) throws SVNException {
        Structure<RepositoryInfo> repositoryInfo = getRepositoryAccess().createRepositoryFor(
                SvnTarget.fromURL(url), 
                revision, 
                pegRevision, 
                null);
        
        url = repositoryInfo.<SVNURL>get(RepositoryInfo.url);
        SVNRepository repository = repositoryInfo.<SVNRepository>get(RepositoryInfo.repository);
        long revnum = repositoryInfo.lng(RepositoryInfo.revision);
        
        repositoryInfo.release();
        
        SVNURL rootUrl = repository.getRepositoryRoot(true);
        String uuid = repository.getRepositoryUUID(true);
        SVNNodeKind kind = repository.checkPath("", revnum);
    
        if (kind == SVNNodeKind.FILE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "URL ''{0}'' refers to a file, not a directory", url);
            SVNErrorManager.error(err, SVNLogType.WC);
        } else if (kind == SVNNodeKind.NONE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_ILLEGAL_URL, "URL ''{0}'' doesn''t exist", url);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        SVNFileType fileKind = SVNFileType.getType(localAbspath);
        
        if (fileKind == SVNFileType.NONE) {
            SVNFileUtil.ensureDirectoryExists(localAbspath);
            getWcContext().initializeWC(localAbspath, url, rootUrl, uuid, revnum, depth == SVNDepth.UNKNOWN ? SVNDepth.INFINITY : depth);
        } else if (fileKind == SVNFileType.DIRECTORY) {
            int formatVersion = getWcContext().checkWC(localAbspath);
            if (formatVersion == SVNWCDb.WC_FORMAT_17 && SvnOperationFactory.isVersionedDirectory(localAbspath)) {
                SVNURL entryUrl = getWcContext().getNodeUrl(localAbspath);
                if (entryUrl != null && !url.equals(entryUrl)) {                
                    String message = "''{0}'' is already a working copy for a different URL";
                    message += "; perform update to complete it";
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_OBSTRUCTED_UPDATE, message, localAbspath);
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
            } else {
                depth = depth == SVNDepth.UNKNOWN ? SVNDepth.INFINITY : depth;
                getWcContext().initializeWC(localAbspath, url, rootUrl, uuid, revnum, depth == SVNDepth.UNKNOWN ? SVNDepth.INFINITY : depth);
            }
        } else {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_NODE_KIND_CHANGE, "''{0}'' already exists and is not a directory", localAbspath);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        return update(getWcContext(), localAbspath, revision, depth, true, ignoreExternals, allowUnversionedObstructions, true, false, false, sleepForTimestamp);
    }

    protected static boolean isEmptyWc(File root, File anchorAbspath) {
        if (!root.equals(anchorAbspath)) {
            return false;
        }

        File[] children = SVNFileListUtil.listFiles(root);
        if (children != null) {
            return children.length == 1 && SVNFileUtil.getAdminDirectoryName().equals(children[0].getName());
        }
        return true;
    
    }
}
