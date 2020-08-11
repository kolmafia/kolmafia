package org.tmatesoft.svn.core.internal.wc2.patch;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.InflaterInputStream;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNMergeRangeList;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.internal.util.SVNFormatUtil;
import org.tmatesoft.svn.core.internal.util.SVNMergeInfoUtil;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.SVNPropertiesManager;
import org.tmatesoft.svn.core.internal.wc.patch.SVNPatchFileStream;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnDiffCallback;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnDiffGenerator;

public class SvnPatch {

    public static final File DEV_NULL = new File("/dev/null");

    private static final Transition[] TRANSITIONS = new Transition[] {
            new Transition("--- ", ParserState.START, IParserFunction.DIFF_MINUS),
            new Transition("+++ ", ParserState.MINUS_SEEN, IParserFunction.DIFF_PLUS),
            new Transition("diff --git", ParserState.START, IParserFunction.GIT_START),
            new Transition("--- a/", ParserState.GIT_DIFF_SEEN, IParserFunction.GIT_MINUS),
            new Transition("--- a/", ParserState.GIT_MODE_SEEN, IParserFunction.GIT_MINUS),
            new Transition("--- a/", ParserState.GIT_TREE_SEEN, IParserFunction.GIT_MINUS),
            new Transition("--- /dev/null", ParserState.GIT_MODE_SEEN, IParserFunction.GIT_MINUS),
            new Transition("--- /dev/null", ParserState.GIT_TREE_SEEN, IParserFunction.GIT_MINUS),
            new Transition("+++ b/", ParserState.GIT_MINUS_SEEN, IParserFunction.GIT_PLUS),
            new Transition("+++ /dev/null", ParserState.GIT_MINUS_SEEN, IParserFunction.GIT_PLUS),
            new Transition("old mode ", ParserState.GIT_DIFF_SEEN, IParserFunction.GIT_OLD_MODE),
            new Transition("new mode ", ParserState.OLD_MODE_SEEN, IParserFunction.GIT_NEW_MODE),
            new Transition("rename from ", ParserState.GIT_DIFF_SEEN, IParserFunction.GIT_MOVE_FROM),
            new Transition("rename from ", ParserState.GIT_MODE_SEEN, IParserFunction.GIT_MOVE_FROM),
            new Transition("rename to ", ParserState.MOVE_FROM_SEEN, IParserFunction.GIT_MOVE_TO),
            new Transition("copy from ", ParserState.GIT_DIFF_SEEN, IParserFunction.GIT_COPY_FROM),
            new Transition("copy from ", ParserState.GIT_MODE_SEEN, IParserFunction.GIT_COPY_FROM),
            new Transition("copy to ", ParserState.COPY_FROM_SEEN, IParserFunction.GIT_COPY_TO),
            new Transition("new file ", ParserState.GIT_DIFF_SEEN, IParserFunction.GIT_NEW_FILE),
            new Transition("deleted file ", ParserState.GIT_DIFF_SEEN, IParserFunction.GIT_DELETED_FILE),
            new Transition("index ", ParserState.GIT_DIFF_SEEN, IParserFunction.GIT_INDEX),
            new Transition("index ", ParserState.GIT_TREE_SEEN, IParserFunction.GIT_INDEX),
            new Transition("index ", ParserState.GIT_MODE_SEEN, IParserFunction.GIT_INDEX),
            new Transition("GIT binary patch", ParserState.GIT_DIFF_SEEN, IParserFunction.BINARY_PATCH_START),
            new Transition("GIT binary patch", ParserState.GIT_TREE_SEEN, IParserFunction.BINARY_PATCH_START),
            new Transition("GIT binary patch", ParserState.GIT_MODE_SEEN, IParserFunction.BINARY_PATCH_START)
    };

    public static SvnPatch parseNextPatch(SvnPatchFile patchFile, boolean reverse, boolean ignoreWhitespace) throws IOException, SVNException {
        boolean eof;
        long lastLine;

        if (patchFile.getPatchFileStream().isEOF()) {
            /* No more patches here. */
            return null;
        }

        ParserState state = ParserState.START;
        boolean lineAfterTreeHeaderRead = false;

        SvnPatch patch = new SvnPatch();
        patch.setNodeKind(SVNNodeKind.UNKNOWN);
        patch.setOperation(SvnDiffCallback.OperationKind.Unchanged);

        long pos = patchFile.getNextPatchOffset();
        patchFile.getPatchFileStream().setSeekPosition(pos);

        do {
            boolean validHeaderLine = false;
            lastLine = pos;

            StringBuffer lineBuffer = new StringBuffer();
            eof = patchFile.getPatchFileStream().readLine(lineBuffer);
            String line = lineBuffer.toString();

            if (!eof) {
                pos = patchFile.getPatchFileStream().getSeekPosition();
            }

            for (int i = 0; i < TRANSITIONS.length; i++) {
                final Transition transition = TRANSITIONS[i];
                if (transition.matches(line, state)) {
                    state = transition.getParserFunction().parse(line, patch, state);
                    validHeaderLine = true;
                    break;
                }
            }

            if (state == ParserState.UNIDIFF_FOUND || state == ParserState.GIT_HEADER_FOUND || state == ParserState.BINARY_PATCH_FOUND) {
                break;
            } else if ((state == ParserState.GIT_TREE_SEEN || state == ParserState.GIT_MODE_SEEN) && lineAfterTreeHeaderRead && !validHeaderLine) {
                patchFile.getPatchFileStream().setSeekPosition(lastLine);
                break;
            } else if (state == ParserState.GIT_TREE_SEEN || state == ParserState.GIT_MODE_SEEN) {
                lineAfterTreeHeaderRead = true;
            } else if (!validHeaderLine && state != ParserState.START
                    && state != ParserState.GIT_DIFF_SEEN) {
                patchFile.getPatchFileStream().setSeekPosition(lastLine);
                state = ParserState.START;
            }
        } while (!eof);

        patch.setReverse(reverse);
        if (reverse) {
            File tmp = patch.getOldFileName();
            patch.setOldFileName(patch.getNewFileName());
            patch.setNewFileName(tmp);

            switch (patch.getOperation()) {
                case Added:
                    patch.setOperation(SvnDiffCallback.OperationKind.Deleted);
                    break;
                case Deleted:
                    patch.setOperation(SvnDiffCallback.OperationKind.Added);
                    break;
                case Modified:
                    break;
                case Copied:
                case Moved:
                    break;
                case Unchanged:
                    break;
            }

            Boolean tmpBit = patch.getOldExecutableBit();
            patch.setOldExecutableBit(patch.getNewExecutableBit());
            patch.setNewExecutableBit(tmpBit);

            tmpBit = patch.getOldSymlinkBit();
            patch.setOldSymlinkBit(patch.getNewSymlinkBit());
            patch.setNewSymlinkBit(tmpBit);
        }

        if (patch.getOldFileName() == null || patch.getNewFileName() == null) {
            patch = null;
        } else {
            if (state == ParserState.BINARY_PATCH_FOUND) {
                patch.parseBinaryPatch(patch, patchFile.getPatchFileStream(), reverse);
            }
            patch.parseHunks(patchFile.getPatchFileStream(), ignoreWhitespace);
        }

        patchFile.setNextPatchOffset(0);
        patchFile.setNextPatchOffset(patchFile.getPatchFileStream().getSeekPosition());

        if (patch != null) {
            Collections.sort(patch.hunks);
        }

        return patch;
    }

    private List<SvnDiffHunk> hunks;
    private Map<String, SvnPropertiesPatch> propPatches;

    private SvnDiffCallback.OperationKind operation;

    private boolean reverse;
    private boolean gitPatchFormat;

    Map<String, SVNMergeRangeList> mergeInfo;
    private Map reverseMergeInfo;
    private File oldFileName;
    private File newFileName;

    private File path;
    private SVNPatchFileStream patchFileStream;
    private BinaryPatch binaryPatch;

    private Boolean newExecutableBit; //tristate: true/false/unknown
    private Boolean oldExecutableBit; //tristate: true/false/unknown

    private Boolean newSymlinkBit; //tristate: true/false/unknown
    private Boolean oldSymlinkBit; //tristate: true/false/unknown

    private SVNNodeKind nodeKind;

    private void parseHunks(SVNPatchFileStream patchFileStream, boolean ignoreWhitespace) throws IOException, SVNException {
        String lastPropName = null;
        SvnDiffHunk hunk;

        hunks = new ArrayList<SvnDiffHunk>();
        propPatches = new HashMap<String, SvnPropertiesPatch>();

        do {
            boolean[] isProperty = new boolean[1];
            String[] propName = new String[1];
            SvnDiffCallback.OperationKind[] propOperation = (SvnDiffCallback.OperationKind[]) new SvnDiffCallback.OperationKind[1];
            hunk = parseNextHunk(isProperty, propName, propOperation, patchFileStream, ignoreWhitespace);

            if (hunk != null && isProperty[0]) {
                if (propName[0] == null) {
                    propName[0] = lastPropName;
                } else {
                    lastPropName = propName[0];
                }
                if (SVNProperty.MERGE_INFO.equals(propName[0])) {
                    continue;
                }

                addPropertyHunk(propName[0], hunk, propOperation[0]);
            } else if (hunk != null) {
                hunks.add(hunk);
                lastPropName = null;
            }
        } while (hunk != null);
    }

    private void parseBinaryPatch(SvnPatch patch, SVNPatchFileStream patchFileStream, boolean reverse) throws IOException, SVNException {
        boolean eof = false;
        boolean inBlob = false;
        boolean inSrc = false;

        final BinaryPatch binaryPatch = new BinaryPatch();
        binaryPatch.setPatchFileStream(patchFileStream);

        long lastLine = -1;
        long pos = patchFileStream.getSeekPosition();
        while (!eof) {
            lastLine = pos;

            StringBuffer lineBuffer = new StringBuffer();
            eof = patchFileStream.readLine(lineBuffer);
            String line = lineBuffer.toString();

            pos = patchFileStream.getSeekPosition();

            if (inBlob) {
                char c = line.length() == 0 ? '\0' : line.charAt(0);
                if (((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'))
                        && line.length() <= 66
                        && !line.contains(":")
                        && !line.contains(" ")) {
                    if (inSrc) {
                        binaryPatch.setSrcEnd(pos);
                    } else {
                        binaryPatch.setDstEnd(pos);
                    }
                } else if (containsNonSpaceCharacter(line) && !(inSrc && binaryPatch.getSrcStart() < lastLine)) {
                    //bad patch
                    break;
                } else if (inSrc) {
                    //success
                    patch.setBinaryPatch(binaryPatch);
                    break;
                } else {
                    inBlob = false;
                    inSrc = true;
                }
            } else if (line.startsWith("literal ")) {
                try {
                    long expandedSize = Long.parseLong(line.substring("literal ".length()));

                    if (inSrc) {
                        binaryPatch.setSrcStart(pos);
                        binaryPatch.setSrcFileSize(expandedSize);
                    } else {
                        binaryPatch.setDstStart(pos);
                        binaryPatch.setDstFileSize(expandedSize);
                    }
                    inBlob = true;
                } catch (NumberFormatException e) {
                    break;
                }
            } else {
                //Git deltas are not supported
                break;
            }
        }
        if (!eof) {
            patchFileStream.setSeekPosition(lastLine);
        } else if (inSrc && ((binaryPatch.getSrcEnd() > binaryPatch.getSrcStart()) || (binaryPatch.getSrcFileSize() == 0))) {
            //success
            patch.setBinaryPatch(binaryPatch);
        }

        if (reverse && (patch.getBinaryPatch() != null)) {
            long tmpStart = binaryPatch.getSrcStart();
            long tmpEnd = binaryPatch.getSrcEnd();
            long tmpFileSize = binaryPatch.getSrcFileSize();

            binaryPatch.setSrcStart(binaryPatch.getDstStart());
            binaryPatch.setSrcEnd(binaryPatch.getDstEnd());
            binaryPatch.setSrcFileSize(binaryPatch.getDstFileSize());

            binaryPatch.setDstStart(tmpStart);
            binaryPatch.setDstEnd(tmpEnd);
            binaryPatch.setDstFileSize(tmpFileSize);
        }
    }

    private boolean containsNonSpaceCharacter(String line) {
        for (int i = 0; i < line.length(); i++) {
            final char c = line.charAt(i);
            if (!Character.isSpaceChar(c)) {
                return true;
            }
        }
        return false;
    }

    public SvnDiffHunk parseNextHunk(boolean[] isProperty, String[] propName, SvnDiffCallback.OperationKind[] propOperation, SVNPatchFileStream patchStream, boolean ignoreWhitespace) throws IOException, SVNException {
        final String minus = "--- ";
        final String textAtat = "@@";
        final String propAtat = "##";

        boolean originalNoFinalEol = false;
        boolean modifiedNoFinalEol = false;


        propOperation[0] = SvnDiffCallback.OperationKind.Unchanged;
        propName[0] = null;
        isProperty[0] = false;

        if (patchStream.isEOF()) {
            return null;
        }

        boolean inHunk = false;
        boolean hunkSeen = false;
        int leadingContext = 0;
        int trailingContext = 0;
        boolean changedLineSeen = false;
        long originalEnd = 0;
        long modifiedEnd = 0;
        long start = 0;
        long end = 0;

        int originalLines = 0;
        int modifiedLines = 0;

        SvnDiffHunk hunk = new SvnDiffHunk();
        long pos = patchStream.getSeekPosition();

        String line;
        boolean eof;
        long lastLine;
        LineType lastLineType = LineType.NOISE_LINE;
        do {
            lastLine = pos;

            StringBuffer lineBuffer = new StringBuffer();
            eof = patchStream.readLine(lineBuffer);
            line = lineBuffer.toString();

            pos = patchStream.getSeekPosition();
            if (line.startsWith("\\")) {
                if (inHunk) {
                    patchStream.setSeekPosition(lastLine - 2);//2 = max(length(EOL))
                    StringBuffer buffer = new StringBuffer();
                    String s = buffer.toString();
                    StringBuffer eolStrBuffer = new StringBuffer();
                    eof = patchStream.readLineWithEol(lineBuffer, eolStrBuffer);
                    String eolStr = eolStrBuffer.length() == 0 ? null : eolStrBuffer.toString();
                    long hunkTextEnd;

                    if (eolStr == null) {
                        hunkTextEnd = lastLine;
                    } else if (eolStr.charAt(0) == '\r' && eolStr.charAt(1) == '\n') {
                        hunkTextEnd = lastLine - 2;
                    } else if (eolStr.charAt(0) == '\n' || eolStr.charAt(0) == '\r') {
                        hunkTextEnd = lastLine - 1;
                    } else {
                        hunkTextEnd = lastLine;
                    }

                    if (lastLineType == LineType.ORIGINAL_LINE && originalEnd == 0) {
                        originalEnd = hunkTextEnd;
                    } else if (lastLineType == LineType.MODIFIED_LINE && modifiedEnd == 0) {
                        modifiedEnd = hunkTextEnd;
                    } else if (lastLineType == LineType.CONTEXT_LINE) {
                        if (originalEnd == 0) {
                            originalEnd = hunkTextEnd;
                        }
                        if (modifiedEnd == 0) {
                            modifiedEnd = hunkTextEnd;
                        }
                    }
                    patchStream.setSeekPosition(pos);
                    if (lastLineType != LineType.MODIFIED_LINE) {
                        originalNoFinalEol = true;
                    }
                    if (lastLineType != LineType.ORIGINAL_LINE) {
                        modifiedNoFinalEol = true;
                    }
                }
                continue;
            }

            if (inHunk && isProperty[0] && propName[0] != null && propName[0].equals(SVNProperty.MERGE_INFO)) {
                boolean foundMergeInfo = parseMergeInfo(line, hunk);
                if (foundMergeInfo) {
                    continue;
                }
            }

            if (inHunk) {
                final char add = '+';
                final char del = '-';

                if (!hunkSeen) {
                    start = lastLine;
                }

                char c = line.length() == 0 ? '\0' : line.charAt(0);
                if (originalLines > 0 && modifiedLines > 0 && ((c == ' ') || (!eof && line.length() == 0) || (ignoreWhitespace && c != del && c != add))) {
                    hunkSeen = true;
                    originalLines--;
                    modifiedLines--;
                    if (changedLineSeen) {
                        trailingContext++;
                    } else {
                        leadingContext++;
                    }
                    lastLineType = LineType.CONTEXT_LINE;
                } else if (c == del && (originalLines > 0 || line.charAt(1) != del)) {
                    hunkSeen = true;
                    changedLineSeen = true;

                    if (trailingContext > 0) {
                        trailingContext = 0;
                    }

                    if (originalLines > 0) {
                        originalLines--;
                    } else {
                        hunk.setOriginalLength(hunk.getOriginalLength() + 1);
                        hunk.setOriginalFuzz(hunk.getOriginalFuzz() + 1);
                    }
                    lastLineType = LineType.ORIGINAL_LINE;
                } else if (c == add && (modifiedLines > 0 || line.charAt(1) != add)) {
                    hunkSeen = true;
                    changedLineSeen = true;

                    if (trailingContext > 0) {
                        trailingContext = 0;
                    }
                    if (modifiedLines > 0) {
                        modifiedLines--;
                    } else {
                        hunk.setModifiedLength(hunk.getModifiedLength() + 1);
                        hunk.setModifiedFuzz(hunk.getModifiedFuzz() + 1);
                    }
                    lastLineType = LineType.MODIFIED_LINE;
                } else {
                    if (eof) {
                        end = pos;
                    } else {
                        end = lastLine;
                    }

                    if (originalEnd == 0) {
                        originalEnd = end;
                    }
                    if (modifiedEnd == 0) {
                        modifiedEnd = end;
                    }
                    break;
                }
            } else {
                if (line.startsWith(textAtat)) {
                    inHunk = parseHunkHeader(line, hunk, textAtat);
                    if (inHunk) {
                        originalLines = hunk.getOriginalLength();
                        modifiedLines = hunk.getModifiedLength();
                        isProperty[0] = false;
                    }
                } else if (line.startsWith(propAtat)) {
                    inHunk = parseHunkHeader(line, hunk, propAtat);
                    if (inHunk) {
                        originalLines = hunk.getOriginalLength();
                        modifiedLines = hunk.getModifiedLength();
                        isProperty[0] = true;
                    }
                } else if (line.startsWith("Added: ")) {
                    propName[0] = parsePropName(line, "Added: ");
                    if (propName[0] != null) {
                        propOperation[0] = SvnDiffCallback.OperationKind.Added;
                    }
                } else if (line.startsWith("Deleted: ")) {
                    propName[0] = parsePropName(line, "Deleted: ");
                    if (propName[0] != null) {
                        propOperation[0] = SvnDiffCallback.OperationKind.Deleted;
                    }
                } else if (line.startsWith("Modified: ")) {
                    propName[0] = parsePropName(line, "Modified: ");
                    if (propName[0] != null) {
                        propOperation[0] = SvnDiffCallback.OperationKind.Modified;
                    }
                } else if (line.startsWith(minus) || line.startsWith("diff --git ")) {
                    break;
                }
            }
        } while (!eof || line.length() > 0);

        if (!eof) {
            patchStream.setSeekPosition(lastLine);
        }

        if (hunkSeen && start < end) {
            if (originalLines != 0) {
                hunk.setOriginalLength(hunk.getOriginalLength() - originalLines);
                hunk.setOriginalFuzz(hunk.getOriginalFuzz() + originalLines);
            }
            if (modifiedLines != 0) {
                hunk.setModifiedLength(hunk.getModifiedLength() - modifiedLines);
                hunk.setModifiedFuzz(hunk.getModifiedFuzz() + modifiedLines);
            }

            hunk.setPatch(this);
            hunk.setPatchFileStream(patchStream);
            hunk.setLeadingContext(leadingContext);
            hunk.setTrailingContext(trailingContext);
            hunk.setDiffTextRange(new SvnDiffHunk.Range(start, end, start));
            hunk.setOriginalTextRange(new SvnDiffHunk.Range(start, originalEnd, start));
            hunk.setModifiedTextRange(new SvnDiffHunk.Range(start, modifiedEnd, start));
            hunk.setOriginalNoFinalEol(originalNoFinalEol);
            hunk.setModifiedNoFinalEol(modifiedNoFinalEol);
            return hunk;
        } else {
            hunk = null;
        }
        return hunk;
    }

    private boolean parseMergeInfo(String line, SvnDiffHunk hunk) throws SVNException {
        int slashPos = line.indexOf('/');
        int colonPos = line.indexOf(':');

        boolean foundMergeInfo = false;
        if (slashPos >= 0 && colonPos >= 0 && line.charAt(colonPos + 1) == 'r' && slashPos < colonPos) {
            int s = slashPos;

            StringBuilder input = new StringBuilder();
            while (s <= colonPos) {
                input.append(line.charAt(s));
                s++;
            }

            s++;

            while (s < line.length()) {
                if (SVNFormatUtil.isSpace(line.charAt(s))) {
                    break;
                }
                input.append(line.charAt(s));
                s++;
            }

            Map<String, SVNMergeRangeList> mergeInfoMap;
            try {
                mergeInfoMap = SVNMergeInfoUtil.parseMergeInfo(new StringBuffer(input.toString()), null);
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() == SVNErrorCode.MERGE_INFO_PARSE_ERROR) {
                    mergeInfoMap = null;
                } else {
                    throw e;
                }
            }

            if (mergeInfoMap != null) {
                if (hunk.getOriginalLength() > 0) {
                    if (isReverse()) {
                        if (getMergeInfo() == null) {
                            setMergeInfo(mergeInfoMap);
                        } else {
                            SVNMergeInfoUtil.mergeMergeInfos(getMergeInfo(), mergeInfoMap);
                        }
                    } else {
                        if (getReverseMergeInfo() == null) {
                            setReverseMergeInfo(mergeInfoMap);
                        } else {
                            SVNMergeInfoUtil.mergeMergeInfos(getReverseMergeInfo(), mergeInfoMap);
                        }
                    }
                    hunk.decreaseOriginalLength();
                } else if (hunk.getModifiedLength() > 0) {
                    if (isReverse()) {
                        if (getReverseMergeInfo() == null) {
                            setReverseMergeInfo(mergeInfoMap);
                        } else {
                            SVNMergeInfoUtil.mergeMergeInfos(getReverseMergeInfo(), mergeInfoMap);
                        }
                    } else {
                        if (getMergeInfo() == null) {
                            setMergeInfo(mergeInfoMap);
                        } else {
                            SVNMergeInfoUtil.mergeMergeInfos(getMergeInfo(), mergeInfoMap);
                        }
                    }
                    hunk.decreaseModifiedLength();
                }
                foundMergeInfo = true;
            }
        }
        return foundMergeInfo;
    }

    private String parsePropName(String header, String indicator) throws SVNException {
        String propName = header.substring(indicator.length());
        if (propName.length() == 0) {
            return null;
        } else if (!SVNPropertiesManager.isValidPropertyName(propName)) {
            propName = propName.trim();
            return SVNPropertiesManager.isValidPropertyName(propName) ? propName : null;
        }
        return propName;
    }

    private boolean parseHunkHeader(String header, SvnDiffHunk hunk, String atat) {
        int pos = atat.length();

        if (header.charAt(pos) != ' ') {
            return false;
        }
        pos++;
        if (header.charAt(pos) != '-') {
            return false;
        }
        StringBuilder range = new StringBuilder();
        int start = ++pos;
        while (pos < header.length() && header.charAt(pos) != ' ') {
            pos++;
        }
        if (pos == header.length() || header.charAt(pos) != ' ') {
            return false;
        }
        range.append(header.substring(start, pos));

        int[] startArray = new int[1];
        int[] lengthArray = new int[1];
        if (!parseRange(startArray, lengthArray, range)) {
            hunk.setOriginalStart(startArray[0]);
            hunk.setOriginalLength(lengthArray[0]);
            return false;
        }
        hunk.setOriginalStart(startArray[0]);
        hunk.setOriginalLength(lengthArray[0]);

        range = new StringBuilder();
        pos++;
        if (header.charAt(pos) != '+') {
            return false;
        }
        start = ++pos;
        while (pos < header.length() && header.charAt(pos) != ' ') {
            pos++;
        }
        if (pos == header.length() || header.charAt(pos) != ' ') {
            return false;
        }
        range.append(header.substring(start, pos));

        pos++;
        if (!header.substring(pos).startsWith(atat)) {
            return false;
        }
        if (!parseRange(startArray, lengthArray, range)) {
            hunk.setModifiedStart(startArray[0]);
            hunk.setModifiedLength(lengthArray[0]);
            return false;
        }
        hunk.setModifiedStart(startArray[0]);
        hunk.setModifiedLength(lengthArray[0]);
        return true;
    }

    private boolean parseRange(int[] start, int[] length, StringBuilder range) {
        if (range.length() == 0) {
            return false;
        }
        int commaPos = range.indexOf(",");
        if (commaPos >= 0) {
            if (range.length() > 1) {
                if (!parseOffset(length, range.substring(commaPos + ",".length()))) {
                    return false;
                }
                range.setLength(commaPos);
            } else {
                return false;
            }
        } else {
            length[0] = 1;
        }
        return parseOffset(start, range.toString());
    }

    private boolean parseOffset(int[] offset, String range) {
        final String s = range;
        try {
            offset[0] = Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void addPropertyHunk(String propName, SvnDiffHunk hunk, SvnDiffCallback.OperationKind operation) {
        SvnPropertiesPatch propPatch = propPatches.get(propName);
        if (propPatch == null) {
            propPatch = new SvnPropertiesPatch(propName, new ArrayList<SvnDiffHunk>(), operation);
            propPatches.put(propName, propPatch);
        }
        propPatch.addHunk(hunk);
    }

    public File getOldFileName() {
        return oldFileName;
    }

    public File getNewFileName() {
        return newFileName;
    }

    public List<SvnDiffHunk> getHunks() {
        return hunks;
    }

    public Map<String, SvnPropertiesPatch> getPropPatches() {
        return propPatches;
    }

    public SvnDiffCallback.OperationKind getOperation() {
        return operation;
    }

    public boolean isReverse() {
        return reverse;
    }

    public boolean isGitPatchFormat() {
        return gitPatchFormat;
    }

    public void setGitPatchFormat(boolean gitPatchFormat) {
        this.gitPatchFormat = gitPatchFormat;
    }

    public Map getMergeInfo() {
        return mergeInfo;
    }

    public Map getReverseMergeInfo() {
        return reverseMergeInfo;
    }

    public void setMergeInfo(Map<String, SVNMergeRangeList> mergeInfo) {
        this.mergeInfo = mergeInfo;
    }

    public void setReverseMergeInfo(Map reverseMergeInfo) {
        this.reverseMergeInfo = reverseMergeInfo;
    }

    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    public void setOldFileName(File oldFileName) {
        this.oldFileName = oldFileName;
    }

    public void setNewFileName(File newFileName) {
        this.newFileName = newFileName;
    }

    public void setOperation(SvnDiffCallback.OperationKind operation) {
        this.operation = operation;
    }

    private static File grabFileName(String s) {
        if ("/dev/null".equals(s)) {
            return DEV_NULL;
        }
        return SVNFileUtil.createFilePath(SVNPathUtil.canonicalizePath(s));
    }

    public void setBinaryPatch(BinaryPatch binaryPatch) {
        this.binaryPatch = binaryPatch;
    }

    public BinaryPatch getBinaryPatch() {
        return binaryPatch;
    }

    public Boolean getNewExecutableBit() {
        return newExecutableBit;
    }

    public void setNewExecutableBit(Boolean newExecutableBit) {
        this.newExecutableBit = newExecutableBit;
    }

    public Boolean getOldExecutableBit() {
        return oldExecutableBit;
    }

    public void setOldExecutableBit(Boolean oldExecutableBit) {
        this.oldExecutableBit = oldExecutableBit;
    }

    public Boolean getNewSymlinkBit() {
        return newSymlinkBit;
    }

    public void setNewSymlinkBit(Boolean newSymlinkBit) {
        this.newSymlinkBit = newSymlinkBit;
    }

    public Boolean getOldSymlinkBit() {
        return oldSymlinkBit;
    }

    public void setOldSymlinkBit(Boolean oldSymlinkBit) {
        this.oldSymlinkBit = oldSymlinkBit;
    }

    public SVNNodeKind getNodeKind() {
        return nodeKind;
    }

    public void setNodeKind(SVNNodeKind nodeKind) {
        this.nodeKind = nodeKind;
    }

    private static enum ParserState {
        START, GIT_DIFF_SEEN, GIT_TREE_SEEN, GIT_MINUS_SEEN, GIT_PLUS_SEEN, OLD_MODE_SEEN, GIT_MODE_SEEN, MOVE_FROM_SEEN, COPY_FROM_SEEN, MINUS_SEEN, UNIDIFF_FOUND, GIT_HEADER_FOUND, BINARY_PATCH_FOUND
    }

    private static interface IParserFunction {
        IParserFunction DIFF_MINUS = new IParserFunction() {
            public ParserState parse(String line, SvnPatch patch, ParserState currentState) {
                String s = line.split("\t")[0].substring("--- ".length());
                patch.setOldFileName(grabFileName(s));
                return ParserState.MINUS_SEEN;
            }
        };
        IParserFunction DIFF_PLUS = new IParserFunction() {
            public ParserState parse(String line, SvnPatch patch, ParserState currentState) {
                String s = line.split("\t")[0].substring("+++ ".length());
                patch.setNewFileName(grabFileName(s));
                return ParserState.UNIDIFF_FOUND;
            }
        };
        IParserFunction GIT_START = new IParserFunction() {
            public ParserState parse(String line, SvnPatch patch, ParserState currentState) {
                int oldPathMarkerPos = line.indexOf(" a/");
                if (oldPathMarkerPos < 0) {
                    return ParserState.START;
                }
                if (oldPathMarkerPos + 3 == line.length()) {
                    return ParserState.START;
                }
                int newPathMarkerPos = line.indexOf(" b/");
                if (newPathMarkerPos < 0) {
                    return ParserState.START;
                }
                
                int oldPathEndPos;
                int oldPathStartPos = "diff --git a/".length();
                int newPathEndPos = line.length();
                int newPathStartPos = oldPathStartPos;

                while (true) {
                    newPathMarkerPos = line.indexOf(" b/", newPathStartPos);
                    if (newPathMarkerPos < 0) {
                        break;
                    }
                    oldPathEndPos = newPathMarkerPos;
                    newPathStartPos = newPathMarkerPos + " b/".length();

                    int lenOld = oldPathEndPos - oldPathStartPos;
                    int lenNew = newPathEndPos - newPathStartPos;

                    if (lenOld == lenNew && line.substring(oldPathStartPos, oldPathEndPos).equals(line.substring(newPathStartPos, newPathEndPos))) {
                        patch.setOldFileName(grabFileName(line.substring(oldPathStartPos, oldPathEndPos)));
                        patch.setNewFileName(grabFileName(line.substring(newPathStartPos)));
                        break;
                    }
                }
                patch.setOperation(SvnDiffCallback.OperationKind.Modified);
                patch.setGitPatchFormat(true);
                return ParserState.GIT_DIFF_SEEN;
            }
        };
        IParserFunction GIT_MINUS = new IParserFunction() {
            public ParserState parse(String line, SvnPatch patch, ParserState currentState) {
                String s = line.split("\t")[0];
                if (s.startsWith("--- /dev/null")) {
                    patch.setOldFileName(grabFileName("/dev/null"));
                } else {
                    patch.setOldFileName(grabFileName(s.substring("--- a/".length())));
                }
                patch.setGitPatchFormat(true);
                return ParserState.GIT_MINUS_SEEN;
            }
        };
        IParserFunction GIT_PLUS = new IParserFunction() {
            public ParserState parse(String line, SvnPatch patch, ParserState currentState) {
                String s = line.split("\t")[0];
                if (s.startsWith("+++ /dev/null")) {
                    patch.setNewFileName(grabFileName("/dev/null"));
                } else {
                    patch.setNewFileName(grabFileName(s.substring("+++ b/".length())));
                }
                patch.setGitPatchFormat(true);
                return ParserState.GIT_HEADER_FOUND;
            }
        };
        IParserFunction GIT_OLD_MODE = new IParserFunction() {
            public ParserState parse(String line, SvnPatch patch, ParserState currentState) {
                final Boolean[] modeBits = patch.parseGitModeBits(line.substring("old mode ".length()));
                patch.setOldExecutableBit(modeBits[0]);
                patch.setOldSymlinkBit(modeBits[1]);
                patch.setGitPatchFormat(true);
                return ParserState.OLD_MODE_SEEN;
            }
        };
        IParserFunction GIT_NEW_MODE = new IParserFunction() {
            @Override
            public ParserState parse(String line, SvnPatch patch, ParserState currentState) {
                final Boolean[] modeBits = patch.parseGitModeBits(line.substring("new mode ".length()));
                patch.setNewExecutableBit(modeBits[0]);
                patch.setNewSymlinkBit(modeBits[1]);
                patch.setGitPatchFormat(true);
                return ParserState.GIT_MODE_SEEN;
            }
        };
        IParserFunction GIT_MOVE_FROM = new IParserFunction() {
            public ParserState parse(String line, SvnPatch patch, ParserState currentState) {
                patch.setOldFileName(grabFileName(line.substring("rename from ".length())));
                patch.setGitPatchFormat(true);
                return ParserState.MOVE_FROM_SEEN;
            }
        };
        IParserFunction GIT_MOVE_TO = new IParserFunction() {
            public ParserState parse(String line, SvnPatch patch, ParserState currentState) {
                patch.setNewFileName(grabFileName(line.substring("rename to ".length())));
                patch.setOperation(SvnDiffCallback.OperationKind.Moved);
                patch.setGitPatchFormat(true);
                return ParserState.GIT_TREE_SEEN;
            }
        };
        IParserFunction GIT_COPY_FROM = new IParserFunction() {
            public ParserState parse(String line, SvnPatch patch, ParserState currentState) {
                patch.setOldFileName(grabFileName(line.substring("copy from ".length())));
                patch.setGitPatchFormat(true);
                return ParserState.COPY_FROM_SEEN;
            }
        };
        IParserFunction GIT_COPY_TO = new IParserFunction() {
            public ParserState parse(String line, SvnPatch patch, ParserState currentState) {
                patch.setNewFileName(grabFileName(line.substring("copy to ".length())));
                patch.setOperation(SvnDiffCallback.OperationKind.Copied);
                patch.setGitPatchFormat(true);
                return ParserState.GIT_TREE_SEEN;
            }
        };
        IParserFunction GIT_NEW_FILE = new IParserFunction() {
            public ParserState parse(String line, SvnPatch patch, ParserState currentState) {
                final Boolean[] modeBits = patch.parseGitModeBits(line.substring("new file mode ".length()));
                patch.setNewExecutableBit(modeBits[0]);
                patch.setNewSymlinkBit(modeBits[1]);

                patch.setOperation(SvnDiffCallback.OperationKind.Added);
                patch.setNodeKind(SVNNodeKind.FILE);
                patch.setGitPatchFormat(true);
                return ParserState.GIT_TREE_SEEN;
            }
        };
        IParserFunction GIT_DELETED_FILE = new IParserFunction() {
            public ParserState parse(String line, SvnPatch patch, ParserState currentState) {
                patch.setOperation(SvnDiffCallback.OperationKind.Deleted);
                patch.setGitPatchFormat(true);
                return ParserState.GIT_TREE_SEEN;
            }
        };
        IParserFunction GIT_INDEX = new IParserFunction() {
            @Override
            public ParserState parse(String line, SvnPatch patch, ParserState currentState) {
                final int pos = line.substring("index ".length()).indexOf(' ');
                if (pos >= 0 &&
                        patch.getNewExecutableBit() == null &&
                        patch.getNewSymlinkBit() == null &&
                        patch.getOperation() != SvnDiffCallback.OperationKind.Added &&
                        patch.getOperation() != SvnDiffCallback.OperationKind.Deleted) {
                    final Boolean[] modeBits = patch.parseGitModeBits(line.substring(" ".length()));
                    patch.setNewExecutableBit(modeBits[0]);
                    patch.setNewSymlinkBit(modeBits[1]);
                    patch.setOldExecutableBit(patch.getNewExecutableBit());
                    patch.setOldSymlinkBit(patch.getNewSymlinkBit());
                }
                patch.setGitPatchFormat(true);
                return currentState;
            }
        };
        IParserFunction BINARY_PATCH_START = new IParserFunction() {
            @Override
            public ParserState parse(String line, SvnPatch patch, ParserState currentState) {
                return ParserState.BINARY_PATCH_FOUND;
            }
        };
        ParserState parse(String line, SvnPatch patch, ParserState currentState);
    }

    @SuppressWarnings("OctalInteger")
    private Boolean[] parseGitModeBits(String modeString) {
        Boolean executableBit;
        Boolean symlinkBit;
        //this method should assign newExecutableBit and newSymlinkBit
        final int mode = Integer.parseInt(modeString, 8);
        switch (mode  & 0777) {
            case 0644:
                executableBit = Boolean.FALSE;
                break;
            case 0755:
                executableBit = Boolean.TRUE;
                break;
            default:
                executableBit = null;
                break;
        }
        switch (mode & 0170000) {
            case 0120000:
                symlinkBit = Boolean.TRUE;
                break;
            case 0100000:
            case 0040000:
                symlinkBit = Boolean.FALSE;
                break;
            default:
                symlinkBit = null;
                break;
        }
        return new Boolean[] {executableBit, symlinkBit};
    }

    private static class Transition {
        private String expectedInput;
        private ParserState state;
        private IParserFunction parserFunction;

        public Transition(String expectedInput, ParserState state, IParserFunction parserFunction) {
            this.expectedInput = expectedInput;
            this.state = state;
            this.parserFunction = parserFunction;
        }

        public boolean matches(String line, ParserState state) {
            return line.startsWith(expectedInput) && this.state == state;
        }

        private IParserFunction getParserFunction() {
            return parserFunction;
        }
    }

    private static enum LineType {
        NOISE_LINE, ORIGINAL_LINE, MODIFIED_LINE, CONTEXT_LINE
    }

    public static class BinaryPatch {
        private SvnPatch patch;
        private SVNPatchFileStream patchFileStream;
        private long srcStart;
        private long srcEnd;
        private long srcFileSize;
        private long dstStart;
        private long dstEnd;
        private long dstFileSize;

        public SvnPatch getPatch() {
            return patch;
        }

        public void setPatch(SvnPatch patch) {
            this.patch = patch;
        }

        public SVNPatchFileStream getPatchFileStream() {
            return patchFileStream;
        }

        public void setPatchFileStream(SVNPatchFileStream patchFileStream) {
            this.patchFileStream = patchFileStream;
        }

        public long getSrcStart() {
            return srcStart;
        }

        public void setSrcStart(long srcStart) {
            this.srcStart = srcStart;
        }

        public long getSrcEnd() {
            return srcEnd;
        }

        public void setSrcEnd(long srcEnd) {
            this.srcEnd = srcEnd;
        }

        public long getSrcFileSize() {
            return srcFileSize;
        }

        public void setSrcFileSize(long srcFileSize) {
            this.srcFileSize = srcFileSize;
        }

        public long getDstStart() {
            return dstStart;
        }

        public void setDstStart(long dstStart) {
            this.dstStart = dstStart;
        }

        public long getDstEnd() {
            return dstEnd;
        }

        public void setDstEnd(long dstEnd) {
            this.dstEnd = dstEnd;
        }

        public long getDstFileSize() {
            return dstFileSize;
        }

        public void setDstFileSize(long dstFileSize) {
            this.dstFileSize = dstFileSize;
        }

        public InputStream getBinaryDiffOriginalStream() {
            InputStream inputStream = new Base85DataStream(patchFileStream, srcStart, srcEnd);
            inputStream = new InflaterInputStream(inputStream);

            return new CheckBase85LengthInputStream(inputStream, srcFileSize);
        }

        public InputStream getBinaryDiffResultStream() {
            InputStream inputStream = new Base85DataStream(patchFileStream, dstStart, dstEnd);
            inputStream = new InflaterInputStream(inputStream);

            return new CheckBase85LengthInputStream(inputStream, dstFileSize);
        }
    }

    private static class Base85DataStream extends InputStream {

        private final SVNPatchFileStream patchFileStream;
        private long start;
        private final long end;

        private boolean done;
        private byte[] buffer;
        private int bufSize;
        private int bufPos;
        private byte[] singleByteBuffer;

        public Base85DataStream(SVNPatchFileStream patchFileStream, long start, long end) {
            this.patchFileStream = patchFileStream;
            this.start = start;
            this.end = end;
            this.done = false;
            this.buffer = new byte[52];
            this.bufSize = 0;
            this.bufPos = 0;
            this.singleByteBuffer = new byte[1];
        }

        @Override
        public int read() throws IOException {
            final int bytesRead = read(singleByteBuffer, 0, 1);
            if (bytesRead < 0) {
                return bytesRead;
            } else {
                return singleByteBuffer[0] & 0xff;
            }
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            try {
                int remaining = len;
                int destOff = off;
                if (done) {
                    return -1;
                }

                while (remaining != 0 && (bufSize > bufPos || start < end)) {
                    boolean atEof;

                    int available = bufSize - bufPos;
                    if (available != 0) {
                        int n = (remaining < available) ? remaining : available;

                        System.arraycopy(buffer, bufPos, b, destOff, n);
                        destOff += n;
                        remaining -= n;
                        bufPos += n;

                        if (remaining == 0) {
                            return len;
                        }
                    }

                    if (start >= end) {
                        break;
                    }
                    patchFileStream.setSeekPosition(start);
                    final StringBuffer lineBuf = new StringBuffer();
                    atEof = patchFileStream.readLine(lineBuf);
                    final String line = lineBuf.toString();

                    if (atEof) {
                        start = end;
                    } else {
                        start = patchFileStream.getSeekPosition();
                    }
                    if (line.length() > 0 && line.charAt(0) >= 'A' && line.charAt(0) <= 'Z') {
                        bufSize = line.charAt(0) - 'A' + 1;
                    } else if (line.length() > 0 && line.charAt(0) >= 'a' && line.charAt(0) <= 'z') {
                        bufSize = line.charAt(0) - 'a' + 26 + 1;
                    } else {
                        throw new IOException("Unexpected data in base85 section");
                    }
                    if (bufSize < 52) {
                        start = end;
                    }
                    base85DecodeLine(buffer, bufSize, line.substring(1));
                    bufPos = 0;
                }

                len -= remaining;
                done = true;

                return len;
            } catch (SVNException e) {
                throw new IOException(e);
            }
        }

        private static void base85DecodeLine(byte[] outputBuffer, int outputBufferSize, String line) throws IOException {
            int expectedData = (outputBufferSize + 3) / 4 * 5;
            if (line.length() != expectedData) {
                throw new IOException("Unexpected base85 line length");
            }
            int base85Offet = 0;
            int base85Length = line.length();
            int outputBufferOffset = 0;
            while (base85Length != 0) {
                long info = 0;

                for (int i = 0; i < 5; i++) {
                    int value = base85Value(line.charAt(base85Offet + i));
                    info *= 85;
                    info += value;
                }
                for (int i = 0, n = 24; i < 4; i++, n -= 8) {
                    if (i < outputBufferSize) {
                        outputBuffer[outputBufferOffset + i] = (byte) ((info >> n) & 0xFF);
                    }
                }
                base85Offet += 5;
                base85Length -= 5;
                outputBufferOffset += 4;
                outputBufferSize -= 4;
            }
        }

        private static int base85Value(char c) throws IOException {
            final int index = SvnDiffGenerator.B85_TABLE.indexOf(String.valueOf(c));
            if (index < 0) {
                throw new IOException("Invalid base85 value");
            }
            return index;
        }
    }
    private static class CheckBase85LengthInputStream extends InputStream {

        private final InputStream inputStream;
        private final byte[] singleByteBuffer;

        private long remaining;

        private CheckBase85LengthInputStream(InputStream inputStream, long remaining) {
            this.inputStream = inputStream;
            this.remaining = remaining;
            this.singleByteBuffer = new byte[1];
        }

        @Override
        public int read() throws IOException {
            final int read = inputStream.read(singleByteBuffer, 0, 1);
            if (read < 0) {
                return read;
            } else {
                return singleByteBuffer[0] & 0xff;
            }
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int requestedLength = len;
            len = readFully(inputStream, b, off, len);
            if (len < 0) {
                // <0 means stream finished, so we've read 0 bytes
                len = 0;
            }

            if (len > remaining) {
                throw new IOException("Base85 data expands to longer than declared filesize");
            } else if (requestedLength > len && len != remaining) {
                throw new IOException("Base85 data expands to smaller than declared filesize");
            }
            remaining -= len;
            return len == 0 ? -1 : len;
        }

        @Override
        public long skip(long n) throws IOException {
            return inputStream.skip(n);
        }

        @Override
        public int available() throws IOException {
            return inputStream.available();
        }

        @Override
        public void close() throws IOException {
            inputStream.close();
        }

        @Override
        public void mark(int readlimit) {
            inputStream.mark(readlimit);
        }

        @Override
        public void reset() throws IOException {
            inputStream.reset();
        }

        @Override
        public boolean markSupported() {
            return inputStream.markSupported();
        }
    }

    protected static int readFully(InputStream inputStream, byte[] b, int off, int len) throws IOException {
        int totalBytesRead = 0;
        while (true) {
            final int bytesRead = inputStream.read(b, off, len);
            if (bytesRead < 0) {
                break;
            }
            totalBytesRead += bytesRead;
            off += bytesRead;
            len -= bytesRead;
            if (len == 0) {
                break;
            }
        }
        if (totalBytesRead == 0) {
            return -1;
        }
        return totalBytesRead;
    }
}
