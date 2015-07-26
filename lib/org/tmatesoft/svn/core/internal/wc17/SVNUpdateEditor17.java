/*
 * ====================================================================
 * Copyright (c) 2004-2010 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.wc17;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.util.SVNSkel;
import org.tmatesoft.svn.core.internal.util.SVNURLUtil;
import org.tmatesoft.svn.core.internal.wc.ISVNUpdateEditor;
import org.tmatesoft.svn.core.internal.wc.SVNCancellableEditor;
import org.tmatesoft.svn.core.internal.wc.SVNConflictVersion;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.admin.SVNChecksumInputStream;
import org.tmatesoft.svn.core.internal.wc.admin.SVNChecksumOutputStream;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.MergeInfo;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.MergePropertiesInfo;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.TranslateInfo;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.WritableBaseInfo;
import org.tmatesoft.svn.core.internal.wc17.db.*;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbAdditionInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbAdditionInfo.AdditionInfoField;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbBaseInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbBaseInfo.BaseInfoField;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbInfo.InfoField;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbRepositoryInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbRepositoryInfo.RepositoryInfoField;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgPropertiesManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.diff.SVNDeltaProcessor;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;
import org.tmatesoft.svn.core.wc.*;
import org.tmatesoft.svn.core.wc2.SvnChecksum;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author TMate Software Ltd.
 */
public class SVNUpdateEditor17 implements ISVNUpdateEditor {

    private SVNWCContext myWCContext;

    private String myTargetBasename;
    private File myAnchorAbspath;
    private File myTargetAbspath;
    private String[] myExtensionPatterns;
    private long myTargetRevision;
    private SVNDepth myRequestedDepth;
    private boolean myIsDepthSticky;
    private boolean myIsUseCommitTimes;
    private boolean rootOpened;
    private boolean myIsTargetDeleted;
    private boolean myIsUnversionedObstructionsAllowed;
    private File mySwitchRelpath;
    private SVNURL myReposRootURL;
    private String myReposUuid;
    private Set<File> mySkippedTrees = new HashSet<File>();
    private SVNDeltaProcessor myDeltaProcessor;
    private SVNExternalsStore myExternalsStore;
    private DirectoryBaton myCurrentDirectory;

    private FileBaton myCurrentFile;
    private boolean myAddsAsModification = true;
    private Map<File, Map<String, SVNDirEntry>> myDirEntries;
    private boolean myIsCleanCheckout;
    private File myWCRootAbsPath;

    private Map<File, Map<String, SVNProperties>> myInheritableProperties;
    private ISVNConflictHandler myConflictHandler;

    public static ISVNUpdateEditor createUpdateEditor(SVNWCContext context,
            long targetRevision,
            File anchorAbspath,
            String targetName,
            Map<File, Map<String, SVNProperties>> inheritableProperties,
            boolean useCommitTimes,
            SVNURL switchURL,
            SVNDepth depth,
            boolean depthIsSticky,
            boolean allowUnversionedObstructions,
            boolean addsAsModifications,
            boolean serverPerformsFiltering,
            boolean cleanCheckout,
            ISVNDirFetcher dirFetcher,
            SVNExternalsStore externalsStore,
            String[] preservedExtensions,
            ISVNConflictHandler conflictHandler) throws SVNException {
        if (depth == SVNDepth.UNKNOWN) {
            depthIsSticky = false;
        }
        WCDbRepositoryInfo repositoryInfo = context.getDb().scanBaseRepository(anchorAbspath, RepositoryInfoField.rootUrl, RepositoryInfoField.uuid);
        assert repositoryInfo != null && repositoryInfo.rootUrl != null && repositoryInfo.uuid != null;
        if (switchURL != null && !SVNURLUtil.isAncestor(repositoryInfo.rootUrl, switchURL)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_INVALID_SWITCH, "''{0}'' is not the same repository as ''{1}''", switchURL, repositoryInfo.rootUrl);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        SVNUpdateEditor17 editor = new SVNUpdateEditor17();
        editor.myIsUseCommitTimes = useCommitTimes;
        editor.myTargetRevision = targetRevision;
        editor.myReposRootURL = repositoryInfo.rootUrl;
        editor.myReposUuid = repositoryInfo.uuid;
        editor.myWCContext = context;
        editor.myTargetBasename = targetName;
        editor.myAnchorAbspath = anchorAbspath;
        editor.myWCRootAbsPath = context.getDb().getWCRoot(anchorAbspath);
        editor.myInheritableProperties = inheritableProperties;

        if (switchURL != null) {
            editor.mySwitchRelpath = SVNFileUtil.createFilePath(SVNPathUtil.getRelativePath(repositoryInfo.rootUrl.getPath(), switchURL.getPath()));
        }
        if ("".equals(targetName) || targetName == null) {
            editor.myTargetAbspath = anchorAbspath;
        } else {
            editor.myTargetAbspath = SVNFileUtil.createFilePath(anchorAbspath, targetName);
        }

        editor.myRequestedDepth = depth;
        editor.myIsDepthSticky = depthIsSticky;
        editor.myIsUnversionedObstructionsAllowed = allowUnversionedObstructions;
        editor.myAddsAsModification = addsAsModifications;
        editor.myIsCleanCheckout = cleanCheckout;
        editor.myExtensionPatterns = preservedExtensions;
        editor.myExternalsStore = externalsStore;
        editor.myConflictHandler = conflictHandler;

        if (dirFetcher != null) {
            editor.initExcludedDirectoryEntries(dirFetcher);
        }

        ISVNUpdateEditor result = editor;
        if (!serverPerformsFiltering && !depthIsSticky) {
            result = new SVNAmbientDepthFilterEditor17(result, context, anchorAbspath, targetName, true);
        }
        return (ISVNUpdateEditor) SVNCancellableEditor.newInstance(result, context.getEventHandler(), null);
    }

    public static ISVNUpdateEditor createUpdateEditor(SVNWCContext wcContext, File anchorAbspath, String target, Map<File, Map<String, SVNProperties>> inheritableProperties, SVNURL reposRoot, SVNURL switchURL, SVNExternalsStore externalsStore,
            boolean allowUnversionedObstructions, boolean depthIsSticky, SVNDepth depth, String[] preservedExts,
            ISVNDirFetcher dirFetcher) throws SVNException {
        if (depth == SVNDepth.UNKNOWN) {
            depthIsSticky = false;
        }
        WCDbInfo info = wcContext.getDb().readInfo(anchorAbspath, InfoField.status, InfoField.reposId, InfoField.reposRootUrl, InfoField.reposUuid);

        /* ### For adds, REPOS_ROOT and REPOS_UUID would be NULL now. */
        if (info.status == SVNWCDbStatus.Added) {
            WCDbAdditionInfo addition = wcContext.getDb().scanAddition(anchorAbspath, AdditionInfoField.reposRootUrl, AdditionInfoField.reposUuid);
            info.reposRootUrl = addition.reposRootUrl;
            info.reposUuid = addition.reposUuid;
        }

        assert (info.reposRootUrl != null && info.reposUuid != null);
        if (switchURL != null) {
            if (!SVNPathUtil.isAncestor(info.reposRootUrl.toDecodedString(), switchURL.toDecodedString())) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_INVALID_SWITCH, "''{0}''\nis not the same repository as\n''{1}''", new Object[] {
                        switchURL.toDecodedString(), info.reposRootUrl.toDecodedString()
                });
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        }
        return new SVNUpdateEditor17(wcContext, anchorAbspath, target, inheritableProperties, info.reposRootUrl, info.reposUuid, switchURL, externalsStore, allowUnversionedObstructions, depthIsSticky, depth, preservedExts,
                dirFetcher);
    }

    private SVNUpdateEditor17() {
        myDirEntries = new HashMap<File, Map<String,SVNDirEntry>>();
        myDeltaProcessor = new SVNDeltaProcessor();
        mySkippedTrees = new HashSet<File>();
    }

    public SVNUpdateEditor17(SVNWCContext wcContext, File anchorAbspath, String targetBasename, Map<File, Map<String, SVNProperties>> inheritableProperties, SVNURL reposRootUrl, String reposUuid, SVNURL switchURL, SVNExternalsStore externalsStore,
            boolean allowUnversionedObstructions, boolean depthIsSticky, SVNDepth depth, String[] preservedExts, ISVNDirFetcher dirFetcher) throws SVNException {
        myWCContext = wcContext;
        myAnchorAbspath = anchorAbspath;
        myTargetBasename = targetBasename;
        myIsUnversionedObstructionsAllowed = allowUnversionedObstructions;
        myTargetRevision = -1;
        myRequestedDepth = depth;
        myIsDepthSticky = depthIsSticky;
        myDeltaProcessor = new SVNDeltaProcessor();
        myExtensionPatterns = preservedExts;
        myTargetAbspath = anchorAbspath;
        myReposRootURL = reposRootUrl;
        myReposUuid = reposUuid;
        myExternalsStore = externalsStore;
        myIsUseCommitTimes = myWCContext.getOptions().isUseCommitTimes();
        if (myTargetBasename != null) {
            myTargetAbspath = SVNFileUtil.createFilePath(myTargetAbspath, myTargetBasename);
        }
        if ("".equals(myTargetBasename)) {
            myTargetBasename = null;
        }
        if (switchURL != null) {
            mySwitchRelpath = SVNFileUtil.createFilePath(SVNPathUtil.getRelativePath(reposRootUrl.getPath(), switchURL.getPath()));
        } else {
            mySwitchRelpath = null;
        }
        if (dirFetcher != null) {
            initExcludedDirectoryEntries(dirFetcher);
        }
        myConflictHandler = myWCContext.getOptions().getConflictResolver();
        myInheritableProperties = inheritableProperties;
    }

    private void initExcludedDirectoryEntries(ISVNDirFetcher dirFetcher) throws SVNException {
        if (!myIsDepthSticky && SVNDepth.EMPTY.compareTo(myRequestedDepth) <= 0 && myRequestedDepth.compareTo(SVNDepth.INFINITY) < 0 &&  myRequestedDepth != SVNDepth.UNKNOWN) {
            myDirEntries = new HashMap<File, Map<String,SVNDirEntry>>();
            WCDbBaseInfo info = null;
            try {
                info = myWCContext.getDb().getBaseInfo(myTargetAbspath, BaseInfoField.status, BaseInfoField.kind, BaseInfoField.reposRelPath, BaseInfoField.depth);
            } catch (SVNException e) {
                info = null;
                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                    throw e;
                }
            }
            if (info != null && info.kind == SVNWCDbKind.Dir && info.status == SVNWCDbStatus.Normal) {
                if (info.depth.compareTo(myRequestedDepth) > 0) {
                    File dirReposRelPath = mySwitchRelpath != null ? mySwitchRelpath : info.reposRelPath;
                    Map<String, SVNDirEntry> dirEntries = dirFetcher.fetchEntries(myReposRootURL, dirReposRelPath);
                    if (dirEntries != null && !dirEntries.isEmpty()) {
                        myDirEntries.put(dirReposRelPath, dirEntries);
                    }
                }
                if (myRequestedDepth == SVNDepth.IMMEDIATES) {
                    Set<String> children = myWCContext.getDb().getBaseChildren(myTargetAbspath);

                    for (String child : children) {
                        File childAbsPath = SVNFileUtil.createFilePath(myTargetAbspath, child);
                        info = myWCContext.getDb().getBaseInfo(childAbsPath, BaseInfoField.status, BaseInfoField.kind, BaseInfoField.reposRelPath, BaseInfoField.depth);
                        if (info.kind == SVNWCDbKind.Dir && info.status == SVNWCDbStatus.Normal &&
                                info.depth.compareTo(SVNDepth.EMPTY) > 0) {
                            File dirReposRelPath = mySwitchRelpath != null ?
                                    SVNFileUtil.createFilePath(mySwitchRelpath, child) : info.reposRelPath;
                            Map<String, SVNDirEntry> dirEntries = dirFetcher.fetchEntries(myReposRootURL, dirReposRelPath);
                            if (dirEntries != null && !dirEntries.isEmpty()) {
                                myDirEntries.put(dirReposRelPath, dirEntries);
                            }
                        }
                    }
                }
            }

        }
    }

    public void targetRevision(long revision) throws SVNException {
        myTargetRevision = revision;
    }

    public long getTargetRevision() {
        return myTargetRevision;
    }

    private void rememberSkippedTree(File localAbspath) throws SVNException {
        assert (SVNFileUtil.isAbsolute(localAbspath));
        File relativePath = SVNWCUtils.skipAncestor(getWCRootAbsPath(), localAbspath);
        mySkippedTrees.add(relativePath);
        return;
    }

    private File getWCRootAbsPath() throws SVNException {
        if (myWCRootAbsPath == null) {
            myWCRootAbsPath = myWCContext.getDb().getWCRoot(myAnchorAbspath);
        }
        return myWCRootAbsPath;
    }

    public void openRoot(long revision) throws SVNException {
        boolean alreadyConflicted;
        boolean conflictIgnored;
        SVNWCDbStatus baseStatus = SVNWCDbStatus.Normal;
        rootOpened = true;
        myCurrentDirectory = makeDirectoryBaton(null, null, false);
        try {
            AlreadyInTreeConflictInfo alreadyInTreeConflictInfo = alreadyInATreeConflict(myCurrentDirectory.localAbsolutePath);
            alreadyConflicted = alreadyInTreeConflictInfo.conflicted;
            conflictIgnored = alreadyInTreeConflictInfo.ignored;
            if (alreadyConflicted) {
                rememberSkippedTree(myCurrentDirectory.localAbsolutePath);
                rememberSkippedTree(myTargetAbspath);

                myCurrentDirectory.skipThis = true;
                myCurrentDirectory.alreadyNotified = true;
                doNotification(myTargetAbspath, SVNNodeKind.UNKNOWN, SVNEventAction.SKIP_CONFLICTED, null, null);
                return;
            }
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                throw e;
            }
            alreadyConflicted = false;
            conflictIgnored = false;
        }

        Structure<StructureFields.NodeInfo> nodeInfoStructure = myWCContext.getDb().readInfo(myCurrentDirectory.localAbsolutePath,
                StructureFields.NodeInfo.status, StructureFields.NodeInfo.kind, StructureFields.NodeInfo.revision,
                StructureFields.NodeInfo.reposRelPath, StructureFields.NodeInfo.changedRev, StructureFields.NodeInfo.changedDate,
                StructureFields.NodeInfo.changedAuthor, StructureFields.NodeInfo.depth, StructureFields.NodeInfo.haveWork);
        SVNWCDbStatus status = nodeInfoStructure.get(StructureFields.NodeInfo.status);
        SVNWCDbKind kind = nodeInfoStructure.get(StructureFields.NodeInfo.kind);
        myCurrentDirectory.oldRevision = nodeInfoStructure.lng(StructureFields.NodeInfo.revision);
        myCurrentDirectory.oldReposRelPath = nodeInfoStructure.get(StructureFields.NodeInfo.reposRelPath);
        myCurrentDirectory.changedAuthor = nodeInfoStructure.get(StructureFields.NodeInfo.changedAuthor);
        myCurrentDirectory.changedRevsion = nodeInfoStructure.lng(StructureFields.NodeInfo.changedRev);
        myCurrentDirectory.changedDate = nodeInfoStructure.get(StructureFields.NodeInfo.changedDate);
        myCurrentDirectory.ambientDepth = nodeInfoStructure.get(StructureFields.NodeInfo.depth);
        boolean haveWork = nodeInfoStructure.is(StructureFields.NodeInfo.haveWork);

        if (conflictIgnored) {
            myCurrentDirectory.shadowed = true;
        } else if (haveWork) {
            ISVNWCDb.SVNWCDbBaseMovedToData baseMovedToData = myWCContext.getDb().baseMovedTo(myCurrentDirectory.localAbsolutePath);
            File moveSrcRootAbsPath = baseMovedToData.moveSrcRootAbsPath;
            if (moveSrcRootAbsPath != null || "".equals(myTargetBasename)) {
                WCDbBaseInfo baseInfo = myWCContext.getDb().getBaseInfo(myCurrentDirectory.localAbsolutePath,
                        BaseInfoField.status, BaseInfoField.revision, BaseInfoField.reposRelPath,
                        BaseInfoField.changedRev, BaseInfoField.changedDate, BaseInfoField.changedAuthor,
                        BaseInfoField.depth);
                baseStatus = baseInfo.status;
                myCurrentDirectory.oldRevision = baseInfo.revision;
                myCurrentDirectory.oldReposRelPath = baseInfo.reposRelPath;
                myCurrentDirectory.changedRevsion = baseInfo.changedRev;
                myCurrentDirectory.changedDate = baseInfo.changedDate;
                myCurrentDirectory.changedAuthor = baseInfo.changedAuthor;
                myCurrentDirectory.ambientDepth = baseInfo.depth;
            }

            if (moveSrcRootAbsPath != null) {
                SVNSkel treeConflict = SVNSkel.createEmptyList();
                SvnWcDbConflicts.addTreeConflict(treeConflict, myWCContext.getDb(), moveSrcRootAbsPath,
                        SVNConflictReason.MOVED_AWAY, SVNConflictAction.EDIT, moveSrcRootAbsPath);
                if (myCurrentDirectory.equals(moveSrcRootAbsPath)) {
                    completeConflict(treeConflict, moveSrcRootAbsPath,
                            myCurrentDirectory.oldReposRelPath,
                            myCurrentDirectory.oldRevision,
                            myCurrentDirectory.newRelativePath,
                            SVNNodeKind.DIR, SVNNodeKind.DIR);
                    myWCContext.getDb().opMarkConflict(moveSrcRootAbsPath, treeConflict, null);
                    doNotification(moveSrcRootAbsPath, SVNNodeKind.DIR, SVNEventAction.TREE_CONFLICT, null, null);
                } else {
                    myCurrentDirectory.editConflict = treeConflict;
                }
            }

            myCurrentDirectory.shadowed = true;
        } else {
            baseStatus = status;
        }

        if ("".equals(myTargetBasename)) {
            myCurrentDirectory.wasIncomplete = (baseStatus == SVNWCDbStatus.Incomplete);
            myWCContext.getDb().opStartDirectoryUpdateTemp(myCurrentDirectory.localAbsolutePath, myCurrentDirectory.newRelativePath, myTargetRevision);
        }
    }

    private void doNotification(File localAbspath, SVNNodeKind kind, SVNEventAction action, SVNURL url, SVNURL previousURL) throws SVNException {
        if (myWCContext.getEventHandler() != null) {
            SVNEvent event = new SVNEvent(localAbspath, kind, null, -1, null, null, null, null, action, null, null, null, null, null, null);

            event.setURL(url);
            event.setPreviousURL(previousURL);

            myWCContext.getEventHandler().handleEvent(event, 0);
        }
    }

    private AlreadyInTreeConflictInfo alreadyInATreeConflict(File localAbspath) throws SVNException {
        assert (SVNFileUtil.isAbsolute(localAbspath));
        File ancestorAbspath = localAbspath;
        boolean conflicted = false;
        boolean ignored = false;
        while (ancestorAbspath != null) {
            SVNWCContext.ConflictInfo conflictInfo = myWCContext.getConflicted(ancestorAbspath, false, false, true);
            conflicted = conflictInfo.treeConflicted;
            ignored = conflictInfo.ignored;
            if (conflicted || ignored) {
                break;
            }

            if (myWCContext.getDb().isWCRoot(ancestorAbspath)) {
                break;
            }
            ancestorAbspath = SVNFileUtil.getParentFile(ancestorAbspath);
        }
        return new AlreadyInTreeConflictInfo(conflicted, ignored);
    }

    private static class AlreadyInTreeConflictInfo {
        public boolean conflicted;
        public boolean ignored;

        private AlreadyInTreeConflictInfo(boolean conflicted, boolean ignored) {
            this.conflicted = conflicted;
            this.ignored = ignored;
        }
    }

    public void deleteEntry(String path, long revision) throws SVNException {
        if (myCurrentDirectory.skipThis) {
            return;
        }
        myCurrentDirectory.markEdited();
        String base = SVNFileUtil.getFileName(SVNFileUtil.createFilePath(path));
        File localAbsPath = SVNFileUtil.createFilePath(myCurrentDirectory.localAbsolutePath, base);

        boolean isRoot = myWCContext.getDb().isWCRoot(localAbsPath);
        if (isRoot) {
            rememberSkippedTree(localAbsPath);
            doNotification(localAbsPath, SVNNodeKind.UNKNOWN, SVNEventAction.UPDATE_SKIP_OBSTRUCTION, null, null);
            return;
        }

        boolean deletingSwitched;
        boolean deletingTarget = localAbsPath.equals(myTargetAbspath);
        SVNWCDbStatus baseStatus;
        SVNWCDbKind baseKind;

        WCDbInfo info = myWCContext.getDb().readInfo(localAbsPath, InfoField.status, InfoField.kind, InfoField.revision, InfoField.reposRelPath, InfoField.conflicted, InfoField.haveBase, InfoField.haveWork);
        SVNURL previousURL = info.reposRelPath != null ? SVNWCUtils.join(myReposRootURL, info.reposRelPath) : null;

        if (!info.haveWork) {
            baseStatus = info.status;
            baseKind = info.kind;
        } else {
            WCDbBaseInfo baseInfo = myWCContext.getDb().getBaseInfo(localAbsPath, BaseInfoField.status, BaseInfoField.kind, BaseInfoField.revision, BaseInfoField.reposRelPath);
            baseStatus = baseInfo.status;
            baseKind = baseInfo.kind;
            info.reposRelPath = baseInfo.reposRelPath;
            info.revision = baseInfo.revision;
        }

        if (myCurrentDirectory.oldReposRelPath != null && info.reposRelPath != null) {
            String expectedName = SVNPathUtil.getRelativePath(myCurrentDirectory.oldReposRelPath.getPath(), info.reposRelPath.getPath());
            deletingSwitched = (expectedName == null || !expectedName.equals(base));
        } else {
            deletingSwitched = false;
        }

        if (myCurrentDirectory.shadowed) {
            info.conflicted = false;
        } else if (info.conflicted) {
            NodeAlreadyConflictedInfo alreadyConflicted = isNodeAlreadyConflicted(localAbsPath);
            info.conflicted = alreadyConflicted.conflicted;
        }
        if (info.conflicted) {
            rememberSkippedTree(localAbsPath);
            doNotification(localAbsPath, SVNNodeKind.UNKNOWN, SVNEventAction.SKIP_CONFLICTED, null, null);
            return;
        }

        if (baseStatus == SVNWCDbStatus.NotPresent || baseStatus == SVNWCDbStatus.Excluded || baseStatus == SVNWCDbStatus.ServerExcluded) {
            myWCContext.getDb().removeBase(localAbsPath, false, false, false, SVNRepository.INVALID_REVISION, null, null);
            if (deletingTarget) {
                myIsTargetDeleted = true;
            }
            return;
        }

        boolean queueDeletes = true;

        SVNSkel treeConflict = null;
        if (!myCurrentDirectory.shadowed && !myCurrentDirectory.editedObstructed) {
            treeConflict = checkTreeConflict(localAbsPath, info.status, true, info.kind == SVNWCDbKind.Dir ? SVNNodeKind.DIR : SVNNodeKind.FILE, SVNConflictAction.DELETE);
        } else {
             queueDeletes = false;
        }

        boolean keepAsWorking = false;

        if (treeConflict != null) {
            if (myCurrentDirectory.deletionConflicts == null) {
                myCurrentDirectory.deletionConflicts = new HashMap<String, SVNSkel>();
            }
            myCurrentDirectory.deletionConflicts.put(base, treeConflict);

            Structure<SvnWcDbConflicts.TreeConflictInfo> treeConflictInfoStructure = SvnWcDbConflicts.readTreeConflict(myWCContext.getDb(), localAbsPath, treeConflict);
            SVNConflictReason reason = treeConflictInfoStructure.get(SvnWcDbConflicts.TreeConflictInfo.localChange);

            if (reason == SVNConflictReason.EDITED ||
                    reason == SVNConflictReason.OBSTRUCTED) {
                keepAsWorking = true;
            } else if (reason == SVNConflictReason.DELETED ||
                    reason == SVNConflictReason.MOVED_AWAY ||
                    reason == SVNConflictReason.REPLACED) {
                //do nothing
            } else {
                throw new IllegalStateException();
            }
        }

        completeConflict(treeConflict, localAbsPath, info.reposRelPath, info.revision, null, info.kind == SVNWCDbKind.Dir ? SVNNodeKind.DIR : SVNNodeKind.FILE, SVNNodeKind.NONE);

        if (!deletingTarget && !deletingSwitched) {
            myWCContext.getDb().removeBase(localAbsPath, keepAsWorking, queueDeletes, false, -1, treeConflict, null);
        } else {
            myWCContext.getDb().removeBase(localAbsPath, keepAsWorking, queueDeletes, false, getTargetRevision(), treeConflict, null);
            if (deletingTarget) {
                myIsTargetDeleted = true;
            } else {
                rememberSkippedTree(localAbsPath);
            }
        }
        myWCContext.wqRun(myCurrentDirectory.localAbsolutePath);

        if (treeConflict != null) {
            doNotification(localAbsPath, SVNNodeKind.UNKNOWN, SVNEventAction.TREE_CONFLICT, null, null);
        } else {
            SVNEventAction action = SVNEventAction.UPDATE_DELETE;
            if (myCurrentDirectory.shadowed || myCurrentDirectory.editedObstructed) {
                action = SVNEventAction.UPDATE_SHADOWED_DELETE;
            }
            doNotification(localAbsPath, info.kind == SVNWCDbKind.Dir ? SVNNodeKind.DIR : SVNNodeKind.FILE, action, null, previousURL);
        }
    }

    private NodeAlreadyConflictedInfo isNodeAlreadyConflicted(File localAbspath) throws SVNException {
        NodeAlreadyConflictedInfo alreadyConflictedInfo = new NodeAlreadyConflictedInfo();
        List<SVNConflictDescription> conflicts = myWCContext.getDb().readConflicts(localAbspath);
        for (SVNConflictDescription cd : conflicts) {
            if (cd.isTreeConflict()) {
                alreadyConflictedInfo.conflicted = true;
                alreadyConflictedInfo.conflictIgnored = false;
                return alreadyConflictedInfo;
            } else if (cd.isPropertyConflict() || cd.isTextConflict()) {
                SVNWCContext.ConflictInfo info = myWCContext.getConflicted(localAbspath, true, true, true);
                alreadyConflictedInfo.conflicted = info.textConflicted || info.propConflicted || info.treeConflicted;
                alreadyConflictedInfo.conflictIgnored = info.ignored;
                return alreadyConflictedInfo;
            }
        }
        alreadyConflictedInfo.conflicted = false;
        alreadyConflictedInfo.conflictIgnored = false;
        return alreadyConflictedInfo;
    }

    private static class NodeAlreadyConflictedInfo {
        public boolean conflicted;
        public boolean conflictIgnored;
    }

    private SVNSkel checkTreeConflict(File localAbspath, SVNWCDbStatus workingStatus,
                                      boolean existsInRepos, SVNNodeKind expectedKind,
                                      SVNConflictAction action) throws SVNException {
        SVNConflictReason reason = null;
        boolean modified = false;
        boolean allModsAreDeleted = false;
        File moveSrcOpRootAbsPath = null;

        SVNSkel conflict;

        switch (workingStatus) {
            case Added:
            case MovedHere:
            case Copied:
                if (!existsInRepos) {
                    assert action == SVNConflictAction.ADD;

                    if (workingStatus == SVNWCDbStatus.Added) {
                        myWCContext.getDb().scanAddition(localAbspath, AdditionInfoField.status);
                    }

                    if (workingStatus == SVNWCDbStatus.MovedHere) {
                        reason = SVNConflictReason.MOVED_HERE;
                    } else {
                        reason = SVNConflictReason.ADDED;
                    }
                } else {
                    ISVNWCDb.SVNWCDbBaseMovedToData baseMovedToData = myWCContext.getDb().baseMovedTo(localAbspath);
                    moveSrcOpRootAbsPath = baseMovedToData.moveSrcOpRootAbsPath;
                    if (moveSrcOpRootAbsPath != null) {
                        reason = SVNConflictReason.MOVED_AWAY;
                    } else {
                        reason = SVNConflictReason.REPLACED;
                    }
                }
                break;

            case Deleted:
                ISVNWCDb.SVNWCDbBaseMovedToData baseMovedToData = myWCContext.getDb().baseMovedTo(localAbspath);
                moveSrcOpRootAbsPath = baseMovedToData.moveSrcOpRootAbsPath;
                if (moveSrcOpRootAbsPath != null) {
                    reason = SVNConflictReason.MOVED_AWAY;
                } else {
                    reason = SVNConflictReason.DELETED;
                }
                break;

            case Incomplete:
            case Normal:
                if (action == SVNConflictAction.EDIT) {
                    if (existsInRepos) {
                        SVNNodeKind diskKind = SVNFileType.getNodeKind(SVNFileType.getType(localAbspath));
                        if (diskKind != expectedKind && diskKind != SVNNodeKind.NONE) {
                            reason = SVNConflictReason.OBSTRUCTED;
                            break;
                        }
                    }
                    return null;
                }
                assert action == SVNConflictAction.DELETE;
                SVNWCContext.TreeLocalModsInfo treeLocalModsInfo = myWCContext.hasLocalMods(localAbspath, myAnchorAbspath);
                modified = treeLocalModsInfo.modificationsFound;
                allModsAreDeleted = !treeLocalModsInfo.nonDeleteModificationsFound;

                if (modified) {
                    if (allModsAreDeleted) {
                        reason = SVNConflictReason.DELETED;
                    } else {
                        reason = SVNConflictReason.EDITED;
                    }
                }

                break;

            case ServerExcluded:
            case Excluded:
            case NotPresent:
                return null;

            case BaseDeleted:
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ASSERTION_FAIL);
                SVNErrorManager.error(err, SVNLogType.WC);
                break;
            default:
                break;
        }

        if (reason == null) {
            return null;
        }
        if (reason == SVNConflictReason.EDITED ||
                reason == SVNConflictReason.OBSTRUCTED ||
                reason == SVNConflictReason.DELETED ||
                reason == SVNConflictReason.MOVED_AWAY ||
                reason == SVNConflictReason.REPLACED) {
            if (action != SVNConflictAction.EDIT && action != SVNConflictAction.DELETE && action != SVNConflictAction.REPLACE) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_FOUND_CONFLICT, "Unexpected attempt to add a node at path '{{0}}'",
                        localAbspath);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
        } else if (reason == SVNConflictReason.ADDED ||
                reason == SVNConflictReason.MOVED_HERE) {
            if (action != SVNConflictAction.ADD) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_FOUND_CONFLICT, "Unexpected attempt to edit, delete, or replace " +
                        "a node at path '{{0}}'", localAbspath);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
        }
        conflict = SvnWcDbConflicts.createConflictSkel();
        SvnWcDbConflicts.addTreeConflict(conflict, myWCContext.getDb(), localAbspath, reason, action, moveSrcOpRootAbsPath);
        return conflict;
    }

    public void absentDir(String path) throws SVNException {
        absentEntry(path, SVNNodeKind.DIR);
    }

    public void absentFile(String path) throws SVNException {
        absentEntry(path, SVNNodeKind.FILE);
    }

    private void absentEntry(String path, SVNNodeKind nodeKind) throws SVNException {
        if (myCurrentDirectory.skipThis) {
            return;
        }
        String name = SVNPathUtil.tail(path);
        SVNWCDbKind absentKind = nodeKind == SVNNodeKind.DIR ? SVNWCDbKind.Dir : SVNWCDbKind.File;
        myCurrentDirectory.markEdited();
        File localAbsPath = SVNFileUtil.createFilePath(myCurrentDirectory.localAbsolutePath, name);

        WCDbInfo info = null;
        SVNWCDbStatus status = null;
        SVNWCDbKind kind = null;
        try {
            info = myWCContext.getDb().readInfo(localAbsPath, InfoField.status, InfoField.kind);
            status = info.status;
            kind = info.kind;
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                throw e;
            }
            status = SVNWCDbStatus.NotPresent;
            kind = SVNWCDbKind.Unknown;
        }
        if (status == SVNWCDbStatus.Normal && kind == SVNWCDbKind.Dir) {
            // ok
        } else if (status == SVNWCDbStatus.NotPresent || status == SVNWCDbStatus.ServerExcluded || status == SVNWCDbStatus.Excluded) {
            // ok
        } else {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_OBSTRUCTED_UPDATE, "Failed to mark ''{0}'' absent: item of the same name is already scheduled for addition", path);
            SVNErrorManager.error(err, SVNLogType.WC);
        }

        File reposRelPath = SVNFileUtil.createFilePath(myCurrentDirectory.newRelativePath, name);
        myWCContext.getDb().addBaseExcludedNode(localAbsPath, reposRelPath, myReposRootURL, myReposUuid, myTargetRevision, absentKind, SVNWCDbStatus.ServerExcluded, null, null);
    }

    public void addDir(String path, String copyFromPath, long copyFromRevision) throws SVNException {
        assert ((copyFromPath != null && SVNRevision.isValidRevisionNumber(copyFromRevision)) || (copyFromPath == null && !SVNRevision.isValidRevisionNumber(copyFromRevision)));
        DirectoryBaton pb = myCurrentDirectory;
        myCurrentDirectory = makeDirectoryBaton(path, myCurrentDirectory, true);
        DirectoryBaton db = myCurrentDirectory;
        if (db.skipThis) {
            return;
        }
        db.markEdited();

        boolean conflictIgnored = false;

        if (myTargetAbspath.equals(db.localAbsolutePath)) {
            db.ambientDepth = (myRequestedDepth == SVNDepth.UNKNOWN) ? SVNDepth.INFINITY : myRequestedDepth;
        } else if (myRequestedDepth == SVNDepth.IMMEDIATES || (myRequestedDepth == SVNDepth.UNKNOWN && pb.ambientDepth == SVNDepth.IMMEDIATES)) {
            db.ambientDepth = SVNDepth.EMPTY;
        } else {
            db.ambientDepth = SVNDepth.INFINITY;
        }
        if (SVNFileUtil.getAdminDirectoryName().equals(db.name)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_OBSTRUCTED_UPDATE, "Failed to add directory ''{0}'': object of the same name as the administrative directory",
                    db.localAbsolutePath);
            SVNErrorManager.error(err, SVNLogType.WC);
            return;
        }
        SVNNodeKind kind = SVNFileType.getNodeKind(SVNFileType.getType(db.localAbsolutePath));
        SVNWCDbStatus status;
        SVNWCDbKind wcKind;
        boolean conflicted;
        boolean versionedLocallyAndPresent;
        boolean error = false;
        try {
            WCDbInfo readInfo = myWCContext.getDb().readInfo(db.localAbsolutePath, InfoField.status, InfoField.kind, InfoField.conflicted, InfoField.reposRelPath);
            status = readInfo.status;
            wcKind = readInfo.kind;
            conflicted = readInfo.conflicted;
            versionedLocallyAndPresent = false;
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                throw e;
            }
            error = true;
            wcKind = SVNWCDbKind.Unknown;
            status = SVNWCDbStatus.Normal;
            conflicted = false;
            versionedLocallyAndPresent = false;
        }
        if (!error) {
            if (wcKind == SVNWCDbKind.Dir && status == SVNWCDbStatus.Normal) {
                myWCContext.getDb().addBaseNotPresentNode(db.localAbsolutePath, db.newRelativePath, myReposRootURL, myReposUuid, myTargetRevision, SVNWCDbKind.File, null, null);
                rememberSkippedTree(db.localAbsolutePath);
                db.skipThis = true;
                db.alreadyNotified = true;
                doNotification(db.localAbsolutePath, SVNNodeKind.DIR, SVNEventAction.UPDATE_SKIP_OBSTRUCTION, db.getURL(), db.getPreviousURL());
                return;
            } else if (status == SVNWCDbStatus.Normal && (wcKind == SVNWCDbKind.File ||  wcKind == SVNWCDbKind.Symlink)) {
                rememberSkippedTree(db.localAbsolutePath);
                db.skipThis = true;
                db.alreadyNotified = true;
                doNotification(db.localAbsolutePath, SVNNodeKind.FILE, SVNEventAction.UPDATE_SKIP_OBSTRUCTION, db.getURL(), db.getPreviousURL());
                return;
            } else if (wcKind == SVNWCDbKind.Unknown) {
                versionedLocallyAndPresent = false;
            } else {
                versionedLocallyAndPresent = isNodePresent(status);
            }
        }

        SVNSkel treeConflict = null;
        if (conflicted) {
            if (pb.deletionConflicts != null) {
                treeConflict = pb.deletionConflicts.get(db.name);
            }
            if (treeConflict != null) {
                Structure<SvnWcDbConflicts.TreeConflictInfo> treeConflictInfoStructure = SvnWcDbConflicts.readTreeConflict(myWCContext.getDb(), db.localAbsolutePath, treeConflict);
                SVNConflictReason reason = treeConflictInfoStructure.get(SvnWcDbConflicts.TreeConflictInfo.localChange);

                treeConflict = SvnWcDbConflicts.createConflictSkel();

                SvnWcDbConflicts.addTreeConflict(treeConflict, myWCContext.getDb(), db.localAbsolutePath,
                        reason, SVNConflictAction.REPLACE, null);

                db.editConflict = treeConflict;
                treeConflict = null;
                db.shadowed = true;
                conflicted = false;
            } else {
                NodeAlreadyConflictedInfo alreadyConflicted = isNodeAlreadyConflicted(db.localAbsolutePath);
                conflicted = alreadyConflicted.conflicted;
                conflictIgnored = alreadyConflicted.conflictIgnored;
            }

        }
        if (conflicted) {
            rememberSkippedTree(db.localAbsolutePath);
            db.skipThis = true;
            db.alreadyNotified = true;
            myWCContext.getDb().addBaseNotPresentNode(db.localAbsolutePath, db.newRelativePath, myReposRootURL, myReposUuid, myTargetRevision, SVNWCDbKind.Dir, null, null);
            doNotification(db.localAbsolutePath, SVNNodeKind.DIR, SVNEventAction.SKIP_CONFLICTED, db.getURL(), db.getPreviousURL());
            return;
        } else if (conflictIgnored) {
            db.shadowed = true;
        }
        if (db.shadowed) {

        } else if (versionedLocallyAndPresent) {
            SVNWCDbStatus addStatus = SVNWCDbStatus.Normal;
            if (status == SVNWCDbStatus.Added) {
                addStatus = myWCContext.getDb().scanAddition(db.localAbsolutePath, AdditionInfoField.status).status;
            }
            boolean localIsNonDir = wcKind != SVNWCDbKind.Dir && status != SVNWCDbStatus.Deleted;
            if (!myAddsAsModification || localIsNonDir || addStatus != SVNWCDbStatus.Added) {
                treeConflict = checkTreeConflict(db.localAbsolutePath, status, false, wcKind == SVNWCDbKind.Dir ? SVNNodeKind.DIR : SVNNodeKind.FILE, SVNConflictAction.ADD);
            }
            if (treeConflict == null) {
                db.addExisted = true;
            } else {
                db.shadowed = true;
            }
        } else if (kind != SVNNodeKind.NONE) {
            db.obstructionFound = true;
            if (!(kind == SVNNodeKind.DIR && myIsUnversionedObstructionsAllowed)) {
                db.shadowed = true;
                treeConflict = SvnWcDbConflicts.createConflictSkel();
                SvnWcDbConflicts.addTreeConflict(treeConflict, myWCContext.getDb(), db.localAbsolutePath, SVNConflictReason.UNVERSIONED, SVNConflictAction.ADD, null);
                db.editConflict = treeConflict;
            }
        }
        if (treeConflict != null) {
            completeConflict(treeConflict, db.localAbsolutePath, db.oldReposRelPath, db.oldRevision, db.newRelativePath, wcKind == SVNWCDbKind.Dir ? SVNNodeKind.DIR : SVNNodeKind.FILE, SVNNodeKind.DIR);
        }

        myWCContext.getDb().opSetNewDirToIncompleteTemp(db.localAbsolutePath, db.newRelativePath, myReposRootURL, myReposUuid, myTargetRevision, db.ambientDepth,
                db.shadowed && db.obstructionFound, !db.shadowed && status == SVNWCDbStatus.Added, treeConflict, null);
        if (!db.shadowed) {
            SVNFileUtil.ensureDirectoryExists(db.localAbsolutePath);
        }
        if (treeConflict != null) {
            db.alreadyNotified = true;
            doNotification(db.localAbsolutePath, SVNNodeKind.DIR, SVNEventAction.TREE_CONFLICT, null, null);
        }
        if (myWCContext.getEventHandler() != null && !db.alreadyNotified && !db.addExisted) {
            SVNEventAction action;
            if (db.shadowed) {
                action = SVNEventAction.UPDATE_SHADOWED_ADD;
            } else if (db.obstructionFound || db.addExisted) {
                action = SVNEventAction.UPDATE_EXISTS;
            } else {
                action = SVNEventAction.UPDATE_ADD;
            }
            db.alreadyNotified = true;
            doNotification(db.localAbsolutePath, SVNNodeKind.DIR, action, db.getURL(), db.getPreviousURL());

        }
        return;
    }

    public void openDir(String path, long revision) throws SVNException {
        DirectoryBaton pb = myCurrentDirectory;
        myCurrentDirectory = makeDirectoryBaton(path, pb, false);
        DirectoryBaton db = myCurrentDirectory;
        if (db.skipThis) {
            return;
        }

        boolean isWCRoot = myWCContext.getDb().isWCRoot(db.localAbsolutePath);
        if (isWCRoot) {
            rememberSkippedTree(db.localAbsolutePath);
            db.skipThis = true;
            db.alreadyNotified = true;
            doNotification(db.localAbsolutePath, SVNNodeKind.DIR, SVNEventAction.UPDATE_SKIP_OBSTRUCTION, null, null);
            return;
        }

        myWCContext.writeCheck(db.localAbsolutePath);
        WCDbInfo readInfo = myWCContext.getDb().readInfo(db.localAbsolutePath, InfoField.reposRelPath, InfoField.status, InfoField.kind, InfoField.revision, InfoField.changedRev, InfoField.changedDate, InfoField.changedAuthor, InfoField.depth, InfoField.haveWork, InfoField.conflicted, InfoField.haveWork);
        SVNWCDbStatus status = readInfo.status;
        SVNWCDbKind wcKind = readInfo.kind;
        db.oldRevision = readInfo.revision;
        db.ambientDepth = readInfo.depth;
        db.changedRevsion = readInfo.changedRev;
        db.changedAuthor = readInfo.changedAuthor;
        db.changedDate = readInfo.changedDate;
        db.oldReposRelPath = readInfo.reposRelPath;

        boolean haveWork = readInfo.haveWork;
        boolean conflicted = readInfo.conflicted;

        SVNSkel treeConflict = null;
        SVNWCDbStatus baseStatus;
        if (!haveWork) {
            baseStatus = status;
        } else {
            WCDbBaseInfo baseInfo = myWCContext.getDb().getBaseInfo(db.localAbsolutePath, BaseInfoField.status, BaseInfoField.revision, BaseInfoField.reposRelPath,
                    BaseInfoField.changedRev, BaseInfoField.changedDate, BaseInfoField.changedAuthor, BaseInfoField.depth);
            baseStatus = baseInfo.status;
            db.oldRevision = baseInfo.revision;
            db.ambientDepth = baseInfo.depth;
            db.changedAuthor = baseInfo.changedAuthor;
            db.changedDate = baseInfo.changedDate;
            db.changedRevsion = baseInfo.changedRev;
            db.oldReposRelPath = baseInfo.reposRelPath;
        }
        db.wasIncomplete  = baseStatus == SVNWCDbStatus.Incomplete;

        boolean conflictIgnored = false;

        if (db.shadowed) {
            conflicted = false;
        } else if (conflicted) {
            NodeAlreadyConflictedInfo alreadyConflicted = isNodeAlreadyConflicted(db.localAbsolutePath);
            conflicted = alreadyConflicted.conflicted;
            conflictIgnored = alreadyConflicted.conflictIgnored;
        }

        if (conflicted) {
            rememberSkippedTree(db.localAbsolutePath);
            db.skipThis = true;
            db.alreadyNotified = true;
            doNotification(db.localAbsolutePath, SVNNodeKind.UNKNOWN, SVNEventAction.SKIP_CONFLICTED, null, null);
            return;
        } else if (conflictIgnored) {
            db.shadowed = true;
        }
        if (!db.shadowed) {
            treeConflict = checkTreeConflict(db.localAbsolutePath, status, true, wcKind == SVNWCDbKind.Dir ? SVNNodeKind.DIR : SVNNodeKind.FILE, SVNConflictAction.EDIT);
        }
        if (treeConflict != null) {
            db.editConflict = treeConflict;

            Structure<SvnWcDbConflicts.TreeConflictInfo> treeConflictInfoStructure = SvnWcDbConflicts.readTreeConflict(myWCContext.getDb(), db.localAbsolutePath, treeConflict);
            SVNConflictReason reason = treeConflictInfoStructure.get(SvnWcDbConflicts.TreeConflictInfo.localChange);

            assert (reason == SVNConflictReason.DELETED || reason == SVNConflictReason.MOVED_AWAY ||
                    reason == SVNConflictReason.REPLACED || reason == SVNConflictReason.OBSTRUCTED);

            if (reason == SVNConflictReason.OBSTRUCTED) {
                db.editedObstructed = true;
            } else {
                db.shadowed = true;
            }
        }
        myWCContext.getDb().opStartDirectoryUpdateTemp(db.localAbsolutePath, db.newRelativePath, myTargetRevision);
    }

    public void changeDirProperty(String name, SVNPropertyValue value) throws SVNException {
        if (myCurrentDirectory.skipThis) {
            return;
        }

        if (myCurrentDirectory.propChanges == null) {
            myCurrentDirectory.propChanges = new SVNProperties();
        }
        myCurrentDirectory.propChanges.put(name, value);

        // TODO use String pool for property names and some of the values.
        if (!myCurrentDirectory.edited && SVNProperty.isRegularProperty(name)) {
            myCurrentDirectory.markEdited();
        }
    }

    public void closeDir() throws SVNException {
        DirectoryBaton db = myCurrentDirectory;
        if (db.skipThis) {
            maybeBumpDirInfo(db.bumpInfo);
            myCurrentDirectory = myCurrentDirectory.parentBaton;
            return;
        }
        SVNSkel conflictSkel = null;
        if (db.edited) {
            conflictSkel = db.editConflict;
        }
        SVNSkel allWorkItems = null;

        SVNProperties regularProps = new SVNProperties();
        SVNProperties entryProps = new SVNProperties();
        SVNProperties davProps = new SVNProperties();
        SvnNgPropertiesManager.categorizeProperties(db.propChanges, regularProps, entryProps, davProps);

        SVNPropertyValue newWCDavURL = davProps != null ? davProps.getSVNPropertyValue(SVNProperty.WC_URL) : null;
        if (newWCDavURL == null) {
            davProps = null;
        }

        SVNProperties actualProps = null;
        SVNProperties baseProps = null;

        if ((!db.addingDir || db.addExisted) && !db.shadowed) {
            actualProps = myWCContext.getActualProps(db.localAbsolutePath);
        } else {
            actualProps = new SVNProperties();
        }

        if (db.addExisted) {
            baseProps = myWCContext.getDb().readPristineProperties(db.localAbsolutePath);
        } else if (!db.addingDir) {
            baseProps = myWCContext.getDb().getBaseProps(db.localAbsolutePath);
        } else {
            baseProps = new SVNProperties();
        }

        SVNStatusType[] propStatus = new SVNStatusType[] {SVNStatusType.UNKNOWN};
        SVNProperties newBaseProps = null;
        SVNProperties newActualProps = null;
        long newChangedRev = -1;
        SVNDate newChangedDate = null;
        String newChangedAuthor = null;

        if (db.wasIncomplete) {
            SVNProperties propertiesToDelete = new SVNProperties(baseProps);
            if (regularProps == null) {
                regularProps = new SVNProperties();
            }
            for (Iterator<?> names = regularProps.nameSet().iterator(); names.hasNext();) {
                String name = (String) names.next();
                propertiesToDelete.remove(name);
            }
            for (Iterator<?> names = propertiesToDelete.nameSet().iterator(); names.hasNext();) {
                String name = (String) names.next();
                regularProps.put(name, SVNPropertyValue.create(null));
            }
        }
        if (regularProps != null && regularProps.size() > 0) {
            if (myExternalsStore != null) {
                if (regularProps.containsName(SVNProperty.EXTERNALS)) {
                    String newValue = regularProps.getStringValue(SVNProperty.EXTERNALS);
                    String oldValue = baseProps.getStringValue(SVNProperty.EXTERNALS);
                    if (oldValue == null && newValue == null)
                        ;
                    else if (oldValue != null && newValue != null && oldValue.equals(newValue))
                        ;
                    else if (oldValue != null || newValue != null) {
                        myExternalsStore.addExternal(db.localAbsolutePath, oldValue, newValue);
                        myExternalsStore.addDepth(db.localAbsolutePath, db.ambientDepth);
                    }
                }
            }
            if (db.shadowed) {
                if (db.addingDir) {
                    actualProps = new SVNProperties();
                } else {
                    actualProps = baseProps;
                }
            }

            newBaseProps = new SVNProperties(baseProps);
            newBaseProps.putAll(regularProps);

            MergePropertiesInfo mergeProperiesInfo = null;
            mergeProperiesInfo = myWCContext.mergeProperties3(mergeProperiesInfo, db.localAbsolutePath,
                    null, baseProps, actualProps, regularProps);
            newActualProps = mergeProperiesInfo.newActualProperties;
            propStatus[0] = mergeProperiesInfo.mergeOutcome;
            conflictSkel = mergeProperiesInfo.conflictSkel;

            newActualProps.removeNullValues();
            newBaseProps.removeNullValues();
//            allWorkItems = myWCContext.wqMerge(allWorkItems, mergeProperiesInfo.workItems);
        }
        AccumulatedChangeInfo change = accumulateLastChange(db.localAbsolutePath, entryProps);

        newChangedRev = change.changedRev;
        newChangedDate = change.changedDate;
        newChangedAuthor = change.changedAuthor;

        Map<String, SVNDirEntry> newChildren = myDirEntries != null ? myDirEntries.get(db.newRelativePath) : null;
        if (newChildren != null) {
            for(String childName : newChildren.keySet()) {
                SVNDirEntry childEntry = newChildren.get(childName);
                File childAbsPath = SVNFileUtil.createFilePath(db.localAbsolutePath, childName);
                if (db.ambientDepth.compareTo(SVNDepth.IMMEDIATES) < 0 && childEntry.getKind() == SVNNodeKind.DIR) {
                    continue;
                }
                try {
                    myWCContext.getDb().getBaseInfo(childAbsPath, BaseInfoField.status);
                    if (!myWCContext.getDb().isWCRoot(childAbsPath)) {
                        continue;
                    }
                } catch (SVNException e) {
                    if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                        throw e;
                    }
                }
                File childRelPath = SVNFileUtil.createFilePath(db.newRelativePath, childName);
                SVNWCDbKind childKind = childEntry.getKind() == SVNNodeKind.DIR ? SVNWCDbKind.Dir : SVNWCDbKind.File;
                myWCContext.getDb().addBaseNotPresentNode(childAbsPath, childRelPath, myReposRootURL, myReposUuid, myTargetRevision, childKind, null, null);
            }
        }

        if (db.notPresentFiles != null && db.notPresentFiles.size() > 0) {
            for(String fileName : db.notPresentFiles) {
                File childAbsPath = SVNFileUtil.createFilePath(db.localAbsolutePath, fileName);
                File childRelPath = SVNFileUtil.createFilePath(db.newRelativePath, fileName);

                myWCContext.getDb().addBaseNotPresentNode(childAbsPath, childRelPath, myReposRootURL, myReposUuid, myTargetRevision, SVNWCDbKind.File, null, null);
            }
        }

        if (db.parentBaton == null && (!"".equals(myTargetBasename) && myTargetBasename != null)) {
            // there should be no prop changes here.
        } else {
            if (newChangedRev >= 0) {
                db.changedRevsion = newChangedRev;
            }
            if (newChangedDate != null && newChangedDate.getTime() != 0) {
                db.changedDate = newChangedDate;
            }
            if (newChangedAuthor != null) {
                db.changedAuthor = newChangedAuthor;
            }
            if (db.ambientDepth == SVNDepth.UNKNOWN) {
                db.ambientDepth = SVNDepth.INFINITY;
            }
            if (myIsDepthSticky && db.ambientDepth != myRequestedDepth) {
                if (myRequestedDepth == SVNDepth.INFINITY || (db.localAbsolutePath.equals(myTargetAbspath) && myRequestedDepth.compareTo(db.ambientDepth) > 0)) {
                    db.ambientDepth = myRequestedDepth;
                }
            }
            SVNProperties props = newBaseProps;
            if (props == null) {
                props = baseProps;
            }
            if (davProps != null) {
                davProps.removeNullValues();
            }

            Map<String, SVNProperties> iprops = null;

            if (conflictSkel != null) {
                completeConflict(conflictSkel, db.localAbsolutePath, db.oldReposRelPath, db.oldRevision,
                        db.newRelativePath, SVNNodeKind.DIR, SVNNodeKind.DIR);

                SVNSkel workItem = myWCContext.conflictCreateMarker(conflictSkel, db.localAbsolutePath);
                allWorkItems = myWCContext.wqMerge(allWorkItems, workItem);
            }

            if (myInheritableProperties != null) {
                iprops = myInheritableProperties.remove(db.localAbsolutePath);
            }

            myWCContext.getDb().addBaseDirectory(db.localAbsolutePath, db.newRelativePath, myReposRootURL, myReposUuid, myTargetRevision, props, db.changedRevsion, db.changedDate, db.changedAuthor, null, db.ambientDepth,
                    davProps != null && !davProps.isEmpty() ? davProps : null, conflictSkel, !db.shadowed && newBaseProps != null, newActualProps, iprops, allWorkItems);

        }
        myWCContext.wqRun(db.localAbsolutePath);

        if (conflictSkel != null && myConflictHandler != null) {
            myWCContext.invokeConflictResolver(db.localAbsolutePath, conflictSkel, myConflictHandler, ISVNCanceller.NULL);
        }

        if (!db.alreadyNotified && myWCContext.getEventHandler() != null && db.edited) {
            SVNEventAction action = null;
            if (db.shadowed || db.editedObstructed) {
                action = SVNEventAction.UPDATE_SHADOWED_UPDATE;
            } else if (db.obstructionFound || db.addExisted) {
                action = SVNEventAction.UPDATE_EXISTS;
            } else {
                action = SVNEventAction.UPDATE_UPDATE;
            }
            SVNEvent event = new SVNEvent(db.localAbsolutePath, SVNNodeKind.DIR, null, myTargetRevision, null, propStatus[0], null, null, action, null, null, null, null, null, null);
            event.setPreviousRevision(db.oldRevision);
            event.setURL(db.getURL());
            event.setPreviousURL(db.getPreviousURL());
            myWCContext.getEventHandler().handleEvent(event, 0);
        }
        maybeBumpDirInfo(db.bumpInfo);
        myCurrentDirectory = myCurrentDirectory.parentBaton;
    }

    public void addFile(String path, String copyFromPath, long copyFromRevision) throws SVNException {
        assert ((copyFromPath != null && SVNRevision.isValidRevisionNumber(copyFromRevision)) || (copyFromPath == null && !SVNRevision.isValidRevisionNumber(copyFromRevision)));
        DirectoryBaton pb = myCurrentDirectory;
        FileBaton fb = makeFileBaton(pb, path, true);
        myCurrentFile = fb;
        if (fb.skipThis) {
            return;
        }
        fb.markEdited();
        if (SVNFileUtil.getAdminDirectoryName().equals(fb.name)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_OBSTRUCTED_UPDATE, "Failed to add file ''{0}'' : object of the same name as the administrative directory",
                    fb.localAbsolutePath);
            SVNErrorManager.error(err, SVNLogType.WC);
            return;
        }

        SVNNodeKind kind = SVNNodeKind.NONE;
        SVNWCDbKind wcKind = SVNWCDbKind.Unknown;;
        SVNWCDbStatus status = SVNWCDbStatus.Normal;
        boolean conflicted = false;
        boolean versionedLocallyAndPresent = false;
        boolean error = false;
        boolean conflictIgnored = false;
        SVNSkel treeConflict = null;

        if (!myIsCleanCheckout) {
            kind = SVNFileType.getNodeKind(SVNFileType.getType(fb.localAbsolutePath));
            try {
                WCDbInfo readInfo = myWCContext.getDb().readInfo(fb.localAbsolutePath, InfoField.status, InfoField.kind, InfoField.conflicted, InfoField.reposRelPath);
                status = readInfo.status;
                wcKind = readInfo.kind;
                conflicted = readInfo.conflicted;
                versionedLocallyAndPresent = isNodePresent(status);
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                    throw e;
                }
                error = true;
                wcKind = SVNWCDbKind.Unknown;
                conflicted = false;
                versionedLocallyAndPresent = false;
            }
        }

        if (!error) {
            if (wcKind == SVNWCDbKind.Dir && status == SVNWCDbStatus.Normal) {
                if (pb.notPresentFiles == null) {
                    pb.notPresentFiles = new HashSet<String>();
                }
                pb.notPresentFiles.add(fb.name);
                rememberSkippedTree(fb.localAbsolutePath);
                fb.skipThis = true;
                fb.alreadyNotified = true;
                doNotification(fb.localAbsolutePath, SVNNodeKind.FILE, SVNEventAction.UPDATE_SKIP_OBSTRUCTION, fb.getURL(), fb.getPreviousURL());
                return;
            } else if (status == SVNWCDbStatus.Normal && (wcKind == SVNWCDbKind.File || wcKind == SVNWCDbKind.Symlink)) {
                rememberSkippedTree(fb.localAbsolutePath);
                fb.skipThis = true;
                fb.alreadyNotified = true;
                doNotification(fb.localAbsolutePath, SVNNodeKind.FILE, SVNEventAction.UPDATE_SKIP_OBSTRUCTION, fb.getURL(), fb.getPreviousURL());
                return;
            } else if (wcKind == SVNWCDbKind.Unknown) {
                versionedLocallyAndPresent = false;
            } else {
                versionedLocallyAndPresent = isNodePresent(status);
            }
        }

        if (fb.shadowed) {
            conflicted = false;
        } else if (conflicted) {
            if (pb.deletionConflicts != null) {
                treeConflict = pb.deletionConflicts.get(fb.name);
            }
            if (treeConflict != null) {

                Structure<SvnWcDbConflicts.TreeConflictInfo> treeConflictInfoStructure = SvnWcDbConflicts.readTreeConflict(myWCContext.getDb(), fb.localAbsolutePath, treeConflict);
                SVNConflictReason reason = treeConflictInfoStructure.get(SvnWcDbConflicts.TreeConflictInfo.localChange);

                treeConflict = SvnWcDbConflicts.createConflictSkel();
                SvnWcDbConflicts.addTreeConflict(treeConflict, myWCContext.getDb(), fb.localAbsolutePath, reason, SVNConflictAction.REPLACE, null);

                fb.editConflict = treeConflict;

                treeConflict = null;
                fb.shadowed = true;
                conflicted = false;
            } else {
                NodeAlreadyConflictedInfo alreadyConflicted = isNodeAlreadyConflicted(fb.localAbsolutePath);
                conflicted = alreadyConflicted.conflicted;
                conflictIgnored = alreadyConflicted.conflictIgnored;
            }
        }

        if (conflicted) {
            rememberSkippedTree(fb.localAbsolutePath);
            fb.skipThis = true;
            fb.alreadyNotified = true;
            if (pb.notPresentFiles == null) {
                pb.notPresentFiles = new HashSet<String>();
            }
            pb.notPresentFiles.add(fb.name);
            doNotification(fb.localAbsolutePath, SVNNodeKind.UNKNOWN, SVNEventAction.SKIP_CONFLICTED, fb.getURL(), fb.getPreviousURL());
            return;
        } else if (conflictIgnored) {
            fb.shadowed = true;
        }

        if (fb.shadowed) {
            //
        } else if (versionedLocallyAndPresent) {
            boolean localIsFile = false;
            if (status == SVNWCDbStatus.Added) {
                status = myWCContext.getDb().scanAddition(fb.localAbsolutePath, AdditionInfoField.status).status;
            }
            localIsFile = wcKind == SVNWCDbKind.File || wcKind == SVNWCDbKind.Symlink;
            if (!myAddsAsModification || !localIsFile || status != SVNWCDbStatus.Added) {
                treeConflict = checkTreeConflict(fb.localAbsolutePath, status, false, wcKind == SVNWCDbKind.Dir ? SVNNodeKind.DIR : SVNNodeKind.FILE, SVNConflictAction.ADD);
            }
            if (treeConflict == null) {
                fb.addExisted = true;
            } else {
                fb.shadowed = true;
            }
        } else if (kind != SVNNodeKind.NONE) {
            fb.obstructionFound = true;
            if (!(kind == SVNNodeKind.FILE && myIsUnversionedObstructionsAllowed)) {
                fb.shadowed = true;

                treeConflict = SvnWcDbConflicts.createConflictSkel();

                SvnWcDbConflicts.addTreeConflict(treeConflict, myWCContext.getDb(), fb.localAbsolutePath, SVNConflictReason.UNVERSIONED, SVNConflictAction.ADD, null);
            }
        }
        if (pb.parentBaton != null || myTargetBasename == null || "".equals(myTargetBasename) || !fb.localAbsolutePath.equals(myTargetAbspath)) {
            if (pb.notPresentFiles == null) {
                pb.notPresentFiles = new HashSet<String>();
            }
            pb.notPresentFiles.add(fb.name);
        }
        if (treeConflict != null) {
            completeConflict(treeConflict, fb.localAbsolutePath, fb.oldReposRelPath, fb.oldRevision, fb.newRelativePath, wcKind == SVNWCDbKind.Dir ? SVNNodeKind.DIR : SVNNodeKind.FILE, SVNNodeKind.FILE);
            myWCContext.getDb().opMarkConflict(fb.localAbsolutePath, treeConflict, null);
            fb.alreadyNotified = true;
            doNotification(fb.localAbsolutePath, SVNNodeKind.FILE, SVNEventAction.TREE_CONFLICT, fb.getURL(), fb.getPreviousURL());
        }
        return;
    }

    public void openFile(String path, long revision) throws SVNException {
        DirectoryBaton pb = myCurrentDirectory;
        FileBaton fb = makeFileBaton(pb, path, false);
        myCurrentFile = fb;
        if (fb.skipThis) {
            return;
        }
        boolean isRoot = myWCContext.getDb().isWCRoot(fb.localAbsolutePath);
        if (isRoot) {
            rememberSkippedTree(fb.localAbsolutePath);
            fb.skipThis = true;
            fb.alreadyNotified = true;
            doNotification(fb.localAbsolutePath, SVNNodeKind.FILE, SVNEventAction.UPDATE_SKIP_OBSTRUCTION, null, null);
            return;
        }
        boolean conflicted = false;
        boolean conflictIgnored = false;
        SVNWCDbStatus status;
        SVNWCDbKind kind = SVNWCDbKind.Unknown;
        SVNSkel treeConflict = null;

        WCDbInfo readInfo = myWCContext.getDb().readInfo(fb.localAbsolutePath, InfoField.status, InfoField.kind,
                InfoField.revision, InfoField.changedRev, InfoField.changedDate, InfoField.changedRev,
                InfoField.checksum, InfoField.haveWork, InfoField.conflicted, InfoField.propsMod, InfoField.reposRelPath);
        status = readInfo.status;
        fb.changedAuthor = readInfo.changedAuthor;
        fb.changedDate = readInfo.changedDate;
        fb.changedRevison = readInfo.changedRev;
        fb.oldRevision = readInfo.revision;
        fb.originalChecksum = readInfo.checksum;
        fb.oldReposRelPath = readInfo.reposRelPath;
        fb.localPropMods = readInfo.propsMod;

        conflicted = readInfo.conflicted;

        if (readInfo.haveWork) {
            WCDbBaseInfo baseInfo = myWCContext.getDb().getBaseInfo(fb.localAbsolutePath, BaseInfoField.revision, BaseInfoField.reposRelPath,
                    BaseInfoField.changedRev, BaseInfoField.changedAuthor, BaseInfoField.changedDate,
                    BaseInfoField.checksum);
            fb.changedAuthor = baseInfo.changedAuthor;
            fb.changedDate = baseInfo.changedDate;
            fb.changedRevison = baseInfo.changedRev;
            fb.oldRevision = baseInfo.revision;
            fb.originalChecksum = baseInfo.checksum;
            fb.oldReposRelPath = baseInfo.reposRelPath;
        }

        if (fb.shadowed) {
            conflicted = false;
        } else if (conflicted) {
            NodeAlreadyConflictedInfo alreadyConflicted = isNodeAlreadyConflicted(fb.localAbsolutePath);
            conflicted = alreadyConflicted.conflicted;
            conflictIgnored = alreadyConflicted.conflictIgnored;
        }
        if (conflicted ) {
            rememberSkippedTree(fb.localAbsolutePath);
            fb.skipThis  = true;
            fb.alreadyNotified = true;
            doNotification(fb.localAbsolutePath, SVNNodeKind.UNKNOWN, SVNEventAction.SKIP_CONFLICTED, fb.getURL(), fb.getPreviousURL());
            return;
        } else if (conflictIgnored) {
            fb.shadowed = true;
        }
        if (!fb.shadowed) {
            treeConflict = checkTreeConflict(fb.localAbsolutePath, status, true, kind == SVNWCDbKind.Dir ? SVNNodeKind.DIR : SVNNodeKind.FILE, SVNConflictAction.EDIT);
        }
        if (treeConflict != null) {
            fb.editConflict = treeConflict;

            Structure<SvnWcDbConflicts.TreeConflictInfo> treeConflictInfoStructure = SvnWcDbConflicts.readTreeConflict(myWCContext.getDb(), fb.localAbsolutePath, treeConflict);
            SVNConflictReason reason = treeConflictInfoStructure.get(SvnWcDbConflicts.TreeConflictInfo.localChange);

            assert reason == SVNConflictReason.DELETED || reason == SVNConflictReason.MOVED_AWAY ||
                    reason == SVNConflictReason.REPLACED || reason == SVNConflictReason.OBSTRUCTED;

            if (reason == SVNConflictReason.OBSTRUCTED) {
                fb.editObstructed = true;
            } else {
                fb.shadowed = true;
            }
        }
    }

    public void changeFileProperty(String path, String propertyName, SVNPropertyValue propertyValue) throws SVNException {
        FileBaton fb = myCurrentFile;
        if (fb.skipThis) {
            return;
        }

        myCurrentFile.propChanges.put(propertyName, propertyValue);

        if (!fb.edited && SVNProperty.isRegularProperty(propertyName)) {
            fb.markEdited();
        }
        if (!fb.shadowed && (SVNProperty.SPECIAL.equals(propertyName))) {
            boolean becomesSymlink = propertyValue != null;
            boolean wasSymlink;
            boolean modified = false;
            if (fb.addingFile) {
                wasSymlink = becomesSymlink;
            } else {
                final SVNProperties props = myWCContext.getDb().getBaseProps(fb.localAbsolutePath);
                wasSymlink = props != null && props.getSVNPropertyValue(SVNProperty.SPECIAL) != null;
            }
            if (wasSymlink != becomesSymlink) {
                if (fb.localPropMods) {
                    modified = true;
                } else {
                    modified = myWCContext.isTextModified(fb.localAbsolutePath, false);
                }
                if (modified) {
                    if (fb.editConflict == null) {
                        fb.editConflict = SvnWcDbConflicts.createConflictSkel();
                    }

                    SvnWcDbConflicts.addTreeConflict(fb.editConflict, myWCContext.getDb(), fb.localAbsolutePath, SVNConflictReason.EDITED, SVNConflictAction.REPLACE, null);
                    completeConflict(fb.editConflict, fb.localAbsolutePath, fb.oldReposRelPath, fb.oldRevision, fb.newRelativePath, SVNNodeKind.FILE, SVNNodeKind.FILE);

                    ((SVNWCDb)myWCContext.getDb()).opMakeCopy(fb.localAbsolutePath, fb.editConflict, null);
                    doNotification(fb.localAbsolutePath, SVNNodeKind.FILE, SVNEventAction.TREE_CONFLICT, fb.getURL(), fb.getPreviousURL());

                    fb.shadowed = true;
                    fb.addExisted = false;
                    fb.alreadyNotified = true;
                }
            }
        }
        return;
    }

    public void closeFile(String path, String expectedMd5Digest) throws SVNException {
        FileBaton fb = myCurrentFile;
        if (fb.skipThis) {
            maybeBumpDirInfo(fb.bumpInfo);
            return;
        }
        SVNSkel conflictSkel = null;
        if (fb.edited) {
            conflictSkel = fb.editConflict;
        }

        SvnChecksum expectedMd5Checksum = null;
        if (expectedMd5Digest != null) {
            expectedMd5Checksum = new SvnChecksum(SvnChecksum.Kind.md5, expectedMd5Digest);
        }
        if (expectedMd5Checksum != null && fb.newTextBaseMD5Digest != null &&
                !expectedMd5Checksum.getDigest().equals(fb.newTextBaseMD5Digest)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CHECKSUM_MISMATCH, "Checksum mismatch for ''{0}''; expected: ''{1}'', actual: ''{2}''", new Object[] {
                    fb.localAbsolutePath, expectedMd5Checksum, fb.newTextBaseMD5Digest
            });
            SVNErrorManager.error(err, SVNLogType.WC);
        }

        SVNProperties regularPropChanges = new SVNProperties();
        SVNProperties entryProps = new SVNProperties();
        SVNProperties davProps = new SVNProperties();
        SvnNgPropertiesManager.categorizeProperties(fb.propChanges, regularPropChanges, entryProps, davProps);

        SVNPropertyValue newWCDavURL = davProps != null ? davProps.getSVNPropertyValue(SVNProperty.WC_URL) : null;
        if (newWCDavURL == null) {
            davProps = null;
        }

        AccumulatedChangeInfo lastChange = accumulateLastChange(fb.localAbsolutePath, entryProps);
        long newChangedRev = lastChange.changedRev;
        SVNDate newChangedDate = lastChange.changedDate;
        String newChangedAuthor = lastChange.changedAuthor;
        if (newChangedRev >= 0) {
            fb.changedRevison = newChangedRev;
        }
        if (newChangedDate != null) {
            fb.changedDate = newChangedDate;
        }
        if (newChangedAuthor != null) {
            fb.changedAuthor = newChangedAuthor;
        }

        SVNStatusType lockState = SVNStatusType.LOCK_UNCHANGED;
        for (Iterator<?> i = entryProps.nameSet().iterator(); i.hasNext();) {
            String name = (String) i.next();
            if (SVNProperty.LOCK_TOKEN.equals(name)) {
                if (mySwitchRelpath == null || fb.newRelativePath.equals(fb.oldReposRelPath)) {
                assert (entryProps.getStringValue(name) == null);
                myWCContext.getDb().removeLock(fb.localAbsolutePath);
                lockState = SVNStatusType.LOCK_UNLOCKED;
                break;
                }
            }
        }

        SVNProperties localActualProps = null;
        SVNProperties currentBaseProps = null;
        SVNProperties currentActualProps = null;
        if ((!fb.addingFile || fb.addExisted) && !fb.shadowed) {
            localActualProps = myWCContext.getActualProps(fb.localAbsolutePath);
        }
        if (localActualProps == null) {
            localActualProps = new SVNProperties();
        }
        if (fb.addExisted) {
            currentBaseProps = myWCContext.getDb().readPristineProperties(fb.localAbsolutePath);
            currentActualProps = localActualProps;
        } else if (!fb.addingFile) {
            currentBaseProps = myWCContext.getDb().getBaseProps(fb.localAbsolutePath);
            currentActualProps = localActualProps;
        }

        if (currentBaseProps == null) {
            currentBaseProps = new SVNProperties();
        }
        if (currentActualProps == null) {
            currentActualProps = new SVNProperties();
        }

        SVNStatusType[] propState = new SVNStatusType[] {SVNStatusType.UNKNOWN};
        SVNStatusType contentState = null;
        SVNProperties newBaseProps = new SVNProperties();
        SVNProperties newActualProps = new SVNProperties();
        SVNSkel allWorkItems = null;

        boolean keepRecordedInfo = false;
        if (!fb.shadowed) {
            boolean installPristine = false;
            MergePropertiesInfo info = new MergePropertiesInfo();
            info.newActualProperties = newActualProps;
            info.newBaseProperties = new SVNProperties(currentBaseProps);
            info.newBaseProperties.putAll(regularPropChanges);
            info.conflictSkel = conflictSkel;
            info = myWCContext.mergeProperties3(info, fb.localAbsolutePath,
                    null, currentBaseProps, currentActualProps, regularPropChanges);
            newActualProps = info.newActualProperties;
            newBaseProps = info.newBaseProperties;
            propState[0] = info.mergeOutcome;
            conflictSkel = info.conflictSkel;
            allWorkItems = myWCContext.wqMerge(allWorkItems, info.workItems);

            newActualProps.removeNullValues();
            newBaseProps.removeNullValues();

            File installFrom = null;
            if (!fb.obstructionFound && !fb.editObstructed) {
                MergeFileInfo fileInfo = new MergeFileInfo();
                fileInfo.conflictSkel = conflictSkel;
                fileInfo.workItem = allWorkItems;
                try {
                    fileInfo = mergeFile(fb, fileInfo, currentActualProps, fb.changedDate);
                    contentState = fileInfo.contentState;
                    installFrom = fileInfo.installFrom;
                    installPristine = fileInfo.installPristine;
                    conflictSkel = fileInfo.conflictSkel;
                } catch (SVNException e) {
                    if (SVNWCContext.isErrorAccess(e)) {
                        doNotification(fb.localAbsolutePath, SVNNodeKind.FILE, SVNEventAction.UPDATE_SKIP_ACCESS_DENINED, fb.getURL(), fb.getPreviousURL());
                        rememberSkippedTree(fb.localAbsolutePath);
                        fb.skipThis = true;
                        maybeBumpDirInfo(fb.bumpInfo);
                        return;
                    }
                    throw e;
                }
                if (fileInfo != null) {
                    allWorkItems = myWCContext.wqMerge(allWorkItems, fileInfo.workItem);
                }
            } else {
                installPristine = false;
                if (fb.newTextBaseSHA1Checksum != null) {
                    contentState = SVNStatusType.CHANGED;
                } else {
                    contentState = SVNStatusType.UNCHANGED;
                }
            }
            if (installPristine) {
                boolean recordFileInfo = installFrom == null;
                SVNSkel wi = myWCContext.wqBuildFileInstall(fb.localAbsolutePath, installFrom, myIsUseCommitTimes, recordFileInfo);
                allWorkItems = myWCContext.wqMerge(allWorkItems, wi);
            } else if (lockState == SVNStatusType.LOCK_UNLOCKED && !fb.obstructionFound) {
                SVNSkel wi = myWCContext.wqBuildSyncFileFlags(fb.localAbsolutePath);
                allWorkItems = myWCContext.wqMerge(allWorkItems, wi);
            }

            if (!installPristine && contentState == SVNStatusType.UNCHANGED) {
                keepRecordedInfo = true;
            }
            if (installFrom != null && !fb.localAbsolutePath.equals(installFrom)) {
                SVNSkel wi = myWCContext.wqBuildFileRemove(fb.localAbsolutePath, installFrom);
                allWorkItems = myWCContext.wqMerge(allWorkItems, wi);

            }
        } else {
            SVNProperties fakeActualProperties;
            if (fb.addingFile) {
                fakeActualProperties = new SVNProperties();
            } else {
                fakeActualProperties = currentBaseProps;
            }
            MergePropertiesInfo info = new MergePropertiesInfo();
            info.newActualProperties = newActualProps;
            info.newBaseProperties = newBaseProps;
            info.conflictSkel = conflictSkel;
            info = myWCContext.mergeProperties3(info, fb.localAbsolutePath, null, currentBaseProps, fakeActualProperties, regularPropChanges);
            conflictSkel = info.conflictSkel;
            newActualProps = info.newActualProperties;
            newBaseProps = info.newBaseProperties;
            propState[0] = info.mergeOutcome;
            allWorkItems = myWCContext.wqMerge(allWorkItems, info.workItems);
            if (fb.newTextBaseSHA1Checksum != null) {
                contentState = SVNStatusType.CHANGED;
            } else {
                contentState = SVNStatusType.UNCHANGED;
            }
        }

        SvnChecksum newChecksum = fb.newTextBaseSHA1Checksum;
        if (newChecksum == null) {
            newChecksum = fb.originalChecksum;
        }

        if (conflictSkel != null) {
            completeConflict(conflictSkel, fb.localAbsolutePath, fb.oldReposRelPath, fb.oldRevision,
                    fb.newRelativePath, SVNNodeKind.FILE, SVNNodeKind.FILE);
            SVNSkel workItem = myWCContext.conflictCreateMarker(conflictSkel, fb.localAbsolutePath);
            allWorkItems = myWCContext.wqMerge(allWorkItems, workItem);
        }

        if (myInheritableProperties != null) {
            Map<String, SVNProperties> iprops = myInheritableProperties.get(fb.localAbsolutePath);
            if (iprops != null) {
                myInheritableProperties.remove(fb.localAbsolutePath);
            }
        }

        if (davProps != null) {
            davProps.removeNullValues();
        }
        Map<String, SVNProperties> iprops = null;
        if (myInheritableProperties != null) {
            iprops = myInheritableProperties.remove(fb.localAbsolutePath);
        }
        myWCContext.getDb().addBaseFile(fb.localAbsolutePath, fb.newRelativePath, myReposRootURL, myReposUuid,
                myTargetRevision, newBaseProps, fb.changedRevison, fb.changedDate, fb.changedAuthor, newChecksum,
                davProps != null && !davProps.isEmpty() ? davProps : null, fb.addExisted && fb.addingFile,
                !fb.shadowed && newBaseProps != null, newActualProps, keepRecordedInfo, fb.shadowed && fb.obstructionFound,
                iprops, conflictSkel, allWorkItems);

        if (conflictSkel != null && myConflictHandler != null) {
            myWCContext.invokeConflictResolver(fb.localAbsolutePath, conflictSkel, myConflictHandler, ISVNCanceller.NULL);
        }
        if (fb.directoryBaton.notPresentFiles != null) {
            fb.directoryBaton.notPresentFiles.remove(fb.name);
        }

        if (myWCContext.getEventHandler() != null && !fb.alreadyNotified
                && (fb.edited || lockState == SVNStatusType.LOCK_UNLOCKED)) {
            SVNEventAction action = SVNEventAction.UPDATE_UPDATE;

            if (fb.edited) {
                if (fb.shadowed || fb.editObstructed) {
                    action = fb.addingFile ? SVNEventAction.UPDATE_SHADOWED_ADD : SVNEventAction.UPDATE_SHADOWED_UPDATE;
                } else if (fb.obstructionFound || fb.addExisted) {
                    if (contentState != SVNStatusType.CONFLICTED) {
                        action = SVNEventAction.UPDATE_EXISTS;
                    }
                } else if (fb.addingFile) {
                    action = SVNEventAction.UPDATE_ADD;
                }
            } else {
                assert lockState == SVNStatusType.LOCK_UNLOCKED;
                action = SVNEventAction.UPDATE_BROKEN_LOCK;
            }

            String mimeType = myWCContext.getProperty(fb.localAbsolutePath, SVNProperty.MIME_TYPE);
            SVNEvent event = SVNEventFactory.createSVNEvent(fb.localAbsolutePath, SVNNodeKind.FILE, mimeType, myTargetRevision, contentState, propState[0], lockState, action, null, null, null);
            SVNURL url = fb.getURL();
            event.setPreviousRevision(fb.oldRevision);
            event.setURL(url);
            myWCContext.getEventHandler().handleEvent(event, 0);
        }
        maybeBumpDirInfo(fb.bumpInfo);
    }

    public SVNCommitInfo closeEdit() throws SVNException {
        if (!rootOpened && ("".equals(myTargetBasename) || myTargetBasename == null)) {
            myWCContext.getDb().opSetBaseIncompleteTemp(myAnchorAbspath, false);
        }
        if (!myIsTargetDeleted) {
            myWCContext.getDb().opBumpRevisionPostUpdate(myTargetAbspath , myRequestedDepth, mySwitchRelpath, myReposRootURL, myReposUuid, myTargetRevision, mySkippedTrees, myInheritableProperties, myWCContext.getEventHandler());
            if (myTargetBasename == null || "".equals(myTargetBasename)) {
                SVNWCDbStatus status = null;
                boolean error = false;
                try {
                    status = myWCContext.getDb().getBaseInfo(myTargetAbspath, BaseInfoField.status).status;
                } catch (SVNException e) {
                    if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                        throw e;
                    }
                    error = true;
                }
                if (!error && status == SVNWCDbStatus.Excluded) {
                    myWCContext.getDb().removeBase(myTargetAbspath);
                }
            }
        }

        myWCContext.wqRun(myAnchorAbspath);
        return null;
    }

    public void abortEdit() throws SVNException {
    }

    public void applyTextDelta(String path, String expectedChecksum) throws SVNException {
        FileBaton fb = myCurrentFile;
        if (fb.skipThis) {
            return;
        }
        fb.markEdited();
        SvnChecksum expectedBaseChecksum = expectedChecksum != null ? new SvnChecksum(SvnChecksum.Kind.md5, expectedChecksum) : null;
        SvnChecksum recordedBaseChecksum = fb.originalChecksum;

        if (recordedBaseChecksum != null && expectedBaseChecksum != null && recordedBaseChecksum.getKind() != SvnChecksum.Kind.md5) {
            recordedBaseChecksum = myWCContext.getDb().getPristineMD5(myAnchorAbspath, recordedBaseChecksum);
        }
        if (recordedBaseChecksum != null && expectedBaseChecksum != null && !expectedBaseChecksum.equals(recordedBaseChecksum)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_CORRUPT_TEXT_BASE, "Checksum mismatch for ''{0}'':\n " + "   expected:  ''{1}''\n" + "   recorded:  ''{2}''\n",
                    new Object[] {
                            fb.localAbsolutePath, expectedBaseChecksum, recordedBaseChecksum
                    });
            SVNErrorManager.error(err, SVNLogType.WC);
            return;

        }
        InputStream source;
        if (!fb.addingFile) {
            source = myWCContext.getDb().readPristine(fb.localAbsolutePath, fb.originalChecksum);
            if (source == null) {
                source = SVNFileUtil.DUMMY_IN;
            }
        } else {
            source = SVNFileUtil.DUMMY_IN;
        }
        if (recordedBaseChecksum == null) {
            recordedBaseChecksum = expectedBaseChecksum;
        }

        if (recordedBaseChecksum != null) {
            fb.expectedSourceChecksum = new SvnChecksum(recordedBaseChecksum.getKind(), recordedBaseChecksum.getDigest());
            if (source != SVNFileUtil.DUMMY_IN) {
                source = new SVNChecksumInputStream(source, SVNChecksumInputStream.MD5_ALGORITHM);
                fb.sourceChecksumStream = (SVNChecksumInputStream) source;
            }
        }
        WritableBaseInfo openWritableBase = myWCContext.openWritableBase(fb.localAbsolutePath, false, true);
        OutputStream target = openWritableBase.stream;
        fb.newTextBaseTmpAbsPath = openWritableBase.tempBaseAbspath;
        myDeltaProcessor.applyTextDelta(source, target, true);
        fb.newTextBaseSHA1ChecksumStream = openWritableBase.sha1ChecksumStream;
    }

    public OutputStream textDeltaChunk(String path, SVNDiffWindow diffWindow) throws SVNException {
        if (myCurrentFile.skipThis) {
            return SVNFileUtil.DUMMY_OUT;
        }
        try {
            myDeltaProcessor.textDeltaChunk(diffWindow);
        } catch (SVNException svne) {
            myDeltaProcessor.textDeltaEnd();
            SVNFileUtil.deleteFile(myCurrentFile.newTextBaseTmpAbsPath);
            myCurrentFile.newTextBaseTmpAbsPath = null;
            throw svne;
        }
        return SVNFileUtil.DUMMY_OUT;
    }

    public void textDeltaEnd(String path) throws SVNException {
        if (myCurrentFile.skipThis) {
            return;
        }
        myCurrentFile.newTextBaseMD5Digest = myDeltaProcessor.textDeltaEnd();
        if (myCurrentFile.newTextBaseSHA1ChecksumStream != null) {
            myCurrentFile.newTextBaseSHA1Checksum = new SvnChecksum(SvnChecksum.Kind.sha1, myCurrentFile.newTextBaseSHA1ChecksumStream.getDigest());
        }

        if (myCurrentFile.expectedSourceChecksum != null && myCurrentFile.expectedSourceChecksum.getKind() == SvnChecksum.Kind.md5) {
            String actualSourceChecksum = myCurrentFile.sourceChecksumStream != null ? myCurrentFile.sourceChecksumStream.getDigest() : null;
            if (!myCurrentFile.expectedSourceChecksum.getDigest().equals(actualSourceChecksum)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_CORRUPT_TEXT_BASE, "Checksum mismatch while updating ''{0}''; expected: ''{1}'', actual: ''{2}''", new Object[] {
                        myCurrentFile.localAbsolutePath, myCurrentFile.expectedSourceChecksum.getDigest(), actualSourceChecksum
                });
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        }
        if (myCurrentFile.newTextBaseTmpAbsPath != null && myCurrentFile.newTextBaseSHA1Checksum != null && myCurrentFile.newTextBaseMD5Digest != null) {
            myWCContext.getDb().installPristine(myCurrentFile.newTextBaseTmpAbsPath, myCurrentFile.newTextBaseSHA1Checksum,
                    new SvnChecksum(SvnChecksum.Kind.md5, myCurrentFile.newTextBaseMD5Digest));
        }
        if (myCurrentFile.originalChecksum != null && myCurrentFile.newTextBaseSHA1Checksum != null && myCurrentFile.originalChecksum.equals(myCurrentFile.newTextBaseSHA1Checksum)) {
            myCurrentFile.newTextBaseSHA1Checksum = null;
        }
    }

    private static class BumpDirectoryInfo {
        private int refCount;
        private BumpDirectoryInfo parent;
    }

    private class DirectoryBaton {
        private String name;
        private File localAbsolutePath;
        private File newRelativePath;
        private long oldRevision;

        private File oldReposRelPath;

        DirectoryBaton parentBaton;

        private boolean skipThis;
        private boolean alreadyNotified;
        private boolean addingDir;
        private boolean shadowed;
        private boolean editedObstructed;

        private long changedRevsion;
        private SVNDate changedDate;
        private String changedAuthor;

        private Map<String, SVNSkel> deletionConflicts;
        private Set<String> notPresentFiles;

        private boolean obstructionFound;
        private boolean addExisted;

        private SVNProperties propChanges;

        private boolean edited;
        private SVNSkel editConflict;

        private BumpDirectoryInfo bumpInfo;
        private SVNDepth ambientDepth;
        private boolean wasIncomplete;

        public void markEdited() throws SVNException {
            if (edited) {
                return;
            }
            if (parentBaton != null) {
                parentBaton.markEdited();
            }
            edited = true;
            if (editConflict != null) {
                completeConflict(editConflict, localAbsolutePath, oldReposRelPath, oldRevision, newRelativePath, SVNNodeKind.DIR, SVNNodeKind.DIR);
                myWCContext.getDb().opMarkConflict(localAbsolutePath, editConflict, null);

                doNotification(localAbsolutePath, SVNNodeKind.DIR, SVNEventAction.TREE_CONFLICT, getURL(), getPreviousURL());
                alreadyNotified = true;
            }
        }

        public SVNURL getURL() throws SVNException {
            if (newRelativePath != null) {
                return SVNWCUtils.join(myReposRootURL, newRelativePath);
            }
            return null;
        }

        public SVNURL getPreviousURL() throws SVNException {
            if (oldReposRelPath != null) {
                return SVNWCUtils.join(myReposRootURL, oldReposRelPath);
            }
            return null;
        }
    }

    private class FileBaton {
        private String name;
        private File localAbsolutePath;
        private File newRelativePath;
        private long oldRevision;

        private File oldReposRelPath;

        private DirectoryBaton directoryBaton;

        private boolean skipThis;
        private boolean alreadyNotified;
        private boolean addingFile;
        private boolean obstructionFound;
        private boolean addExisted;
        private boolean shadowed;
        private boolean editObstructed;

        private long changedRevison;
        private SVNDate changedDate;
        private String changedAuthor;

        private SvnChecksum newTextBaseMd5Checksum;
        private SvnChecksum newTextBaseSha1Checksum;

        private SvnChecksum originalChecksum;

        private SVNProperties propChanges;

        private boolean localPropMods;

        private BumpDirectoryInfo bumpInfo;

        private boolean edited;
        private SVNSkel editConflict;

        public void markEdited() throws SVNException {
            if (edited) {
                return;
            }

            if (directoryBaton != null) {
                directoryBaton.markEdited();
            }
            edited = true;
            if (editConflict != null) {
                completeConflict(editConflict, localAbsolutePath, oldReposRelPath, oldRevision, newRelativePath, SVNNodeKind.FILE, SVNNodeKind.FILE);
                myWCContext.getDb().opMarkConflict(localAbsolutePath, editConflict, null);

                doNotification(localAbsolutePath, SVNNodeKind.FILE, SVNEventAction.TREE_CONFLICT, getURL(), getPreviousURL());
                alreadyNotified = true;
            }
        }

        public SVNURL getURL() throws SVNException {
            if (newRelativePath != null) {
                return SVNWCUtils.join(myReposRootURL, newRelativePath);
            }
            return null;
        }

        public SVNURL getPreviousURL() throws SVNException {
            if (oldReposRelPath != null) {
                return SVNWCUtils.join(myReposRootURL, oldReposRelPath);
            }
            return null;
        }

        // delta handling
        File newTextBaseTmpAbsPath;
        SVNChecksumInputStream sourceChecksumStream;
        SVNChecksumOutputStream newTextBaseSHA1ChecksumStream;

        SvnChecksum expectedSourceChecksum;
        String newTextBaseMD5Digest;
        public SvnChecksum newTextBaseSHA1Checksum;
    }

    private DirectoryBaton makeDirectoryBaton(String path, DirectoryBaton parent, boolean adding) throws SVNException {
        DirectoryBaton d = new DirectoryBaton();
        if (path != null) {
            d.name = SVNPathUtil.tail(path);
            d.localAbsolutePath = SVNFileUtil.createFilePath(parent.localAbsolutePath, d.name);
        } else {
            d.name = null;
            d.localAbsolutePath = myAnchorAbspath;
        }

        if (mySwitchRelpath != null) {
            if (parent == null ) {
                if ("".equals(myTargetBasename) || myTargetBasename == null) {
                    d.newRelativePath = mySwitchRelpath;
                } else {
                    d.newRelativePath = myWCContext.getDb().scanBaseRepository(d.localAbsolutePath, RepositoryInfoField.relPath).relPath;
                }
            } else {
                if (parent.parentBaton ==null && myTargetBasename.equals(d.name)) {
                    d.newRelativePath = mySwitchRelpath;
                } else {
                    d.newRelativePath = SVNFileUtil.createFilePath(parent.newRelativePath, d.name);
                }
            }
        } else {
            if (adding) {
                d.newRelativePath = SVNFileUtil.createFilePath(parent.newRelativePath, d.name);
            } else {
                d.newRelativePath = myWCContext.getDb().scanBaseRepository(d.localAbsolutePath, RepositoryInfoField.relPath).relPath;
            }
        }
        BumpDirectoryInfo bdi = new BumpDirectoryInfo();
        bdi.parent = parent != null ? parent.bumpInfo : null;
        bdi.refCount = 1;

        if (parent != null) {
            bdi.parent.refCount++;
        }
        d.parentBaton = parent;
        d.bumpInfo = bdi;
        d.oldRevision = -1;
        d.addingDir = adding;
        d.changedRevsion = -1;
        d.notPresentFiles =  new HashSet<String>();
        if (parent != null) {
            d.skipThis = parent.skipThis;
            d.shadowed = parent.shadowed || parent.editedObstructed;
        }
        d.ambientDepth = SVNDepth.UNKNOWN;
        d.propChanges = new SVNProperties();
        return d;
    }

    private FileBaton makeFileBaton(DirectoryBaton parent, String path, boolean adding) throws SVNException {
        FileBaton f = new FileBaton();
        f.name = SVNPathUtil.tail(path);
        f.oldRevision = -1;
        f.localAbsolutePath = SVNFileUtil.createFilePath(parent.localAbsolutePath, f.name);

        if (mySwitchRelpath != null) {
            if (parent.parentBaton == null && myTargetBasename != null && f.name.equals(myTargetBasename)) {
                f.newRelativePath = mySwitchRelpath;
            } else {
                f.newRelativePath = SVNFileUtil.createFilePath(parent.newRelativePath, f.name);
            }
        } else {
            if (adding) {
                f.newRelativePath = SVNFileUtil.createFilePath(parent.newRelativePath, f.name);
            } else {
                f.newRelativePath = myWCContext.getDb().scanBaseRepository(f.localAbsolutePath, RepositoryInfoField.relPath).relPath;
            }
        }
        f.bumpInfo = parent.bumpInfo;
        f.addingFile = adding;
        f.skipThis = parent.skipThis;
        f.shadowed = parent.shadowed | parent.editedObstructed;
        f.directoryBaton = parent;
        f.changedRevison = -1;
        f.bumpInfo.refCount++;
        f.propChanges = new SVNProperties();

        return f;
    }

    private static boolean isNodePresent(SVNWCDbStatus status) {
        return status != SVNWCDbStatus.ServerExcluded && status != SVNWCDbStatus.Excluded && status != SVNWCDbStatus.NotPresent;
    }

    private SVNTreeConflictDescription createTreeConflict(File localAbspath, SVNConflictReason reason, SVNConflictAction action, SVNNodeKind theirNodeKind, File theirRelpath) throws SVNException {
        assert (reason != null);
        File leftReposRelpath;
        long leftRevision;
        SVNNodeKind leftKind = null;
        File addedReposRelpath = null;
        SVNURL reposRootUrl;
        if (reason == SVNConflictReason.ADDED) {
            leftKind = SVNNodeKind.NONE;
            leftRevision = SVNWCContext.INVALID_REVNUM;
            leftReposRelpath = null;
            WCDbAdditionInfo scanAddition = myWCContext.getDb().scanAddition(localAbspath, AdditionInfoField.status, AdditionInfoField.reposRelPath, AdditionInfoField.reposRootUrl);
            SVNWCDbStatus addedStatus = scanAddition.status;
            addedReposRelpath = scanAddition.reposRelPath;
            reposRootUrl = scanAddition.reposRootUrl;
            assert (addedStatus == SVNWCDbStatus.Added || addedStatus == SVNWCDbStatus.Copied || addedStatus == SVNWCDbStatus.MovedHere);
        } else if (reason == SVNConflictReason.UNVERSIONED) {
            leftKind = SVNNodeKind.NONE;
            leftRevision = SVNWCContext.INVALID_REVNUM;
            leftReposRelpath = null;
            reposRootUrl = myReposRootURL;
        } else {
            assert (reason == SVNConflictReason.EDITED || reason == SVNConflictReason.DELETED || reason == SVNConflictReason.REPLACED || reason == SVNConflictReason.OBSTRUCTED);
            WCDbBaseInfo baseInfo = myWCContext.getDb().getBaseInfo(localAbspath, BaseInfoField.kind, BaseInfoField.revision, BaseInfoField.reposRelPath, BaseInfoField.reposRootUrl);
            SVNWCDbKind baseKind = baseInfo.kind;
            leftRevision = baseInfo.revision;
            leftReposRelpath = baseInfo.reposRelPath;
            reposRootUrl = baseInfo.reposRootUrl;
            if (baseKind == SVNWCDbKind.File || baseKind == SVNWCDbKind.Symlink)
                leftKind = SVNNodeKind.FILE;
            else if (baseKind == SVNWCDbKind.Dir)
                leftKind = SVNNodeKind.DIR;
            else {
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.ASSERTION_FAIL), SVNLogType.WC);
            }
        }
        assert (reposRootUrl.equals(myReposRootURL));
        File rightReposRelpath;
        if (mySwitchRelpath != null) {
            if (theirRelpath != null) {
                rightReposRelpath = theirRelpath;
            } else {
                rightReposRelpath = mySwitchRelpath;
                rightReposRelpath = SVNFileUtil.createFilePath(rightReposRelpath.getPath() + "_THIS_IS_INCOMPLETE");
            }
        } else {
            rightReposRelpath = (reason == SVNConflictReason.ADDED ? addedReposRelpath : leftReposRelpath);
            if (rightReposRelpath == null) {
                rightReposRelpath = theirRelpath;
            }
        }
        assert (rightReposRelpath != null);
        SVNNodeKind conflictNodeKind = (action == SVNConflictAction.DELETE ? leftKind : theirNodeKind);
        assert (conflictNodeKind == SVNNodeKind.FILE || conflictNodeKind == SVNNodeKind.DIR);
        SVNConflictVersion srcLeftVersion;
        if (leftReposRelpath == null) {
            srcLeftVersion = null;
        } else {
            srcLeftVersion = new SVNConflictVersion(reposRootUrl, leftReposRelpath.getPath(), leftRevision, leftKind);
        }
        SVNConflictVersion srcRightVersion = new SVNConflictVersion(reposRootUrl, rightReposRelpath.getPath(), myTargetRevision, theirNodeKind);
        return new SVNTreeConflictDescription(localAbspath, conflictNodeKind, action, reason, mySwitchRelpath != null ? SVNOperation.SWITCH : SVNOperation.UPDATE, srcLeftVersion, srcRightVersion);
    }

    private void maybeBumpDirInfo(BumpDirectoryInfo bdi) throws SVNException {
        while (bdi != null) {
            if (--bdi.refCount > 0) {
                return;
            }
//            myWcContext.getDb().opSetBaseIncompleteTemp(bdi.absPath, false);
            bdi = bdi.parent;
        }
        return;
    }

    public static class AccumulatedChangeInfo {

        public long changedRev;
        public SVNDate changedDate;
        public String changedAuthor;
    }

    public static AccumulatedChangeInfo accumulateLastChange(File localAbspath, SVNProperties entryProps) throws SVNException {
        AccumulatedChangeInfo info = new AccumulatedChangeInfo();
        info.changedRev = SVNWCContext.INVALID_REVNUM;
        info.changedDate = null;
        info.changedAuthor = null;
        if (entryProps != null) {
            for (Iterator<?> i = entryProps.nameSet().iterator(); i.hasNext();) {
                String propertyName = (String) i.next();
                String propertyValue = entryProps.getStringValue(propertyName);
                if (propertyValue == null) {
                    continue;
                }
                if (SVNProperty.LAST_AUTHOR.equals(propertyName)) {
                    info.changedAuthor = propertyValue;
                } else if (SVNProperty.COMMITTED_REVISION.equals(propertyName)) {
                    info.changedRev = Long.valueOf(propertyValue);
                } else if (SVNProperty.COMMITTED_DATE.equals(propertyName)) {
                    info.changedDate = SVNDate.parseDate(propertyValue);
                }
            }
        }
        return info;
    }

    private static class MergeFileInfo {

        public SVNSkel workItem;
        public boolean installPristine;
        public File installFrom;
        public SVNStatusType contentState;
        public SVNSkel conflictSkel;
    }

    public static MergeInfo performFileMerge(MergeInfo mergeInfo, SVNWCContext context, File localAbsPath, File wriAbsPath, SvnChecksum newChecksum, SvnChecksum originalChecksum,
            SVNProperties actualProperties, String[] extPatterns,
            long oldRevision, long targetRevision, SVNProperties propChanges) throws SVNException {
        File mergeLeft = null;
        boolean deleteLeft = false;
        File newTextBaseTmpAbsPath = context.getDb().getPristinePath(wriAbsPath, newChecksum);
        if (extPatterns != null && extPatterns.length > 0) {

        }
        if (oldRevision < 0) {
            oldRevision = 0;
        }
        String oldRevStr = String.format(".r%s%s%s", oldRevision, "", "");
        String newRevStr = String.format(".r%s%s%s", targetRevision, "", "");
        String mineStr = String.format(".mine%s%s", "", "");

        if (originalChecksum == null) {
            File tmpDir = context.getDb().getWCRootTempDir(wriAbsPath);
            mergeLeft = SVNFileUtil.createUniqueFile(tmpDir, "file", "tmp", false);
            deleteLeft = true;
        } else {
            mergeLeft = context.getDb().getPristinePath(wriAbsPath, originalChecksum);
        }

        mergeInfo = context.merge(mergeInfo.workItems, mergeInfo.conflictSkel, mergeLeft, newTextBaseTmpAbsPath, localAbsPath, wriAbsPath,
                oldRevStr, newRevStr, mineStr, actualProperties, false, null, propChanges);
        mergeInfo.foundTextConflict = mergeInfo.mergeOutcome == SVNStatusType.CONFLICTED;
        if (deleteLeft) {
            SVNSkel workItem = context.wqBuildFileRemove(wriAbsPath, mergeLeft);
            context.wqMerge(mergeInfo.workItems, workItem);
        }
        return mergeInfo;
    }

    private MergeFileInfo mergeFile(FileBaton fb, MergeFileInfo mergeFileInfo, SVNProperties actualProps, SVNDate lastChangedDate) throws SVNException {
        DirectoryBaton pb = fb.directoryBaton;
        boolean isLocallyModified;
        boolean magicPropsChanged= false;
        boolean foundTextConflict = false;

        assert !fb.shadowed && !fb.obstructionFound && !fb.editObstructed;

        if (mergeFileInfo == null) {
            mergeFileInfo = new MergeFileInfo();
        }
        mergeFileInfo.workItem = null;
        mergeFileInfo.installPristine = false;
        mergeFileInfo.contentState = SVNStatusType.UNCHANGED;
        mergeFileInfo.installFrom = null;

        if (fb.addingFile && !fb.addExisted) {
            isLocallyModified = false;
        } else {
            isLocallyModified = myWCContext.isTextModified(fb.localAbsolutePath, false);
        }

        SVNProperties propChanges = fb.propChanges;

        if (!isLocallyModified && fb.newTextBaseSHA1Checksum != null) {
            mergeFileInfo.installPristine = true;
        } else if (fb.newTextBaseSHA1Checksum != null) {
            MergeInfo mergeInfo = new MergeInfo();
            mergeInfo.conflictSkel = mergeFileInfo.conflictSkel;
            mergeInfo.workItems = mergeFileInfo.workItem;
            mergeInfo = performFileMerge(mergeInfo, myWCContext,
                    fb.localAbsolutePath,
                    pb.localAbsolutePath,
                    fb.newTextBaseSHA1Checksum,
                    fb.addExisted ? null : fb.originalChecksum,
                    actualProps,
                    myExtensionPatterns,
                    fb.oldRevision,
                    myTargetRevision,
                    propChanges);
            mergeFileInfo.workItem = myWCContext.wqMerge(mergeFileInfo.workItem, mergeInfo.workItems);
            mergeFileInfo.contentState = mergeInfo.mergeOutcome;
            mergeFileInfo.conflictSkel = mergeInfo.conflictSkel;
            foundTextConflict = mergeInfo.foundTextConflict;
        } else {
            magicPropsChanged = myWCContext.hasMagicProperty(propChanges);
            TranslateInfo translateInfo = myWCContext.getTranslateInfo(fb.localAbsolutePath, actualProps, true, false, false, true, false);
            if (magicPropsChanged || (translateInfo.keywords != null && !translateInfo.keywords.isEmpty())) {
                if (isLocallyModified) {
                    File tmpText = null;

                    tmpText = myWCContext.getTranslatedFile(fb.localAbsolutePath, fb.localAbsolutePath, true, true, false, false, false);
                    mergeFileInfo.installPristine = true;
                    mergeFileInfo.installFrom = tmpText;
                } else {
                    mergeFileInfo.installPristine = true;
                }
            }
        }

        if (foundTextConflict) {
            mergeFileInfo.contentState = SVNStatusType.CONFLICTED;
        } else if (fb.newTextBaseSHA1Checksum != null) {
            if (isLocallyModified) {
                mergeFileInfo.contentState = SVNStatusType.MERGED;
            } else {
                mergeFileInfo.contentState = SVNStatusType.CHANGED;
            }
        } else {
            mergeFileInfo.contentState = SVNStatusType.UNCHANGED;
        }
        return mergeFileInfo;
    }

    public void completeConflict(SVNSkel conflict,
                                    File localAbsPath, File oldReposRelPath,
                                    long oldRevision, File newReposRelPath,
                                    SVNNodeKind localKind, SVNNodeKind targetKind) throws SVNException {
        SVNConflictVersion originalVersion;
        SVNConflictVersion targetVersion;

        if (conflict == null) {
            return;
        }

        boolean isComplete = SvnWcDbConflicts.isConflictSkelComplete(conflict);
        if (isComplete) {
            return;
        }

        if (oldReposRelPath != null) {
            originalVersion = new SVNConflictVersion(myReposRootURL, SVNFileUtil.getFilePath(oldReposRelPath), oldRevision, localKind);
        } else {
            originalVersion = null;
        }

        if (newReposRelPath != null) {
            targetVersion = new SVNConflictVersion(myReposRootURL, SVNFileUtil.getFilePath(newReposRelPath), myTargetRevision, targetKind);
        } else {
            targetVersion = null;
        }

        if (mySwitchRelpath != null) {
            SvnWcDbConflicts.conflictSkelOpSwitch(conflict, originalVersion, targetVersion);
        } else {
            SvnWcDbConflicts.conflictSkelOpUpdate(conflict, originalVersion, targetVersion);
        }
    }
}
