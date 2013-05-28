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

import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
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
import org.tmatesoft.svn.core.io.diff.SVNDeltaProcessor;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;
import org.tmatesoft.svn.core.wc.SVNConflictAction;
import org.tmatesoft.svn.core.wc.SVNConflictDescription;
import org.tmatesoft.svn.core.wc.SVNConflictReason;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNOperation;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNTreeConflictDescription;
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver;
import org.tmatesoft.svn.core.wc2.SvnChecksum;
import org.tmatesoft.svn.core.wc2.SvnStatus;
import org.tmatesoft.svn.core.wc2.SvnTarget;
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
    
    public static ISVNUpdateEditor createUpdateEditor(SVNWCContext context, 
            long targetRevision, 
            File anchorAbspath, 
            String targetName, 
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
            String[] preservedExtensions) throws SVNException {        
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
        
        if (dirFetcher != null) {
            editor.initExcludedDirectoryEntries(dirFetcher);
        }
        
        ISVNUpdateEditor result = editor;
        if (!serverPerformsFiltering && !depthIsSticky) {
            result = new SVNAmbientDepthFilterEditor17(result, context, anchorAbspath, targetName, true);
        }
        return (ISVNUpdateEditor) SVNCancellableEditor.newInstance(result, context.getEventHandler(), null);
    }
    
    

    public static ISVNUpdateEditor createUpdateEditor(SVNWCContext wcContext, File anchorAbspath, String target, SVNURL reposRoot, SVNURL switchURL, SVNExternalsStore externalsStore,
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
        return new SVNUpdateEditor17(wcContext, anchorAbspath, target, info.reposRootUrl, info.reposUuid, switchURL, externalsStore, allowUnversionedObstructions, depthIsSticky, depth, preservedExts,
                dirFetcher);
    }
    
    private SVNUpdateEditor17() {  
        myDirEntries = new HashMap<File, Map<String,SVNDirEntry>>();
        myDeltaProcessor = new SVNDeltaProcessor();
        mySkippedTrees = new HashSet<File>();
    }

    public SVNUpdateEditor17(SVNWCContext wcContext, File anchorAbspath, String targetBasename, SVNURL reposRootUrl, String reposUuid, SVNURL switchURL, SVNExternalsStore externalsStore,
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
        rootOpened = true;
        myCurrentDirectory = makeDirectoryBaton(null, null, false);
        try {
            alreadyConflicted = alreadyInATreeConflict(myCurrentDirectory.localAbsolutePath);
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                throw e;
            }
            alreadyConflicted = false;
        }
        if (alreadyConflicted) {
            myCurrentDirectory.skipThis = true;
            myCurrentDirectory.alreadyNotified = true;
            doNotification(myTargetAbspath, SVNNodeKind.UNKNOWN, SVNEventAction.SKIP_CONFLICTED, null, null);
            return;
        }
        
        if ("".equals(myTargetBasename) || myTargetBasename == null) {
            WCDbBaseInfo baseInfo = myWCContext.getDb().getBaseInfo(myCurrentDirectory.localAbsolutePath, BaseInfoField.reposRelPath, BaseInfoField.changedAuthor, BaseInfoField.changedDate, BaseInfoField.changedRev, BaseInfoField.status, BaseInfoField.depth);
            
            myCurrentDirectory.ambientDepth = baseInfo.depth;
            myCurrentDirectory.wasIncomplete = baseInfo.status == SVNWCDbStatus.Incomplete;
            myCurrentDirectory.changedAuthor = baseInfo.changedAuthor;
            myCurrentDirectory.changedDate = baseInfo.changedDate;
            myCurrentDirectory.changedRevsion = baseInfo.changedRev;
            myCurrentDirectory.oldRelativePath = baseInfo.reposRelPath;
            
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

    private boolean alreadyInATreeConflict(File localAbspath) throws SVNException {
        assert (SVNFileUtil.isAbsolute(localAbspath));
        File ancestorAbspath = localAbspath;
        boolean conflicted = false;
        while (true && ancestorAbspath != null) {
            SVNConflictDescription conflict;
            WCDbInfo readInfo = myWCContext.getDb().readInfo(ancestorAbspath, InfoField.conflicted);
            if (readInfo.conflicted) {
                conflict = myWCContext.getDb().opReadTreeConflict(ancestorAbspath);
                if (conflict != null) {
                    return true;
                }
            }
            if (myWCContext.getDb().isWCRoot(ancestorAbspath)) {
                break;
            }
            ancestorAbspath = SVNFileUtil.getParentFile(ancestorAbspath);
        }
        return conflicted;
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
        
        boolean deletingTarget = localAbsPath.equals(myTargetAbspath);
        SVNWCDbStatus baseStatus;
        SVNWCDbKind baseKind;
        
        WCDbInfo info = myWCContext.getDb().readInfo(localAbsPath, InfoField.status, InfoField.kind, InfoField.reposRelPath, InfoField.conflicted, InfoField.haveBase, InfoField.haveWork);
        SVNURL previousURL = info.reposRelPath != null ? SVNWCUtils.join(myReposRootURL, info.reposRelPath) : null;
        
        if (!info.haveWork) {
            baseStatus = info.status;
            baseKind = info.kind;
        } else {
            WCDbBaseInfo baseInfo = myWCContext.getDb().getBaseInfo(localAbsPath, BaseInfoField.status, BaseInfoField.kind, BaseInfoField.reposRelPath);
            baseStatus = baseInfo.status;
            baseKind = baseInfo.kind;
            info.reposRelPath = baseInfo.reposRelPath;
        }
        
        if (info.conflicted) {
            info.conflicted = isNodeAlreadyConflicted(localAbsPath);
        }
        if (info.conflicted) {
            rememberSkippedTree(localAbsPath);
            doNotification(localAbsPath, SVNNodeKind.UNKNOWN, SVNEventAction.SKIP_CONFLICTED, null, null);
            return;
        }
        
        if (baseStatus == SVNWCDbStatus.NotPresent || baseStatus == SVNWCDbStatus.Excluded || baseStatus == SVNWCDbStatus.ServerExcluded) {
            myWCContext.getDb().removeBase(localAbsPath);
            if (deletingTarget) {
                myIsTargetDeleted = true;
            }
            return;
        }
     
        SVNTreeConflictDescription treeConflict = null;
        if (!myCurrentDirectory.shadowed) {
            treeConflict = checkTreeConflict(localAbsPath, info.status, info.kind, true, SVNConflictAction.DELETE, SVNNodeKind.NONE, info.reposRelPath);
        }
        if (treeConflict != null) {
            if (myCurrentDirectory.deletionConflicts == null) {
                myCurrentDirectory.deletionConflicts = new HashMap<String, SVNTreeConflictDescription>();
            }
            myCurrentDirectory.deletionConflicts.put(base, treeConflict);
            myWCContext.getDb().opSetTreeConflict(localAbsPath, treeConflict);
            doNotification(localAbsPath, SVNNodeKind.UNKNOWN, SVNEventAction.TREE_CONFLICT, null, null);
            
            if (treeConflict.getConflictReason() == SVNConflictReason.EDITED) {
                myWCContext.getDb().opMakeCopyTemp(localAbsPath, false);
            } else if (treeConflict.getConflictReason() == SVNConflictReason.DELETED || treeConflict.getConflictReason() == SVNConflictReason.REPLACED) {
                // skip
            } else {
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.ASSERTION_FAIL), SVNLogType.WC);
            }
        }
        SVNSkel workItem;
        if (!deletingTarget) {
            workItem = myWCContext.wqBuildBaseRemove(localAbsPath, -1, SVNWCDbKind.Unknown);
        } else {
            workItem = myWCContext.wqBuildBaseRemove(localAbsPath, myTargetRevision, baseKind);
            myIsTargetDeleted = true;
        }
        myWCContext.getDb().addWorkQueue(localAbsPath, workItem);
        myWCContext.wqRun(localAbsPath);
        
        if (treeConflict == null) {
            SVNEventAction action = SVNEventAction.UPDATE_DELETE;
            if (myCurrentDirectory.shadowed) {
                action = SVNEventAction.UPDATE_SHADOWED_DELETE;
            }
            doNotification(localAbsPath, info.kind == SVNWCDbKind.Dir ? SVNNodeKind.DIR : SVNNodeKind.FILE, action, null, previousURL);
        }
        
    }

    private boolean isNodeAlreadyConflicted(File localAbspath) throws SVNException {
        List<SVNConflictDescription> conflicts = myWCContext.getDb().readConflicts(localAbspath);
        for (SVNConflictDescription cd : conflicts) {
            if (cd.isTreeConflict()) {
                return true;
            } else if (cd.isPropertyConflict() || cd.isTextConflict()) {
                SVNWCContext.ConflictInfo info = myWCContext.getConflicted(localAbspath, true, true, true);
                return (info.textConflicted || info.propConflicted || info.treeConflicted);
            }
        }
        return false;
    }

    private SVNTreeConflictDescription checkTreeConflict(File localAbspath, SVNWCDbStatus workingStatus, SVNWCDbKind workingKind, boolean existsInRepos, SVNConflictAction action, SVNNodeKind theirNodeKind, File theirRelpath) throws SVNException {
        SVNConflictReason reason = null;
        boolean locally_replaced = false;
        switch (workingStatus) {
            case Added:
            case MovedHere:
            case Copied:
                if (existsInRepos) {
                    SVNWCDbStatus base_status = myWCContext.getDb().getBaseInfo(localAbspath, BaseInfoField.status).status;
                    if (base_status != SVNWCDbStatus.NotPresent) {
                        locally_replaced = true;
                    }
                }
                if (!locally_replaced) {
                    assert (action == SVNConflictAction.ADD);
                    reason = SVNConflictReason.ADDED;
                } else {
                    reason = SVNConflictReason.REPLACED;
                }
                break;

            case Deleted:
                reason = SVNConflictReason.DELETED;
                break;
            
            case Incomplete:
            case Normal:
                if (action == SVNConflictAction.EDIT) {
                    return null;
                }
                TreeLocalModsInfo info = hasLocalMods(localAbspath);
                if (info.modificationsFound) {
                    if (!info.nonDeleteModificationsFound) {
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
        if (reason == SVNConflictReason.EDITED || reason == SVNConflictReason.DELETED || reason == SVNConflictReason.REPLACED) {
            assert (action == SVNConflictAction.EDIT || action == SVNConflictAction.DELETE || action == SVNConflictAction.REPLACE);
        } else if (reason == SVNConflictReason.ADDED) {
            assert (action == SVNConflictAction.ADD);
        }
        return createTreeConflict(localAbspath, reason, action, theirNodeKind, theirRelpath);
    }
    
    private TreeLocalModsInfo hasLocalMods(File localAbspath) throws SVNException {
        final TreeLocalModsInfo modsInfo = new TreeLocalModsInfo();
        SVNStatusEditor17 statusEditor = new SVNStatusEditor17(myAnchorAbspath, myWCContext, myWCContext.getOptions(), false, false, SVNDepth.INFINITY, new ISvnObjectReceiver<SvnStatus>() {
            
            public void receive(SvnTarget target, SvnStatus status) throws SVNException {
                SVNStatusType nodeStatus = status.getNodeStatus();
                if (nodeStatus == SVNStatusType.STATUS_NORMAL 
                	|| nodeStatus == SVNStatusType.STATUS_INCOMPLETE
                    || nodeStatus == SVNStatusType.STATUS_IGNORED
                    || nodeStatus == SVNStatusType.STATUS_NONE
                    || nodeStatus == SVNStatusType.STATUS_UNVERSIONED
                    || nodeStatus == SVNStatusType.STATUS_EXTERNAL) {
                    return;
                }
                if (nodeStatus == SVNStatusType.STATUS_DELETED) {
                    modsInfo.modificationsFound = true;
                    return;
                }
                if (nodeStatus == SVNStatusType.STATUS_MISSING || nodeStatus == SVNStatusType.STATUS_OBSTRUCTED) {
                    if (status.getPropertiesStatus() != SVNStatusType.STATUS_MODIFIED) {
                        return;
                    }
                }
                modsInfo.modificationsFound = true;
                modsInfo.nonDeleteModificationsFound = true;
                throw new SVNCancelException();
            }
        });
        
        try {
            statusEditor.walkStatus(localAbspath, SVNDepth.INFINITY, false, false, false, null);
        } catch (SVNCancelException cancel) { 
        } 
        
        return modsInfo;
    }

    private static class TreeLocalModsInfo {

        public boolean modificationsFound;
        public boolean nonDeleteModificationsFound;
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
        SVNWCDbKind wc_kind;
        boolean conflicted;
        boolean versionedLocallyAndPresent;
        boolean error = false;
        try {
            WCDbInfo readInfo = myWCContext.getDb().readInfo(db.localAbsolutePath, InfoField.status, InfoField.kind, InfoField.conflicted, InfoField.reposRelPath);
            status = readInfo.status;
            wc_kind = readInfo.kind;
            conflicted = readInfo.conflicted;
            versionedLocallyAndPresent = isNodePresent(status);
            db.oldRelativePath = readInfo.reposRelPath;
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                throw e;
            }
            error = true;
            wc_kind = SVNWCDbKind.Unknown;
            status = SVNWCDbStatus.Normal;
            conflicted = false;
            versionedLocallyAndPresent = false;
        }
        if (!error) {
            if (wc_kind == SVNWCDbKind.Dir && status == SVNWCDbStatus.Normal) {
                myWCContext.getDb().addBaseNotPresentNode(db.localAbsolutePath, db.newRelativePath, myReposRootURL, myReposUuid, myTargetRevision, SVNWCDbKind.File, null, null);
                rememberSkippedTree(db.localAbsolutePath);
                db.skipThis = true;
                db.alreadyNotified = true;
                doNotification(db.localAbsolutePath, SVNNodeKind.DIR, SVNEventAction.UPDATE_SKIP_OBSTRUCTION, db.getURL(), db.getPreviousURL());
                return;
            } else if (status == SVNWCDbStatus.Normal && (wc_kind == SVNWCDbKind.File ||  wc_kind == SVNWCDbKind.Symlink)) {
                rememberSkippedTree(db.localAbsolutePath);
                db.skipThis = true;
                db.alreadyNotified = true;
                doNotification(db.localAbsolutePath, SVNNodeKind.FILE, SVNEventAction.UPDATE_SKIP_OBSTRUCTION, db.getURL(), db.getPreviousURL());
                return;
            } else if (wc_kind == SVNWCDbKind.Unknown) {
                versionedLocallyAndPresent = false;
            } else {
                versionedLocallyAndPresent = isNodePresent(status);
            }
        }
        SVNTreeConflictDescription treeConflict = null;
        if (conflicted) {
            if (pb.deletionConflicts != null) {
                treeConflict = pb.deletionConflicts.get(db.name);
            }
            if (treeConflict != null) {
                treeConflict.setConflictAction(SVNConflictAction.REPLACE);
                myWCContext.getDb().opSetTreeConflict(db.localAbsolutePath, treeConflict);
                treeConflict = null;
                db.shadowed = true;
                conflicted = false;
            } else {
                conflicted = isNodeAlreadyConflicted(db.localAbsolutePath);                
            }
        }        
        if (conflicted) {
            rememberSkippedTree(db.localAbsolutePath);
            db.skipThis = true;
            db.alreadyNotified = true;
            myWCContext.getDb().addBaseNotPresentNode(db.localAbsolutePath, db.newRelativePath, myReposRootURL, myReposUuid, myTargetRevision, SVNWCDbKind.Dir, null, null);
            doNotification(db.localAbsolutePath, SVNNodeKind.DIR, SVNEventAction.SKIP_CONFLICTED, db.getURL(), db.getPreviousURL());
            return;
        }
        if (db.shadowed) {
            
        } else if (versionedLocallyAndPresent) {
            SVNWCDbStatus addStatus = SVNWCDbStatus.Normal;
            if (status == SVNWCDbStatus.Added) {
                addStatus = myWCContext.getDb().scanAddition(db.localAbsolutePath, AdditionInfoField.status).status;
            }
            boolean localIsNonDir = wc_kind != SVNWCDbKind.Dir && status != SVNWCDbStatus.Deleted;  
            if (!myAddsAsModification || localIsNonDir || addStatus != SVNWCDbStatus.Added) {
                treeConflict = checkTreeConflict(db.localAbsolutePath, status, wc_kind, false, SVNConflictAction.ADD, SVNNodeKind.DIR, db.newRelativePath);
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
                treeConflict = createTreeConflict(db.localAbsolutePath, SVNConflictReason.UNVERSIONED, SVNConflictAction.ADD, SVNNodeKind.DIR, db.newRelativePath);
            }
        }
        myWCContext.getDb().opSetNewDirToIncompleteTemp(db.localAbsolutePath, db.newRelativePath, myReposRootURL, myReposUuid, myTargetRevision, db.ambientDepth);
        if (!db.shadowed) {
            SVNFileUtil.ensureDirectoryExists(db.localAbsolutePath);
        }
        if (!db.shadowed && status == SVNWCDbStatus.Added) {
            myWCContext.getDb().opRemoveWorkingTemp(db.localAbsolutePath);
        }
        if (db.shadowed && db.obstructionFound) {
            myWCContext.getDb().opDelete(db.localAbsolutePath, null);
        }
        if (treeConflict != null) {
            myWCContext.getDb().opSetTreeConflict(db.localAbsolutePath, treeConflict);
            db.alreadyNotified = true;
            doNotification(db.localAbsolutePath, SVNNodeKind.DIR, SVNEventAction.TREE_CONFLICT, db.getURL(), db.getPreviousURL());
        }
        
        if (myWCContext.getEventHandler() != null && !db.alreadyNotified && !db.addExisted) {
            SVNEventAction action;
            if (db.shadowed) {
                action = SVNEventAction.UPDATE_SHADOWED_ADD;
            } else if (db.obstructionFound) {
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
        db.oldRelativePath = readInfo.reposRelPath;
        
        boolean have_work = readInfo.haveWork;
        boolean conflicted = readInfo.conflicted;
        
        SVNTreeConflictDescription treeConflict = null;
        SVNWCDbStatus baseStatus;
        if (!have_work) {
            baseStatus = status;
        } else {
            WCDbBaseInfo baseInfo = myWCContext.getDb().getBaseInfo(db.localAbsolutePath, BaseInfoField.status, BaseInfoField.revision, 
                    BaseInfoField.changedRev, BaseInfoField.changedDate, BaseInfoField.changedAuthor, BaseInfoField.depth);
            baseStatus = baseInfo.status;
            db.oldRevision = baseInfo.revision;
            db.ambientDepth = baseInfo.depth;
            db.changedAuthor = baseInfo.changedAuthor;
            db.changedDate = baseInfo.changedDate;
            db.changedRevsion = baseInfo.changedRev;
        }
        db.wasIncomplete  = baseStatus == SVNWCDbStatus.Incomplete;
        if (conflicted) {
            conflicted = isNodeAlreadyConflicted(db.localAbsolutePath);
        }
        
        if (conflicted) {
            rememberSkippedTree(db.localAbsolutePath);
            db.skipThis = true;
            db.alreadyNotified = true;
            doNotification(db.localAbsolutePath, SVNNodeKind.UNKNOWN, SVNEventAction.SKIP_CONFLICTED, null, null);
            return;
        }
        if (!db.shadowed) {
            treeConflict = checkTreeConflict(db.localAbsolutePath, status, wcKind, true, SVNConflictAction.EDIT, SVNNodeKind.DIR, db.newRelativePath);
        }
        if (treeConflict != null) {
            db.editConflict = treeConflict;
            db.shadowed = true;
        }
        myWCContext.getDb().opStartDirectoryUpdateTemp(db.localAbsolutePath, db.newRelativePath, myTargetRevision);
    }

    public void changeDirProperty(String name, SVNPropertyValue value) throws SVNException {
        if (myCurrentDirectory.skipThis) {
            return;
        }
        // TODO use String pool for property names and some of the values.
        if (SVNProperty.isRegularProperty(name)) {
            if (myCurrentDirectory.regularPropChanges == null) {
                myCurrentDirectory.regularPropChanges = new SVNProperties();
            }
            myCurrentDirectory.regularPropChanges.put(name, value);
        } else if (SVNProperty.isEntryProperty(name)) {
            if (myCurrentDirectory.entryPropChanges == null) {
                myCurrentDirectory.entryPropChanges = new SVNProperties();
            }
            myCurrentDirectory.entryPropChanges.put(name, value);
        } else if (SVNProperty.isWorkingCopyProperty(name)) {
            if (myCurrentDirectory.davPropChanges == null) {
                myCurrentDirectory.davPropChanges = new SVNProperties();
            }
            myCurrentDirectory.davPropChanges.put(name, value);
        }
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
        SVNSkel allWorkItems = null;
        SVNProperties entryProps = db.entryPropChanges;
        SVNProperties davProps = db.davPropChanges;
        SVNPropertyValue newWCDavURL = davProps != null ? davProps.getSVNPropertyValue(SVNProperty.WC_URL) : null;
        if (newWCDavURL == null) {
            davProps = null;
        }

        SVNProperties regularProps = db.regularPropChanges;
        
        SVNProperties actualProps = null;
        SVNProperties baseProps = null;
        
        if ((!db.addingDir || db.addExisted) && !db.shadowed) {
            actualProps = myWCContext.getActualProps(db.localAbsolutePath);
        } else {
            actualProps = new SVNProperties();
        }
        
        if (db.addExisted) {
            baseProps = myWCContext.getPristineProps(db.localAbsolutePath);
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
            MergePropertiesInfo mergeProperiesInfo = myWCContext.mergeProperties2(null, db.localAbsolutePath, SVNWCDbKind.Dir, null, null, null, baseProps, actualProps, regularProps, true, false);
            newActualProps = mergeProperiesInfo.newActualProperties;
            newBaseProps = mergeProperiesInfo.newBaseProperties;
            propStatus[0] = mergeProperiesInfo.mergeOutcome;
            allWorkItems = myWCContext.wqMerge(allWorkItems, mergeProperiesInfo.workItems);            
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
            myWCContext.getDb().addBaseDirectory(db.localAbsolutePath, db.newRelativePath, myReposRootURL, myReposUuid, myTargetRevision, props, db.changedRevsion, db.changedDate, db.changedAuthor, null, db.ambientDepth, 
                    davProps != null && !davProps.isEmpty() ? davProps : null, null, !db.shadowed && newBaseProps != null, newActualProps, allWorkItems);
            
        }
        myWCContext.wqRun(db.localAbsolutePath);
        if (!db.alreadyNotified && myWCContext.getEventHandler() != null && db.edited) {
            SVNEventAction action = null;
            if (db.shadowed) {
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
        SVNTreeConflictDescription treeConflict = null;

        if (!myIsCleanCheckout) {
            kind = SVNFileType.getNodeKind(SVNFileType.getType(fb.localAbsolutePath));
            try {
                WCDbInfo readInfo = myWCContext.getDb().readInfo(fb.localAbsolutePath, InfoField.status, InfoField.kind, InfoField.conflicted, InfoField.reposRelPath);
                status = readInfo.status;
                wcKind = readInfo.kind;
                conflicted = readInfo.conflicted;
                fb.oldRelativePath = readInfo.reposRelPath;
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
        
        if (conflicted) {
            if (pb.deletionConflicts != null) {
                treeConflict = pb.deletionConflicts.get(fb.name);
            }
            if (treeConflict != null) {
                treeConflict.setConflictAction(SVNConflictAction.REPLACE);
                myWCContext.getDb().opSetTreeConflict(fb.localAbsolutePath, treeConflict);
                treeConflict = null;
                fb.shadowed = true;
                conflicted = false;
            } else {
                conflicted = isNodeAlreadyConflicted(fb.localAbsolutePath);
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
                treeConflict = checkTreeConflict(fb.localAbsolutePath, status, wcKind, false, SVNConflictAction.ADD, SVNNodeKind.FILE, fb.newRelativePath);
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
                treeConflict = createTreeConflict(fb.localAbsolutePath, SVNConflictReason.UNVERSIONED, SVNConflictAction.ADD, SVNNodeKind.FILE, fb.newRelativePath);
                
                assert (treeConflict != null);
            }        
        }        
        if (pb.parentBaton != null || myTargetBasename == null || "".equals(myTargetBasename) || !fb.localAbsolutePath.equals(myTargetAbspath)) {
            if (pb.notPresentFiles == null) {
                pb.notPresentFiles = new HashSet<String>();
            }
            pb.notPresentFiles.add(fb.name);
        }
        if (treeConflict != null) {
            myWCContext.getDb().opSetTreeConflict(fb.localAbsolutePath, treeConflict);
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
        SVNWCDbStatus status;
        SVNWCDbKind kind = SVNWCDbKind.Unknown;
        SVNTreeConflictDescription treeConflict = null;
        
        WCDbInfo readInfo = myWCContext.getDb().readInfo(fb.localAbsolutePath, InfoField.status, InfoField.kind,
                InfoField.revision, InfoField.changedRev, InfoField.changedDate, InfoField.changedRev,
                InfoField.checksum, InfoField.haveWork, InfoField.conflicted, InfoField.propsMod, InfoField.reposRelPath);
        status = readInfo.status;
        fb.changedAuthor = readInfo.changedAuthor;
        fb.changedDate = readInfo.changedDate;
        fb.changedRevison = readInfo.changedRev;
        fb.oldRevision = readInfo.revision;
        fb.originalChecksum = readInfo.checksum;
        fb.oldRelativePath = readInfo.reposRelPath;
        fb.localPropMods = readInfo.propsMod;
        
        conflicted = readInfo.conflicted;
        
        if (readInfo.haveWork) {
            WCDbBaseInfo baseInfo = myWCContext.getDb().getBaseInfo(fb.localAbsolutePath, BaseInfoField.revision, 
                    BaseInfoField.changedRev, BaseInfoField.changedAuthor, BaseInfoField.changedDate,
                    BaseInfoField.checksum);
            fb.changedAuthor = baseInfo.changedAuthor;
            fb.changedDate = baseInfo.changedDate;
            fb.changedRevison = baseInfo.changedRev;
            fb.oldRevision = baseInfo.revision;
            fb.originalChecksum = baseInfo.checksum;
        }
        if (conflicted) {
            conflicted = isNodeAlreadyConflicted(fb.localAbsolutePath);
        }
        if (conflicted ) {
            rememberSkippedTree(fb.localAbsolutePath);
            fb.skipThis  = true;
            fb.alreadyNotified = true;
            doNotification(fb.localAbsolutePath, SVNNodeKind.UNKNOWN, SVNEventAction.SKIP_CONFLICTED, fb.getURL(), fb.getPreviousURL());
            return;
        }
        fb.shadowed = pb.shadowed;
        if (!fb.shadowed) {
            treeConflict = checkTreeConflict(fb.localAbsolutePath, status, kind, true, SVNConflictAction.EDIT, SVNNodeKind.FILE, fb.newRelativePath);
        }
        if (treeConflict != null) {
            fb.editConflict = treeConflict;
            fb.shadowed = true;
        }
    }

    public void changeFileProperty(String path, String propertyName, SVNPropertyValue propertyValue) throws SVNException {
        FileBaton fb = myCurrentFile;
        if (fb.skipThis) {
            return;
        }
        if (SVNProperty.isRegularProperty(propertyName)) {
            if (myCurrentFile.regularPropChanges == null) {
                myCurrentFile.regularPropChanges = new SVNProperties();
            }
            myCurrentFile.regularPropChanges.put(propertyName, propertyValue);
        } else if (SVNProperty.isEntryProperty(propertyName)) {
            if (myCurrentFile.entryPropChanges == null) {
                myCurrentFile.entryPropChanges = new SVNProperties();
            }
            myCurrentFile.entryPropChanges.put(propertyName, propertyValue);
        } else if (SVNProperty.isWorkingCopyProperty(propertyName)) {
            if (myCurrentFile.davPropChanges == null) {
                myCurrentFile.davPropChanges = new SVNProperties();
            }
            myCurrentFile.davPropChanges.put(propertyName, propertyValue);
        }
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
                    myWCContext.getDb().opMakeCopyTemp(fb.localAbsolutePath, false);
                    SVNTreeConflictDescription tc = checkTreeConflict(fb.localAbsolutePath, SVNWCDbStatus.Added, SVNWCDbKind.File, true, SVNConflictAction.EDIT, SVNNodeKind.FILE, fb.newRelativePath);
                    assert tc != null;
                    myWCContext.getDb().opSetTreeConflict(fb.localAbsolutePath, tc);
                    fb.editConflict = tc;
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

        SVNProperties entryProps = fb.entryPropChanges;
        SVNProperties davProps = fb.davPropChanges;
        SVNPropertyValue newWCDavURL = davProps != null ? davProps.getSVNPropertyValue(SVNProperty.WC_URL) : null;
        if (newWCDavURL == null) {
            davProps = null;
        }
        SVNProperties regularProps = fb.regularPropChanges;

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
                assert (entryProps.getStringValue(name) == null);
                myWCContext.getDb().removeLock(fb.localAbsolutePath);
                lockState = SVNStatusType.LOCK_UNLOCKED;
                break;
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
            currentBaseProps = myWCContext.getPristineProps(fb.localAbsolutePath);
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
            info.newBaseProperties = newBaseProps;
            info = myWCContext.mergeProperties2(info, fb.localAbsolutePath, 
                    SVNWCDbKind.File, null, null, null, currentBaseProps, currentActualProps, regularProps, true, false);
            newActualProps = info.newActualProperties;
            newBaseProps = info.newBaseProperties;
            propState[0] = info.mergeOutcome;
            allWorkItems = myWCContext.wqMerge(allWorkItems, info.workItems);

            File installFrom = null;
            if (!fb.obstructionFound) {
                MergeFileInfo fileInfo = null;                
                try {
                    fileInfo = mergeFile(fb, currentActualProps, fb.changedDate);
                    contentState = fileInfo.contentState;
                    installFrom = fileInfo.installFrom;
                    installPristine = fileInfo.installPristine;
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
                    allWorkItems = myWCContext.wqMerge(allWorkItems, fileInfo.workItems);
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
                SVNSkel wi = myWCContext.wqBuildFileRemove(installFrom);
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
            info = myWCContext.mergeProperties2(info, fb.localAbsolutePath, SVNWCDbKind.File, null, null, null, currentBaseProps, fakeActualProperties, regularProps, true, false);
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

        if (davProps != null) {
            davProps.removeNullValues();
        }
        myWCContext.getDb().addBaseFile(fb.localAbsolutePath, fb.newRelativePath, myReposRootURL, myReposUuid, myTargetRevision, newBaseProps, fb.changedRevison, fb.changedDate, fb.changedAuthor, newChecksum, 
                davProps != null && !davProps.isEmpty() ? davProps : null, null, 
                !fb.shadowed && newBaseProps != null, newActualProps, keepRecordedInfo, fb.shadowed && fb.obstructionFound, allWorkItems);

        if (fb.addExisted && fb.addingFile) {
            myWCContext.getDb().opRemoveWorkingTemp(fb.localAbsolutePath);
        }
        if (fb.directoryBaton.notPresentFiles != null) {
            fb.directoryBaton.notPresentFiles.remove(fb.name);
        }
        
        if (myWCContext.getEventHandler() != null && !fb.alreadyNotified
                && (fb.edited || lockState == SVNStatusType.LOCK_UNLOCKED)) {
            SVNEventAction action = SVNEventAction.UPDATE_UPDATE;

            if (fb.edited) {
                if (fb.shadowed) {
                    action = fb.addingFile ? SVNEventAction.UPDATE_SHADOWED_ADD : SVNEventAction.UPDATE_SHADOWED_UPDATE;
                } else if (fb.obstructionFound || fb.addExisted) {
                    if (contentState != SVNStatusType.CONFLICTED) {
                        action = SVNEventAction.UPDATE_EXISTS;
                    }
                } else if (fb.addingFile) {
                    action = SVNEventAction.UPDATE_ADD;
                }
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
            myWCContext.getDb().opBumpRevisionPostUpdate(myTargetAbspath , myRequestedDepth, mySwitchRelpath, myReposRootURL, myReposUuid, myTargetRevision, mySkippedTrees);
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

    }
    
    private static class BumpDirectoryInfo {
        private int refCount;
        private BumpDirectoryInfo parent;
    }
    
    private class DirectoryBaton {
        private String name;
        private File localAbsolutePath;
        private File newRelativePath;
        private File oldRelativePath;
        
        private long oldRevision;
        DirectoryBaton parentBaton;
        
        private boolean skipThis;
        private boolean alreadyNotified;
        private boolean addingDir;
        private boolean shadowed;
        
        private long changedRevsion;
        private SVNDate changedDate;
        private String changedAuthor;
        
        private Map<String, SVNTreeConflictDescription> deletionConflicts;
        private Set<String> notPresentFiles;
        
        private boolean obstructionFound;
        private boolean addExisted;
        
        private SVNProperties entryPropChanges;
        private SVNProperties regularPropChanges;
        private SVNProperties davPropChanges;
        
        private boolean edited;
        private SVNTreeConflictDescription editConflict;
        
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
                myWCContext.getDb().opSetTreeConflict(localAbsolutePath, editConflict);
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
            if (oldRelativePath != null) {
                return SVNWCUtils.join(myReposRootURL, oldRelativePath);
            }
            return null;
        }
    }
    
    private class FileBaton {
        public boolean localPropMods;
        private String name;
        private File localAbsolutePath;
        private File newRelativePath;
        private File oldRelativePath;
        private long oldRevision;
        private DirectoryBaton directoryBaton;
        
        private boolean skipThis;
        private boolean alreadyNotified;
        private boolean addingFile;
        private boolean obstructionFound;
        private boolean addExisted;
        private boolean shadowed;
        
        private long changedRevison;
        private SVNDate changedDate;
        private String changedAuthor;
        
        private SvnChecksum originalChecksum;
        
        private SVNProperties entryPropChanges;
        private SVNProperties regularPropChanges;
        private SVNProperties davPropChanges;
        
        private BumpDirectoryInfo bumpInfo;
        
        private boolean edited;
        private SVNTreeConflictDescription editConflict;
        
        public void markEdited() throws SVNException {
            if (edited) {
                return;
            }
            
            if (directoryBaton != null) {
                directoryBaton.markEdited();
            }
            edited = true;
            if (editConflict != null) {
                myWCContext.getDb().opSetTreeConflict(localAbsolutePath, editConflict);
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
            if (oldRelativePath != null) {
                return SVNWCUtils.join(myReposRootURL, oldRelativePath);
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
            d.shadowed = parent.shadowed;            
        }
        d.ambientDepth = SVNDepth.UNKNOWN;
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
        f.shadowed = parent.shadowed;
        f.directoryBaton = parent;
        f.changedRevison = -1;
        f.bumpInfo.refCount++;
        
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

    private static class AccumulatedChangeInfo {

        public long changedRev;
        public SVNDate changedDate;
        public String changedAuthor;
    }

    private AccumulatedChangeInfo accumulateLastChange(File localAbspath, SVNProperties entryProps) throws SVNException {
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

        public SVNSkel workItems;
        public boolean installPristine;
        public File installFrom;
        public SVNStatusType contentState;
    }
    
    public static MergeInfo performFileMerge(SVNWCContext context, File localAbsPath, File wriAbsPath, SvnChecksum newChecksum, SvnChecksum originalChecksum,
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
        
        MergeInfo mergeInfo = context.merge(mergeLeft, null, newTextBaseTmpAbsPath, null, localAbsPath, wriAbsPath, 
                oldRevStr, newRevStr, mineStr, actualProperties, false, null, propChanges);
        if (deleteLeft) {
            SVNSkel workItem = context.wqBuildFileRemove(mergeLeft);
            context.wqMerge(mergeInfo.workItems, workItem);
        }
        return mergeInfo;
    }

    private MergeFileInfo mergeFile(FileBaton fb, SVNProperties actualProps, SVNDate lastChangedDate) throws SVNException {
        DirectoryBaton pb = fb.directoryBaton;
        boolean isLocallyModified;
        boolean magicPropsChanged= false;
        
        MergeFileInfo mergeFileInfo = new MergeFileInfo();
        mergeFileInfo.installPristine = false;
        mergeFileInfo.contentState = SVNStatusType.UNCHANGED;
        
        if (fb.addingFile && !fb.addExisted) {
            isLocallyModified = false;
        } else {
            isLocallyModified = myWCContext.isTextModified(fb.localAbsolutePath, false);
        }
        
        SVNProperties propChanges = new SVNProperties();
        if (fb.regularPropChanges != null) {
            propChanges.putAll(fb.regularPropChanges);
        } 
        if (fb.entryPropChanges != null) {
            propChanges.putAll(fb.entryPropChanges);
        }
        if (fb.davPropChanges != null) {
            propChanges.putAll(fb.davPropChanges);
        }
        
        if (!isLocallyModified && fb.newTextBaseSHA1Checksum != null) {
            mergeFileInfo.installPristine = true;
        } else if (fb.newTextBaseSHA1Checksum != null) {
            MergeInfo mergeInfo = performFileMerge(myWCContext, fb.localAbsolutePath, 
                    pb.localAbsolutePath, 
                    fb.newTextBaseSHA1Checksum, 
                    fb.addExisted ? null : fb.originalChecksum,
                    actualProps, 
                    myExtensionPatterns, 
                    fb.oldRevision, 
                    myTargetRevision, 
                    propChanges);
            mergeFileInfo.workItems = myWCContext.wqMerge(mergeFileInfo.workItems, mergeInfo.workItems);
            mergeFileInfo.contentState = mergeInfo.mergeOutcome;
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
        
        if (!mergeFileInfo.installPristine && !isLocallyModified) {
            SVNDate date = SVNDate.NULL;
            if (lastChangedDate != null) {
                date = lastChangedDate;
            }
            SVNSkel workItem = myWCContext.wqBuildRecordFileinfo(fb.localAbsolutePath, date);
            mergeFileInfo.workItems = myWCContext.wqMerge(mergeFileInfo.workItems, workItem);
        }
        
        if (mergeFileInfo.contentState == SVNStatusType.CONFLICTED) {
            
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
    
    
}
