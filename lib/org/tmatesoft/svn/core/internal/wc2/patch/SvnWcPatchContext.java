package org.tmatesoft.svn.core.internal.wc2.patch;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.admin.SVNTranslator;
import org.tmatesoft.svn.core.internal.wc.patch.SVNPatchTargetInfo;
import org.tmatesoft.svn.core.internal.wc17.SVNStatusEditor17;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgAdd;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgPropertiesManager;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgRemove;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgWcToWcCopy;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc2.SvnStatus;

public class SvnWcPatchContext implements ISvnPatchContext {
    
    private final SVNWCContext context;

    public SvnWcPatchContext(SVNWCContext context) {
        this.context = context;
    }

    @Override
    public void resolvePatchTargetStatus(SvnPatchTarget patchTarget,
                                         File workingCopyDirectory,
                                         boolean followMoves,
                                         List<SVNPatchTargetInfo> targetsInfo) throws SVNException {
        SvnStatus status;
        try {
            status = SVNStatusEditor17.internalStatus(context, patchTarget.getAbsPath(), true);

            if (status.getNodeStatus() == SVNStatusType.STATUS_IGNORED ||
                    status.getNodeStatus() == SVNStatusType.STATUS_UNVERSIONED ||
                    status.getNodeStatus() == SVNStatusType.MISSING ||
                    status.getNodeStatus() == SVNStatusType.OBSTRUCTED ||
                    status.isConflicted()) {
                patchTarget.setSkipped(true);
                patchTarget.setObstructed(true);
                return;
            } else if (status.getNodeStatus() == SVNStatusType.STATUS_DELETED) {
                patchTarget.setLocallyDeleted(true);
            }
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                throw e;
            }
            patchTarget.setLocallyDeleted(true);
            patchTarget.setDbKind(SVNNodeKind.NONE);
            status = null;
        }

        if (status != null && status.getKind() != SVNNodeKind.UNKNOWN) {
            patchTarget.setDbKind(status.getKind());
        } else {
            patchTarget.setDbKind(SVNNodeKind.NONE);
        }

        SVNFileType fileType = SVNFileType.getType(patchTarget.getAbsPath());
        patchTarget.setSymlink(fileType == SVNFileType.SYMLINK);
        patchTarget.setKindOnDisk(SVNFileType.getNodeKind(fileType));

        if (patchTarget.isLocallyDeleted()) {
            SVNWCContext.NodeMovedAway nodeMovedAway = null;
            if (followMoves &&
                    !SvnPatchTarget.targetIsAdded(targetsInfo, patchTarget.getAbsPath())) {
                nodeMovedAway = context.nodeWasMovedAway(patchTarget.getAbsPath());
            }
            if (nodeMovedAway != null && nodeMovedAway.movedToAbsPath != null) {
                patchTarget.setAbsPath(nodeMovedAway.movedToAbsPath);
                patchTarget.setRelPath(SVNFileUtil.skipAncestor(workingCopyDirectory, nodeMovedAway.movedToAbsPath));

                assert patchTarget.getRelPath() != null && patchTarget.getRelPath().getPath().length() > 0;

                patchTarget.setLocallyDeleted(false);
                fileType = SVNFileType.getType(patchTarget.getAbsPath());
                patchTarget.setSymlink(fileType == SVNFileType.SYMLINK);
                patchTarget.setKindOnDisk(SVNFileType.getNodeKind(fileType));
            } else if (patchTarget.getKindOnDisk() != SVNNodeKind.NONE) {
                patchTarget.setSkipped(true);
                return;
            }
        }

        if (SVNFileUtil.symlinksSupported()) {
            if (patchTarget.getKindOnDisk() == SVNNodeKind.FILE &&
                    !patchTarget.isSymlink() &&
                    !patchTarget.isLocallyDeleted() &&
                    status != null &&
                    status.getPropertiesStatus() != SVNStatusType.STATUS_NONE) {
                final String value = context.getProperty(patchTarget.getAbsPath(), SVNProperty.SPECIAL);
                if (value != null) {
                    patchTarget.setSymlink(true);
                }
            }
        }
    }

    @Override
    public File createTempFile(File workingCopyDirectory) throws SVNException {
        return SVNFileUtil.createUniqueFile(context.getDb().getWCRootTempDir(workingCopyDirectory), "", "", true); //TODO: remove temp files
    }

    @Override
    public SVNProperties getActualProps(File absPath) throws SVNException {
        return context.getActualProps(absPath);
    }

    @Override
    public boolean isTextModified(File absPath, boolean exactComparison) throws SVNException {
        return context.isTextModified(absPath, exactComparison);
    }

    @Override
    public SVNNodeKind readKind(File absPath, boolean showDeleted, boolean showHidden) throws SVNException {
        return context.readKind(absPath, showDeleted, showHidden);
    }

    @Override
    public Map<? extends String, ? extends byte[]> computeKeywords(File localAbsPath, SVNPropertyValue keywordsVal) throws SVNException {
        ISVNWCDb.WCDbInfo nodeChangedInfo = context.getNodeChangedInfo(localAbsPath);
        long changedRev = nodeChangedInfo.changedRev;
        SVNDate changedDate = nodeChangedInfo.changedDate;
        String changedAuthor = nodeChangedInfo.changedAuthor;

        SVNURL url = context.getNodeUrl(localAbsPath);
        SVNWCContext.SVNWCNodeReposInfo nodeReposInfo = context.getNodeReposInfo(localAbsPath);
        SVNURL reposRootUrl = nodeReposInfo.reposRootUrl;

        return SVNTranslator.computeKeywords(SVNPropertyValue.getPropertyAsString(keywordsVal), url == null ? null : url.toString(), reposRootUrl  == null ? null : reposRootUrl.toString(), changedAuthor, changedDate.format(), String.valueOf(changedRev), null);
    }

    @Override
    public ISVNEventHandler getEventHandler() {
        return context.getEventHandler();
    }

    @Override
    public void setProperty(File absPath, String propertyName, SVNPropertyValue propertyValue) throws SVNException {
        SvnNgPropertiesManager.setProperty(context, absPath, propertyName, propertyValue, SVNDepth.EMPTY, true, null, null);
    }

    @Override
    public void delete(File absPath) throws SVNException {
        SvnNgRemove.delete(context, absPath, null, false, false, null);
    }

    @Override
    public void add(File absPath) throws SVNException {
        SvnNgAdd add = new SvnNgAdd();
        add.setWcContext(context);
        add.addFromDisk(absPath, null, false);
    }

    @Override
    public void move(File absPath, File moveTargetAbsPath) throws SVNException {
        SvnNgWcToWcCopy svnNgWcToWcCopy = new SvnNgWcToWcCopy();
        svnNgWcToWcCopy.setWcContext(context);
        svnNgWcToWcCopy.move(context, absPath, moveTargetAbsPath, true);
        SVNFileUtil.deleteFile(absPath);
    }

    @Override
    public boolean isExecutable(File absPath) throws SVNException {
        return SVNFileUtil.isExecutable(absPath);
    }

    @Override
    public void setExecutable(File absPath, boolean executable) {
        SVNFileUtil.setExecutable(absPath, executable);
    }

    @Override
    public void translate(File patchedAbsPath, File dst, String charset, byte[] eol, Map<String, byte[]> keywords, boolean special, boolean expand) throws SVNException {
        SVNTranslator.translate(patchedAbsPath, dst, charset, eol, keywords, special, expand);
    }

    @Override
    public void copySymlink(File src, File dst) throws SVNException {
        SVNFileUtil.deleteFile(dst);
        SVNFileUtil.copySymlink(src, dst);
    }

    @Override
    public void writeSymlinkContent(File absPath, String linkName) throws SVNException {
        if (linkName.endsWith("\n")) {
            linkName = linkName.substring(0, linkName.length() - "\n".length());
        }
        if (linkName.endsWith("\r")) {
            linkName = linkName.substring(0, linkName.length() - "\r".length());
        }
        if (SVNFileType.getType(absPath) != SVNFileType.NONE) {
            SVNFileUtil.deleteFile(absPath);
        }
        SVNFileUtil.createSymlink(absPath, linkName);
    }

    @Override
    public String readSymlinkContent(File absPath) {
        return "link " + SVNFileUtil.getSymlinkName(absPath);
    }

    @Override
    public SVNFileType getKindOnDisk(File file) {
        return SVNFileType.getType(file);
    }

    @Override
    public File wasNodeMovedHere(File localAbsPath) throws SVNException {
        try {
            final ISVNWCDb.Moved moved = context.getDb().scanMoved(localAbsPath);
            final File movedFromAbsPath = moved.movedFromAbsPath;
            final File deleteOpRootAbsPath = moved.movedFromDeleteAbsPath;

            return movedFromAbsPath;
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_UNEXPECTED_STATUS) {
                throw e;
            }
        }
        return null;
    }
}
