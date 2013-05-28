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
package org.tmatesoft.svn.core.internal.wc;

import java.io.OutputStream;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public interface ISVNFileContentFetcher {

    public void fetchFileContent(OutputStream os) throws SVNException;

    public boolean fileIsBinary() throws SVNException;
    
    public SVNPropertyValue getProperty(String propertyName) throws SVNException;
}
