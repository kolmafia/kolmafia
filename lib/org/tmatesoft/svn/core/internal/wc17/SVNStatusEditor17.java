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

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb.Mode;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.wc.SVNFileListUtil;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.SVNWCSchedule;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.ScheduleInternalInfo;
import org.tmatesoft.svn.core.internal.wc17.db.*;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.*;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbAdditionInfo.AdditionInfoField;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbBaseInfo.BaseInfoField;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbInfo.InfoField;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbRepositoryInfo.RepositoryInfoField;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb.DirParsedInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.ExternalNodeInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeOriginInfo;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgPropertiesManager;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver;
import org.tmatesoft.svn.core.wc2.SvnStatus;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.core.wc2.hooks.ISvnFileListHook;

import java.io.File;
import java.util.*;

/**
 * @version 1.3
 * @author TMate Software Ltd.
 */
public class SVNStatusEditor17 {

    protected SVNWCContext myWCContext;
    protected File myPath;

    protected boolean myIsReportAll;
    protected boolean myIsNoIgnore;
    protected SVNDepth myDepth;

    protected ISvnObjectReceiver<SvnStatus> myStatusHandler;

    protected Map<File, File> myExternalsMap;
    protected Collection<String> myGlobalIgnores;

    protected SVNURL myRepositoryRoot;
    protected Map<String, SVNLock> myRepositoryLocks;
    protected long myTargetRevision;
    protected String myWCRootPath;
    protected ISvnFileListHook myFileListHook;
    protected ISvnFileListHook myDefaultFileListHook;
    protected boolean myIsGetExcluded;

    private boolean myIgnoreTextMods;

    public SVNStatusEditor17(File path, SVNWCContext wcContext, ISVNOptions options, boolean noIgnore, boolean reportAll, SVNDepth depth, ISvnObjectReceiver<SvnStatus> handler) {

        myWCContext = wcContext;
        myPath = path;
        myIsNoIgnore = noIgnore;
        myIsReportAll = reportAll;
        myDepth = depth;
        myStatusHandler = handler;
        myExternalsMap = new HashMap<File, File>();
        myGlobalIgnores = getGlobalIgnores(options);
        myTargetRevision = -1;
        myDefaultFileListHook = new DefaultSvnFileListHook();
        myFileListHook = myDefaultFileListHook;

        myIsGetExcluded = false;

    }
    
    protected void collectExternals(File path) throws SVNException {
        myExternalsMap = myWCContext.getDb().getExternalsDefinedBelow(path);
    }

    public SVNCommitInfo closeEdit() throws SVNException {

        final SVNNodeKind localKind = SVNFileType.getNodeKind(SVNFileType.getType(myPath));
        final SVNNodeKind kind = myWCContext.readKind(myPath, false);

        File anchor_abspath;
        String target_name;
        boolean skip_root;

        if (kind == SVNNodeKind.FILE && localKind == SVNNodeKind.FILE) {
            anchor_abspath = SVNFileUtil.getFileDir(myPath);
            target_name = SVNFileUtil.getFileName(myPath);
            skip_root = true;
        } else if (kind == SVNNodeKind.DIR && localKind == SVNNodeKind.DIR) {
            anchor_abspath = myPath;
            target_name = null;
            skip_root = false;
        } else {
            anchor_abspath = SVNFileUtil.getFileDir(myPath);
            target_name = SVNFileUtil.getFileName(myPath);
            skip_root = false;
        }

        SVNFileType fileType = SVNFileType.getType(anchor_abspath);
        getDirStatus(anchor_abspath, target_name, skip_root, null, null, fileType, myGlobalIgnores, myDepth, myIsReportAll, true, getDefaultHandler());

        return null;
    }

    public long getTargetRevision() {
        return myTargetRevision;
    }

    public void targetRevision(long revision) {
        myTargetRevision = revision;
    }

    public void setFileListHook(ISvnFileListHook filesListHook) {
        if (filesListHook != null) { 
            myFileListHook = filesListHook;
        }
    }

    public SVNDepth getDepth() {
        return myDepth;
    }

    protected ISvnObjectReceiver<SvnStatus> getDefaultHandler() {
        return myStatusHandler;
    }

    protected boolean isReportAll() {
        return myIsReportAll;
    }

    protected boolean isNoIgnore() {
        return myIsNoIgnore;
    }

    private static Collection<String> getGlobalIgnores(ISVNOptions options) {
        if (options != null) {
            String[] ignores = options.getIgnorePatterns();
            if (ignores != null) {
                Collection<String> patterns = new HashSet<String>();
                for (int i = 0; i < ignores.length; i++) {
                    patterns.add(ignores[i]);
                }
                return patterns;
            }
        }
        return Collections.emptySet();
    }

    private static class DefaultSvnFileListHook implements ISvnFileListHook {

        public Map<String, File> listFiles(File parent) {
            File[] children = SVNFileListUtil.listFiles(parent);
            if (children != null) {
                @SuppressWarnings("unchecked")
                Map<String, File> map = new SVNHashMap();
                for (int i = 0; i < children.length; i++) {
                    map.put(SVNFileUtil.getFileName(children[i]), children[i]);
                }
                return map;
            }
            return Collections.emptyMap();
        }
    }

    private void sendStatusStructure(File localAbsPath, WCDbRepositoryInfo parentReposInfo, SVNWCDbInfo info, SVNNodeKind pathKind, boolean pathSpecial, boolean getAll, ISvnObjectReceiver<SvnStatus> handler) throws SVNException {
        SVNLock repositoryLock = null;
        if (myRepositoryLocks != null) {
            WCDbRepositoryInfo reposInfo = getRepositoryRootUrlRelPath(myWCContext, parentReposInfo, info, localAbsPath);
            if (reposInfo != null && reposInfo.relPath != null) {
                repositoryLock = (SVNLock) myRepositoryLocks.get("/" + SVNFileUtil.getFilePath(reposInfo.relPath));
            }
        }
        SvnStatus status = assembleStatus(myWCContext, localAbsPath, parentReposInfo, info, pathKind, pathSpecial, getAll, myIgnoreTextMods, repositoryLock);
        if (status != null && handler != null) {
            handler.receive(SvnTarget.fromFile(localAbsPath), status);
        }

    }

    private void sendUnversionedItem(File nodeAbsPath, SVNNodeKind pathKind, boolean treeConflicted, Collection<String> patterns, boolean noIgnore, ISvnObjectReceiver<SvnStatus> handler) throws SVNException {
        boolean isIgnored = SvnNgPropertiesManager.isIgnored(SVNFileUtil.getFileName(nodeAbsPath), patterns);
        boolean isExternal = isExternal(nodeAbsPath);
        SvnStatus status = assembleUnversioned17(nodeAbsPath, pathKind, treeConflicted, isIgnored);
        if (status != null) {
            if (isExternal) {
                status.setNodeStatus(SVNStatusType.STATUS_EXTERNAL);
            }
            if (status.isConflicted()) {
                isIgnored = false;
            }
            if (handler != null && (noIgnore || !isIgnored || isExternal)) {
                handler.receive(SvnTarget.fromFile(nodeAbsPath), status);
            }
        }

    }

    public static SvnStatus assembleUnversioned17(File localAbspath, SVNNodeKind pathKind, boolean treeConflicted, boolean isIgnored) throws SVNException {

        SvnStatus stat = new SvnStatus();
        stat.setPath(localAbspath);
        stat.setKind(SVNNodeKind.UNKNOWN); 
        stat.setDepth(SVNDepth.UNKNOWN);
        stat.setNodeStatus(SVNStatusType.STATUS_NONE);
        stat.setTextStatus(SVNStatusType.STATUS_NONE);
        stat.setPropertiesStatus(SVNStatusType.STATUS_NONE);
        stat.setRepositoryNodeStatus(SVNStatusType.STATUS_NONE);
        stat.setRepositoryTextStatus(SVNStatusType.STATUS_NONE);
        stat.setRepositoryPropertiesStatus(SVNStatusType.STATUS_NONE);

        if (pathKind != SVNNodeKind.NONE) {
            if (isIgnored) {
                stat.setNodeStatus(SVNStatusType.STATUS_IGNORED);
            } else {
                stat.setNodeStatus(SVNStatusType.STATUS_UNVERSIONED);
            }
        } else if (treeConflicted) {
            stat.setNodeStatus(SVNStatusType.STATUS_CONFLICTED);
        }

        stat.setRevision(SVNWCContext.INVALID_REVNUM);
        stat.setChangedRevision(SVNWCContext.INVALID_REVNUM);
        stat.setRepositoryChangedRevision(SVNWCContext.INVALID_REVNUM);
        stat.setRepositoryKind(SVNNodeKind.NONE);

        stat.setConflicted(treeConflicted);
        stat.setChangelist(null);
        return stat;
    }


    public static SvnStatus assembleStatus(SVNWCContext context, File localAbsPath, WCDbRepositoryInfo parentReposInfo, SVNWCDbInfo info, SVNNodeKind pathKind, boolean pathSpecial, boolean getAll, boolean ignoreTextMods, SVNLock repositoryLock) throws SVNException {

        boolean switched_p, copied = false;

        SVNStatusType node_status = SVNStatusType.STATUS_NORMAL;
        SVNStatusType text_status = SVNStatusType.STATUS_NORMAL;
        SVNStatusType prop_status = SVNStatusType.STATUS_NONE;
        
        if (info == null) {
            info = readInfo(context, localAbsPath);
        }
        if (info.reposRelpath == null || parentReposInfo == null || parentReposInfo.relPath == null) {
            switched_p = false;
        } else {
            String name = SVNFileUtil.getFilePath(SVNWCUtils.skipAncestor(parentReposInfo.relPath, info.reposRelpath));
            switched_p = name == null || !name.equals(SVNFileUtil.getFileName(localAbsPath)); 
        }

        if (info.kind == SVNWCDbKind.Dir) {
            if (info.status == SVNWCDbStatus.Incomplete || info.incomplete) {
                node_status = SVNStatusType.STATUS_INCOMPLETE;
            } else if (info.status == SVNWCDbStatus.Deleted) {
                node_status = SVNStatusType.STATUS_DELETED;
                if (!info.haveBase) {
                    copied = true;
                } else {
                    copied = context.getNodeScheduleInternal(localAbsPath, false, true).copied;
                }
            } else if (pathKind == null || pathKind != SVNNodeKind.DIR) {
                if (pathKind == null || pathKind == SVNNodeKind.NONE) {
                    node_status = SVNStatusType.STATUS_MISSING;
                } else {
                    node_status = SVNStatusType.STATUS_OBSTRUCTED;
                }
            }
        } else {
            if (info.status == SVNWCDbStatus.Deleted) {
                node_status = SVNStatusType.STATUS_DELETED;
                copied = context.getNodeScheduleInternal(localAbsPath, false, true).copied;
            } else if (pathKind == null || pathKind != SVNNodeKind.FILE) {
                if (pathKind == null || pathKind == SVNNodeKind.NONE) {
                    node_status = SVNStatusType.STATUS_MISSING;
                } else {
                    node_status = SVNStatusType.STATUS_OBSTRUCTED;
                }
            }
        }
        
        if (info.status != SVNWCDbStatus.Deleted) {
            if (info.propsMod) {
                prop_status = SVNStatusType.STATUS_MODIFIED;
            } else if (info.hadProps) {
                prop_status = SVNStatusType.STATUS_NORMAL;
            }
        }
        
        if (info.kind != SVNWCDbKind.Dir && node_status == SVNStatusType.STATUS_NORMAL) {
            boolean text_modified_p = false;
            long fileSize = SVNFileUtil.getFileLength(localAbsPath);
            long fileTime = SVNFileUtil.getFileLastModified(localAbsPath);
            
            if ((info.kind == SVNWCDbKind.File || info.kind == SVNWCDbKind.Symlink) && (!SVNFileUtil.symlinksSupported() || info.special == pathSpecial)) {
                if (!info.hasChecksum) {
                    text_modified_p = true;
                } else if (ignoreTextMods ||
                    (pathKind != null && 
                            info.recordedSize != -1 &&
                            info.recordedModTime != 0 &&
                            (info.recordedModTime / 1000) == fileTime &&
                            info.recordedSize == fileSize)) {
                    text_modified_p = false;
                } else {
                    try {
                        text_modified_p = context.isTextModified(localAbsPath, false);
                    } catch (SVNException e) {
                        if (!SVNWCContext.isErrorAccess(e)) {
                            throw e;
                        }
                        text_modified_p = true;
                    }
                }                
            } else if (SVNFileUtil.symlinksSupported() && (info.special != (pathKind != null && pathSpecial))) {
                node_status = SVNStatusType.STATUS_OBSTRUCTED;
            }
            if (text_modified_p) {
                text_status = SVNStatusType.STATUS_MODIFIED;
            }
        }
        boolean conflicted = info.conflicted;
        if (conflicted) {
            SVNWCContext.ConflictInfo conflictInfo = context.getConflicted(localAbsPath, true, true, true);
            if (!conflictInfo.propConflicted && !conflictInfo.textConflicted && !conflictInfo.treeConflicted) {
                conflicted =false;
            }
        }
        if (node_status == SVNStatusType.STATUS_NORMAL) {
            if (info.status == SVNWCDbStatus.Added) {
                if (!info.opRoot) {
                    copied = true;
                } else if (info.kind == SVNWCDbKind.File && !info.haveBase && !info.haveMoreWork) {
                    node_status = SVNStatusType.STATUS_ADDED;
                    copied = info.hasChecksum;
                } else {
                    ScheduleInternalInfo scheduleInfo = context.getNodeScheduleInternal(localAbsPath, true, true);
                    copied = scheduleInfo.copied;
                    if (scheduleInfo.schedule == SVNWCSchedule.add) {
                        node_status = SVNStatusType.STATUS_ADDED;
                    } else if (scheduleInfo.schedule == SVNWCSchedule.replace) {
                        node_status = SVNStatusType.STATUS_REPLACED;
                    }
                }
            }
        }
        
        if (node_status == SVNStatusType.STATUS_NORMAL) {
            node_status = text_status;
        }

        if (node_status == SVNStatusType.STATUS_NORMAL && prop_status != SVNStatusType.STATUS_NONE) {
            node_status = prop_status;
        }
        if (!getAll) {
            if ((node_status == SVNStatusType.STATUS_NONE || node_status == SVNStatusType.STATUS_NORMAL) 
                    && !switched_p
                    && !info.locked
                    && (info.lock == null)
                    && repositoryLock == null
                    && info.changelist == null
                    && !conflicted) {
                return null;
            }
        }
        SVNURL copyFromUrl = null;
        long copyFromRevision = -1;

        if (copied) {
            Structure<NodeOriginInfo> origin = context.getNodeOrigin(localAbsPath, false, NodeOriginInfo.reposRootUrl, NodeOriginInfo.reposRelpath, NodeOriginInfo.revision);
            copyFromUrl = origin.get(NodeOriginInfo.reposRootUrl);
            if (copyFromUrl != null) {
                copyFromUrl = SVNWCUtils.join(copyFromUrl, origin.<File>get(NodeOriginInfo.reposRelpath));
                copyFromRevision = origin.lng(NodeOriginInfo.revision);
            }
            origin.release();
        }
        
        WCDbRepositoryInfo reposInfo = getRepositoryRootUrlRelPath(context, parentReposInfo, info, localAbsPath);
        SVNNodeKind statusKind = null;
        switch (info.kind) {
        case Dir:
            statusKind = SVNNodeKind.DIR;
            break;
        case File:
        case Symlink:
            statusKind = SVNNodeKind.FILE;
            break;
        case Unknown:
        default:
            statusKind = SVNNodeKind.UNKNOWN;
        }
        SvnStatus stat = new SvnStatus();
        stat.setKind(statusKind);
        stat.setPath(localAbsPath);

        if (info.lock != null) {
            stat.setLock(new SVNLock(SVNFileUtil.getFilePath(reposInfo.relPath), info.lock.token, info.lock.owner, info.lock.comment, info.lock.date, null));
        }

        stat.setCopyFromUrl(copyFromUrl);
        stat.setCopyFromRevision(copyFromRevision);

        stat.setDepth(info.depth);
        stat.setNodeStatus(node_status);
        stat.setTextStatus(text_status);
        stat.setPropertiesStatus(prop_status);
        stat.setRepositoryNodeStatus(SVNStatusType.STATUS_NONE); 
        stat.setRepositoryTextStatus(SVNStatusType.STATUS_NONE);
        stat.setRepositoryPropertiesStatus(SVNStatusType.STATUS_NONE);
        stat.setSwitched(switched_p);
        stat.setCopied(copied);
        stat.setRepositoryLock(repositoryLock);
        stat.setRevision(info.revnum);
        stat.setChangedRevision(info.changedRev);
        stat.setChangedAuthor(info.changedAuthor);
        stat.setChangedDate(info.changedDate);

        stat.setRepositoryKind(SVNNodeKind.NONE);
        stat.setRepositoryChangedRevision(SVNWCContext.INVALID_REVNUM);
        stat.setRepositoryChangedDate(null);
        stat.setRepositoryChangedAuthor(null);

        stat.setWcLocked(info.locked);
        stat.setConflicted(info.conflicted);
        stat.setVersioned(true);
        stat.setChangelist(info.changelist);
        stat.setRepositoryRootUrl(reposInfo.rootUrl);
        stat.setRepositoryRelativePath(SVNFileUtil.getFilePath(reposInfo.relPath));
        stat.setRepositoryUuid(reposInfo.uuid);

        if (stat.isVersioned() && stat.isConflicted()) {
            SVNWCContext.ConflictInfo conflictInfo = context.getConflicted(stat.getPath(), true, true, true);
            if (conflictInfo.textConflicted) {
                stat.setTextStatus(SVNStatusType.STATUS_CONFLICTED);
            }
            if (conflictInfo.propConflicted) {
                stat.setPropertiesStatus(SVNStatusType.STATUS_CONFLICTED);
            }
            if (conflictInfo.textConflicted || conflictInfo.propConflicted) {
                stat.setNodeStatus(SVNStatusType.STATUS_CONFLICTED);
            }
        }

        if (stat.isSwitched() && stat.isVersioned() && stat.getKind() == SVNNodeKind.FILE) {
            try {
                Structure<ExternalNodeInfo> externalInfo = SvnWcDbExternals.readExternal(context, stat.getPath(), stat.getPath(), ExternalNodeInfo.kind);
                if (externalInfo != null) {
                    stat.setFileExternal(externalInfo.<SVNWCDbKind>get(ExternalNodeInfo.kind) == SVNWCDbKind.File);
                    stat.setSwitched(false);
                    stat.setNodeStatus(stat.getTextStatus());
                    
                    externalInfo.release();
                }
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                    throw e;
                }
            }
            
        }
        
        return stat;
    }

    private boolean isExternal(File nodeAbsPath) {
        if (!myExternalsMap.containsKey(nodeAbsPath)) {
            for (Iterator<File> paths = myExternalsMap.keySet().iterator(); paths.hasNext();) {
                File externalPath = (File) paths.next();
                if (SVNWCUtils.isChild(nodeAbsPath, externalPath)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private Collection<String> collectIgnorePatterns(SVNWCDbRoot root, File localRelPath, Collection<String> ignores) throws SVNException {
        SVNProperties props = SvnWcDbProperties.readProperties(root, localRelPath);
        final String localIgnores = props != null ? props.getStringValue(SVNProperty.IGNORE) : null;
        if (localIgnores != null) {
            final List<String> patterns = new ArrayList<String>();
            patterns.addAll(ignores);
            for (StringTokenizer tokens = new StringTokenizer(localIgnores, "\r\n"); tokens.hasMoreTokens();) {
                String token = tokens.nextToken().trim();
                if (token.length() > 0) {
                    patterns.add(token);
                }
            }
            return patterns;
        }
        return ignores;
    }

    public void setRepositoryInfo(SVNURL repositoryRoot, HashMap<String, SVNLock> repositoryLocks) {
        myRepositoryRoot = repositoryRoot;
        myRepositoryLocks = repositoryLocks;
    }
    
    private static SVNWCDbInfo readInfo(SVNWCContext context, File localAbsPath) throws SVNException {
        SVNWCDbInfo result = new SVNWCDbInfo();
        
        WCDbInfo readInfo = context.getDb().readInfo(localAbsPath, InfoField.status, InfoField.kind, InfoField.revision, InfoField.reposRelPath, InfoField.reposRootUrl, InfoField.reposUuid,
                InfoField.changedRev, InfoField.changedDate, InfoField.changedAuthor, InfoField.depth, InfoField.checksum, InfoField.lock, InfoField.translatedSize,
                InfoField.lastModTime, InfoField.changelist, InfoField.conflicted, InfoField.opRoot, InfoField.hadProps, InfoField.propsMod, InfoField.haveBase, InfoField.haveMoreWork);
        result.load(readInfo);
        
        result.locked = context.getDb().isWCLocked(localAbsPath);
        if (result.haveBase && (result.status == SVNWCDbStatus.Added || result.status == SVNWCDbStatus.Deleted)) {
            result.lock = context.getDb().getBaseInfo(localAbsPath, BaseInfoField.lock).lock;
        }
        result.hasChecksum = readInfo.checksum != null;
        if (result.kind == SVNWCDbKind.File && (result.hadProps || result.propsMod)) {
            SVNProperties properties;
            if (result.propsMod) {
                properties = context.getDb().readProperties(localAbsPath);
            } else {
                properties = context.getDb().readPristineProperties(localAbsPath);
            }
            result.special = properties.getSVNPropertyValue(SVNProperty.SPECIAL) != null;
        }
        return result;
    }
    
    public void walkStatus(File localAbsPath, SVNDepth depth, boolean getAll, boolean noIgnore, boolean ignoreTextMods, Collection<String> ignorePatterns) throws SVNException {
        collectExternals(localAbsPath);
        if (ignorePatterns == null) {
            ignorePatterns = getGlobalIgnores(myWCContext.getOptions());
        }
        SVNWCDbInfo dirInfo = null;
        try {
            dirInfo = readInfo(myWCContext, localAbsPath);
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                throw e;
            }
        }
        SVNFileType fileType = SVNFileType.getType(localAbsPath);
        File anchorAbsPath;
        String targetName;
        boolean skipRoot;
        if (dirInfo != null 
                && dirInfo.kind == SVNWCDbKind.Dir
                && dirInfo.status != SVNWCDbStatus.Excluded
                && dirInfo.status != SVNWCDbStatus.NotPresent
                && dirInfo.status != SVNWCDbStatus.ServerExcluded) {
            
            anchorAbsPath = localAbsPath;
            targetName = null;
            skipRoot = false;
        } else {
            dirInfo = null;
            anchorAbsPath = SVNFileUtil.getParentFile(localAbsPath);
            targetName = SVNFileUtil.getFileName(localAbsPath);
            skipRoot = true;
        }
        
        myIgnoreTextMods = ignoreTextMods;
        
        getDirStatus(anchorAbsPath, targetName, skipRoot, null, dirInfo, fileType, ignorePatterns, depth, getAll, noIgnore, getDefaultHandler());        
    }
    
    private SVNWCDbRoot wcRoot;
   
    protected void getDirStatus(File localAbsPath, String selected, boolean skipThisDir, WCDbRepositoryInfo parentReposInfo, 
            SVNWCDbInfo dirInfo, SVNFileType fileType, Collection<String> ignorePatterns, SVNDepth depth, boolean getAll, boolean noIgnore, ISvnObjectReceiver<SvnStatus> handler) throws SVNException {
        myWCContext.checkCancelled();
        
        if (depth == SVNDepth.UNKNOWN) {
            depth = SVNDepth.INFINITY;
        }
        Map<String, File> childrenFiles = myFileListHook.listFiles(localAbsPath);
        if (childrenFiles == null) {
            childrenFiles = Collections.emptyMap();
        }
        final Set<String> allChildren = new HashSet<String>();
        final Set<String> conflicts = new HashSet<String>();
        final Map<String, SVNWCDbInfo> nodes = new HashMap<String, ISVNWCDb.SVNWCDbInfo>();
        Collection<String> patterns = null;
        
        if (dirInfo == null) {
            dirInfo = readInfo(myWCContext, localAbsPath);
        }
        
        WCDbRepositoryInfo dirReposInfo = getRepositoryRootUrlRelPath(myWCContext, parentReposInfo, dirInfo, localAbsPath);
        if (wcRoot == null) {
            DirParsedInfo pdh = ((SVNWCDb) myWCContext.getDb()).parseDir(localAbsPath, Mode.ReadOnly);
            wcRoot = pdh.wcDbDir.getWCRoot();
        }
        if (selected == null) {
            File localRelPath = wcRoot.computeRelPath(localAbsPath);
            ((SVNWCDb) myWCContext.getDb()).readChildren(wcRoot, localRelPath, nodes, conflicts);
            allChildren.addAll(nodes.keySet());
            allChildren.addAll(childrenFiles.keySet());
            allChildren.addAll(conflicts);
        } else {
            File selectedAbsPath = SVNFileUtil.createFilePath(localAbsPath, selected);
            
            SVNWCDbInfo info = null;
            try {
                info = readInfo(myWCContext, selectedAbsPath);
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                    throw e;
                }
            }
            if (info != null) {
                if (!info.conflicted || info.status != SVNWCDbStatus.Normal || info.kind != SVNWCDbKind.Unknown) {
                    nodes.put(selected, info);
                }
                if (info.conflicted) {
                    conflicts.add(selected);
                }
                
            }
            allChildren.add(selected);
        }
        if (selected == null) {
            if (!skipThisDir) {
                sendStatusStructure(localAbsPath, parentReposInfo, dirInfo, SVNFileType.getNodeKind(fileType), fileType == SVNFileType.SYMLINK, getAll, handler);
            }
            if (depth == SVNDepth.EMPTY) {
                return;
            }
        }
        for(String name : allChildren) {
            File nodeAbsPath = SVNFileUtil.createFilePath(localAbsPath, name);
            SVNFileType nodeFileType = childrenFiles.containsKey(name) ? SVNFileType.getType(childrenFiles.get(name)) : null;
            SVNWCDbInfo nodeInfo = nodes.get(name);
            
            if (nodeInfo != null) {
                if (nodeInfo.status != SVNWCDbStatus.NotPresent && nodeInfo.status != SVNWCDbStatus.Excluded && 
                        nodeInfo.status != SVNWCDbStatus.ServerExcluded) {
                    if (depth == SVNDepth.FILES && nodeInfo.kind == SVNWCDbKind.Dir) {
                        continue;
                    }
                    sendStatusStructure(nodeAbsPath, dirReposInfo, nodeInfo, SVNFileType.getNodeKind(nodeFileType), nodeFileType == SVNFileType.SYMLINK, getAll, handler);
                    if (depth == SVNDepth.INFINITY && nodeInfo.kind == SVNWCDbKind.Dir) {
                        getDirStatus(nodeAbsPath, null, true, dirReposInfo, nodeInfo, nodeFileType, ignorePatterns, SVNDepth.INFINITY, getAll, noIgnore, handler);
                    }
                    continue;
                }
            }
            
            if (conflicts.contains(name)) {
                if (ignorePatterns != null && patterns == null) {
                    patterns = collectIgnorePatterns(wcRoot, wcRoot.computeRelPath(localAbsPath), ignorePatterns);
                }
                sendUnversionedItem(nodeAbsPath, SVNFileType.getNodeKind(nodeFileType), true, patterns, noIgnore, handler);                
                continue;
            }
            if (nodeFileType == null) {
                continue;
            }
            if (depth == SVNDepth.FILES && nodeFileType == SVNFileType.DIRECTORY) {
                continue;
            }
            if (SVNFileUtil.getAdminDirectoryName().equals(name)) {
                continue;
            }            
            if (ignorePatterns != null && patterns == null) {
                patterns = collectIgnorePatterns(wcRoot, wcRoot.computeRelPath(localAbsPath), ignorePatterns);
            }
            sendUnversionedItem(nodeAbsPath, SVNFileType.getNodeKind(nodeFileType), false, patterns, noIgnore || selected != null, handler);                
        }
    }
    
    private static WCDbRepositoryInfo getRepositoryRootUrlRelPath(SVNWCContext context, WCDbRepositoryInfo parentRelPath, SVNWCDbInfo info, File localAbsPath) throws SVNException {
        WCDbRepositoryInfo result = new WCDbRepositoryInfo();
        if (info.reposRelpath != null && info.reposRootUrl != null) {
            result.relPath = info.reposRelpath;
            result.uuid = info.reposUuid;
            result.rootUrl = info.reposRootUrl;
        } else if (parentRelPath != null && parentRelPath.rootUrl != null && parentRelPath.relPath != null) {
            result.relPath = SVNFileUtil.createFilePath(parentRelPath.relPath, SVNFileUtil.getFileName(localAbsPath));
            result.uuid = parentRelPath.uuid;
            result.rootUrl = parentRelPath.rootUrl;            
        } else if (info.status == SVNWCDbStatus.Added) {
            WCDbAdditionInfo additionInfo = context.getDb().scanAddition(localAbsPath, AdditionInfoField.reposRelPath, AdditionInfoField.reposRootUrl, AdditionInfoField.reposUuid);
            result.relPath = additionInfo.reposRelPath;
            result.uuid = additionInfo.reposUuid;
            result.rootUrl = additionInfo.reposRootUrl;
        } else if (info.haveBase) {
            WCDbRepositoryInfo repoInfo = context.getDb().scanBaseRepository(localAbsPath, RepositoryInfoField.relPath, RepositoryInfoField.rootUrl, RepositoryInfoField.uuid);
            result.relPath = repoInfo.relPath;
            result.uuid = repoInfo.uuid;
            result.rootUrl = repoInfo.rootUrl;
        }
        return result;        
    }

    public static SvnStatus internalStatus(SVNWCContext context, File localAbsPath) throws SVNException {
    
        SVNWCDbKind node_kind;
        SVNWCDbStatus node_status = null;
        boolean conflicted;
    
        assert (SVNWCDb.isAbsolute(localAbsPath));
    
        SVNNodeKind kind = SVNFileType.getNodeKind(SVNFileType.getType(localAbsPath));
        try {
            WCDbInfo info = context.getDb().readInfo(localAbsPath, InfoField.status, InfoField.kind, InfoField.conflicted);
            node_status = info.status;
            node_kind = info.kind;
            conflicted = info.conflicted;
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                throw e;
            }
            node_kind = SVNWCDbKind.Unknown;
            conflicted = false;
        }
        if (node_status == SVNWCDbStatus.ServerExcluded || 
            node_status == SVNWCDbStatus.NotPresent || 
            node_status == SVNWCDbStatus.Excluded) {
            
            node_kind = SVNWCDbKind.Unknown;
        }
    
        if (node_kind != SVNWCDbKind.Unknown) {
            /* Check for hidden in the parent stub */
            boolean hidden = context.getDb().isNodeHidden(localAbsPath);
    
            if (hidden) {
                node_kind = SVNWCDbKind.Unknown;
            }
        }
    
        if (node_kind == SVNWCDbKind.Unknown) {
            return assembleUnversioned17(localAbsPath, kind, conflicted, false);
        }
    
        boolean isRoot;
        if (SVNFileUtil.getParentFile(localAbsPath) == null) {
            isRoot = true;
        } else {
            isRoot = context.getDb().isWCRoot(localAbsPath);
        }
        
        WCDbRepositoryInfo reposInfo = new WCDbRepositoryInfo();
        if (SVNFileUtil.getFileDir(localAbsPath) != null && !isRoot) {
    
            File parent_abspath = SVNFileUtil.getFileDir(localAbsPath);
            try {
                WCDbInfo parent_info = context.getDb().readInfo(parent_abspath, InfoField.reposRelPath, InfoField.reposRootUrl, InfoField.reposUuid);
                reposInfo.relPath  = parent_info.reposRelPath;
                reposInfo.rootUrl  = parent_info.reposRootUrl;
                reposInfo.uuid = parent_info.reposUuid;
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND || e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_NOT_WORKING_COPY)
                /* || SVN_WC__ERR_IS_NOT_CURRENT_WC(err)) */ {
                    reposInfo.relPath  = null;
                    reposInfo.rootUrl  = null;
                    reposInfo.uuid = null;
                } else {
                    throw e;
                }
            }
        } 
        return assembleStatus(context, localAbsPath, reposInfo, null, kind, false, true, false, null);
    
    }
}
