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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.tmatesoft.svn.core.SVNException;

/**
 * Data type to manage parsing of patches.
 * 
 * @version 1.3
 * @author TMate Software Ltd.
 */
public class SVNPatch {

    public static final String MINUS = "--- ";
    public static final String PLUS = "+++ ";
    public static final String ATAT = "@@";

    /** Path to the patch file. */
    private File path;

    /** The patch file itself. */
    private SVNPatchFileStream patchFile;

    /**
     * The old and new file names as retrieved from the patch file. These paths
     * are UTF-8 encoded and canonicalized, but otherwise left unchanged from
     * how they appeared in the patch file.
     */
    private File oldFilename;
    private File newFilename;

    /**
     * An array containing an svn_hunk_t object for each hunk parsed from the
     * patch.
     */
    private List hunks;

    public File getPath() {
        return path;
    }

    public SVNPatchFileStream getPatchFile() {
        return patchFile;
    }

    public File getOldFilename() {
        return oldFilename;
    }

    public File getNewFilename() {
        return newFilename;
    }

    public List getHunks() {
        return hunks;
    }

    public void close() throws IOException {
        if (hunks != null) {
            int hunksCount = hunks.size();
            if (hunksCount > 0) {
                for (int i = 0; i < hunksCount; i++) {
                    final SVNPatchHunk hunk = (SVNPatchHunk) hunks.get(i);
                    hunk.close();
                }
            }
        }
    }

    /**
     * Return the next PATCH in PATCH_FILE.
     * 
     * If no patch can be found, set PATCH to NULL.
     * 
     * @throws SVNException
     * @throws IOException
     */
    public static SVNPatch parseNextPatch(SVNPatchFileStream patchFile) throws SVNException, IOException {

        if (patchFile.isEOF()) {
            /* No more patches here. */
            return null;
        }

        /* Get the patch's filename. */
        final File patchPath = patchFile.getPath();

        /* Record what we already know about the patch. */
        final SVNPatch patch = new SVNPatch();
        patch.patchFile = patchFile;
        patch.path = patchPath;

        String indicator = MINUS;
        boolean eof = false, in_header = false;
        final StringBuffer lineBuf = new StringBuffer();
        do {

            lineBuf.setLength(0);

            /* Read a line from the stream. */
            eof = patchFile.readLine(lineBuf);
            final String line = lineBuf.toString();

            /* See if we have a diff header. */
            if (!eof && line.length() > indicator.length() && line.startsWith(indicator)) {
                /*
                 * If we can find a tab, it separates the filename from the rest
                 * of the line which we can discard.
                 */
                final int tab = line.indexOf('\t');
                final File filePath = new File(line.substring(indicator.length(), tab > 0 ? tab : line.length()));

                if ((!in_header) && MINUS.equals(indicator)) {
                    /* First line of header contains old filename. */
                    patch.oldFilename = filePath;
                    indicator = PLUS;
                    in_header = true;
                } else if (in_header && PLUS.equals(indicator)) {
                    /* Second line of header contains new filename. */
                    patch.newFilename = filePath;
                    in_header = false;
                    break; /* All good! */
                } else {
                    in_header = false;
                }
            }
        } while (!eof);

        if (patch.oldFilename == null || patch.newFilename == null) {
            /* Something went wrong, just discard the result. */
            return null;
        }
        /* Parse hunks. */
        patch.hunks = new ArrayList(10);
        SVNPatchHunk hunk;
        do {
            hunk = SVNPatchHunk.parseNextHunk(patch);
            if (hunk != null) {
                patch.hunks.add(hunk);
            }
        } while (hunk != null);

        /*
         * Usually, hunks appear in the patch sorted by their original line
         * offset. But just in case they weren't parsed in this order for some
         * reason, we sort them so that our caller can assume that hunks are
         * sorted as if parsed from a usual patch.
         */
        Collections.sort(patch.hunks, SVNPatchHunk.COMPARATOR);

        return patch;
    }

}
