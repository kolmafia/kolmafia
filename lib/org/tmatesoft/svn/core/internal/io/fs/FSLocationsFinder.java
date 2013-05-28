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
import java.util.Arrays;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.ISVNLocationEntryHandler;
import org.tmatesoft.svn.core.io.ISVNLocationSegmentHandler;
import org.tmatesoft.svn.core.io.SVNLocationEntry;
import org.tmatesoft.svn.core.io.SVNLocationSegment;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSLocationsFinder {
    
    private FSFS myFSFS;

    public FSLocationsFinder(FSFS fsfs) {
        myFSFS = fsfs;
    }
    
    public int traceNodeLocations(String path, long pegRevision, long[] revisions, 
            ISVNLocationEntryHandler handler) throws SVNException {
        ArrayList locationEntries = new ArrayList(0);
        long[] locationRevs = new long[revisions.length];
        long revision;
        Arrays.sort(revisions);

        for (int i = 0; i < revisions.length; ++i) {
            locationRevs[i] = revisions[revisions.length - (i + 1)];
        }

        int count = 0;
        boolean isAncestor = false;
        
        for (count = 0; count < locationRevs.length && locationRevs[count] > pegRevision; ++count) {
            isAncestor = FSNodeHistory.checkAncestryOfPegPath(path, pegRevision, locationRevs[count], myFSFS);
            if (isAncestor) {
                break;
            }
        }
        
        if (count >= locationRevs.length) {
            return 0;
        }
        revision = isAncestor ? locationRevs[count] : pegRevision;

        FSRevisionRoot root = null;
        while (count < revisions.length) {
            long[] appearedRevision = new long[1];
            root = myFSFS.createRevisionRoot(revision);
            SVNLocationEntry previousLocation = root.getPreviousLocation(path, appearedRevision);
            if (previousLocation == null) {
                break;
            }
            String previousPath = previousLocation.getPath();
            long previousRevision = previousLocation.getRevision();
            while ((count < revisions.length) && (locationRevs[count] >= appearedRevision[0])) {
                locationEntries.add(new SVNLocationEntry(locationRevs[count], path));
                ++count;
            }

            while ((count < revisions.length) && locationRevs[count] > previousRevision) {
                ++count;
            }
            
            path = previousPath;
            revision = previousRevision;
        }
        
        if (root != null && revision != root.getRevision()) {
            root = myFSFS.createRevisionRoot(revision);
        }
        FSRevisionNode curNode = root.getRevisionNode(path);

        while (count < revisions.length) {
            root = myFSFS.createRevisionRoot(locationRevs[count]);
            if (root.checkNodeKind(path) == SVNNodeKind.NONE) {
                break;
            }
            FSRevisionNode currentNode = root.getRevisionNode(path);
            if (!curNode.getId().isRelated(currentNode.getId())) {
                break;
            }
            locationEntries.add(new SVNLocationEntry(locationRevs[count], path));
            ++count;
        }
        
        for (count = 0; count < locationEntries.size(); count++) {
            if (handler != null) {
                handler.handleLocationEntry((SVNLocationEntry) locationEntries.get(count));
            }
        }
        return count;
    }
    
    public long getNodeLocationSegments(String path, long pegRevision, long startRevision, 
            long endRevision, ISVNLocationSegmentHandler handler) throws SVNException {
        long youngestRevision = SVNRepository.INVALID_REVISION; 
        if (FSRepository.isInvalidRevision(pegRevision)) {
            pegRevision = youngestRevision = myFSFS.getYoungestRevision();
        }
        
        if (FSRepository.isInvalidRevision(startRevision)) {
            if (FSRepository.isValidRevision(youngestRevision)) {
                startRevision = youngestRevision;
            } else {
                startRevision = myFSFS.getYoungestRevision();
            }
        }
        
        if (FSRepository.isInvalidRevision(endRevision)) {
            endRevision = 0;
        }
        
        if (endRevision > startRevision) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, 
                    "End revision {0} must be less or equal to start revision {1}",
                    new Object[] { new Long(endRevision), new Long(startRevision) });
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        if (pegRevision < startRevision) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, 
                    "Peg revision {0} must be greater or equal to start revision {1}",
                    new Object[] { new Long(pegRevision), new Long(startRevision) });
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        
        long count = 0;
        long currentRevision = pegRevision;
        String currentPath = path;
        long[] appearedRevision = new long[1];
        while (currentRevision >= endRevision) {
            long segmentStartRevision = endRevision;
            long segmentEndRevision = currentRevision;
            String segmentPath = currentPath;
            FSRevisionRoot root = myFSFS.createRevisionRoot(currentRevision);
            SVNLocationEntry previousLocation = root.getPreviousLocation(currentPath, appearedRevision);
            if (previousLocation == null) {
                segmentStartRevision = root.getNodeOriginRevision(currentPath);
                if (segmentStartRevision < endRevision) {
                    segmentStartRevision = endRevision;
                }
                currentRevision = SVNRepository.INVALID_REVISION;
            } else {
                segmentStartRevision = appearedRevision[0];
                currentPath = previousLocation.getPath();
                currentRevision = previousLocation.getRevision();
            }
            
            count += maybeCropAndSendSegment(segmentStartRevision, segmentEndRevision, startRevision, endRevision, 
                    segmentPath, handler);
         
            if (FSRepository.isInvalidRevision(currentRevision)) {
                break;
            }
            
            if (segmentStartRevision - currentRevision > 1) {
                count += maybeCropAndSendSegment(currentRevision + 1, segmentStartRevision - 1, startRevision, 
                        endRevision, null, handler);
            }
        }
        return count;
    }

    protected void reset(FSFS fsfs) {
        myFSFS = fsfs;
    }

    private long maybeCropAndSendSegment(long segmentStartRevision, long segmentEndRevision, 
            long startRevision, long endRevision, String segmentPath, ISVNLocationSegmentHandler handler) throws SVNException {
        if (!(segmentStartRevision > startRevision || segmentEndRevision < endRevision)) {
            if (segmentStartRevision < endRevision) {
                segmentStartRevision = endRevision;
            }
            if (segmentEndRevision > startRevision) {
                segmentEndRevision = startRevision;
            }
            if (handler != null) {
                handler.handleLocationSegment(new SVNLocationSegment(segmentStartRevision, segmentEndRevision, 
                        segmentPath));
            }
            return segmentEndRevision - segmentStartRevision + 1;
        }
        return 0;
    }
}
