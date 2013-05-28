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
package org.tmatesoft.svn.core.wc;

import java.io.File;


/**
 * ISVNCommitParameters is the interface for parameters 
 * which set behaviour for a commit operation that touches 
 * still versioned files or dirs that are somehow missing.  
 * 
 * <p>
 * To bring your commit parameters into usage, simply pass them to 
 * a committer object, for example, to 
 * {@link SVNCommitClient#setCommitParameters(ISVNCommitParameters) SVNCommitClient}. 
 *   
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 * @see     DefaultSVNCommitParameters
 */
public interface ISVNCommitParameters {
    /**
     * A constant that defines a file/dir missing situation as an 
     * error, commit should fail.
     */
    public static final Action ERROR = new Action();
    /**
     * A constant that instructs a commit operation to skip a 
     * missing item. So, the item is not committed. 
     */
    public static final Action SKIP = new Action();
    /**
     * A constant that instructs a commit operation to force
     * a deletion of a missing item. Although the item may be not 
     * scheduled for deletion (only missing in filesystem) it will 
     * be deleted from version control.
     */
    public static final Action DELETE = new Action();
    
    /**
     * Returns the action a commit operation should undertake 
     * if there's a missing file under commit scope that is not however 
     * scheduled for deletion.    
     * 
     * @param  file a missing file
     * @return      an action that must be one of 
     *              the constants defined in the interface 
     */
    public Action onMissingFile(File file);

    /**
     * Returns the action a commit operation should undertake 
     * if there's a missing directory under commit scope that is not 
     * however scheduled for deletion.    
     * 
     * @param  file a missing directory
     * @return      an action that must be one of 
     *              the constants defined in the interface 
     */
    public Action onMissingDirectory(File file);
    
    /**
     * Instructs whether to remove the local <code>directory</code> after commit or not.
     *    
     * @param directory  working copy directory
     * @return           <span class="javakeyword">true</span> if directory should be deleted after commit
     */
    public boolean onDirectoryDeletion(File directory);
    
    /**
     * Instructs whether to remove the local <code>file</code> after commit or not.
     * 
     * @param file  working copy file 
     * @return      <span class="javakeyword">true</span> if file should be deleted after commit
     */
    public boolean onFileDeletion(File file);
    
    /**
     * This class is simply used to define an action a commit 
     * operation should undertake in case of a missing file/directory. 
     * 
     * @version 1.1
     * @author  TMate Software Ltd.
     */
    public static class Action {
        private Action() {
        }
    }
}
