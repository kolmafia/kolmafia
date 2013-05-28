package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;

/**
 * This interface describes Svn* operation runner.
 * Provides context for the operation.
 * Defines runner's working copy generation, which is used by 
 * for deciding whether it is applicable runner implementation of the operation.
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 *
 * @param <V> type of the operation's return value
 * @param <T> type of the operation, subclass of {@link SvnOperation}
 * @see SvnOperation
 * @see SvnOperationFactory
 */
public interface ISvnOperationRunner<V, T extends SvnOperation<V>> {
    
	/**
	 * Returns whether this runner is applicable for the operation on concrete working copy generation (1.7 or 1.6) 
	 * 
	 * @param operation operation that needs runner
	 * @param wcGeneration working copy generation
	 * @return <code>true</code> if the runner is applicable, otherwise <code>false</code>
	 * @throws SVNException
	 */
    public boolean isApplicable(T operation, SvnWcGeneration wcGeneration) throws SVNException;
    
    /**
     * Implementation of operation's <code>run</code> method for concrete working copy generation 
     *  
     * @param operation operation that needs to be executed
     * @return execution result value of operation's return type
     * @throws SVNException
     */
    public V run(T operation) throws SVNException;
    
    /**
     * Sets operation's context
     * 
     * @param context context of the operation
     */
    public void setWcContext(SVNWCContext context);
    
    /**
     * Resets runner's working copy generation.
     * 
     * @param detectedWcGeneration new working copy generation for the runner
     */
    public void reset(SvnWcGeneration detectedWcGeneration);
    
    /**
     * Returns runner's working copy generation it is able to operate on.
     * 
     * @return working copy generation of the runner.
     */
    public SvnWcGeneration getWcGeneration();
}
