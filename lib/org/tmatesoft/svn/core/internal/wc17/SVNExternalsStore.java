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
import java.util.Collections;
import java.util.Map;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNExternalsStore {

    private SVNHashMap newExternals;
    private SVNHashMap oldExternals;
    private SVNHashMap depths;

    public void addOldExternal(File path, String oldValue) {
        if (oldExternals == null) {
            oldExternals = new SVNHashMap();
        }
        oldExternals.put(path, oldValue);
    }

    public void addNewExternal(File path, String newValue) {
        if (newExternals == null) {
            newExternals = new SVNHashMap();
        }
        newExternals.put(path, newValue);
    }

    public void addExternal(File path, String oldValue, String newValue) {
        addNewExternal(path, newValue);
        addOldExternal(path, oldValue);
    }

    public void addDepth(File localAbsPath, SVNDepth depth) {
        if (depths == null) {
            depths = new SVNHashMap();
        }
        depths.put(localAbsPath, depth);
    }

    public void removeDepth(String path) {
        if (depths != null) {
            depths.remove(path);
        }
    }

    public void removeExternal(String path) {
        if (newExternals != null) {
            newExternals.remove(path);
        }
        if (oldExternals != null) {
            oldExternals.remove(path);
        }
    }

    public Map<File, String> getNewExternals() {
        return newExternals == null ? Collections.EMPTY_MAP : newExternals;
    }

    public Map<File, String> getOldExternals() {
        return oldExternals == null ? Collections.EMPTY_MAP : oldExternals;
    }

    public Map<File, SVNDepth> getDepths() {
        return depths == null ? Collections.EMPTY_MAP : depths;
    }

}
