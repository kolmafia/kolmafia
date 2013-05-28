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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.SVNStatusUtil;
import org.tmatesoft.svn.core.internal.wc.SVNWCManager;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminArea;
import org.tmatesoft.svn.core.internal.wc.admin.SVNEntry;
import org.tmatesoft.svn.core.internal.wc.admin.SVNTranslator;
import org.tmatesoft.svn.core.internal.wc.admin.SVNVersionedProperties;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author TMate Software Ltd.
 */
public class SVNPatchTarget {

    private static final int MAX_FUZZ = 2;

    private SVNPatch patch;
    private List lines = new ArrayList();
    private List hunks = new ArrayList();;

    private boolean localMods;
    private boolean executable;
    private boolean skipped;
    private String eolStr;
    private Map keywords;
    private String eolStyle;
    private SVNNodeKind kind;
    private int currentLine;
    private boolean modified;
    private boolean hadRejects;
    private boolean deleted;
    private boolean eof;
    private boolean added;

    private File absPath;
    private File relPath;
    private File canonPathFromPatchfile;

    private RandomAccessFile file;
    private SVNPatchFileStream stream;

    private File patchedPath;
    private OutputStream patchedRaw;
    private OutputStream patched;
    private File rejectPath;
    private SVNPatchFileStream reject;
    private boolean parentDirExists;

    private SVNPatchTarget() {
    }

    public boolean isLocalMods() {
        return localMods;
    }

    public String getEolStr() {
        return eolStr;
    }

    public Map getKeywords() {
        return keywords;
    }

    public String getEolStyle() {
        return eolStyle;
    }

    public RandomAccessFile getFile() {
        return file;
    }

    public OutputStream getPatchedRaw() {
        return patchedRaw;
    }

    public File getCanonPathFromPatchfile() {
        return canonPathFromPatchfile;
    }

    public SVNPatch getPatch() {
        return patch;
    }

    public int getCurrentLine() {
        return currentLine;
    }

    public boolean isModified() {
        return modified;
    }

    public boolean isEof() {
        return eof;
    }

    public List getLines() {
        return lines;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public List getHunks() {
        return hunks;
    }

    public SVNNodeKind getKind() {
        return kind;
    }

    public SVNPatchFileStream getStream() {
        return stream;
    }

    public OutputStream getPatched() {
        return patched;
    }

    public SVNPatchFileStream getReject() {
        return reject;
    }

    public File getPatchedPath() {
        return patchedPath;
    }

    public boolean isAdded() {
        return added;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isExecutable() {
        return executable;
    }

    public File getRejectPath() {
        return rejectPath;
    }

    public File getAbsPath() {
        return absPath;
    }

    public File getRelPath() {
        return relPath;
    }

    public boolean isHadRejects() {
        return hadRejects;
    }

    public boolean isParentDirExists() {
        return parentDirExists;
    }

    /**
     * Attempt to initialize a patch TARGET structure for a target file
     * described by PATCH. Use client context CTX to send notifiations and
     * retrieve WC_CTX. STRIP_COUNT specifies the number of leading path
     * components which should be stripped from target paths in the patch. Upon
     * success, return the patch target structure. Else, return NULL.
     * 
     * @throws SVNException
     * @throws IOException
     */
    public static SVNPatchTarget initPatchTarget(SVNPatch patch, File baseDir, int stripCount, SVNAdminArea wc) throws SVNException, IOException {

        final SVNPatchTarget new_target = new SVNPatchTarget();
        new_target.resolveTargetPath(patch.getNewFilename(), baseDir, stripCount, wc);

        new_target.localMods = false;
        new_target.executable = false;

        if (!new_target.skipped) {

            final String nativeEOLMarker = SVNFileUtil.getNativeEOLMarker(wc.getWCAccess().getOptions());
            new_target.eolStr = nativeEOLMarker;
            new_target.keywords = null;
            new_target.eolStyle = null;

            if (new_target.kind == SVNNodeKind.FILE) {

                /* Open the file. */
                new_target.file = SVNFileUtil.openRAFileForReading(new_target.absPath);
                /* Create a stream to read from the target. */
                new_target.stream = SVNPatchFileStream.openReadOnly(new_target.absPath);

                /* Handle svn:keyword and svn:eol-style properties. */
                SVNVersionedProperties props = wc.getProperties(new_target.absPath.getAbsolutePath());
                String keywords_val = props.getStringPropertyValue(SVNProperty.KEYWORDS);
                if (null != keywords_val) {
                    SVNEntry entry = wc.getEntry(new_target.absPath.getAbsolutePath(), false);
                    long changed_rev = entry.getRevision();
                    String author = entry.getAuthor();
                    String changed_date = entry.getCommittedDate();
                    String url = entry.getURL();
                    String rev_str = Long.toString(changed_rev);
                    new_target.keywords = SVNTranslator.computeKeywords(keywords_val, url, author, changed_date, rev_str, wc.getWCAccess().getOptions());
                }

                String eol_style_val = props.getStringPropertyValue(SVNProperty.EOL_STYLE);
                if (null != eol_style_val) {
                    new_target.eolStyle = new String(SVNTranslator.getEOL(eol_style_val, wc.getWCAccess().getOptions()));
                } else {
                    /* Just use the first EOL sequence we can find in the file. */
                    new_target.eolStr = detectFileEOL(new_target.file);
                    /* But don't enforce any particular EOL-style. */
                    new_target.eolStyle = null;
                }

                if (new_target.eolStyle == null) {
                    /*
                     * We couldn't figure out the target files's EOL scheme,
                     * just use native EOL makers.
                     */
                    new_target.eolStr = nativeEOLMarker;
                    new_target.eolStyle = SVNProperty.EOL_STYLE_NATIVE;
                }

                /* Also check the file for local mods and the Xbit. */
                new_target.localMods = wc.hasTextModifications(new_target.absPath.getAbsolutePath(), false);
                new_target.executable = SVNFileUtil.isExecutable(new_target.absPath);

            }

            /*
             * Create a temporary file to write the patched result to. Expand
             * keywords in the patched file.
             */
            new_target.patchedPath = SVNFileUtil.createTempFile("", null);
            new_target.patchedRaw = SVNFileUtil.openFileForWriting(new_target.patchedPath);
            new_target.patched = SVNTranslator.getTranslatingOutputStream(new_target.patchedRaw, null, new_target.eolStr.getBytes(), new_target.eolStyle != null, new_target.keywords, true);

            /*
             * We'll also need a stream to write rejected hunks to. We don't
             * expand keywords, nor normalise line-endings, in reject files.
             */
            new_target.rejectPath = SVNFileUtil.createTempFile("", null);
            new_target.reject = SVNPatchFileStream.openForWrite(new_target.rejectPath);

            /* The reject stream needs a diff header. */
            String diff_header = "--- " + new_target.canonPathFromPatchfile + nativeEOLMarker + "+++ " + new_target.canonPathFromPatchfile + nativeEOLMarker;

            new_target.reject.write(diff_header);

        }

        new_target.patch = patch;
        new_target.currentLine = 1;
        new_target.modified = false;
        new_target.hadRejects = false;
        new_target.deleted = false;
        new_target.eof = false;
        new_target.lines = new ArrayList();
        new_target.hunks = new ArrayList();

        return new_target;

    }

    /**
     * Detect the EOL marker used in file and return it. If it cannot be
     * detected, return NULL.
     * 
     * The file is searched starting at the current file cursor position. The
     * first EOL marker found will be returnd. So if the file has inconsistent
     * EOL markers, this won't be detected.
     * 
     * Upon return, the original file cursor position is always preserved, even
     * if an error is thrown.
     */
    private static String detectFileEOL(RandomAccessFile file) throws IOException {
        /* Remember original file offset. */
        final long pos = file.getFilePointer();
        try {
            final BufferedInputStream stream = new BufferedInputStream(new FileInputStream(file.getFD()));
            final StringBuffer buf = new StringBuffer();
            int b1;
            while ((b1 = stream.read()) > 0) {
                final char c1 = (char) b1;
                if (c1 == '\n' || c1 == '\r') {
                    buf.append(c1);
                    if (c1 == '\r') {
                        final int b2 = stream.read();
                        if (b2 > 0) {
                            final char c2 = (char) b2;
                            if (c2 == '\n') {
                                buf.append(c2);
                            }
                        }
                    }
                    return buf.toString();
                }
            }
        } finally {
            file.seek(pos);
        }
        return null;
    }

    /**
     * Resolve the exact path for a patch TARGET at path PATH_FROM_PATCHFILE,
     * which is the path of the target as it appeared in the patch file. Put a
     * canonicalized version of PATH_FROM_PATCHFILE into
     * TARGET->CANON_PATH_FROM_PATCHFILE. WC_CTX is a context for the working
     * copy the patch is applied to. If possible, determine TARGET->WC_PATH,
     * TARGET->ABS_PATH, TARGET->KIND, TARGET->ADDED, and
     * TARGET->PARENT_DIR_EXISTS. Indicate in TARGET->SKIPPED whether the target
     * should be skipped. STRIP_COUNT specifies the number of leading path
     * components which should be stripped from target paths in the patch.
     * 
     * @throws SVNException
     * @throws IOException
     */
    private void resolveTargetPath(File pathFromPatchfile, File absWCPath, int stripCount, SVNAdminArea wc) throws SVNException, IOException {

        final SVNPatchTarget target = this;

        target.canonPathFromPatchfile = pathFromPatchfile;

        if ("".equals(target.canonPathFromPatchfile.getPath())) {
            /* An empty patch target path? What gives? Skip this. */
            target.skipped = true;
            target.kind = SVNNodeKind.FILE;
            target.absPath = null;
            target.relPath = null;
            return;
        }

        File stripped_path;
        if (stripCount > 0) {
            stripped_path = stripPath(target.canonPathFromPatchfile, stripCount);
        } else {
            stripped_path = target.canonPathFromPatchfile;
        }

        if (stripped_path.isAbsolute()) {

            target.relPath = getChildPath(absWCPath, stripped_path);

            if (null == target.relPath) {
                /*
                 * The target path is either outside of the working copy or it
                 * is the working copy itself. Skip it.
                 */
                target.skipped = true;
                target.kind = SVNNodeKind.FILE;
                target.absPath = null;
                target.relPath = stripped_path;
                return;
            }
        } else {
            target.relPath = stripped_path;
        }

        /*
         * Make sure the path is secure to use. We want the target to be inside
         * of the working copy and not be fooled by symlinks it might contain.
         */
        if (!isChildPath(absWCPath, target.relPath)) {
            /* The target path is outside of the working copy. Skip it. */
            target.skipped = true;
            target.kind = SVNNodeKind.FILE;
            target.absPath = null;
            return;
        }

        target.absPath = new File(absWCPath, target.relPath.getPath());

        /* Skip things we should not be messing with. */

        final SVNStatus status = SVNStatusUtil.getStatus(target.absPath, wc.getWCAccess());
        final SVNStatusType contentsStatus = status.getContentsStatus();

        if (contentsStatus == SVNStatusType.STATUS_UNVERSIONED || contentsStatus == SVNStatusType.STATUS_IGNORED || contentsStatus == SVNStatusType.STATUS_OBSTRUCTED) {
            target.skipped = true;
            target.kind = SVNFileType.getNodeKind(SVNFileType.getType(target.absPath));
            return;
        }

        target.kind = status.getKind();

        if (SVNNodeKind.FILE.equals(target.kind)) {

            target.added = false;
            target.parentDirExists = true;

        } else if (SVNNodeKind.NONE.equals(target.kind) || SVNNodeKind.UNKNOWN.equals(target.kind)) {

            /*
             * The file is not there, that's fine. The patch might want to
             * create it. Check if the containing directory of the target
             * exists. We may need to create it later.
             */
            target.added = true;
            File absDirname = target.absPath.getParentFile();

            final SVNStatus status2 = SVNStatusUtil.getStatus(absDirname, wc.getWCAccess());
            final SVNStatusType contentsStatus2 = status2.getContentsStatus();
            SVNNodeKind kind = status2.getKind();
            target.parentDirExists = (kind == SVNNodeKind.DIR && contentsStatus2 != SVNStatusType.STATUS_DELETED && contentsStatus2 != SVNStatusType.STATUS_MISSING);

        } else {
            target.skipped = true;
        }

        return;
    }

    private boolean isChildPath(final File baseFile, final File file) throws IOException {
        if (null != file && baseFile != null) {
            final String basePath = baseFile.getCanonicalPath();
            final File childFile = new File(basePath, file.getPath());
            final String childPath = childFile.getCanonicalPath();
            return childPath.startsWith(basePath) && childPath.length() > basePath.length();
        }
        return false;
    }

    private File getChildPath(File basePath, File childPath) throws IOException {
        if (null != childPath && basePath != null) {
            final String base = basePath.getCanonicalPath();
            final String child = childPath.getCanonicalPath();
            if (child.startsWith(base) && child.length() > base.length()) {
                String substr = child.substring(base.length());
                File subPath = new File(substr);
                if (!subPath.isAbsolute()) {
                    return subPath;
                }
                if (substr.length() > 1) {
                    substr = substr.substring(1);
                    subPath = new File(substr);
                    if (!subPath.isAbsolute()) {
                        return subPath;
                    }
                }
            }
        }
        return null;
    }

    private File stripPath(File path, int stripCount) {
        if (path != null && stripCount > 0) {
            final String[] components = decomposePath(path);
            final StringBuffer buf = new StringBuffer();
            if (stripCount > components.length) {
                for (int i = stripCount; i < components.length; i++) {
                    if (i > stripCount) {
                        buf.append(File.pathSeparator);
                    }
                    buf.append(components[i]);
                }
                return new File(buf.toString());
            }
        }
        return path;
    }

    /**
     * Write the diff text of the hunk described by HI to the reject stream of
     * TARGET, and mark TARGET as having had rejects.
     * 
     * @throws IOException
     * @throws SVNException
     */
    public void rejectHunk(final SVNPatchHunkInfo hi) throws SVNException, IOException {

        final SVNPatchTarget target = this;
        final SVNPatchHunk hunk = hi.getHunk();

        final StringBuffer hunk_header = new StringBuffer();
        hunk_header.append("@@");
        hunk_header.append(" -").append(hunk.getOriginal().getStart()).append(",").append(hunk.getOriginal().getLength());
        hunk_header.append(" +").append(hunk.getModified().getStart()).append(",").append(hunk.getModified().getLength());
        hunk_header.append(" ").append(target.eolStr);

        target.reject.write(hunk_header);

        boolean eof;
        final StringBuffer hunk_line = new StringBuffer();
        final StringBuffer eol_str = new StringBuffer();
        do {
            hunk_line.setLength(0);
            eol_str.setLength(0);

            eof = hunk.getDiffText().readLineWithEol(hunk_line, eol_str);

            if (!eof) {
                if (hunk_line.length() > 0) {
                    target.reject.tryWrite(hunk_line);
                }
                if (eol_str.length() > 0) {
                    target.reject.tryWrite(eol_str);
                }
            }
        } while (!eof);

        target.hadRejects = true;

    }

    /**
     * Write the modified text of hunk described by HI to the patched stream of
     * TARGET.
     * 
     * @throws SVNException
     * @throws IOException
     */
    public void applyHunk(final SVNPatchHunkInfo hi) throws SVNException, IOException {

        final SVNPatchTarget target = this;
        final SVNPatchHunk hunk = hi.getHunk();

        if (target.kind == SVNNodeKind.FILE) {
            /*
             * Move forward to the hunk's line, copying data as we go. Also copy
             * leading lines of context which matched with fuzz. The target has
             * changed on the fuzzy-matched lines, so we should retain the
             * target's version of those lines.
             */
            target.copyLinesToTarget(hi.getMatchedLine() + hi.getFuzz());

            /*
             * Skip the target's version of the hunk. Don't skip trailing lines
             * which matched with fuzz.
             */
            target.seekToLine(target.getCurrentLine() + hunk.getOriginal().getLength() - (2 * hi.getFuzz()));
        }

        /*
         * Write the hunk's version to the patched result. Don't write the lines
         * which matched with fuzz.
         */
        long lines_read = 0;
        boolean eof = false;

        final StringBuffer hunk_line = new StringBuffer();
        final StringBuffer eol_str = new StringBuffer();
        do {

            eof = hunk.getModifiedText().readLineWithEol(hunk_line, eol_str);

            lines_read++;

            if (!eof && lines_read > hi.getFuzz() && lines_read <= hunk.getModified().getLength() - hi.getFuzz()) {
                if (hunk_line.length() > 0) {
                    tryWrite(target.getPatched(), hunk_line);
                }
                if (eol_str.length() > 0) {
                    tryWrite(target.getPatched(), eol_str);
                }
            }
        } while (!eof);

    }

    /**
     * Seek to the specified LINE in TARGET. Mark any lines not read before in
     * TARGET->LINES.
     * 
     * @throws SVNException
     * @throws IOException
     */
    public void seekToLine(int line) throws SVNException, IOException {

        if (line <= 0) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ASSERTION_FAIL, "Line to seek must be more than zero");
            SVNErrorManager.error(err, Level.FINE, SVNLogType.WC);
        }

        final SVNPatchTarget target = this;

        if (line == target.currentLine) {
            return;
        }

        if (line <= target.lines.size()) {
            final Long mark = (Long) target.lines.get(line - 1);
            target.stream.setSeekPosition(mark.longValue());
            target.currentLine = line;
        } else {
            final StringBuffer dummy = new StringBuffer();

            while (target.currentLine < line) {
                target.readLine(dummy);
            }
        }

    }

    /**
     * Read a *LINE from TARGET. If the line has not been read before mark the
     * line in TARGET->LINES.
     * 
     * @throws SVNException
     * @throws IOException
     */
    public void readLine(final StringBuffer line) throws SVNException, IOException {

        final SVNPatchTarget target = this;

        if (target.eof) {
            return;
        }

        if (target.currentLine > target.lines.size() + 1) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ASSERTION_FAIL, "Lines reading isn't sequenced");
            SVNErrorManager.error(err, Level.FINE, SVNLogType.WC);
        }

        if (target.currentLine == target.lines.size() + 1) {
            final Long mark = new Long(target.stream.getSeekPosition());
            target.lines.add(mark);
        }

        final StringBuffer line_raw = new StringBuffer();
        target.eof = target.stream.readLine(line_raw, target.eolStr);

        /* Contract keywords. */
        final byte[] eol = target.eolStr.getBytes(); // TODO EOL bytes
        line.append(SVNTranslator.translateString(line_raw.toString(), eol, target.keywords, false, false));

        target.currentLine++;

    }

    /**
     * Copy lines to the patched stream until the specified LINE has been
     * reached. Indicate in *EOF whether end-of-file was encountered while
     * reading from the target. If LINE is zero, copy lines until end-of-file
     * has been reached.
     * 
     * @throws IOException
     */
    public void copyLinesToTarget(int line) throws SVNException, IOException {

        final SVNPatchTarget target = this;

        while ((target.currentLine < line || line == 0) && !target.eof) {
            final StringBuffer target_line = new StringBuffer();

            target.readLine(target_line);

            if (!target.eof) {
                target_line.append(target.eolStr);
            }

            tryWrite(target.patched, target_line);
        }

    }

    /**
     * Install a patched TARGET into the working copy at ABS_WC_PATH. Use client
     * context CTX to retrieve WC_CTX, and possibly doing notifications. If
     * DRY_RUN is TRUE, don't modify the working copy.
     * 
     * @throws SVNException
     */
    public void installPatchedTarget(File absWCPath, boolean dryRun, SVNAdminArea wc) throws SVNException {

        final SVNPatchTarget target = this;

        if (target.deleted) {
            if (!dryRun) {
                /*
                 * Schedule the target for deletion. Suppress notification,
                 * we'll do it manually in a minute. Also suppress cancellation.
                 */
                SVNWCManager.delete(wc.getWCAccess(), wc, target.getAbsPath(), false, false);
            }
        } else {
            /*
             * If the target's parent directory does not yet exist we need to
             * create it before we can copy the patched result in place.
             */
            if (target.isAdded() && !target.isParentDirExists()) {

                /* Check if we can safely create the target's parent. */
                File absPath = absWCPath;
                String[] components = decomposePath(target.getRelPath());
                int present_components = 0;
                for (int i = 0; i < components.length - 1; i++) {
                    final String component = components[i];
                    absPath = new File(absPath, component);

                    final SVNEntry entry = wc.getWCAccess().getEntry(absPath, false);
                    final SVNNodeKind kind = entry != null ? entry.getKind() : null;

                    if (kind == SVNNodeKind.FILE) {
                        /* Obstructed. */
                        target.skipped = true;
                        break;
                    } else if (kind == SVNNodeKind.DIR) {
                        /*
                         * ### wc-ng should eventually be able to replace
                         * directories in-place, so this schedule conflict check
                         * will go away. We could then also make the
                         * svn_wc__node_get_kind() call above ignore hidden
                         * nodes.
                         */
                        if (entry.isDeleted()) {
                            target.skipped = true;
                            break;
                        }

                        present_components++;
                    } else {
                        /*
                         * The WC_DB doesn't know much about this node. Check
                         * what's on disk.
                         */
                        final SVNFileType disk_kind = SVNFileType.getType(absPath);
                        if (disk_kind != SVNFileType.NONE) {
                            /* An unversioned item is in the way. */
                            target.skipped = true;
                            break;
                        }
                    }
                }

                if (!target.isSkipped()) {
                    absPath = absWCPath;
                    for (int i = 0; i < present_components; i++) {
                        final String component = components[i];
                        absPath = new File(absPath, component);
                        if (dryRun) {
                            /* Just do notification. */
                            SVNEvent mergeCompletedEvent = SVNEventFactory.createSVNEvent(absPath, SVNNodeKind.NONE, null, SVNRepository.INVALID_REVISION, SVNStatusType.INAPPLICABLE,
                                    SVNStatusType.INAPPLICABLE, SVNStatusType.LOCK_INAPPLICABLE, SVNEventAction.ADD, null, null, null);
                            wc.getWCAccess().handleEvent(mergeCompletedEvent);
                        } else {
                            /*
                             * Create the missing component and add it to
                             * version control. Suppress cancellation.
                             */
                            if (absPath.mkdirs()) {
                                SVNWCManager.add(absPath, wc, null, SVNRepository.INVALID_REVISION, SVNDepth.INFINITY);
                            }
                        }
                    }
                }
            }

            if (!dryRun && !target.isSkipped()) {
                /* Copy the patched file on top of the target file. */
                SVNFileUtil.copyFile(target.getPatchedPath(), target.getAbsPath(), false);
                if (target.isAdded()) {
                    /*
                     * The target file didn't exist previously, so add it to
                     * version control. Suppress notification, we'll do that
                     * later. Also suppress cancellation.
                     */
                    SVNWCManager.add(target.getAbsPath(), wc, null, SVNRepository.INVALID_REVISION, SVNDepth.INFINITY);
                }

                /* Restore the target's executable bit if necessary. */
                SVNFileUtil.setExecutable(target.getAbsPath(), target.isExecutable());
            }
        }

        /* Write out rejected hunks, if any. */
        if (!dryRun && !target.skipped && target.hadRejects) {
            final String rej_path = target.getAbsPath().getPath() + ".svnpatch.rej";
            SVNFileUtil.copyFile(target.getRejectPath(), new File(rej_path), true);
            /* ### TODO mark file as conflicted. */
        }

    }

    public static String[] decomposePath(File path) {
        return SVNAdminArea.fromString(path.getPath(), File.pathSeparator);
    }

    /**
     * Apply a PATCH to a working copy at ABS_WC_PATH.
     * 
     * STRIP_COUNT specifies the number of leading path components which should
     * be stripped from target paths in the patch.
     * 
     * @throws SVNException
     * @throws IOException
     */
    public static SVNPatchTarget applyPatch(SVNPatch patch, File absWCPath, int stripCount, SVNAdminArea wc) throws SVNException, IOException {

        final SVNPatchTarget target = SVNPatchTarget.initPatchTarget(patch, absWCPath, stripCount, wc);

        if (target.skipped) {
            return target;
        }

        /* Match hunks. */
        for (final Iterator i = patch.getHunks().iterator(); i.hasNext();) {
            final SVNPatchHunk hunk = (SVNPatchHunk) i.next();

            SVNPatchHunkInfo hi;
            int fuzz = 0;

            /*
             * Determine the line the hunk should be applied at. If no match is
             * found initially, try with fuzz.
             */
            do {
                hi = target.getHunkInfo(hunk, fuzz);
                fuzz++;
            } while (hi.isRejected() && fuzz <= MAX_FUZZ);

            target.hunks.add(hi);
        }

        /* Apply or reject hunks. */
        for (final Iterator i = target.hunks.iterator(); i.hasNext();) {
            final SVNPatchHunkInfo hi = (SVNPatchHunkInfo) i.next();

            if (hi.isRejected()) {
                target.rejectHunk(hi);
            } else {
                target.applyHunk(hi);
            }
        }

        if (target.kind == SVNNodeKind.FILE) {
            /* Copy any remaining lines to target. */
            target.copyLinesToTarget(0);
            if (!target.eof) {
                /*
                 * We could not copy the entire target file to the temporary
                 * file, and would truncate the target if we copied the
                 * temporary file on top of it. Cancel any modifications to the
                 * target file and report is as skipped.
                 */
                target.skipped = true;
            }
        }

        /*
         * Close the streams of the target so that their content is flushed to
         * disk. This will also close underlying streams.
         */
        if (target.getKind() == SVNNodeKind.FILE) {
            target.stream.close();
        }
        target.patched.close();
        target.reject.close();

        if (!target.skipped) {

            /*
             * Get sizes of the patched temporary file and the working file.
             * We'll need those to figure out whether we should add or delete
             * the patched file.
             */
            final long patchedFileSize = target.patchedPath.length();
            final long workingFileSize = target.kind == SVNNodeKind.FILE ? target.absPath.length() : 0;

            if (patchedFileSize == 0 && workingFileSize > 0) {
                /*
                 * If a unidiff removes all lines from a file, that usually
                 * means deletion, so we can confidently schedule the target for
                 * deletion. In the rare case where the unidiff was really meant
                 * to replace a file with an empty one, this may not be
                 * desirable. But the deletion can easily be reverted and
                 * creating an empty file manually is not exactly hard either.
                 */
                target.deleted = target.kind != SVNNodeKind.NONE;
            } else if (workingFileSize == 0 && patchedFileSize == 0) {
                /*
                 * The target was empty or non-existent to begin with and
                 * nothing has changed by patching. Report this as skipped if it
                 * didn't exist.
                 */
                if (target.kind != SVNNodeKind.FILE)
                    target.skipped = true;
            } else if (target.kind != SVNNodeKind.FILE && patchedFileSize > 0) {
                /* The patch has created a file. */
                target.added = true;
            }

        }

        return target;

    }

    /**
     * Determine the line at which a HUNK applies to the TARGET file, and return
     * an appropriate hunk_info object in *HI, allocated from RESULT_POOL. Use
     * fuzz factor FUZZ. Set HI->FUZZ to FUZZ. If no correct line can be
     * determined, set HI->REJECTED to TRUE. When this function returns, neither
     * TARGET->CURRENT_LINE nor the file offset in the target file will have
     * changed.
     * 
     * @throws SVNException
     * @throws IOException
     */
    public SVNPatchHunkInfo getHunkInfo(final SVNPatchHunk hunk, final int fuzz) throws SVNException, IOException {

        final SVNPatchTarget target = this;

        int matchedLine;

        /*
         * An original offset of zero means that this hunk wants to create a new
         * file. Don't bother matching hunks in that case, since the hunk
         * applies at line 1. If the file already exists, the hunk is rejected.
         */
        if (hunk.getOriginal().getStart() == 0) {
            if (target.getKind() == SVNNodeKind.FILE) {
                matchedLine = 0;
            } else {
                matchedLine = 1;
            }
        } else if (hunk.getOriginal().getStart() > 0 && target.getKind() == SVNNodeKind.FILE) {

            int savedLine = target.getCurrentLine();
            boolean savedEof = target.isEof();

            /*
             * Scan for a match at the line where the hunk thinks it should be
             * going.
             */
            target.seekToLine(hunk.getOriginal().getStart());
            matchedLine = target.scanForMatch(hunk, true, hunk.getOriginal().getStart() + 1, fuzz);

            if (matchedLine != hunk.getOriginal().getStart()) {

                /* Scan the whole file again from the start. */
                target.seekToLine(1);

                /*
                 * Scan forward towards the hunk's line and look for a line
                 * where the hunk matches.
                 */
                matchedLine = target.scanForMatch(hunk, false, hunk.getOriginal().getStart(), fuzz);

                /*
                 * In tie-break situations, we arbitrarily prefer early matches
                 * to save us from scanning the rest of the file.
                 */
                if (matchedLine == 0) {
                    /*
                     * Scan forward towards the end of the file and look for a
                     * line where the hunk matches.
                     */
                    matchedLine = target.scanForMatch(hunk, true, 0, fuzz);

                }
            }

            target.seekToLine(savedLine);
            target.eof = savedEof;

        } else {
            /* The hunk wants to modify a file which doesn't exist. */
            matchedLine = 0;
        }

        return new SVNPatchHunkInfo(hunk, matchedLine, (matchedLine == 0), fuzz);
    }

    /**
     * Scan lines of TARGET for a match of the original text of HUNK, up to but
     * not including the specified UPPER_LINE. Use fuzz factor FUZZ. If
     * UPPER_LINE is zero scan until EOF occurs when reading from TARGET. Return
     * the line at which HUNK was matched in *MATCHED_LINE. If the hunk did not
     * match at all, set *MATCHED_LINE to zero. If the hunk matched multiple
     * times, and MATCH_FIRST is TRUE, return the line number at which the first
     * match occured in *MATCHED_LINE. If the hunk matched multiple times, and
     * MATCH_FIRST is FALSE, return the line number at which the last match
     * occured in *MATCHED_LINE.
     * 
     * @throws SVNException
     * @throws IOException
     */
    public int scanForMatch(SVNPatchHunk hunk, boolean matchFirst, int upperLine, int fuzz) throws SVNException, IOException {

        final SVNPatchTarget target = this;

        int matched_line = 0;

        while ((target.currentLine < upperLine || upperLine == 0) && !target.eof) {

            boolean matched = target.matchHunk(hunk, fuzz);

            if (matched) {
                boolean taken = false;

                /* Don't allow hunks to match at overlapping locations. */
                for (Iterator i = target.hunks.iterator(); i.hasNext();) {
                    final SVNPatchHunkInfo hi = (SVNPatchHunkInfo) i.next();
                    taken = (!hi.isRejected() && target.currentLine >= hi.getMatchedLine() && target.currentLine < hi.getMatchedLine() + hi.getHunk().getOriginal().getLength());
                    if (taken) {
                        break;
                    }
                }

                if (!taken) {
                    matched_line = target.currentLine;
                    if (matchFirst) {
                        break;
                    }
                }
            }

            target.seekToLine(target.currentLine + 1);

        }

        return matched_line;

    }

    /**
     * Indicate in *MATCHED whether the original text of HUNK matches the patch
     * TARGET at its current line. Lines within FUZZ lines of the start or end
     * of HUNK will always match. When this function returns, neither
     * TARGET->CURRENT_LINE nor the file offset in the target file will have
     * changed. HUNK->ORIGINAL_TEXT will be reset.
     * 
     * @throws SVNException
     * @throws IOException
     */
    private boolean matchHunk(SVNPatchHunk hunk, int fuzz) throws SVNException, IOException {

        final SVNPatchTarget target = this;

        final StringBuffer hunkLine = new StringBuffer();
        final StringBuffer targetLine = new StringBuffer();
        final StringBuffer eol_str = new StringBuffer();

        int linesRead;
        int savedLine;
        boolean hunkEof;
        boolean linesMatched;

        boolean matched = false;

        if (target.eof) {
            return matched;
        }

        savedLine = target.currentLine;
        linesRead = 0;
        linesMatched = false;
        hunk.getOriginalText().reset();

        do {
            String hunk_line_translated;

            hunkLine.setLength(0);
            eol_str.setLength(0);

            hunkEof = hunk.getOriginalText().readLineWithEol(hunkLine, eol_str);

            /* Contract keywords, if any, before matching. */
            final byte[] eol = eol_str.toString().getBytes();
            hunk_line_translated = SVNTranslator.translateString(hunkLine.toString(), eol, target.keywords, false, false);

            linesRead++;
            targetLine.setLength(0);
            target.readLine(targetLine);

            if (!hunkEof) {
                if (linesRead <= fuzz && hunk.getLeadingContext() > fuzz) {
                    linesMatched = true;
                } else if (linesRead > hunk.getOriginal().getLength() - fuzz && hunk.getTrailingContext() > fuzz) {
                    linesMatched = true;
                } else {
                    linesMatched = hunk_line_translated.equals(targetLine.toString());
                }
            }
        } while (linesMatched && !(hunkEof || target.eof));

        if (hunkEof) {
            matched = linesMatched;
        } else if (target.eof) {
            /*
             * If the target has no newline at end-of-file, we get an EOF
             * indication for the target earlier than we do get it for the hunk.
             */
            hunkEof = hunk.getOriginalText().readLineWithEol(hunkLine, null);
            if (hunkLine.length() == 0 && hunkEof) {
                matched = linesMatched;
            } else {
                matched = false;
            }
        }
        target.seekToLine(savedLine);
        target.eof = false;

        return matched;
    }

    /**
     * Attempt to write LEN bytes of DATA to STREAM, the underlying file of
     * which is at ABSPATH. Fail if not all bytes could be written to the
     * stream.
     */
    private void tryWrite(OutputStream stream, StringBuffer buffer) throws IOException {
        stream.write(buffer.toString().getBytes());
    }

    /**
     * Use client context CTX to send a suitable notification for a patch
     * TARGET.
     * 
     * @throws SVNException
     */
    public void sendPatchNotification(SVNAdminArea wc) throws SVNException {

        final SVNPatchTarget target = this;

        final ISVNEventHandler eventHandler = wc.getWCAccess().getEventHandler();

        if (eventHandler == null) {
            return;
        }

        SVNEventAction action;

        if (target.skipped) {
            action = SVNEventAction.SKIP;
        } else if (target.deleted) {
            action = SVNEventAction.DELETE;
        } else if (target.added) {
            action = SVNEventAction.ADD;
        } else {
            action = SVNEventAction.PATCH;
        }

        SVNStatusType contentState = SVNStatusType.INAPPLICABLE;

        if (action == SVNEventAction.SKIP) {
            if (target.parentDirExists && (target.kind == SVNNodeKind.NONE || target.kind == SVNNodeKind.UNKNOWN)) {
                contentState = SVNStatusType.MISSING;
            } else if (target.kind == SVNNodeKind.DIR) {
                contentState = SVNStatusType.OBSTRUCTED;
            } else {
                contentState = SVNStatusType.UNKNOWN;
            }
        } else {
            if (target.hadRejects) {
                contentState = SVNStatusType.CONFLICTED;
            } else if (target.localMods) {
                contentState = SVNStatusType.MERGED;
            } else {
                contentState = SVNStatusType.CHANGED;
            }
        }

        final SVNEvent notify = SVNEventFactory.createSVNEvent(target.absPath != null ? target.absPath : target.relPath, target.kind, null, 0, contentState, SVNStatusType.INAPPLICABLE,
                SVNStatusType.LOCK_INAPPLICABLE, action, null, null, null);

        eventHandler.handleEvent(notify, ISVNEventHandler.UNKNOWN);

        if (action == SVNEventAction.PATCH) {

            for (final Iterator i = target.hunks.iterator(); i.hasNext();) {
                final SVNPatchHunkInfo hi = (SVNPatchHunkInfo) i.next();

                if (hi.isRejected()) {
                    action = SVNEventAction.PATCH_REJECTED_HUNK;
                } else {
                    action = SVNEventAction.PATCH_APPLIED_HUNK;
                }

                //TODO: propertyName should be set in notify2
                final SVNEvent notify2 = SVNEventFactory.createSVNEvent(target.absPath != null ? target.absPath : target.relPath, target.kind, null, 0, action, null, null, null);
                notify2.setInfo(hi);

                eventHandler.handleEvent(notify2, ISVNEventHandler.UNKNOWN);
            }
        }

    }

}
