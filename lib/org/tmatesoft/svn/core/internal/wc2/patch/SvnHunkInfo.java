package org.tmatesoft.svn.core.internal.wc2.patch;

public class SvnHunkInfo {

    private SvnDiffHunk hunk;
    private int matchedLine;
    private boolean rejected;
    boolean alreadyApplied;
    private int fuzz;

    public SvnHunkInfo(SvnDiffHunk hunk, int matchedLine, boolean rejected, boolean alreadyApplied, int fuzz) {
        this.hunk = hunk;
        this.matchedLine = matchedLine;
        this.rejected = rejected;
        this.alreadyApplied = alreadyApplied;
        this.fuzz = fuzz;
    }

    public SvnDiffHunk getHunk() {
        return hunk;
    }

    public void setHunk(SvnDiffHunk hunk) {
        this.hunk = hunk;
    }

    public int getMatchedLine() {
        return matchedLine;
    }

    public void setMatchedLine(int matchedLine) {
        this.matchedLine = matchedLine;
    }

    public boolean isRejected() {
        return rejected;
    }

    public void setRejected(boolean rejected) {
        this.rejected = rejected;
    }

    public boolean isAlreadyApplied() {
        return alreadyApplied;
    }

    public void setAlreadyApplied(boolean alreadyApplied) {
        this.alreadyApplied = alreadyApplied;
    }

    public int getFuzz() {
        return fuzz;
    }

    public void setFuzz(int fuzz) {
        this.fuzz = fuzz;
    }
}
