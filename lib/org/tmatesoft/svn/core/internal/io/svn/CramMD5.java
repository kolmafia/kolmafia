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

package org.tmatesoft.svn.core.internal.io.svn;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
import org.tmatesoft.svn.core.internal.util.SVNFormatUtil;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class CramMD5 {

    private SVNPasswordAuthentication myCredentials;

    public void setUserCredentials(SVNPasswordAuthentication credentials) {
        myCredentials = credentials;
    }

    public byte[] buildChallengeResponse(byte[] challenge) throws IOException {
        byte[] password = myCredentials.getPassword().getBytes("UTF-8");
        byte[] secret = new byte[64];
        Arrays.fill(secret, (byte) 0);
        System.arraycopy(password, 0, secret, 0, Math.min(secret.length, password.length));
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        for (int i = 0; i < secret.length; i++) {
            secret[i] ^= 0x36;
        }
        digest.update(secret);
        digest.update(challenge);
        byte[] result = digest.digest();
        for (int i = 0; i < secret.length; i++) {
            secret[i] ^= (0x36 ^ 0x5c);
        }
        digest.update(secret);
        digest.update(result);
        result = digest.digest();
        String hexDigest = "";
        for (int i = 0; i < result.length; i++) {
            hexDigest += SVNFormatUtil.getHexNumberFromByte(result[i]);            
        }
        String response = myCredentials.getUserName() + " " + hexDigest;
        response = response.getBytes("UTF-8").length + ":" + response + " ";
        return response.getBytes("UTF-8");
    }

}
