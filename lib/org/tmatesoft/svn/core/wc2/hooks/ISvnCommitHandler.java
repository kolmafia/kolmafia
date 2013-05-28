package org.tmatesoft.svn.core.wc2.hooks;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.wc.DefaultSVNCommitHandler;
import org.tmatesoft.svn.core.wc2.SvnCommitItem;

/**
 * Implementing this interface allows to manage commit log messages for items to be committed in
 * a common transaction.
 * 
 * <p>
 * The interface defines the only one method which takes the initial log message
 * and an array of items that are intended for a commit. For example, an implementor's 
 * code can process those items and add some generated additional comment to that one 
 * passed into the method. There could be plenty of scenarios.  
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 * @see     DefaultSVNCommitHandler      
 */
public interface ISvnCommitHandler {
    
	/**
     * Handles the incoming initial log message and items intended for a commit and 
     * returns a new commit log message.
     *  
     * @param  message			an initial log message
     * @param  commitables		an array of items to be committed
     * @return					a new log message string or <code>null</code> to cancel commit operation.
     * @throws SVNException
     */
    public String getCommitMessage(String message, SvnCommitItem[] commitables) throws SVNException;

    /**
     * Handles the incoming revision properties and returns filtered revision properties given the paths 
     * (represented by <code>commitables</code>) collected for committing and the commit log message.
     * 
     * <p>
     * Only the returned filtered revision properties will be set on a new committed revision.
     * 
     * @param  message             log message for commit    
     * @param  commitables         paths to commit
     * @param  revisionProperties  initial revision properties
     * @return                     filtered revision properties
     * @throws SVNException 
     */
    public SVNProperties getRevisionProperties(String message, SvnCommitItem[] commitables, SVNProperties revisionProperties) throws SVNException;
}
