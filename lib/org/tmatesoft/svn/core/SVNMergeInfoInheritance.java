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
 * This class contains enumeration that describes the ways of requesting merge information.
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public class SVNMergeInfoInheritance {
    
    /**
     * Represents the way of requesting the explicit merge information for the element.
     */
    public static final SVNMergeInfoInheritance EXPLICIT = new SVNMergeInfoInheritance("explicit");
    
    /**
     * Represents the way of requesting the explicit merge information for the element, if exists, otherwise
     * inherited merge information from the nearest ancestor of the element.
     */
    public static final SVNMergeInfoInheritance INHERITED = new SVNMergeInfoInheritance("inherited");
    
    /**
     * Represents the way of requesting the merge information from the element's nearest ancestor, 
     * regardless of whether the element has explicit info.
     */
    public static final SVNMergeInfoInheritance NEAREST_ANCESTOR = new SVNMergeInfoInheritance("nearest-ancestor");
    
    private String myName;
    
    private SVNMergeInfoInheritance(String name) {
        myName = name;
    }
  
    /**
     * Returns a string representation of this object.
     * @return this object as a string 
     */
    public String toString() {
        return myName;
    }
}
