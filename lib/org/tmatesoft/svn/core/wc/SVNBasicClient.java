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
package org.tmatesoft.svn.core.wc;

import java.io.File;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminArea;
import org.tmatesoft.svn.core.internal.wc.admin.SVNWCAccess;
import org.tmatesoft.svn.core.internal.wc16.SVNBasicDelegate;
import org.tmatesoft.svn.core.wc2.SvnGetInfo;
import org.tmatesoft.svn.core.wc2.SvnInfo;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.ISVNDebugLog;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * The <b>SVNBasicClient</b> is the base class of all <b>SVN</b>*<b>Client</b>
 * classes that provides a common interface and realization.
 *
 * <p>
 * All of <b>SVN</b>*<b>Client</b> classes use inherited methods of
 * <b>SVNBasicClient</b> to access Working Copies metadata, to create a driver
 * object to access a repository if it's necessary, etc. In addition
 * <b>SVNBasicClient</b> provides some interface methods - such as those that
 * allow you to set your {@link ISVNEventHandler event handler}, obtain run-time
 * configuration options, and others.
 *
 * @version 1.3
 * @author TMate Software Ltd.
 * @since 1.2
 */
public class SVNBasicClient {

    private static final String SVNKIT_WC_17_PROPERTY = "svnkit.wc.17";
    private static final String SVNKIT_WC_17_DEFAULT = "true";
    private static final String SVNKIT_WC_17_EXPECTED = "true";

    private SvnOperationFactory operationFactory;
    private boolean ignoreExternals;
    private boolean leaveConflictsUnresolved;
    private ISVNPathListHandler pathListHandler;
    private ISVNDebugLog debugLog;

    protected SVNBasicClient(ISVNAuthenticationManager authManager, ISVNOptions options) {
        if (authManager == null) {
            authManager = SVNWCUtil.createDefaultAuthenticationManager();
        }
        if (options == null) {
            options = SVNWCUtil.createDefaultOptions(true);
        }
        this.operationFactory = new SvnOperationFactory();
        setOptions(options);
        this.operationFactory.setAuthenticationManager(authManager);
        this.operationFactory.setRepositoryPool(new DefaultSVNRepositoryPool(authManager, options));
        this.operationFactory.setAutoDisposeRepositoryPool(true);
        
        setPathListHandler(null);
        setDebugLog(null);
        setEventPathPrefix(null);
        setEventHandler(null);
    }

    protected SVNBasicClient(ISVNRepositoryPool pool, ISVNOptions options) {
        boolean autoDisposeRepositoryPool = false;
        if (pool == null) {
            pool = new DefaultSVNRepositoryPool(SVNWCUtil.createDefaultAuthenticationManager(), options);
            autoDisposeRepositoryPool = true;
        }
        if (options == null) {
            options = SVNWCUtil.createDefaultOptions(true);
        }
        this.operationFactory = new SvnOperationFactory();
        setOptions(options);
        this.operationFactory.setRepositoryPool(pool);
        this.operationFactory.setAutoDisposeRepositoryPool(autoDisposeRepositoryPool);
        
        setPathListHandler(null);
        setDebugLog(null);
        setEventPathPrefix(null);
        setEventHandler(null);
    }

    public static boolean isWC17Supported() {
        return SVNKIT_WC_17_EXPECTED.equalsIgnoreCase(System.getProperty(SVNKIT_WC_17_PROPERTY, SVNKIT_WC_17_DEFAULT));
    }

    protected static SVNBasicDelegate dontWC17Support() throws SVNException {
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_UNSUPPORTED_FORMAT);
        SVNErrorManager.error(err, SVNLogType.CLIENT);
        return null;
    }

    /**
     * Gets run-time configuration options used by this object.
     *
     * @return the run-time options being in use
     */
    public ISVNOptions getOptions() {
        return getOperationsFactory().getOptions();
    }

    /**
     * Sets run-time global configuration options to this object.
     *
     * @param options
     *            the run-time configuration options
     */
    public void setOptions(ISVNOptions options) {
        if (options == null) {
            options = SVNWCUtil.createDefaultOptions(true);
        }
        getOperationsFactory().setOptions(options);
    }

    /**
     * Sets externals definitions to be ignored or not during operations.
     *
     * <p>
     * For example, if external definitions are set to be ignored then a
     * checkout operation won't fetch them into a Working Copy.
     *
     * @param ignore
     *            <span class="javakeyword">true</span> to ignore externals
     *            definitions, <span class="javakeyword">false</span> - not to
     * @see #isIgnoreExternals()
     */
    public void setIgnoreExternals(boolean ignore) {
        this.ignoreExternals = ignore;
    }

    /**
     * Determines if externals definitions are ignored.
     *
     * @return <span class="javakeyword">true</span> if ignored, otherwise <span
     *         class="javakeyword">false</span>
     * @see #setIgnoreExternals(boolean)
     */
    public boolean isIgnoreExternals() {
        return this.ignoreExternals;
    }

    /**
     * Sets (or unsets) all conflicted working files to be untouched by update
     * and merge operations.
     *
     * <p>
     * By default when a file receives changes from the repository that are in
     * conflict with local edits, an update operation places two sections for
     * each conflicting snatch into the working file one of which is a user's
     * local edit and the second is the one just received from the repository.
     * Like this:
     *
     * <pre class="javacode">
     * <<<<<<< .mine
     * user's text
     * =======
     * received text
     * >>>>>>> .r2
     * </pre>
     *
     * <br />
     * Also the operation creates three temporary files that appear in the same
     * directory as the working file. Now if you call this method with
     * <code>leave</code> set to <span class="javakeyword">true</span>, an
     * update will still create temporary files but won't place those two
     * sections into your working file. And this behaviour also concerns merge
     * operations: any merging to a conflicted file will be prevented. In
     * addition if there is any registered event handler for an
     * <b>SVNDiffClient</b> or <b>SVNUpdateClient</b> instance then the handler
     * will be dispatched an event with the status type set to
     * {@link SVNStatusType#CONFLICTED_UNRESOLVED}.
     *
     * <p>
     * The default value is <span class="javakeyword">false</span> until a
     * caller explicitly changes it calling this method.
     *
     * @param leave
     *            <span class="javakeyword">true</span> to prevent conflicted
     *            files from merging (all merging operations will be skipped),
     *            otherwise <span class="javakeyword">false</span>
     * @see #isLeaveConflictsUnresolved()
     * @see SVNUpdateClient
     * @see SVNDiffClient
     * @see ISVNEventHandler
     * @deprecated this method should not be used anymore
     */
    public void setLeaveConflictsUnresolved(boolean leave) {
        this.leaveConflictsUnresolved = leave;
    }

    /**
     * Determines if conflicted files should be left unresolved preventing from
     * merging their contents during update and merge operations.
     *
     * @return <span class="javakeyword">true</span> if conflicted files are set
     *         to be prevented from merging, <span
     *         class="javakeyword">false</span> if there's no such restriction
     * @see #setLeaveConflictsUnresolved(boolean)
     * @deprecated this method should not be used anymore
     */
    public boolean isLeaveConflictsUnresolved() {
        return leaveConflictsUnresolved;
    }

    /**
     * Sets an event handler for this object. This event handler will be
     * dispatched {@link SVNEvent} objects to provide detailed information about
     * actions and progress state of version control operations performed by
     * <b>do</b>*<b>()</b> methods of <b>SVN</b>*<b>Client</b> classes.
     *
     * @param dispatcher
     *            an event handler
     */
    public void setEventHandler(ISVNEventHandler dispatcher) {
        getOperationsFactory().setEventHandler(dispatcher);
    }

    /**
     * Sets a path list handler implementation to this object.
     *
     * @param handler
     *            handler implementation
     * @since 1.2.0
     */
    public void setPathListHandler(ISVNPathListHandler handler) {
        this.pathListHandler = handler;
    }
    
    public ISVNPathListHandler getPathListHandler() {
        return this.pathListHandler;
    }

    /**
     * Sets a logger to write debug log information to.
     *
     * @param log
     *            a debug logger
     */
    public void setDebugLog(ISVNDebugLog log) {
        if (log == null) {
            log = SVNDebugLog.getDefaultLog();
        }
        this.debugLog = log;
    }

    /**
     * Returns the debug logger currently in use.
     *
     * <p>
     * If no debug logger has been specified by the time this call occurs, a
     * default one (returned by
     * <code>org.tmatesoft.svn.util.SVNDebugLog.getDefaultLog()</code>) will be
     * created and used.
     *
     * @return a debug logger
     */
    public ISVNDebugLog getDebugLog() {
        return debugLog;
    }

    /**
     * Returns the root of the repository.
     *
     * <p/>
     * If <code>path</code> is not <span class="javakeyword">null</span> and
     * <code>pegRevision</code> is either {@link SVNRevision#WORKING} or
     * {@link SVNRevision#BASE}, then attempts to fetch the repository root from
     * the working copy represented by <code>path</code>. If these conditions
     * are not met or if the repository root is not recorded in the working
     * copy, then a repository connection is established and the repository root
     * is fetched from the session.
     *
     * <p/>
     * When fetching the repository root from the working copy and if
     * <code>access</code> is <span class="javakeyword">null</span>, a new
     * working copy access will be created and the working copy will be opened
     * non-recursively for reading only.
     *
     * <p/>
     * All necessary cleanup (session or|and working copy close) will be
     * performed automatically as the routine finishes.
     *
     * @param path
     *            working copy path
     * @param url
     *            repository url
     * @param pegRevision
     *            revision in which the target is valid
     * @param adminArea
     *            working copy administrative area object
     * @param access
     *            working copy access object
     * @return repository root url
     * @throws SVNException
     * @since 1.2.0
     *
     * @deprecated
     */
    public SVNURL getReposRoot(File path, SVNURL url, SVNRevision pegRevision, SVNAdminArea adminArea, SVNWCAccess access) throws SVNException {
        SVNBasicDelegate delegate = new SVNBasicDelegate(getOperationsFactory().getAuthenticationManager(), getOperationsFactory().getOptions());
        return delegate.getReposRoot(path, url, pegRevision, adminArea, access);
    }

    /**
     * Returns the root of the repository.
     *
     * <p/>
     * If <code>path</code> is not <span class="javakeyword">null</span> and
     * <code>pegRevision</code> is either {@link SVNRevision#WORKING} or
     * {@link SVNRevision#BASE}, then attempts to fetch the repository root from
     * the working copy represented by <code>path</code>. If these conditions
     * are not met or if the repository root is not recorded in the working
     * copy, then a repository connection is established and the repository root
     * is fetched from the session.
     *
     * <p/>
     * All necessary cleanup (session or|and working copy close) will be
     * performed automatically as the routine finishes.
     *
     * @param path
     *            working copy path
     * @param url
     *            repository url
     * @param pegRevision
     *            revision in which the target is valid
     * @return repository root url
     * @throws SVNException
     * @since 1.2.0
     *
     */
    public SVNURL getReposRoot(File path, SVNURL url, SVNRevision pegRevision) throws SVNException {
        SvnGetInfo info = getOperationsFactory().createGetInfo();
        if (path != null) {
            info.setSingleTarget(SvnTarget.fromFile(path, pegRevision));
        } else {
            info.setSingleTarget(SvnTarget.fromURL(url, pegRevision));
        }
        info.setDepth(SVNDepth.EMPTY);
        SvnInfo i = info.run();
        return i != null ? i.getRepositoryRootUrl() : null;
    }

    /**
     * Removes or adds a path prefix. This method is not intended for users
     * (from an API point of view).
     *
     * @param prefix
     *            a path prefix
     * @deprecated
     */
    public void setEventPathPrefix(String prefix) {
    }
    
    public SvnOperationFactory getOperationsFactory() {
        return this.operationFactory;
    }

}