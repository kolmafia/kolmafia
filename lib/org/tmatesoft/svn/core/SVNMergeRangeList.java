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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * The <b>SVNMergeRangeList</b> represents an array of merge ranges applied to a single target. 
 * Provides addition functionality to operate with merge range lists.
 * 
 * @version 1.3 
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public class SVNMergeRangeList {
    /**
     * A string that is used in mergeinfo to mark the mergeinfo as being non-inheritable. 
     */
    public static String MERGE_INFO_NONINHERITABLE_STRING = "*";
    
    private SVNMergeRange[] myRanges;
    
    /**
     * Creates a new merge range list initializing it with a single merge range which parameters are passed 
     * to this constructor.
     * 
     * @param start          merge range start revision  
     * @param end            merge range end revision
     * @param inheritable    inheritance information
     */
    public SVNMergeRangeList(long start, long end, boolean inheritable) {
        this(new SVNMergeRange(start, end, inheritable));
    }

    /**
     * Creates a new merge range list initializing it with the specified single merge range.
     * 
     * @param range merge range
     */
    public SVNMergeRangeList(SVNMergeRange range) {
    	this(new SVNMergeRange[] { range });
    }

    /**
     * Creates a new merge range list initializing it with an array of merge ranges.
     * 
     * <p/>
     * Note: <code>ranges</code> are not copied to a separate array but stored immediately, as is.
     * 
     * @param ranges      merge ranges array 
     */
    
    public SVNMergeRangeList(SVNMergeRange[] ranges) {
        myRanges = ranges == null ? new SVNMergeRange[0] : ranges;
    }
    
    /**
     * Replaces the array of {@link SVNMergeRange} objects backed by this object 
     * with a new one.
     * 
     * </p>
     * This method was introduced because of purposes of convenience. Use this method 
     * with care as it changes the internal state of this <code>SVNMergeRangeList</code> 
     * object.
     * 
     * @param ranges  new merge ranges array
     * @since 1.2.2        
     */
    public void setRanges(SVNMergeRange[] ranges) {
        myRanges = ranges;
    }
    
    public void setInheritable(boolean inheritable) {
        if (myRanges != null) {
            for (int i = 0; i < myRanges.length; i++) {
                myRanges[i].setInheritable(inheritable);                
            }
        }
    }
    
    /**
     * Returns an array of {@link SVNMergeRange} ranges backed by this merge range list object.
     * 
     * <p/>
     * Note: this method does not make a copy of the original array, instead it returns the original array 
     * itself. If you want a safe copy of merge ranges, use {@link #getRangesAsList()} instead.
     * 
     * <p/>
     * Note: merge ranges returned in the array are not copied. 
     * 
     * @return array of merge ranges 
     */
    public SVNMergeRange[] getRanges() {
        return myRanges;
    }
    
    /**
     * Returns a list of merge ranges backed by this merge range list.
     * 
     * <p/> 
     * Note: ranges themselves are not copied but placed in the list as is.
     * 
     * @return a new list instance containing all of the ranges stored in this merge range list 
     */
    public List<SVNMergeRange> getRangesAsList() {
    	List<SVNMergeRange> list = new ArrayList<SVNMergeRange>();
    	for (int i = 0; i < myRanges.length; i++) {
			SVNMergeRange range = myRanges[i];
			list.add(range);
		}
    	return list;
    }
    
    /**
     * Appends a new merge range to the end of the ranges list.
     * A new {@link SVNMergeRange} is created used the parameters passed to this method.
     * 
     * @param start            merge range start revision 
     * @param end              merge range end revision
     * @param inheritable      inheritance information
     */
    public void pushRange(long start, long end, boolean inheritable) {
        SVNMergeRange[] ranges = new SVNMergeRange[myRanges.length + 1];
        ranges[ranges.length - 1] = new SVNMergeRange(start, end, inheritable);
        System.arraycopy(myRanges, 0, ranges, 0, myRanges.length);
        myRanges = ranges;
    }
    
    /**
     * Returns number of merge ranges stored in this merge range list.
     * 
     * @return number of merge ranges 
     */
    public int getSize() {
        return myRanges.length;
    }
    
    /**
     * Checks whether this merge range list has no merge ranges.
     * 
     * @return  <span class="javakeyword">true</span> if this merge range list is empty;
     *          otherwise <span class="javakeyword">false</span>
     */
    public boolean isEmpty() {
        return myRanges.length == 0;
    }
    
    /**
     * Makes a copy of this merge range list. All merge ranges stored in this list will be copied 
     * to a new array which will be covered into a new <code>SVNMergeRangeList</code> instance.
     * 
     * @return copy of this merge range list  
     */
    public SVNMergeRangeList dup() {
        SVNMergeRange[] ranges = new SVNMergeRange[myRanges.length];
        for (int i = 0; i < myRanges.length; i++) {
            SVNMergeRange range = myRanges[i];
            ranges[i] = range.dup();
        }
        return new SVNMergeRangeList(ranges);
    }
    
    /** 
     * Merges two range lists placing the results into a new {@link SVNMergeRangeList} object. 
     * Either range list may be empty.
     *
     * <p/>
     * When intersecting range lists are merged, the inheritability of the resulting {@link SVNMergeRange} 
     * depends on the inheritability of the operands. If two non-inheritable ranges are merged the result is 
     * always non-inheritable, in all other cases the resulting range is inheritable.
     *
     * <p/>
     * Note: range lists must be sorted in ascending order. The return range list is guaranteed to remain
     * in sorted order and be compacted to the minimal number of ranges needed to represent the merged result.
     *
     * <p/>
     * Note: this method does not change the state of this object. Instead it produces a result in a new object.
     * 
     * @param  rangeList       range list to merge with 
     * @return                 resultant range list                 
     * @throws SVNException 
     */
    public SVNMergeRangeList merge(SVNMergeRangeList rangeList) throws SVNException {
        int i = 0;
        int j = 0;
        SVNMergeRange lastRange = null;
        Collection resultRanges = new LinkedList();
        while (i < myRanges.length && j < rangeList.myRanges.length) {
            SVNMergeRange range1 = myRanges[i];
            SVNMergeRange range2 = rangeList.myRanges[j];
            int res = range1.compareTo(range2);
            if (res == 0) {
                if (range1.isInheritable() || range2.isInheritable()) {
                    range1.setInheritable(true);
                }

                lastRange = combineWithLastRange(resultRanges, lastRange, range1, true, false);
                i++;
                j++;
            } else if (res < 0) {
                lastRange = combineWithLastRange(resultRanges, lastRange, range1, true, false);
                i++;
            } else { 
                lastRange = combineWithLastRange(resultRanges, lastRange, range2, true, false);
                j++;
            }
        }
        
        SVNErrorManager.assertionFailure(i >= myRanges.length || j >= rangeList.myRanges.length, "expected to reach the end of at least one range list", SVNLogType.DEFAULT);
        
        for (; i < myRanges.length; i++) {
            SVNMergeRange range = myRanges[i];
            lastRange = combineWithLastRange(resultRanges, lastRange, range, true, false);
        }

        for (; j < rangeList.myRanges.length; j++) {
            SVNMergeRange range = rangeList.myRanges[j];
            lastRange = combineWithLastRange(resultRanges, lastRange, range, true, false);
        }
        return SVNMergeRangeList.fromCollection(resultRanges);
    }
    
    /**
     * Returns a string representation of this object.
     * 
     * @return this object as a string
     */
    public String toString() {
        String output = "";
        for (int i = 0; i < myRanges.length; i++) {
            SVNMergeRange range = myRanges[i];
            output += range.toString();
            if (i < myRanges.length - 1) {
                output += ',';
            }
        }
        return output;
    }

    /** 
     * Removes <code>eraserRangeList</code> (the subtrahend) from this range list (the
     * minuend), and places the resulting difference into a new <code>SVNMergeRangeList</code> object.
     * 
     * @param  eraserRangeList          ranges to remove from this range list 
     * @param  considerInheritance      whether inheritance information should be taken into account
     * @return                          the resultant difference 
     */
    public SVNMergeRangeList diff(SVNMergeRangeList eraserRangeList, boolean considerInheritance) {
        return removeOrIntersect(eraserRangeList, true, considerInheritance);
    }
    
    /** 
     * Finds the intersection of this range list and <code>rangeList</code> and places the result into 
     * a new <code>SVNMergeRangeList</code> object.
     * 
     * @param  rangeList               range list to intersect with
     * @param  considerInheritance     whether inheritance information should be taken into account
     * @return                         the result of intersection
     */
    public SVNMergeRangeList intersect(SVNMergeRangeList rangeList, boolean considerInheritance) {
        return removeOrIntersect(rangeList, false, considerInheritance);
    }
    
    /**
     * Runs through all merge ranges in this object and says, whether the specified <code>revision</code>
     * falls between start and end revision of any of those ranges.
     * 
     * @param revision revision to find in ranges 
     * @return <span class="javakeyword">true</span> if one of the ranges in this list includes the 
     *         specified <code>revision</code>    
     */
    public boolean includes(long revision) {
        for (int i = 0; i < myRanges.length; i++) {
            SVNMergeRange range = myRanges[i];
            if (revision > range.getStartRevision() && revision <= range.getEndRevision()) {
                return true;
            }
        }
        return false;
    }
    
    /** 
     * Reverses this range list, and the start and end fields of each
     * range in this range list, in place.
     * 
     * @return this object itself 
     */
    public SVNMergeRangeList reverse() {
        if (myRanges.length != 0) {
            for (int i = 0; i < myRanges.length/2; i++) {
                int swapInex =  myRanges.length - i - 1;
                SVNMergeRange range = myRanges[i];
                myRanges[i] = myRanges[swapInex].swapEndPoints();
                myRanges[swapInex] = range.swapEndPoints();
            }
            
            if (myRanges.length % 2 == 1) {
                myRanges[myRanges.length/2].swapEndPoints();
            }
        }
        return this;
    }
    
    /** 
     * Returns a sublist of this range list which excludes all non-inheritable merge ranges. 
     * If <code>startRev</code> and <code>endRev</code> are 
     * {@link org.tmatesoft.svn.core.wc.SVNRevision#isValidRevisionNumber(long) valid} 
     * revisions and <code>startRev</code> is less than or equal to <code>endRev</code>, then excludes only 
     * the non-inheritable revision ranges that intersect inclusively with the range
     * defined by <code>startRev</code> and <code>endRev</code>. If this range list contains no elements, 
     * returns an empty array. 
     * 
     * @param startRev     start revision 
     * @param endRev       end revision
     * @return             a new <code>SVNMergeRangeList</code> object with only inheritable ranges from 
     *                     this range list
     */
    public SVNMergeRangeList getInheritableRangeList(long startRev, long endRev) {
        return getInheritableRangeList(startRev, endRev, true);
    }

    public SVNMergeRangeList getInheritableRangeList(long startRev, long endRev, boolean inheritable) {
        LinkedList inheritableRanges = new LinkedList();
        if (myRanges.length > 0) {
            if (!SVNRevision.isValidRevisionNumber(startRev) ||
                !SVNRevision.isValidRevisionNumber(endRev) ||
                endRev < startRev) {
                for (int i = 0; i < myRanges.length; i++) {
                    SVNMergeRange range = myRanges[i];
                    if (range.isInheritable() == inheritable) {
                        SVNMergeRange inheritableRange = new SVNMergeRange(range.getStartRevision(),
                                                                           range.getEndRevision(), 
                                                                           true);
                        inheritableRanges.add(inheritableRange);
                    }
                }
            } else {
                SVNMergeRange range = new SVNMergeRange(startRev, endRev, false);
                SVNMergeRangeList boundRangeList = new SVNMergeRangeList(range);
                return diff(boundRangeList, true);
            }
        }
        SVNMergeRange[] ranges = (SVNMergeRange[]) inheritableRanges.toArray(new SVNMergeRange[inheritableRanges.size()]);
        return new SVNMergeRangeList(ranges);
    }
    
    /**
     * Creates a new <code>SVNMergeRangeList</code> from a collection of 
     * {@link SVNMergeRange merge ranges}.
     * 
     * @param  mergeRanges merge ranges collection 
     * @return             merge range list containing all the ranges from <code>mergeRanges</code>
     */
    public static SVNMergeRangeList fromCollection(Collection mergeRanges) {
        return new SVNMergeRangeList((SVNMergeRange[]) 
                mergeRanges.toArray(new SVNMergeRange[mergeRanges.size()]));
    }

    private SVNMergeRangeList removeOrIntersect(SVNMergeRangeList eraserRangeList, boolean remove, boolean considerInheritance) {
        Collection ranges = new LinkedList();
        SVNMergeRange lastRange = null;
        SVNMergeRange range1 = null;
        int i = 0;
        int j = 0;
        int lastInd = -1;
        SVNMergeRange whiteBoardElement = new SVNMergeRange(-1, -1, false);
        while (i < myRanges.length && j < eraserRangeList.myRanges.length) {
            SVNMergeRange range2 = eraserRangeList.myRanges[j];
            if (i != lastInd) {
                SVNMergeRange tmpRange = myRanges[i]; 
                range1 = tmpRange.dup();
                whiteBoardElement = range1;
                lastInd = i;
            }
            
            if (range2.contains(range1, considerInheritance)) {
                if (!remove) {
                    lastRange = combineWithLastRange(ranges, lastRange, range1, true, considerInheritance);
                }
                
                i++;
                
                if (range1.getStartRevision() == range2.getStartRevision() && 
                        range1.getEndRevision() == range2.getEndRevision()) {
                    j++;
                }
            } else if (range2.intersects(range1, considerInheritance)) {
                if (range1.getStartRevision() < range2.getStartRevision()) {
                    SVNMergeRange tmpRange = null;
                    if (remove) {
                        tmpRange = new SVNMergeRange(range1.getStartRevision(), range2.getStartRevision(),
                                range1.isInheritable());    
                    } else {
                        tmpRange = new SVNMergeRange(range2.getStartRevision(),
                                Math.min(range1.getEndRevision(), range2.getEndRevision()), range1.isInheritable());                        
                    }

                    lastRange = combineWithLastRange(ranges, lastRange, tmpRange, true, considerInheritance);
                }
                
                if (range1.getEndRevision() > range2.getEndRevision()) {
                    if (!remove) {
                        SVNMergeRange tmpRange = new SVNMergeRange(Math.max(range1.getStartRevision(), 
                                range2.getStartRevision()), range2.getEndRevision(), range1.isInheritable());
                        lastRange = combineWithLastRange(ranges, lastRange, tmpRange, true, considerInheritance);
                    }
                    whiteBoardElement.setStartRevision(range2.getEndRevision());
                    whiteBoardElement.setEndRevision(range1.getEndRevision());
                } else {
                    i++;
                }
            } else {
                if (range2.compareTo(range1) < 0) {
                    j++;
                } else {
                    if (remove && !(lastRange != null && lastRange.canCombine(range1, considerInheritance))) {
                        lastRange = range1.dup();
                        ranges.add(lastRange);
                    }
                    i++;
                }
            }
        }
        
        if (remove) {
            if (i == lastInd && i < myRanges.length) {
                lastRange = combineWithLastRange(ranges, lastRange, whiteBoardElement, true, considerInheritance);
                i++;
            }
            for (; i < myRanges.length; i++) {
                SVNMergeRange range = myRanges[i];
                lastRange = combineWithLastRange(ranges, lastRange, range, true, considerInheritance);
            }
        }
        return SVNMergeRangeList.fromCollection(ranges);
    }

    public SVNMergeRangeList remove(SVNMergeRangeList remove, boolean considerInheritance) {
        return removeOrIntersect(remove, true, considerInheritance);
    }
    

    private SVNMergeRange combineWithLastRange(Collection rangeList, SVNMergeRange lastRange, SVNMergeRange mRange, boolean dupMRange, boolean considerInheritance) {
        SVNMergeRange pushedMRange1 = null;
        SVNMergeRange pushedMRange2 = null;
        boolean rangesIntersect = false;
        boolean rangesHaveSameInheritance = false;
        if (lastRange != null) {
            if (lastRange.getStartRevision() <= mRange.getEndRevision() && mRange.getStartRevision() <= lastRange.getEndRevision()) {
                rangesIntersect = true;
            }
            if (lastRange.isInheritable() == mRange.isInheritable()) {
                rangesHaveSameInheritance = true;
            }
        }
        
        if (lastRange == null || !rangesIntersect || (!rangesHaveSameInheritance && considerInheritance)) {
            if (dupMRange) {
                pushedMRange1 = mRange.dup();
            } else {
                pushedMRange1 = mRange;
            }
        } else {
            long tmpRevision = -1;
            if (rangesHaveSameInheritance) {
                lastRange.setStartRevision(Math.min(lastRange.getStartRevision(), mRange.getStartRevision()));
                lastRange.setEndRevision(Math.max(lastRange.getEndRevision(), mRange.getEndRevision()));
                lastRange.setInheritable(lastRange.isInheritable() || mRange.isInheritable());
            } else {
                if (lastRange.getStartRevision() == mRange.getStartRevision()) {
                    if (lastRange.getEndRevision() == mRange.getEndRevision()) {
                        lastRange.setInheritable(true);
                    } else  if (lastRange.getEndRevision() > mRange.getEndRevision()) {
                        if (!lastRange.isInheritable()) {
                            tmpRevision = lastRange.getEndRevision();
                            lastRange.setEndRevision(mRange.getEndRevision());
                            lastRange.setInheritable(true);
                            if (dupMRange) {
                                pushedMRange1 = mRange.dup();
                            } else {
                                pushedMRange1 = mRange;
                            }
                            pushedMRange1.setEndRevision(tmpRevision);
                            lastRange = pushedMRange1;
                        }
                    } else {
                        if (mRange.isInheritable()) {
                            lastRange.setInheritable(true);
                            lastRange.setEndRevision(mRange.getEndRevision());
                        } else {
                            if (dupMRange) {
                                pushedMRange1 = mRange.dup();
                            } else {
                                pushedMRange1 = mRange;
                            }
                            pushedMRange1.setStartRevision(lastRange.getEndRevision());
                        }
                    }
                } else if (lastRange.getEndRevision() == mRange.getEndRevision()) {
                    if (lastRange.getStartRevision() < mRange.getStartRevision()) {
                        if (!lastRange.isInheritable()) {
                            lastRange.setEndRevision(mRange.getStartRevision());
                            if (dupMRange) {
                                pushedMRange1 = mRange.dup();
                            } else {
                                pushedMRange1 = mRange;
                            }
                            lastRange = pushedMRange1;
                        }
                    } else {
                        lastRange.setStartRevision(mRange.getStartRevision());
                        lastRange.setEndRevision(mRange.getEndRevision());
                        lastRange.setInheritable(mRange.isInheritable());
                        if (dupMRange) {
                            pushedMRange1 = mRange.dup();
                        } else {
                            pushedMRange1 = mRange;
                        }
                        pushedMRange1.setStartRevision(lastRange.getEndRevision());
                        pushedMRange1.setInheritable(true);
                    }
                } else {
                    if (lastRange.getStartRevision() < mRange.getStartRevision()) {
                        if (!(lastRange.getEndRevision() > mRange.getEndRevision() && lastRange.isInheritable())) {
                            tmpRevision = lastRange.getEndRevision();
                            if (!lastRange.isInheritable()) {
                                lastRange.setEndRevision(mRange.getStartRevision());
                            } else {
                                mRange.setStartRevision(lastRange.getEndRevision());
                            }
                            if (dupMRange) {
                                pushedMRange1 = mRange.dup();
                            } else {
                                pushedMRange1 = mRange;
                            }
                            if (tmpRevision > mRange.getEndRevision()) {
                                pushedMRange2 = new SVNMergeRange(mRange.getEndRevision(), tmpRevision, lastRange.isInheritable());
                            }
                            mRange.setInheritable(true);
                        }
                    } else {
                        if (lastRange.getEndRevision() < mRange.getEndRevision()) {
                            if (pushedMRange2 == null) {
                                pushedMRange2 = new SVNMergeRange(lastRange.getEndRevision(), mRange.getEndRevision(), mRange.isInheritable());
                            }
                            
                            tmpRevision = lastRange.getStartRevision();
                            lastRange.setStartRevision(mRange.getStartRevision());
                            lastRange.setEndRevision(tmpRevision);
                            lastRange.setInheritable(mRange.isInheritable());
                            
                            mRange.setStartRevision(tmpRevision);
                            mRange.setEndRevision(pushedMRange2.getStartRevision());
                            mRange.setInheritable(true);
                        } else {
                            if (pushedMRange2 == null) {
                                pushedMRange2 = new SVNMergeRange(mRange.getEndRevision(), lastRange.getEndRevision(), lastRange.isInheritable());
                            }
                            
                            tmpRevision = lastRange.getStartRevision();
                            lastRange.setStartRevision(mRange.getStartRevision());
                            lastRange.setEndRevision(tmpRevision);
                            lastRange.setInheritable(mRange.isInheritable());
                            
                            mRange.setStartRevision(tmpRevision);
                            mRange.setEndRevision(pushedMRange2.getStartRevision());
                            mRange.setInheritable(true);
                        }
                    }
                }
            }
        }
        
        if (pushedMRange1 != null) {
            rangeList.add(pushedMRange1);
            lastRange = pushedMRange1;
        }
        if (pushedMRange2 != null) {
            rangeList.add(pushedMRange2);
            lastRange = pushedMRange2;
        }
        return lastRange;
    }

    public SVNMergeRangeList mergeRevision(long revision) {
        if (getSize() > 0 && getRanges()[getSize() - 1].getEndRevision() == revision - 1) {
            getRanges()[getSize() - 1].setEndRevision(revision);
            return this;
        }
        pushRange(revision -1 , revision, true);
        return this;
    }
}
