/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.io.fs;

import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNMergeInfo;
import org.tmatesoft.svn.core.SVNMergeInfoInheritance;
import org.tmatesoft.svn.core.SVNMergeRange;
import org.tmatesoft.svn.core.SVNMergeRangeList;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.internal.delta.SVNDeltaCombiner;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.util.SVNMergeInfoUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.SVNMergeInfoManager;
import org.tmatesoft.svn.core.io.ISVNFileRevisionHandler;
import org.tmatesoft.svn.core.io.SVNFileRevision;
import org.tmatesoft.svn.core.io.SVNLocationEntry;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSFileRevisionsFinder {
    private FSFS myFSFS;
    private SVNDeltaGenerator myDeltaGenerator;
    
    public FSFileRevisionsFinder(FSFS fsfs) {
        myFSFS = fsfs;
    }

    public int getFileRevisions(String path, long startRevision, long endRevision, 
            boolean includeMergedRevisions, ISVNFileRevisionHandler handler) throws SVNException {
        Map duplicatePathRevs = new SVNHashMap();
        LinkedList mainLinePathRevisions = findInterestingRevisions(null, path, startRevision, endRevision, 
                includeMergedRevisions, false, duplicatePathRevs);
        
        LinkedList mergedPathRevisions = null;
        if (includeMergedRevisions) {
            mergedPathRevisions = findMergedRevisions(mainLinePathRevisions, duplicatePathRevs);
        } else {
            mergedPathRevisions = new LinkedList();
        }
       
        SVNErrorManager.assertionFailure(!mainLinePathRevisions.isEmpty(), "no main line path revisions found", SVNLogType.FSFS);

        SendBaton sb = new SendBaton();
        sb.myLastProps = new SVNProperties();
        int mainLinePos = mainLinePathRevisions.size() - 1;
        int mergedPos = mergedPathRevisions.size() - 1;
        int i = 0;
        while (mainLinePos >= 0 && mergedPos >= 0) {
            SVNLocationEntry mainPathRev = (SVNLocationEntry) mainLinePathRevisions.get(mainLinePos);
            SVNLocationEntry mergedPathRev = (SVNLocationEntry) mergedPathRevisions.get(mergedPos);
            if (mainPathRev.getRevision() <= mergedPathRev.getRevision()) {
                sendPathRevision(mainPathRev, sb, handler);
                mainLinePos--;
            } else {
                sendPathRevision(mergedPathRev, sb, handler);
                mergedPos--;
            }
            i++;
        }
        
        for (; mainLinePos >= 0; mainLinePos--) {
            SVNLocationEntry mainPathRev = (SVNLocationEntry) mainLinePathRevisions.get(mainLinePos);
            sendPathRevision(mainPathRev, sb, handler);
            i++;
        }
        
        return i;
    }
    
    private void sendPathRevision(SVNLocationEntry pathRevision, SendBaton sendBaton, 
            ISVNFileRevisionHandler handler) throws SVNException {
        SVNProperties revProps = myFSFS.getRevisionProperties(pathRevision.getRevision());
        FSRevisionRoot root = myFSFS.createRevisionRoot(pathRevision.getRevision());
        FSRevisionNode fileNode = root.getRevisionNode(pathRevision.getPath());
        SVNProperties props = fileNode.getProperties(myFSFS);
        SVNProperties propDiffs = FSRepositoryUtil.getPropsDiffs(sendBaton.myLastProps, props);
        boolean contentsChanged = false;
        if (sendBaton.myLastRoot != null) {
            contentsChanged = FSRepositoryUtil.areFileContentsChanged(sendBaton.myLastRoot, 
                    sendBaton.myLastPath, root, pathRevision.getPath());
        } else {
            contentsChanged = true;
        }

        if (handler != null) {
            handler.openRevision(new SVNFileRevision(pathRevision.getPath(), pathRevision.getRevision(), 
                    revProps, propDiffs, pathRevision.isResultOfMerge()));
            if (contentsChanged) {
                SVNDeltaCombiner sourceCombiner = new SVNDeltaCombiner();
                SVNDeltaCombiner targetCombiner = new SVNDeltaCombiner();
                handler.applyTextDelta(pathRevision.getPath(), null);
                InputStream sourceStream = null;
                InputStream targetStream = null;
                try {
                    if (sendBaton.myLastRoot != null && sendBaton.myLastPath != null) {
                        sourceStream = sendBaton.myLastRoot.getFileStreamForPath(sourceCombiner, 
                                sendBaton.myLastPath);
                    } else {
                        sourceStream = FSInputStream.createDeltaStream(sourceCombiner, (FSRevisionNode) null, 
                                myFSFS);
                    }
                    targetStream = root.getFileStreamForPath(targetCombiner, pathRevision.getPath());
                    SVNDeltaGenerator deltaGenerator = getDeltaGenerator();
                    deltaGenerator.sendDelta(pathRevision.getPath(), sourceStream, 0, targetStream, handler, 
                            false);
                } finally {
                    SVNFileUtil.closeFile(sourceStream);
                    SVNFileUtil.closeFile(targetStream);
                }
                handler.closeRevision(pathRevision.getPath());
            } else {
                handler.closeRevision(pathRevision.getPath());
            }
        }
        sendBaton.myLastRoot = root;
        sendBaton.myLastPath = pathRevision.getPath();
        sendBaton.myLastProps = props;
    }
    
    private SVNDeltaGenerator getDeltaGenerator() {
        if (myDeltaGenerator == null) {
            myDeltaGenerator = new SVNDeltaGenerator();
        }
        return myDeltaGenerator;
    }
    
    private LinkedList findMergedRevisions(LinkedList mainLinePathRevisions, Map duplicatePathRevs) throws SVNException {
        LinkedList mergedPathRevisions = new LinkedList();
        LinkedList oldPathRevisions = mainLinePathRevisions;
        LinkedList newPathRevisions = null;
        do {
            newPathRevisions = new LinkedList();
            for (Iterator oldPathRevsIter = oldPathRevisions.iterator(); oldPathRevsIter.hasNext();) {
                SVNLocationEntry oldPathRevision = (SVNLocationEntry) oldPathRevsIter.next();
                Map mergedMergeInfo = oldPathRevision.getMergedMergeInfo();
                if (mergedMergeInfo == null) {
                    continue;
                }
                for (Iterator mergeInfoIter = mergedMergeInfo.keySet().iterator(); mergeInfoIter.hasNext();) {
                    String path = (String) mergeInfoIter.next();
                    SVNMergeRangeList rangeList = (SVNMergeRangeList) mergedMergeInfo.get(path);
                    SVNMergeRange[] ranges = rangeList.getRanges();
                    for (int j = 0; j < ranges.length; j++) {
                        SVNMergeRange range = ranges[j];
                        FSRevisionRoot root = myFSFS.createRevisionRoot(range.getEndRevision());
                        SVNNodeKind kind = root.checkNodeKind(path);
                        if (kind != SVNNodeKind.FILE) {
                            continue;
                        }
                        
                        newPathRevisions = findInterestingRevisions(newPathRevisions, path, 
                                range.getStartRevision(), range.getEndRevision(), true, true, duplicatePathRevs);
                    }
                }
            }
            mergedPathRevisions.addAll(newPathRevisions);
            oldPathRevisions = newPathRevisions;
        } while (!newPathRevisions.isEmpty());
        
        Collections.sort(mergedPathRevisions, new Comparator() {
            public int compare(Object arg0, Object arg1) {
                SVNLocationEntry pathRevision1 = (SVNLocationEntry) arg0;
                SVNLocationEntry pathRevision2 = (SVNLocationEntry) arg1;
                if (pathRevision1.getRevision() == pathRevision2.getRevision()) {
                    return 0;
                }
                return pathRevision1.getRevision() < pathRevision2.getRevision() ? 1 : -1;
            }
        });
        return mergedPathRevisions;
    }
    
    private LinkedList findInterestingRevisions(LinkedList pathRevisions, String path, 
            long startRevision, long endRevision, boolean includeMergedRevisions, 
            boolean markAsMerged, Map duplicatePathRevs) throws SVNException {
        FSRevisionRoot root = myFSFS.createRevisionRoot(endRevision);
        if (root.checkNodeKind(path) != SVNNodeKind.FILE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_FILE, 
                    "''{0}'' is not a file in revision ''{1}''", 
                    new Object[] { path, new Long(endRevision) });
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        pathRevisions = pathRevisions == null ? new LinkedList() : pathRevisions;
        FSNodeHistory history = root.getNodeHistory(path);
        while (true) {
            history = history.getPreviousHistory(true);
            if (history == null) {
                break;
            }

            long histRev = history.getHistoryEntry().getRevision();
            String histPath = history.getHistoryEntry().getPath();
            
            if (includeMergedRevisions && duplicatePathRevs.containsKey(histPath + ":" + histRev)) {
                break;
            }
            
            SVNLocationEntry pathRev = null;
            Map mergedMergeInfo = null;
            if (includeMergedRevisions) {
                mergedMergeInfo = getMergedMergeInfo(histPath, histRev);
                pathRev = new SVNLocationEntry(histRev, histPath, markAsMerged, mergedMergeInfo);
                duplicatePathRevs.put(histPath + ":" + histRev, pathRev);
            } else {
                pathRev = new SVNLocationEntry(histRev, histPath, markAsMerged, null);
            }
              
            pathRevisions.addLast(pathRev);

            if (histRev <= startRevision) {
                break;
            }
        }
        return pathRevisions;
    }

    private Map getMergedMergeInfo(String path, long revision) throws SVNException {
        Map currentMergeInfo = getPathMergeInfo(path, revision);
        Map previousMergeInfo = null; 
        try {    
            previousMergeInfo = getPathMergeInfo(path, revision - 1);
        } catch (SVNException svne) {
            if (svne.getErrorMessage().getErrorCode() == SVNErrorCode.FS_NOT_FOUND) {
                previousMergeInfo = new TreeMap();
            } else {
                throw svne;
            }
        }
        
        Map deleted = new TreeMap();
        Map changed = new TreeMap();
        SVNMergeInfoUtil.diffMergeInfo(deleted, changed, previousMergeInfo, currentMergeInfo, false);
        changed = SVNMergeInfoUtil.mergeMergeInfos(changed, deleted);
        return changed;
    }

    public Map getPathMergeInfo(String path, long revision) throws SVNException {
        SVNMergeInfoManager mergeInfoManager = new SVNMergeInfoManager();
        FSRevisionRoot root = myFSFS.createRevisionRoot(revision);
        Map tmpMergeInfo = mergeInfoManager.getMergeInfo(new String[] { path }, root, 
                SVNMergeInfoInheritance.INHERITED, false);
        SVNMergeInfo mergeInfo = (SVNMergeInfo) tmpMergeInfo.get(path);
        if (mergeInfo != null) {
            return mergeInfo.getMergeSourcesToMergeLists();
        }
        return new TreeMap();
    }

    private static class SendBaton {
        private FSRevisionRoot myLastRoot;
        private String myLastPath;
        private SVNProperties myLastProps;
    }
}
