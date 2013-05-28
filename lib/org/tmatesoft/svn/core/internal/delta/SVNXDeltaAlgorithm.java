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

import java.util.Map;

import org.tmatesoft.svn.core.internal.util.SVNHashMap;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNXDeltaAlgorithm extends SVNDeltaAlgorithm {
    
    private static final int MATCH_BLOCK_SIZE = 64;
    
    public void computeDelta(byte[] a, int aLength, byte[] b, int bLength) {
        if (bLength < MATCH_BLOCK_SIZE) {
            copyFromNewData(b, 0, bLength);
            return;
        }
        PseudoAdler32 bAdler = new PseudoAdler32();
        Map aMatchesTable = createMatchesTable(a, aLength, MATCH_BLOCK_SIZE, bAdler);
        bAdler.reset();
        bAdler.add(b, 0, MATCH_BLOCK_SIZE);

        int lo = 0;
        int size = bLength;
        Match previousInsertion = null;
        
        while(lo < size) {
            Match match = findMatch(aMatchesTable, bAdler, a, aLength, b, bLength, lo, previousInsertion);
            if (match == null) {
                if (previousInsertion != null && previousInsertion.length > 0) {
                    previousInsertion.length++;
                } else {
                    previousInsertion = new Match(lo, 1);
                }
            } else {
                if (previousInsertion != null && previousInsertion.length > 0) {
                    copyFromNewData(b, previousInsertion.position, previousInsertion.length);
                    previousInsertion = null;
                }
                copyFromSource(match.position, match.length);                
            }
            int advance = match != null ? match.advance : 1;
            for (int next = lo; next < lo + advance; next++) {
                bAdler.remove(b[next]);
                if (next + MATCH_BLOCK_SIZE < bLength) {
                    bAdler.add(b[next + MATCH_BLOCK_SIZE]);
                }
            }
            lo += advance;
        }
        if (previousInsertion != null && previousInsertion.length > 0) {
            copyFromNewData(b, previousInsertion.position, previousInsertion.length);
            previousInsertion = null;
        }
    }
    
    private static Match findMatch(Map matchesTable, PseudoAdler32 checksum, byte[] a, int aLength, byte[] b, int bLength, int bPos, Match previousInsertion) {
        Match existingMatch = (Match) matchesTable.get(new Integer(checksum.getValue()));
        if (existingMatch == null) {
            return null;
        }
        if (!equals(a, aLength, existingMatch.position, existingMatch.length, b, bLength, bPos)) {
            return null;
        }
        existingMatch = new Match(existingMatch.position, existingMatch.length);
        existingMatch.advance = existingMatch.length;

        // extend forward 
        while(existingMatch.position + existingMatch.length < aLength &&
                bPos + existingMatch.advance < bLength &&
                a[existingMatch.position + existingMatch.length] == b[bPos + existingMatch.advance]) {
            existingMatch.length++;
            existingMatch.advance++;
        }
        // extend backward
        if (previousInsertion != null) {
            while(existingMatch.position > 0 && bPos > 0 &&
                    a[existingMatch.position - 1] == b[bPos -1] &&
                    previousInsertion.length != 0) {
                previousInsertion.length--;
                bPos--;
                existingMatch.position--;
                existingMatch.length++;
            }
        }
        return existingMatch;
    }
    
    private static Map createMatchesTable(byte[] data, int dataLength, int blockLength, PseudoAdler32 adler32) {
        Map matchesTable = new SVNHashMap();
        for(int i = 0; i < dataLength; i+= blockLength) {
            int length = i + blockLength >= dataLength ? dataLength - i : blockLength;
            adler32.add(data, i, length);
            Integer checksum = new Integer(adler32.getValue());
            if (!matchesTable.containsKey(checksum)) {
                matchesTable.put(checksum, new Match(i, length));
            }
            adler32.reset();
        }
        return matchesTable;
    }
    
    private static boolean equals(byte[] a, int aLength, int aPos, int length, byte[] b, int bLength, int bPos) {
        if (aPos + length - 1 > aLength || bPos + length > bLength) {
            return false;
        }
        for(int i = 0; i < length; i++) {
            if (a[aPos + i] != b[bPos + i]) {
                return false;
            }
        }
        return true;
    }
    
    private static class Match {
        
        public Match(int p, int l) {
            position = p;
            length = l;
        }
        
        public int position;
        public int length;
        public int advance;
    }

    private static int ADLER32_MASK = 0x0000FFFF;

    private static class PseudoAdler32 {        
        
        private int myS1;
        private int myS2;
        private int myLength;
        
        public PseudoAdler32() {
            reset();
        }
        
        public void add(byte b) {
            int z = b & 0x000000FF;
            myS1 = myS1 + z;
            myS1 = myS1 & ADLER32_MASK;
            myS2 = myS2 + myS1;
            myS2 = myS2 & ADLER32_MASK;
            myLength++;
        }
        
        public void remove(byte b) {
            int z = b & 0x000000FF;
            myS1 = myS1 - z;
            myS1 = myS1 & ADLER32_MASK;
            myS2 = myS2 - (myLength * z + 1);
            myS2 = myS2 & ADLER32_MASK;
            myLength--;
        }
        
        public void add(byte[] data, int offset, int length) {
            for (int i = offset; i < offset + length; i++) {
                add(data[i]);
            }
        }
        
        public int getValue() {
            return (myS2 << 16) | myS1;
        }
        
        public void reset() {
            myS1 = 1;
            myS2 = 0;
            myLength = 0;
        }
    }
}
 