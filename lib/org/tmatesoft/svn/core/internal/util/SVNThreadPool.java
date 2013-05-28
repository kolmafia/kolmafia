/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.util;

import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNThreadPool implements ISVNThreadPool {
    
    private ThreadPoolExecutor myThreadPool;
    private CustomThreadFactory myThreadFactory;
    private ISVNThreadPool myFailSafePool;
    
    public ISVNTask run(Runnable task, boolean daemon) {
        ThreadPoolExecutor threadPool = getThreadPool(daemon);
        try {
            Future<?> future = threadPool.submit(task);
            return new SVNTask(future); 
        } catch (RejectedExecutionException e) {
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.DEFAULT, "Could not submit task: " + e.getMessage());
        }
        
        ISVNThreadPool failSafePool = getFailSafePool();
        return failSafePool.run(task, daemon);
    }
    
    private synchronized ISVNThreadPool getFailSafePool() {
        if (myFailSafePool == null) {
            myFailSafePool = new SVNEmptyThreadPool();
        }
        return myFailSafePool;
    }
    
    private synchronized ThreadPoolExecutor getThreadPool(boolean daemon) {
        CustomThreadFactory threadFactory = getThreadFactory(daemon);
        if (myThreadPool == null) {
            myThreadPool = new ThreadPoolExecutor(2, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), threadFactory);
        }
        return myThreadPool;
    }

    private synchronized CustomThreadFactory getThreadFactory(boolean daemon) {
        if (myThreadFactory == null) {
            myThreadFactory = new CustomThreadFactory(daemon);
        } else {
            myThreadFactory.setIsDaemon(daemon);
        }
        
        return myThreadFactory; 
    }
    
    private static class CustomThreadFactory implements ThreadFactory {
        private static final AtomicInteger ourPoolNumber = new AtomicInteger(1);
        private final ThreadGroup myGroup;
        private final AtomicInteger myThreadNumber = new AtomicInteger(1);
        private final String myNamePrefix;
        private boolean myIsDaemon;
        
        CustomThreadFactory(boolean daemon) {
            SecurityManager s = System.getSecurityManager();
            myGroup = (s != null)? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            myNamePrefix = "pool-" + ourPoolNumber.getAndIncrement() + "-svnkit-thread-";
            myIsDaemon = daemon;
        }

        public void setIsDaemon(boolean isDaemon) {
            myIsDaemon = isDaemon;
        }
        
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(myGroup, task, myNamePrefix + myThreadNumber.getAndIncrement(), 0);
            thread.setDaemon(myIsDaemon);
            if (thread.getPriority() != Thread.NORM_PRIORITY) {
                thread.setPriority(Thread.NORM_PRIORITY);
            }
            return thread;
        }
        
    }
    
}
