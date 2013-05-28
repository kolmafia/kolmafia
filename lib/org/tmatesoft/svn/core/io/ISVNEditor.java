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

package org.tmatesoft.svn.core.io;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;

/**
 * The <b>ISVNEditor</b> interface is used by <b>SVNRepository</b> to 
 * update versioned files/dirs, check out files/dirs from a repository, 
 * commit changes to a repository, take status of files/dirs,
 * get differences between files.
 * 
 * <p>
 * When used for updating (checking out, getting differences or status), an 
 * implementor should provide its own appropriate implementation of the 
 * <b>ISVNEditor</b> interface along with a reporter baton (<b>ISVNReposrterBaton</b>)
 * to a corresponding method of an <b>SVNRepository</b> driver. Reporter baton
 * will be used to describe the state of local dirs/files - their current revisions,
 * whether a file/dir is deleted or switched. An editor is invoked after the reporter 
 * baton finishes its work. It is used to "edit" the state of files/dirs, where
 * "edit" may mean anything: applying changes in updating, switching, checking out (what 
 * really changes the state), or handling changes in getting status or differences (what is 
 * only used to inform about, show, separately store changes, and so on). The concrete behaviour
 * of the editor is implemented by the provider of that editor.      
 * 
 * <p>
 * The other kind of using <b>ISVNEditor</b> is committing changes to a repository.
 * Here an editor is given to a caller, and the caller himself describes the changes
 * of local files/dirs. All that collected info is then committed in a single transaction
 * when a caller invokes the {@link #closeEdit()} method of the editor.
 * 
 * For more information on using editors, please, read these on-line articles: 
 * <ul> 
 * <li><a href="http://svnkit.com/kb/dev-guide-update-operation.html">Using ISVNReporter/ISVNEditor in update-related operations</a>
 * <li><a href="http://svnkit.com/kb/dev-guide-commit-operation.html">Using ISVNEditor in commit operations</a>
 * </ul>
 *  
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 * @see		ISVNReporterBaton
 * @see     <a target="_top" href="http://svnkit.com/kb/examples/">Examples</a>
 */
public interface ISVNEditor extends ISVNDeltaConsumer {
    /**
     * Sets the target revision the operation is running for. For example, 
     * the target revision to which an update is running.
     *  
     * @param  revision			a revision number
     * @throws SVNException
     */
    public void targetRevision(long revision) throws SVNException;
    
    /**
     * Opens the root directory on which the operation was invoked. All
     * property changes as well as entries adding/deletion will be applied to this
     * root directory.
     * When coming back up to this root (after traversing the entire tree) you
     * should close the root by calling {@link #closeDir()}.
     * 
     * @param  revision			the revision number of the root directory 			
     * @throws SVNException
     */
    public void openRoot(long revision) throws SVNException;
    
    /**
     * Deletes an entry. 
     * <p>
     * In a commit - deletes an entry from a repository. In an update - 
     * deletes an entry locally (since it has been deleted in the repository). 
     * In a status - informs that an entry has been deleted. 
     *  
     * @param  path			an entry path relative to the root		
     *                      directory opened by {@link #openRoot(long) openRoot()} 
     * @param  revision		the revision number of <code>path</code>
     * @throws SVNException 
     */
    public void deleteEntry(String path, long revision) throws SVNException;
    
    /**
     * Indicates that a path is present as a subdirectory in the edit source, 
     * but can not be conveyed to the edit consumer (perhaps because of 
     * authorization restrictions). 
     * 
     * @param  path			 a dir path relative to the root       
     *                       directory opened by {@link #openRoot(long) openRoot()}
     * @throws SVNException
     */
    public void absentDir(String path) throws SVNException;
    
    /**
     * Indicates that a path is present as a file in the edit source, 
     * but can not be conveyed to the edit consumer (perhaps because of 
     * authorization restrictions). 
     * 
     * @param  path				a file path relative to the root       
     *                          directory opened by {@link #openRoot(long) openRoot()}
     * @throws SVNException
     */
    public void absentFile(String path) throws SVNException;
    
    /**
     * Adds a directory.
     * 
     * <p>
     * In a commit - adds a new directory to a repository. In an update - locally adds
     * a directory that was added in the repository. In a status - informs about a new
     * directory scheduled for addition.
     *   
     * <p>
     * If <code>copyFromPath</code> is not <span class="javakeyword">null</span> then it says
     * that <code>path</code> is copied from <code>copyFromPath</code> located in 
     * <code>copyFromRevision</code>.
     *    
     * @param  path					a directory path relative to the root       
     *                              directory opened by {@link #openRoot(long) openRoot()}  
     * @param  copyFromPath			an ancestor of the added directory
     * @param  copyFromRevision		the revision of the ancestor
     * @throws SVNException
     */
    public void addDir(String path, String copyFromPath, long copyFromRevision) throws SVNException;
    
    /**
     * Opens a directory. All property changes as well as entries 
     * adding/deletion can be applied to this directory. 
     * 
     * @param path			a directory path relative to the root       
     *                      directory opened by {@link #openRoot(long) openRoot()} 
     * @param revision		the revision of the directory
     * @throws SVNException
     */
	public void openDir(String path, long revision) throws SVNException;
    
	/**
     * Changes the value of a property of the currently opened/added directory.
     * 
     * @param  name				the name of a property to be changed
     * @param  value			new property value
     * @throws SVNException
     * @see 					#openDir(String, long)
     * @since                   1.2.0
     */
    public void changeDirProperty(String name, SVNPropertyValue value) throws SVNException;

    /**
     * Closes the currently opened directory fixing all changes of its 
     * properties and/or entries. Closing a directory picks up an editor
     * to a parent directory.
     * 
     * @throws SVNException
     */
    public void closeDir() throws SVNException;
    
    /**
     * Adds a file.
     * 
     * <p>
     * In a commit - adds a new file to a repository. In an update - locally adds
     * a file that was added in the repository. In a status - informs about a new
     * file scheduled for addition.
     *   
     * <p>
     * If <code>copyFromPath</code> is not <span class="javakeyword">null</span> then it says
     * that <code>path</code> is copied from <code>copyFromPath</code> located in 
     * <code>copyFromRevision</code>.
     * 
     * @param  path					a file path relative to the root       
     *                              directory opened by {@link #openRoot(long) openRoot()}				
     * @param  copyFromPath         an ancestor of the added file
     * @param  copyFromRevision     the revision of the ancestor
     * @throws SVNException
     */
    public void addFile(String path, String copyFromPath, long copyFromRevision) throws SVNException;
    
    /**
     * Opens a file. After it's opened, apply delta to its contents or change the 
     * file properties. 
     *  
     * @param path          a file path relative to the root       
     *                      directory opened by {@link #openRoot(long) openRoot()} 
     * @param revision      the revision of the file
     * 
     * @throws SVNException
     */
    public void openFile(String path, long revision) throws SVNException;

    /**
     * Changes the value of a property of the currently opened/added file. 
     * 
     * @param  path           file path relative to the root of this editor 
     * @param  propertyName   property name
     * @param  propertyValue  property value
     * @throws SVNException
     * @since                 1.2.0        
     */
    public void changeFileProperty(String path, String propertyName, SVNPropertyValue propertyValue) throws SVNException;

    /**
     * Closes the opened file fixing all properties and/or contents changes. 
     * 
     * @param  path          a file path relative to the root       
     *                       directory opened by {@link #openRoot(long) openRoot()}  
     * @param  textChecksum	 an MD5 checksum for the modified file 
     * @throws SVNException	 if the calculated upon the actual changed contents 
     *                       checksum does not match the expected <code>textChecksum</code>
     */
    public void closeFile(String path, String textChecksum) throws SVNException;
    
    /**
     * Closes this editor finalizing the whole operation the editor
     * was used for. In a commit - sends collected data to commit a transaction. 
     * 
     * @return              a committed revision information  					
     * @throws SVNException
     */
    public SVNCommitInfo closeEdit() throws SVNException;
    
    /**
     * Aborts the current running editor due to errors occured.
     * 
     * <p>
     * If an exception is thrown from an editor's method, call this method
     * to abort the editor.
     *  
     * @throws SVNException
     */
    public void abortEdit() throws SVNException;
}
