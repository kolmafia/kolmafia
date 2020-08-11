package org.tmatesoft.svn.core.internal.wc2.patch;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.patch.SVNPatchTargetInfo;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;

public interface ISvnPatchContext {

    void resolvePatchTargetStatus(SvnPatchTarget patchTarget, File workingCopyDirectory, boolean followMoves, List<SVNPatchTargetInfo> targetsInfo) throws SVNException;

    File createTempFile(File workingCopyDirectory) throws SVNException;

    SVNProperties getActualProps(File absPath) throws SVNException;

    boolean isTextModified(File absPath, boolean exactComparison) throws SVNException;

    SVNNodeKind readKind(File absPath, boolean showDeleted, boolean showHidden) throws SVNException;

    Map<? extends String,? extends byte[]> computeKeywords(File localAbsPath, SVNPropertyValue keywordsVal) throws SVNException;

    ISVNEventHandler getEventHandler();

    void setProperty(File absPath, String propertyName, SVNPropertyValue propertyValue) throws SVNException;

    void delete(File absPath) throws SVNException;

    void add(File absPath) throws SVNException;

    void move(File absPath, File moveTargetAbsPath) throws SVNException;

    boolean isExecutable(File absPath) throws SVNException;

    void setExecutable(File absPath, boolean executable);

    void translate(File patchedAbsPath, File dst, String charset, byte[] eol, Map<String, byte[]> keywords, boolean special, boolean expand) throws SVNException;

    void copySymlink(File src, File dst) throws SVNException;

    void writeSymlinkContent(File absPath, String linkName) throws SVNException;

    String readSymlinkContent(File absPath) throws SVNException;

    SVNFileType getKindOnDisk(File file);

    File wasNodeMovedHere(File localAbsPath) throws SVNException;
}
