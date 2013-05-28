package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.ISVNCanceller;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.ISVNRepositoryPool;
import org.tmatesoft.svn.core.wc.SVNEvent;

/**
 * Implementing this interface allows to handle the operation options:
 * event handler, canceler, options, pool of repositories and authentication manager.  
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public interface ISvnOperationOptionsProvider {

	 /**
     * Gets the event handler for the operation. This event handler will be
     * dispatched {@link SVNEvent} objects to provide detailed information about
     * actions and progress state of version control operations. 
     * 
     * @return handler for events
     * @see ISVNEventHandler
     */
    ISVNEventHandler getEventHandler();

    /**
     * Gets operation's options.
     * 
     * @return options of the operation
     */
    ISVNOptions getOptions();

    /**
     * Gets the operation's pool of repositories.
     * 
     * @return pool of repositories
     */
    ISVNRepositoryPool getRepositoryPool();

    /**
     * Gets operation's authentication manager.
     * 
     * @return authentication manager
     */
    ISVNAuthenticationManager getAuthenticationManager();

    /**
     * Gets the cancel handler of the operation.
     * 
     * @return cancel handler
     */
    ISVNCanceller getCanceller();
}
