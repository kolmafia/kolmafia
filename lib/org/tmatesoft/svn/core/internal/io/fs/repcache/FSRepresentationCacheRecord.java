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
package org.tmatesoft.svn.core.internal.io.fs.repcache;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSRepresentationCacheRecord {
    public static int HASH_FIELD = 0;
    public static int REVISION_FIELD = 1;
    public static int OFFSET_FIELD = 2;
    public static int SIZE_FIELD = 3;
    public static int EXPANDED_SIZE_FIELD = 4;

    private String myHash = "";
    private long myRevision = 0L;
    private long myOffset = 0L;
    private long mySize = 0L;
    private long myExpandedSize = 0L;

    public FSRepresentationCacheRecord(String hash, long revision, long offset, long size, long expandedSize) {
        myHash = hash;
        myRevision = revision;
        myOffset = offset;
        mySize = size;
        myExpandedSize = expandedSize;
    }

    FSRepresentationCacheRecord(ISqlJetCursor cursor) throws SqlJetException {
        final int fieldsCount = cursor.getFieldsCount();
        if (fieldsCount == 0) {
            return;
        }
        
        if (!cursor.isNull(HASH_FIELD)) {
            myHash = cursor.getString(HASH_FIELD);
        }
        
        if (fieldsCount == 1) {
            return;
        }
        
        if (!cursor.isNull(REVISION_FIELD)) {
            myRevision = cursor.getInteger(REVISION_FIELD);
        }
        
        if (fieldsCount == 2) {
            return;
        }
        
        if (!cursor.isNull(OFFSET_FIELD)) {
            myOffset = cursor.getInteger(OFFSET_FIELD);
        }
        
        if (fieldsCount == 3) {
            return;
        }
        
        if (!cursor.isNull(SIZE_FIELD)) {
            mySize = cursor.getInteger(SIZE_FIELD);
        }
        
        if (fieldsCount == 4) {
            return;
        }
        
        if (!cursor.isNull(EXPANDED_SIZE_FIELD)) {
            myExpandedSize = cursor.getInteger(EXPANDED_SIZE_FIELD);
        }
    }

    public String toString() {
        final StringBuffer b = new StringBuffer();
        b.append("rep_cache( ");
        b.append("hash: ").append(myHash);
        b.append(", revision: ").append(myRevision);
        b.append(", offset: ").append(myOffset);
        b.append(", size: ").append(mySize);
        b.append(", expanded_size: ").append(myExpandedSize);
        b.append(" )");
        return b.toString();
    }

    /**
     * @return the hash
     */
    public String getHash() {
        return myHash;
    }

    /**
     * @param hash
     *            the hash to set
     */
    public void setHash(String hash) {
        myHash = hash;
    }

    /**
     * @return the revision
     */
    public long getRevision() {
        return myRevision;
    }

    /**
     * @param revision
     *            the revision to set
     */
    public void setRevision(long revision) {
        myRevision = revision;
    }

    /**
     * @return the offset
     */
    public long getOffset() {
        return myOffset;
    }

    /**
     * @param offset
     *            the offset to set
     */
    public void setOffset(long offset) {
        myOffset = offset;
    }

    /**
     * @return the size
     */
    public long getSize() {
        return mySize;
    }

    /**
     * @param size
     *            the size to set
     */
    public void setSize(long size) {
        mySize = size;
    }

    /**
     * @return the expanded_size
     */
    public long getExpandedSize() {
        return myExpandedSize;
    }

    /**
     * @param expanded_size
     *            the expanded_size to set
     */
    public void setExpandedSize(long expandedSize) {
        myExpandedSize = expandedSize;
    }

}
