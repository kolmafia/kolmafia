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
package org.tmatesoft.svn.core.internal.io.fs;

import org.tmatesoft.svn.core.SVNException;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public interface IFSRepresentationCacheManager {
    
    public void insert(final FSRepresentation representation, boolean rejectDup) throws SVNException;

    public void runWriteTransaction(IFSSqlJetTransaction transaction) throws SVNException;

    public void runReadTransaction(IFSSqlJetTransaction transaction) throws SVNException;
    
    public FSRepresentation getRepresentationByHash(String hash) throws SVNException;
    
    public void close() throws SVNException;
}
