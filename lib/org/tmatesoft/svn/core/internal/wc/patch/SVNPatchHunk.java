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

import java.io.IOException;
import java.util.Comparator;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.patch.SVNPatchFileStream.SVNPatchFileLineFilter;
import org.tmatesoft.svn.core.internal.wc.patch.SVNPatchFileStream.SVNPatchFileLineTransformer;

/**
 * A single hunk inside a patch.
 * 
 * @version 1.3
 * @author TMate Software Ltd.
 */
public class SVNPatchHunk {

    public static class SVNPatchHunkRange {

        private int start;

        private int length;

        public int getStart() {
            return start;
        }

        public int getLength() {
            return length;
        }

    }

    /**
     * Compare function for sorting hunks after parsing. We sort hunks by their
     * original line offset.
     */
    public static final Comparator COMPARATOR = new Comparator() {

        public int compare(Object a, Object b) {
            final SVNPatchHunk ha = (SVNPatchHunk) a;
            final SVNPatchHunk hb = (SVNPatchHunk) b;
            if (ha.original.start < hb.original.start)
                return -1;
            if (ha.original.start > hb.original.start)
                return 1;
            return 0;
        };
    };

    /**
     * A stream line-filter which allows only original text from a hunk, and
     * filters special lines (which start with a backslash).
     */
    private static final SVNPatchFileLineFilter original_line_filter = new SVNPatchFileLineFilter() {

        public boolean lineFilter(String line) {
            return (getChar(line,0) == '+' || getChar(line,0) == '\\');
        }
    };

    /**
     * A stream line-filter which allows only modified text from a hunk, and
     * filters special lines (which start with a backslash).
     */
    private static final SVNPatchFileLineFilter modified_line_filter = new SVNPatchFileLineFilter() {

        public boolean lineFilter(String line) {
            return (getChar(line,0) == '-' || getChar(line,0) == '\\');
        }
    };

    /** Line-transformer callback to shave leading diff symbols. */
    private static final SVNPatchFileLineTransformer remove_leading_char_transformer = new SVNPatchFileLineTransformer() {

        public String lineTransformer(String line) {
            if (getChar(line,0) == '+' || getChar(line,0) == '-' || getChar(line,0) == ' ') {
                return line.substring(1);
            }
            return line;
        }
    };

    /**
     * The hunk's unidiff text as it appeared in the patch file, without range
     * information.
     */
    private SVNPatchFileStream diffText;

    /**
     * The original and modified texts in the hunk range. Derived from the diff
     * text.
     * 
     * For example, consider a hunk such as:
     * 
     * <pre>
     *   @@ -1,5 +1,5 @@
     *    #include <stdio.h>
     *    int main(int argc, char *argv[])
     *    {
     *   -        printf("Hello World!\n");
     *   +        printf("I like Subversion!\n");
     *    }
     * </pre>
     * 
     * Then, the original text described by the hunk is:
     * 
     * <pre>
     *    #include <stdio.h>
     *   int main(int argc, char *argv[])
     *   {
     *           printf("Hello World!\n");
     *   }
     * </pre>
     * 
     * And the modified text described by the hunk is:
     * 
     * <pre>
     *   #include <stdio.h>
     *   int main(int argc, char *argv[])
     *   {
     *           printf("I like Subversion!\n");
     *   }
     * </pre>
     * 
     * Because these streams make use of line filtering and transformation, they
     * should only be read line-by-line with svn_stream_readline(). Reading them
     * with svn_stream_read() will not yield the expected result, because it
     * will return the unidiff text from the patch file unmodified. The streams
     * support resetting.
     */
    private SVNPatchFileStream originalText;
    private SVNPatchFileStream modifiedText;

    /**
     * Hunk ranges as they appeared in the patch file. All numbers are lines,
     * not bytes.
     */
    private SVNPatchHunkRange original = new SVNPatchHunkRange();
    private SVNPatchHunkRange modified = new SVNPatchHunkRange();

    /** Number of lines starting with ' ' before first '+' or '-'. */
    private long leadingContext;

    /** Number of lines starting with ' ' after last '+' or '-'. */
    private long trailingContext;

    public SVNPatchFileStream getDiffText() {
        return diffText;
    }

    public SVNPatchFileStream getOriginalText() {
        return originalText;
    }

    public SVNPatchFileStream getModifiedText() {
        return modifiedText;
    }

    public SVNPatchHunkRange getOriginal() {
        return original;
    }

    public SVNPatchHunkRange getModified() {
        return modified;
    }

    public long getLeadingContext() {
        return leadingContext;
    }

    public long getTrailingContext() {
        return trailingContext;
    }

    public void close() throws IOException {
        if (originalText != null) {
            originalText.close();
        }
        if (modifiedText != null) {
            modifiedText.close();
        }
        if (diffText != null) {
            diffText.close();
        }
    }

    /**
     * Return the next HUNK from a PATCH, using STREAM to read data from the
     * patch file. If no hunk can be found, set HUNK to NULL.
     * 
     * @throws IOException
     * @throws SVNException
     */
    public static SVNPatchHunk parseNextHunk(SVNPatch patch) throws IOException, SVNException {

        boolean eof, in_hunk, hunk_seen;
        long pos, last_line;
        long start, end;
        long original_lines;
        long leading_context;
        long trailing_context;
        boolean changed_line_seen;

        if (patch.getPatchFile().isEOF()) {
            /* No more hunks here. */
            return null;
        }

        in_hunk = false;
        hunk_seen = false;
        leading_context = 0;
        trailing_context = 0;
        changed_line_seen = false;

        start = 0;
        end = 0;
        original_lines = 0;

        SVNPatchHunk hunk = new SVNPatchHunk();

        /* Get current seek position */
        pos = patch.getPatchFile().getSeekPosition();

        final StringBuffer lineBuf = new StringBuffer();
        do {

            /* Remember the current line's offset, and read the line. */
            last_line = pos;

            lineBuf.setLength(0);
            eof = patch.getPatchFile().readLine(lineBuf);

            final String line = lineBuf.toString();

            if (!eof) {
                /* Update line offset for next iteration */
                pos = patch.getPatchFile().getSeekPosition();
            }

            /*
             * Lines starting with a backslash are comments, such as "\ No
             * newline at end of file".
             */
            if (getChar(line, 0) == '\\')
                continue;

            if (in_hunk) {
                char c;

                if (!hunk_seen) {
                    /*
                     * We're reading the first line of the hunk, so the start of
                     * the line just read is the hunk text's byte offset.
                     */
                    start = last_line;
                }

                c = getChar(line, 0);
                /* Tolerate chopped leading spaces on empty lines. */
                if (original_lines > 0 && (c == ' ' || (!eof && line.length() == 0))) {
                    hunk_seen = true;
                    original_lines--;
                    if (changed_line_seen)
                        trailing_context++;
                    else
                        leading_context++;
                } else if (c == '+' || c == '-') {
                    hunk_seen = true;
                    changed_line_seen = true;

                    /*
                     * A hunk may have context in the middle. We only want the
                     * last lines of context.
                     */
                    if (trailing_context > 0)
                        trailing_context = 0;

                    if (original_lines > 0 && c == '-')
                        original_lines--;
                } else {
                    in_hunk = false;

                    /*
                     * The start of the current line marks the first byte after
                     * the hunk text.
                     */
                    end = last_line;

                    break; /* Hunk was empty or has been read. */
                }
            } else {
                if (line.startsWith(SVNPatch.ATAT)) {
                    /*
                     * Looks like we have a hunk header, let's try to rip it
                     * apart.
                     */
                    in_hunk = parseHunkHeader(line, hunk);
                    if (in_hunk)
                        original_lines = hunk.original.length;
                } else if (line.startsWith(SVNPatch.MINUS))
                    /* This could be a header of another patch. Bail out. */
                    break;
            }
        } while (!eof);

        if (!eof) {
            /*
             * Rewind to the start of the line just read, so subsequent calls to
             * this function or svn_diff__parse_next_patch() don't end up
             * skipping the line -- it may contain a patch or hunk header.
             */
            patch.getPatchFile().setSeekPosition(last_line);
        }

        if (hunk_seen && start < end) {

            /* Create a stream which returns the hunk text itself. */
            SVNPatchFileStream diff_text = SVNPatchFileStream.openRangeReadOnly(patch.getPath(), start, end);

            /* Create a stream which returns the original hunk text. */
            SVNPatchFileStream original_text = SVNPatchFileStream.openRangeReadOnly(patch.getPath(), start, end);
            original_text.setLineFilter(original_line_filter);
            original_text.setLineTransformer(remove_leading_char_transformer);

            /* Create a stream which returns the modified hunk text. */

            SVNPatchFileStream modified_text = SVNPatchFileStream.openRangeReadOnly(patch.getPath(), start, end);
            modified_text.setLineFilter(modified_line_filter);
            modified_text.setLineTransformer(remove_leading_char_transformer);

            /* Set the hunk's texts. */
            hunk.diffText = diff_text;
            hunk.originalText = original_text;
            hunk.modifiedText = modified_text;
            hunk.leadingContext = leading_context;
            hunk.trailingContext = trailing_context;
        } else {
            /* Something went wrong, just discard the result. */
            return null;
        }

        return hunk;

    }

    private static char getChar(final String line, int i) {
        if (line != null && line.length() > 0 && i < line.length()) {
            return line.charAt(i);
        }
        return (char)0;
    }

    /**
     * Try to parse a hunk header in string HEADER, putting parsed information
     * into HUNK. Return TRUE if the header parsed correctly.
     */
    private static boolean parseHunkHeader(String header, SVNPatchHunk hunk) {

        int p = SVNPatch.ATAT.length();
        if (p >= header.length() || header.charAt(p) != ' ')
            /* No. */
            return false;
        p++;
        if (p >= header.length() || header.charAt(p) != '-')
            /* Nah... */
            return false;

        /* OK, this may be worth allocating some memory for... */
        StringBuffer range = new StringBuffer(31);
        p++;
        while (p < header.length() && header.charAt(p) != ' ') {
            range.append(header.charAt(p));
            p++;
        }
        if (p >= header.length() || header.charAt(p) != ' ')
            /* No no no... */
            return false;

        /* Try to parse the first range. */
        if (!parseRange(hunk.original, range))
            return false;

        /* Clear the stringbuf so we can reuse it for the second range. */
        range.setLength(0);
        p++;
        if (p >= header.length() || header.charAt(p) != '+')
            /* Eeek! */
            return false;
        /* OK, this may be worth copying... */
        p++;
        while (p < header.length() && header.charAt(p) != ' ') {
            range.append(header.charAt(p));
            p++;
        }
        if (p >= header.length() || header.charAt(p) != ' ')
            /* No no no... */
            return false;

        /* Check for trailing @@ */
        p++;
        if (p >= header.length() || !header.startsWith(SVNPatch.ATAT, p))
            return false;

        /*
         * There may be stuff like C-function names after the trailing @@, but
         * we ignore that.
         */

        /* Try to parse the second range. */
        if (!parseRange(hunk.modified, range))
            return false;

        /* Hunk header is good. */
        return true;

    }

    /**
     * Try to parse a hunk range specification from the string RANGE. Return
     * parsed information in START and LENGTH, and return TRUE if the range
     * parsed correctly. Note: This function may modify the input value RANGE.
     */
    private static boolean parseRange(SVNPatchHunkRange hunkRange, StringBuffer range) {

        int comma;

        if (range.length() == 0)
            return false;

        comma = range.indexOf(",");
        if (comma >= 0) {
            if ((comma + 1) < range.length()) {

                /* Try to parse the length. */
                final Integer offset = parseOffset(range.substring(comma + 1));
                if (offset == null) {
                    return false;
                }
                hunkRange.length = offset.intValue();

                /*
                 * Snip off the end of the string, so we can comfortably parse
                 * the line number the hunk starts at.
                 */
                range.setLength(comma);
            } else
                /* A comma but no length? */
                return false;
        } else {
            hunkRange.length = 1;
        }

        /* Try to parse the line number the hunk starts at. */
        final Integer offset = parseOffset(range.toString());
        if (offset == null) {
            return false;
        }
        hunkRange.start = offset.intValue();
        return true;

    }

    /**
     * Try to parse a positive number from a decimal number encoded in the
     * string NUMBER. Return parsed number in OFFSET, and return TRUE if parsing
     * was successful.
     */
    private static Integer parseOffset(String number) {
        if (number != null) {
            try {
                return Integer.valueOf(number);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

}
