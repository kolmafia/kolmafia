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
 * The <b>SVNDirEntry</b> class is a representation of a versioned 
 * directory entry.
 * 
 * <p>
 * <b>SVNDirEntry</b> keeps an entry name, entry kind (is it a file or directory), 
 * file size (in case an entry is a file), the last changed revision, the date when 
 * the entry was last changed, the name of the author who last changed the entry, the
 * commit log message for the last changed revision. <b>SVNDirEntry</b> also knows 
 * if the entry has any properties. 
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 * @see 	ISVNDirEntryHandler
 */
public class SVNDirEntry implements Comparable {
    
    /**
     * It is sometimes convenient to indicate which parts of an entry
     * you are actually interested in, so that calculating and sending
     * the data corresponding to the other fields can be avoided.  These values
     * can be used for that purpose.
     */
    
    /**
     * Represents entry kind (if it is a file or a directory).
     * @since 1.2.0
     */
    public static final int DIRENT_KIND = 0x00001;
    
    /**
     * Represents file size (if the entry is a file).
     * @since 1.2.0
     */
    public static final int DIRENT_SIZE = 0x00002;
    
    /**
     * Contains the information whether the entry has any properties.
     * @since 1.2.0
     */
    public static final int DIRENT_HAS_PROPERTIES = 0x00004;
    
    /**
     * Represents the last revision when the entry was changed.
     * @since 1.2.0
     */
    public static final int DIRENT_CREATED_REVISION = 0x00008;
    
    /**
     * Represents the time of the last changed revision.
     * @since 1.2.0
     */
    public static final int DIRENT_TIME = 0x00010;
    
    /**
     * Represents the author of the last changed revision.
     * @since 1.2.0
     */
    public static final int DIRENT_LAST_AUTHOR = 0x00020;
    
    /**
     * Represents commit log message for the last changed revision.
     * @since 1.2.0
     */
    public static final int DIRENT_COMMIT_MESSAGE = 0x00040;
    
    /**
     * Represents a combination of all the entry fields.
     * @since 1.2.0
     */
    public static final int DIRENT_ALL = ~0;

    private String myName;
    private SVNNodeKind myKind;
    private long mySize;
    private boolean myHasProperties;
    private long myRevision;
    private Date myCreatedDate;
    private String myLastAuthor;
    private String myPath;
    private String myCommitMessage;
    private SVNLock myLock;
    private SVNURL myURL;
    private SVNURL myRepositoryRoot;

    /**
     * Constructs an instance of <b>SVNDirEntry</b>.
     * 
     * @param url               a url of this entry
     * @param repositoryRoot    a url of the root of repository this entry belongs to
     * @param name 			    an entry name
     * @param kind 			    the node kind for the entry
     * @param size 			    the entry size in bytes
     * @param hasProperties     <span class="javakeyword">true</span> if the
     *                          entry has properties, otherwise <span class="javakeyword">false</span>
     * @param revision          the last changed revision of the entry
     * @param createdDate 	    the date the entry was last changed
     * @param lastAuthor 	    the person who last changed the entry
     */
    public SVNDirEntry(SVNURL url, SVNURL repositoryRoot, String name, SVNNodeKind kind, long size,
            boolean hasProperties, long revision, Date createdDate, String lastAuthor) {
        myURL = url;
        myRepositoryRoot = repositoryRoot;
        myName = name;
        myKind = kind;
        mySize = size;
        myHasProperties = hasProperties;
        myRevision = revision;
        myCreatedDate = createdDate;
        myLastAuthor = lastAuthor;
    }

    /**
     * Constructs an instance of <b>SVNDirEntry</b>.
     *
     * @param url               a url of this entry
     * @param repositoryRoot    a url of the root of repository this entry belongs to
     * @param name              an entry name
     * @param kind              the node kind for the entry
     * @param size              the entry size in bytes
     * @param hasProperties     <span class="javakeyword">true</span> if the
     *                          entry has properties, otherwise <span class="javakeyword">false</span>
     * @param revision          the last changed revision of the entry
     * @param createdDate       the date the entry was last changed
     * @param lastAuthor        the person who last changed the entry
     * @param commitMessage     the log message of the last change commit
     */
    public SVNDirEntry(SVNURL url, SVNURL repositoryRoot, String name, SVNNodeKind kind, long size,
            boolean hasProperties, long revision, Date createdDate, String lastAuthor, String commitMessage) {
        myURL = url;
        myRepositoryRoot = repositoryRoot;
        myName = name;
        myKind = kind;
        mySize = size;
        myHasProperties = hasProperties;
        myRevision = revision;
        myCreatedDate = createdDate;
        myLastAuthor = lastAuthor;
        myCommitMessage = commitMessage;
    }


    /**
     * Returns the entry's URL.
     *
     * @return this entry's URL.
     */
    public SVNURL getURL() {
        return myURL;
    }

    /**
     * Returns the entry's repository root URL.
     *
     * @return the URL of repository root.
     */
    public SVNURL getRepositoryRoot() {
        return myRepositoryRoot;
    }
    
    /**
     * Gets the the directory entry name
     * 
     * @return 	the name of this entry
     */
    public String getName() {
        return myName;
    }
    
    /**
     * Returns the file size in bytes (if this entry is a file).
     *
     * @return  the size of this entry in bytes
     */
    public long getSize() {
        return mySize;
    }

    /**
     * Returns the file size in bytes (if this entry is a file).
     *
     * @deprecated use {@link #getSize()} instead
     * @return 	the size of this entry in bytes
     */
    public long size() {
        return getSize();
    }
    
    /**
     * Tells if the entry has any properties.
     * 
     * @return 	<span class="javakeyword">true</span> if has, 
     *          <span class="javakeyword">false</span> otherwise
     */
    public boolean hasProperties() {
        return myHasProperties;
    }
    
    /**
     * Returns the entry node kind.
     * 
     * @return  the node kind of this entry 
     * @see 	SVNNodeKind
     */
    public SVNNodeKind getKind() {
        return myKind;
    }
    
    /**
     * Returns the date the entry was last changed.
     * 
     * @return 	the datestamp when the entry was last changed
     */
    public Date getDate() {
        return myCreatedDate;
    }
    
    /**
     * Gets the last changed revision of this entry.
     * 
     * @return 	the revision of this entry when it was last changed 
     */
    public long getRevision() {
        return myRevision;
    }
    
    /**
     * Retrieves the name of the author who last changed this entry.
     * 
     * @return 	the last author's name.
     */
    public String getAuthor() {
        return myLastAuthor;
    }

    /**
     * Returns the entry's path relative to the target directory.
     * 
     * <p>
     * This method is guaranteed to return a non-<span class="javakeyword">null</span> path only 
     * for {@link org.tmatesoft.svn.core.wc.SVNLogClient#doList(java.io.File, org.tmatesoft.svn.core.wc.SVNRevision, org.tmatesoft.svn.core.wc.SVNRevision, boolean, SVNDepth, int, ISVNDirEntryHandler) list} 
     * operations. It always returns a path relative to the target location which a list 
     * operation is launched on. When listing a directory the relative path for the target 
     * directory itself is <code>""</code>, for its children - just their names, for deeper 
     * directories (when listing recursively) - paths relative to the target directory path.   
     * 
     * @return path relative to the target directory  
     */
    public String getRelativePath() {
        return myPath == null ? getName() : myPath;
    }
    
    /**
     * @return     repository path
     * @deprecated use {@link #getRelativePath()} instead.
     */
    public String getPath() {
        return getRelativePath();        
    }
    
    /**
     * Returns the commit log message for the revision of this entry.
     * 
     * <p/>
     * This is guaranteed to be non-<span class="javakeyword">null</span> only for the target entry 
     * returned by the {@link org.tmatesoft.svn.core.io.SVNRepository#getDir(String, long, boolean, java.util.Collection)}
     * method.
     * 
     * @return a commit log message
     */
    public String getCommitMessage() {
        return myCommitMessage;
    }
    
    /**
     * Gets the lock object for this entry (if it's locked).
     * 
     * @return a lock object or <span class="javakeyword">null</span>
     */
    public SVNLock getLock() {
        return myLock;
    }

    /**
     * This method is used by SVNKit internals and not intended for users (from an API point of view).
     *
     * @param name this entry's name
     */
    public void setName(String name) {
        this.myName = name;
    }

    /**
     * This method is used by SVNKit internals and not intended for users (from an API point of view).
     * 
     * @param path this entry's path
     */
    public void setRelativePath(String path) {
        myPath = path;
    }
    
    /**
     * This method is used by SVNKit internals and not intended for users (from an API point of view).
     * 
     * @param message a commit message
     */
    public void setCommitMessage(String message) {
        myCommitMessage = message;
    }

    /**
     * Sets the lock object for this entry (if it's locked).
     * 
     * @param lock a lock object
     */
    public void setLock(SVNLock lock) {
        myLock = lock;
    }
    
    /**
     * Retirns a string representation of this object. 
     * 
     * @return 	a string representation of this directory entry
     */
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("name=");
        result.append(myName);
        result.append(", kind=");
        result.append(myKind);
        result.append(", size=");
        result.append(mySize);
        result.append(", hasProps=");
        result.append(myHasProperties);
        result.append(", lastchangedrev=");
        result.append(myRevision);
        if (myLastAuthor != null) {
            result.append(", lastauthor=");
            result.append(myLastAuthor);
        }
        if (myCreatedDate != null) {
            result.append(", lastchangeddate=");
            result.append(myCreatedDate);
        }
        return result.toString();
    }
    
    /**
     * Compares this object with another one.
     * 
     * @param   o an object to compare with
     * @return    <ul>
     *            <li>-1 - if <code>o</code> is either <span class="javakeyword">null</span>,
     *            or is not an instance of <b>SVNDirEntry</b>, or this entry's URL is lexicographically 
     *            less than the name of <code>o</code>; 
     *            </li>
     *            <li>1 - if this entry's URL is lexicographically greater than the name of <code>o</code>;
     *            </li>
     *            <li>0 - if and only if <code>o</code> has got the same URL as this one has
     *            </li>
     *            </ul>
     */
    public int compareTo(Object o) {
        if (o == null || o.getClass() != SVNDirEntry.class) {
            return -1;
        }
        SVNNodeKind otherKind = ((SVNDirEntry) o).getKind();
        if (otherKind != getKind()) {
            return getKind().compareTo(otherKind);    
        }
        String otherURL = ((SVNDirEntry) o).getURL().toString();
        return myURL.toString().compareTo(otherURL);
    }
}