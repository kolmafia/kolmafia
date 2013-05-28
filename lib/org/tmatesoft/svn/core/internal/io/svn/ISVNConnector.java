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
import java.io.InputStream;
import java.io.OutputStream;

import org.tmatesoft.svn.core.SVNException;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public interface ISVNConnector {
    
    public void open(SVNRepositoryImpl repository) throws SVNException;
    
    public void handleExceptionOnOpen(SVNRepositoryImpl repository, SVNException exception) throws SVNException;
    
    public boolean isConnected(SVNRepositoryImpl repository) throws SVNException;

    public void close(SVNRepositoryImpl repository) throws SVNException;
    
    public boolean isStale();
    
    public OutputStream getOutputStream() throws IOException;

    public InputStream getInputStream() throws IOException;
}