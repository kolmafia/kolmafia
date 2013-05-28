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
import org.tmatesoft.svn.core.SVNURL;

/**
 * The <b>ISVNAuthenticationProvider</b> interface is implemented by user 
 * credentials providers. Such a provider is set to an authentication manager 
 * calling {@link ISVNAuthenticationManager#setAuthenticationProvider(ISVNAuthenticationProvider) setAuthenticationProvider()}.  
 * When a repository server pulls user's credentials, an <b>SVNRepository</b> driver 
 * asks the registered <b>ISVNAuthenticationManager</b> for credentials. The auth manager in its turn 
 * will ask the registered auth provider for credentials.
 * 
 * <p>
 * <b>ISVNAuthenticationProvider</b> may be implemented to keep a list of credentials, for example, there is 
 * such a default SVNKit implementation (that comes along with a default implementation of 
 * <b>ISVNAuthenticationManager</b> - <b>org.tmatesoft.svn.core.internal.wc.DefaultSVNAuthenticationManager</b>),  
 * that saves credentials in and retrieves them from the in-memory cache only during runtime (not on the disk); 
 * or the default one that uses the auth area cache (read the <a href="http://svnbook.red-bean.com/nightly/en/svn-book.html#svn.serverconfig.netmodel.credcache">Subversion book chapter</a>).    
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 * @see     ISVNAuthenticationManager
 * @see     org.tmatesoft.svn.core.io.SVNRepository
 */
public interface ISVNAuthenticationProvider {
    /**
     * Denotes that a user credential is rejected by a server. 
     */
    public int REJECTED = 0;
    
    /**
     * Denotes that a user credential is accepted by a server and will be 
     * cached only during runtime, not on the disk.
     */
    public int ACCEPTED_TEMPORARY = 1;
    
    /**
     * Denotes that a user credential is accepted by a server and will be 
     * cached on the disk.
     */
    public int ACCEPTED = 2;
    
    /**
     * Returns a next user credential of the specified kind for the given 
     * authentication realm.
     * 
     * <p>
     * If this provider has got more than one credentials (say, a list of credentials), 
     * to get the first one of them <code>previousAuth</code> is set to 
     * <span class="javakeyword">null</span>.
     * 
     * @param kind              a credential kind (for example, like those defined in 
     *                          {@link ISVNAuthenticationManager})
     * @param  url              a repository location that is to be accessed
     * @param  realm            a repository authentication realm (host, port, realm string) 
     * @param  errorMessage     the recent authentication failure error message
     * @param  previousAuth     the credential that was previously retrieved (to tell if it's 
     *                          not accepted)  
     * @param  authMayBeStored  if <span class="javakeyword">true</span> then the returned credential 
     *                          can be cached, otherwise it won't be cached anyway
     * @return                  a next user credential
     */
    public SVNAuthentication requestClientAuthentication(String kind,
            SVNURL url, String realm, SVNErrorMessage errorMessage, SVNAuthentication previousAuth,
            boolean authMayBeStored);
    /**
     * Checks a server authentication certificate and whether accepts it 
     * (if the client trusts it) or not.
     * 
     * <p>
     * This method is used by an SSL manager (see {@link org.tmatesoft.svn.core.internal.wc.DefaultSVNSSLTrustManager}).
     * 
     * @param url                 a repository location that is accessed
     * @param realm               a repository authentication realm (host, port, realm string) 
     * @param certificate         a server certificate object
     * @param resultMayBeStored   if <span class="javakeyword">true</span> then the server certificate 
     *                            can be cached, otherwise not 
     * @return                    the result of the certificate check ({@link #REJECTED}, {@link #ACCEPTED_TEMPORARY}, or {@link #ACCEPTED})                  
     */
    public int acceptServerAuthentication(SVNURL url, String realm, Object certificate, boolean resultMayBeStored);
}
