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
package org.tmatesoft.svn.core.wc.admin;

import java.io.OutputStream;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.ISVNDiffGenerator;


/**
 * The <b>ISVNGNUDiffGenerator</b> is the interface for diff generators used 
 * in diff operations of <b>SVNLookClient</b>.  
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public interface ISVNGNUDiffGenerator extends ISVNDiffGenerator {
    
    /**
     * The type of modification denoting addition. 
     */
    public static final int ADDED = 0;
    
    /**
     * The type of modification denoting deletion. 
     */
    public static final int DELETED = 1;
    /**
     * The type of modification denoting modification. 
     */
    public static final int MODIFIED = 2;

    /**
     * The type of modification denoting copying. 
     */
    public static final int COPIED = 3;
    
    /**
     * The type of modification denoting that no diff is available after 
     * a header. Called if a header is written, but differences can not be 
     * written due to some reasons. Default generator simple prints a new line
     * symbol when handling this type of change.   
     */
    public static final int NO_DIFF = 4;
    
    /**
     * Informs this diff generator about a change to a path.  
     *  
     * @param  type                  one of static fields of this interface  
     * @param  path                  a changed path
     * @param  copyFromPath          a copy-from source path if <code>path</code> is 
     *                               the result of a copy          
     * @param  copyFromRevision      a copy-from source revision if <code>path</code> is 
     *                               the result of a copy    
     * @param  result                an output stream where a header is to be written
     * @throws SVNException
     */
    public void displayHeader(int type, String path, String copyFromPath, long copyFromRevision, OutputStream result) throws SVNException; 
    
}
