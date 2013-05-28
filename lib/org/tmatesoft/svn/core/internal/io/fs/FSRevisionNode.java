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

import java.util.Collections;
import java.util.Map;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSRevisionNode {

    // rev-node files keywords
    public static final String HEADER_ID = "id";
    public static final String HEADER_TYPE = "type";
    public static final String HEADER_COUNT = "count";
    public static final String HEADER_PROPS = "props";
    public static final String HEADER_TEXT = "text";
    public static final String HEADER_CPATH = "cpath";
    public static final String HEADER_PRED = "pred";
    public static final String HEADER_COPYFROM = "copyfrom";
    public static final String HEADER_COPYROOT = "copyroot";
    public static final String HEADER_IS_FRESH_TXN_ROOT = "is-fresh-txn-root";
    public static final String HEADER_MERGE_INFO_COUNT = "minfo-cnt";
    public static final String HEADER_MERGE_INFO_HERE = "minfo-here";

    // id: a.b.r<revID>/offset
    private FSID myId;

    // type: 'dir' or 'file'
    private SVNNodeKind myType;

    // count: count of revs since base
    private long myCount;

    // (_)a.(_)b.tx-y

    // pred: a.b.r<revID>/offset
    private FSID myPredecessorId;

    // text: <rev> <offset> <length> <size> <digest>
    private FSRepresentation myTextRepresentation;

    // props: <rev> <offset> <length> <size> <digest>
    private FSRepresentation myPropsRepresentation;

    // cpath: <path>
    private String myCreatedPath;

    // copyfrom: <revID> <path>
    private long myCopyFromRevision;
    private String myCopyFromPath;

    // copyroot: <revID> <created-path>
    private long myCopyRootRevision;
    private String myCopyRootPath;

    // for only node-revs representing dirs
    private Map myDirContents;

    //in case of txn root: whether it wasn't 
    //changed yet (fresh) or was
    private boolean myIsFreshTxnRoot;
    private FSID myFreshRootPredecessorId;
    
    private long myMergeInfoCount;
    private boolean myHasMergeInfo;
    
    public void setId(FSID revNodeID) {
        myId = revNodeID;
    }

    public void setType(SVNNodeKind nodeKind) {
        myType = nodeKind;
    }

    public void setCount(long count) {
        myCount = count;
    }

    public void setPredecessorId(FSID predRevNodeId) {
        myPredecessorId = predRevNodeId;
    }

    public void setTextRepresentation(FSRepresentation textRepr) {
        myTextRepresentation = textRepr;
    }

    public void setPropsRepresentation(FSRepresentation propsRepr) {
        myPropsRepresentation = propsRepr;
    }

    public void setCreatedPath(String cpath) {
        myCreatedPath = cpath;
    }

    public void setCopyFromRevision(long copyFromRev) {
        myCopyFromRevision = copyFromRev;
    }

    public void setCopyFromPath(String copyFromPath) {
        myCopyFromPath = copyFromPath;
    }

    public void setCopyRootRevision(long copyRootRev) {
        myCopyRootRevision = copyRootRev;
    }

    public void setCopyRootPath(String copyRootPath) {
        myCopyRootPath = copyRootPath;
    }

    public void setMergeInfoCount(long mergeInfoCount) {
        myMergeInfoCount = mergeInfoCount;
    }
    
    public void setHasMergeInfo(boolean hasMergeInfo) {
        myHasMergeInfo = hasMergeInfo;
    }
    
    public FSID getId() {
        return myId;
    }

    public SVNNodeKind getType() {
        return myType;
    }

    public long getCount() {
        return myCount;
    }

    public FSID getPredecessorId() {
        return myPredecessorId;
    }

    // text
    public FSRepresentation getTextRepresentation() {
        return myTextRepresentation;
    }

    // props
    public FSRepresentation getPropsRepresentation() {
        return myPropsRepresentation;
    }

    public String getCreatedPath() {
        return myCreatedPath;
    }

    public long getCreatedRevision() {
        if (myFreshRootPredecessorId != null) {
            return myFreshRootPredecessorId.getRevision();
        }
        return myId.getRevision();
    }
    
    public long getCopyFromRevision() {
        return myCopyFromRevision;
    }

    public String getCopyFromPath() {
        return myCopyFromPath;
    }

    public long getCopyRootRevision() {
        return myCopyRootRevision;
    }

    public String getCopyRootPath() {
        return myCopyRootPath;
    }

    public static FSRevisionNode dumpRevisionNode(FSRevisionNode revNode) {
        FSRevisionNode clone = new FSRevisionNode();
        clone.setId(revNode.getId());
        if (revNode.getPredecessorId() != null) {
            clone.setPredecessorId(revNode.getPredecessorId());
        }
        clone.setType(revNode.getType());
        clone.setCopyFromPath(revNode.getCopyFromPath());
        clone.setCopyFromRevision(revNode.getCopyFromRevision());
        clone.setCopyRootPath(revNode.getCopyRootPath());
        clone.setCopyRootRevision(revNode.getCopyRootRevision());
        clone.setCount(revNode.getCount());
        clone.setCreatedPath(revNode.getCreatedPath());
        if (revNode.getPropsRepresentation() != null) {
            clone.setPropsRepresentation(new FSRepresentation(revNode.getPropsRepresentation()));
        }
        if (revNode.getTextRepresentation() != null) {
            clone.setTextRepresentation(new FSRepresentation(revNode.getTextRepresentation()));
        }
        clone.setMergeInfoCount(revNode.getMergeInfoCount());
        clone.setHasMergeInfo(revNode.hasMergeInfo());
        return clone;
    }

    protected Map getDirContents() {
        return myDirContents;
    }

    public void setDirContents(Map dirContents) {
        myDirContents = dirContents;
    }

    public boolean hasMergeInfo() {
        return myHasMergeInfo;
    }
    
    public long getMergeInfoCount() {
        return myMergeInfoCount;
    }
    
    public boolean hasDescendantsWithMergeInfo() {
        if (myType != SVNNodeKind.DIR) {
            return false;
        }
        
        if (myMergeInfoCount > 1) {
            return true;
        } else if (myMergeInfoCount == 1 && !myHasMergeInfo) {
            return true;
        }
        return false;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("{[");
        buffer.append("ID:");
        buffer.append(myId);
        buffer.append("][");
        buffer.append("text representation:");
        buffer.append(myTextRepresentation);
        buffer.append("][");
        buffer.append("Node kind:");
        buffer.append(myType);
        buffer.append("]}");
        return buffer.toString();
    }

    public static FSRevisionNode fromMap(Map headers) throws SVNException {
        FSRevisionNode revNode = new FSRevisionNode();

        // Read the rev-node id.
        String revNodeId = (String) headers.get(FSRevisionNode.HEADER_ID);
        if (revNodeId == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Missing id field in node-rev");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        FSID revnodeID = FSID.fromString(revNodeId);
        if (revnodeID == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Corrupted node-id in node-rev");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        revNode.setId(revnodeID);

        // Read the type.
        SVNNodeKind nodeKind = SVNNodeKind.parseKind((String) headers.get(FSRevisionNode.HEADER_TYPE));
        if (nodeKind == SVNNodeKind.NONE || nodeKind == SVNNodeKind.UNKNOWN) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Missing kind field in node-rev");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        revNode.setType(nodeKind);

        // Read the 'count' field.
        String countString = (String) headers.get(FSRevisionNode.HEADER_COUNT);
        if (countString == null) {
            revNode.setCount(0);
        } else {
            long cnt = -1;
            try {
                cnt = Long.parseLong(countString);
            } catch (NumberFormatException nfe) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, 
                        "Corrupted count field in node-rev");
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
            revNode.setCount(cnt);
        }

        // Get the properties location (if any).
        String propsRepr = (String) headers.get(FSRevisionNode.HEADER_PROPS);
        if (propsRepr != null) {
            parseRepresentationHeader(propsRepr, revNode, revnodeID.getTxnID(), false, true);
        }

        // Get the data location (if any).
        String textRepr = (String) headers.get(FSRevisionNode.HEADER_TEXT);
        if (textRepr != null) {
            parseRepresentationHeader(textRepr, revNode, revnodeID.getTxnID(), true, nodeKind == SVNNodeKind.DIR);
        }

        // Get the created path.
        String cpath = (String) headers.get(FSRevisionNode.HEADER_CPATH);
        if (cpath == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Missing cpath in node-rev");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        revNode.setCreatedPath(cpath);

        // Get the predecessor rev-node id (if any).
        String predId = (String) headers.get(FSRevisionNode.HEADER_PRED);
        if (predId != null) {
            FSID predRevNodeId = FSID.fromString(predId);
            if (predRevNodeId == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, 
                        "Corrupted predecessor node-id in node-rev");
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
            revNode.setPredecessorId(predRevNodeId);
        }

        // Get the copyroot.
        String copyroot = (String) headers.get(FSRevisionNode.HEADER_COPYROOT);
        if (copyroot == null) {
            revNode.setCopyRootPath(revNode.getCreatedPath());
            revNode.setCopyRootRevision(revNode.getCreatedRevision());
        } else {
            parseCopyRoot(copyroot, revNode);
        }

        // Get the copyfrom.
        String copyfrom = (String) headers.get(FSRevisionNode.HEADER_COPYFROM);
        if (copyfrom == null) {
            revNode.setCopyFromPath(null);
            revNode.setCopyFromRevision(SVNRepository.INVALID_REVISION);
        } else {
            parseCopyFrom(copyfrom, revNode);
        }
        
        revNode.myIsFreshTxnRoot = headers.containsKey(HEADER_IS_FRESH_TXN_ROOT);
        
        String mergeInfoCountStr = (String) headers.get(HEADER_MERGE_INFO_COUNT);
        if (mergeInfoCountStr == null) {
            revNode.myMergeInfoCount = 0;
        } else {
            try {
                revNode.myMergeInfoCount = Long.parseLong(mergeInfoCountStr);
            } catch (NumberFormatException e) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, 
                        "Corrupted mergeinfo count in node-rev");
                SVNErrorManager.error(err, e, SVNLogType.FSFS);
            }
        }

        revNode.myHasMergeInfo = headers.containsKey(HEADER_MERGE_INFO_HERE);
        return revNode;
    }

    public static void parseRepresentationHeader(String representation, FSRevisionNode revNode, String txnId, 
            boolean isData, boolean mutableRepTuncated) throws SVNException {
        if (revNode == null) {
            return;
        }

        FSRepresentation rep = new FSRepresentation();

        int delimiterInd = representation.indexOf(' ');
        String revision = null;
        if (delimiterInd == -1) {
            revision = representation;
        } else {
            revision = representation.substring(0, delimiterInd);
        }

        long rev = -1;
        try {
            rev = Long.parseLong(revision);
        } catch (NumberFormatException nfe) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, 
                    "Malformed text rep offset line in node-rev");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        
        rep.setRevision(rev);

        if (FSRepository.isInvalidRevision(rep.getRevision())) {
            rep.setTxnId(txnId);
            if (isData) {
                revNode.setTextRepresentation(rep);
            } else {
                revNode.setPropsRepresentation(rep);
            }
            if (mutableRepTuncated) {
                return;
            }
        }

        representation = representation.substring(delimiterInd + 1);

        delimiterInd = representation.indexOf(' ');
        if (delimiterInd == -1) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Malformed text rep offset line in node-rev");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        String repOffset = representation.substring(0, delimiterInd);

        long offset = -1;
        try {
            offset = Long.parseLong(repOffset);
            if (offset < 0) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Malformed text rep offset line in node-rev");
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
        } catch (NumberFormatException nfe) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Malformed text rep offset line in node-rev");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        rep.setOffset(offset);

        representation = representation.substring(delimiterInd + 1);
        delimiterInd = representation.indexOf(' ');
        if (delimiterInd == -1) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Malformed text rep offset line in node-rev");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        String repSize = representation.substring(0, delimiterInd);

        long size = -1;
        try {
            size = Long.parseLong(repSize);
            if (size < 0) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Malformed text rep offset line in node-rev");
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
        } catch (NumberFormatException nfe) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Malformed text rep offset line in node-rev");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        rep.setSize(size);

        representation = representation.substring(delimiterInd + 1);
        delimiterInd = representation.indexOf(' ');
        if (delimiterInd == -1) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Malformed text rep offset line in node-rev");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        String repExpandedSize = representation.substring(0, delimiterInd);

        long expandedSize = -1;
        try {
            expandedSize = Long.parseLong(repExpandedSize);
            if (expandedSize < 0) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Malformed text rep offset line in node-rev");
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
        } catch (NumberFormatException nfe) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Malformed text rep offset line in node-rev");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        rep.setExpandedSize(expandedSize);

        representation = representation.substring(delimiterInd + 1);
        delimiterInd = representation.indexOf(' ');
        String hexMD5Digest = null;
        if (delimiterInd == -1) {
            hexMD5Digest = representation;
        } else {
            hexMD5Digest = representation.substring(0, delimiterInd);
        }
        
        if (hexMD5Digest.length() != 32 || SVNFileUtil.fromHexDigest(hexMD5Digest) == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Malformed text rep offset line in node-rev");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        rep.setMD5HexDigest(hexMD5Digest);
        
        if (isData) {
            revNode.setTextRepresentation(rep);
        } else {
            revNode.setPropsRepresentation(rep);
        }
        
        if (delimiterInd == -1) {
            return;
        }
        
        representation = representation.substring(delimiterInd + 1);
        delimiterInd = representation.indexOf(' ');
        String hexSHA1Digest = null;
        if (delimiterInd == -1) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Malformed text rep offset line in node-rev");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        hexSHA1Digest = representation.substring(0, delimiterInd);
        if (hexSHA1Digest.length() != 40 || SVNFileUtil.fromHexDigest(hexSHA1Digest) == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Malformed text rep offset line in node-rev");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        rep.setSHA1HexDigest(hexSHA1Digest);
        
        representation = representation.substring(delimiterInd + 1);
        delimiterInd = representation.indexOf(' ');
        String uniquifier = null;
        if (delimiterInd != -1) {
            uniquifier = representation.substring(0, delimiterInd);
        } else {
            uniquifier = representation;
        }
        
        rep.setUniquifier(uniquifier);
    }

    private static void parseCopyFrom(String copyfrom, FSRevisionNode revNode) throws SVNException {
        if (copyfrom == null || copyfrom.length() == 0) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, 
                    "Malformed copyfrom line in node-rev");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        int delimiterInd = copyfrom.indexOf(' ');
        if (delimiterInd == -1) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, 
                    "Malformed copyfrom line in node-rev");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        String copyfromRev = copyfrom.substring(0, delimiterInd);
        String copyfromPath = copyfrom.substring(delimiterInd + 1);

        long rev = -1;
        try {
            rev = Long.parseLong(copyfromRev);
        } catch (NumberFormatException nfe) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, 
                    "Malformed copyfrom line in node-rev");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        revNode.setCopyFromRevision(rev);
        revNode.setCopyFromPath(copyfromPath);
    }

    private static void parseCopyRoot(String copyroot, FSRevisionNode revNode) throws SVNException {
        if (copyroot == null || copyroot.length() == 0) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Malformed copyroot line in node-rev");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        int delimiterInd = copyroot.indexOf(' ');
        if (delimiterInd == -1) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Malformed copyroot line in node-rev");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        String copyrootRev = copyroot.substring(0, delimiterInd);
        String copyrootPath = copyroot.substring(delimiterInd + 1);

        long rev = -1;
        try {
            rev = Long.parseLong(copyrootRev);
        } catch (NumberFormatException nfe) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Malformed copyroot line in node-rev");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        revNode.setCopyRootRevision(rev);
        revNode.setCopyRootPath(copyrootPath);
    }

    public FSRevisionNode getChildDirNode(String childName, FSFS fsfsOwner) throws SVNException {
        if (!SVNPathUtil.isSinglePathComponent(childName)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_SINGLE_PATH_COMPONENT, "Attempted to open node with an illegal name ''{0}''", childName);
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        Map entries = getDirEntries(fsfsOwner);
        FSEntry entry = entries != null ? (FSEntry) entries.get(childName) : null;

        if (entry == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_FOUND, "Attempted to open non-existent child node ''{0}''", childName);
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        return fsfsOwner.getRevisionNode(entry.getId());
    }

    public Map getDirEntries(FSFS fsfsOwner) throws SVNException {
        if (getType() != SVNNodeKind.DIR) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_DIRECTORY, "Can't get entries of non-directory");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        Map dirContents = getDirContents();

        if (dirContents == null) {
            dirContents = fsfsOwner.getDirContents(this);
            setDirContents(dirContents);
        }
        return Collections.unmodifiableMap(dirContents);
    }

    public SVNProperties getProperties(FSFS fsfsOwner) throws SVNException {
        return fsfsOwner.getProperties(this);
    }

    public FSRepresentation chooseDeltaBase(FSFS fsfsOwner) throws SVNException {
        if (getCount() == 0) {
            return null;
        }

        long count = getCount();
        count = count & (count - 1);
        FSRevisionNode baseNode = this;
        while ((count++) < getCount()) {
            baseNode = fsfsOwner.getRevisionNode(baseNode.getPredecessorId());
        }
        return baseNode.getTextRepresentation();
    }

    public String getFileMD5Checksum() throws SVNException {
        if (getType() != SVNNodeKind.FILE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_FILE, "Attempted to get checksum of a *non*-file node");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        return getTextRepresentation() != null ? getTextRepresentation().getMD5HexDigest() : "";
    }

    public String getFileSHA1Checksum() throws SVNException {
        if (getType() != SVNNodeKind.FILE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_FILE, "Attempted to get checksum of a *non*-file node");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        return getTextRepresentation() != null ? getTextRepresentation().getSHA1HexDigest() : "";
    }

    public long getFileLength() throws SVNException {
        if (getType() != SVNNodeKind.FILE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_FILE, "Attempted to get length of a *non*-file node");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        return getTextRepresentation() != null ? getTextRepresentation().getExpandedSize() : 0;
    }

    public void setIsFreshTxnRoot(boolean isFreshTxnRoot) {
        myIsFreshTxnRoot = isFreshTxnRoot;
    }

    public boolean isFreshTxnRoot() {
        return myIsFreshTxnRoot;
    }

    public void setFreshRootPredecessorId(FSID freshRootPredecessorId) {
        myFreshRootPredecessorId = freshRootPredecessorId;
    }
}
