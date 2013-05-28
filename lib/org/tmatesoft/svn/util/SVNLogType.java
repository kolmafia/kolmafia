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
package org.tmatesoft.svn.util;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNLogType {
    public static final SVNLogType NETWORK = new SVNLogType("svnkit-network", "NETWORK");
    public static final SVNLogType WC = new SVNLogType("svnkit-wc", "WC");
    public static final SVNLogType FSFS = new SVNLogType("svnkit-fsfs", "FSFS");
    public static final SVNLogType CLIENT = new SVNLogType("svnkit-cli", "CLI");
    public static final SVNLogType DEFAULT = new SVNLogType("svnkit", "DEFAULT");
    public static final SVNLogType NATIVE_CALL = new SVNLogType("svnkit-native", "NATIVE");
    
    private String myName;
    private String myShortName;
    
    private SVNLogType(String name, String shortName) {
        myName = name;
        myShortName = shortName;
    }
    
    public String getName() {
        return myName;
    }
    
    public String getShortName() {
        return myShortName;
    }
}
