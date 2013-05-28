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
import java.io.OutputStream;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;

/**
 * The <b>ISVNDiffGenerator</b> should be implemented by drivers generating
 * contents difference between files in order to be used in 'diff' operations 
 * performed by <b>SVNDiffClient</b>. 
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 * @see     SVNDiffClient
 * @see     DefaultSVNDiffGenerator
 */
public interface ISVNDiffGenerator {
    /**
     * Initializes the driver setting up the paths/URLs that should be compared, or
     * root paths/URLs for those paths to which a diff operation should be restricted.
     * 
     * <p>
     * These paths have got the same meaning as <i>OLD-TGT</i> (<code>anchorPath1</code>)
     * and <i>NEW-TGT</i> (<code>anchorPath2</code>) in the SVN command line client's 
     * <i>'svn diff'</i> command. So, they can be either local paths, or URLs pointing to
     * repository locations. If one of them (or both) is a URL it may differ from that
     * one passed to an appropriate <b>doDiff()</b> method of <b>SVNDiffClient</b> in that
     * case when in a peg revision it's one URL, but in the target revision it was changed
     * (moved?) to some other one. So, this method should receive the real one.
     * 
     * @param anchorPath1  an old path/URL
     * @param anchorPath2  a new path/URL
     */
    public void init(String anchorPath1, String anchorPath2);
    
    /**
     * Sets the root path for this diff generator.
     * 
     * <p>
     * This can be used to make all paths in a diff output be relative
     * to this base path.
     * 
     * @param basePath a base path for this driver
     */
    public void setBasePath(File basePath);
    
    /**
     * Enables or disables generating differnces between files having
     * a binary MIME type.
     * 
     * <p>
     * Like the <i>'--force'</i> option of the <i>'svn diff'</i> command.
     * 
     * @param forced if <span class="javakeyword">true</span> binary
     *               files will also be diffed, otherwise not
     */
    public void setForcedBinaryDiff(boolean forced);
    
    /**
     * Sets the encoding charset to be used for a diff output. 
     * 
     * @param encoding the name of a charset 
     */
    public void setEncoding(String encoding);
    
    /**
     * Gets the encoding charset being in use for a diff output. 
     * 
     * @return the name of the charset being in use
     */
    public String getEncoding();

    /**
     * Sets the EOL marker bytes to use in diff output.
     * 
     * @param eol     EOL bytes
     * @since         1.2.0
     */
    public void setEOL(byte[] eol);

    /**
     * Returns the EOL marker bytes used in diff output.
     * 
     * @return  EOL bytes
     * @since   1.2.0 
     */
    public byte[] getEOL();
    
    /**
     * Enables or disables generating differences for deleted
     * files.
     * 
     * @param isDiffDeleted if <span class="javakeyword">true</span> then
     *                      deleted files will be diffed, otherwise not
     * @see                 #isDiffDeleted()                      
     */
    public void setDiffDeleted(boolean isDiffDeleted);
    
    /**
     * Tells whether deleted files are enabled to be diffed.  
     * 
     * @return <span class="javakeyword">true</span> if deleted files
     *         should be diffed (the driver is set to generate differences
     *         for deleted files as well), otherwise 
     *         <span class="javakeyword">false</span>
     * @see    #setDiffDeleted(boolean) 
     */
    public boolean isDiffDeleted();
    
    /**
     * Enables or disables generating differences for added
     * files.   
     *
     * @param isDiffAdded   if <span class="javakeyword">true</span> then
     *                      added files will be diffed, otherwise not
     * @see                 #isDiffAdded()
     */
    public void setDiffAdded(boolean isDiffAdded);

    /**
     * Tells whether added files are enabled to be diffed. 
     * 
     * @return <span class="javakeyword">true</span> if added files
     *         should be diffed, otherwise 
     *         <span class="javakeyword">false</span>
     * @see    #setDiffAdded(boolean)
     */
    public boolean isDiffAdded();

    /**
     * Enables or disables generating differences against copy source 
     * for copied files. This switch is relevant to  
     * {@link org.tmatesoft.svn.core.wc.admin.SVNLookClient}'s diff 
     * operations. 
     *
     * <p>
     * Like the <i>'--diff-copy-from'</i> option of the <i>'svnlook diff'</i> command.
     * 
     * @param isDiffCopied   if <span class="javakeyword">true</span> then
     *                       copied files will be diffed against copy sources, 
     *                       otherwise they will be treated as newly added files
     * @see                  #isDiffCopied()
     */
    public void setDiffCopied(boolean isDiffCopied);

    /**
     * Tells whether copied files are enabled to be diffed against their 
     * copy sources. This switch is relevant to  
     * {@link org.tmatesoft.svn.core.wc.admin.SVNLookClient}'s diff 
     * operations. 
     * 
     * @return  <span class="javakeyword">true</span> if copied files
     *          should be diffed against copy sources;  
     *          <span class="javakeyword">false</span> if copied files 
     *          should be treated as newly added
     * @see     #setDiffCopied(boolean) 
     */
    public boolean isDiffCopied();

    /**
     * Includes or not unversioned files into diff processing. 
     * 
     * <p>
     * If a diff operation is invoked on  a versioned directory and 
     * <code>diffUnversioned</code> is <span class="javakeyword">true</span> 
     * then all unversioned files that may be met in the directory will 
     * be processed as added. Otherwise if <code>diffUnversioned</code> 
     * is <span class="javakeyword">false</span> such files are ignored. 
     * 
     * @param diffUnversioned controls whether to diff unversioned files 
     *                        or not 
     * @see                   #isDiffUnversioned()
     */
    public void setDiffUnversioned(boolean diffUnversioned);
    
    /**
     * Says if unversioned files are also diffed or ignored.
     * 
     * @return <span class="javakeyword">true</span> if diffed, 
     *         <span class="javakeyword">false</span> if ignored  
     * @see    #setDiffUnversioned(boolean)
     */
    public boolean isDiffUnversioned();
    
    /**
     * Creates a temporary directory (when necessary) where temporary files
     * will be created.
     * 
     * <p>  
     * This temporary directory exists till the end of the diff operation. 
     *  
     * @return an abstract pathname denoting a newly-created temporary 
     *         directory
     * @throws SVNException if a directory can not be created
     */
    public File createTempDirectory() throws SVNException;
    
    /**
     * Writes the differences in file properties to the specified output 
     * stream. 
     * 
     * @param  path           a file path on which the property changes
     *                        are written to the output    
     * @param  baseProps      a {@link java.util.Map} of old properties
     *                        (property names are mapped to their values)
     * @param  diff           a {@link java.util.Map} of changed properties
     *                        (property names are mapped to their values)
     * @param  result         the target {@link java.io.OutputStream} where
     *                        the differences will be written to
     * @throws SVNException   if can not save diff data
     */
    public void displayPropDiff(String path, SVNProperties baseProps, SVNProperties diff, OutputStream result) throws SVNException;
    
    /**
     * Generates and writes differences between two files to the specified 
     * output stream. 
     * 
     * <p>
     * <code>file1</code> or <code>file2</code> may be temporary files crteated
     * to get file contents from the repository (when running diff on URLs).
     * These temporary files will be deleted with the temporary directory
     * (created by {@link #createTempDirectory()}) when the operation ends up.
     * 
     * @param  path         a file path on which the differences are 
     *                      generated and written to the output
     * @param  file1        a file with old contents
     * @param  file2        a file with new contents
     * @param  rev1         the first diff revision of <code>file1</code>
     * @param  rev2         the second diff revision of <code>file2</code>
     * @param  mimeType1    the MIME-type of <code>file1</code> 
     * @param  mimeType2    the MIME-type of <code>file2</code>
     * @param  result       the target {@link java.io.OutputStream} where
     *                      the differences will be written to
     * @throws SVNException if can not save diff data
     */
    public void displayFileDiff(String path, File file1, File file2, String rev1, String rev2, String mimeType1, String mimeType2, OutputStream result) throws SVNException;
    
    /**
     * Notifies this generator that the directory was deleted in revision <code>rev2</code>.
     * 
     * @param  path           a directory path
     * @param  rev1           the first diff revision
     * @param  rev2           the second diff revision
     * @throws SVNException   
     * @since                 1.1
     */
    public void displayDeletedDirectory(String path, String rev1, String rev2) throws SVNException;

    /**
     * Notifies this generator that the directory was added in revision <code>rev2</code>.
     * 
     * @param  path           a directory path
     * @param  rev1           the first diff revision
     * @param  rev2           the second diff revision
     * @throws SVNException
     * @since                 1.1
     */
    public void displayAddedDirectory(String path, String rev1, String rev2) throws SVNException;

    /**
     * Tells whether to force diff even if files are binary.
     * 
     * @return <span class="javakeyword">true</span> to force diff for binary files; otherwise 
     *         <span class="javakeyword">false</span>
     * @since  1.1.1
     */
    public boolean isForcedBinaryDiff();

}
