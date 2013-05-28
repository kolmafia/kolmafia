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
package org.tmatesoft.svn.core.internal.wc;

import java.io.InputStream;
import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;



/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public interface ISVNLoadHandler {
    
    public void closeRevision() throws SVNException;
    
    public void openRevision(Map headers) throws SVNException;
    
    public void openNode(Map headers) throws SVNException;
    
    public void closeNode() throws SVNException;
    
    public void parseUUID(String uuid) throws SVNException;
    
    public void removeNodeProperties() throws SVNException;

    public void setRevisionProperty(String propertyName, SVNPropertyValue propertyValue) throws SVNException;

    public void applyTextDelta() throws SVNException;

    public void setFullText() throws SVNException;

    public void parseTextBlock(InputStream dumpStream, long contentLength, boolean isDelta) throws SVNException;

    public void deleteNodeProperty(String propertyName) throws SVNException;

    public void setNodeProperty(String propertyName, SVNPropertyValue propertyValue) throws SVNException;

}
