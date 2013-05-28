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
package org.tmatesoft.svn.core.internal.util;

import java.rmi.server.UID;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNUUIDGenerator {

    private static final int NODE_LENGTH = 6;
    private static byte[] ourUUIDStateNode = new byte[NODE_LENGTH];
    private static long ourUUIDStateSeqNum;
    private static long ourLastGeneratedTime;
    private static long ourFudgeFactor;

    /*
     * We must be sure we don't use the same time values for generating 'random'
     * UUIDs
     */
    private static long getCurrentTime() {
        long currentTime = System.currentTimeMillis();
        /* if clock reading changed since last UUID generated... */
        if (ourLastGeneratedTime != currentTime) {
            /*
             * The clock reading has changed since the last UUID was generated.
             * Reset the fudge factor. if we are generating them too fast, then
             * the fudge may need to be reset to something greater than zero.
             */
            if (ourLastGeneratedTime + ourFudgeFactor > currentTime) {
                ourFudgeFactor = ourLastGeneratedTime + ourFudgeFactor - currentTime + 1;
            } else {
                ourFudgeFactor = 0;
            }
            ourLastGeneratedTime = currentTime;
        } else {
            /* We generated two really fast. Bump the fudge factor. */
            ++ourFudgeFactor;
        }
        return currentTime + ourFudgeFactor;
    }

    public static String generateUUIDString() throws SVNException {
        return formatUUID(generateUUID());
    }
    
    public static synchronized byte[] generateUUID() throws SVNException {
        if (ourUUIDStateNode[0] == 0) {
            initState();
        }
        long timestamp = getCurrentTime();
        byte[] uuidData = new byte[16];
        uuidData[0] = (byte) timestamp;
        uuidData[1] = (byte) (timestamp >> 8);
        uuidData[2] = (byte) (timestamp >> 16);
        uuidData[3] = (byte) (timestamp >> 24);
        uuidData[4] = (byte) (timestamp >> 32);
        uuidData[5] = (byte) (timestamp >> 40);
        uuidData[6] = (byte) (timestamp >> 48);
        uuidData[7] = (byte) (((timestamp >> 56) & 0x0F) | 0x10);
        uuidData[8] = (byte) (((ourUUIDStateSeqNum >> 8) & 0x3F) | 0x80);
        uuidData[9] = (byte) ourUUIDStateSeqNum;
        System.arraycopy(ourUUIDStateNode, 0, uuidData, 10, NODE_LENGTH);
        return uuidData;
    }

    public static String formatUUID(byte[] uuid) {
        if (uuid.length < 16) {
            byte[] tmpBuf = new byte[16];
            Arrays.fill(tmpBuf, (byte) 0);
            System.arraycopy(uuid, 0, tmpBuf, 0, uuid.length);
            uuid = tmpBuf;
        }
        StringBuffer sb = new StringBuffer();
        SVNFormatUtil.appendHexNumber(sb, uuid[0]);
        SVNFormatUtil.appendHexNumber(sb, uuid[1]);
        SVNFormatUtil.appendHexNumber(sb, uuid[2]);
        SVNFormatUtil.appendHexNumber(sb, uuid[3]);
        sb.append('-');
        SVNFormatUtil.appendHexNumber(sb, uuid[4]);
        SVNFormatUtil.appendHexNumber(sb, uuid[5]);
        sb.append('-');
        SVNFormatUtil.appendHexNumber(sb, uuid[6]);
        SVNFormatUtil.appendHexNumber(sb, uuid[7]);
        sb.append('-');
        SVNFormatUtil.appendHexNumber(sb, uuid[8]);
        SVNFormatUtil.appendHexNumber(sb, uuid[9]);
        sb.append('-');
        SVNFormatUtil.appendHexNumber(sb, uuid[10]);
        SVNFormatUtil.appendHexNumber(sb, uuid[11]);
        SVNFormatUtil.appendHexNumber(sb, uuid[12]);
        SVNFormatUtil.appendHexNumber(sb, uuid[13]);
        SVNFormatUtil.appendHexNumber(sb, uuid[14]);
        SVNFormatUtil.appendHexNumber(sb, uuid[15]);
        return sb.toString();
    }

    private static void initState() throws SVNException {
        /*
         * Offset between UUID formatted times and System.currentTimeMillis()
         * formatted times. UUID UTC base time is October 15, 1582.
         * System.currentTimeMillis() base time is January 1, 1970.
         */
        long currentTime = System.currentTimeMillis() * 10 + 0x01B21DD213814000L;
        Random randomGen = new Random();
        randomGen.setSeed(((currentTime >> 32) ^ currentTime) & 0xffffffffL);
        ourUUIDStateSeqNum = randomGen.nextLong() & 0x0FFFFL;
        getRandomInfo(ourUUIDStateNode);
    }

    private static void getRandomInfo(byte[] node) throws SVNException {
        UID uid = new UID();
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "MD5 implementation not found: {0}", e.getLocalizedMessage());
            SVNErrorManager.error(err, e, SVNLogType.DEFAULT);
        }
        digest.update(uid.toString().getBytes());
        byte[] seed = digest.digest();
        int numToCopy = node.length < seed.length ? node.length : seed.length;
        System.arraycopy(seed, 0, node, 0, numToCopy);
        node[0] |= 0x01;
    }
}
