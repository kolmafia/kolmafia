package org.tmatesoft.svn.core.internal.wc2.patch;

import org.tmatesoft.svn.core.ISVNCanceller;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.util.SVNFormatUtil;
import org.tmatesoft.svn.core.internal.wc.admin.SVNTranslator;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SvnTargetContent {

    boolean existed;
    int currentLine;
    private SVNWCContext.SVNEolStyle eolStyle;
    private String eolStr;
    private List<Long> lines;
    private List<SvnHunkInfo> hunkInfos;
    private boolean eof;
    private Map<String, byte[]> keywords;
    private Object readBaton;
    private Object writeBaton;
    private ISeekCallback seekCallback;
    private ITellCallback tellCallback;
    private IRealLineCallback readLineCallback;
    private IWriteCallback writeCallback;

    public SvnTargetContent() {
        this.hunkInfos = new ArrayList<SvnHunkInfo>();
        this.lines = new ArrayList<Long>();
        this.keywords = new HashMap<String, byte[]>();
        this.eolStyle = SVNWCContext.SVNEolStyle.Unknown;
    }

    public String readLine() throws SVNException {
        int maxLine = getLines().size() + 1;

        if (isEof() || getReadLineCallback() == null) {
            return "";
        }

        assert getCurrentLine() <= maxLine;

        if (getCurrentLine() == maxLine) {
            long offset = getTellCallback().tell(getReadBaton());
            getLines().add(offset);
        }

        String[] eolStr = {null};
        boolean[] eof = {false};
        String line = getReadLineCallback().readLine(getReadBaton(), eolStr, eof);
        setEof(eof[0]);
        if (getEolStyle() == SVNWCContext.SVNEolStyle.None) {
            setEolStr(eolStr[0]);
        }

        if (line != null) {
            line = SVNTranslator.translateString(line, null, getKeywords(), false, false);
        } else {
            line = "";
        }
        if ((line != null && line.length() > 0) || eolStr != null) {
            setCurrentLine(getCurrentLine() + 1);
        }
        assert getCurrentLine() > 0;

        return line;
    }

    protected void seekToLine(int line) throws SVNException {
        assert line > 0;

        if (line == getCurrentLine()) {
            return;
        }

        int savedLine = getCurrentLine();
        boolean savedEof = isEof();

        if (line <= getLines().size()) {
            long offset = (long) getLines().get(line - 1);
            getSeekCallback().seek(getReadBaton(), offset);
            setCurrentLine(line);
        } else {
            while (!isEof() && getCurrentLine() < line) {
                readLine();
            }
        }

        if (savedEof && savedLine > getCurrentLine()) {
            setEof(false);
        }
    }

    protected int scanForMatch(SvnDiffHunk hunk, boolean matchFirst, int upperLine, int fuzz, boolean ignoreWhitespace, boolean matchModified, ISVNCanceller canceller) throws SVNException {
        int matchedLine = 0;
        while ((getCurrentLine() < upperLine || upperLine == 0) && !isEof()) {
            if (canceller != null) {
                canceller.checkCancelled();
            }

            boolean matched = matchHunk(hunk, fuzz, ignoreWhitespace, matchModified);
            if (matched) {
                boolean taken = false;
                int length;

                List<SvnHunkInfo> hunks = getHunkInfos();
                for (SvnHunkInfo hunkInfo : hunks) {
                    if (matchModified) {
                        length = hunkInfo.getHunk().getDirectedModifiedLength();
                    } else {
                        length = hunkInfo.getHunk().getDirectedOriginalLength();
                    }
                    taken = (!hunkInfo.isRejected() &&
                            getCurrentLine() >= hunkInfo.getMatchedLine() &&
                            getCurrentLine() < (hunkInfo.getMatchedLine() + length));
                    if (taken) {
                        break;
                    }
                }

                if (!taken) {
                    matchedLine = getCurrentLine();
                    if (matchFirst) {
                        break;
                    }
                }
            }
            if (!isEof()) {
                seekToLine(getCurrentLine() + 1);
            }
        }
        return matchedLine;
    }

    private boolean matchHunk(SvnDiffHunk hunk, int fuzz, boolean ignoreWhitespace, boolean matchModified) throws SVNException {
        boolean matched = false;

        if (isEof()) {
            return matched;
        }

        int savedLine = getCurrentLine();
        int linesRead = 0;
        boolean linesMatched = false;
        int leadingContext = hunk.getLeadingContext();
        int trailingContext = hunk.getTrailingContext();
        int hunkLength;
        if (matchModified) {
            hunk.resetModifiedText();
            hunkLength = hunk.getDirectedModifiedLength();
        } else {
            hunk.resetOriginalText();
            hunkLength = hunk.getDirectedOriginalLength();
        }

        boolean[] hunkEof;
        String hunkLine;
        do {
            String hunkLineTranslated;

            hunkEof = new boolean[1];
            if (matchModified) {
                hunkLine = hunk.readLineModifiedText(null, hunkEof);
            } else {
                hunkLine = hunk.readLineOriginalText(null, hunkEof);
            }

            hunkLineTranslated = SVNTranslator.translateString(hunkLine, null, getKeywords(), false, false);
            String targetLine = readLine();
            linesRead++;
            if ((hunkEof[0] && hunkLine.length() == 0) || (isEof() && targetLine.length() == 0)) {
                break;
            }

            if ((linesRead <= fuzz && leadingContext > fuzz) ||
                    linesRead > hunkLength - fuzz && trailingContext > fuzz) {
                linesMatched = true;
            } else {
                if (ignoreWhitespace) {
                    String hunkLineTrimmed = hunkLineTranslated;
                    String targetLineTrimmed = targetLine;
                    hunkLineTrimmed = SVNFormatUtil.collapseSpaces(hunkLineTrimmed);
                    targetLineTrimmed = SVNFormatUtil.collapseSpaces(targetLineTrimmed);
                    linesMatched = hunkLineTrimmed.equals(targetLineTrimmed);
                } else {
                    linesMatched = hunkLineTranslated.equals(targetLine);
                }
            }

        } while (linesMatched);

        boolean ret = linesMatched && hunkEof[0] && hunkLine.length() == 0;
        seekToLine(savedLine);
        return ret;
    }

    protected boolean matchExistingTarget(SvnDiffHunk hunk) throws SVNException {
        hunk.resetModifiedText();
        boolean[] hunkEof = {false};
        boolean linesMatched;
        int savedLine = getCurrentLine();

        do {
            String line = readLine();
            String hunkLine = hunk.readLineModifiedText(null, hunkEof);

            String lineTranslated = SVNTranslator.translateString(line, null, getKeywords(), false, false);
            String hunkLineTranslated = SVNTranslator.translateString(hunkLine, null, getKeywords(), false, false);

            linesMatched = lineTranslated.equals(hunkLineTranslated);

            if (isEof() != hunkEof[0]) {
                return false;
            }
        } while (linesMatched && !isEof() && !hunkEof[0]);

        boolean result = linesMatched && isEof() == hunkEof[0];
        seekToLine(savedLine);
        return result;
    }

    public boolean isExisted() {
        return existed;
    }

    public int getCurrentLine() {
        return currentLine;
    }

    public SVNWCContext.SVNEolStyle getEolStyle() {
        return eolStyle;
    }

    public String getEolStr() {
        return eolStr;
    }

    public List<Long> getLines() {
        return lines;
    }

    public List<SvnHunkInfo> getHunkInfos() {
        return hunkInfos;
    }

    public boolean isEof() {
        return eof;
    }

    public Map<String, byte[]> getKeywords() {
        return keywords;
    }

    public Object getReadBaton() {
        return readBaton;
    }

    public ISeekCallback getSeekCallback() {
        return seekCallback;
    }

    public ITellCallback getTellCallback() {
        return tellCallback;
    }

    public IRealLineCallback getReadLineCallback() {
        return readLineCallback;
    }

    public IWriteCallback getWriteCallback() {
        return writeCallback;
    }

    public void setExisted(boolean existed) {
        this.existed = existed;
    }

    public void setCurrentLine(int currentLine) {
        this.currentLine = currentLine;
    }

    public void setEolStyle(SVNWCContext.SVNEolStyle eolStyle) {
        this.eolStyle = eolStyle;
    }

    public void setEolStr(String eolStr) {
        this.eolStr = eolStr;
    }

    public void setLines(List<Long> lines) {
        this.lines = lines;
    }

    public void addHunkInfo(SvnHunkInfo hunkInfo) {
        this.hunkInfos.add(hunkInfo);
    }

    public void setEof(boolean eof) {
        this.eof = eof;
    }

    public void setKeywords(Map<String, byte[]> keywords) {
        this.keywords = keywords;
    }

    public void setReadLineCallback(IRealLineCallback readLineCallback) {
        this.readLineCallback = readLineCallback;
    }

    public void setTellCallback(ITellCallback tellCallback) {
        this.tellCallback = tellCallback;
    }

    public void setSeekCallback(ISeekCallback seekCallback) {
        this.seekCallback = seekCallback;
    }

    public void setWriteCallback(IWriteCallback writeCallback) {
        this.writeCallback = writeCallback;
    }

    public void setReadBaton(Object readBaton) {
        this.readBaton = readBaton;
    }

    public void setWriteBaton(Object writeBaton) {
        this.writeBaton = writeBaton;
    }

    public Object getWriteBaton() {
        return writeBaton;
    }

    private static class SymlinkBaton {

        private final File localAbsPath;

        public SymlinkBaton(File absPath) {
            this.localAbsPath = absPath;
        }
    }

    public static interface ISeekCallback {
        void seek(Object object, long offset) throws SVNException;
    }

    public static interface ITellCallback {
        long tell(Object readBaton) throws SVNException;
    }

    public static interface IRealLineCallback {
        String readLine(Object baton, String[] eolStr, boolean[] eof) throws SVNException;
    }

    public static interface IWriteCallback {
        void write(Object writeBaton, String s) throws SVNException;
    }
}
