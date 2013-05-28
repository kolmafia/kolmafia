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

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.internal.util.SVNDate;


/**
 * The <b>SVNLogEntry</b> class encapsulates such per revision information as: 
 * a revision number, the datestamp when the revision was committed, the author 
 * of the revision, a commit log message and all paths changed in that revision. 
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 * @see 	SVNLogEntryPath
 * @see     ISVNLogEntryHandler
 * @see     <a target="_top" href="http://svnkit.com/kb/examples/">Examples</a>
 */
public class SVNLogEntry implements Serializable {
    
    private static final long serialVersionUID = 4845L;

    /**
     * This is a log entry children stop marker use by the <code>SVNKit</code> internals. Users should not
     * compare the log entry received in their {@link ISVNLogEntryHandler} implementations with this one.
     * Instead, to find the end of the log entry children sequence they should check the log entry's revision 
     * for validity (i.e. that it is not less than <code>0</code>).
     * 
     * @since 1.2.0 
     */
    public static SVNLogEntry EMPTY_ENTRY = new SVNLogEntry(Collections.EMPTY_MAP, 
            SVNRepository.INVALID_REVISION, null, false);

    private long myRevision;
    private Map myChangedPaths;
    private SVNProperties myRevisionProperties;
    private boolean myHasChildren;
    private boolean myIsSubtractiveMerge;
    private boolean myIsNonInheritable;
    
    /**
     * Constructs an <b>SVNLogEntry</b> object. 
     * 
     * @param changedPaths 	a map collection which keys are
     * 						all the paths that were changed in   
     *                      <code>revision</code>, and values are 
     * 						<b>SVNLogEntryPath</b> representation objects
     * @param revision 		a revision number
     * @param author 		the author of <code>revision</code>
     * @param date 			the datestamp when the revision was committed
     * @param message 		an commit log message for <code>revision</code>
     * @see 				SVNLogEntryPath
     */
    public SVNLogEntry(Map changedPaths, long revision, String author, Date date, String message) {
        myRevision = revision;
        myRevisionProperties = new SVNProperties();
        myChangedPaths = changedPaths;
        if (author != null) {
            myRevisionProperties.put(SVNRevisionProperty.AUTHOR, author);    
        }
        if (date != null) {
            myRevisionProperties.put(SVNRevisionProperty.DATE, SVNDate.formatDate(date));
        }
        if (message != null) {
            myRevisionProperties.put(SVNRevisionProperty.LOG, message);    
        }
    }

    /**
     * Constructs an <b>SVNLogEntry</b> object. 
     * 
     * @param changedPaths        a map collection which keys are
     *                            all the paths that were changed in   
     *                            <code>revision</code>, and values are 
     *                            <b>SVNLogEntryPath</b> representation objects
     * @param revision            a revision number
     * @param revisionProperties  revision properties
     * @param hasChildren         whether this entry has children or not
     * @since 1.2.0
     */
    public SVNLogEntry(Map changedPaths, long revision, SVNProperties revisionProperties, boolean hasChildren) {
        myRevision = revision;
        myChangedPaths = changedPaths;
        myRevisionProperties = revisionProperties != null ? revisionProperties : new SVNProperties();
        myHasChildren = hasChildren;
    }

    /**
     * Sets wheteher this log entry has children entries or not.
     * 
     * <p/>
     * Note: this method is not intended for API users.
     *  
     * @param hasChildren  whether this entry has has children or not 
     * @see                #hasChildren()
     * @since              1.2.0
     */
    public void setHasChildren(boolean hasChildren) {
        myHasChildren = hasChildren;
    }

    /**
     * Gets a map containing all the paths that were changed in the 
     * revision that this object represents.
     * 
     * @return 		a <code>String</code> to {@link SVNLogEntryPath} map which keys are all the paths that 
     *              were changed in the revision and values represent information about each changed path
     * 
     */
    public Map<String, SVNLogEntryPath> getChangedPaths() {
        return myChangedPaths;
    }
    
    /**
     * Returns the author of the revision that this object represents.
     * 
     * @return the author of the revision
     */
    public String getAuthor() {
        return myRevisionProperties.getStringValue(SVNRevisionProperty.AUTHOR);
    }
    
    /**
     * Gets the datestamp when the revision was committed.
     * 
     * @return 	   the moment in time when the revision was committed
     */
    public Date getDate() {
        String date = myRevisionProperties.getStringValue(SVNRevisionProperty.DATE);
        return date == null ? null : SVNDate.parseDate(date);
    }
    
    /**
     * Gets the log message attached to the revision.
     * 
     * @return 		the commit log message
     */
    public String getMessage() {
        return myRevisionProperties.getStringValue(SVNRevisionProperty.LOG);
    }
    
    /** 
     * Returns the requested revision properties, which may be <span class="javakeyword">null</span> if it
     * would contain no revision properties. 
     * 
     * @return   revision properties   
     * @since    1.2.0 
     */
    public SVNProperties getRevisionProperties() {
        return myRevisionProperties;
    }
    
    /**
     * Gets the number of the revision that this object represents.
     * 
     * @return 	a revision number 
     */
    public long getRevision() {
        return myRevision;
    }

    /**
     * Calculates and returns a hash code for this object.
     * 
     * @return a hash code
     */
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (int) (myRevision ^ (myRevision >>> 32));
        result = PRIME * result + ((myChangedPaths == null) ? 0 : myChangedPaths.hashCode());
        result = PRIME * result + ((myRevisionProperties == null) ? 0 : myRevisionProperties.hashCode());
        return result;
    }
    
    /**
     * Compares this object with another one.
     * 
     * @param  obj  an object to compare with
     * @return      <span class="javakeyword">true</span> 
     *              if this object is the same as the <code>obj</code> 
     *              argument
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SVNLogEntry other = (SVNLogEntry) obj;
        return myRevision == other.myRevision &&
            compare(myRevisionProperties, other.myRevisionProperties) &&
            compare(myChangedPaths, other.myChangedPaths);
    }
    
    /**
     * Gives a string representation of this oobject.
     * 
     * @return a string representing this object
     */
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append(myRevision);
        for (Iterator propNames = myRevisionProperties.nameSet().iterator(); propNames.hasNext();) {
            String propName = (String) propNames.next();
            Object propVal = myRevisionProperties.getSVNPropertyValue(propName);
            result.append('\n');
            result.append(propName);
            result.append('=');
            result.append(propVal);
        }
        if (myChangedPaths != null && !myChangedPaths.isEmpty()) {
            for (Iterator paths = myChangedPaths.values().iterator(); paths.hasNext();) {
                result.append('\n');
                SVNLogEntryPath path = (SVNLogEntryPath) paths.next();
                result.append(path.toString());
            }
        }
        return result.toString();
    }

    /**
     * Tells whether or not this log entry has children.
     * 
     * <p/>
     * When a log operation requests additional merge information, extra log entries may be returned as a 
     * result of this entry. The new entries, are considered children of the original entry, and will follow it. 
     * When the HAS_CHILDREN flag is set, the receiver should increment its stack
     * depth, and wait until an entry is provided with {@link SVNRepository#INVALID_REVISION} which
     * indicates the end of the children.
     * 
     * <p/>
     * For log operations which do not request additional merge information, the HAS_CHILDREN flag is always 
     * <span class="javakeyword">false</span>.
     * 
     * <p/>
     * Also for more information see:
     * <a target="_top" href="http://subversion.tigris.org/merge-tracking/design.html#commutative-reporting">Subversion documentation</a>
     * 
     * @return  <span class="javakeyword">true</span> if this log entry has children entries due to 
     *          merge-tracking information       
     * @since   1.2.0, new in Subversion 1.5.0
     */
    
    public boolean hasChildren() {
        return myHasChildren;
    }

    /**
     * Compares two objects.
     * 
     * @param o1 the first object to compare
     * @param o2 the second object to compare
     * @return   <span class="javakeyword">true</span> if either both
     *           <code>o1</code> and <code>o2</code> are <span class="javakeyword">null</span> 
     *           or <code>o1.equals(o2)</code> returns <span class="javakeyword">true</span>  
     */
    static boolean compare(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        } 
        return o1.equals(o2);
    }

    public void setSubtractiveMerge(boolean substractiveMerge) {
        myIsSubtractiveMerge = substractiveMerge;
    }
    
    public boolean isSubtractiveMerge() {
        return myIsSubtractiveMerge;
    }


    public void setNonInheriable(boolean nonInheritable) {
        myIsNonInheritable = nonInheritable;
    }
    
    public boolean isNonInheritable() {
        return myIsNonInheritable;
    }
}
