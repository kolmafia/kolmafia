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

import java.io.Serializable;

import org.tmatesoft.svn.core.io.SVNRepository;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSID implements Serializable {

    private static final long serialVersionUID = 4845L;
    private String myNodeID;
    private String myCopyID;
    private String myTxnID;
    private long myRevision;
    private long myOffset;

    public boolean isTxn() {
        return myTxnID != null;
    }

    public static FSID createTxnId(String nodeId, String copyId, String txnId) {
        return new FSID(nodeId, txnId, copyId, SVNRepository.INVALID_REVISION, -1);
    }

    public static FSID createRevId(String nodeId, String copyId, long revision, long offset) {
        return new FSID(nodeId, null, copyId, revision, offset);
    }

    private FSID(String nodeId, String txnId, String copyId, long revision, long offset) {
        myNodeID = nodeId;
        myCopyID = copyId;
        myTxnID = txnId;
        myRevision = revision;
        myOffset = offset;
    }

    public FSID copy() {
        return new FSID(getNodeID(), getTxnID(), getCopyID(), getRevision(), getOffset());
    }

    public String getNodeID() {
        return myNodeID;
    }

    public String getTxnID() {
        return myTxnID;
    }

    public String getCopyID() {
        return myCopyID;
    }

    public long getRevision() {
        return myRevision;
    }

    public long getOffset() {
        return myOffset;
    }

    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != FSID.class) {
            return false;
        }
        FSID id = (FSID) obj;
        if (this == id) {
            return true;
        }

        if (myNodeID != null && !myNodeID.equals(id.getNodeID())) {
            return false;
        } else if (myNodeID == null && id.getNodeID() != null) {
            return false;
        }

        if (myCopyID != null && !myCopyID.equals(id.getCopyID())) {
            return false;
        } else if (myCopyID == null && id.getCopyID() != null) {
            return false;
        }

        if (myTxnID != null && !myTxnID.equals(id.getTxnID())) {
            return false;
        } else if (myTxnID == null && id.getTxnID() != null) {
            return false;
        }

        if (myRevision != id.getRevision() || myOffset != id.getOffset()) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((myNodeID == null) ? 0 : myNodeID.hashCode());
        result = PRIME * result + ((myCopyID == null) ? 0 : myCopyID.hashCode());
        result = PRIME * result + ((myTxnID == null) ? 0 : myTxnID.hashCode());
        result = PRIME * result + (int) (myRevision ^ (myRevision >>> 32));
        result = PRIME * result + (int) (myOffset ^ (myOffset >>> 32));
        return result;
    }

    /*
     * Return values: 0 - id1 equals to id2 1 - id1 is related to id2 (id2 is a
     * result of user's modifications) -1 - id1 is not related to id2
     * (absolutely different items)
     */
    public int compareTo(FSID otherID) {
        if (otherID == null) {
            return -1;
        } else if (otherID.equals(this)) {
            return 0;
        }
        return isRelated(otherID) ? 1 : -1;
    }

    public boolean isRelated(FSID otherID) {
        if (otherID == null) {
            return false;
        }

        if (this == otherID) {
            return true;
        }

        if (myNodeID != null && myNodeID.startsWith("_")) {
            if (myTxnID != null && !myTxnID.equals(otherID.getTxnID())) {
                return false;
            } else if (myTxnID == null && otherID.getTxnID() != null) {
                return false;
            }
        }
        return myNodeID.equals(otherID.getNodeID());
    }

    public String toString() {
        return myNodeID + "." + myCopyID + "." + (isTxn() ? "t" + myTxnID : "r" + myRevision + "/" + myOffset);
    }

    public static FSID fromString(String revNodeId) {
        int dotInd = revNodeId.indexOf('.');
        if (dotInd == -1) {
            return null;
        }

        String nodeId = revNodeId.substring(0, dotInd);
        revNodeId = revNodeId.substring(dotInd + 1);

        dotInd = revNodeId.indexOf('.');
        if (dotInd == -1) {
            return null;
        }

        String copyId = revNodeId.substring(0, dotInd);
        revNodeId = revNodeId.substring(dotInd + 1);

        if (revNodeId.charAt(0) == 'r') {
            int slashInd = revNodeId.indexOf('/');
            long rev = -1;
            long offset = -1;
            try {
                rev = Long.parseLong(revNodeId.substring(1, slashInd));
                offset = Long.parseLong(revNodeId.substring(slashInd + 1));
            } catch (NumberFormatException nfe) {
                return null;
            }
            return createRevId(nodeId, copyId, rev, offset);
        } else if (revNodeId.charAt(0) == 't') {
            String txnId = revNodeId.substring(1);
            return createTxnId(nodeId, copyId, txnId);
        }
        return null;
    }
}
