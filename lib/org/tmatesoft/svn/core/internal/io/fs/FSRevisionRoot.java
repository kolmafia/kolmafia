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

import java.util.Map;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.SVNLocationEntry;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSRevisionRoot extends FSRoot {
    private long myRevision;
    private long myRootOffset;
    private long myChangesOffset;

    public FSRevisionRoot(FSFS owner, long revision) {
        super(owner);
        myRevision = revision;
        myRootOffset = -1;
        myChangesOffset = -1;
    }

    public long getRevision() {
        return myRevision;
    }

    public Map getChangedPaths() throws SVNException {
        FSFile file = getOwner().getPackOrRevisionFSFile(getRevision());
        try {
            loadOffsets(file);
            file.seek(myChangesOffset);
            return fetchAllChanges(file, true);
        } finally {
            file.close();
        }
    }

    public FSCopyInheritance getCopyInheritance(FSParentPath child) throws SVNException{
        return null;
    }
    
    public FSNodeHistory getNodeHistory(String path) throws SVNException {
        SVNNodeKind kind = checkNodeKind(path);
        if (kind == SVNNodeKind.NONE) {
            SVNErrorManager.error(FSErrors.errorNotFound(this, path), SVNLogType.FSFS);
        }
        return new FSNodeHistory(new SVNLocationEntry(getRevision(), SVNPathUtil.canonicalizeAbsolutePath(path)), 
                false, new SVNLocationEntry(SVNRepository.INVALID_REVISION, null), getOwner());
    }

    public FSClosestCopy getClosestCopy(String path) throws SVNException {
        FSParentPath parentPath = openPath(path, true, true);
        SVNLocationEntry copyDstEntry = FSNodeHistory.findYoungestCopyroot(getOwner().getRepositoryRoot(), 
                parentPath);
        if (copyDstEntry == null || copyDstEntry.getRevision() == 0) {
            return null;
        }

        FSRevisionRoot copyDstRoot = getOwner().createRevisionRoot(copyDstEntry.getRevision());
        if (copyDstRoot.checkNodeKind(path) == SVNNodeKind.NONE) {
            return null;
        }
        FSParentPath copyDstParentPath = copyDstRoot.openPath(path, true, true);
        FSRevisionNode copyDstNode = copyDstParentPath.getRevNode();
        if (!copyDstNode.getId().isRelated(parentPath.getRevNode().getId())) {
            return null;
        }

        long createdRev = copyDstNode.getCreatedRevision();
        if (createdRev == copyDstEntry.getRevision()) {
            if (copyDstNode.getPredecessorId() == null) {
                return null;
            }
        }
        return new FSClosestCopy(copyDstRoot, copyDstEntry.getPath());
    }

    public FSRevisionNode getRootRevisionNode() throws SVNException {
        if (myRootRevisionNode == null) {
            FSFile file = getOwner().getPackOrRevisionFSFile(getRevision());
            try {
                loadOffsets(file);
                file.seek(myRootOffset);
                Map headers = file.readHeader();
                myRootRevisionNode = FSRevisionNode.fromMap(headers);
            } finally {
                file.close();
            }
        }
        return myRootRevisionNode;
    }

    public SVNLocationEntry getPreviousLocation(String path, long[] appearedRevision) throws SVNException {
        if (appearedRevision != null && appearedRevision.length > 0) {
            appearedRevision[0] = SVNRepository.INVALID_REVISION;
        }
        FSClosestCopy closestCopy = getClosestCopy(path);
        if (closestCopy == null) {
            return null;
        }
        
        FSRevisionRoot copyTargetRoot = closestCopy.getRevisionRoot();
        String copyTargetPath = closestCopy.getPath();
        FSRevisionNode copyFromNode = copyTargetRoot.getRevisionNode(copyTargetPath);
        String copyFromPath = copyFromNode.getCopyFromPath();
        long copyFromRevision = copyFromNode.getCopyFromRevision();
        String remainder = "";
        if (!path.equals(copyTargetPath)) {
            remainder = path.substring(copyTargetPath.length());
            if (remainder.startsWith("/")) {
                remainder = remainder.substring(1);
            }
        }
        String previousPath = SVNPathUtil.getAbsolutePath(SVNPathUtil.append(copyFromPath, remainder));
        if (appearedRevision != null && appearedRevision.length > 0) {
            appearedRevision[0] = copyTargetRoot.getRevision();
        }
        return new SVNLocationEntry(copyFromRevision, previousPath);
    }

    public long getNodeOriginRevision(String path) throws SVNException {
        path = SVNPathUtil.canonicalizeAbsolutePath(path);
        FSRevisionNode node = getRevisionNode(path);
        String nodeID = node.getId().getNodeID();
        FSFS fsfs = getOwner();
        if (nodeID.startsWith("_")) {
            return SVNRepository.INVALID_REVISION;
        }
        
        int dashIndex = nodeID.indexOf('-');
        if (dashIndex != -1 && dashIndex != nodeID.length() - 1) {
            try {
                return Long.parseLong(nodeID.substring(dashIndex + 1));
            } catch (NumberFormatException nfe) {
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, nfe), SVNLogType.FSFS);
            }
        }
        
        String cachedOriginID = fsfs.getNodeOrigin(nodeID);
        if (cachedOriginID != null) {
            FSID id = FSID.fromString(cachedOriginID);
            return id.getRevision();
        }
        
        long lastRev = SVNRepository.INVALID_REVISION;
        String lastPath = path; 
        FSRevisionRoot curRoot = this;
        while (true) {
            if (FSRepository.isValidRevision(lastRev)) {
                curRoot = fsfs.createRevisionRoot(lastRev);
            }
            
            SVNLocationEntry previousLocation = curRoot.getPreviousLocation(lastPath, null);
            if (previousLocation == null) {
                break;
            }
            lastPath = previousLocation.getPath();
            lastRev = previousLocation.getRevision();
        }
        
        node = curRoot.getRevisionNode(lastPath);
        FSID predID = node.getId();
        while (predID != null) {
            node = fsfs.getRevisionNode(predID);
            predID = node.getPredecessorId();
        }
        
        if (!nodeID.startsWith("_")) {
            fsfs.setNodeOrigin(nodeID, node.getId());
        }
        
        return node.getCreatedRevision();
    }
    
    private void loadOffsets(FSFile file) throws SVNException {
        if (myRootOffset >= 0) {
            return;
        }
        long[] rootOffset = { -1 };
        long[] changesOffset = { -1 };
        FSRepositoryUtil.loadRootChangesOffset(getOwner(), getRevision(), file, rootOffset, changesOffset);
        myRootOffset = rootOffset[0];
        myChangesOffset = changesOffset[0];
    }

}
