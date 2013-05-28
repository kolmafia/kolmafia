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

import java.io.File;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.io.fs.FSFS;
import org.tmatesoft.svn.core.internal.io.fs.IFSRepresentationCacheManager;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public interface IFSRepresentationCacheManagerFactory {
    
    public IFSRepresentationCacheManager openRepresentationCache(FSFS fsfs) throws SVNException;
    
    public void createRepresentationCache(File path) throws SVNException;

}
