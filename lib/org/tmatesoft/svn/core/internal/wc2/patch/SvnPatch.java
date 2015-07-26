package org.tmatesoft.svn.core.internal.wc2.patch;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNMergeRangeList;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.internal.util.SVNFormatUtil;
import org.tmatesoft.svn.core.internal.util.SVNMergeInfoUtil;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.SVNPropertiesManager;
import org.tmatesoft.svn.core.internal.wc.patch.SVNPatchFileStream;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnDiffCallback;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SvnPatch {

    private static final Transition[] TRANSITIONS = new Transition[] {
            new Transition("--- ", ParserState.START, IParserFunction.DIFF_MINUS),
            new Transition("+++ ", ParserState.MINUS_SEEN, IParserFunction.DIFF_PLUS),
            new Transition("diff --git", ParserState.START, IParserFunction.GIT_START),
            new Transition("--- a/", ParserState.GIT_DIFF_SEEN, IParserFunction.GIT_MINUS),
            new Transition("--- a/", ParserState.GIT_TREE_SEEN, IParserFunction.GIT_MINUS),
            new Transition("--- /dev/null", ParserState.GIT_TREE_SEEN, IParserFunction.GIT_MINUS),
            new Transition("+++ b/", ParserState.GIT_MINUS_SEEN, IParserFunction.GIT_PLUS),
            new Transition("+++ /dev/null", ParserState.GIT_MINUS_SEEN, IParserFunction.GIT_PLUS),
            new Transition("rename from ", ParserState.GIT_DIFF_SEEN, IParserFunction.GIT_MOVE_FROM),
            new Transition("rename to ", ParserState.MOVE_FROM_SEEN, IParserFunction.GIT_MOVE_TO),
            new Transition("copy from ", ParserState.GIT_DIFF_SEEN, IParserFunction.GIT_COPY_FROM),
            new Transition("copy to ", ParserState.COPY_FROM_SEEN, IParserFunction.GIT_COPY_TO),
            new Transition("new file ", ParserState.GIT_DIFF_SEEN, IParserFunction.GIT_NEW_FILE),
            new Transition("deleted file ", ParserState.GIT_DIFF_SEEN, IParserFunction.GIT_DELETED_FILE)
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
                    state = transition.getParserFunction().parse(line, patch);
                    validHeaderLine = true;
                    break;
                }
            }

            if (state == ParserState.UNIDIFF_FOUND || state == ParserState.GIT_HEADER_FOUND) {
                break;
            } else if (state == ParserState.GIT_TREE_SEEN && lineAfterTreeHeaderRead) {
                if (!line.startsWith("index ")) {
                    patchFile.getPatchFileStream().setSeekPosition(lastLine);
                    break;
                }
            } else if (state == ParserState.GIT_TREE_SEEN) {
                lineAfterTreeHeaderRead = true;
            } else if (!validHeaderLine && state != ParserState.START
                    && state != ParserState.GIT_DIFF_SEEN
                    && !line.startsWith("index ")) {
                patchFile.getPatchFileStream().setSeekPosition(lastLine);
                state = ParserState.START;
            }
        } while (!eof);

        patch.setReverse(reverse);
        if (reverse) {
            File tmp = patch.getOldFileName();
            patch.setOldFileName(patch.getNewFileName());
            patch.setNewFileName(tmp);
        }

        if (patch.getOldFileName() == null || patch.getNewFileName() == null) {
            patch = null;
        } else {
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

    Map<String, SVNMergeRangeList> mergeInfo;
    private Map reverseMergeInfo;
    private File oldFileName;
    private File newFileName;

    private File path;
    private SVNPatchFileStream patchFileStream;

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

    public SvnDiffHunk parseNextHunk(boolean[] isProperty, String[] propName, SvnDiffCallback.OperationKind[] propOperation, SVNPatchFileStream patchStream, boolean ignoreWhitespace) throws IOException, SVNException {
        final String minus = "--- ";
        final String textAtat = "@@";
        final String propAtat = "##";

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
                } else if (originalLines > 0 && c == del) {
                    hunkSeen = true;
                    changedLineSeen = true;

                    if (trailingContext > 0) {
                        trailingContext = 0;
                    }

                    originalLines--;
                    lastLineType = LineType.MODIFIED_LINE;
                } else if (modifiedLines > 0 && c == add) {
                    hunkSeen = true;
                    changedLineSeen = true;

                    if (trailingContext > 0) {
                        trailingContext = 0;
                    }
                    modifiedLines--;
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
            hunk.setPatch(this);
            hunk.setPatchFileStream(patchStream);
            hunk.setLeadingContext(leadingContext);
            hunk.setTrailingContext(trailingContext);
            hunk.setDiffTextRange(new SvnDiffHunk.Range(start, end, start));
            hunk.setOriginalTextRange(new SvnDiffHunk.Range(start, originalEnd, start));
            hunk.setModifiedTextRange(new SvnDiffHunk.Range(start, modifiedEnd, start));
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
        return SVNFileUtil.createFilePath(SVNPathUtil.canonicalizePath(s));
    }

    private static enum ParserState {
        START, GIT_DIFF_SEEN, GIT_TREE_SEEN, GIT_MINUS_SEEN, GIT_PLUS_SEEN, MOVE_FROM_SEEN, COPY_FROM_SEEN, MINUS_SEEN, UNIDIFF_FOUND, GIT_HEADER_FOUND
    }

    private static interface IParserFunction {
        IParserFunction DIFF_MINUS = new IParserFunction() {
            public ParserState parse(String line, SvnPatch patch) {
                String s = line.split("\t")[0].substring("--- ".length());
                patch.setOldFileName(grabFileName(s));
                return ParserState.MINUS_SEEN;
            }
        };
        IParserFunction DIFF_PLUS = new IParserFunction() {
            public ParserState parse(String line, SvnPatch patch) {
                String s = line.split("\t")[0].substring("+++ ".length());
                patch.setNewFileName(grabFileName(s));
                return ParserState.UNIDIFF_FOUND;
            }
        };
        IParserFunction GIT_START = new IParserFunction() {
            public ParserState parse(String line, SvnPatch patch) {
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
                if (newPathMarkerPos + 3 == line.length()) {
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

                    if (newPathStartPos == line.length()) {
                        break;
                    }
                    int lenOld = oldPathEndPos - oldPathStartPos;
                    int lenNew = newPathEndPos - newPathStartPos;

                    if (lenOld == lenNew && line.substring(oldPathStartPos, oldPathEndPos).equals(line.substring(newPathStartPos, newPathEndPos))) {
                        patch.setOldFileName(grabFileName(line.substring(oldPathStartPos, oldPathEndPos)));
                        patch.setNewFileName(grabFileName(line.substring(newPathStartPos)));
                        break;
                    }
                }
                patch.setOperation(SvnDiffCallback.OperationKind.Modified);
                return ParserState.GIT_DIFF_SEEN;
            }
        };
        IParserFunction GIT_MINUS = new IParserFunction() {
            public ParserState parse(String line, SvnPatch patch) {
                String s = line.split("\t")[0];
                if (s.startsWith("--- /dev/null")) {
                    patch.setOldFileName(grabFileName("/dev/null"));
                } else {
                    patch.setOldFileName(grabFileName(s.substring("--- a/".length())));
                }
                return ParserState.GIT_MINUS_SEEN;
            }
        };
        IParserFunction GIT_PLUS = new IParserFunction() {
            public ParserState parse(String line, SvnPatch patch) {
                String s = line.split("\t")[0];
                if (s.startsWith("+++ /dev/null")) {
                    patch.setNewFileName(grabFileName("/dev/null"));
                } else {
                    patch.setNewFileName(grabFileName(s.substring("+++ b/".length())));
                }
                return ParserState.GIT_HEADER_FOUND;
            }
        };
        IParserFunction GIT_MOVE_FROM = new IParserFunction() {
            public ParserState parse(String line, SvnPatch patch) {
                patch.setOldFileName(grabFileName(line.substring("rename from ".length())));
                return ParserState.MOVE_FROM_SEEN;
            }
        };
        IParserFunction GIT_MOVE_TO = new IParserFunction() {
            public ParserState parse(String line, SvnPatch patch) {
                patch.setNewFileName(grabFileName(line.substring("rename to ".length())));
                patch.setOperation(SvnDiffCallback.OperationKind.Moved);
                return ParserState.GIT_TREE_SEEN;
            }
        };
        IParserFunction GIT_COPY_FROM = new IParserFunction() {
            public ParserState parse(String line, SvnPatch patch) {
                patch.setOldFileName(grabFileName(line.substring("copy from ".length())));
                return ParserState.COPY_FROM_SEEN;
            }
        };
        IParserFunction GIT_COPY_TO = new IParserFunction() {
            public ParserState parse(String line, SvnPatch patch) {
                patch.setNewFileName(grabFileName(line.substring("copy to ".length())));
                patch.setOperation(SvnDiffCallback.OperationKind.Copied);
                return ParserState.GIT_TREE_SEEN;
            }
        };
        IParserFunction GIT_NEW_FILE = new IParserFunction() {
            public ParserState parse(String line, SvnPatch patch) {
                patch.setOperation(SvnDiffCallback.OperationKind.Added);
                return ParserState.GIT_TREE_SEEN;
            }
        };
        IParserFunction GIT_DELETED_FILE = new IParserFunction() {
            public ParserState parse(String line, SvnPatch patch) {
                patch.setOperation(SvnDiffCallback.OperationKind.Deleted);
                return ParserState.GIT_TREE_SEEN;
            }
        };
        ParserState parse(String line, SvnPatch patch);
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
}
