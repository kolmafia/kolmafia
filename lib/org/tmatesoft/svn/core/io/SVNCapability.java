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
package org.tmatesoft.svn.core.io;

/**
 * This class contains enumeration that describes the repository capabilities or,
 * in other words, features that the repository may be capable of.
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public class SVNCapability {
    
    /**
     * Represents the capability of the repository to understand 
     * what the client means when the client describes the
     * depth of a working copy to the server.
     */
	public static final SVNCapability DEPTH = new SVNCapability("depth");
	
    /**
     * Represents the capability of the repository to support merge-tracking
     * information.
     */
	public static final SVNCapability MERGE_INFO = new SVNCapability("mergeinfo");
    
	/**
     * Represents the capability of retrieving arbitrary revision properties. 
     */
	public static final SVNCapability LOG_REVPROPS = new SVNCapability("log-revprops");
    
	/**
     * Represents the capability of replaying a directory in the repository (partial replay).
     */
	public static final SVNCapability PARTIAL_REPLAY = new SVNCapability("partial-replay");
    
    /**
     * Represents the capability of committing revision properties modifications along with
     * a normal transaction.
     */
	public static final SVNCapability COMMIT_REVPROPS = new SVNCapability("commit-revprops");

	/**
     * Represents the capability of specifying (and atomically verifying) expected
     * preexisting values when modifying revprops.
     */
    public static final SVNCapability ATOMIC_REVPROPS = new SVNCapability("atomic-revprops");
	
	private String myName;
	
	private SVNCapability(String name) {
		myName = name;
	}

	/**
	 * Returns a string representation of this object.
	 * @return this object's string representation
	 */
	public String toString() {
        return myName;
    }

}
