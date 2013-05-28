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
package org.tmatesoft.svn.core.internal.wc;

/**
 * The <b>ISVNAuthenticationStorage</b> interface is used to implement custom 
 * runtime authentication storage managers, that are responsible for caching 
 * user credentials as well as for retrieving cached credentials from the 
 * storage of a preferable type (it may be an in-memory cache, or a disk cache).
 * 
 * <p>
 * To make an authentication manager use your custom auth storage manager, 
 * provide it to the {@link org.tmatesoft.svn.core.internal.wc.DefaultSVNAuthenticationManager#setRuntimeStorage(ISVNAuthenticationStorage) setRuntimeStorage()}
 * method of the authentication manager. 
 * 
 * <p>
 * A default implementation of <b>ISVNAuthenticationStorage</b> (that comes along 
 * with a default implementation of <b>ISVNAuthenticationManager</b> - <b>org.tmatesoft.svn.core.internal.wc.DefaultSVNAuthenticationManager</b>) 
 * caches credentials only in the memory (not in the filesystem) during runtime. This feature is handy especially when 
 * on-disk caching is disabled in the standard <i>config</i> file (option <span class="javastring">"store-auth-creds"</span> is <span class="javastring">"no"</span>).
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @see     org.tmatesoft.svn.core.auth.ISVNAuthenticationManager
 */
public interface ISVNAuthenticationStorage {
    /**
     * Caches a credential of the specified kind for the given repository 
     * authentication realm in the auth storage.  
     * 
     * @param kind   a credential kind (for example, like those defined in 
     *               {@link org.tmatesoft.svn.core.auth.ISVNAuthenticationManager})
     * @param realm  a repository authentication realm including a hostname, 
     *               a port number and a realm string 
     * @param data   a credential object
     */
    public void putData(String kind, String realm, Object data);
    
    /**
     * Retrieves a cached credential of the specified kind for the 
     * given repository authentication realm from the auth storage.
     * 
     * @param  kind   a credential kind (for example, like those defined in 
     *                {@link org.tmatesoft.svn.core.auth.ISVNAuthenticationManager})
     * @param  realm  a repository authentication realm including a hostname, 
     *                a port number and a realm string 
     * @return        a credential object
     */
    public Object getData(String kind, String realm);

}
