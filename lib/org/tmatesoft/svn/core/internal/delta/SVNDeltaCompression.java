/*
 * ====================================================================
 * Copyright (c) 2004-2010 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.delta;

public enum SVNDeltaCompression {

    /**
     * @since 1.10
     */
    None(new byte[] {'S', 'V', 'N', '\0'}),

    /**
     * @since 1.10, new in Subversion 1.4
     */
    Zlib(new byte[] {'S', 'V', 'N', '\1'}),

    /**
     * @since 1.10, new in Subversion 1.10
     */
    LZ4(new byte[] {'S', 'V', 'N', '\2'});

    /**
     * Bytes of the delta header of a compressed diff window.
     */
    private final byte[] header;

    SVNDeltaCompression(byte[] header) {
        this.header = header;
    }

    public byte[] getHeader() {
        return header;
    }

    /**
     * This method is only used to convert legacy 'compress' flag into a {@link SVNDeltaCompression} member.
     * It is supposed to be deleted as soon as all deprecated methods with 'compress' flag are removed from svnkit.
     */
    @Deprecated
    public static SVNDeltaCompression fromLegacyCompress(boolean compress) {
        return compress ? Zlib : None;
    }
}
