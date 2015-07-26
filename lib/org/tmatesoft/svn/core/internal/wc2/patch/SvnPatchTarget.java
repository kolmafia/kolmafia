package org.tmatesoft.svn.core.internal.wc2.patch;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.*;
import org.tmatesoft.svn.core.internal.wc.admin.SVNTranslator;
import org.tmatesoft.svn.core.internal.wc.patch.*;
import org.tmatesoft.svn.core.internal.wc17.SVNStatusEditor17;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc2.ng.*;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc2.SvnScheduleForAddition;
import org.tmatesoft.svn.core.wc2.SvnStatus;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.*;
import java.util.*;

public class SvnPatchTarget extends SvnTargetContent {

    private static final int MAX_FUZZ = 2;

    private File absPath;
    private File relPath;

    private File patchedAbsPath;
    private File rejectAbsPath;
    private File moveTargetAbsPath;

    private boolean filtered;
    private boolean skipped;
    private boolean hasTextChanges;
    private boolean added;
    private boolean deleted;
    private boolean hasPropChanges;
    private Map<String, SvnPropertiesPatchTarget> propTargets;
    private boolean special;
    private boolean symlink;
    private boolean replaced;
    private boolean locallyDeleted;
    private SVNNodeKind kindOnDisk;
    private SVNNodeKind dbKind;

    private boolean hasLocalModifications;
    private boolean hadRejects;
    private boolean hadPropRejects;
    private boolean executable;
    private File canonPathFromPatchfile;
    private String eolStr;

    private SVNPatchFileStream stream;
    private SVNPatchFileStream patchedStream;
    private SVNPatchFileStream rejectStream;

    public SvnPatchTarget() {
        this.propTargets = new HashMap<String, SvnPropertiesPatchTarget>();
    }

    public boolean isFiltered() {
        return filtered;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public boolean hasTextChanges() {
        return hasTextChanges;
    }

    public boolean isAdded() {
        return added;
    }

    public File getAbsPath() {
        return absPath;
    }

    public File getMoveTargetAbsPath() {
        return moveTargetAbsPath;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean hasPropChanges() {
        return hasPropChanges;
    }

    public void setSpecial(boolean special) {
        this.special = special;
    }

    public Map<String, SvnPropertiesPatchTarget> getPropTargets() {
        return propTargets;
    }

    public String getEolStr() {
        return eolStr;
    }

    public File getRejectAbsPath() {
        return rejectAbsPath;
    }

    public void setRejectAbsPath(File rejectAbsPath) {
        this.rejectAbsPath = rejectAbsPath;
    }

    public SVNPatchFileStream getStream() {
        return stream;
    }

    public SVNPatchFileStream getPatchedStream() {
        return patchedStream;
    }

    public void setPatchedStream(SVNPatchFileStream patchedStream) {
        this.patchedStream = patchedStream;
    }

    public SVNPatchFileStream getRejectStream() {
        return rejectStream;
    }

    public void setRejectStream(SVNPatchFileStream rejectStream) {
        this.rejectStream = rejectStream;
    }

    public static SvnPatchTarget applyPatch(SvnPatch patch, File workingCopyDirectory, int stripCount, SVNWCContext context, boolean ignoreWhitespace, boolean removeTempFiles) throws SVNException, IOException {
        SvnPatchTarget target = initPatchTarget(patch, workingCopyDirectory, stripCount, removeTempFiles, context);
        if (target.isSkipped()) {
            return target;
        }
        List<SvnDiffHunk> hunks = patch.getHunks();
        for (SvnDiffHunk hunk : hunks) {
            SvnHunkInfo hunkInfo;
            int fuzz = 0;
            do {
                hunkInfo = target.getHunkInfo(hunk, target, fuzz, ignoreWhitespace, false);
                fuzz++;
            } while (hunkInfo.isRejected() && fuzz <= MAX_FUZZ && !hunkInfo.isAlreadyApplied());

            target.addHunkInfo(hunkInfo);
        }

        for (SvnHunkInfo hunkInfo : target.getHunkInfos()) {
            if (hunkInfo.isAlreadyApplied()) {
                continue;
            } else if (hunkInfo.isRejected()) {
                rejectHunk(target, hunkInfo.getHunk(), null);
            } else {
                applyHunk(target, target, hunkInfo, null);
            }
        }

        if (target.getKindOnDisk() == SVNNodeKind.FILE) {
            copyLinesToTarget(target, 0);
            if (!target.isEof()) {
                target.setSkipped(true);
            }
        }

        for (Map.Entry<String, SvnPropertiesPatch> entry : patch.getPropPatches().entrySet()) {
            final String propName = entry.getKey();
            final SvnPropertiesPatch propPatch = entry.getValue();

            if (SVNProperty.SPECIAL.equals(propName)) {
                target.setSpecial(true);
            }

            final Map<String, SvnPropertiesPatchTarget> propTargets = target.getPropTargets();
            final SvnPropertiesPatchTarget propTarget = propTargets.get(propName);

            final List<SvnDiffHunk> propPatchHunks = propPatch.getHunks();
            for (SvnDiffHunk hunk : propPatchHunks) {
                SvnHunkInfo hunkInfo;
                int fuzz = 0;
                do {
                    hunkInfo = target.getHunkInfo(hunk, propTarget, fuzz, ignoreWhitespace, true);
                    fuzz++;
                } while (hunkInfo.isRejected() && fuzz <= MAX_FUZZ && !hunkInfo.isAlreadyApplied());

                propTarget.addHunkInfo(hunkInfo);
            }
        }

        final Map<String, SvnPropertiesPatchTarget> propTargets = target.getPropTargets();
        for (Map.Entry<String, SvnPropertiesPatchTarget> entry : propTargets.entrySet()) {
            SvnPropertiesPatchTarget propTarget = entry.getValue();

            final List<SvnHunkInfo> hunkInfos = propTarget.getHunkInfos();

            for (SvnHunkInfo hunkInfo : hunkInfos) {
                if (hunkInfo.isAlreadyApplied()) {
                    continue;
                } else if (hunkInfo.isRejected()) {
                    rejectHunk(target, hunkInfo.getHunk(), propTarget.getName());
                } else {
                    applyHunk(target, propTarget, hunkInfo, propTarget.getName());
                }
            }

            if (propTarget.isExisted()) {
                copyLinesToTarget(propTarget, 0);
                if (!propTarget.isEof()) {
                    target.setSkipped(true);
                }
            }
        }

        if (!target.isSymlink()) {
            if (target.getKindOnDisk() == SVNNodeKind.FILE) {
                target.getStream().close();
            }

            target.getPatchedStream().close();
        }

        if (!target.isSkipped()) {
            long patchedFileSize = SVNFileUtil.getFileLength(target.getPatchedAbsPath());
            long workingFileSize;
            if (target.getKindOnDisk() == SVNNodeKind.FILE) {
                workingFileSize = SVNFileUtil.getFileLength(target.getAbsPath());
            } else {
                workingFileSize = 0;
            }

            if (patchedFileSize == 0 && workingFileSize > 0) {
                target.setDeleted(target.getDbKind() == SVNNodeKind.FILE);
            } else if (patchedFileSize == 0 && workingFileSize == 0) {
                if (target.getKindOnDisk() == SVNNodeKind.NONE &&
                        !target.hasPropChanges() &&
                        !target.isAdded()) {
                    target.setSkipped(true);
                }
            } else if (patchedFileSize > 0 && workingFileSize == 0) {
                if (target.isLocallyDeleted()) {
                    target.setReplaced(true);
                } else if (target.getDbKind() == SVNNodeKind.NONE) {
                    target.setAdded(true);
                }
            }
        }
        return target;
    }

    private static void rejectHunk(SvnPatchTarget target, SvnDiffHunk hunk, String propName) throws SVNException {
        try {
            String atat;
            String textAtat = "@@";
            String propAtat = "##";
            if (propName != null) {
                String propHeader = "Property: " + propName + "\n";
                target.getRejectStream().write(propHeader);
                atat = propAtat;
            } else {
                atat = textAtat;
            }
            String hunkHeader = String.format("%s -%s,%s +%s,%s %s\n",
                    atat,
                    hunk.getDirectedOriginalStart(),
                    hunk.getDirectedOriginalLength(),
                    hunk.getDirectedModifiedStart(),
                    hunk.getDirectedModifiedLength(),
                    atat);

            target.getRejectStream().write(hunkHeader);

            boolean[] eof = new boolean[1];
            String[] eolStr = new String[1];
            do {
                String hunkLine = hunk.readLineDiffText(eolStr, eof);
                if (!eof[0]) {
                    if (hunkLine.length() >= 1) {
                        target.getRejectStream().write(hunkLine);
                    }
                    if (eolStr[0] != null) {
                        target.getRejectStream().write(eolStr[0]);
                    }
                }
            } while (!eof[0]);

            if (propName != null) {
                target.setHadPropRejects(true);
            } else {
                target.setHadRejects(true);
            }
        } catch (IOException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }
    }

    private static void applyHunk(SvnPatchTarget target, SvnTargetContent targetContent, SvnHunkInfo hunkInfo, String propName) throws SVNException {
        if (target.getKindOnDisk() == SVNNodeKind.FILE || propName != null) {
            copyLinesToTarget(targetContent, hunkInfo.getMatchedLine() + hunkInfo.getFuzz());
            int line = targetContent.getCurrentLine() + hunkInfo.getHunk().getDirectedOriginalLength() - (2 * hunkInfo.getFuzz());
            targetContent.seekToLine(line);
            if (targetContent.getCurrentLine() != line && !targetContent.isEof()) {
                hunkInfo.setRejected(true);
                rejectHunk(target, hunkInfo.getHunk(), propName);
                return;
            }
        }

        int linesRead = 0;
        hunkInfo.getHunk().resetModifiedText();
        boolean[] eof = new boolean[1];
        do {
            String[] eolStr = new String[1];
            String hunkLine = hunkInfo.getHunk().readLineModifiedText(eolStr, eof);
            linesRead++;
            if (linesRead > hunkInfo.getFuzz() &&
                    linesRead <= hunkInfo.getHunk().getDirectedModifiedLength() - hunkInfo.getFuzz()) {
                if (hunkLine.length() >= 1) {
                    targetContent.getWriteCallback().write(target.getWriteBaton(), hunkLine);
                }
                if (eolStr[0] != null) {
                    if (targetContent.getEolStyle() != SVNWCContext.SVNEolStyle.None) {
                        eolStr[0] = targetContent.getEolStr();
                    }
                    targetContent.getWriteCallback().write(target.getWriteBaton(), eolStr[0]);
                }
            }
        } while (!eof[0]);

        if (propName != null) {
            target.setHasPropChanges(true);
        } else {
            target.setHasTextChanges(true);
        }
    }

    private SvnHunkInfo getHunkInfo(SvnDiffHunk hunk, SvnTargetContent targetContent, int fuzz, boolean ignoreWhitespace, boolean isPropHunk) throws SVNException {
        int originalStart = hunk.getDirectedOriginalStart();
        boolean alreadyApplied = false;
        int matchedLine;

        if (originalStart == 0 && fuzz > 0) {
            matchedLine = 0;
        } else if (originalStart == 0 && !isPropHunk) {
            if (getKindOnDisk() == SVNNodeKind.FILE) {
                SVNFileType kind = SVNFileType.getType(getAbsPath());
                boolean special = kind == SVNFileType.SYMLINK;
                long fileLength = SVNFileUtil.getFileLength(getAbsPath());

                if (kind == SVNFileType.FILE && !special && fileLength == 0) {
                    matchedLine = 1;
                } else {
                    if (getDbKind() == SVNNodeKind.FILE) {
                        boolean fileMatches = targetContent.matchExistingTarget(hunk);
                        if (fileMatches) {
                            matchedLine = 1;
                            alreadyApplied = true;
                        } else {
                            matchedLine = 0;
                        }
                    } else {
                        matchedLine = 0;
                    }
                }

            } else {
                matchedLine = 1;
            }
        } else if (originalStart == 0 && isPropHunk) {
            if (targetContent.isExisted()) {
                boolean propMatches = targetContent.matchExistingTarget(hunk);
                if (propMatches) {
                    matchedLine = 1;
                    alreadyApplied = true;
                } else {
                    matchedLine = 0;
                }
            } else {
                matchedLine = 1;
            }
        } else if (originalStart > 0 && targetContent.isExisted()) {
            int savedLine = targetContent.getCurrentLine();
            targetContent.seekToLine(originalStart);
            if (targetContent.getCurrentLine() != originalStart) {
                matchedLine = 0;
            } else {
                matchedLine = targetContent.scanForMatch(hunk, true, originalStart + 1, fuzz, ignoreWhitespace, false, null);
            }

            if (matchedLine != originalStart) {
                if (fuzz == 0) {
                    int modifiedStart = hunk.getDirectedModifiedStart();
                    if (modifiedStart == 0) {
                        alreadyApplied = isLocallyDeleted();
                    } else {
                        targetContent.seekToLine(modifiedStart);
                        matchedLine = targetContent.scanForMatch(hunk, true, modifiedStart + 1, fuzz, ignoreWhitespace, true, null);
                        alreadyApplied = (matchedLine == modifiedStart);
                    }
                } else {
                    alreadyApplied = false;
                }

                if (!alreadyApplied) {
                    targetContent.seekToLine(1);
                    matchedLine = targetContent.scanForMatch(hunk, false, originalStart, fuzz, ignoreWhitespace, false, null);
                    if (matchedLine == 0) {
                        matchedLine = targetContent.scanForMatch(hunk, true, 0, fuzz, ignoreWhitespace, false, null);
                    }
                }

            }
            targetContent.seekToLine(savedLine);
        } else {
            matchedLine = 0;
        }
        return new SvnHunkInfo(hunk, matchedLine, matchedLine == 0, alreadyApplied, fuzz);
    }

    private static void copyLinesToTarget(SvnTargetContent target, int line) throws SVNException {
        while ((target.getCurrentLine() < line || line == 0) && !target.isEof()) {
            String targetLine = target.readLine();
            if (!target.isEof()) {
                targetLine = targetLine + target.getEolStr();
            }
            target.getWriteCallback().write(target.getWriteBaton(), targetLine);
        }
    }

    private static SvnPatchTarget initPatchTarget(SvnPatch patch, File workingCopyDirectory, int stripCount, boolean removeTempFiles, SVNWCContext context) throws SVNException, IOException {
        boolean hasPropChanges = false;
        boolean propChangesOnly = false;

        for (Map.Entry<String, SvnPropertiesPatch> entry : patch.getPropPatches().entrySet()) {
            final SvnPropertiesPatch propPatch = entry.getValue();
            if (!hasPropChanges) {
                hasPropChanges = propPatch.getHunks().size() > 0;
            } else {
                break;
            }
        }

        propChangesOnly = hasPropChanges && patch.getHunks().size() == 0;

        SvnPatchTarget target = new SvnPatchTarget();//empty lists are created in the constructor
        target.setCurrentLine(1);
        target.setEolStyle(SVNWCContext.SVNEolStyle.None);

        target.setDbKind(SVNNodeKind.NONE);
        target.setKindOnDisk(SVNNodeKind.NONE);

        target.resolveTargetPath(chooseTargetFilename(patch), workingCopyDirectory, stripCount, propChangesOnly, context);
        if (!target.isSkipped()) {
            if (target.isSymlink()) {
                target.setExisted(true);

                target.setReadBaton(new SymlinkReadBaton(target.getAbsPath()));

                final SymlinkCallbacks symlinkCallbacks = new SymlinkCallbacks(workingCopyDirectory, context);
                target.setReadLineCallback(symlinkCallbacks);
                target.setTellCallback(symlinkCallbacks);
                target.setSeekCallback(symlinkCallbacks);
            } else if (target.getKindOnDisk() == SVNNodeKind.FILE) {
                target.setHasLocalModifications(context.isTextModified(target.getAbsPath(), false));
                target.setExecutable(SVNFileUtil.isExecutable(target.getAbsPath()));

                final Map<String, byte[]> keywords = new HashMap<String, byte[]>();
                SVNWCContext.SVNEolStyle[] eolStyle = (SVNWCContext.SVNEolStyle[]) new SVNWCContext.SVNEolStyle[1];
                String[] eolStr = new String[1];

                if (target.getKeywords() != null) {
                    keywords.putAll(target.getKeywords());
                }
                eolStyle[0] = target.getEolStyle();
                eolStr[0] = target.getEolStr();
                obtainEolAndKeywordsForFile(keywords, eolStyle, eolStr, context, target.getAbsPath());

                target.setKeywords(keywords);
                target.setEolStyle(eolStyle[0]);
                target.setEolStr(eolStr[0]);

                RegularCallbacks regularCallbacks = new RegularCallbacks();

                target.setExisted(true);
                target.setReadLineCallback(regularCallbacks);
                target.setSeekCallback(regularCallbacks);
                target.setTellCallback(regularCallbacks);
                target.setStream(SVNPatchFileStream.openReadOnly(target.getAbsPath()));
                target.setReadBaton(target.getStream());
            }

            if (patch.getOperation() == SvnDiffCallback.OperationKind.Added) {
                target.setAdded(true);
            } else if (patch.getOperation() == SvnDiffCallback.OperationKind.Deleted) {
                target.setDeleted(true);
            } else if (patch.getOperation() == SvnDiffCallback.OperationKind.Moved) {
                File moveTargetPath = patch.getNewFileName();

                if (stripCount > 0) {
                    moveTargetPath = SVNPatchTarget.stripPath(moveTargetPath, stripCount);
                }

                final File moveTargetRelPath;
                if (SVNFileUtil.isAbsolute(moveTargetPath)) {
                    moveTargetRelPath = SVNFileUtil.createFilePath(SVNPathUtil.getPathAsChild(SVNFileUtil.getFilePath(workingCopyDirectory),
                            SVNFileUtil.getFilePath(moveTargetPath)));
                    if (moveTargetRelPath == null) {
                        target.setSkipped(true);
                        target.setAbsPath(null);
                        return null;
                    }
                } else {
                    moveTargetRelPath = moveTargetPath;
                }

                boolean underRoot = isUnderRoot(workingCopyDirectory, moveTargetRelPath);
                if (!underRoot) {
                    target.setSkipped(true);
                    target.setAbsPath(null);
                    return target;
                } else {
                    target.setAbsPath(SVNFileUtil.createFilePath(workingCopyDirectory, moveTargetRelPath));
                }

                SVNNodeKind kindOnDisk = SVNFileType.getNodeKind(SVNFileType.getType(target.getMoveTargetAbsPath()));
                SVNNodeKind wcKind = context.readKind(target.getMoveTargetAbsPath(), false);

                if (kindOnDisk != SVNNodeKind.NONE || wcKind != SVNNodeKind.NONE) {
                    target.setSkipped(true);
                    target.setAbsPath(null);
                    return target;
                }
            }
            if (!target.isSymlink()) {
                File uniqueFile = createTempFile(workingCopyDirectory, context); //TODO: remove temp files
                target.setPatchedAbsPath(uniqueFile);
                target.setPatchedStream(SVNPatchFileStream.openForWrite(uniqueFile));
                target.setWriteBaton(target.getPatchedStream());
                target.setWriteCallback(new RegularWriteCallback());
            } else {
                File uniqueFile = createTempFile(workingCopyDirectory, context);
                target.setPatchedAbsPath(uniqueFile);
                target.setWriteBaton(uniqueFile);
                target.setWriteCallback(new SymlinkCallbacks(workingCopyDirectory, context));
            }

            target.setRejectAbsPath(createTempFile(workingCopyDirectory, context));
            target.setRejectStream(SVNPatchFileStream.openForWrite(target.getRejectAbsPath()));

            String diffHeader = "--- " + target.getCanonPathFromPatchfile() + "\n" + "+++ " + target.getCanonPathFromPatchfile() + "\n";
            SVNFileUtil.writeToFile(target.getRejectAbsPath(), diffHeader, "UTF-8");

            target.getRejectStream().write(diffHeader);

            if (!target.isSkipped()) {
                Map<String, SvnPropertiesPatch> propPatches = patch.getPropPatches();
                for (Map.Entry<String, SvnPropertiesPatch> entry : propPatches.entrySet()) {
                    String propName = entry.getKey();
                    SvnPropertiesPatch propPatch = entry.getValue();

                    SvnPropertiesPatchTarget propTarget = SvnPropertiesPatchTarget.initPropTarget(propName, propPatch.getOperation(), context, target.getAbsPath());
                    target.putPropTarget(propName, propTarget);
                }
            }
        }

        return target;
    }

    private static void obtainEolAndKeywordsForFile(Map<String, byte[]> keywords,
                                             SVNWCContext.SVNEolStyle[] eolStyle,
                                             String[] eolStr,
                                             SVNWCContext context, File localAbsPath) throws SVNException {
        final SVNProperties actualProps = context.getActualProps(localAbsPath);
        SVNPropertyValue keywordsVal = actualProps.getSVNPropertyValue(SVNProperty.KEYWORDS);
        if (keywordsVal != null) {
            ISVNWCDb.WCDbInfo nodeChangedInfo = context.getNodeChangedInfo(localAbsPath);
            long changedRev = nodeChangedInfo.changedRev;
            SVNDate changedDate = nodeChangedInfo.changedDate;
            String changedAuthor = nodeChangedInfo.changedAuthor;

            SVNURL url = context.getNodeUrl(localAbsPath);
            SVNWCContext.SVNWCNodeReposInfo nodeReposInfo = context.getNodeReposInfo(localAbsPath);
            SVNURL reposRootUrl = nodeReposInfo.reposRootUrl;

            if (keywords != null) {
                keywords.putAll(SVNTranslator.computeKeywords(SVNPropertyValue.getPropertyAsString(keywordsVal), url == null ? null : url.toString(), reposRootUrl  == null ? null : reposRootUrl.toString(), changedAuthor, changedDate.format(), String.valueOf(changedRev), null));
            }
        }
        SVNPropertyValue eolStyleVal = actualProps.getSVNPropertyValue(SVNProperty.EOL_STYLE);
        if (eolStyleVal != null) {
            String eolStyleValString = SVNPropertyValue.getPropertyAsString(eolStyleVal);
            SVNWCContext.SVNEolStyleInfo eolStyleInfo = SVNWCContext.SVNEolStyleInfo.fromValue(eolStyleValString);
            if (eolStr != null) {
                eolStr[0] = new String(eolStyleInfo.eolStr);
            }
            if (eolStyle != null) {
                eolStyle[0] = eolStyleInfo.eolStyle;
            }
        }
    }

    private void resolveTargetPath(File pathFromPatchFile, File workingCopyDirectory, int stripCount, boolean propChangesOnly, SVNWCContext context) throws SVNException, IOException {
        final File canonPathFromPatchfile = pathFromPatchFile;
        setCanonPathFromPatchfile(canonPathFromPatchfile);

        if (!propChangesOnly && SVNFileUtil.getFilePath(canonPathFromPatchfile).length() == 0) {
            setSkipped(true);
            setAbsPath(null);
            setRelPath(SVNFileUtil.createFilePath(""));
            return;
        }

        File strippedPath;
        if (stripCount > 0) {
            strippedPath = SVNPatchTarget.stripPath(canonPathFromPatchfile, stripCount);
        } else {
            strippedPath = canonPathFromPatchfile;
        }

        if (SVNFileUtil.isAbsolute(strippedPath)) {
            setRelPath(SVNFileUtil.createFilePath(SVNPathUtil.getPathAsChild(SVNFileUtil.getFilePath(workingCopyDirectory), SVNFileUtil.getFilePath(strippedPath))));

            if (getRelPath() == null) {
                setSkipped(true);
                setAbsPath(null);
                setRelPath(strippedPath);
                return;
            }
        } else {
            setRelPath(strippedPath);
        }

        boolean isUnderRoot = isUnderRoot(workingCopyDirectory, getRelPath());

        if (!isUnderRoot) {
            setSkipped(true);
            setAbsPath(null);
            return;
        } else {
            setAbsPath(SVNFileUtil.createFilePath(workingCopyDirectory, getRelPath()));
        }


        SvnStatus status;
        try {
            status = SVNStatusEditor17.internalStatus(context, getAbsPath());

            if (status.getNodeStatus() == SVNStatusType.STATUS_IGNORED ||
                    status.getNodeStatus() == SVNStatusType.STATUS_UNVERSIONED ||
                    status.getNodeStatus() == SVNStatusType.MISSING ||
                    status.getNodeStatus() == SVNStatusType.OBSTRUCTED ||
                    status.isConflicted()) {
                setSkipped(true);
                return;
            } else if (status.getNodeStatus() == SVNStatusType.STATUS_DELETED) {
                setLocallyDeleted(true);
            }
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                throw e;
            }
            setLocallyDeleted(true);
            setDbKind(SVNNodeKind.NONE);
            status = null;
        }

        if (status != null && status.getKind() != SVNNodeKind.UNKNOWN) {
            setDbKind(status.getKind());
        } else {
            setDbKind(SVNNodeKind.NONE);
        }

        SVNFileType fileType = SVNFileType.getType(getAbsPath());
        setSymlink(fileType == SVNFileType.SYMLINK);
        setKindOnDisk(SVNFileType.getNodeKind(fileType));

        if (isLocallyDeleted()) {
            SVNWCContext.NodeMovedAway nodeMovedAway = context.nodeWasMovedAway(getAbsPath());
            if (nodeMovedAway != null && nodeMovedAway.movedToAbsPath != null) {
                setAbsPath(nodeMovedAway.movedToAbsPath);
                setRelPath(SVNFileUtil.skipAncestor(workingCopyDirectory, nodeMovedAway.movedToAbsPath));

                assert getRelPath() != null && getRelPath().getPath().length() > 0;

                setLocallyDeleted(false);
                fileType = SVNFileType.getType(getAbsPath());
                setSymlink(fileType == SVNFileType.SYMLINK);
                setKindOnDisk(SVNFileType.getNodeKind(fileType));
            } else if (getKindOnDisk() != SVNNodeKind.NONE) {
                setSkipped(true);
                return;
            }
        }
    }

    private static boolean isUnderRoot(File workingCopyDirectory, File relPath) throws SVNException {
        File fullPath = SVNFileUtil.createFilePath(workingCopyDirectory, relPath);
        try {
            String workingCopyDirectoryPath = SVNFileUtil.getFilePath(workingCopyDirectory.getCanonicalFile());
            String canonicalFullPath = fullPath.getCanonicalPath();
            return canonicalFullPath.equals(workingCopyDirectoryPath) || SVNPathUtil.isAncestor(workingCopyDirectoryPath, canonicalFullPath);
        } catch (IOException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }
        return false;
    }

    private static File chooseTargetFilename(SvnPatch patch) {
        if (patch.getOldFileName().getPath().equals("/dev/null")) {
            return patch.getNewFileName();
        }
        if (patch.getNewFileName().getPath().equals("/dev/null")) {
            return patch.getOldFileName();
        }
        if (patch.getOperation() == SvnDiffCallback.OperationKind.Moved) {
            return patch.getOldFileName();
        }

        int oldCount = SVNPathUtil.getSegmentsCount(SVNFileUtil.getFilePath(patch.getOldFileName()));
        int newCount = SVNPathUtil.getSegmentsCount(SVNFileUtil.getFilePath(patch.getNewFileName()));
        if (oldCount == newCount) {
            oldCount = SVNPathUtil.tail(SVNFileUtil.getFilePath(patch.getOldFileName())).length();
            newCount = SVNPathUtil.tail(SVNFileUtil.getFilePath(patch.getNewFileName())).length();

            if (oldCount == newCount) {
                oldCount = SVNFileUtil.getFilePath(patch.getOldFileName()).length();
                newCount = SVNFileUtil.getFilePath(patch.getNewFileName()).length();
            }
        }
        return (oldCount < newCount) ? patch.getOldFileName() : patch.getNewFileName();
    }

    private void putPropTarget(String propName, SvnPropertiesPatchTarget propTarget) {
        propTargets.put(propName, propTarget);
    }

    private static File createTempFile(File workingCopyDirectory, SVNWCContext context) throws SVNException {
        return SVNFileUtil.createUniqueFile(context.getDb().getWCRootTempDir(workingCopyDirectory), "", "", true);
    }

    public void installPatchedTarget(File workingCopyDirectory, boolean dryRun, SVNWCContext context) throws SVNException {
        if (isDeleted()) {
            if (!dryRun) {
                SvnNgRemove.delete(context, getAbsPath(), null, false, false, null);
            }
        } else {
            if (isAdded() || isReplaced()) {
                File parentAbsPath = SVNFileUtil.getParentFile(getAbsPath());

                SVNNodeKind parentDbKind = context.readKind(parentAbsPath, false);

                if (parentDbKind == SVNNodeKind.DIR || parentDbKind == SVNNodeKind.FILE) {
                    if (parentDbKind != SVNNodeKind.DIR) {
                        setSkipped(true);
                    } else {
                        if (SVNFileType.getType(parentAbsPath) != SVNFileType.DIRECTORY) {
                            setSkipped(true);
                        }
                    }
                } else {
                    createMissingParents(workingCopyDirectory, context, dryRun);
                }
            } else {
                SVNNodeKind wcKind = context.readKind(getAbsPath(), false);

                if (getKindOnDisk() == SVNNodeKind.NONE || wcKind != getKindOnDisk()) {
                    setSkipped(true);
                }
            }

            if (!dryRun && !isSkipped()) {
                if (isSpecial()) {
                    //setPatchedStream(SVNFileUtil.openFileForReading(getPatchedAbsPath()));
                    String linkName = SVNFileUtil.readFile(getPatchedAbsPath());
                    if (linkName.startsWith("link ")) {
                        linkName = linkName.substring("link ".length());
                    }
                    if (linkName.endsWith("\n")) {
                        linkName = linkName.substring(0, linkName.length() - "\n".length());
                    }
                    if (linkName.endsWith("\r")) {
                        linkName = linkName.substring(0, linkName.length() - "\r".length());
                    }
                    SVNFileUtil.createSymlink(getAbsPath(), linkName);
                } else {
                    //TODO: a special method for special files? atomicity?
                    File dst = getMoveTargetAbsPath() != null ? getMoveTargetAbsPath() : getAbsPath();
                    if (SVNFileType.getType(getPatchedAbsPath()) == SVNFileType.SYMLINK) {
                        SVNFileUtil.deleteFile(dst);
                        SVNFileUtil.copySymlink(getPatchedAbsPath(), dst);
                    } else {
                        boolean repairEol = getEolStyle() == SVNWCContext.SVNEolStyle.Fixed || getEolStyle() == SVNWCContext.SVNEolStyle.Native;
                        SVNTranslator.translate(getPatchedAbsPath(), dst, null, getEolStr() == null ? null : getEolStr().getBytes(), getKeywords(), false, true);
                    }
                }

                if (isAdded() || isReplaced()) {
                    SvnNgAdd add = new SvnNgAdd();
                    add.setWcContext(context);
                    add.addFromDisk(getAbsPath(), null, false);
                }

                SVNFileUtil.setExecutable(getMoveTargetAbsPath() != null ? getMoveTargetAbsPath() : getAbsPath(), isExecutable());

                if (getMoveTargetAbsPath() != null) {
                    SvnNgWcToWcCopy svnNgWcToWcCopy = new SvnNgWcToWcCopy();
                    svnNgWcToWcCopy.setWcContext(context);
                    svnNgWcToWcCopy.move(context, getAbsPath(), getMoveTargetAbsPath(), true);
                    SVNFileUtil.deleteFile(getAbsPath());
                }
            }
        }
    }

    private void createMissingParents(File workingCopyDirectory,  SVNWCContext context, boolean dryRun) throws SVNException {
        File localAbsPath = workingCopyDirectory;
        File relPath = getRelPath();
        String relPathString = SVNFileUtil.getFilePath(relPath);
        String[] components = relPathString.split("/");
        int presentComponents = 0;

        for (String component : components) {
            localAbsPath = SVNFileUtil.createFilePath(localAbsPath, component);
            SVNNodeKind wcKind = context.readKind(localAbsPath, true);

            SVNNodeKind diskKind = SVNFileType.getNodeKind(SVNFileType.getType(localAbsPath));
            if (diskKind == SVNNodeKind.FILE || wcKind == SVNNodeKind.FILE) {
                setSkipped(true);
                break;
            } else if (diskKind == SVNNodeKind.DIR) {
                if (wcKind == SVNNodeKind.DIR) {
                    presentComponents++;
                } else {
                    setSkipped(true);
                    break;
                }
            } else if (wcKind != SVNNodeKind.NONE) {
                setSkipped(true);
                break;
            } else {
                break;
            }
        }

        if (!isSkipped()) {
            localAbsPath = workingCopyDirectory;
            for (int i = 0; i < presentComponents; i++) {
                String component = components[i];
                localAbsPath = SVNFileUtil.createFilePath(localAbsPath, component);
            }

            if (!dryRun && presentComponents < components.length - 1) {
                SVNFileUtil.ensureDirectoryExists(SVNFileUtil.createFilePath(workingCopyDirectory, SVNFileUtil.getFileDir(getRelPath())));
            }

            for (int i = presentComponents; i < components.length - 1; i++) {
                String component = components[i];
                localAbsPath = SVNFileUtil.createFilePath(localAbsPath, component);

                if (dryRun) {
                    ISVNEventHandler eventHandler = context.getEventHandler();
                    if (eventHandler != null) {
                        SVNEvent event = SVNEventFactory.createSVNEvent(localAbsPath, SVNNodeKind.DIR, null, -1, SVNEventAction.ADD, SVNEventAction.ADD, null, null);
                        eventHandler.handleEvent(event, ISVNEventHandler.UNKNOWN);
                    }
                } else {
                    ISVNCanceller canceller = context.getEventHandler();
                    if (canceller != null) {
                        canceller.checkCancelled();
                    }
                    SvnNgAdd add = new SvnNgAdd();
                    add.setWcContext(context);
                    add.addFromDisk(localAbsPath, null, true);
                }
            }
        }
    }

    public void installPatchedPropTarget(boolean dryRun, SVNWCContext context) throws SVNException {
        final Map<String, SvnPropertiesPatchTarget> propTargets = getPropTargets();
        for (Map.Entry<String, SvnPropertiesPatchTarget> entry : propTargets.entrySet()) {
            SvnPropertiesPatchTarget propTarget = entry.getValue();
            ISVNCanceller canceller = context.getEventHandler();
            if (canceller != null) {
                canceller.checkCancelled();
            }

            if (propTarget.getOperation() == SvnDiffCallback.OperationKind.Deleted) {
                if (!dryRun) {
                    SvnNgPropertiesManager.setProperty(context, getAbsPath(), propTarget.getName(), null, SVNDepth.EMPTY, true, null, null);
                }
                continue;
            }

            if (!hasTextChanges() && getKindOnDisk() == SVNNodeKind.NONE && !isAdded()) {
                if (!dryRun) {
                    SVNFileUtil.createEmptyFile(getAbsPath());

                    SvnNgAdd add = new SvnNgAdd();
                    add.setWcContext(context);
                    add.addFromDisk(getAbsPath(), null, false);
                }
                setAdded(true);
            }

            SVNPropertyValue propVal;
            if (propTarget.getValue() != null && SVNPropertyValue.getPropertyAsBytes(propTarget.getValue()).length != 0 &&
                    propTarget.getPatchedValue() != null && SVNPropertyValue.getPropertyAsBytes(propTarget.getPatchedValue()).length == 0) {
                propVal = null;
            } else {
                propVal = propTarget.getPatchedValue();
            }

            try {
                if (dryRun) {
                    SVNPropertyValue canonicalPropertyValue = SVNPropertiesManager.validatePropertyValue(getAbsPath(), getDbKind(), propTarget.getName(), propVal, true, null, null);
                } else {
                    SvnNgPropertiesManager.setProperty(context, getAbsPath(), propTarget.getName(), propVal, SVNDepth.EMPTY, true, null, null);
                }
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() == SVNErrorCode.ILLEGAL_TARGET ||
                        e.getErrorMessage().getErrorCode() == SVNErrorCode.NODE_UNEXPECTED_KIND ||
                        e.getErrorMessage().getErrorCode() == SVNErrorCode.IO_UNKNOWN_EOL ||
                        e.getErrorMessage().getErrorCode() == SVNErrorCode.BAD_MIME_TYPE ||
                        e.getErrorMessage().getErrorCode() == SVNErrorCode.CLIENT_INVALID_EXTERNALS_DESCRIPTION) {
                    for (SvnHunkInfo hunkInfo : propTarget.getHunkInfos()) {
                        hunkInfo.setRejected(true);
                        rejectHunk(this, hunkInfo.getHunk(), propTarget.getName());
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    public void writeOutRejectedHunks(boolean dryRun) throws SVNException {
        try {
            getRejectStream().close();
        } catch (IOException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }
        if (!dryRun && (hadRejects() || hadPropRejects())) {
            SVNFileUtil.copyFile(getRejectAbsPath(), SVNFileUtil.createFilePath(SVNFileUtil.getFilePath(getAbsPath()) + ".svnpatch.rej"), false);
        }
    }

    public void sendPatchNotification(SVNWCContext context) throws SVNException {
        ISVNEventHandler eventHandler = context.getEventHandler();
        if (eventHandler == null) {
            return;
        }
        SVNEventAction action;
        if (isSkipped()) {
            action = SVNEventAction.SKIP;
        } else if (isDeleted()) {
            action = SVNEventAction.DELETE;
        } else if (isAdded() || isReplaced() || getMoveTargetAbsPath() != null) {
            action = SVNEventAction.ADD;
        } else {
            action = SVNEventAction.PATCH;
        }

        File eventPath;
        if (getMoveTargetAbsPath() != null) {
            eventPath = getMoveTargetAbsPath();
        } else {
            eventPath = getAbsPath() != null ? getAbsPath() : getRelPath();
        }

        SVNStatusType contentState = SVNStatusType.UNKNOWN;
        SVNStatusType propState = SVNStatusType.UNKNOWN;

        if (action == SVNEventAction.SKIP) {
            if (getDbKind() == SVNNodeKind.NONE || getDbKind() == SVNNodeKind.UNKNOWN) {
                contentState = SVNStatusType.MISSING;
            } else if (getDbKind() == SVNNodeKind.DIR) {
                contentState = SVNStatusType.OBSTRUCTED;
            } else {
                contentState = SVNStatusType.UNKNOWN;
            }
        } else {
            if (hadRejects()) {
                contentState = SVNStatusType.CONFLICTED;
            } else if (hasLocalModifications()) {
                contentState = SVNStatusType.MERGED;
            } else if (hasTextChanges()) {
                contentState = SVNStatusType.CHANGED;
            }

            if (hadPropRejects()) {
                propState = SVNStatusType.CONFLICTED;
            } else if (hasPropChanges()) {
                propState = SVNStatusType.CHANGED;
            }
        }

        SVNEvent event = SVNEventFactory.createSVNEvent(eventPath, SVNNodeKind.FILE, null, -1, contentState, propState, null, action, action, null, null);
        eventHandler.handleEvent(event, ISVNEventHandler.UNKNOWN);

        if (action == SVNEventAction.PATCH) {
            for (SvnHunkInfo hunkInfo : getHunkInfos()) {
                sendHunkNotification(hunkInfo, null, context);
            }
            final Map<String, SvnPropertiesPatchTarget> propTargets = getPropTargets();
            for (Map.Entry<String, SvnPropertiesPatchTarget> entry : propTargets.entrySet()) {
                SvnPropertiesPatchTarget propTarget = entry.getValue();

                List<SvnHunkInfo> hunks = propTarget.getHunkInfos();
                for (SvnHunkInfo hunkInfo : hunks) {
                    if (propTarget.getOperation() != SvnDiffCallback.OperationKind.Added &&
                            propTarget.getOperation() != SvnDiffCallback.OperationKind.Deleted) {
                        sendHunkNotification(hunkInfo, propTarget.getName(), context);
                    }
                }
            }
        }
        if (getMoveTargetAbsPath() != null) {
            event = SVNEventFactory.createSVNEvent(getAbsPath(), SVNNodeKind.FILE, null, -1, SVNEventAction.DELETE, SVNEventAction.DELETE, null, null);
            eventHandler.handleEvent(event, ISVNEventHandler.UNKNOWN);
        }
    }

    private void sendHunkNotification(SvnHunkInfo hunkInfo, String propName, SVNWCContext context) throws SVNException {
        SVNEventAction action;
        if (hunkInfo.isAlreadyApplied()) {
            action = SVNEventAction.PATCH_HUNK_ALREADY_APPLIED;
        } else if (hunkInfo.isRejected()) {
            action = SVNEventAction.PATCH_REJECTED_HUNK;
        } else {
            action = SVNEventAction.PATCH_APPLIED_HUNK;
        }

        SVNEvent event = SVNEventFactory.createSVNEvent(getAbsPath() != null ? getAbsPath() : getRelPath(), SVNNodeKind.UNKNOWN, null, -1, action, action, null, null);
        event.setInfo(hunkInfo);
        event.setPropertyName(propName);

        ISVNEventHandler eventHandler = context.getEventHandler();
        if (eventHandler != null) {
            eventHandler.handleEvent(event, ISVNEventHandler.UNKNOWN);
        }
    }

    private boolean hasLocalModifications() {
        return hasLocalModifications;
    }

    private boolean hadRejects() {
        return hadRejects;
    }

    public void setHadRejects(boolean hadRejects) {
        this.hadRejects = hadRejects;
    }

    private boolean hadPropRejects() {
        return hadPropRejects;
    }

    public void setHadPropRejects(boolean hadPropRejects) {
        this.hadPropRejects = hadPropRejects;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    public boolean isSymlink() {
        return symlink;
    }

    public void setAdded(boolean added) {
        this.added = added;
    }

    public void setReplaced(boolean replaced) {
        this.replaced = replaced;
    }

    public boolean isLocallyDeleted() {
        return locallyDeleted;
    }

    public SVNNodeKind getKindOnDisk() {
        return kindOnDisk;
    }

    public SVNNodeKind getDbKind() {
        return dbKind;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void setDbKind(SVNNodeKind dbKind) {
        this.dbKind = dbKind;
    }

    public void setKindOnDisk(SVNNodeKind kindOnDisk) {
        this.kindOnDisk = kindOnDisk;
    }

    public void setExisted(boolean existed) {
        this.existed = existed;
    }

    public void setCurrentLine(int currentLine) {
        this.currentLine = currentLine;
    }

    public void setHasLocalModifications(boolean hasLocalModifications) {
        this.hasLocalModifications = hasLocalModifications;
    }

    public void setExecutable(boolean executable) {
        this.executable = executable;
    }

    public void setAbsPath(File absPath) {
        this.absPath = absPath;
    }

    public void setRelPath(File relPath) {
        this.relPath = relPath;
    }

    public File getCanonPathFromPatchfile() {
        return canonPathFromPatchfile;
    }

    public void setCanonPathFromPatchfile(File canonPathFromPatchfile) {
        this.canonPathFromPatchfile = canonPathFromPatchfile;
    }

    public File getRelPath() {
        return relPath;
    }

    public void setLocallyDeleted(boolean locallyDeleted) {
        this.locallyDeleted = locallyDeleted;
    }

    public void setSymlink(boolean symlink) {
        this.symlink = symlink;
    }

    public boolean isExisted() {
        return existed;
    }

    public int getCurrentLine() {
        return currentLine;
    }

    public void setEolStr(String eolStr) {
        this.eolStr = eolStr;
    }

    public boolean isReplaced() {
        return replaced;
    }

    public boolean isSpecial() {
        return special;
    }

    public boolean isExecutable() {
        return executable;
    }

    public File getPatchedAbsPath() {
        return patchedAbsPath;
    }

    public void setPatchedAbsPath(File patchedAbsPath) {
        this.patchedAbsPath = patchedAbsPath;
    }

    public void setHasPropChanges(boolean hasPropChanges) {
        this.hasPropChanges = hasPropChanges;
    }

    public void setHasTextChanges(boolean hasTextChanges) {
        this.hasTextChanges = hasTextChanges;
    }

    public void setStream(SVNPatchFileStream stream) {
        this.stream = stream;
    }

    private static class RegularWriteCallback implements IWriteCallback {
        public void write(Object writeBaton, String s) throws SVNException {
            SVNPatchFileStream outputStream = (SVNPatchFileStream) writeBaton;
            try {
                outputStream.write(s);
            } catch (IOException e) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
        }
    }

    private static class SymlinkCallbacks implements IWriteCallback, IRealLineCallback, ISeekCallback, ITellCallback {

        private File workingCopyDirectory;
        private SVNWCContext context;

        public SymlinkCallbacks(File workingCopyDirectory, SVNWCContext context) {
            this.workingCopyDirectory = workingCopyDirectory;
            this.context = context;
        }

        public void write(Object writeBaton, String s) throws SVNException {
            if (!s.startsWith("link ")) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_WRITE_ERROR, "Invalid link representation");
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
            s = s.substring("link ".length());
            File targetAbsPath = (File) writeBaton;
            if (SVNFileType.getType(targetAbsPath) == SVNFileType.FILE) {
                SVNFileUtil.deleteFile(targetAbsPath);
            }
            SVNFileUtil.createSymlink(targetAbsPath, s);
        }

        public String readLine(Object baton, String[] eolStr, boolean[] eof) throws SVNException {
            if (eof != null) {
                eof[0] = true;
            }
            if (eolStr != null) {
                eolStr[0] = null;
            }
            SymlinkReadBaton symlinkReadBaton = (SymlinkReadBaton) baton;
            if (symlinkReadBaton.isAtEof()) {
                return null;
            } else {
                String symlinkName = SVNFileUtil.getSymlinkName(symlinkReadBaton.getAbsPath());
                String symlinkContent = "link " + symlinkName;
                return symlinkContent;
            }
        }

        public void seek(Object readBaton, long offset) {
            SymlinkReadBaton symlinkReadBaton = (SymlinkReadBaton) readBaton;
            symlinkReadBaton.atEof = offset != 0;
        }

        public long tell(Object readBaton) {
            SymlinkReadBaton symlinkReadBaton = (SymlinkReadBaton) readBaton;
            return symlinkReadBaton.isAtEof() ? 1 : 0;
        }
    }

    private static class SymlinkReadBaton {
        private final File absPath;
        private boolean atEof;

        public SymlinkReadBaton(File absPath) {
            this.absPath = absPath;
        }

        private File getAbsPath() {
            return absPath;
        }

        private boolean isAtEof() {
            return atEof;
        }
    }

    private static class RegularCallbacks implements IRealLineCallback, ISeekCallback, ITellCallback{

        public String readLine(Object baton, String[] eolStr, boolean[] eof) throws SVNException {
            try {
                SVNPatchFileStream inputStream = (SVNPatchFileStream) baton;

                StringBuffer lineBuffer = new StringBuffer();
                StringBuffer eolStrBuffer = new StringBuffer();
                boolean isEof = inputStream.readLineWithEol(lineBuffer, eolStrBuffer);
                if (eof != null) {
                    eof[0] = isEof;
                }
                if (eolStr != null) {
                    eolStr[0] = eolStrBuffer.length() == 0 ? null : eolStrBuffer.toString();
                }

                return lineBuffer.toString();
            } catch (IOException e) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
            return null;
        }

        @Override
        public void seek(Object baton, long offset) throws SVNException {
            SVNPatchFileStream inputStream = (SVNPatchFileStream) baton;
            try {
                inputStream.setSeekPosition(offset);
            } catch (IOException e) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
        }

        public long tell(Object baton) throws SVNException {
            SVNPatchFileStream inputStream = (SVNPatchFileStream) baton;
            try {
                return inputStream.getSeekPosition();
            } catch (IOException e) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
            return -1;
        }
    }
}
