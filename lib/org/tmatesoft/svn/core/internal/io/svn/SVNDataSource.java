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

import java.io.InputStream;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
class SVNDataSource {

    private InputStream myInputStream;

    private long myLenght;

    public SVNDataSource(InputStream is, long length) {
        myInputStream = is;
        myLenght = length;
    }

    public InputStream getInputStream() {
        return myInputStream;
    }

    public long lenght() {
        return myLenght;
    }

}
