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

import org.tmatesoft.svn.core.SVNException;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNUUIDGenerator {

    public static String generateUUIDString() throws SVNException {
        return UUID.randomUUID().toString();
    }

    public static byte[] generateUUID() throws SVNException {
        final UUID uuid = UUID.randomUUID();

        final ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    public static String formatUUID(byte[] uuid) {
        final ByteBuffer bb = ByteBuffer.allocate(16);
        bb.put(uuid, 0, Math.min(uuid.length, 16));
        bb.clear();

        final long firstLong = bb.getLong();
        final long secondLong = bb.getLong();

        return new UUID(firstLong, secondLong).toString();
    }
}
