package org.tmatesoft.svn.core.wc.admin;

import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.DefaultSVNRepositoryPool;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.ISVNRepositoryPool;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.util.ISVNDebugLog;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

/** 
 * The <b>SVNBasicClient</b> is the base class of all 
 * <b>SVN</b>*<b>Client</b> classes that provides a common interface
 * and realization.
 * <p>
 * All of <b>SVN</b>*<b>Client</b> classes use inherited methods of
 * <b>SVNBasicClient</b> to access Working Copies metadata, to create 
 * a driver object to access a repository if it's necessary, etc. In addition
 * <b>SVNBasicClient</b> provides some interface methods  - such as those
 * that allow you to set your {@link ISVNEventHandler event handler}, 
 * obtain run-time configuration options, and others. 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public class SVNAdminBasicClient implements ISVNEventHandler {
	private ISVNRepositoryPool myRepositoryPool;
	private ISVNOptions myOptions;
	private ISVNEventHandler myEventDispatcher;
	private ISVNDebugLog myDebugLog;
	
	protected SVNRepository createRepository(SVNURL url, String uuid,
			boolean mayReuse) throws SVNException {
		SVNRepository repository = null;
		if (myRepositoryPool == null) {
			repository = SVNRepositoryFactory.create(url, null);
		} else {
			repository = myRepositoryPool.createRepository(url, mayReuse);
		}
		if (uuid != null) {
			String reposUUID = repository.getRepositoryUUID(true);
			if (!uuid.equals(reposUUID)) {
				SVNErrorMessage err = SVNErrorMessage
						.create(
								SVNErrorCode.RA_UUID_MISMATCH,
								"Repository UUID ''{0}'' doesn''t match expected UUID ''{1}''",
								new Object[] { reposUUID, uuid });
				SVNErrorManager.error(err, SVNLogType.WC);
			}
		}
		repository.setDebugLog(getDebugLog());
		repository.setCanceller(getEventDispatcher());
		return repository;
	}

	protected void dispatchEvent(SVNEvent event, double progress)
			throws SVNException {
		if (myEventDispatcher != null) {
			try {
				myEventDispatcher.handleEvent(event, progress);
			} catch (SVNException e) {
				throw e;
			} catch (Throwable th) {
				SVNDebugLog.getDefaultLog().logSevere(SVNLogType.WC, th);
				SVNErrorMessage err = SVNErrorMessage
						.create(SVNErrorCode.UNKNOWN,
								"Error while dispatching event: {0}",
								new Object[] { th.getMessage() },
								SVNErrorMessage.TYPE_ERROR, th);
				SVNErrorManager.error(err, th, SVNLogType.DEFAULT);
			}
		}
	}

	/** 
	 * Sets a logger to write debug log information to.
	 * @param log a debug logger
	 */
	public void setDebugLog(ISVNDebugLog log) {
		myDebugLog = log;
	}

	/** 
	 * Gets run-time configuration options used by this object.
	 * @return the run-time options being in use
	 */
	public ISVNOptions getOptions() {
		return myOptions;
	}

	/** 
	 * Redirects this call to the registered event handler (if any).
	 * @throws SVNCancelException  if the current operation
	 * was cancelled
	 */
	public void checkCancelled() throws SVNCancelException {
		if (myEventDispatcher != null) {
			myEventDispatcher.checkCancelled();
		}
	}

	/** 
	 * Sets an event handler for this object. This event handler
	 * will be dispatched {@link SVNEvent} objects to provide 
	 * detailed information about actions and progress state 
	 * of version control operations performed by <b>do</b>*<b>()</b>
	 * methods of <b>SVN</b>*<b>Client</b> classes.
	 * @param dispatcher an event handler
	 */
	public void setEventHandler(ISVNEventHandler dispatcher) {
		myEventDispatcher = dispatcher;
	}

	protected ISVNEventHandler getEventDispatcher() {
		return myEventDispatcher;
	}

	protected SVNAdminBasicClient(final ISVNAuthenticationManager authManager,
			ISVNOptions options) {
		this(new DefaultSVNRepositoryPool(authManager == null ? SVNWCUtil
				.createDefaultAuthenticationManager() : authManager, options,
				0, false), options);
	}

	protected SVNAdminBasicClient(ISVNRepositoryPool repositoryPool,
			ISVNOptions options) {
		myRepositoryPool = repositoryPool;
		setOptions(options);
	}

	/** 
	 * Sets run-time global configuration options to this object.
	 * @param options  the run-time configuration options 
	 */
	public void setOptions(ISVNOptions options) {
		myOptions = options;
		if (myOptions == null) {
			myOptions = SVNWCUtil.createDefaultOptions(true);
		}
	}

	/** 
	 * Returns the debug logger currently in use.  
	 * <p>
	 * If no debug logger has been specified by the time this call occurs, 
	 * a default one (returned by <code>org.tmatesoft.svn.util.SVNDebugLog.getDefaultLog()</code>) 
	 * will be created and used.
	 * @return a debug logger
	 */
	public ISVNDebugLog getDebugLog() {
		if (myDebugLog == null) {
			return SVNDebugLog.getDefaultLog();
		}
		return myDebugLog;
	}

	/** 
	 * Dispatches events to the registered event handler (if any). 
	 * @param event       the current event
	 * @param progress    progress state (from 0 to 1)
	 * @throws SVNException
	 */
	public void handleEvent(SVNEvent event, double progress)
			throws SVNException {
		dispatchEvent(event, progress);
	}
}
