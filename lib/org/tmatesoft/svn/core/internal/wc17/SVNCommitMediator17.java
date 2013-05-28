/*
 * ====================================================================
 * Copyright (c) 2004-2011 TMate Software Ltd.  All rights reserved.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.io.ISVNWorkspaceMediator;
import org.tmatesoft.svn.core.wc2.SvnCommitItem;

/**
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNCommitMediator17 implements ISVNWorkspaceMediator {

    private Collection<File> myTmpFiles;
    private Map<String, SvnCommitItem> myCommitItems;
    private SVNWCContext myContext;

    public SVNCommitMediator17(SVNWCContext context, Map<String, SvnCommitItem> committables) {
        this.myCommitItems = committables;
        this.myContext = context;
        myTmpFiles = new ArrayList<File>();
    }

    public Collection<File> getTmpFiles() {
        return myTmpFiles;
    }

    public void setWorkspaceProperty(String path, String name, SVNPropertyValue value) throws SVNException {
        if (name != null) {
            SvnCommitItem item = (SvnCommitItem) myCommitItems.get(path);
            if (item != null) {
                item.addIncomingProperty(name, value);
            }
        }
    }

    public SVNPropertyValue getWorkspaceProperty(String path, String name) throws SVNException {
        if (name != null) {
            SvnCommitItem item = (SvnCommitItem) myCommitItems.get(path);
            if (item != null) {
                try {
                    return myContext.getPropertyValue(item.getPath(), name);
                } catch (SVNException e) {
                }
            }
        }
        return null;
    }

}
