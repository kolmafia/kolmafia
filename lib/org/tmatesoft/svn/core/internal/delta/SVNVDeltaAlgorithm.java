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
package org.tmatesoft.svn.core.internal.delta;

import java.util.Arrays;

/**
 * @version 1.3
 * @deprecated {@link SVNXDeltaAlgorithm} is used instead in all cases.
 * @author  TMate Software Ltd.
 */
public class SVNVDeltaAlgorithm extends SVNDeltaAlgorithm {
    
    private static final int VD_KEY_SIZE = 4;
    
    private SlotsTable mySlotsTable; 
    
    public void computeDelta(byte[] a, int aLength, byte[] b, int bLength) {
        int dataLength;
        byte[] data;
        if (aLength > 0 && bLength > 0) {
            // both are non-empty (reuse some local array).
            data = new byte[(aLength + bLength)];
            System.arraycopy(a, 0, data, 0, aLength);
            System.arraycopy(b, 0, data, aLength, bLength);
            dataLength = aLength + bLength;
        } else if (aLength == 0) {
            // a is empty
            data = b;
            dataLength = bLength;
        } else {
            // b is empty
            data = a;
            dataLength = aLength;
        }
        SlotsTable slotsTable = getSlotsTable(dataLength);
        vdelta(slotsTable, data, 0, aLength, false);
        vdelta(slotsTable, data, aLength, dataLength, true);
    }
    
    private SlotsTable getSlotsTable(int dataLength) {
        if (mySlotsTable == null) {
            mySlotsTable = new SlotsTable();
        } 
        mySlotsTable.reset(dataLength);
        return mySlotsTable;
    }
    
    private void vdelta(SlotsTable table, byte[] data, int start, int end, boolean doOutput) {
        int here = start; 
        int insertFrom = -1; 
        
        while(true) {
            
            if (end - here < VD_KEY_SIZE) {
                int from = insertFrom >= 0 ? insertFrom : here;
                if (doOutput && from < end) {
                    copyFromNewData(data, from, end - from);
                }
                return;
            }

            int currentMatch = -1;
            int currentMatchLength = 0;
            int key;
            int slot;
            boolean progress = false;
            
            key = here;
            
            do {
                progress = false;
                for(slot = table.getBucket(table.getBucketIndex(data, key)); slot >= 0; slot = table.mySlots[slot]) {
                    
                    if (slot < key - here) {
                        continue;
                    }
                    int match = slot - (key - here);
                    int matchLength = findMatchLength(data, match, here, end);
                    if (match < start && match + matchLength > start) {
                        matchLength = start - match;
                    }
                    if (matchLength >= VD_KEY_SIZE && matchLength > currentMatchLength) {
                        currentMatch = match;
                        currentMatchLength = matchLength;
                        progress = true;
                    }
                }
                if (progress) {
                    key = here + currentMatchLength - (VD_KEY_SIZE - 1);
                }                
            } while (progress && end - key >= VD_KEY_SIZE);
            
            if (currentMatchLength < VD_KEY_SIZE) {
                table.storeSlot(data, here);
                if (insertFrom < 0) {
                    insertFrom = here;
                }
                here++;
                continue;
            } else if (doOutput) {
                if (insertFrom >= 0) {
                    copyFromNewData(data, insertFrom, here - insertFrom);
                    insertFrom = -1;
                } 
                if (currentMatch < start) {
                    copyFromSource(currentMatch, currentMatchLength);
                } else {
                    copyFromTarget(currentMatch - start, currentMatchLength);
                }
            }
            here += currentMatchLength;
            if (end - here >= VD_KEY_SIZE) {
                int last = here - (VD_KEY_SIZE - 1);
                for(; last < here; ++last) {
                    table.storeSlot(data, last);
                }
            }            
        }
            
    }
    

    private int findMatchLength(byte[] data, int match, int from, int end) {
        int here = from;
        while(here < end && data[match] == data[here]) {
            match++;
            here++;
        }
        return here - from;
    }
    
    private static class SlotsTable {
        
        private int[] mySlots;
        private int[] myBuckets;
        private int myBucketsCount;

        public SlotsTable() {
        }

        public void reset(int dataLength) {
            mySlots = allocate(mySlots, dataLength);
            myBucketsCount = (dataLength / 3) | 1;
            myBuckets = allocate(myBuckets, myBucketsCount);
            
            Arrays.fill(mySlots, 0, dataLength, -1);
            Arrays.fill(myBuckets, 0, myBucketsCount, -1);
        }

        public int getBucketIndex(byte[] data, int index) {
            int hash = 0;
            hash += (data[index] & 0xFF);
            hash += hash*127 + (data[index + 1] & 0xFF);
            hash += hash*127 + (data[index + 2] & 0xFF);
            hash += hash*127 + (data[index + 3] & 0xFF);
            hash = hash % myBucketsCount;
            return hash < 0 ? -hash : hash;
        }
        
        public int getBucket(int bucketIndex) {
            return myBuckets[bucketIndex];
        }
        
        public void storeSlot(byte[] data, int slotIndex) {
            int bucketIndex = getBucketIndex(data, slotIndex);
            mySlots[slotIndex] = myBuckets[bucketIndex];
            myBuckets[bucketIndex] = slotIndex; 
        }
        
        private static int[] allocate(int[] array, int length) {
            if (array == null || array.length < length) {
                return new int[length*3/2];
            }
            return array;
        }
    }
}