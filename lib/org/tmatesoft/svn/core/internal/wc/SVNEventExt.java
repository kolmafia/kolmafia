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

import java.io.File;

import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNLock;
import org.tmatesoft.svn.core.SVNMergeRange;

/**
 * @author TMate Software Ltd.
 * @version 1.3
 * @since 1.2
 */
public class SVNEventExt extends SVNEvent {

    private long myProcessedItemsCount;
    private long myTotalItemsCount;

    public SVNEventExt(SVNErrorMessage errorMessage) {
        super(errorMessage, null);
    }

    public SVNEventExt(File file, SVNNodeKind kind, String mimetype, long revision, SVNStatusType cstatus, SVNStatusType pstatus, SVNStatusType lstatus, SVNLock lock, SVNEventAction action, SVNEventAction expected, SVNErrorMessage error, SVNMergeRange range, String changelistName, long processedItemsCount, long totalItemsCount, SVNProperties revisionProperties, String propertyName) {
        super(file, kind, mimetype, revision, cstatus, pstatus, lstatus, lock, action, expected, error, range, changelistName, revisionProperties, propertyName);
        myProcessedItemsCount = processedItemsCount;
        myTotalItemsCount = totalItemsCount;
    }

    public long getProcessedItemsCount() {
        return myProcessedItemsCount;
    }

    public long getTotalItemsCount() {
        return myTotalItemsCount;
    }
}
