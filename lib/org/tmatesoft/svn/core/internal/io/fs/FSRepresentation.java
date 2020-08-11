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

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc2.SvnChecksum;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSRepresentation {

    public static final String REP_DELTA = "DELTA";
    public static final String REP_PLAIN = "PLAIN";
    public static final String REP_TRAILER = "ENDREP";

    public static FSRepresentation parse(String representationString) throws SVNException {
        final String[] fields = representationString.split(" ");
        if (fields.length != 5 && fields.length != 7) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Malformed text representation offset line in node-rev");
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }

        try {
            long revision = Long.parseLong(fields[0]);
            long itemIndex = Long.parseLong(fields[1]);
            long size = Long.parseLong(fields[2]);
            long expandedSize = Long.parseLong(fields[3]);
            final String md5Checksum = fields[4];

            final FSRepresentation representation = new FSRepresentation();
            representation.setRevision(revision);
            representation.setItemIndex(itemIndex);
            representation.setSize(size);
            representation.setExpandedSize(expandedSize);
            representation.setMD5HexDigest(md5Checksum);

            if (fields.length == 7) {
                final String sha1Checksum = fields[5];

                final String[] transactionWithUniquifier = fields[6].split("/_");
                if (transactionWithUniquifier.length != 2) {
                    SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Malformed text representation offset line in node-rev");
                    SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
                }

                final String txnId = transactionWithUniquifier[0];

                representation.setSHA1HexDigest(sha1Checksum);
                representation.setTxnId(txnId);
                representation.setUniquifier(fields[6]);
            }
            return representation;

        } catch (NumberFormatException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Malformed text representation offset line in node-rev");
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }
        return null;
    }

    private long myRevision;
    private long myItemIndex;
    private long mySize;
    private long myExpandedSize;
    private String myMD5HexDigest;
    private String mySHA1HexDigest;
    private String myTxnId;
    private String myUniquifier;
    
    public FSRepresentation(FSRepresentation representation) {
        myRevision = representation.myRevision;
        myItemIndex = representation.myItemIndex;
        mySize = representation.mySize;
        myExpandedSize = representation.myExpandedSize;
        myMD5HexDigest = representation.myMD5HexDigest;
        mySHA1HexDigest = representation.mySHA1HexDigest;
        myUniquifier = representation.myUniquifier;
        myTxnId = representation.myTxnId;
    }

    public FSRepresentation() {
        myRevision = SVNRepository.INVALID_REVISION;
        myItemIndex = -1;
        mySize = -1;
        myExpandedSize = -1;
    }

    public void setRevision(long rev) {
        myRevision = rev;
    }

    public void setItemIndex(long itemIndex) {
        myItemIndex = itemIndex;
    }

    public void setSize(long size) {
        mySize = size;
    }

    public void setExpandedSize(long expandedSize) {
        myExpandedSize = expandedSize;
    }

    public void setMD5HexDigest(String hexDigest) {
        myMD5HexDigest = hexDigest;
    }

    public String getSHA1HexDigest() {
        return mySHA1HexDigest;
    }
    
    public void setSHA1HexDigest(String hexDigest) {
        mySHA1HexDigest = hexDigest;
    }

    public String getUniquifier() {
        return myUniquifier;
    }
    
    public void setUniquifier(String uniquifier) {
        myUniquifier = uniquifier;
    }

    public long getRevision() {
        return myRevision;
    }

    public long getItemIndex() {
        return myItemIndex;
    }

    public long getSize() {
        return mySize;
    }

    public long getExpandedSize() {
        return myExpandedSize;
    }

    public String getMD5HexDigest() {
        return myMD5HexDigest;
    }

    public static boolean compareRepresentations(FSRepresentation r1, FSRepresentation r2) {
        if (r1 == r2) {
            return true;
        } else if (r1 == null) {
            return false;
        }
        return r1.equals(r2);
    }

    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != FSRepresentation.class) {
            return false;
        }
        FSRepresentation rep = (FSRepresentation) obj;
        if (myRevision != rep.myRevision) {
            return false;
        }
        if (myItemIndex != rep.myItemIndex) {
            return false;
        }
        if (myUniquifier == rep.myUniquifier) {
            return true;
        } else if (myUniquifier == null || rep.myUniquifier == null) {
            return false;
        } else {
            return myUniquifier.equals(rep.myUniquifier);
        }
    }

    public String getStringRepresentation(int dbFormat) {
        if (dbFormat < FSFS.MIN_REP_SHARING_FORMAT || mySHA1HexDigest == null || myUniquifier == null) {
            return myRevision + " " + myItemIndex + " " + mySize + " " + myExpandedSize + " " + myMD5HexDigest;
        }
        return myRevision + " " + myItemIndex + " " + mySize + " " + myExpandedSize + " " + myMD5HexDigest + " " +
               mySHA1HexDigest + " " + myUniquifier;
    }
    
    public String getTxnId() {
        return myTxnId;
    }

    public void setTxnId(String txnId) {
        myTxnId = txnId;
    }

    public boolean isTxn() {
        return myTxnId != null;
    }
}
