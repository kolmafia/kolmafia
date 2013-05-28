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
package org.tmatesoft.svn.core.auth;

import org.tmatesoft.svn.core.SVNErrorMessage;

/**
 * The <b>ISVNProxyManager</b> interface is used to manage http server 
 * options. 
 * 
 * <p>
 * A default implementation of the <b>ISVNProxyManager</b> interface (that comes along 
 * with a default implementation of <b>ISVNAuthenticationManager</b> - <b>org.tmatesoft.svn.core.internal.wc.DefaultSVNAuthenticationManager</b>) 
 * uses servers options from the standard <i>servers</i> file (it can be found in the 
 * Subversion runtime configuration area - read {@link org.tmatesoft.svn.core.wc.ISVNOptions more}). 
 * 
 * <p>
 * HTTP proxy options handled by <b>ISVNProxyManager</b> are similar to 
 * the native SVN options - read more on <b>servers</b> options in the  
 * <a href="http://svnbook.red-bean.com/nightly/en/svn.advanced.html#svn.advanced.confarea.opts.servers">Subversion online book</a>. 
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 * @see     ISVNAuthenticationManager
 */
public interface ISVNProxyManager {
   
    /**
     * Returns the proxy host for HTTP connection. 
     * 
     * @return the hostname of the proxy server through which 
     *         HTTP-based requests must pass
     *          
     */
    public String getProxyHost();
    
    /**
     * Returns the port number on the proxy host to use.
     * 
     * @return a port number
     */
    public int getProxyPort();
    
    /**
     * Returns the username to supply to the proxy machine.
     * 
     * @return a username
     */
    public String getProxyUserName();
    
    /**
     * Returns the password to supply to the proxy machine.
     * 
     * @return a password
     */
    public String getProxyPassword();
    
    /**
     * Accepts this proxy settings if successfully connected 
     * to the proxy server, or not if failed to connect. 
     * 
     * @param accepted      <span class="javakeyword">true</span> if 
     *                      the proxy is successfully reached, otherwise 
     *                      <span class="javakeyword">false</span>
     * @param errorMessage  the reason of the failure to connect to 
     *                      the proxy server
     */
    public void acknowledgeProxyContext(boolean accepted, SVNErrorMessage errorMessage);
}
