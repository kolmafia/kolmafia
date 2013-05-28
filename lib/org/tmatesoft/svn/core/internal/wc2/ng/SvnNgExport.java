package org.tmatesoft.svn.core.internal.wc2.ng;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.wc.*;
import org.tmatesoft.svn.core.internal.wc.admin.SVNTranslator;
import org.tmatesoft.svn.core.internal.wc17.SVNStatusEditor17;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.PristineContentsInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.ExternalNodeInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeOriginInfo;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbExternals;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc2.SvnExport;
import org.tmatesoft.svn.core.wc2.SvnStatus;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class SvnNgExport extends SvnNgOperationRunner<Long, SvnExport> {

    @Override
    protected Long run(SVNWCContext context) throws SVNException {
        SVNRevision revision = getOperation().getRevision();
        File to = getOperation().getFirstTarget().getFile();
        File from = getOperation().getSource().getFile();
        
        String eolStyle = getOperation().getEolStyle();
        boolean ignoreKeywords = !getOperation().isExpandKeywords();
        boolean force = getOperation().isForce();
        SVNDepth depth = getOperation().getDepth();
        
        if (revision == SVNRevision.UNDEFINED) {
            revision = SVNRevision.WORKING;
        }
        
        if (SVNFileType.getType(from) == SVNFileType.FILE) {
            if (SVNFileType.getType(to) == SVNFileType.DIRECTORY) {
                to = new File(to, from.getName());
            }
        }        
        copyVersionedDir(from, to, revision, eolStyle, ignoreKeywords, force, depth);
        handleEvent(SVNEventFactory.createSVNEvent(to, SVNNodeKind.NONE, null, -1, SVNEventAction.UPDATE_COMPLETED, null, null, null));
        return Long.valueOf(revision.getNumber());
    }
    
    private void copyVersionedDir(File from, File to, SVNRevision revision, String eolStyle, boolean ignoreKeywords, boolean force, SVNDepth depth) throws SVNException {
        if (revision != SVNRevision.WORKING) {
            Structure<NodeOriginInfo> originInfo = getWcContext().getNodeOrigin(from, false, NodeOriginInfo.isCopy, NodeOriginInfo.reposRelpath);
            if (originInfo.is(NodeOriginInfo.isCopy) && !originInfo.hasValue(NodeOriginInfo.reposRelpath)) {
                return;
            }
            originInfo.release();
        } else {
            boolean isDeleted = getWcContext().isNodeStatusDeleted(from);
            if (isDeleted) {
                return;
            }
        }
        SVNNodeKind fromKind = getWcContext().readKind(from, false);
        if (fromKind == SVNNodeKind.DIR) {
            if (to.exists() && !force) {
                SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, 
                        "Destination directory exists, and will not be overwritten unless forced");
                SVNErrorManager.error(error, SVNLogType.WC);
            }
            SVNFileUtil.ensureDirectoryExists(to);
            List<File> children = getWcContext().getNodeChildren(from, false);
            for (File child : children) {
                checkCancelled();
                File targetPath = new File(to, child.getName());
                SVNNodeKind childKind = getWcContext().readKind(child, false);
                if (childKind == SVNNodeKind.DIR && depth.compareTo(SVNDepth.IMMEDIATES) >= 0) {
                    handleEvent(SVNEventFactory.createSVNEvent(targetPath, SVNNodeKind.NONE, null, -1, SVNEventAction.UPDATE_ADD, null, null, null));
                    
                    if (depth == SVNDepth.INFINITY) {
                        copyVersionedDir(child, targetPath, revision, eolStyle, ignoreKeywords, force, depth);
                    } else {
                        SVNFileUtil.ensureDirectoryExists(targetPath);
                    }
                } else if (childKind == SVNNodeKind.FILE && depth.compareTo(SVNDepth.FILES) >= 0) {
                    ISVNWCDb.SVNWCDbKind externalKind = null;
                    try {
                        Structure<ExternalNodeInfo> info = SvnWcDbExternals.readExternal(getWcContext(), child, child, ExternalNodeInfo.kind);
                        externalKind = info.<ISVNWCDb.SVNWCDbKind>get(ExternalNodeInfo.kind);
                        info.release();
                    } catch (SVNException e) {
                        if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                            throw e;
                        }
                        externalKind = null;
                    }
                    if (externalKind != ISVNWCDb.SVNWCDbKind.File) {
                        copyVersionedFile(child, targetPath, revision, eolStyle, ignoreKeywords);
                    }
                }
            }
            SVNDepth nodeDepth = getWcContext().getNodeDepth(from);
            if (!getOperation().isIgnoreExternals() && depth == SVNDepth.INFINITY && nodeDepth == SVNDepth.INFINITY) {
                String externalProperty = getWcContext().getProperty(from, SVNProperty.EXTERNALS);
                if (externalProperty != null) {
                    SVNExternal[] externals = SVNExternal.parseExternals(from, externalProperty);
                    for (int i = 0; i < externals.length; i++) {
                        File extFrom = new File(from, externals[i].getPath());
                        File extTo = new File(to, externals[i].getPath());
                        if (extTo.getParentFile() != null) {
                            SVNFileUtil.ensureDirectoryExists(extTo.getParentFile());
                        }
                        copyVersionedDir(extFrom, extTo, revision, eolStyle, ignoreKeywords, force, SVNDepth.INFINITY);
                    }
                }
            }
        } else if (fromKind == SVNNodeKind.FILE) {
            SVNFileType toType = SVNFileType.getType(to);
            if ((toType == SVNFileType.FILE || toType == SVNFileType.UNKNOWN) && !force) {
                SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, 
                        "Destination file ''{0}'' exists, and will not be overwritten unless forced", to);
                SVNErrorManager.error(error, SVNLogType.WC);
            } else if (toType == SVNFileType.DIRECTORY) {
                SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, 
                        "Destination ''{0}'' exists. Cannot overwrite directory with non-directory", to);
                SVNErrorManager.error(error, SVNLogType.WC);
            }
            copyVersionedFile(from, to, revision, eolStyle, ignoreKeywords);
        }
    }

    private void copyVersionedFile(File from, File to, SVNRevision revision, String eolStyle, boolean ignoreKeywords) throws SVNException {
        boolean isDeleted = getWcContext().isNodeStatusDeleted(from);
        if (revision == SVNRevision.WORKING && isDeleted) {
            return;
        }
        
        File source = null;
        SVNProperties properties = null;
        boolean  modified = false;
        if (revision != SVNRevision.WORKING) {
            PristineContentsInfo pristine = getWcContext().getPristineContents(from, false, true);
            if (pristine != null) {
                source = pristine.path;
            }
            if (source == null) {
                return;
            }
            properties = getWcContext().getPristineProps(from);
        } else {
            properties = getWcContext().getDb().readProperties(from);
            SvnStatus fromStatus = SVNStatusEditor17.internalStatus(getWcContext(), from);
            modified = fromStatus.getTextStatus() != SVNStatusType.STATUS_NORMAL;
            source = from;
        }

        long timestamp;
        boolean special = properties.getStringValue(SVNProperty.SPECIAL) != null;
        boolean executable = properties.getStringValue(SVNProperty.EXECUTABLE) != null;
        String keywords = properties.getStringValue(SVNProperty.KEYWORDS);
        String charsetProp = properties.getStringValue(SVNProperty.CHARSET);
        String mimeType = properties.getStringValue(SVNProperty.MIME_TYPE);        
        String charset = SVNTranslator.getCharset(charsetProp, mimeType, from, getOperation().getOptions());
        if (special && SVNFileUtil.symlinksSupported()) {
            String linkTarget = SVNFileUtil.getSymlinkName(from);  
            SVNFileUtil.createSymlink(to, linkTarget);
            return;
        }
        
        byte[] eols = eolStyle != null ? SVNTranslator.getEOL(eolStyle, getOperation().getOptions()) : null;
        if (eols == null) {
            eolStyle = properties.getStringValue(SVNProperty.EOL_STYLE);
            eols = SVNTranslator.getEOL(eolStyle, getOperation().getOptions());
        }
        SVNDate committedDate; 
        if (modified) {
            timestamp = SVNFileUtil.getFileLastModified(from);
            committedDate = new SVNDate(timestamp, 0);
        } else {
            Structure<NodeInfo> nodeInfo = getWcContext().getDb().readInfo(from, NodeInfo.changedDate);
            committedDate = nodeInfo.<SVNDate>get(NodeInfo.changedDate);
            timestamp = committedDate.getTime();
            nodeInfo.release();
        }

        Map<String, byte[]> keywordsMap = null;
        if (keywords != null) {
            Structure<NodeInfo> nodeInfo = getWcContext().getDb().readInfo(from, NodeInfo.changedAuthor, NodeInfo.changedRev);
            String rev = Long.toString(nodeInfo.lng(NodeInfo.changedRev));
            String author = nodeInfo.get(NodeInfo.changedAuthor);
            nodeInfo.release();
            if (modified) {
                author = "(local)";
                rev += "M";
            } 
            SVNURL nodeUrl = getWcContext().getNodeUrl(from);
            keywordsMap = SVNTranslator.computeKeywords(keywords, nodeUrl.toString(), author, SVNDate.formatDate(committedDate), rev, getOperation().getOptions());
        }

        File tmpFile = SVNFileUtil.createUniqueFile(to.getParentFile(), "svnkit", ".tmp", false);
        try {
            OutputStream os = null;
            InputStream is = null;
            try {
                is = SVNFileUtil.openFileForReading(source);
                os = SVNFileUtil.openFileForWriting(tmpFile);
                if (eols != null || keywordsMap != null) {
                    os = SVNTranslator.getTranslatingOutputStream(os, charset, eols, false, keywordsMap, !ignoreKeywords);
                }
                SVNTranslator.copy(is, os);
            } catch (IOException e) {
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e), SVNLogType.WC);
            } finally {
                SVNFileUtil.closeFile(is);
                SVNFileUtil.closeFile(os);
            }
            SVNFileUtil.rename(tmpFile, to);
        } finally {
            SVNFileUtil.deleteFile(tmpFile);
        }
        if (executable) {
            SVNFileUtil.setExecutable(to, true);
        }
        if (!special && timestamp > 0) {
            SVNFileUtil.setLastModified(to, timestamp);
        }
        handleEvent(SVNEventFactory.createSVNEvent(to, SVNNodeKind.NONE, null, -1, SVNEventAction.UPDATE_ADD, null, null, null));
    }


}
