/*
 * ====================================================================
 * Copyright (c) 2004-2010 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.wc.patch;

/**
 * @version 1.3
 * @author TMate Software Ltd.
 */
public class SVNPatchHunkInfo {

    private SVNPatchHunk hunk;
    private boolean rejected;
    private int matchedLine;
    private int fuzz;

    public SVNPatchHunkInfo(SVNPatchHunk hunk, int matchedLine, boolean rejected, int fuzz) {
        this.hunk = hunk;
        this.matchedLine = matchedLine;
        this.rejected = rejected;
        this.fuzz = fuzz;
    }

    public boolean isRejected() {
        return rejected;
    }

    public SVNPatchHunk getHunk() {
        return hunk;
    }

    public int getMatchedLine() {
        return matchedLine;
    }

    public int getFuzz() {
        return fuzz;
    }

}
