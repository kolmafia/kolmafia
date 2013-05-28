package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNException;

/**
 * This interface describes the operation that can receive object(s). 
 * Implementation is {@link SvnReceivingOperation},
 * and many intermediate classes for 1.6 compatibility (classes that deal with handlers).
 * Clients can provide their own handlers for receiving the object(s) 
 * by implementing this interface and assign them in 
 * {@link SvnReceivingOperation#setReceiver(ISvnObjectReceiver)}.
 *   
 * @author TMate Software Ltd.
 * @version 1.7
 * @param <T> type of received object
 * @see SvnReceivingOperation
 */
public interface ISvnObjectReceiver<T> {
    
	/**
	 * Receives object with is target.
	 * 
	 * @param target target of the object
	 * @param object object
	 * @throws SVNException
	 */
    public void receive(SvnTarget target, T object) throws SVNException;

}
