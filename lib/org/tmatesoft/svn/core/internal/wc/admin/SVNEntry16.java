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
package org.tmatesoft.svn.core.internal.wc.admin;

import java.util.Map;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.internal.wc.SVNTreeConflictUtil;
import org.tmatesoft.svn.core.io.SVNRepository;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNEntry16 extends SVNEntry {

    private SVNAdminArea myAdminArea;
    
    public SVNEntry16(SVNAdminArea adminArea, String name) {
        this(null, adminArea, name);
    }
    
    public SVNEntry16(Map attributes, SVNAdminArea adminArea, String name) {
        myAdminArea = adminArea;

        setName(name);
        setWorkingSize(SVNProperty.WORKING_SIZE_UNKNOWN);
        setCommittedRevision(SVNRepository.INVALID_REVISION);
        setRevision(SVNRepository.INVALID_REVISION);
        setCopyFromRevision(SVNRepository.INVALID_REVISION);
        setDepth(SVNDepth.INFINITY);
        
        if (attributes != null) {
            applyChanges(attributes);
        }
    }

    public boolean isThisDir() {
        if (myAdminArea != null) {
            return myAdminArea.getThisDirName().equals(getName());
        }
        return "".equals(getName());
    }

    public Map getTreeConflicts() throws SVNException {
        String conflictData = getTreeConflictData();
        return SVNTreeConflictUtil.readTreeConflicts(getAdminArea().getRoot(), conflictData);
    }

    public SVNAdminArea getAdminArea() {
        return myAdminArea;
    }

}