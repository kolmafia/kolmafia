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

import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.svn.ISVNConnector;

/**
 * The <b>ISVNTunnelProvider</b> is the interface for 
 * providers of tunnel command lines matching a specific 
 * <code>"svn+xxx"</code> tunnel scheme. 
 * 
 * <p>
 * With Subversion you may put your own URL scheme into the 
 * <code>config</code> file under the <code>tunnels</code> 
 * section like this:
 * <pre class="javacode">
 * ssh = $SVN_SSH ...
 * rsh = $SVN_RSH ...
 * ...</pre>
 * The idea of this tunnel provider interface is the same: 
 * given a subprotocol name (a string following <code>svn+</code>, 
 * like <code>ssh</code>) a provider returns a command string 
 * (like <code>$SVN_SSH ...</code>).
 * 
 * <p>
 * A tunnel provider is passed to an <b>SVNRepository</b> driver 
 * that is expected to work through a tunnel (see {@link SVNRepository#setTunnelProvider(ISVNTunnelProvider) 
 * SVNRepository.setTunnelProvider()}). Just as you instantiate an <b>SVNRepository</b> object 
 * set it to use your tunnel provider.  
 * 
 * <p>
 * If you would like to use tunnel scheme definitions from the 
 * standard Subversion <code>config</code> file, you may use 
 * a default provider implementation which is a default options 
 * driver you get calling a <b>createDefaultOptions()</b> method 
 * of the {@link org.tmatesoft.svn.core.wc.SVNWCUtil} class.
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public interface ISVNTunnelProvider {
    
    /**
     * Returns a tunnel comand line matching the given subprotocol 
     * name. 
     * 
     * @param location
     * @return                 a tunnel command line
     */
    public ISVNConnector createTunnelConnector(SVNURL location);
}
