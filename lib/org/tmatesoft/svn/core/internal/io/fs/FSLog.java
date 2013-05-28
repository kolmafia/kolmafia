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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNMergeInfo;
import org.tmatesoft.svn.core.SVNMergeInfoInheritance;
import org.tmatesoft.svn.core.SVNMergeRange;
import org.tmatesoft.svn.core.SVNMergeRangeList;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNRevisionProperty;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.util.SVNHashSet;
import org.tmatesoft.svn.core.internal.util.SVNMergeInfoUtil;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNMergeDriver;
import org.tmatesoft.svn.core.internal.wc.SVNMergeInfoManager;
import org.tmatesoft.svn.core.io.ISVNLocationSegmentHandler;
import org.tmatesoft.svn.core.io.SVNLocationEntry;
import org.tmatesoft.svn.core.io.SVNLocationSegment;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNRevision;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSLog {
    private static final int MAX_OPEN_HISTORIES = 128;  
    
    private FSFS myFSFS;
    private String[] myPaths;
    private boolean myIsDescending;
    private boolean myIsDiscoverChangedPaths;
    private boolean myIsStrictNode;
    private boolean myIsIncludeMergedRevisions;
    private long myStartRevision;
    private long myEndRevision;
    private long myLimit;
    private ISVNLogEntryHandler myHandler;
    private SVNMergeInfoManager myMergeInfoManager;
    private String[] myRevPropNames;
    
    private static final Comparator RLP_COMPARATOR = new Comparator() {
        public int compare(Object arg1, Object arg2) {
            RangeListPath rangeListPath1 = (RangeListPath) arg1;
            RangeListPath rangeListPath2 = (RangeListPath) arg2;
            SVNMergeRange[] ranges1 = rangeListPath1.myRangeList.getRanges();
            SVNMergeRange[] ranges2 = rangeListPath2.myRangeList.getRanges();
            SVNMergeRange range1 = ranges1[0];
            SVNMergeRange range2 = ranges2[0];
            if (range1.getStartRevision() < range2.getStartRevision()) {
                return -1;
            }
            if (range1.getStartRevision() > range2.getStartRevision()) {
                return 1;
            }
            if (range1.getEndRevision() < range2.getEndRevision()) {
                return -1;
            }
            if (range1.getEndRevision() > range2.getEndRevision()) {
                return 1;
            }
            return 0;
        }
    };
    
    private static final Comparator PLR_COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            PathListRange plr1 = (PathListRange) o1;
            PathListRange plr2 = (PathListRange) o2;
            
            if (plr1.myRange.getStartRevision() < plr2.myRange.getStartRevision()) {
                return -1;
            }
            if (plr1.myRange.getStartRevision() > plr2.myRange.getStartRevision()) {
                return 1;
            }
            if (plr1.myRange.getEndRevision() < plr2.myRange.getEndRevision()) {
                return -1;
            }
            if (plr1.myRange.getEndRevision() < plr2.myRange.getEndRevision()) {
                return 1;
            }
            return 0;
        }
    };
    
    public FSLog(FSFS owner, String[] paths, long limit, long start, long end, boolean descending, 
            boolean discoverChangedPaths, boolean strictNode, boolean includeMergedRevisions, 
            String[] revPropNames, ISVNLogEntryHandler handler) {
        myFSFS = owner;
        myPaths = paths;
        myStartRevision = start;
        myEndRevision = end;
        myIsDescending = descending;
        myIsDiscoverChangedPaths = discoverChangedPaths;
        myIsStrictNode = strictNode;
        myIsIncludeMergedRevisions = includeMergedRevisions;
        myRevPropNames = revPropNames;
        myLimit = limit;
        myHandler = handler;
    }
    
    public void reset(FSFS owner, String[] paths, long limit, long start, long end, boolean descending, 
            boolean discoverChangedPaths, boolean strictNode, boolean includeMergedRevisions, 
            String[] revPropNames, ISVNLogEntryHandler handler) {
        myFSFS = owner;
        myPaths = paths;
        myStartRevision = start;
        myEndRevision = end;
        myIsDescending = descending;
        myIsDiscoverChangedPaths = discoverChangedPaths;
        myIsStrictNode = strictNode;
        myIsIncludeMergedRevisions = includeMergedRevisions;
        myRevPropNames = revPropNames;
        myLimit = limit;
        myHandler = handler;
    }
    
    public long runLog() throws SVNException {
        long count = 0;
        if (!myIsIncludeMergedRevisions && myPaths.length == 1 && "/".equals(myPaths[0])) {
            count = myEndRevision - myStartRevision + 1;
            if (myLimit > 0 && count > myLimit) {
                count = myLimit;
            }
        
            for (int i = 0; i < count; i++) {
                long rev = myStartRevision + i;
                if (myIsDescending) {
                    rev = myEndRevision - i;
                }
                sendLog(rev, null, null, false, false, false);
            }
            
            return count;
        }

        Map logTargetHistoryAsMergeInfo = null;
        if (myIsIncludeMergedRevisions) {
            logTargetHistoryAsMergeInfo = getPathsHistoryAsMergeInfo(myPaths, myStartRevision, myEndRevision);
        }
        return doLogs(myPaths, logTargetHistoryAsMergeInfo, null, myStartRevision, myEndRevision, myIsIncludeMergedRevisions, false, false, myIsDescending, myLimit);
    }
    
    private long doLogs(String[] paths, Map logTargetHistoryAsMergeinfo, Set nestedMerges, long startRevision, long endRevision, boolean includeMergedRevisions, 
            boolean subtractiveMerge, boolean handlingMergedRevisions, boolean isDescendingOrder, long limit) throws SVNException {
        long sendCount = 0;
        PathInfo[] histories = getPathHistories(paths, startRevision, endRevision, myIsStrictNode);
        
        LinkedList revisions = null;
        Map revMergeInfo = null;
        boolean anyHistoriesLeft = true;
        for (long currentRev = endRevision; anyHistoriesLeft; currentRev = getNextHistoryRevision(histories)) {
            
            boolean changed = false;
            anyHistoriesLeft = false;
            
            for (int i = 0; i < histories.length; i++) {
                PathInfo info = histories[i];
                changed = info.checkHistory(currentRev, myIsStrictNode, startRevision, changed);
                
                if (!info.myIsDone) {
                    anyHistoriesLeft = true;
                }
            }

            if (changed) {
                boolean hasChildren = false;
                Map addedMergeInfo = null;
                Map deletedMergeInfo = null;
                Map[] mergeInfo = null;
                if (includeMergedRevisions) {
                    LinkedList currentPaths = new LinkedList();
                    for (int i = 0; i < histories.length; i++) {
                        PathInfo info = histories[i];
                        currentPaths.add(info.myPath);
                    }
                    mergeInfo = getCombinedMergeInfoChanges((String[]) currentPaths.toArray(new String[currentPaths.size()]), currentRev);
                    addedMergeInfo = mergeInfo[0];
                    deletedMergeInfo = mergeInfo[1];
                    hasChildren = (addedMergeInfo.size() > 0 || deletedMergeInfo.size() > 0);
                }
                
                if (isDescendingOrder) {
                    sendLog(currentRev, logTargetHistoryAsMergeinfo, nestedMerges, subtractiveMerge, handlingMergedRevisions, hasChildren);
                    sendCount++;
                    if (hasChildren) {
                        if (nestedMerges == null) {
                            nestedMerges = new SVNHashSet();
                        }
                        handleMergedRevisions(addedMergeInfo, deletedMergeInfo, logTargetHistoryAsMergeinfo, nestedMerges);
                    }
                    if (limit > 0 && sendCount >= limit) {
                        break;
                    }
                } else {
                    if (revisions == null) {
                        revisions = new LinkedList();
                    }
                    revisions.addLast(new Long(currentRev));
                    
                    if (mergeInfo != null) {
                        if (revMergeInfo == null) {
                            revMergeInfo = new TreeMap();
                        }
                        revMergeInfo.put(new Long(currentRev), mergeInfo);
                    }
                }
            }
        }
        
        nestedMerges = null;

        if (revisions != null) {
            for (int i = 0; i < revisions.size(); i++) {
                boolean hasChildren = false;
                Map[] mergeInfo = null;
                long rev = ((Long) revisions.get(revisions.size() - i - 1)).longValue();
                
                if (revMergeInfo != null) {
                    mergeInfo = (Map[]) revMergeInfo.get(new Long(rev));
                    if (mergeInfo != null && mergeInfo.length == 2) {
                        hasChildren = !mergeInfo[0].isEmpty() || !mergeInfo[1].isEmpty();
                    }
                }
                sendLog(rev, logTargetHistoryAsMergeinfo, nestedMerges, subtractiveMerge, handlingMergedRevisions, hasChildren);
                if (hasChildren) {
                    if (nestedMerges == null) {
                        nestedMerges = new SVNHashSet();
                    }
                    handleMergedRevisions(mergeInfo[0], mergeInfo[1], logTargetHistoryAsMergeinfo, nestedMerges);
                }
                sendCount++;
                if (limit > 0 && sendCount >= limit) {
                    break;
                }
            }
        }
        return sendCount;
    }
    
    private long getNextHistoryRevision(PathInfo[] histories) {
        long nextRevision = SVNRepository.INVALID_REVISION;
        for (int i = 0; i < histories.length; i++) {
            PathInfo info = histories[i];
            if (info.myIsDone) {
                continue;
            }
            if (info.myHistoryRevision > nextRevision) {
                nextRevision = info.myHistoryRevision;
            }
        }
        return nextRevision;
    }

    private void sendLog(long revision, Map logTargetHistoryAsMergeInfo, Set nestedMerges, boolean subtractiveMerge, boolean handlingMergedRevision, boolean hasChildren) throws SVNException {
        if (myHandler == null) {
            return;
        }
        SVNLogEntry logEntry = fillLogEntry(revision, myIsDiscoverChangedPaths || handlingMergedRevision);
        logEntry.setHasChildren(hasChildren);
        logEntry.setSubtractiveMerge(subtractiveMerge);
        boolean revisionIsInteresting = true;
        if (handlingMergedRevision && !logEntry.getChangedPaths().isEmpty() && logTargetHistoryAsMergeInfo != null && !logTargetHistoryAsMergeInfo.isEmpty()) {
            boolean pathIsInHistory = false;
            revisionIsInteresting = false;
            for (Iterator changedPaths = logEntry.getChangedPaths().keySet().iterator(); changedPaths.hasNext();) {
                String changedPath = (String) changedPaths.next();
                for(Iterator mergedPaths = logTargetHistoryAsMergeInfo.keySet().iterator(); mergedPaths.hasNext();) {
                    String mergedPath = (String) mergedPaths.next();
                    if (SVNPathUtil.isAncestor(mergedPath, changedPath)) {
                        SVNMergeRangeList rangeList = (SVNMergeRangeList) logTargetHistoryAsMergeInfo.get(mergedPath);
                        SVNMergeRange[] ranges = rangeList.getRanges();
                        for (int i = 0; i < ranges.length; i++) {
                            if (revision > ranges[i].getStartRevision() && revision <= ranges[i].getEndRevision()) {
                                pathIsInHistory = true;
                                break;
                            }
                        }
                        if (pathIsInHistory) {
                            break;
                        }
                    }
                }
                if (!pathIsInHistory) {
                    revisionIsInteresting = true;
                    break;
                }
                
            }
        }
        if (!myIsDiscoverChangedPaths) {
            logEntry.getChangedPaths().clear();
        }
        if (revisionIsInteresting) {
            if (handlingMergedRevision && nestedMerges != null) {
                if (nestedMerges.contains(new Long(revision))) {
                    return;
                }
                nestedMerges.add(new Long(revision));
            }
            myHandler.handleLogEntry(logEntry);
        }
    }

    private SVNLogEntry fillLogEntry(long revision, boolean discoverChangedPaths) throws SVNException {
        Map changedPaths = null;
        SVNProperties entryRevProps = null;
        boolean getRevProps = true;
        boolean censorRevProps = false;
        if (revision > 0 && discoverChangedPaths) {
            FSRevisionRoot root = myFSFS.createRevisionRoot(revision);
            changedPaths = root.detectChanged();
        }

        if (getRevProps) {
            SVNProperties revisionProps = myFSFS.getRevisionProperties(revision);

            if (revisionProps != null) {
                String author = revisionProps.getStringValue(SVNRevisionProperty.AUTHOR);
                String datestamp = revisionProps.getStringValue(SVNRevisionProperty.DATE);
                Date date = datestamp != null ? SVNDate.parseDateString(datestamp) : null;

                if (myRevPropNames == null || myRevPropNames.length == 0) {
                    if (censorRevProps) {
                        entryRevProps = new SVNProperties();
                        if (author != null) {
                            entryRevProps.put(SVNRevisionProperty.AUTHOR, author);
                        }
                        if (date != null) {
                            entryRevProps.put(SVNRevisionProperty.DATE, SVNDate.formatDate(date));
                        }
                    } else {
                        entryRevProps = revisionProps;
                        if (date != null) {
                            entryRevProps.put(SVNRevisionProperty.DATE, SVNDate.formatDate(date));
                        }
                    }
                } else {
                    for (int i = 0; i < myRevPropNames.length; i++) {
                        String propName = myRevPropNames[i];
                        SVNPropertyValue propVal = revisionProps.getSVNPropertyValue(propName);
                        if (censorRevProps && !SVNRevisionProperty.AUTHOR.equals(propName) &&
                                !SVNRevisionProperty.DATE.equals(propName)) {
                            continue;
                        }
                        if (entryRevProps == null) {
                            entryRevProps = new SVNProperties();
                        }
                        if (SVNRevisionProperty.DATE.equals(propName) && date != null) {
                            entryRevProps.put(propName, SVNDate.formatDate(date));
                        } else if (propVal != null) {
                            entryRevProps.put(propName, propVal);
                        }
                    }
                }
            }
        }
        
        if (changedPaths == null) {
            changedPaths = new SVNHashMap();
        }
        if (entryRevProps == null) {
            entryRevProps = new SVNProperties();
        }
        SVNLogEntry entry = new SVNLogEntry(changedPaths, revision, entryRevProps, false);
        return entry;
    }
    
    private void handleMergedRevisions(Map addedMergeInfo, Map deletedMergeInfo, Map logTargetHistoryAsMergeInfo, Set nestedMerges) throws SVNException {
        if ((addedMergeInfo == null || addedMergeInfo.isEmpty()) &&
                (deletedMergeInfo == null || deletedMergeInfo.isEmpty())) {           
            return;
        }
        
        LinkedList combinedList = new LinkedList();
        if (addedMergeInfo != null && addedMergeInfo.size() > 0) {
            combinedList = combineMergeInfoPathLists(addedMergeInfo, false);
        }
        if (deletedMergeInfo != null && deletedMergeInfo.size() > 0) {
            combinedList.addAll(combineMergeInfoPathLists(deletedMergeInfo, true));
        }
        
        Collections.sort(combinedList, PLR_COMPARATOR);

        for (int i = combinedList.size() - 1; i >= 0; i--) {
            PathListRange pathListRange = (PathListRange) combinedList.get(i);
            try {
                doLogs(pathListRange.myPaths, logTargetHistoryAsMergeInfo, nestedMerges, pathListRange.myRange.getStartRevision(), 
                        pathListRange.myRange.getEndRevision(), true, pathListRange.reverseMerge, true, true, 0);
            } catch (SVNException svne) {
                SVNErrorCode errCode = svne.getErrorMessage().getErrorCode();
                if (errCode == SVNErrorCode.FS_NOT_FOUND || errCode == SVNErrorCode.FS_NO_SUCH_REVISION) {
                    continue;
                }
                throw svne;
            }
        }
        if (myHandler != null) {
            myHandler.handleLogEntry(SVNLogEntry.EMPTY_ENTRY);
        }
    }
    
    private Map getPathsHistoryAsMergeInfo(String[] paths, long startRevision, long endRevision) throws SVNException {
        if (startRevision < endRevision) {
            long temp = startRevision;
            startRevision = endRevision;
            endRevision = temp;
        }
        Map target = new SVNHashMap();
        FSLocationsFinder locationsFinder = new FSLocationsFinder(myFSFS);
        final Collection locationSegments = new ArrayList();        
        ISVNLocationSegmentHandler locationsReceiver = new ISVNLocationSegmentHandler() {
            public void handleLocationSegment(SVNLocationSegment locationSegment) throws SVNException {
                locationSegments.add(locationSegment);                    
            }
        };
        
        for (int i = 0; i < paths.length; i++) {
            locationsFinder.getNodeLocationSegments(paths[i], startRevision, startRevision, endRevision, locationsReceiver);
            Map mergeInfo = SVNMergeDriver.getMergeInfoFromSegments(locationSegments);
            target = SVNMergeInfoUtil.mergeMergeInfos(target, mergeInfo);
        }
        return target;
    }

    private PathInfo[] getPathHistories(String[] paths, long start, long end, boolean strictNodeHistory) throws SVNException {
        PathInfo[] histories = new PathInfo[paths.length];
        FSRevisionRoot root = myFSFS.createRevisionRoot(end);
        for (int i = 0; i < paths.length; i++) {
            String path = paths[i];
            
            PathInfo pathHistory = new PathInfo();
            pathHistory.myPath = path;
            pathHistory.myHistoryRevision = end;
            pathHistory.myIsDone = false;
            pathHistory.myIsFirstTime = true;
            
            if (i < MAX_OPEN_HISTORIES) {
                pathHistory.myHistory = root.getNodeHistory(path);
            }
            
            histories[i] = pathHistory.getHistory(strictNodeHistory, start);
        }
        return histories;
    }
    
    private Map[] getCombinedMergeInfoChanges(String[] paths, long revision) throws SVNException {
        if (revision == 0) {
            return new Map[] {new TreeMap(), new TreeMap()};
        }
        if (paths == null || paths.length == 0) {
            return new Map[] {new TreeMap(), new TreeMap()};
        }
        Map resultAdded = new SVNHashMap();
        Map resultDeleted = new SVNHashMap();
        Map addedMergeInfoCatalog = new SVNHashMap();
        Map deletedMergeInfoCatalog = new SVNHashMap();
        FSRevisionRoot root = myFSFS.createRevisionRoot(revision);
        
        collectChangedMergeInfo(addedMergeInfoCatalog, deletedMergeInfoCatalog, revision);
        
        for (int i = 0; i < paths.length; i++) {
            String path = paths[i];
            if (deletedMergeInfoCatalog.containsKey(path)) {
                continue;
            }
            long[] appearedRevision = new long[] {-1};
            SVNLocationEntry prevLocation = null;
            try {
                prevLocation = myFSFS.getPreviousLocation(path, revision, appearedRevision);                
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() == SVNErrorCode.FS_NOT_FOUND) {
                    continue;
                }
                throw e;
            }
            String prevPath = null;
            long prevRevision = -1;
            if (!(prevLocation != null && prevLocation.getPath() != null && prevLocation.getRevision() >=0 && appearedRevision[0] == revision)) {
                prevPath = path;
                prevRevision = revision - 1;
            } else if (prevLocation != null) {
                prevPath = prevLocation.getPath();
                prevRevision = prevLocation.getRevision();
            }
            
            FSRevisionRoot prevRoot = myFSFS.createRevisionRoot(prevRevision);
            String[] queryPaths = new String[] {prevPath};
            Map catalog = null;
            try {
                catalog = getMergeInfoManager().getMergeInfo(queryPaths, prevRoot, SVNMergeInfoInheritance.INHERITED, false);
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() == SVNErrorCode.FS_NOT_FOUND) {
                    continue;
                }
                throw e;
            }
            SVNMergeInfo prevMergeinfo = (SVNMergeInfo) catalog.get(prevPath);
            queryPaths = new String[] {path};
            catalog = getMergeInfoManager().getMergeInfo(queryPaths, root, SVNMergeInfoInheritance.INHERITED, false);
            SVNMergeInfo mergeInfo = (SVNMergeInfo) catalog.get(path);
            
            Map deleted = new SVNHashMap();
            Map added = new SVNHashMap();
            
            SVNMergeInfoUtil.diffMergeInfo(deleted, added, 
                    prevMergeinfo != null ? prevMergeinfo.getMergeSourcesToMergeLists() : null, mergeInfo != null ? mergeInfo.getMergeSourcesToMergeLists() : null, false);
            
            resultAdded = SVNMergeInfoUtil.mergeMergeInfos(resultAdded, added);            
            resultDeleted = SVNMergeInfoUtil.mergeMergeInfos(resultDeleted, deleted);            
        }
        for(Iterator ps = addedMergeInfoCatalog.keySet().iterator(); ps.hasNext();) {
            String changedPath = (String) ps.next();
            Map addedMergeInfo = (Map) addedMergeInfoCatalog.get(changedPath);
            for (int i = 0; i < paths.length; i++) {
                String path = paths[i];
                if (!SVNPathUtil.isAncestor(path, changedPath)) {
                    continue;
                }
                Map deletedMergeInfo = (Map) deletedMergeInfoCatalog.get(changedPath);
                resultDeleted = SVNMergeInfoUtil.mergeMergeInfos(resultDeleted, deletedMergeInfo);
                resultAdded = SVNMergeInfoUtil.mergeMergeInfos(resultAdded, addedMergeInfo);
                break;
            }
        }
        return new Map[] {resultAdded, resultDeleted};
    }
    
    private void collectChangedMergeInfo(Map addedMergeInfo, Map deletedMergeInfo, long revision) throws SVNException {
        if (revision == 0) {
            return;
        }
        FSRevisionRoot root = myFSFS.createRevisionRoot(revision);
        Map changedPaths = root.getChangedPaths();
        if (changedPaths == null || changedPaths.isEmpty()) {
            return;
        }
        for (Iterator paths = changedPaths.keySet().iterator(); paths.hasNext();) {
            String changedPath = (String) paths.next();
            FSPathChange change = (FSPathChange) changedPaths.get(changedPath);
            if (!change.arePropertiesModified()) {
                continue;
            }
            FSPathChangeKind changeKind = change.getChangeKind();
            
            String basePath = null;
            long baseRevision = -1;
            String mergeInfoValue = null;
            String previousMergeInfoValue = null;
            
            if (changeKind == FSPathChangeKind.FS_PATH_CHANGE_ADD ||
                    changeKind == FSPathChangeKind.FS_PATH_CHANGE_REPLACE) {
                String copyFromPath = change.getCopyPath();
                long copyFromRev = change.getCopyRevision();
                if (copyFromPath != null && copyFromRev >= 0) {
                    basePath = copyFromPath;
                    baseRevision = copyFromRev;
                }
            } else if (changeKind == FSPathChangeKind.FS_PATH_CHANGE_MODIFY) {
                long[] appearedRevision = new long[] {-1};
                SVNLocationEntry prevLocation = myFSFS.getPreviousLocation(changedPath, revision, appearedRevision);
                if (!(prevLocation != null && 
                        prevLocation.getPath() != null && prevLocation.getRevision() >= 0 && appearedRevision[0] == prevLocation.getRevision())) {
                    basePath = changedPath;
                    baseRevision = revision - 1;
                } else {
                    basePath = prevLocation.getPath();
                    baseRevision = prevLocation.getRevision();
                }
            } else {
                continue;
            }
            
            FSRevisionRoot baseRoot = null;
            if (basePath != null && baseRevision >= 0) {
                baseRoot = myFSFS.createRevisionRoot(baseRevision);
                SVNProperties props = myFSFS.getProperties(baseRoot.getRevisionNode(basePath));
                previousMergeInfoValue = props.getStringValue(SVNProperty.MERGE_INFO);
            }

            SVNProperties props = myFSFS.getProperties(root.getRevisionNode(changedPath));
            if (props != null) {
                mergeInfoValue = props.getStringValue(SVNProperty.MERGE_INFO);
            }
  
            if (mergeInfoValue == null && previousMergeInfoValue == null) {
                continue;
            }
            
            if (previousMergeInfoValue != null && mergeInfoValue == null) {
              String[] queryPaths = new String[] { changedPath };
              Map tmpCatalog = getMergeInfoManager().getMergeInfo(queryPaths, root, SVNMergeInfoInheritance.INHERITED, false);
              SVNMergeInfo tmpMergeInfo = (SVNMergeInfo) tmpCatalog.get(changedPath);
              if (tmpMergeInfo != null) {
                  mergeInfoValue = SVNMergeInfoUtil.formatMergeInfoToString(tmpMergeInfo.getMergeSourcesToMergeLists(), null);
              }
            } else if (mergeInfoValue != null && previousMergeInfoValue == null && basePath != null && 
                    SVNRevision.isValidRevisionNumber(baseRevision)) {
                String[] queryPaths = new String[] { basePath };
                Map tmpCatalog = getMergeInfoManager().getMergeInfo(queryPaths, baseRoot, SVNMergeInfoInheritance.INHERITED, false);
                SVNMergeInfo tmpMergeInfo = (SVNMergeInfo) tmpCatalog.get(basePath);
                if (tmpMergeInfo != null) {
                    previousMergeInfoValue = SVNMergeInfoUtil.formatMergeInfoToString(tmpMergeInfo.getMergeSourcesToMergeLists(), null);
                }
            }
        
            if ((previousMergeInfoValue != null && mergeInfoValue == null) ||
                    (previousMergeInfoValue == null && mergeInfoValue != null) ||
                    (previousMergeInfoValue != null && mergeInfoValue != null &&
                            !previousMergeInfoValue.equals(mergeInfoValue))) {
                Map mergeInfo = null;
                Map previousMergeInfo = null;
                if (mergeInfoValue != null) {
                    mergeInfo = SVNMergeInfoUtil.parseMergeInfo(new StringBuffer(mergeInfoValue), null);
                }
                if (previousMergeInfoValue != null) {
                    previousMergeInfo = SVNMergeInfoUtil.parseMergeInfo(new StringBuffer(previousMergeInfoValue), null);
                }
                Map added = new SVNHashMap();
                Map deleted = new SVNHashMap();
                SVNMergeInfoUtil.diffMergeInfo(deleted, added, previousMergeInfo, mergeInfo, false);
                
                addedMergeInfo.put(changedPath, added);
                deletedMergeInfo.put(changedPath, deleted);
            }
        }
        
    }

    private LinkedList combineMergeInfoPathLists(Map mergeInfo, boolean reverseMerge) {
        List rangeListPaths = new LinkedList();
        for (Iterator pathsIter = mergeInfo.keySet().iterator(); pathsIter.hasNext();) {
            String path = (String) pathsIter.next();
            SVNMergeRangeList changes = (SVNMergeRangeList) mergeInfo.get(path);
            RangeListPath rangeListPath = new RangeListPath();
            rangeListPath.myPath = path;
            rangeListPath.myRangeList = changes.dup();
            SVNMergeRange[] rangesArray = rangeListPath.myRangeList.getRanges(); 
            for (int i = 0; i < rangesArray.length; i++) {
                SVNMergeRange range = rangesArray[i];
                range.setStartRevision(range.getStartRevision() + 1);
            }
            rangeListPaths.add(rangeListPath);
        }
        
        LinkedList combinedList = new LinkedList();
        
        while (rangeListPaths.size() > 1) {
            Collections.sort(rangeListPaths, RLP_COMPARATOR);
            RangeListPath rangeListPath = (RangeListPath) rangeListPaths.get(0);
            RangeListPath firstRLP = rangeListPath;
            long youngest = rangeListPath.myRangeList.getRanges()[0].getStartRevision();
            long nextYoungest = youngest;
            int numRevs = 1;
            for (; nextYoungest == youngest; numRevs++) {
                if (numRevs == rangeListPaths.size()) {
                    numRevs++;
                    break;
                }
                rangeListPath = (RangeListPath) rangeListPaths.get(numRevs);
                nextYoungest = rangeListPath.myRangeList.getRanges()[0].getStartRevision();
            }
            numRevs--;
            long youngestEnd = firstRLP.myRangeList.getRanges()[0].getEndRevision();
            long tail = nextYoungest - 1;
            if (nextYoungest == youngest || youngestEnd < nextYoungest) {
                tail = youngestEnd;
            }

            PathListRange pathListRange = new PathListRange();
            pathListRange.reverseMerge = reverseMerge;
            pathListRange.myRange = new SVNMergeRange(youngest, tail, false);
            List paths = new LinkedList();
            for (int i = 0; i < numRevs; i++) {
                RangeListPath rp = (RangeListPath) rangeListPaths.get(i); 
                paths.add(rp.myPath);
            }
            pathListRange.myPaths = (String[]) paths.toArray(new String[paths.size()]);
        
            combinedList.add(pathListRange);
            
            for (int i = 0; i < numRevs; i++) {
                RangeListPath rp = (RangeListPath) rangeListPaths.get(i);
                SVNMergeRange range = rp.myRangeList.getRanges()[0];
                range.setStartRevision(tail + 1);
                if (range.getStartRevision() > range.getEndRevision()) {
                    if (rp.myRangeList.getSize() == 1) {
                        rangeListPaths.remove(0);
                        i--;
                        numRevs--;
                    } else {
                        SVNMergeRange[] ranges = new SVNMergeRange[rp.myRangeList.getSize() - 1];
                        System.arraycopy(rp.myRangeList.getRanges(), 1, ranges, 0, ranges.length);
                        rp.myRangeList = new SVNMergeRangeList(ranges);
                    }
                }
            }
        }
        
        if (!rangeListPaths.isEmpty()) {
            RangeListPath firstRangeListPath = (RangeListPath) rangeListPaths.get(0);
            while (!firstRangeListPath.myRangeList.isEmpty()) {
                PathListRange pathListRange = new PathListRange();
                pathListRange.reverseMerge = reverseMerge;
                pathListRange.myPaths = new String[] { firstRangeListPath.myPath };
                pathListRange.myRange = firstRangeListPath.myRangeList.getRanges()[0];
                SVNMergeRange[] ranges = new SVNMergeRange[firstRangeListPath.myRangeList.getSize() - 1];
                System.arraycopy(firstRangeListPath.myRangeList.getRanges(), 1, ranges, 0, ranges.length);
                firstRangeListPath.myRangeList = new SVNMergeRangeList(ranges);
                combinedList.add(pathListRange);
            }
        }
        
        return combinedList;
    }

    private SVNMergeInfoManager getMergeInfoManager() {
        if (myMergeInfoManager == null) {
            myMergeInfoManager = new SVNMergeInfoManager();
        }
        return myMergeInfoManager;
    }
    
    private class RangeListPath {
        String myPath;
        SVNMergeRangeList myRangeList;
    }
    
    private class PathListRange {
        public boolean reverseMerge;
        String myPaths[];
        SVNMergeRange myRange;
    }
    
    private class PathInfo {
        FSNodeHistory myHistory;
        boolean myIsDone;
        boolean myIsFirstTime;
        long myHistoryRevision;
        String myPath;
        
        public PathInfo getHistory(boolean strictNodeHistory, long start) throws SVNException {
            FSNodeHistory history = null;
            if (myHistory != null) {
                history = myHistory.getPreviousHistory(strictNodeHistory ? false : true);
                myHistory = history;
            } else {
                FSRevisionRoot historyRoot = myFSFS.createRevisionRoot(myHistoryRevision);
                history = historyRoot.getNodeHistory(myPath);
                history = history.getPreviousHistory(strictNodeHistory ? false : true);
                if (myIsFirstTime) {
                    myIsFirstTime = false;
                } else if (history != null) {
                    history = history.getPreviousHistory(strictNodeHistory ? false : true);
                }
            }

            if (history == null) {
                myIsDone = true;
                return this;
            }

            myPath = history.getHistoryEntry().getPath();
            myHistoryRevision = history.getHistoryEntry().getRevision();
            
            if (myHistoryRevision < start) {
                myIsDone = true;
            }
            return this;
        }

        public boolean checkHistory(long currentRevision, boolean strictNodeHistory, 
                                    long start, boolean changed) throws SVNException {
            if (myIsDone) {
                return changed;
            }
            
            if (myHistoryRevision < currentRevision) {
                return changed;
            }
            
            changed = true;
            getHistory(strictNodeHistory, start);
            return changed;
        }
    }
    
}
