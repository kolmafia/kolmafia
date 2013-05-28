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

import java.util.Collection;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;


/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class SVNURLUtil {

    public static String getRelativeURL(SVNURL parent, SVNURL child, boolean encoded) {
        String parentURLAsString = encoded ? parent.toString() : parent.toDecodedString();
        String childURLAsString = encoded ? child.toString() : child.toDecodedString();
        String relativePath = SVNPathUtil.getPathAsChild(parentURLAsString, childURLAsString);
        return relativePath == null ? "" : relativePath;
    }
    
    public static boolean isAncestor(SVNURL ancestor, SVNURL descendant) {
        if (ancestor == null || descendant == null) {
            return false;
        }
        String aStr = ancestor.toString();
        String dStr = descendant.toString();
        if (aStr.length() > dStr.length()) {
            return false;
        }
        if (dStr.startsWith(aStr)) {
            if (aStr.length() == dStr.length()) {
                return true;
            }
            return dStr.charAt(aStr.length()) == '/';
        }
        return false;
    }
    
    public static SVNURL getCommonURLAncestor(SVNURL url1, SVNURL url2) {
        // skip protocol and host, if they are different -> return null;
        if (url1 == null || url2 == null) {
            return null;
        }
        if (!url1.getProtocol().equals(url2.getProtocol()) || !url1.getHost().equals(url2.getHost()) ||
                url1.getPort() != url2.getPort()) {
            return null;
        }
        if (url1.getUserInfo() != null) {
            if (!url1.getUserInfo().equals(url2.getUserInfo())) {
                return null;
            }
        } else {
            if (url2.getUserInfo() != null) {
                return null;
            }
        }
        String path1 = url1.getPath();
        String path2 = url2.getPath();
        String commonPath = SVNPathUtil.getCommonPathAncestor(path1, path2);
        try {
            return url1.setPath(commonPath, false);
        } catch (SVNException e) {
        }
        return null;
    }

    public static SVNURL condenceURLs(SVNURL[] urls, Collection condencedPaths, boolean removeRedundantURLs) {
        if (urls == null || urls.length == 0) {
            return null;
        }
        if (urls.length == 1) {
            return urls[0];
        }
        SVNURL rootURL = urls[0];
        for (int i = 0; i < urls.length; i++) {
            rootURL = getCommonURLAncestor(rootURL, urls[i]);
        }

        if (condencedPaths != null && removeRedundantURLs) {
            for (int i = 0; i < urls.length; i++) {
                SVNURL url1 = urls[i];
                if (url1 == null) {
                    continue;
                }
                for (int j = 0; j < urls.length; j++) {
                    if (i == j) {
                        continue;
                    }
                    SVNURL url2 = urls[j];
                    if (url2 == null) {
                        continue;
                    }
                    SVNURL common = getCommonURLAncestor(url1, url2);
                    if (common == null) {
                        continue;
                    }
                    if (common.equals(url1)) {
                        urls[j] = null;
                    } else if (common.equals(url2)) {
                        urls[i] = null;
                    }
                }
            }
            for (int j = 0; j < urls.length; j++) {
                SVNURL url = urls[j];
                if (url != null && url.equals(rootURL)) {
                    urls[j] = null;
                }
            }
        }

        if (condencedPaths != null) {
            for (int i = 0; i < urls.length; i++) {
                SVNURL url = urls[i];
                if (url == null) {
                    continue;
                }
                String path = url.toString();
                if (rootURL != null) {
                    path = path.substring(rootURL.toString().length());
                    if (path.startsWith("/")) {
                        path = path.substring(1);
                    }
                }
                condencedPaths.add(path);
            }
        }
        return rootURL;
    }

}
