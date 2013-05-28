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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNSkel;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNFileListUtil;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbLock;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbBaseInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbBaseInfo.BaseInfoField;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbInfo.InfoField;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbRepositoryInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbRepositoryInfo.RepositoryInfoField;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb.DirParsedInfo;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDbRoot;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.AdditionInfo;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbShared;
import org.tmatesoft.svn.core.io.ISVNReporter;
import org.tmatesoft.svn.core.io.ISVNReporterBaton;
import org.tmatesoft.svn.core.wc.SVNConflictChoice;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.util.ISVNDebugLog;

/**
 * @version 1.3
 * @author TMate Software Ltd.
 */
public class SVNReporter17 implements ISVNReporterBaton {

    private final File path;
    private SVNWCContext wcContext;
    private SVNDepth depth;
    private final boolean isRestoreFiles;
    private final boolean isUseDepthCompatibilityTrick;
    private final boolean isHonorDepthExclude;
    private boolean isUseCommitTimes;
    private int reportedFilesCount;
    private int totalFilesCount;

    public SVNReporter17(File path, SVNWCContext wcContext, boolean restoreFiles, boolean useDepthCompatibilityTrick, SVNDepth depth, boolean lockOnDemand, boolean isStatus,
            boolean isHonorDepthExclude, boolean isUseCommitTimes, ISVNDebugLog log) {
        this.path = path;
        this.wcContext = wcContext;
        this.isRestoreFiles = restoreFiles;
        this.isUseDepthCompatibilityTrick = useDepthCompatibilityTrick;
        this.depth = depth;
        this.isHonorDepthExclude = isHonorDepthExclude;
        this.isUseCommitTimes = isUseCommitTimes;
    }

    public int getReportedFilesCount() {
        return reportedFilesCount;
    }

    public int getTotalFilesCount() {
        return totalFilesCount;
    }

    public void report(ISVNReporter reporter) throws SVNException {
        assert (SVNWCDb.isAbsolute(path));
        
        SVNWCDbStatus status = null;
        SVNWCDbKind target_kind = null;
        long target_rev = 0;
        File repos_relpath = null;
        SVNURL repos_root = null;
        SVNDepth target_depth = SVNDepth.UNKNOWN;
        SVNWCDbLock target_lock = null;
        boolean start_empty;
        SVNErrorMessage err = null;
        
        try {

            final WCDbBaseInfo baseInfo = wcContext.getDb().getBaseInfo(path, 
                    BaseInfoField.status, BaseInfoField.kind, BaseInfoField.revision, 
                    BaseInfoField.reposRelPath, BaseInfoField.reposRootUrl,
                    BaseInfoField.depth, BaseInfoField.lock);

            status = baseInfo.status;
            target_kind = baseInfo.kind;
            target_rev = baseInfo.revision;
            repos_relpath = baseInfo.reposRelPath;
            repos_root = baseInfo.reposRootUrl;
            target_depth = baseInfo.depth;
            target_lock = baseInfo.lock;

        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                throw e;
            }
            err = e.getErrorMessage();
        }
        if (err != null || (status != SVNWCDbStatus.Normal && status != SVNWCDbStatus.Incomplete)) {
            if (depth == SVNDepth.UNKNOWN) {
                depth = SVNDepth.INFINITY;
            }
            reporter.setPath("", null, 0, depth, false);
            reporter.deletePath("");
            reporter.finishReport();
            return;
        }
        
        if (repos_relpath == null) {
            WCDbRepositoryInfo rInfo = wcContext.getDb().scanBaseRepository(path, RepositoryInfoField.relPath, RepositoryInfoField.rootUrl);
            repos_relpath = rInfo.relPath;
            repos_root = rInfo.rootUrl;
        }
        if (target_depth == SVNDepth.UNKNOWN) {
            target_depth = SVNDepth.INFINITY;
        }
        start_empty = status == SVNWCDbStatus.Incomplete;
        if (isUseDepthCompatibilityTrick 
                && target_depth.compareTo(SVNDepth.IMMEDIATES) <= 0 
                && depth.compareTo(target_depth) > 0) {
            start_empty = true;
        }
        SVNFileType diskType = SVNFileType.UNKNOWN;
        if (isRestoreFiles) {
            diskType = SVNFileType.getType(path);
        } 
            
        if (isRestoreFiles && diskType == SVNFileType.NONE) {
            // restore node
            WCDbInfo wInfo = null;
            SVNWCDbKind wrkKind;
            SVNWCDbStatus wrkStatus;
            try {
                wInfo = wcContext.getDb().readInfo(path, InfoField.status, InfoField.kind);
                wrkStatus = wInfo.status;
                wrkKind = wInfo.kind;
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                    wrkStatus = SVNWCDbStatus.NotPresent;
                    wrkKind = SVNWCDbKind.File;
                } else {
                    throw e;
                }
            }
            if (wrkStatus == SVNWCDbStatus.Added) {
                Structure<AdditionInfo> additionInfo = SvnWcDbShared.scanAddition((SVNWCDb) wcContext.getDb(), path);
                wrkStatus = additionInfo.get(AdditionInfo.status);
                additionInfo.release();
            }
            if (wrkStatus == SVNWCDbStatus.Normal
                    || wrkStatus == SVNWCDbStatus.Copied
                    || wrkStatus == SVNWCDbStatus.MovedHere
                    || (wrkKind == SVNWCDbKind.Dir && (wrkStatus == SVNWCDbStatus.Added || wrkStatus == SVNWCDbStatus.Incomplete))) {
                restoreNode(wcContext, path, wrkKind, target_rev, isUseCommitTimes);
            }
        }
        
        try {
            SVNDepth reportDepth = target_depth;
            if (isHonorDepthExclude && depth != SVNDepth.UNKNOWN && depth.compareTo(target_depth) < 0) {
                reportDepth = depth;
            }
            reporter.setPath("", null, target_rev, reportDepth, start_empty);
            if (target_kind == SVNWCDbKind.Dir) {
                if (depth != SVNDepth.EMPTY) {
                    DirParsedInfo rootInfo = ((SVNWCDb) wcContext.getDb()).obtainWcRoot(path);
                    reportRevisionsAndDepths(rootInfo.wcDbDir.getWCRoot(), path, rootInfo.localRelPath, 
                            SVNFileUtil.createFilePath(""), 
                            target_rev, repos_relpath, repos_root, reportDepth, reporter, isRestoreFiles, depth, start_empty);
                }
            } else if (target_kind == SVNWCDbKind.Symlink || target_kind == SVNWCDbKind.File) {
                String base = SVNFileUtil.getFileName(path);
                File parentPath = SVNFileUtil.getParentFile(path);
                WCDbBaseInfo parentInfo = wcContext.getDb().getBaseInfo(parentPath, BaseInfoField.reposRelPath);
                if (!repos_relpath.equals(SVNFileUtil.createFilePath(parentInfo.reposRelPath, base))) {
                    SVNURL url = SVNWCUtils.join(repos_root, repos_relpath);
                    reporter.linkPath(url, "", target_lock != null ? target_lock.token : null, target_rev, SVNDepth.INFINITY, false);
                } else if (target_lock != null) {
                    reporter.setPath("", target_lock.token, target_rev, SVNDepth.INFINITY, false);
                }
            } 
            reporter.finishReport();
        } catch (SVNException e) {
            // abort report
            try {
                reporter.abortReport();
            } catch (SVNException inner) {
                e.getErrorMessage().setChildErrorMessage(inner.getErrorMessage());
            }
            throw e;
        }
    }

    public static boolean restoreNode(SVNWCContext context, File local_abspath, SVNWCDbKind kind, long target_rev, boolean useCommitTimes) throws SVNException {
        boolean restored = false;

        /*
         * Currently we can only restore files, but we will be able to restore
         * directories after we move to a single database and pristine store.
         */
        if (kind == SVNWCDbKind.File || kind == SVNWCDbKind.Symlink) {
            /* ... recreate file from text-base, and ... */
            restoreFile(context, local_abspath, useCommitTimes, true);
            restored = true;
        } else if (kind == SVNWCDbKind.Dir) {
            /* Recreating a directory is just a mkdir */
            local_abspath.mkdirs();
            restored = true;
        }

        if (restored) {
            /* ... report the restoration to the caller. */
            if (context.getEventHandler() != null) {
                context.getEventHandler().handleEvent(SVNEventFactory.createSVNEvent(local_abspath, SVNNodeKind.FILE, null, target_rev, SVNEventAction.RESTORE, null, null, null), 0);
            }
        }
        return restored;
    }


    private void reportRevisionsAndDepths(SVNWCDbRoot root, File dirPath, File dirLocalRelPath, File reportRelPath, long dirRev, File dirReposRelPath, SVNURL dirReposRoot,  
            SVNDepth dirDepth, ISVNReporter reporter, boolean restoreFiles, SVNDepth depth, boolean reportEverything) throws SVNException {
        
        Map<String, WCDbBaseInfo> baseChildren = wcContext.getDb().getBaseChildrenMap(root, dirLocalRelPath, true);
        Set<String> dirEntries = null;
        if (restoreFiles) {
            dirEntries = new HashSet<String>();
            File[] list = SVNFileListUtil.listFiles(dirPath);
            if (list != null) {
                for (File file : list) {
                    dirEntries.add(SVNFileUtil.getFileName(file));
                }
            }
        }
        for (String child : baseChildren.keySet()) {
            boolean thisSwitched = false;
            wcContext.checkCancelled();
            
            String thisReportRelpath = SVNFileUtil.getFilePath(SVNFileUtil.createFilePath(reportRelPath, child));
            File thisAbsPath = SVNFileUtil.createFilePath(dirPath, child);
            
            WCDbBaseInfo ths = baseChildren.get(child); 
            if (ths.updateRoot) {
                continue;
            }
            if (ths.status == SVNWCDbStatus.Excluded) {
                if (isHonorDepthExclude) {
                    reporter.setPath(thisReportRelpath, null, dirRev, SVNDepth.EXCLUDE, false);
                } else {
                    if (!reportEverything) {
                        reporter.deletePath(thisReportRelpath);
                    }
                }
                continue;
            }
            if (ths.status == SVNWCDbStatus.ServerExcluded || ths.status == SVNWCDbStatus.NotPresent) {
                if (!reportEverything) {
                    reporter.deletePath(thisReportRelpath);
                }
                continue;
            }
            if (restoreFiles && !dirEntries.contains(child)) {
                WCDbInfo wInfo = null;
                SVNWCDbKind wrkKind;
                SVNWCDbStatus wrkStatus;
                try {
                    wInfo = wcContext.getDb().readInfo(thisAbsPath, InfoField.status, InfoField.kind);
                    wrkStatus = wInfo.status;
                    wrkKind = wInfo.kind;
                } catch (SVNException e) {
                    if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                        wrkStatus = SVNWCDbStatus.NotPresent;
                        wrkKind = SVNWCDbKind.File;
                    } else {
                        throw e;
                    }
                }
                if (wrkStatus == SVNWCDbStatus.Added) {
                    Structure<AdditionInfo> additionInfo = SvnWcDbShared.scanAddition((SVNWCDb) wcContext.getDb(), thisAbsPath);
                    wrkStatus = additionInfo.get(AdditionInfo.status);
                    additionInfo.release();
                }
                if (wrkStatus == SVNWCDbStatus.Normal
                        || wrkStatus == SVNWCDbStatus.Copied
                        || wrkStatus == SVNWCDbStatus.MovedHere
                        || (wrkKind == SVNWCDbKind.Dir && (wrkStatus == SVNWCDbStatus.Added || wrkStatus == SVNWCDbStatus.Incomplete))) {
                    if (SVNFileType.getType(thisAbsPath) == SVNFileType.NONE) {
                        restoreNode(wcContext, thisAbsPath, wrkKind, dirRev, isUseCommitTimes);
                    }
                }                
            }
            if (ths.reposRelPath == null) {
                ths.reposRelPath = SVNFileUtil.createFilePath(dirReposRelPath, child);
            } else {
                String childName = SVNWCUtils.getPathAsChild(dirReposRelPath, ths.reposRelPath);
                if (childName == null || !child.equals(childName)) {
                    thisSwitched = true;
                }
            }
            if (ths.depth == SVNDepth.UNKNOWN) {
                ths.depth = SVNDepth.INFINITY;
            }
            if (ths.kind == SVNWCDbKind.File || ths.kind == SVNWCDbKind.Symlink) {
                 if (reportEverything) {
                     if (thisSwitched) {
                         SVNURL url = SVNWCUtils.join(dirReposRoot, ths.reposRelPath);
                         reporter.linkPath(url, thisReportRelpath, 
                                 ths.lock != null ? ths.lock.token : null, 
                                         ths.revision, ths.depth, false);
                     } else {
                         reporter.setPath(thisReportRelpath, ths.lock != null ? ths.lock.token : null, ths.revision, ths.depth, false);
                     }
                 } else if (thisSwitched) {
                     SVNURL url = SVNWCUtils.join(dirReposRoot, ths.reposRelPath);
                     reporter.linkPath(url, thisReportRelpath, 
                             ths.lock != null ? ths.lock.token : null, 
                                     ths.revision, ths.depth, false);
                 } else if (ths.revision != dirRev || ths.lock != null || dirDepth == SVNDepth.EMPTY) {
                     reporter.setPath(thisReportRelpath, ths.lock != null ? ths.lock.token : null, ths.revision, ths.depth, false);
                 }
            } else if (ths.kind == SVNWCDbKind.Dir && (depth == SVNDepth.UNKNOWN || depth.compareTo(SVNDepth.FILES) > 0)) {
                boolean isIncomplete = ths.status == SVNWCDbStatus.Incomplete;
                boolean startEmpty = isIncomplete;
                SVNDepth reportDepth = ths.depth;

                if (!depth.isRecursive()) {
                    reportDepth = SVNDepth.EMPTY;
                }

                if (isIncomplete && ths.revision < 0) {
                    ths.revision = dirRev;
                }
                if (isUseDepthCompatibilityTrick 
                        && ths.depth.compareTo(SVNDepth.FILES) <= 0
                        && depth.compareTo(ths.depth) > 0) {
                    startEmpty = true;
                }
                if (reportEverything) {
                    if (thisSwitched) {
                        SVNURL url = SVNWCUtils.join(dirReposRoot, ths.reposRelPath);
                        reporter.linkPath(url, thisReportRelpath, 
                                ths.lock != null ? ths.lock.token : null, 
                                        ths.revision, reportDepth, startEmpty);
                    } else {
                        reporter.setPath(thisReportRelpath, ths.lock != null ? ths.lock.token : null, ths.revision, reportDepth, startEmpty);
                    }
                } else if (thisSwitched) {
                    SVNURL url = SVNWCUtils.join(dirReposRoot, ths.reposRelPath);
                    reporter.linkPath(url, thisReportRelpath, 
                            ths.lock != null ? ths.lock.token : null, 
                                    ths.revision, reportDepth, startEmpty);
                } else if (ths.revision != dirRev
                        || isIncomplete
                        || ths.lock != null 
                        || dirDepth == SVNDepth.EMPTY
                        || dirDepth == SVNDepth.FILES
                        || (dirDepth == SVNDepth.IMMEDIATES && ths.depth != SVNDepth.EMPTY)
                        || (ths.depth.compareTo(SVNDepth.INFINITY) < 0 && depth.isRecursive())) {
                    reporter.setPath(thisReportRelpath, ths.lock != null ? ths.lock.token : null, ths.revision, reportDepth, startEmpty);
                }
                if (depth.isRecursive()) {
                    File reposRelPath = ths.reposRelPath;
                    if (reposRelPath == null) {
                        reposRelPath = SVNFileUtil.createFilePath(dirReposRelPath, child);
                    }
                    reportRevisionsAndDepths(root, thisAbsPath, SVNFileUtil.createFilePath(dirLocalRelPath, child), 
                            SVNFileUtil.createFilePath(thisReportRelpath), 
                            ths.revision, 
                            reposRelPath, 
                            dirReposRoot, ths.depth, reporter, restoreFiles, depth, startEmpty);
                }
            }
        }
    }

    private static void restoreFile(SVNWCContext context, File localAbsPath, boolean useCommitTimes, boolean removeTextConflicts) throws SVNException {
        SVNSkel workItem = context.wqBuildFileInstall(localAbsPath, null, useCommitTimes, true);
        context.getDb().addWorkQueue(localAbsPath, workItem);
        if (removeTextConflicts) {
            resolveTextConflict(context, localAbsPath);
        }
    }

    private static void resolveTextConflict(SVNWCContext context, File localAbsPath) throws SVNException {
        context.resolveConflictOnNode(localAbsPath, true, false, SVNConflictChoice.MERGED);
    }

}
