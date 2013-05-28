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
package org.tmatesoft.svn.core.wc.admin;


/**
 * The <b>SVNUUIDAction</b> class is an enumeration of possible actions 
 * that <b>SVNAdminClient</b> can perform with uuids when loading a dumpstream.
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public class SVNUUIDAction {

    private SVNUUIDAction() {
    }
    
    /**
     * A command to ignore any met uuid.
     */
    public static final SVNUUIDAction IGNORE_UUID = new SVNUUIDAction();
    
    /**
     * A command to override the existing uuid with any one met in a dumpstream.  
     */
    public static final SVNUUIDAction FORCE_UUID = new SVNUUIDAction();
    
    /**
     * A default behaviour: any met uuid is ignored unless the latest revision 
     * of the target repository is 0.
     */
    public static final SVNUUIDAction DEFAULT = new SVNUUIDAction();

}
