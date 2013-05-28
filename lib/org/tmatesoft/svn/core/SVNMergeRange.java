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
package org.tmatesoft.svn.core;


/**
 * The <b>SVNMergeRange</b> class represents a range of merged revisions.
 *  
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public class SVNMergeRange implements Comparable {
    private long myStartRevision;
    private long myEndRevision;
    private boolean myIsInheritable; 
    /**
     * Constructs a new <code>SVNMergeRange</code> object.
     * 
     * @param startRevision   start revision of this merge range
     * @param endRevision     end revision of this merge range
     * @param isInheritable   whether this range is inheritable or not
     */
    public SVNMergeRange(long startRevision, long endRevision, boolean isInheritable) {
        myStartRevision = startRevision;
        myEndRevision = endRevision;
        myIsInheritable = isInheritable;
    }
    
    /**
     * Returns the end revision of this merge range.
     * @return end revision
     */
    public long getEndRevision() {
        return myEndRevision;
    }
    
    /**
     * Returns the start revision of this merge range.
     * @return start revision
     */
    public long getStartRevision() {
        return myStartRevision;
    }
    
    /**
     * Sets the end revision of this merge range. This method is used by <code>SVNKit</code>
     * internals and is not intended for API users.
     * 
     * @param endRevision merge range end revision
     */
    public void setEndRevision(long endRevision) {
        myEndRevision = endRevision;
    }

    /**
     * Sets the start revision of this merge range. This method is used by <code>SVNKit</code>
     * internals and is not intended for API users.
     * 
     * @param startRevision merge range start revision
     */
    public void setStartRevision(long startRevision) {
        myStartRevision = startRevision;
    }

    /**
     * Compares this object to another one.
     * 
     * <p/>
     * Note: merge range {@link #isInheritable() inheritance} is not taken into account when comparing two 
     * <code>SVNMergeRange</code> objects.
     * 
     * @param o    object to compare to 
     * @return     <code>0</code> if <code>o</code> is exactly this object or 
     *             <code>o.getStartRevision() == this.getStartRevision() && o.getEndRevision() == this.getEndRevision()</code>; 
     *             <code>-1</code> if either <code>this.getStartRevision() < o.getStartRevision()</code> or 
     *             <code>this.getStartRevision() == o.getStartRevision() && this.getEndRevision() < o.getEndRevision()</code>;
     *             <code>1</code> if either <code>this.getStartRevision() > o.getStartRevision()</code> or 
     *             <code>this.getStartRevision() == o.getStartRevision() && this.getEndRevision() > o.getEndRevision()</code>
     */
    public int compareTo(Object o) {
        if (o == this) {
            return 0;
        }
        if (o == null || o.getClass() != SVNMergeRange.class) {
            return 1;
        }
        
        SVNMergeRange range = (SVNMergeRange) o;
        if (range.myStartRevision == myStartRevision && 
            range.myEndRevision == myEndRevision) {
            return 0;
        } else if (range.myStartRevision == myStartRevision) {
            return myEndRevision < range.myEndRevision ? -1 : 1;
        }
        return myStartRevision < range.myStartRevision ? -1 : 1;
    }

    /**
     * Says if this object is equal to <code>obj</code> or not.
     * 
     * <p/>
     * Identical to <code>this.compareTo(obj) == 0</code>. 
     * 
     * @param  obj  object ot compare to 
     * @return      <span class="javakeyword">true</span> if equal; otherwise <span class="javakeyword">false</span> 
     */
    public boolean equals(Object obj) {
        return this.compareTo(obj) == 0;
    }

    /**
     * Combines this merge range and the given <code>range</code> into a single one.
     * 
     * <p/>
     * Combining may only occur if {@link #canCombine(SVNMergeRange, boolean)} returns <span class="javakeyword">true</span> 
     * for the specified parameters.
     * 
     * <p/>
     * Note: combining changes the state of this object. 
     *
     * @param  range                 range to combine with this range  
     * @param  considerInheritance   whether inheritance information should be taken into account
     * @return                       if combining occurs, returns this object which is now a combination 
     *                               of the two ranges; otherwise returns <code>range</code>                      
     */
    public SVNMergeRange combine(SVNMergeRange range, boolean considerInheritance) {
        if (canCombine(range, considerInheritance)) {
            myStartRevision = Math.min(myStartRevision, range.getStartRevision());
            myEndRevision = Math.max(myEndRevision, range.getEndRevision());
            myIsInheritable = myIsInheritable || range.myIsInheritable;
            return this; 
        }
        return range;
    }
    
    /**
     * Tells whether this range can me combined with the given <code>range</code> depending on 
     * inheritance or not.
     * 
     * <p/> 
     * Combining may occur only when two ranges intersect each other what may be checked by the following case: 
     * <code>range1.getStartRevision() &lt;= range2.getEndRevision() && range2.getStartRevision() &lt;= range1.getEndRevision()</code>.
     * If this condition evaluates to <span class="javakeyword">true</span>, then the ranges intersect.
     *
     * @param   range                 range to combine with this range 
     * @param   considerInheritance   whether inheritance information should be taken into account or not 
     * @return                        <span class="javakeyword">true</span> when the ranges intersect, 
     *                                <code>considerInheritance</code> is not set, or if set, both ranges 
     *                                have the same inheritance; otherwise <span class="javakeyword">false</span>
     */
    public boolean canCombine(SVNMergeRange range, boolean considerInheritance) {
        if (range != null && myStartRevision <= range.getEndRevision() &&
            range.getStartRevision() <= myEndRevision) {
            if (!considerInheritance || (considerInheritance && myIsInheritable == range.myIsInheritable)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Says whether this range contains the specified <code>range</code> depending on inheritance or not.
     * 
     * @param  range                  range to check 
     * @param  considerInheritance    whether inheritance information should be taken into account or not 
     * @return                        <span class="javakeyword">true</span> if this range contains the 
     *                                specified <code>range</code>; otherwise <span class="javakeyword">false</span>                 
     */
    public boolean contains(SVNMergeRange range, boolean considerInheritance) {
        return range != null && myStartRevision <= range.myStartRevision && 
        range.myEndRevision <= myEndRevision && 
        (!considerInheritance || (!myIsInheritable == !range.myIsInheritable));
    }
    
    /**
     * Says whether this range intersects the specified <code>range</code> depending on inheritance or not.
     * 
     * @param  range                  range to check 
     * @param  considerInheritance    whether inheritance information should be taken into account or not 
     * @return                        <span class="javakeyword">true</span> if this range intersects the 
     *                                specified <code>range</code>; otherwise <span class="javakeyword">false</span>                 
     */
    public boolean intersects(SVNMergeRange range, boolean considerInheritance) {
        return range != null && myStartRevision + 1 <= range.myEndRevision && 
        range.myStartRevision + 1 <= myEndRevision && 
        (!considerInheritance || (!myIsInheritable == !range.myIsInheritable));
    }
    
    /**
     * Swaps the start revision and the end revision of this merge range object.
     * 
     * @return this object itself 
     */
    public SVNMergeRange swapEndPoints() {
        long tmp = myStartRevision;
        myStartRevision = myEndRevision;
        myEndRevision = tmp;
        return this;
    }
   
    /**
     * Tells whether this merge range should be inherited by treewise descendants of the path to which the range applies. 
     * 
     * @return <span class="javakeyword">true</span> if inheritable; otherwise <span class="javakeyword">false</span>
     */
    public boolean isInheritable() {
        return myIsInheritable;
    }
    
    /**
     * Sets whether this merge range is inheritable or not.
     * This method is used by <code>SVNKit</code> internals and is not intended for API users.
     * 
     * @param isInheritable whether this range is inheritable or not
     */
    public void setInheritable(boolean isInheritable) {
        myIsInheritable = isInheritable;
    }

    /**
     * Makes an exact copy of this object.
     * 
     * @return  exact copy of this object
     */
    public SVNMergeRange dup() {
        return new SVNMergeRange(myStartRevision, myEndRevision, myIsInheritable);
    }

    /**
     * Return a string representation of this object.
     * @return this object as a string 
     */
    public String toString() {
        String output = "";
        if (myStartRevision == myEndRevision - 1) {
            output += String.valueOf(myEndRevision);
        } else {
            output += String.valueOf(myStartRevision + 1) + "-" + String.valueOf(myEndRevision);
        }
        if (!isInheritable()) {
            output += SVNMergeRangeList.MERGE_INFO_NONINHERITABLE_STRING;
        }
        return output;
    }

}
