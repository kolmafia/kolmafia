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
package org.tmatesoft.svn.core.internal.util;

import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class SVNPathUtil {

    public static final Comparator PATH_COMPARATOR = new Comparator() {

        public int compare(Object o1, Object o2) {
            if (o1 == o2) {
                return 0;
            } else if (o1 == null) {
                return -1;
            } else if (o2 == null) {
                return 1;
            } else if (o1.getClass() != String.class || o2.getClass() != String.class) {
                return o1.getClass() == o2.getClass() ? 0 : o1.getClass() == String.class ? 1 : -1;
            }
            String p1 = (String) o1;
            String p2 = (String) o2;
            return p1.replace('/', '\0').compareTo(p2.replace('/', '\0'));
        }
    };

    public static boolean isCanonical(String path) {
        return (path != null && path.equals(canonicalizePath(path)));
    }
    
    public static void checkPathIsValid(String path) throws SVNException {
        for (int i = 0; i < path.length(); i++) {
            char ch = path.charAt(i);
            if (SVNEncodingUtil.isASCIIControlChar(ch)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_PATH_SYNTAX, "Invalid control character ''{0}'' in path ''{1}''", new String[]{"0x" + SVNFormatUtil.getHexNumberFromByte((byte) ch), path});
                SVNErrorManager.error(err, SVNLogType.DEFAULT);
            }
        }
    }

    public static String getAbsolutePath(String path){
        if (path == null){
            return null;
        }        
        if (path.length() == 0 || "/".equals(path)){
            return "/";           
        }
        if (isURL(path)){
            return path;            
        }
        path = path.endsWith("/") ? path.substring(0, path.length() - "/".length()) : path;
        return path.startsWith("/") ? path : "/" + path;       
    }

    public static String validateFilePath(String path) {
        path = path.replace(File.separatorChar, '/');
        StringBuffer result = new StringBuffer();
        List segments = new LinkedList();
        for (StringTokenizer tokens = new StringTokenizer(path, "/", false); tokens.hasMoreTokens();) {
            String segment = tokens.nextToken();
            if ("..".equals(segment)) {
                if (!segments.isEmpty()) {
                    segments.remove(segments.size() - 1);
                } else {
                    File root = new File(System.getProperty("user.dir"));
                    while (root.getParentFile() != null) {
                        segments.add(0, root.getParentFile().getName());
                        root = root.getParentFile();
                    }
                }
                continue;
            } else if (".".equals(segment) || segment.length() == 0) {
                continue;
            }
            segments.add(segment);
        }
        if (path.length() > 0 && path.charAt(0) == '/') {
            result.append("/");
        }
        if (path.length() > 1 && path.charAt(1) == '/') {
            result.append("/");
        }
        for (Iterator tokens = segments.iterator(); tokens.hasNext();) {
            String token = (String) tokens.next();
            result.append(token);
            if (tokens.hasNext()) {
                result.append('/');
            }
        }
        if (result.length() == 2 && result.charAt(result.length() - 1) == ':') {
            result.append('/'); 
        }
        return result.toString();
    }

    public static String canonicalizePath(String path) {
        if (path == null){
            return null;           
        }
        
        boolean isUrl = isURL(path);
        
        StringBuffer result = new StringBuffer();
        int i = 0;
        for (; i < path.length(); i++) {
            if (path.charAt(i) == '/' || path.charAt(i) == ':') {
                break;
            }
        }
        String scheme = null;
        int index = 0;
        if (i > 0 && i + 2 < path.length() && path.charAt(i) == ':' && path.charAt(i + 1) == '/' && path.charAt(i + 2) == '/') {
            scheme = path.substring(0, i + 3);
            result.append(scheme);
            index = i + 3;
        }
        if (index < path.length() && path.charAt(index) == '/') {
            result.append('/');
            index++;
            if (SVNFileUtil.isWindows && scheme == null && index < path.length() && path.charAt(index) == '/') {
                result.append('/');
                index++;
            }
        }
        int segmentCount = 0;
        while (index < path.length()) {
            int nextIndex = index;
            while (nextIndex < path.length() && path.charAt(nextIndex) != '/' 
            		 && !(isUrl && (nextIndex + 2) < path.length() && path.charAt(nextIndex) == '%' && path.charAt(nextIndex + 1) == '2' &&
    				Character.toUpperCase(path.charAt(nextIndex + 2)) == 'F')) {
            	nextIndex++;
            }
            int slashLength = 0;
            if (nextIndex < path.length()) {
            	if (path.charAt(nextIndex) == '/')
            		slashLength = 1;
            	else if (path.charAt(nextIndex) == '%')
            		slashLength = 3;
            }
            int segmentLength = nextIndex - index;
            if (segmentLength == 0 || (segmentLength == 1 && path.charAt(index) == '.')
            		|| (isUrl && segmentLength == 3 && path.charAt(index) == '%' && path.charAt(index + 1) == '2' && Character.toUpperCase(path.charAt(index + 2))  == 'E')) {
            } else {
                result.append(path.substring(index, index + segmentLength));
            	if (slashLength > 0)
            		result.append('/');
            	segmentCount++;
            }
            index = nextIndex;
            if (index < path.length()) {
                index += slashLength;
            }
        }
        if ((segmentCount > 0 || scheme != null) && result.charAt(result.length() - 1) == '/') {
            result = result.delete(result.length() - 1, result.length());
        }
        if (SVNFileUtil.isWindows && segmentCount < 2 && result.length() >= 2 && result.charAt(0) == '/' && result.charAt(1) == '/') {
            result = result.delete(0, 1);
        }
        return result.toString();
    }

    public static String canonicalizeAbsolutePath(String path) {
        if (path == null) {
            return null;
        }
     
        if ("".equals(path)) {
            return "/";
        }
        
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        
        StringBuffer canonicalizedPath = new StringBuffer();
        boolean skipSlashes = false;
        for (int i = 0; i < path.length(); i++) {
            char ch = path.charAt(i);
            if (ch == '/') {
                if (skipSlashes) {
                    continue;
                }
                skipSlashes = true;
            } else {
                if (skipSlashes) {
                    skipSlashes = false;
                }
            }
            canonicalizedPath.append(ch);
        }
        
        if (canonicalizedPath.length() > 1 && canonicalizedPath.charAt(canonicalizedPath.length() - 1) == '/') {
            canonicalizedPath.deleteCharAt(canonicalizedPath.length() - 1);
        }
        return canonicalizedPath.toString();
    }
    
    public static String append(String f, String s) {
        f = f == null ? "" : f;
        s = s == null ? "" : s;
        int l1 = f.length();
        int l2 = s.length();
        char[] r = new char[l1 + l2 + 2];
        int index = 0;
        for (int i = 0; i < l1; i++) {
            char ch = f.charAt(i);
            if (i + 1 == l1 && ch == '/') {
                break;
            }
            r[index++] = ch;
        }
        for (int i = 0; i < l2; i++) {
            char ch = s.charAt(i);
            if (i == 0 && ch != '/' && index > 0) {
                r[index++] = '/';
            }
            if (i + 1 == l2 && ch == '/') {
                break;
            }
            r[index++] = ch;
        }
        return new String(r, 0, index);
    }

    public static boolean isSinglePathComponent(String name) {
        /* Can't be empty or `..'  */
        if (name == null || "".equals(name) || "..".equals(name)) {
            return true;
        }
        /* Slashes are bad */
        if (name.indexOf('/') != -1) {
            return false;
        }
        /* It is valid.  */
        return true;
    }

    public static String head(String path) {
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '/') {
                return path.substring(0, i);
            }
        }
        return path;
    }

    public static String removeHead(String path) {
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '/') {
                int ind = i;
                for (; ind < path.length(); ind++) {
                    if (path.charAt(ind) == '/') {
                        continue;
                    }
                    break;
                }
                return path.substring(ind);
            }
        }
        return "";
    }

    public static String tail(String path) {
        int index = path.length() - 1;
        if (index >= 0 && index < path.length() && path.charAt(index) == '/') {
            index--;
        }
        for (int i = index; i >= 0; i--) {
            if (path.charAt(i) == '/') {
                return path.substring(i + 1, index + 1);
            }
        }
        return path;
    }

    public static String removeTail(String path) {
        int index = path.length() - 1;
        while (index >= 0) {
            if (path.charAt(index) == '/') {
                return path.substring(0, index);
            }
            index--;
        }
        return "";
    }

    public static String getCommonPathAncestor(String path1, String path2) {
        if (path1 == null || path2 == null) {
            return null;
        }
        path1 = path1.replace(File.separatorChar, '/');
        path2 = path2.replace(File.separatorChar, '/');

        int index = 0;
        int separatorIndex = 0;
        while (index < path1.length() && index < path2.length()) {
            if (path1.charAt(index) != path2.charAt(index)) {
                break;
            }
            if (path1.charAt(index) == '/') {
                separatorIndex = index;
            }
            index++;
        }
        if (index == path1.length() && index == path2.length()) {
            return path1;
        } else if (index == path1.length() && path2.charAt(index) == '/') {
            return path1;
        } else if (index == path2.length() && path1.charAt(index) == '/') {
            return path2;
        }
        return path1.substring(0, separatorIndex);
    }

    public static String condencePaths(String[] paths, Collection condencedPaths, boolean removeRedundantPaths) {
        if (paths == null || paths.length == 0) {
            return null;
        }
        if (paths.length == 1) {
            return paths[0];
        }
        String rootPath = paths[0];
        for (int i = 0; i < paths.length; i++) {
            String url = paths[i];
            rootPath = getCommonPathAncestor(rootPath, url);
        }
        if (condencedPaths != null && removeRedundantPaths) {
            for (int i = 0; i < paths.length; i++) {
                String path1 = paths[i];
                if (path1 == null) {
                    continue;
                }
                for (int j = 0; j < paths.length; j++) {
                    if (i == j) {
                        continue;
                    }
                    String path2 = paths[j];
                    if (path2 == null) {
                        continue;
                    }
                    String common = getCommonPathAncestor(path1, path2);

                    if ("".equals(common) || common == null) {
                        continue;
                    }
                    // remove logner path here
                    if (common.equals(path1)) {
                        paths[j] = null;
                    } else if (common.equals(path2)) {
                        paths[i] = null;
                    }
                }
            }
            for (int j = 0; j < paths.length; j++) {
                String path = paths[j];
                if (path != null && path.equals(rootPath)) {
                    paths[j] = null;
                }
            }
        }

        if (condencedPaths != null) {
            for (int i = 0; i < paths.length; i++) {
                String path = paths[i];
                if (path == null) {
                    continue;
                }
                if (rootPath != null && !"".equals(rootPath)) {
                    path = path.substring(rootPath.length());
                    if (path.startsWith("/")) {
                        path = path.substring(1);
                    }
                }
                condencedPaths.add(path);
            }
        }
        return rootPath;
    }

    public static int getSegmentsCount(String path) {
        int count = path.length() > 0 ? 1 : 0;
        // skip first char, then count number of '/'
        for (int i = 1; i < path.length(); i++) {
            if (path.charAt(i) == '/') {
                count++;
            }
        }
        return count;
    }

    public static boolean isAncestor(String parentPath, String childPath) {
        parentPath = parentPath == null ? "" : parentPath;
        childPath = childPath == null ? "" : childPath;

        if (parentPath.length() == 0) {
            return !childPath.startsWith("/");
        }

        if (childPath.startsWith(parentPath)) {
            if (parentPath.length() != childPath.length() && !parentPath.endsWith("/") &&
                    childPath.charAt(parentPath.length()) != '/') {
                if (parentPath.startsWith("file://") && childPath.startsWith("file://")) {
                    //HACK: maybe encoded back slashes (UNC path)?
                    String encodedSlash = SVNEncodingUtil.uriEncode("\\");
                    return parentPath.endsWith(encodedSlash) ||
                            childPath.substring(parentPath.length()).startsWith(encodedSlash);
                }
                return false;
            }
            return true;
        }

        return false;
    }

    /**
     * Former pathIsChild.
     *
     * @param path
     * @param pathChild
     * @return
     */
    public static String getPathAsChild(String path, String pathChild) {
        if (path == null || pathChild == null) {
            return null;
        }
        if (pathChild.compareTo(path) == 0) {
            return null;
        }
	    if (!path.endsWith("/")) { // We don't want to have /foobar being a child of /foo
		    path = path + "/";
	    }
	    if (pathChild.startsWith(path)) {
		    return pathChild.substring(path.length());
	    }
        return null;
    }

    public static String getRelativePath(String parent, String child) {
        parent = parent.replace(File.separatorChar, '/');
        child = child.replace(File.separatorChar, '/');
        String relativePath = getPathAsChild(parent, child);
        return relativePath == null ? "" : relativePath;
    }
    
    public static boolean isURL(String pathOrUrl) {
        pathOrUrl = pathOrUrl != null ? pathOrUrl.toLowerCase() : null;
        return pathOrUrl != null
                && (pathOrUrl.startsWith("http://")
                        || pathOrUrl.startsWith("https://")
                        || pathOrUrl.startsWith("svn://")
                        || (pathOrUrl.startsWith("svn+") && pathOrUrl.indexOf("://") > 4)
                        || pathOrUrl.startsWith("file://"));
    }    
    
    public static boolean isWithinBasePath(String basePath, String path) {
        return "".equals(basePath) || (path.startsWith(basePath) && (path.length() == basePath.length() || 
                path.charAt(basePath.length()) == '/'));
    }

    public static boolean isAbsolute(String path) {
        if (path == null || path.length() == 0) {
            return false;
        }
        if (path.charAt(0) == '/') {
            return true;
        }
        if (SVNFileUtil.isWindows && path.length() > 1) {
            char ch0 = path.charAt(0);
            char ch1 = path.charAt(1);
            if (((ch0 >= 'A' && ch0 <= 'Z') || (ch0 >= 'a' && ch0 <= 'z')) && ch1 == ':') {
                return true;
            }
        }
        return false;
    }

}
