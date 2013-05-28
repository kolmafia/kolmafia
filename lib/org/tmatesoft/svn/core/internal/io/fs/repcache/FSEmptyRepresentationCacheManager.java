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

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.io.fs.FSRepresentation;
import org.tmatesoft.svn.core.internal.io.fs.IFSRepresentationCacheManager;
import org.tmatesoft.svn.core.internal.io.fs.IFSSqlJetTransaction;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSEmptyRepresentationCacheManager implements IFSRepresentationCacheManager {
    
    /**
     * @throws SVNException
     */
    public void close() throws SVNException {
    }

    /**
     * @param hash
     * @return
     * @throws SVNException
     */
    public FSRepresentation getRepresentationByHash(String hash) throws SVNException {
        return null;
    }

    /**
     * @param representation
     * @param rejectDup
     * @throws SVNException
     */
    public void insert(FSRepresentation representation, boolean rejectDup) throws SVNException {
    }

    /**
     * @param transaction
     * @throws SVNException
     */
    public void runWriteTransaction(IFSSqlJetTransaction transaction) throws SVNException {
        transaction.run();
    }

    public void runReadTransaction(IFSSqlJetTransaction transaction) throws SVNException {
        transaction.run();
    }

}
