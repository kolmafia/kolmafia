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
package org.tmatesoft.svn.core.internal.io.fs;

import java.io.File;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.SVNLocationEntry;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSNodeHistory {

    private SVNLocationEntry myHistoryEntry;
    private SVNLocationEntry mySearchResumeEntry;
    private boolean myIsInteresting;
    private FSFS myFSFS;
    
    public FSNodeHistory(SVNLocationEntry newHistoryEntry, boolean interesting, 
            SVNLocationEntry newSearchResumeEntry, FSFS owner) {
        myHistoryEntry = newHistoryEntry;
        mySearchResumeEntry = newSearchResumeEntry;
        myIsInteresting = interesting;
        myFSFS = owner;
    }

    public SVNLocationEntry getHistoryEntry() {
        return myHistoryEntry;
    }

    public static SVNLocationEntry findYoungestCopyroot(File reposRootDir, FSParentPath parPath) throws SVNException {
        SVNLocationEntry parentEntry = null;
        if (parPath.getParent() != null) {
            parentEntry = findYoungestCopyroot(reposRootDir, parPath.getParent());
        }

        SVNLocationEntry myEntry = new SVNLocationEntry(parPath.getRevNode().getCopyRootRevision(), parPath.getRevNode().getCopyRootPath());
        if (parentEntry != null) {
            if (myEntry.getRevision() >= parentEntry.getRevision()) {
                return myEntry;
            }
            return parentEntry;
        }
        return myEntry;
    }

    public static boolean checkAncestryOfPegPath(String fsPath, long pegRev, long futureRev, FSFS owner) throws SVNException {
        FSRevisionRoot root = owner.createRevisionRoot(futureRev);
        FSNodeHistory history = root.getNodeHistory(fsPath);//getNodeHistory(root, fsPath);
        fsPath = null;
        SVNLocationEntry currentHistory = null;
        while (true) {
            history = history.getPreviousHistory(true);
            if (history == null) {
                break;
            }
            currentHistory = new SVNLocationEntry(history.getHistoryEntry().getRevision(), history.getHistoryEntry().getPath());
            if (fsPath == null) {
                fsPath = currentHistory.getPath();
            }
            if (currentHistory.getRevision() <= pegRev) {
                break;
            }
        }

        if (fsPath == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "FATAL error occurred while checking ancestry of peg path");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        return (history != null && (fsPath.equals(currentHistory.getPath())));
    }

    private FSNodeHistory historyPrev(boolean crossCopies) throws SVNException {
        String path = myHistoryEntry.getPath();
        long revision = myHistoryEntry.getRevision();
        boolean reported = myIsInteresting;

        if (mySearchResumeEntry != null && mySearchResumeEntry.getPath() != null && FSRepository.isValidRevision(mySearchResumeEntry.getRevision())) {
            reported = false;
            if (!crossCopies) {
                return null;
            }
            path = mySearchResumeEntry.getPath();
            revision = mySearchResumeEntry.getRevision();
        }

        FSRevisionRoot root = myFSFS.createRevisionRoot(revision);
        FSParentPath parentPath = root.openPath(path, true, true);
        FSRevisionNode revNode = parentPath.getRevNode();
        SVNLocationEntry commitEntry = new SVNLocationEntry(revNode.getCreatedRevision(), 
                                                            revNode.getCreatedPath());

        FSNodeHistory prevHist = null;
        if (revision == commitEntry.getRevision()) {
            if (!reported) {
                prevHist = new FSNodeHistory(commitEntry, true, 
                                             new SVNLocationEntry(SVNRepository.INVALID_REVISION, null),
                                             myFSFS);
                return prevHist;
            }
            FSID predId = revNode.getPredecessorId();
            if (predId == null) {
                return prevHist;
            }
            revNode = myFSFS.getRevisionNode(predId);
            commitEntry = new SVNLocationEntry(revNode.getCreatedRevision(), revNode.getCreatedPath());
        }

        SVNLocationEntry copyrootEntry = findYoungestCopyroot(myFSFS.getRepositoryRoot(), 
                                                              parentPath);
        SVNLocationEntry srcEntry = new SVNLocationEntry(SVNRepository.INVALID_REVISION, null);
        long dstRev = SVNRepository.INVALID_REVISION;
        if (copyrootEntry.getRevision() > commitEntry.getRevision()) {
            FSRevisionRoot copyrootRoot = myFSFS.createRevisionRoot(copyrootEntry.getRevision());
            revNode = copyrootRoot.getRevisionNode(copyrootEntry.getPath());
            String copyDst = revNode.getCreatedPath();
            String reminder = null;
            if (path.equals(copyDst)) {
                reminder = "";
            } else {
                reminder = SVNPathUtil.getPathAsChild(copyDst, path);
            }
            if (reminder != null) {
                String copySrc = revNode.getCopyFromPath();
                srcEntry = new SVNLocationEntry(revNode.getCopyFromRevision(), SVNPathUtil.getAbsolutePath(SVNPathUtil.append(copySrc, reminder)));
                dstRev = copyrootEntry.getRevision();
            }
        }
        if (srcEntry.getPath() != null && FSRepository.isValidRevision(srcEntry.getRevision())) {
            boolean retry = false;
            if ((dstRev == revision) && reported) {
                retry = true;
            }
            return new FSNodeHistory(new SVNLocationEntry(dstRev, path),
                                     retry ? false : true,
                                     new SVNLocationEntry(srcEntry.getRevision(), srcEntry.getPath()), 
                                     myFSFS);
        }
        return new FSNodeHistory(commitEntry, true, 
                                 new SVNLocationEntry(SVNRepository.INVALID_REVISION, null), 
                                 myFSFS);
    }

    public FSNodeHistory getPreviousHistory(boolean crossCopies) throws SVNException {
        if ("/".equals(myHistoryEntry.getPath())) {
            if (!myIsInteresting) {
                return new FSNodeHistory(new SVNLocationEntry(myHistoryEntry.getRevision(), "/"), 
                                         true, 
                                         new SVNLocationEntry(SVNRepository.INVALID_REVISION, null),
                                         myFSFS);
            } else if (myHistoryEntry.getRevision() > 0) {
                return new FSNodeHistory(new SVNLocationEntry(myHistoryEntry.getRevision() - 1, "/"), 
                                         true, 
                                         new SVNLocationEntry(SVNRepository.INVALID_REVISION, null),
                                         myFSFS);
            }
        } else {
            FSNodeHistory prevHist = this;
            while (true) {
                prevHist = prevHist.historyPrev(crossCopies);
                if (prevHist == null) {
                    return null;
                }
                if (prevHist.myIsInteresting) {
                    return prevHist;
                }
            }
        }
        return null;
    }
}
