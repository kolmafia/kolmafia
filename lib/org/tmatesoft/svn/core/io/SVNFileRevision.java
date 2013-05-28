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

import org.tmatesoft.svn.core.SVNProperties;


/**
 * The <b>SVNFileRevision</b> class represents information on what path a file 
 * is located at (in a repository) in a particular revision, contains file properties 
 * and revision properties for that revision.
 * 
 * <p>
 * When getting a range of file revisions (in particular, annotating), 
 * calling an <b>SVNRepository</b>'s 
 * {@link SVNRepository#getFileRevisions(String, long, long, ISVNFileRevisionHandler) getFileRevision()}
 * <b>SVNFileRevision</b> objects are passed to an <b>ISVNFileRevisionHandler</b>'s {@link ISVNFileRevisionHandler#openRevision(SVNFileRevision) openRevision()}
 * method.  
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 * @see     SVNRepository
 * @see		ISVNFileRevisionHandler
 */
public class SVNFileRevision implements Comparable {
    
    private String myPath;
    private long myRevision;
    private SVNProperties myProperties;
    private SVNProperties myPropertiesDelta;
    private boolean myIsResultOfMerge;
    
    /**
     * Constructs an instance of <b>SVNFileRevision</b>.
     *  
     * @param path				a file path relative to a repository location
     * 							(a URL used to create an 
     * 							<b>SVNRepository</b> to access a repository)
     * @param revision			a revision of the file
     * @param properties		revision properties
     * @param propertiesDelta	file properties for the <code>revision</code>
     */
    public SVNFileRevision(String path, long revision, SVNProperties properties, SVNProperties propertiesDelta) {
        this(path, revision, properties, propertiesDelta, false);
    }
    
    /**
     * Constructs an instance of <b>SVNFileRevision</b>.
     *  
     * @param path              a file path relative to a repository location
     *                          (a URL used to create an 
     *                          <b>SVNRepository</b> to access a repository)
     * @param revision          a revision of the file
     * @param properties        revision properties
     * @param propertiesDelta   file properties for the <code>revision</code>
     * @param isResultOfMerge   whether this revision of the file is the result of a merge
     * @since                   1.2.0 
     */
    public SVNFileRevision(String path, long revision, SVNProperties properties, SVNProperties propertiesDelta, boolean isResultOfMerge) {
        myPath = path;
        myRevision = revision;
        myProperties = properties;
        myPropertiesDelta = propertiesDelta;
        myIsResultOfMerge = isResultOfMerge;
    }

    /**
     * Gets the file path (relative to a repository root URL).
     *  
     * @return	the path of the file
     * @see     SVNRepository
     */
    public String getPath() {
        return myPath;
    }
    
    /**
     * Returns revision properties. Use {@link org.tmatesoft.svn.core.SVNRevisionProperty}
     * constants (they are revision property names) to retrieve values of the
     * corresponding properties.
     * 
     * @deprecated use {@link #getRevisionProperties() } instead 
     * @return	a map which keys are revision property names and values
     * 			are their values (both are strings)
     */
    public SVNProperties getProperties() {
        return myProperties;
    }
    
    /**
     * Returns revision properties. Use {@link org.tmatesoft.svn.core.SVNRevisionProperty}
     * constants (they are revision property names) to retrieve values of the
     * corresponding properties.
     * 
     * @return  a map which keys are revision property names and values
     *          are their values (both are strings)
     */
    public SVNProperties getRevisionProperties() {
        return myProperties;
    }
    
    /**
     * Returns file properties for this file (for this revision).
     * Properties delta for a revision is the same as full properties for
     * that revision. 
     * 
     * @return a map where keys are file property names and values are the
     *         property values 
     */
    public SVNProperties getPropertiesDelta() {
        return myPropertiesDelta;
    }
    
    /**
     * Gets the revision of the file.
     *  
     * @return	the revision number of the file
     */
    public long getRevision() {
        return myRevision;
    }
    
    /**
     * Compares this object with another one. 
     * 
     * @param  o  an object to compare with
     * @return    <ul>
     *            <li>1 - if <code>o</code> is either <span class="javakeyword">null</span>,
     *            or is not an instance of <b>SVNFileRevision</b>, or the revision value of
     *            this object is bigger than the one of <code>o</code>;
     *            </li>
     *            <li>-1 -  if the revision value of this object is smaller than the one of 
     *            <code>o</code>;
     *            </li>
     *            <li>0 - if and only if the revision values of this object and <code>o</code> 
     *            are the same (equal)
     *            </li>
     *            </ul>
     */
    public int compareTo(Object o) {
        if (o == null || o.getClass() != SVNFileRevision.class) {
            return 1;
        }
        SVNFileRevision rev = (SVNFileRevision) o;
        long number = rev.getRevision();
        return myRevision == number ? 0 : myRevision > number ? 1 : -1;
    }

    /**
     * Tells whether this file revision is the result of a merge. 
     * @return      <span class="javakeyword">true</span> if this file revision is 
     *              the result of a merge
     * @since       1.2.0
     */
    public boolean isResultOfMerge() {
        return myIsResultOfMerge;
    }

}
