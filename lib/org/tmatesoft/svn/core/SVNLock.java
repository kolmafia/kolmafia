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
 * The <b>SVNLock</b> class represents a file lock. It holds 
 * information on a lock path, token, owner, comment, creation  
 * and expiration dates.
 * 
 * @version     1.3
 * @author      TMate Software Ltd.
 * @since 		1.2, SVN 1.2
 * @see         <a target="_top" href="http://svnkit.com/kb/examples/">Examples</a>
 */
public class SVNLock {
    
    private String myPath;
    private String myID;
    private String myOwner;
    private String myComment;
    private Date myCreationDate;
    private Date myExpirationDate;
    
    
    /**
     * <p>
     * Constructs an <b>SVNLock</b> object.
     * 
     * 
     * @param path 			a file path, relative to the repository root 
     * 						directory
     * @param id 			a string token identifying the lock
     * @param owner 		the owner of the lock 
     * @param comment 		a comment message for the lock (optional) 
     * @param created 		a datestamp when the lock was created
     * @param expires 		a datestamp when the lock expires, i.e. the file is 
     * 						unlocked (optional)
     */
    public SVNLock(String path, String id, String owner, String comment, Date created, Date expires) {
        myPath = path;
        myID = id;
        myOwner = owner;
        myComment = comment;
        myCreationDate = created;
        myExpirationDate = expires;
    }
    
    /**
     * Gets the lock comment.
     * 
     * @return  a lock comment message 
     */
    public String getComment() {
        return myComment;
    }
    
    
    /**
     * Gets the creation datestamp of this lock.
     * 
     * @return 		a datestamp representing the moment in 
     *              time when this lock was created
     */
    public Date getCreationDate() {
        return myCreationDate;
    }
    
    
    /**
     * Gets the expiration datestamp of this lock.
     * 
     * @return 		a datestamp representing the moment in time 
     *              when the this lock expires
     */
    public Date getExpirationDate() {
        return myExpirationDate;
    }
    
    
    /**
     * Gets the lock token. 
     * 
     * @return  a unique string identifying this lock
     */
    public String getID() {
        return myID;
    }
    
    /**
     * Gets the lock owner.
     * 
     * @return	the owner of this lock 
     */
    public String getOwner() {
        return myOwner;
    }
    
    /**
     * Gets the path of the file for which this lock was created. 
     * The path is relative to the repository root directory.
     * 
     * @return	the path of the locked file
     */
    public String getPath() {
        return myPath;
    }
    
    /**
     * Returns a string representation of this object.
     *  
     * @return	a string representation of this lock object
     */
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("path=");
        result.append(myPath);
        result.append(", token=");
        result.append(myID);
        result.append(", owner=");
        result.append(myOwner);
        if (myComment != null) {
            result.append(", comment=");
            result.append(myComment);
        }
        result.append(", created=");
        result.append(myCreationDate);
        if (myExpirationDate != null) {
            result.append(", expires=");
            result.append(myExpirationDate);
        }        
        return result.toString();
    }
}
