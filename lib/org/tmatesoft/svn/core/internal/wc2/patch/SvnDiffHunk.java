package org.tmatesoft.svn.core.internal.wc2.patch;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.patch.SVNPatchFileStream;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.IOException;

public class SvnDiffHunk implements Comparable<SvnDiffHunk> {

    private SvnPatch patch;

    private SVNPatchFileStream patchFileStream;

    private Range diffTextRange;
    private Range originalTextRange;
    private Range modifiedTextRange;

    private int originalStart;
    private int originalLength;
    private int modifiedStart;
    private int modifiedLength;

    private int leadingContext;
    private int trailingContext;

    public void resetDiffText() {
        this.diffTextRange.current = diffTextRange.start;
    }

    public void resetOriginalText() {
        if (patch.isReverse()) {
            modifiedTextRange.current = modifiedTextRange.start;
        } else {
            originalTextRange.current = originalTextRange.start;
        }
    }

    public void resetModifiedText() {
        if (patch.isReverse()) {
            originalTextRange.current = originalTextRange.start;
        } else {
            modifiedTextRange.current = modifiedTextRange.start;
        }
    }

    public int getDirectedOriginalStart() {
        return patch.isReverse() ? modifiedStart : originalStart;
    }

    public int getDirectedOriginalLength() {
        return patch.isReverse() ? modifiedLength : originalLength;
    }

    public int getDirectedModifiedStart() {
        return patch.isReverse() ? originalStart : modifiedStart;
    }

    public int getDirectedModifiedLength() {
        return patch.isReverse() ? originalLength : modifiedLength;
    }

    public int getLeadingContext() {
        return leadingContext;
    }

    public int getTrailingContext() {
        return trailingContext;
    }

    public void setOriginalStart(int originalStart) {
        this.originalStart = originalStart;
    }

    public void setOriginalLength(int originalLength) {
        this.originalLength = originalLength;
    }

    public void setModifiedStart(int modifiedStart) {
        this.modifiedStart = modifiedStart;
    }

    public void setModifiedLength(int modifiedLength) {
        this.modifiedLength = modifiedLength;
    }

    public void setDiffTextRange(Range diffTextRange) {
        this.diffTextRange = diffTextRange;
    }

    public void setOriginalTextRange(Range originalTextRange) {
        this.originalTextRange = originalTextRange;
    }

    public void setModifiedTextRange(Range modifiedTextRange) {
        this.modifiedTextRange = modifiedTextRange;
    }

    private int getOriginalStart() {
        return originalStart;
    }

    public int getOriginalLength() {
        return originalLength;
    }

    private int getModifiedStart() {
        return modifiedStart;
    }

    public int getModifiedLength() {
        return modifiedLength;
    }

    public void setLeadingContext(int leadingContext) {
        this.leadingContext = leadingContext;
    }

    public void setTrailingContext(int trailingContext) {
        this.trailingContext = trailingContext;
    }

    public void setPatch(SvnPatch patch) {
        this.patch = patch;
    }

    public String readLineDiffText(String[] eolStr, boolean[] eof) throws IOException, SVNException {
        if (diffTextRange.current >= diffTextRange.end) {
            eof[0] = true;
            if (eolStr != null) {
                eolStr[0] = null;
            }
            return "";
        }
        long pos = patchFileStream.getSeekPosition();
        patchFileStream.setSeekPosition(diffTextRange.current);

        long maxLen = diffTextRange.end - diffTextRange.current;
        String line = readLine(patchFileStream, eolStr, eof);
        diffTextRange.current = 0;
        diffTextRange.current = patchFileStream.getSeekPosition();
        patchFileStream.setSeekPosition(pos);

        if (patch.isReverse()) {
            if (line.startsWith("+")) {
                line = "-" + line.substring("+".length());
            } else if (line.startsWith("-")) {
                line = "+" + line.substring("-".length());
            }
        }
        return line;
    }

    public String readLineOriginalText(String[] eolStr, boolean[] eof) throws SVNException {
        return readLineOriginalOrModified(patch.isReverse() ? modifiedTextRange : originalTextRange,
                eolStr, eof, patch.isReverse() ? '-' : '+');
    }

    public String readLineModifiedText(String[] eolStr, boolean[] eof) throws SVNException {
        return readLineOriginalOrModified(patch.isReverse() ? originalTextRange : modifiedTextRange,
                eolStr, eof, patch.isReverse() ? '+' : '-');
    }

    private String readLineOriginalOrModified(Range range, String[] eolStr, boolean[] eof, char forbidden) throws SVNException {
        try {
            if (range.current >= range.end) {
                eof[0] = true;
                if (eolStr != null) {
                    eolStr[0] = null;
                }
                return "";
            }
            long pos = patchFileStream.getSeekPosition();
            patchFileStream.setSeekPosition(range.current);

            boolean filtered;
            long maxLen;
            String str;

            do {
                maxLen = range.end - range.current;
                String oldEol = null;
                if (maxLen < 0 && eolStr != null) {
                    oldEol = eolStr[0];
                }

                str = readLine(patchFileStream, eolStr, eof);

                //here we apply "maxLen" restriction; suppose str="abcd\n", maxLen=3: we should cut str to "abc" and forget about finding EOL
                if (maxLen >= 0 && str.length() >= maxLen) {
                    str = str.substring(0, (int) maxLen);
                    if (eolStr != null && eolStr[0] != null && !str.endsWith("\r") && !str.endsWith("\n")) {
                        eolStr[0] = null;
                    }
                    if (eof != null) {
                        eof[0] = maxLen == 0;
                    }
                } else if (maxLen < 0 && eolStr != null) {
                    eolStr[0] = oldEol;
                }

                range.current = patchFileStream.getSeekPosition();
                filtered = (str.length() > 0) && (str.charAt(0) == forbidden || str.charAt(0) == '\\');

            } while (filtered && !eof[0]);

            String result;
            if (filtered) {
                result = "";
            } else if (str.startsWith("+") || str.startsWith("-") || str.startsWith(" ")) {
                result = str.substring(1);
            } else {
                result = str;
            }
            patchFileStream.setSeekPosition(pos);
            return result;
        } catch (IOException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }
        return null;
    }

    private String readLine(SVNPatchFileStream patchFileStream, String[] eolStr, boolean[] eof) throws IOException, SVNException {
        StringBuffer lineBuffer = new StringBuffer();
        StringBuffer eolStrBuffer = new StringBuffer();
        if (eof != null) {
            eof[0] = patchFileStream.readLineWithEol(lineBuffer, eolStrBuffer);
        }
        if (eolStr != null) {
            eolStr[0] = eolStrBuffer.length() == 0 ? null : eolStrBuffer.toString();
        }
        return lineBuffer.toString();
//
//
//        String str = patchFileStream.readLine();
//        eof[0] = str == null;
//        if (str != null) {
//            int suffixSize = 0;
//            if (str.endsWith("\r\n")) {
//                suffixSize = 2;
//            } else if (str.endsWith("\n") || str.endsWith("\r")) {
//                suffixSize = 1;
//            }
//            if (suffixSize > 0) {
//                eolStr[0] = str.substring(str.length() - suffixSize);
//                str = str.substring(0, str.length() - suffixSize);
//            }
//        }
//        return str;
    }

    public int compareTo(SvnDiffHunk diffHunk) {
        if (getOriginalStart() < diffHunk.getOriginalStart()) {
            return -1;
        }
        if (getOriginalStart() > diffHunk.getOriginalStart()) {
            return 1;
        }
        return 0;
    }

    public void setPatchFileStream(SVNPatchFileStream patchFileStream) {
        this.patchFileStream = patchFileStream;
    }

    public void decreaseOriginalLength() {
        originalLength--;
    }

    public void decreaseModifiedLength() {
        modifiedLength--;
    }

    static class Range {
        private long start;
        private long end;
        private long current;

        public Range(long start, long end, long current) {
            this.start = start;
            this.end = end;
            this.current = current;
        }
    }
}
