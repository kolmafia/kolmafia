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

package org.tmatesoft.svn.core.io;

import org.tmatesoft.svn.core.SVNException;

/**
 * The <b>ISVNReporterBaton</b> interface should be implemented by callers
 * of update, checkout, etc. operations of <b>SVNRepository</b> drivers in order
 * to describe the state of local items.
 * 
 * <p>
 * For more information on using reporters, please, read these on-line article: 
 * <a href="http://svnkit.com/kb/dev-guide-update-operation.html">Using ISVNReporter/ISVNEditor in update-related operations</a>
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 * @see 	ISVNReporter
 * @see 	SVNRepository
 * @see     <a target="_top" href="http://svnkit.com/kb/examples/">Examples</a>
 */
public interface ISVNReporterBaton {
    /**
     * Makes a report describing the state of local items in order
     * to get the differences between the local items and what actually
     * is in a repository. 
     * 
     * @param  reporter 		a reporter passed to make reports
     * @throws SVNException
     */
    public void report(ISVNReporter reporter) throws SVNException;

}

