package org.tmatesoft.svn.core.internal.wc2.patch;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.tmatesoft.svn.core.ISVNCanceller;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.SVNPropertiesManager;
import org.tmatesoft.svn.core.internal.wc.patch.SVNPatchFileStream;
import org.tmatesoft.svn.core.internal.wc.patch.SVNPatchTarget;
import org.tmatesoft.svn.core.internal.wc.patch.SVNPatchTargetInfo;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnDiffCallback;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc2.ISvnPatchHandler;
import org.tmatesoft.svn.util.SVNLogType;

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
    private boolean obstructed;
    private SVNNodeKind kindOnDisk;
    private SVNNodeKind dbKind;
    private SvnDiffCallback.OperationKind operation;
    private boolean gitSymlinkFormat;
    private File originalContentFile;

    private boolean hasLocalModifications;
    private boolean hadRejects;
    private boolean hadPropRejects;
    private boolean hadAlreadyApplied;
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

    public void setFiltered(boolean filtered) {
        this.filtered = filtered;
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

    @Deprecated
    public static SvnPatchTarget applyPatch(SvnPatch patch, File workingCopyDirectory, int stripCount, SVNWCContext context, boolean ignoreWhitespace, boolean removeTempFiles, ISvnPatchHandler patchHandler) throws SVNException, IOException {
        final SvnWcPatchContext patchContext = new SvnWcPatchContext(context);
        return applyPatch(patch, workingCopyDirectory, stripCount, new ArrayList<SVNPatchTargetInfo>(), patchContext, ignoreWhitespace, removeTempFiles, patchHandler);
    }

    public static SvnPatchTarget applyPatch(SvnPatch patch, File workingCopyDirectory, int stripCount, List<SVNPatchTargetInfo> targetInfos, ISvnPatchContext patchContext, boolean ignoreWhitespace, boolean removeTempFiles, ISvnPatchHandler patchHandler) throws SVNException, IOException {
        SvnPatchTarget target = initPatchTarget(patch, workingCopyDirectory, stripCount, removeTempFiles, targetInfos, patchContext);
        if (target.isSkipped()) {
            return target;
        }
        if (patchHandler != null) {
            final boolean filtered = patchHandler.singlePatch(target.getCanonPathFromPatchfile(), target.getPatchedAbsPath(), target.getRejectAbsPath());
            if (filtered) {
                target.setFiltered(true);
                return target;
            }
        }
        List<SvnDiffHunk> hunks = patch.getHunks();
        if (hunks.size() > 0) {
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
                    target.setHadAlreadyApplied(true);
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
        } else if (patch.getBinaryPatch() != null) {
            InputStream originalFileInputStream;
            if (target.getOriginalContentFile() != null) {
                originalFileInputStream = new BufferedInputStream(new FileInputStream(target.getOriginalContentFile()));
            } else {
                originalFileInputStream = SVNFileUtil.DUMMY_IN;
            }
            boolean same;
            InputStream binaryDiffOriginalStream = null;
            try {
                binaryDiffOriginalStream = patch.getBinaryPatch().getBinaryDiffOriginalStream();
                same = areStreamsSame(originalFileInputStream, binaryDiffOriginalStream);
            } finally {
                SVNFileUtil.closeFile(binaryDiffOriginalStream);
                SVNFileUtil.closeFile(originalFileInputStream);
            }

            if (same) {
                target.setHasTextChanges(true);
            } else {
                if (target.getOriginalContentFile() != null) {
                    originalFileInputStream = new BufferedInputStream(new FileInputStream(target.getOriginalContentFile()));
                } else {
                    originalFileInputStream = SVNFileUtil.DUMMY_IN;
                }
                InputStream binaryDiffResultStream;
                try {
                    binaryDiffResultStream = patch.getBinaryPatch().getBinaryDiffResultStream();
                    same = areStreamsSame(originalFileInputStream, binaryDiffResultStream);
                    if (same) {
                        target.setHadAlreadyApplied(true);
                    }
                } finally {
                    SVNFileUtil.closeFile(binaryDiffOriginalStream);
                    SVNFileUtil.closeFile(originalFileInputStream);
                }
            }

            if (same) {
                InputStream binaryDiffResultStream = null;
                FileOutputStream fileOutputStream = null;
                try {
                    binaryDiffResultStream = patch.getBinaryPatch().getBinaryDiffResultStream();
                    fileOutputStream = new FileOutputStream(target.getPatchedAbsPath());
                    copyStream(binaryDiffResultStream, fileOutputStream);
                    fileOutputStream.flush();
                } finally {
                    SVNFileUtil.closeFile(binaryDiffResultStream);
                    SVNFileUtil.closeFile(fileOutputStream);
                }
            } else {
                target.setSkipped(true);
            }
        } else if (target.getMoveTargetAbsPath() != null) {
            if (target.getKindOnDisk() == SVNNodeKind.FILE) {
                copyLinesToTarget(target, 0);
                if (!target.isEof()) {
                    target.setSkipped(true);
                }
            }
        }

        if (target.hadRejects() || target.isLocallyDeleted()) {
            target.setDeleted(false);
        }

        if (target.isAdded() &&
                !(target.isLocallyDeleted() || target.getDbKind() == SVNNodeKind.NONE)) {
            target.setAdded(false);
        }

        target.setSpecial(target.isSymlink());

        for (Map.Entry<String, SvnPropertiesPatch> entry : patch.getPropPatches().entrySet()) {
            final String propName = entry.getKey();
            final SvnPropertiesPatch propPatch = entry.getValue();

            if (SVNProperty.SPECIAL.equals(propName)) {
                target.setSpecial(propPatch.getOperation() != SvnDiffCallback.OperationKind.Deleted);
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

        if (patch.getNewExecutableBit() != null &&
                patch.getNewExecutableBit() != patch.getOldExecutableBit() &&
                (target.getPropTargets().get(SVNProperty.EXECUTABLE) != null) &&
                (patch.getPropPatches().get(SVNProperty.EXECUTABLE) == null)) {
            final SvnPropertiesPatchTarget propTarget = target.getPropTargets().get(SVNProperty.EXECUTABLE);
            final SvnDiffHunk hunk;
            if (patch.getNewExecutableBit() == Boolean.TRUE) {
                hunk = createAddsSingleLine(patch,
                        SVNPropertyValue.getPropertyAsString(SVNProperty.getValueOfBooleanProperty(SVNProperty.EXECUTABLE)),
                        patchContext, workingCopyDirectory);
            } else {
                hunk = createDeletesSingleLine(patch,
                        SVNPropertyValue.getPropertyAsString(SVNProperty.getValueOfBooleanProperty(SVNProperty.EXECUTABLE)),
                        patchContext, workingCopyDirectory);
            }
            final SvnHunkInfo hunkInfo = target.getHunkInfo(hunk, propTarget, 0, ignoreWhitespace, true);
            propTarget.addHunkInfo(hunkInfo);
        }

        if (patch.getNewSymlinkBit() != null &&
                patch.getNewSymlinkBit() != patch.getOldSymlinkBit() &&
                (target.getPropTargets().get(SVNProperty.SPECIAL) != null) &&
                (patch.getPropPatches().get(SVNProperty.SPECIAL) == null)) {
            final SvnPropertiesPatchTarget propTarget = target.getPropTargets().get(SVNProperty.SPECIAL);
            final SvnDiffHunk hunk;
            if (patch.getNewSymlinkBit() == Boolean.TRUE) {
                hunk = createAddsSingleLine(patch,
                        SVNPropertyValue.getPropertyAsString(SVNProperty.getValueOfBooleanProperty(SVNProperty.SPECIAL)),
                        patchContext, workingCopyDirectory);
                target.setSpecial(true);
            } else {
                hunk = createDeletesSingleLine(patch,
                        SVNPropertyValue.getPropertyAsString(SVNProperty.getValueOfBooleanProperty(SVNProperty.SPECIAL)),
                        patchContext, workingCopyDirectory);
                target.setSpecial(false);
            }
            final SvnHunkInfo hunkInfo = target.getHunkInfo(hunk, propTarget, 0, ignoreWhitespace, true);
            propTarget.addHunkInfo(hunkInfo);
        }

        if (target.isDeleted() ||
                (!target.isAdded() &&
                        (target.isLocallyDeleted() || target.getDbKind() == SVNNodeKind.NONE))) {
            for (Map.Entry<String, SvnPropertiesPatchTarget> entry : target.getPropTargets().entrySet()) {
                final SvnPropertiesPatchTarget propTarget = entry.getValue();

                if (propTarget.getOperation() == SvnDiffCallback.OperationKind.Deleted) {
                    continue;
                }

                for (SvnHunkInfo hunkInfo : propTarget.getHunkInfos()) {
                    if (hunkInfo.isAlreadyApplied() || hunkInfo.isRejected()) {
                        continue;
                    } else {
                        hunkInfo.setRejected(true);
                        propTarget.setSkipped(true);

                        if (!target.isDeleted() && !target.isAdded()) {
                            target.setSkipped(true);
                        }
                    }
                }
            }
        }

        final SortedMap<String, SvnPropertiesPatchTarget> propTargets = new TreeMap<String, SvnPropertiesPatchTarget>(target.getPropTargets());
        for (Map.Entry<String, SvnPropertiesPatchTarget> entry : propTargets.entrySet()) {
            SvnPropertiesPatchTarget propTarget = entry.getValue();

            boolean appliedOne = false;

            final List<SvnHunkInfo> hunkInfos = propTarget.getHunkInfos();

            for (SvnHunkInfo hunkInfo : hunkInfos) {
                if (hunkInfo.isAlreadyApplied()) {
                    target.setHadAlreadyApplied(true);
                    continue;
                } else if (hunkInfo.isRejected()) {
                    rejectHunk(target, hunkInfo.getHunk(), propTarget.getName());
                } else {
                    applyHunk(target, propTarget, hunkInfo, propTarget.getName());
                    appliedOne = true;
                }
            }
            if (!appliedOne) {
                propTarget.setSkipped(true);
            }

            if (appliedOne && propTarget.isExisted()) {
                copyLinesToTarget(propTarget, 0);
                if (!propTarget.isEof()) {
                    propTarget.setSkipped(true);
                }
            }
        }

        if (!target.isSymlink()) {
            if (target.getKindOnDisk() == SVNNodeKind.FILE) {
                target.getStream().close();
            }

            target.getPatchedStream().close();
        }
        return target;
    }

    private static void copyStream(InputStream sourceStream, FileOutputStream targetStream) throws IOException {
        final byte[] buffer = new byte[8192];
        while (true) {
            final int bytesRead = sourceStream.read(buffer);
            if (bytesRead < 0) {
                break;
            }
            targetStream.write(buffer, 0, bytesRead);
        }
    }

    private static boolean areStreamsSame(InputStream stream1, InputStream stream2) throws IOException {
        final int bufferLength = 8192;
        final byte[] buffer1 = new byte[bufferLength];
        final byte[] buffer2 = new byte[bufferLength];
        while (true) {
            final int bytesRead1 = SvnPatch.readFully(stream1, buffer1, 0, bufferLength);
            final int bytesRead2 = SvnPatch.readFully(stream2, buffer2, 0, bufferLength);
            if (bytesRead1 < 0) {
                return bytesRead2 < 0;
            }
            if (bytesRead1 != bytesRead2) {
                return false;
            }
            for (int i = 0; i < bytesRead1; i++) {
                if (buffer1[i] != buffer2[i]) {
                    return false;
                }
            }
        }
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
                if (target.getEolStr() != null) {
                    targetLine = targetLine + target.getEolStr();
                }
            }
            target.getWriteCallback().write(target.getWriteBaton(), targetLine);
        }
    }

    private static SvnPatchTarget initPatchTarget(SvnPatch patch,
                                                  File workingCopyDirectory,
                                                  int stripCount,
                                                  boolean removeTempFiles,
                                                  List<SVNPatchTargetInfo> targetsInfo,
                                                  ISvnPatchContext patchContext) throws SVNException, IOException {
        boolean hasTextChanges = false;
        boolean followMoves;

        hasTextChanges = (patch.getHunks() != null && patch.getHunks().size() > 0) || patch.getBinaryPatch() != null;

        SvnPatchTarget target = new SvnPatchTarget();//empty lists are created in the constructor
        target.setCurrentLine(1);
        target.setEolStyle(SVNWCContext.SVNEolStyle.None);

        target.setDbKind(SVNNodeKind.NONE);
        target.setKindOnDisk(SVNNodeKind.NONE);

        target.setOperation(patch.getOperation());

        if (patch.getOperation() == SvnDiffCallback.OperationKind.Added ||
                patch.getOperation() == SvnDiffCallback.OperationKind.Moved) {
            followMoves = false;
        } else if (patch.getOperation() == SvnDiffCallback.OperationKind.Unchanged &&
                patch.getHunks() != null && patch.getHunks().size() == 1) {
            final SvnDiffHunk hunk = patch.getHunks().get(0);
            followMoves = hunk.getDirectedOriginalStart() != 0;
        } else {
            followMoves = true;
        }

        target.resolveTargetPath(chooseTargetFilename(patch), workingCopyDirectory,
                stripCount, hasTextChanges, followMoves, targetsInfo, patchContext);

        if (!target.isSkipped()) {
            if (patch.getOldSymlinkBit() == Boolean.TRUE ||
                    patch.getNewSymlinkBit() == Boolean.TRUE) {
                target.setGitSymlinkFormat(true);
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
                        return target;
                    }
                } else {
                    moveTargetRelPath = moveTargetPath;
                }

                boolean underRoot = isUnderRoot(workingCopyDirectory, moveTargetRelPath);
                target.setMoveTargetAbsPath(SVNFileUtil.createFilePath(workingCopyDirectory, moveTargetRelPath));
                if (!underRoot) {
                    target.setSkipped(true);
                    target.setAbsPath(null);
                    return target;
                }
                final SVNFileType typeOnDisk = patchContext.getKindOnDisk(target.getMoveTargetAbsPath());
                final SVNNodeKind kindOnDisk = SVNFileType.getNodeKind(typeOnDisk);
                final SVNNodeKind wcKind = patchContext.readKind(target.getMoveTargetAbsPath(), true, false);

                if (wcKind == SVNNodeKind.FILE || wcKind == SVNNodeKind.DIR) {
                    File movedFromAbsPath;
                    try {
                        movedFromAbsPath = patchContext.wasNodeMovedHere(target.getMoveTargetAbsPath());
                    } catch (SVNException e) {
                        if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                            movedFromAbsPath = null;
                        } else {
                            throw e;
                        }
                    }

                    if (movedFromAbsPath != null && movedFromAbsPath.equals(target.getAbsPath())) {
                        target.setAbsPath(target.getMoveTargetAbsPath());
                        target.setMoveTargetAbsPath(null);
                        target.setOperation(SvnDiffCallback.OperationKind.Modified);
                        target.setLocallyDeleted(false);
                        target.setDbKind(wcKind);
                        target.setKindOnDisk(kindOnDisk);
                        target.setSpecial(typeOnDisk == SVNFileType.SYMLINK);
                        target.setHadAlreadyApplied(true);
                    } else {
                        target.setSkipped(true);
                        target.setMoveTargetAbsPath(null);
                        return target;
                    }
                } else if (kindOnDisk == SVNNodeKind.NONE || targetIsAdded(targetsInfo, target.getMoveTargetAbsPath())) {
                    target.setSkipped(true);
                    target.setMoveTargetAbsPath(null);
                    return target;
                }
            }

            if (target.isSymlink()) {
                target.setExisted(true);

                target.setReadBaton(new SymlinkReadBaton(target.getAbsPath()));

                final SymlinkCallbacks symlinkCallbacks = new SymlinkCallbacks(workingCopyDirectory, patchContext);
                target.setReadLineCallback(symlinkCallbacks);
                target.setTellCallback(symlinkCallbacks);
                target.setSeekCallback(symlinkCallbacks);
            } else if (target.getKindOnDisk() == SVNNodeKind.FILE) {
                target.setHasLocalModifications(patchContext.isTextModified(target.getAbsPath(), false));
                target.setExecutable(patchContext.isExecutable(target.getAbsPath()));

                final Map<String, byte[]> keywords = new HashMap<String, byte[]>();
                SVNWCContext.SVNEolStyle[] eolStyle = (SVNWCContext.SVNEolStyle[]) new SVNWCContext.SVNEolStyle[1];
                String[] eolStr = new String[1];

                if (target.getKeywords() != null) {
                    keywords.putAll(target.getKeywords());
                }
                eolStyle[0] = target.getEolStyle();
                eolStr[0] = target.getEolStr();
                obtainEolAndKeywordsForFile(keywords, eolStyle, eolStr, patchContext, target.getAbsPath());

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

            if (target.isSymlink()) {
                File uniqueFile = patchContext.createTempFile(workingCopyDirectory);
                target.setPatchedAbsPath(uniqueFile);
                target.setWriteBaton(uniqueFile);
                target.setWriteCallback(new SymlinkCallbacks(workingCopyDirectory, patchContext));
                target.setOriginalContentFile(null);
            } else if (target.getKindOnDisk() == SVNNodeKind.FILE) {
                File uniqueFile = patchContext.createTempFile(workingCopyDirectory);
                target.setPatchedAbsPath(uniqueFile);
                target.setPatchedStream(SVNPatchFileStream.openForWrite(uniqueFile));
                target.setWriteBaton(target.getPatchedStream());
                target.setWriteCallback(new RegularWriteCallback());
                target.setOriginalContentFile(target.getAbsPath());
                target.setExecutable(SVNFileUtil.isExecutable(target.getAbsPath()));
            } else {
                File uniqueFile = patchContext.createTempFile(workingCopyDirectory);
                target.setPatchedAbsPath(uniqueFile);
                target.setPatchedStream(SVNPatchFileStream.openForWrite(uniqueFile));
                target.setWriteBaton(target.getPatchedStream());
                target.setWriteCallback(new RegularWriteCallback());
            }

            target.setRejectAbsPath(patchContext.createTempFile(workingCopyDirectory));
            target.setRejectStream(SVNPatchFileStream.openForWrite(target.getRejectAbsPath()));

            if (!target.isSkipped()) {
                final Map<String, SvnPropertiesPatch> propPatches = patch.getPropPatches();
                for (Map.Entry<String, SvnPropertiesPatch> entry : propPatches.entrySet()) {
                    final String propName = entry.getKey();
                    final SvnPropertiesPatch propPatch = entry.getValue();

                    SvnPropertiesPatchTarget propTarget = SvnPropertiesPatchTarget.initPropTarget(propName, propPatch.getOperation(), patchContext, target.getAbsPath());
                    target.putPropTarget(propName, propTarget);
                }

                if (patch.getNewExecutableBit() != null &&
                        patch.getNewExecutableBit() != patch.getOldExecutableBit()) {
                    final SvnDiffCallback.OperationKind operation;
                    if (patch.getNewExecutableBit() == Boolean.TRUE) {
                        operation = SvnDiffCallback.OperationKind.Added;
                    } else if (patch.getNewExecutableBit() == Boolean.FALSE) {
                        if (patch.getOldExecutableBit() == Boolean.TRUE) {
                            operation = SvnDiffCallback.OperationKind.Deleted;
                        } else {
                            operation = SvnDiffCallback.OperationKind.Unchanged;
                        }
                    } else {
                        operation = SvnDiffCallback.OperationKind.Unchanged;
                    }

                    if (operation != SvnDiffCallback.OperationKind.Unchanged) {
                        SvnPropertiesPatchTarget propTarget = target.getPropTargets().get(SVNProperty.EXECUTABLE);
                        if (propTarget != null && operation != propTarget.getOperation()) {
                            final SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.INVALID_INPUT,
                                    "Invalid patch: specifies " +
                                            "contradicting mode changes and " +
                                            "{0} changes (for ''{1}'')",
                                    SVNProperty.EXECUTABLE, target.getAbsPath());
                            SVNErrorManager.error(errorMessage, SVNLogType.WC);
                        } else if (propTarget == null) {
                            propTarget = SvnPropertiesPatchTarget.initPropTarget(
                                    SVNProperty.EXECUTABLE, operation, patchContext, target.getAbsPath());
                            target.putPropTarget(SVNProperty.EXECUTABLE, propTarget);
                        }
                    }
                }

                if (patch.getNewSymlinkBit() != null &&
                        patch.getNewSymlinkBit() != patch.getOldSymlinkBit()) {
                    final SvnDiffCallback.OperationKind operation;

                    if (patch.getNewSymlinkBit() == Boolean.TRUE) {
                        operation = SvnDiffCallback.OperationKind.Added;
                    } else if (patch.getNewSymlinkBit() == Boolean.FALSE) {
                        if (patch.getOldSymlinkBit() == Boolean.TRUE) {
                            operation = SvnDiffCallback.OperationKind.Deleted;
                        } else {
                            operation = SvnDiffCallback.OperationKind.Unchanged;
                        }
                    } else {
                        operation = SvnDiffCallback.OperationKind.Unchanged;
                    }

                    if (operation != SvnDiffCallback.OperationKind.Unchanged) {
                        SvnPropertiesPatchTarget propTarget = target.getPropTargets().get(SVNProperty.EXECUTABLE);
                        if (propTarget != null && operation != propTarget.getOperation()) {
                            final SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.INVALID_INPUT,
                                    "Invalid patch: specifies " +
                                            "contradicting mode changes and " +
                                            "{0} changes (for ''{1}'')",
                                    SVNProperty.SPECIAL, target.getAbsPath());
                            SVNErrorManager.error(errorMessage, SVNLogType.WC);
                        } else if (propTarget == null) {
                            propTarget = SvnPropertiesPatchTarget.initPropTarget(
                                    SVNProperty.SPECIAL, operation, patchContext, target.getAbsPath());
                            target.putPropTarget(SVNProperty.SPECIAL, propTarget);
                        }
                    }
                }
            }
        }

        if ((target.isLocallyDeleted() || target.getDbKind() == SVNNodeKind.NONE) &&
                !target.isAdded() &&
                target.getOperation() == SvnDiffCallback.OperationKind.Unchanged) {
            boolean maybeAdd = false;

            if (patch.getHunks() != null && patch.getHunks().size() == 1) {
                final SvnDiffHunk hunk = patch.getHunks().get(0);
                if (hunk.getDirectedOriginalStart() == 0) {
                    maybeAdd = true;
                }
            } else if (patch.getPropPatches() != null && patch.getPropPatches().size() > 0) {
                boolean allAdd = true;
                final Map<String, SvnPropertiesPatch> propPatches = patch.getPropPatches();
                for (Map.Entry<String, SvnPropertiesPatch> entry : propPatches.entrySet()) {
                    final SvnPropertiesPatch propPatch = entry.getValue();
                    if (propPatch.getOperation() != SvnDiffCallback.OperationKind.Added) {
                        allAdd = false;
                        break;
                    }
                }
                maybeAdd = allAdd;
            }

            if (maybeAdd) {
                target.setAdded(true);
            }
        } else if (!target.isDeleted() && !target.isAdded() && target.getOperation() == SvnDiffCallback.OperationKind.Unchanged) {
            boolean maybeDelete = false;

            if (patch.getHunks() != null && patch.getHunks().size() == 1) {
                final SvnDiffHunk hunk = patch.getHunks().get(0);
                if (hunk.getDirectedModifiedStart() == 0) {
                    maybeDelete = true;
                }
            }

            if (maybeDelete) {
                target.setDeleted(true);
            }
        }

        if (target.getRejectStream() != null) {
            File leftSrc = target.getCanonPathFromPatchfile();
            File rightSrc = target.getCanonPathFromPatchfile();

            if (target.isAdded()) {
                leftSrc = SVNFileUtil.createFilePath("/dev/null");
            }
            if (target.isDeleted()) {
                rightSrc = SVNFileUtil.createFilePath("/dev/null");
            }
            target.getRejectStream().write(
                    "--- " + leftSrc + "\n" +
                            "+++ " + rightSrc + "\n");
        }
        return target;
    }

    protected static boolean targetIsAdded(List<SVNPatchTargetInfo> targetsInfo, File localAbsPath) {
        for (int i = targetsInfo.size() - 1; i >= 0; i--) {
            final SVNPatchTargetInfo targetInfo = targetsInfo.get(i);

            final String info = SVNPathUtil.getPathAsChild(SVNFileUtil.getFilePath(targetInfo.getLocalAbsPath()),
                    SVNFileUtil.getFilePath(localAbsPath));

            if (info != null && info.length() == 0) {
                return targetInfo.isAdded();
            } else if (info != null) {
                return false;
            }
        }
        return false;
    }

    protected static boolean targetIsDeleted(List<SVNPatchTargetInfo> targetsInfo, File localAbsPath) {
        for (int i = targetsInfo.size() - 1; i >= 0; i--) {
            final SVNPatchTargetInfo targetInfo = targetsInfo.get(i);

            final String info = SVNPathUtil.getPathAsChild(SVNFileUtil.getFilePath(targetInfo.getLocalAbsPath()),
                    SVNFileUtil.getFilePath(localAbsPath));

            if (info != null) {
                return targetInfo.isDeleted();
            }
        }
        return false;
    }

    private static void obtainEolAndKeywordsForFile(Map<String, byte[]> keywords,
                                             SVNWCContext.SVNEolStyle[] eolStyle,
                                             String[] eolStr,
                                             ISvnPatchContext patchContext, File localAbsPath) throws SVNException {
        final SVNProperties actualProps = patchContext.getActualProps(localAbsPath);
        SVNPropertyValue keywordsVal = actualProps.getSVNPropertyValue(SVNProperty.KEYWORDS);
        if (keywordsVal != null) {
            if (keywords != null) {
                keywords.putAll(patchContext.computeKeywords(localAbsPath, keywordsVal));
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

    private void resolveTargetPath(File pathFromPatchFile,
                                   File workingCopyDirectory,
                                   int stripCount,
                                   boolean hasTextChanges,
                                   boolean followMoves,
                                   List<SVNPatchTargetInfo> targetsInfo,
                                   ISvnPatchContext patchContext) throws SVNException, IOException {
        final File canonPathFromPatchfile = pathFromPatchFile;
        setCanonPathFromPatchfile(canonPathFromPatchfile);

        if (hasTextChanges && SVNFileUtil.getFilePath(canonPathFromPatchfile).length() == 0) {
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

        if (targetIsDeleted(targetsInfo, getAbsPath())) {
            setLocallyDeleted(true);
            setDbKind(SVNNodeKind.NONE);
            return;
        }

        patchContext.resolvePatchTargetStatus(this, workingCopyDirectory, followMoves, targetsInfo);
    }

    private static boolean isUnderRoot(File workingCopyDirectory, File relPath) throws SVNException {
        File fullPath = SVNFileUtil.createFilePath(workingCopyDirectory, relPath);
        try {
            String workingCopyDirectoryPath = SVNFileUtil.getFilePath(workingCopyDirectory.getCanonicalFile());
            String canonicalFullPath = SVNFileUtil.getFilePath(fullPath.getCanonicalFile());
            return canonicalFullPath.equals(workingCopyDirectoryPath) || SVNPathUtil.isAncestor(workingCopyDirectoryPath, canonicalFullPath);
        } catch (IOException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }
        return false;
    }

    private static File chooseTargetFilename(SvnPatch patch) {
        if (patch.getOldFileName() == SvnPatch.DEV_NULL) {
            return patch.getNewFileName();
        }
        if (patch.getNewFileName() == SvnPatch.DEV_NULL) {
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

    @Deprecated
    public void installPatchedTarget(File workingCopyDirectory, boolean dryRun, SVNWCContext context) throws SVNException {
        final SvnWcPatchContext patchContext = new SvnWcPatchContext(context);
        final ArrayList<SVNPatchTargetInfo> targetInfos = new ArrayList<SVNPatchTargetInfo>();
        installPatchedTarget(workingCopyDirectory, dryRun, patchContext, targetInfos);
    }

    public void installPatchedTarget(File workingCopyDirectory, boolean dryRun, ISvnPatchContext patchContext, List<SVNPatchTargetInfo> targetInfos) throws SVNException {
        if (isDeleted()) {
            if (!dryRun) {
                patchContext.delete(getAbsPath());
            }
        } else {
            if (isAdded()) {
                File parentAbsPath = SVNFileUtil.getParentFile(getAbsPath());

                SVNNodeKind parentDbKind = patchContext.readKind(parentAbsPath, false, false);

                if (parentDbKind == SVNNodeKind.DIR || parentDbKind == SVNNodeKind.FILE) {
                    if (parentDbKind != SVNNodeKind.DIR) {
                        setSkipped(true);
                    } else {
                        if (patchContext.getKindOnDisk(parentAbsPath) != SVNFileType.DIRECTORY) {
                            setSkipped(true);
                        }
                    }
                } else {
                    createMissingParents(workingCopyDirectory, patchContext, dryRun, targetInfos);
                }
            } else {
                SVNNodeKind wcKind = patchContext.readKind(getAbsPath(), false, false);

                if (getKindOnDisk() == SVNNodeKind.NONE || wcKind != getKindOnDisk()) {
                    setSkipped(true);
                    if (wcKind != getKindOnDisk()) {
                        setObstructed(true);
                    }
                }
            }

            if (!dryRun && !isSkipped()) {
                if (isSpecial()) {
                    //setPatchedStream(SVNFileUtil.openFileForReading(getPatchedAbsPath()));
                    String symlinkTarget;
                    if (patchContext.getKindOnDisk(getPatchedAbsPath()) == SVNFileType.FILE) {
                        symlinkTarget = SVNFileUtil.readFile(getPatchedAbsPath());

                        assert symlinkTarget != null;
                        if (!gitSymlinkFormat) {
                            assert symlinkTarget.startsWith("link ");
                            symlinkTarget = symlinkTarget.substring("link ".length());
                        }
                        patchContext.writeSymlinkContent(getAbsPath(), symlinkTarget);
                    } else {
                        assert patchContext.getKindOnDisk(getPatchedAbsPath()) == SVNFileType.SYMLINK;
                        patchContext.copySymlink(getPatchedAbsPath(), getAbsPath());
                    }
                } else {
                    //TODO: a special method for special files? atomicity?
                    File dst = getMoveTargetAbsPath() != null ? getMoveTargetAbsPath() : getAbsPath();
                    if (SVNFileType.getType(getPatchedAbsPath()) == SVNFileType.SYMLINK) {
                        SVNFileUtil.deleteFile(dst);
                        patchContext.copySymlink(getPatchedAbsPath(), dst);
                    } else {
                        boolean repairEol = getEolStyle() == SVNWCContext.SVNEolStyle.Fixed || getEolStyle() == SVNWCContext.SVNEolStyle.Native;
                        patchContext.translate(getPatchedAbsPath(), dst, null, getEolStr() == null ? null : getEolStr().getBytes(), getKeywords(), false, true);
                    }
                }

                if (isAdded()) {
                    patchContext.add(getAbsPath());
                }

                patchContext.setExecutable(getMoveTargetAbsPath() != null ? getMoveTargetAbsPath() : getAbsPath(), isExecutable());

                if (getMoveTargetAbsPath() != null) {
                    patchContext.move(getAbsPath(), getMoveTargetAbsPath());
                    patchContext.delete(getAbsPath());
                }
            }
        }
    }

    private void createMissingParents(File workingCopyDirectory,  ISvnPatchContext patchContext, boolean dryRun, List<SVNPatchTargetInfo> targetInfos) throws SVNException {
        File localAbsPath = workingCopyDirectory;
        File relPath = getRelPath();
        String relPathString = SVNFileUtil.getFilePath(relPath);
        String[] components = relPathString.split("/");
        int presentComponents = 0;

        for (String component : components) {
            localAbsPath = SVNFileUtil.createFilePath(localAbsPath, component);
            SVNNodeKind wcKind = patchContext.readKind(localAbsPath, false, true);

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

                if (targetIsAdded(targetInfos, localAbsPath)) {
                    continue;
                }
                final SVNPatchTargetInfo targetInfo = new SVNPatchTargetInfo(localAbsPath, true, false);
                targetInfos.add(targetInfo);

                if (dryRun) {
                    ISVNEventHandler eventHandler = patchContext.getEventHandler();
                    if (eventHandler != null) {
                        SVNEvent event = SVNEventFactory.createSVNEvent(localAbsPath, SVNNodeKind.DIR, null, -1, SVNEventAction.ADD, SVNEventAction.ADD, null, null);
                        eventHandler.handleEvent(event, ISVNEventHandler.UNKNOWN);
                    }
                } else {
                    ISVNCanceller canceller = patchContext.getEventHandler();
                    if (canceller != null) {
                        canceller.checkCancelled();
                    }
                    patchContext.add(localAbsPath);
                }
            }
        }
    }

    @Deprecated
    public void installPatchedPropTarget(boolean dryRun, SVNWCContext context) throws SVNException {
        final SvnWcPatchContext patchContext = new SvnWcPatchContext(context);
        installPatchedPropTarget(dryRun, patchContext);
    }
    public void installPatchedPropTarget(boolean dryRun, ISvnPatchContext patchContext) throws SVNException {
        final Map<String, SvnPropertiesPatchTarget> propTargets = getPropTargets();
        for (Map.Entry<String, SvnPropertiesPatchTarget> entry : propTargets.entrySet()) {
            SvnPropertiesPatchTarget propTarget = entry.getValue();
            ISVNCanceller canceller = patchContext.getEventHandler();
            if (canceller != null) {
                canceller.checkCancelled();
            }

            if (propTarget.isSkipped()) {
                continue;
            }

            if (propTarget.getOperation() == SvnDiffCallback.OperationKind.Deleted) {
                if (!dryRun) {
                    patchContext.setProperty(getAbsPath(), propTarget.getName(), null);
                }
                continue;
            }

            if (!hasTextChanges() && getKindOnDisk() == SVNNodeKind.NONE && !isAdded()) {
                if (!dryRun) {
                    SVNFileUtil.createEmptyFile(absPath);
                    patchContext.add(getAbsPath());
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
                    patchContext.setProperty(getAbsPath(), propTarget.getName(), propVal);
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

    @Deprecated
    public void sendPatchNotification(SVNWCContext context) throws SVNException {
        sendPatchNotification(new SvnWcPatchContext(context));
    }

    public void sendPatchNotification(ISvnPatchContext patchContext) throws SVNException {
        ISVNEventHandler eventHandler = patchContext.getEventHandler();
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
            if (isObstructed()) {
                contentState = SVNStatusType.OBSTRUCTED;
            } else if (getDbKind() == SVNNodeKind.NONE || getDbKind() == SVNNodeKind.UNKNOWN) {
                contentState = SVNStatusType.MISSING;
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
            } else if (hadAlreadyApplied()) {
                contentState =SVNStatusType.MERGED;
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
                sendHunkNotification(hunkInfo, null, patchContext);
            }
            final Map<String, SvnPropertiesPatchTarget> propTargets = getPropTargets();
            for (Map.Entry<String, SvnPropertiesPatchTarget> entry : propTargets.entrySet()) {
                SvnPropertiesPatchTarget propTarget = entry.getValue();

                List<SvnHunkInfo> hunks = propTarget.getHunkInfos();
                for (SvnHunkInfo hunkInfo : hunks) {
                    if (propTarget.getOperation() != SvnDiffCallback.OperationKind.Added &&
                            propTarget.getOperation() != SvnDiffCallback.OperationKind.Deleted) {
                        sendHunkNotification(hunkInfo, propTarget.getName(), patchContext);
                    }
                }
            }
        }
        if (getMoveTargetAbsPath() != null) {
            event = SVNEventFactory.createSVNEvent(getAbsPath(), SVNNodeKind.FILE, null, -1, SVNEventAction.DELETE, SVNEventAction.DELETE, null, null);
            eventHandler.handleEvent(event, ISVNEventHandler.UNKNOWN);
        }
    }

    private void sendHunkNotification(SvnHunkInfo hunkInfo, String propName, ISvnPatchContext patchContext) throws SVNException {
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

        ISVNEventHandler eventHandler = patchContext.getEventHandler();
        if (eventHandler != null) {
            eventHandler.handleEvent(event, ISVNEventHandler.UNKNOWN);
        }
    }

    private boolean hasLocalModifications() {
        return hasLocalModifications;
    }

    public boolean hadRejects() {
        return hadRejects;
    }

    public void setHadRejects(boolean hadRejects) {
        this.hadRejects = hadRejects;
    }

    public boolean hadAlreadyApplied() {
        return hadAlreadyApplied;
    }

    public void setHadAlreadyApplied(boolean hadAlreadyApplied) {
        this.hadAlreadyApplied = hadAlreadyApplied;
    }

    public boolean hadPropRejects() {
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

    public boolean isObstructed() {
        return obstructed;
    }

    public void setObstructed(boolean obstructed) {
        this.obstructed = obstructed;
    }

    public SVNNodeKind getKindOnDisk() {
        return kindOnDisk;
    }

    public SVNNodeKind getDbKind() {
        return dbKind;
    }

    public SvnDiffCallback.OperationKind getOperation() {
        return operation;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void setDbKind(SVNNodeKind dbKind) {
        this.dbKind = dbKind;
    }

    public void setOperation(SvnDiffCallback.OperationKind operation) {
        this.operation = operation;
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

    public void setGitSymlinkFormat(boolean gitSymlinkFormat) {
        this.gitSymlinkFormat = gitSymlinkFormat;
    }

    public void setMoveTargetAbsPath(File moveTargetAbsPath) {
        this.moveTargetAbsPath = moveTargetAbsPath;
    }

    public void setOriginalContentFile(File originalContentFile) {
        this.originalContentFile = originalContentFile;
    }

    public File getOriginalContentFile() {
        return originalContentFile;
    }

    public static SvnDiffHunk createAddsSingleLine(SvnPatch patch, String line, ISvnPatchContext patchContext, File workingCopyDirectory) throws IOException, SVNException {
        return addOrDeleteSingleLine(patch, line, !patch.isReverse(), patchContext, workingCopyDirectory);
    }
    
    public static SvnDiffHunk createDeletesSingleLine(SvnPatch patch, String line, ISvnPatchContext patchContext, File workingCopyDirectory) throws IOException, SVNException {
        return addOrDeleteSingleLine(patch, line, patch.isReverse(), patchContext, workingCopyDirectory);
    }

    private static SvnDiffHunk addOrDeleteSingleLine(SvnPatch patch, String line, boolean add, ISvnPatchContext patchContext, File workingCopyDirectory) throws SVNException, IOException {
        final String[] hunkHeader = {"@@ -1 +0,0 @@\n", "@@ -0,0 +1 @@\n"};
        final int headerLength = hunkHeader[add ? 1 : 0].length();
        final int len = line.length();
        final int end = headerLength + (1 + len);

        final StringBuilder stringBuilder = new StringBuilder(end + 1);

        final SvnDiffHunk hunk = new SvnDiffHunk();
        hunk.setPatch(patch);
        if (add) {
            hunk.setOriginalTextRange(new SvnDiffHunk.Range(0, 0, 0));
            hunk.setOriginalNoFinalEol(false);
            hunk.setModifiedTextRange(new SvnDiffHunk.Range(headerLength, end, headerLength));
            hunk.setModifiedNoFinalEol(true);
            hunk.setOriginalStart(0);
            hunk.setOriginalLength(0);
            hunk.setModifiedStart(1);
            hunk.setModifiedLength(1);
        } else {
            hunk.setOriginalTextRange(new SvnDiffHunk.Range(headerLength, end, headerLength));
            hunk.setOriginalNoFinalEol(true);
            hunk.setModifiedTextRange(new SvnDiffHunk.Range(0, 0, 0));
            hunk.setModifiedNoFinalEol(false);
            hunk.setOriginalStart(1);
            hunk.setOriginalLength(1);
            hunk.setModifiedStart(0);
            hunk.setModifiedLength(0);
        }
        hunk.setLeadingContext(0);
        hunk.setTrailingContext(0);
        stringBuilder.append(hunkHeader[add ? 1 : 0]);
        stringBuilder.append(add ? '+' : '-');
        stringBuilder.append(line);
        stringBuilder.append('\n');
        stringBuilder.append("\\ No newline at end of hunk\n");
        final String buf = stringBuilder.toString();

        hunk.setDiffTextRange(new SvnDiffHunk.Range(headerLength, buf.length(), headerLength));

        File uniqueFile = patchContext.createTempFile(workingCopyDirectory);
        final SVNPatchFileStream patchFileStream = SVNPatchFileStream.openForWrite(uniqueFile);
        hunk.setPatchFileStream(patchFileStream);
        patchFileStream.write(buf);

        return hunk;

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
        private ISvnPatchContext patchContext;

        public SymlinkCallbacks(File workingCopyDirectory, ISvnPatchContext patchContext) {
            this.workingCopyDirectory = workingCopyDirectory;
            this.patchContext = patchContext;
        }

        public void write(Object writeBaton, String s) throws SVNException {
            if (!s.startsWith("link ")) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_WRITE_ERROR, "Invalid link representation");
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
            File targetAbsPath = (File) writeBaton;
            patchContext.writeSymlinkContent(targetAbsPath, s.substring("link ".length()));
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
                return patchContext.readSymlinkContent(symlinkReadBaton.getAbsPath());
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
