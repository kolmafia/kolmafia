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


/**
 * The <b>ISVNSession</b> interface provides some extra handling operations over 
 * <b>SVNRepository</b> objects. 
 * 
 * <p>
 * For remote accessing a repository (via <code>svn://</code> and 
 * <code>http://</code>) <b>SVNRepository</b> drivers open socket connections to 
 * write and read data from. Session objects (implementing <b>ISVNSession</b>) may
 * enable an <b>SVNRepository</b> object to use a single socket connection during the
 * whole runtime, or, as an alternative, to use a new socket connection per each
 * repository access operation (this slows the speed of operation execution since
 * the operation needs some extra time for opening and closing a socket).
 * 
 * <p>     
 * Also <b>ISVNSession</b> allows to cache and retrieve commit messages during
 * runtime.
 * 
 * <p>
 * How to set a session object for an <b>SVNRepository</b> driver:
 * <pre class="javacode">
 * <span class="javakeyword">import</span> org.tmatesoft.svn.core.io.ISVNSession;
 * <span class="javakeyword">import</span> org.tmatesoft.svn.core.io.SVNRepositoryFactory;
 * <span class="javakeyword">import</span> org.tmatesoft.svn.core.io.SVNRepository;
 * <span class="javakeyword">import</span> org.tmatesoft.svn.core.SVNURL;
 * ...
 * 
 *     ISVNSession session;
 *     ...
 *     SVNURL url = SVNURL.parseURIEncoded(<span class="javastring">"svn://host/path/to/repos"</span>);
 *     <span class="javakeyword">try</span>{
 *         SVNRepository repository = SVNRepositoryFactory.create(url, session);
 *         ...
 *     }<span class="javakeyword">catch</span>(SVNException svne){
 *         ...
 *     }</pre><br />
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 * @see     SVNRepository
 * @see     SVNRepositoryFactory
 */
public interface ISVNSession {
    /**
     * Says if the given <b>SVNRepository</b> object should use a single socket
     * connection (not to open/close a new one for each operation). This will 
     * certainly improve the <b>SVNRepository</b> object's methods performance speed.
     * <p>
     * For examlpe, a session object may hold a number of <b>SVNRepository</b> 
     * object references knowing for everyone of them if it should keep a single
     * connection or not.
     * 
     * @param   repository  an <b>SVNRepository</b> driver
     * @return  <span class="javakeyword">true</span> if <code>repository</code> 
     *          should use a single socket connection during the whole runtime, 
     *          <span class="javakeyword">false</span> - to open/close a new
     *          connection for each repository access operation
     */
    public boolean keepConnection(SVNRepository repository);
    
    /**
     * Caches a commit message for the given revision.
     * 
     * @param repository  an <b>SVNRepository</b> driver (to distinguish
     *                    that repository for which this message is actual)
     * @param revision    a revision number
     * @param message     the commit message for <code>revision</code>
     * @see               #getCommitMessage(SVNRepository, long)
     */
    public void saveCommitMessage(SVNRepository repository, long revision, String message);

    /**
     * Retrieves the cached commit message for a particular revision.
     * Use {@link #getCommitMessage(SVNRepository, long) getCommitMessage()} to 
     * check if there's a message in cache.
     * 
     * @param repository  an <b>SVNRepository</b> driver (to distinguish
     *                    that repository for which a commit message is requested)
     * @param revision    a revision number
     * @return            the commit message for <code>revision</code>
     * @see               #saveCommitMessage(SVNRepository, long, String)
     */
    public String getCommitMessage(SVNRepository repository, long revision);
    
    /**
     * Checks if there's a commit message in cache for a particular repository
     * and revision.
     * 
     * @param repository  an <b>SVNRepository</b> driver (to distinguish
     *                    that repository for which a commit message is requested)
     * @param revision    a revision number
     * @return            <span class="javakeyword">true</span> if the cache
     *                    has got a message for the given repository and revision,
     *                    <span class="javakeyword">false</span> otherwise 
     */
    public boolean hasCommitMessage(SVNRepository repository, long revision);
    
    /**
     * A session options implementation that simply allows to keep 
     * a single connection alive for all data i/o. This implementation 
     * does not cache commit messages. 
     */
    public ISVNSession KEEP_ALIVE = new ISVNSession() {
        public boolean keepConnection(SVNRepository repository) {
            return true;
        }
        public void saveCommitMessage(SVNRepository repository, long revision, String message) {
        }
        public String getCommitMessage(SVNRepository repository, long revision) {
            return null;
        }
        public boolean hasCommitMessage(SVNRepository repository, long revision) {
            return false;
        }
    };

    /**
     * The same as {@link #KEEP_ALIVE}. Left for backward 
     * compatibility. 
     */
    public ISVNSession DEFAULT = KEEP_ALIVE;
}
