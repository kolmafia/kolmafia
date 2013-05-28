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
package org.tmatesoft.svn.core.internal.wc17;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNAdminUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @author TMate Software Ltd.
 */
public class SVNWCUtils {

    /**
     * Return a recursion boolean based on @a depth.
     *
     * Although much code has been converted to use depth, some code still takes
     * a recurse boolean. In most cases, it makes sense to treat unknown or
     * infinite depth as recursive, and any other depth as non-recursive (which
     * in turn usually translates to #svn_depth_files).
     */
    public static boolean isRecursiveDepth(SVNDepth depth) {
        return depth == SVNDepth.INFINITY || depth == SVNDepth.UNKNOWN;
    }

    public static File admChild(File dirAbsPath, String admChildFileName) {
        return SVNFileUtil.createFilePath(SVNFileUtil.createFilePath(dirAbsPath, SVNFileUtil.getAdminDirectoryName()), admChildFileName);
    }

    public static void admCleanupTmpArea(SVNWCContext context, File dirAbsPath) throws SVNException {
    	assert(SVNFileUtil.isAbsolute(dirAbsPath));
    	context.writeCheck(dirAbsPath);
    	File tmpPath = admChild(dirAbsPath, SVNWCContext.WC_ADM_TMP);
    	SVNFileUtil.deleteAll(tmpPath, true);
    	admInitTmpArea(dirAbsPath);
    }
    
    public static void admInitTmpArea(File dirAbsPath) throws SVNException {
    	SVNFileUtil.ensureDirectoryExists(SVNWCUtils.admChild(dirAbsPath, SVNWCContext.WC_ADM_TMP));
    }
    
    public static SVNDate readDate(long date) {
        long time = date / 1000;
        return new SVNDate(time, (int) (date - time * 1000));
    }

    public static SVNProperties propDiffs(SVNProperties targetProps, SVNProperties sourceProps) {
        SVNProperties propdiffs = new SVNProperties();
        for (Iterator<String> i = sourceProps.nameSet().iterator(); i.hasNext();) {
            String key = i.next();
            byte[] propVal1 = SVNPropertyValue.getPropertyAsBytes(sourceProps.getSVNPropertyValue(key));
            byte[] propVal2 = SVNPropertyValue.getPropertyAsBytes(targetProps.getSVNPropertyValue(key));
            if (propVal2 == null) {
                SVNPropertyValue p = SVNPropertyValue.create(null);
                propdiffs.put(key, p);
            } else if (!Arrays.equals(propVal1, propVal2)) {
                SVNPropertyValue p = SVNPropertyValue.create(key, propVal2);
                propdiffs.put(key, p);
            }
        }
        for (Iterator<String> i = targetProps.nameSet().iterator(); i.hasNext();) {
            String key = i.next();
            SVNPropertyValue propVal = targetProps.getSVNPropertyValue(key);
            if (null == sourceProps.getSVNPropertyValue(key)) {
                SVNPropertyValue p = propVal;
                propdiffs.put(key, p);
            }
        }
        return propdiffs;
    }

    public static int relpathDepth(File relpath) {
        if (relpath == null) {
            return 0;
        }
        return relpathDepth(relpath.getPath().replace(File.separatorChar, '/'));
    }

    public static int relpathDepth(String relpath) {
        if (relpath == null || "".equals(relpath)) {
            return 0;
        }
        int n = 1;
        int length = relpath.length();
        for (int i = 0; i < length; i++) {
            if (relpath.charAt(i) == '/')
                n++;
        }
        return n;
    }

    public static class UnserializedFileExternalInfo {

        public String path = null;
        public SVNRevision pegRevision = SVNRevision.UNDEFINED;
        public SVNRevision revision = SVNRevision.UNDEFINED;
    }

    public static UnserializedFileExternalInfo unserializeFileExternal(String str) throws SVNException {
        final UnserializedFileExternalInfo info = new UnserializedFileExternalInfo();
        if (str != null) {
            StringBuffer buffer = new StringBuffer(str);
            info.pegRevision = SVNAdminUtil.parseRevision(buffer);
            info.revision = SVNAdminUtil.parseRevision(buffer);
            info.path = buffer.toString();
        }
        return info;
    }

    public static String serializeFileExternal(String path, SVNRevision pegRevision, SVNRevision revision) throws SVNException {
        String representation = null;
        if (path != null) {
            String revStr = SVNAdminUtil.asString(revision, path);
            String pegRevStr = SVNAdminUtil.asString(pegRevision, path);
            representation = pegRevStr + ":" + revStr + ":" + path;
        }
        return representation;
    }

    public static String getPathAsChild(File parent, File child) {
        if (parent == null || child == null)
            return null;
        if (parent.equals(child))
            return null;
        final String parentPath = parent.toString();
        final String childPath = child.toString();
        return isChild(parentPath, childPath);
    }

    public static boolean isChild(final File parent, final File child) {
        return isChild(SVNFileUtil.getFilePath(parent), SVNFileUtil.getFilePath(child)) != null;
    }

    public static String isChild(String parentPath, String childPath) {
        if (childPath.equals(parentPath)) {
            return "";
        }
        if ("".equals(parentPath)) {
            return childPath;
        }
        childPath = childPath.replace(File.separatorChar, '/');
        parentPath = parentPath.replace(File.separatorChar, '/');
        if (!childPath.startsWith(parentPath + '/')) {
            return null;
        }

        return childPath.substring(parentPath.length() + 1);
    }

    public static boolean isAncestor(File parent, File child) {
        if (parent == null || child == null) {
            return false;
        }
        if (parent.equals(child)) {
            return true;
        }
        final String parentPath = parent.getPath().replace(File.separatorChar, '/');
        final String childPath = child.getPath().replace(File.separatorChar, '/');

        if ("".equals(parentPath)) {
            return !childPath.startsWith("/");
        }
        return childPath.startsWith(parentPath + "/");
    }

    public static File skipAncestor(File parent, File child) {
        if (!isAncestor(parent, child)) {
            return child;
        }
        return SVNFileUtil.createFilePath(getPathAsChild(parent, child));
    }

    public static String isChild(SVNURL parent, SVNURL child) {
        if (parent == null || child == null)
            return null;
        if (parent.equals(child))
            return null;
        final String parentPath = parent.toDecodedString();
        final String childPath = child.toDecodedString();
        return isChild(parentPath, childPath);
    }

    public static SVNURL join(SVNURL rootUrl, File relPath) throws SVNException {
        return rootUrl.appendPath(SVNFileUtil.getFilePath(relPath), false);
    }

    public static long parseLong(String value) throws SVNException {
        try{
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.INCORRECT_PARAMS, "Could not convert ''{0}'' into a number", value);
            SVNErrorManager.error(err, SVNLogType.DEFAULT);
            return 0;
        }
    }

}
