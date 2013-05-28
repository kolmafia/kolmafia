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

package org.tmatesoft.svn.core;

import java.util.Date;


/**
 * The <b>SVNCommitInfo</b> class represents information about a committed 
 * revision. Commit information includes:
 * <ol>
 * <li>a revision number;
 * <li>a datestamp when the revision was committed;
 * <li>the name of the revision author.
 * </ol>
 * In addition, this class provides an exception that, if a commit has failed,
 * has got a description of a failure reason.
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public class SVNCommitInfo {
    /**
     * Denotes an unsuccessful commit.
     */
    public static final SVNCommitInfo NULL = new SVNCommitInfo(-1, null, null, null);
    
    private long myNewRevision;
    private Date myDate;
    private String myAuthor;
    private SVNErrorMessage myErrorMessage;

    /**
     * 
     * Constructs an <b>SVNCommitInfo</b> object.
     * 
     * @param revision 		a revision number 
     * @param author 		the name of the author who committed the revision
     * @param date 			the datestamp when the revision was committed
     */
    public SVNCommitInfo(long revision, String author, Date date) {
        this(revision, author, date, null);        
    }

    /**
     * Constructs an <b>SVNCommitInfo</b> object.
     * 
     * @param revision      a revision number 
     * @param author        the name of the author who committed the revision
     * @param date          the datestamp when the revision was committed
     * @param error         if a commit failed - this is an error description 
     *                      containing details on the failure 
     */
    public SVNCommitInfo(long revision, String author, Date date, SVNErrorMessage error) {
        myNewRevision = revision;
        myAuthor = author;
        myDate = date;
        myErrorMessage = error;
    }

    /**
     * Gets the revision number the repository was committed to.
     * 
     * @return 	a revision number
     */
    public long getNewRevision() {
        return myNewRevision;
    }

    /**
     * Gets the name of the revision author
     * 
     * @return 	a revision author's name
     */
    public String getAuthor() {
        return myAuthor;
    }
    
    /**
     * Gets the datestamp when the revision was committed.
     *
     * @return 	a revision datestamp
     */
    public Date getDate() {
        return myDate;
    }
    
    /**
     * Gets an error message for a failed commit (if it 
     * has failed). This message will usually keep the entire 
     * stack trace of all the error messages as of results of errors
     * occurred. 
     * 
     * @return an error messages or <span class="javakeyword">null</span>. 
     */
    public SVNErrorMessage getErrorMessage() {
        return myErrorMessage;
    }

    /**
     * @return exception occurred
     * @deprecated use {@link #getErrorMessage() } instead
     */
    public SVNException getError() {
        if (myErrorMessage != null) {
            return new SVNException(getErrorMessage());
        }
        return null;
    }
    
    /**
     * Gives a string representation of this object.
     * 
     * @return a string describing commit info
     */
    public String toString() {
        if (this == NULL) {
            return "EMPTY COMMIT";
        } else if (myErrorMessage == null) {
            StringBuffer sb = new StringBuffer();
            sb.append("r");
            sb.append(myNewRevision);
            if (myAuthor != null) {
                sb.append(" by '");
                sb.append(myAuthor);
                sb.append("'");
            }
            if (myDate != null) {
                sb.append(" at ");
                sb.append(myDate);
            }
            return sb.toString(); 
        } else {         
            return myErrorMessage.getFullMessage();
        }
    }
}
