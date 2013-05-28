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
 * The <b>ISVNCanceller</b> interface is used in <code>SVNKit</code> for cancelling operations. 
 * To cancel a running operation an implementor should throw an {@link SVNCancelException} from his 
 * <code>checkCancelled()</code> implementation. This method is called in plenty of <code>SVNKit</code> 
 * methods to give a user a chance to cancel a current running operation. For example, it could be a GUI 
 * application where a 'cancel' button would make the implementor's <code>checkCancelled()</code> method 
 * throw such an exception. 
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public interface ISVNCanceller {
   
    /**
     * Default implementation which never throws an {@link SVNCancelException} (never cancels).
     */
    public ISVNCanceller NULL = new ISVNCanceller() {
        public void checkCancelled() throws SVNCancelException {
        }
    };
    
    /**
     * Checks if the current operation is cancelled (somehow interrupted)
     * and should throw an <b>SVNCancelException</b> or notify the handler if exists.
     * 
     * <p/>
     * This method is often called during iterations when processing trees of versioned items.
     * This way the entire operation may be interrupted without waiting till the iteration run out.
     * 
     * @throws SVNCancelException
     */
    public void checkCancelled() throws SVNCancelException;

}
