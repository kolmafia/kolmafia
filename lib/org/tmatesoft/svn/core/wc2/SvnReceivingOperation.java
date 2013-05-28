package org.tmatesoft.svn.core.wc2;

import java.util.Collection;
import java.util.LinkedList;

import org.tmatesoft.svn.core.SVNException;

/**
 * Represents base class for all operations that can fetch object(s) 
 * for custom processing.
 * 
 * <p/>
 * Clients can provide their own handlers for receiving the object(s) 
 * by implementing {@link ISvnObjectReceiver} interface and assign it in 
 * {@link #setReceiver(ISvnObjectReceiver)}. 
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 * @param <T> type of received object(s)
 * @see SvnOperation
 * @see ISvnObjectReceiver
 * @see SvnAnnotate
 * @see SvnDiffSummarize
 * @see SvnGetChangelistPaths
 * @see SvnGetInfo
 * @see SvnGetProperties
 * @see SvnGetStatus
 * @see SvnList
 * @see SvnLog
 * @see SvnLogMergeInfo
 * @see SvnSetLock
 * @see SvnSetProperty
 * @see SvnUnlock
 */
public class SvnReceivingOperation<T> extends SvnOperation<T> implements ISvnObjectReceiver<T> {

    private ISvnObjectReceiver<T> receiver;
    private T first;
    private T last;
    private Collection<T> receivedObjects;
    
    protected SvnReceivingOperation(SvnOperationFactory factory) {
        super(factory);
    }
    
    /**
     * Sets client's object receiver.
     * 
     * @param receiver object receiver
     */
    public void setReceiver(ISvnObjectReceiver<T> receiver) {
        this.receiver = receiver;
    }
    
    /**
     * Returns client's object receiver.
     * 
     * @return object receiver
     */
    public ISvnObjectReceiver<T> getReceiver() {
        return this.receiver;
    }

    /**
     * Receives the object, adds it to objects list and calls 
     * client's receiver if exists.
     */
    public void receive(SvnTarget target, T object) throws SVNException {
        if (first == null) {
            first = object;
        }
        last = object;
        
        if (getReceivedObjects() != null) {
            getReceivedObjects().add(object);
        }
        if (getReceiver() != null) {
            getReceiver().receive(target, object);
        }
    }
    
    /**
     * Returns first received object from the list.
     * 
     * @return first received object
     */
    public T first() {
        return this.first;
    }
    
    /**
     * Returns last received object from the list.
     * 
     * @return first received object
     */
    public T last() {
        return this.last;
    }
    
    /**
     * Initializes list for received objects with <code>objects</code>
     * or creates an empty list, calls the operation's {@link #run()}
     * method.
     * 
     * @param objects
     * @return list of received objects
     * @throws SVNException
     */
    public Collection<T> run(Collection<T> objects) throws SVNException {
        setReceivingContainer(objects != null ? objects : new LinkedList<T>());
        try {
            run();
            return getReceivedObjects();
        } finally {
            setReceivingContainer(null);
        }
    }

    @Override
    protected void initDefaults() {
        super.initDefaults();
        this.first = null;
    }

    
    private void setReceivingContainer(Collection<T> receivingContainer) {
        this.receivedObjects = receivingContainer;
    }
    
    private Collection<T> getReceivedObjects() {
        return this.receivedObjects;
    }
}
