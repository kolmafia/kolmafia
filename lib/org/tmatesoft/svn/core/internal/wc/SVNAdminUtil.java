/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.wc;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.logging.Level;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminArea;
import org.tmatesoft.svn.core.internal.wc.admin.SVNEntry;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNAdminUtil {

    private static final byte[] FORMAT_TEXT;
    private static final byte[] README_TEXT;
    private static final boolean SKIP_README;

    private static final String BASE_EXT = ".svn-base";
    private static final String REVERT_EXT = ".svn-revert";
    private static final String WORK_EXT = ".svn-work";

    private static final String TEXT_BASE_DIR_NAME = "text-base";
    private static final String PROP_BASE_DIR_NAME = "prop-base";
    private static final String PROP_WORK_DIR_NAME = "props";
    private static final String PROP_WC_DIR_NAME = "wcprops";
    private static final String TMP_DIR_NAME = "tmp";
    //private static final String SDB_FILE_NAME = "wc.db";

    private static final String DIR_PROPS_FILE = "dir-props";
    private static final String DIR_BASE_PROPS_FILE = "dir-prop-base";
    private static final String DIR_REVERT_PROPS_FILE = "dir-prop-revert";
    private static final String DIR_WC_PROPS_FILE = "dir-wcprops";

    static {
        String eol = System.getProperty("line.separator");
        FORMAT_TEXT = new byte[] {'4', '\n'};
        README_TEXT = ("This is a Subversion working copy administrative directory." + eol
            + "Visit http://subversion.tigris.org/ for more information." + eol).getBytes();
        SKIP_README = Boolean.getBoolean("javasvn.skipReadme") ? true : Boolean.getBoolean("svnkit.skipReadme");
    }

    public static void createReadmeFile(File adminDir) throws SVNException {
        if (SKIP_README) {
            return;
        }
        OutputStream os = null;
        try {
            os = SVNFileUtil.openFileForWriting(new File(adminDir, "README.txt"));
            os.write(README_TEXT);
        } catch (IOException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e.getLocalizedMessage());
            SVNErrorManager.error(err, e, SVNLogType.FSFS);
        } finally {
            SVNFileUtil.closeFile(os);
        }

    }

    public static void createFormatFile(File adminDir) throws SVNException {
        OutputStream os = null;
        try {
            os = SVNFileUtil.openFileForWriting(new File(adminDir, "format"));
            os.write(FORMAT_TEXT);
        } catch (IOException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e.getLocalizedMessage());
            SVNErrorManager.error(err, e, SVNLogType.FSFS);
        } finally {
            SVNFileUtil.closeFile(os);
        }
    }

    public static String getTextBasePath(String name, boolean tmp) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(SVNFileUtil.getAdminDirectoryName());
        buffer.append('/');
        if (tmp) {
            buffer.append(TMP_DIR_NAME);
            buffer.append('/');
        }
        buffer.append(TEXT_BASE_DIR_NAME);
        buffer.append('/');
        buffer.append(name);
        buffer.append(BASE_EXT);
        return buffer.toString();
    }

    public static String getTextRevertPath(String name, boolean tmp) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(SVNFileUtil.getAdminDirectoryName());
        buffer.append('/');
        if (tmp) {
            buffer.append(TMP_DIR_NAME);
            buffer.append('/');
        }
        buffer.append(TEXT_BASE_DIR_NAME);
        buffer.append('/');
        buffer.append(name);
        buffer.append(REVERT_EXT);
        return buffer.toString();
    }

    public static String getPropPath(String name, SVNNodeKind kind, boolean tmp) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(SVNFileUtil.getAdminDirectoryName());
        buffer.append('/');
        if (tmp) {
            buffer.append(TMP_DIR_NAME);
            buffer.append('/');
        }
        if (kind == SVNNodeKind.DIR) {
            buffer.append(DIR_PROPS_FILE);
        } else {
            buffer.append(PROP_WORK_DIR_NAME);
            buffer.append('/');
            buffer.append(name);
            buffer.append(WORK_EXT);
        }
        return buffer.toString();
    }

    public static String getPropBasePath(String name, SVNNodeKind kind, boolean tmp) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(SVNFileUtil.getAdminDirectoryName());
        buffer.append('/');
        if (tmp) {
            buffer.append(TMP_DIR_NAME);
            buffer.append('/');
        }
        if (kind == SVNNodeKind.DIR) {
            buffer.append(DIR_BASE_PROPS_FILE);
        } else {
            buffer.append(PROP_BASE_DIR_NAME);
            buffer.append('/');
            buffer.append(name);
            buffer.append(BASE_EXT);
        }
        return buffer.toString();
    }

    public static String getPropRevertPath(String name, SVNNodeKind kind, boolean tmp) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(SVNFileUtil.getAdminDirectoryName());
        buffer.append('/');
        if (tmp) {
            buffer.append(TMP_DIR_NAME);
            buffer.append('/');
        }
        if (kind == SVNNodeKind.DIR) {
            buffer.append(DIR_REVERT_PROPS_FILE);
        } else {
            buffer.append(PROP_BASE_DIR_NAME);
            buffer.append('/');
            buffer.append(name);
            buffer.append(REVERT_EXT);
        }
        return buffer.toString();
    }

    public static String getWCPropPath(String name, SVNNodeKind kind, boolean tmp) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(SVNFileUtil.getAdminDirectoryName());
        buffer.append('/');
        if (tmp) {
            buffer.append(TMP_DIR_NAME);
            buffer.append('/');
        }
        if (kind == SVNNodeKind.DIR) {
            buffer.append(DIR_WC_PROPS_FILE);
        } else {
            buffer.append(PROP_WC_DIR_NAME);
            buffer.append('/');
            buffer.append(name);
            buffer.append(WORK_EXT);
        }
        return buffer.toString();
    }

    /**
     * Creates "tempfile[.n].tmp" in admin area's /tmp dir
     *
     * @param adminArea
     * @return
     * @throws SVNException
     */
    public static File createTmpFile(SVNAdminArea adminArea) throws SVNException {
        return createTmpFile(adminArea, "tempfile", ".tmp", true);
    }

    public static File createTmpFile(SVNAdminArea adminArea, String prefix, String suffix, boolean tmp) throws SVNException {
        StringBuffer buffer = new StringBuffer();
        buffer.append(SVNFileUtil.getAdminDirectoryName());
        buffer.append('/');
        if (tmp) {
            buffer.append(TMP_DIR_NAME);
            buffer.append('/');
        }
        String adminPath = buffer.toString();
        File dir = adminArea.getFile(adminPath);
        return SVNFileUtil.createUniqueFile(dir, prefix, suffix, false);
    }

    public static int getVersion(File path) throws SVNException {
        File adminDir = new File(path, SVNFileUtil.getAdminDirectoryName());
        File entriesFile = new File(adminDir, "entries");
        int formatVersion = -1;

        BufferedReader reader = null;
        String line = null;

        try {
            reader = new BufferedReader(new InputStreamReader(SVNFileUtil.openFileForReading(entriesFile, Level.FINEST, SVNLogType.WC), "UTF-8"));
            line = reader.readLine();
        } catch (IOException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot read entries file ''{0}'': {1}", new Object[] {entriesFile, e.getLocalizedMessage()});
            SVNErrorManager.error(err, e, SVNLogType.WC);
        } catch (SVNException svne) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_NOT_DIRECTORY, "''{0}'' is not a working copy", path);
            err.setChildErrorMessage(svne.getErrorMessage());
            SVNErrorManager.error(err, svne, Level.FINEST, SVNLogType.WC);
        } finally {
            SVNFileUtil.closeFile(reader);
        }

        if (line == null || line.length() == 0) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.STREAM_UNEXPECTED_EOF, "Reading ''{0}''", entriesFile);
            SVNErrorMessage err1 = SVNErrorMessage.create(SVNErrorCode.WC_NOT_DIRECTORY, "''{0}'' is not a working copy", path);
            err1.setChildErrorMessage(err);
            SVNErrorManager.error(err1, Level.FINEST, SVNLogType.WC);
        }

        try {
            formatVersion = Integer.parseInt(line.trim());
        } catch (NumberFormatException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.BAD_VERSION_FILE_FORMAT, "First line of ''{0}'' contains non-digit", entriesFile);
            SVNErrorMessage err1 = SVNErrorMessage.create(SVNErrorCode.WC_NOT_DIRECTORY, "''{0}'' is not a working copy", path);
            err1.setChildErrorMessage(err);
            SVNErrorManager.error(err1, Level.FINEST, SVNLogType.WC);
        }
        return formatVersion;
    }

    public static void unserializeExternalFileData(SVNEntry entry, String rawExternalFileData) throws SVNException {
        SVNRevision pegRevision = SVNRevision.UNDEFINED;
        SVNRevision revision = SVNRevision.UNDEFINED;
        String path = null;
        if (rawExternalFileData != null) {
            StringBuffer buffer = new StringBuffer(rawExternalFileData);
            pegRevision = parseRevision(buffer);
            revision = parseRevision(buffer);
            path = buffer.toString();
        }
        entry.setExternalFilePath(path);
        entry.setExternalFileRevision(revision);
        entry.setExternalFilePegRevision(pegRevision);
    }

    public static SVNRevision parseRevision(StringBuffer str) throws SVNException {
        int ind = str.indexOf(":");
        if ( ind == -1) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.INCORRECT_PARAMS,
                    "Found an unexpected \\0 in the file external ''{0}''", str);
            SVNErrorManager.error(err, SVNLogType.WC);
        }

        SVNRevision revision = null;
        String subStr = str.substring(0, ind);
        if (subStr.equals(SVNRevision.HEAD.getName())) {
            revision = SVNRevision.HEAD;
        } else {
            revision = SVNRevision.parse(subStr);
        }
        str = str.delete(0, ind + 1);
        return revision;
    }

    public static String serializeExternalFileData(SVNEntry entry) throws SVNException {
        String representation = null;
        String path = entry.getExternalFilePath();
        SVNRevision revision = entry.getExternalFileRevision();
        SVNRevision pegRevision = entry.getExternalFilePegRevision();
        if (path != null) {
            String revStr = SVNAdminUtil.asString(revision, path);
            String pegRevStr = SVNAdminUtil.asString(pegRevision, path);
            representation = pegRevStr + ":" + revStr + ":" + path;
        }
        return representation;
    }

    public static String asString(SVNRevision revision, String path) throws SVNException {
        if (revision == SVNRevision.HEAD ||
                SVNRevision.isValidRevisionNumber(revision.getNumber())) {
            return revision.toString();
        }

        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.INCORRECT_PARAMS, "Illegal file external revision kind {0} for path ''{1}''",

                new Object[] { revision.toString(), path });
        SVNErrorManager.error(err, SVNLogType.WC);
        return null;
    }

}