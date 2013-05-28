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
import java.util.Collection;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc2.compat.SvnCodec;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver;
import org.tmatesoft.svn.core.wc2.SvnGetStatus;
import org.tmatesoft.svn.core.wc2.SvnStatus;
import org.tmatesoft.svn.core.wc2.SvnTarget;

/**
 * The <b>SVNStatusClient</b> class provides methods for obtaining information
 * on the status of Working Copy items. The functionality of
 * <b>SVNStatusClient</b> corresponds to the <code>'svn status'</code> command of the native SVN
 * command line client.
 * 
 * <p>
 * One of the main advantages of <b>SVNStatusClient</b> lies in that fact that
 * for each processed item the status information is collected and put into an
 * <b>SVNStatus</b> object. Further there are two ways how this object can be
 * passed to a developer (depending on the version of the <b>doStatus()</b>
 * method that was invoked):
 * <ol>
 * <li>the <b>SVNStatus</b> can be passed to a developer's status handler (that
 * should implement <b>ISVNStatusHandler</b>) in which the developer retrieves
 * status information and decides how to interprete that info;
 * <li>another way is that an appropriate <b>doStatus()</b> method just returns
 * that <b>SVNStatus</b> object.
 * </ol>
 * Those methods that match the first variant can be called recursively -
 * obtaining status information for all child entries, the second variant just
 * the reverse - methods are called non-recursively and allow to get status info
 * on a single item.
 * 
 * @version 1.3
 * @author TMate Software Ltd.
 * @since 1.2
 * @see ISVNStatusHandler
 * @see SVNStatus
 * @see <a target="_top" href="http://svnkit.com/kb/examples/">Examples</a>
 */
public class SVNStatusClient extends SVNBasicClient {

    private ISVNStatusFileProvider myFilesProvider;

    /**
     * Constructs and initializes an <b>SVNStatusClient</b> object with the
     * specified run-time configuration and authentication drivers.
     * 
     * <p>
     * If <code>options</code> is <span class="javakeyword">null</span>, then
     * this <b>SVNStatusClient</b> will be using a default run-time
     * configuration driver which takes client-side settings from the default
     * SVN's run-time configuration area but is not able to change those
     * settings (read more on {@link ISVNOptions} and {@link SVNWCUtil}).
     * 
     * <p>
     * If <code>authManager</code> is <span class="javakeyword">null</span>,
     * then this <b>SVNStatusClient</b> will be using a default authentication
     * and network layers driver (see
     * {@link SVNWCUtil#createDefaultAuthenticationManager()}) which uses
     * server-side settings and auth storage from the default SVN's run-time
     * configuration area (or system properties if that area is not found).
     * 
     * @param authManager
     *            an authentication and network layers driver
     * @param options
     *            a run-time configuration options driver
     */
    public SVNStatusClient(ISVNAuthenticationManager authManager, ISVNOptions options) {
        super(authManager, options);
        setFilesProvider(null);
    }

    /**
     * Constructs and initializes an <b>SVNStatusClient</b> object with the
     * specified run-time configuration and repository pool object.
     * 
     * <p/>
     * If <code>options</code> is <span class="javakeyword">null</span>, then
     * this <b>SVNStatusClient</b> will be using a default run-time
     * configuration driver which takes client-side settings from the default
     * SVN's run-time configuration area but is not able to change those
     * settings (read more on {@link ISVNOptions} and {@link SVNWCUtil}).
     * 
     * <p/>
     * If <code>repositoryPool</code> is <span class="javakeyword">null</span>,
     * then {@link org.tmatesoft.svn.core.io.SVNRepositoryFactory} will be used
     * to create {@link SVNRepository repository access objects}.
     * 
     * @param repositoryPool
     *            a repository pool object
     * @param options
     *            a run-time configuration options driver
     */

    public SVNStatusClient(ISVNRepositoryPool repositoryPool, ISVNOptions options) {
        super(repositoryPool, options);
        setFilesProvider(null);
    }

    /**
     * Collects status information on Working Copy items and passes it to a
     * <code>handler</code>.
     * 
     * @param path
     *            local item's path
     * @param recursive
     *            relevant only if <code>path</code> denotes a directory: <span
     *            class="javakeyword">true</span> to obtain status info
     *            recursively for all child entries, <span
     *            class="javakeyword">false</span> only for items located
     *            immediately in the directory itself
     * @param remote
     *            <span class="javakeyword">true</span> to check up the status
     *            of the item in the repository, that will tell if the local
     *            item is out-of-date (like <i>'-u'</i> option in the SVN
     *            client's <code>'svn status'</code> command), otherwise <span
     *            class="javakeyword">false</span>
     * @param reportAll
     *            <span class="javakeyword">true</span> to collect status
     *            information on those items that are in a <i>'normal'</i> state
     *            (unchanged), otherwise <span class="javakeyword">false</span>
     * @param includeIgnored
     *            <span class="javakeyword">true</span> to force the operation
     *            to collect information on items that were set to be ignored
     *            (like <i>'--no-ignore'</i> option in the SVN client's <i>'svn
     *            status'</i> command to disregard default and
     *            <i>'svn:ignore'</i> property ignores), otherwise <span
     *            class="javakeyword">false</span>
     * @param handler
     *            a caller's status handler that will be involved in processing
     *            status information
     * @return the revision number the status information was collected against
     * @throws SVNException
     * @see ISVNStatusHandler
     * @deprecated use
     *             {@link #doStatus(File, SVNRevision, SVNDepth, boolean, boolean, boolean, boolean, ISVNStatusHandler, Collection)}
     *             instead
     */
    public long doStatus(File path, boolean recursive, boolean remote, boolean reportAll, boolean includeIgnored, ISVNStatusHandler handler) throws SVNException {
        return doStatus(path, SVNRevision.HEAD, SVNDepth.fromRecurse(recursive), remote, reportAll, includeIgnored, false, handler, null);
    }

    /**
     * Collects status information on Working Copy items and passes it to a
     * <code>handler</code>.
     * 
     * <p>
     * Calling this method is equivalent to
     * 
     * <code>doStatus(path, SVNRevision.HEAD, recursive, remote, reportAll, includeIgnored, collectParentExternals, handler)</code>.
     * 
     * @param path
     *            local item's path
     * @param recursive
     *            relevant only if <code>path</code> denotes a directory: <span
     *            class="javakeyword">true</span> to obtain status info
     *            recursively for all child entries, <span
     *            class="javakeyword">false</span> only for items located
     *            immediately in the directory itself
     * @param remote
     *            <span class="javakeyword">true</span> to check up the status
     *            of the item in the repository, that will tell if the local
     *            item is out-of-date (like <i>'-u'</i> option in the SVN
     *            client's <code>'svn status'</code> command), otherwise <span
     *            class="javakeyword">false</span>
     * @param reportAll
     *            <span class="javakeyword">true</span> to collect status
     *            information on all items including those ones that are in a
     *            <i>'normal'</i> state (unchanged), otherwise <span
     *            class="javakeyword">false</span>
     * @param includeIgnored
     *            <span class="javakeyword">true</span> to force the operation
     *            to collect information on items that were set to be ignored
     *            (like <i>'--no-ignore'</i> option in the SVN client's <code>'svn status'</code>
     *            command to disregard default and <i>'svn:ignore'</i> property
     *            ignores), otherwise <span class="javakeyword">false</span>
     * @param collectParentExternals
     *            <span class="javakeyword">false</span> to make the operation
     *            ignore information on externals definitions (like
     *            <i>'--ignore-externals'</i> option in the SVN client's <code>'svn status'</code>
     *            command), otherwise <span class="javakeyword">true</span>
     * @param handler
     *            a caller's status handler that will be involved in processing
     *            status information
     * @return the revision number the status information was collected against
     * @throws SVNException
     * @deprecated use
     *             {@link #doStatus(File, SVNRevision, SVNDepth, boolean, boolean, boolean, boolean, ISVNStatusHandler, Collection)}
     *             instead
     */
    public long doStatus(File path, boolean recursive, boolean remote, boolean reportAll, boolean includeIgnored, boolean collectParentExternals, final ISVNStatusHandler handler) throws SVNException {
        return doStatus(path, SVNRevision.HEAD, SVNDepth.fromRecurse(recursive), remote, reportAll, includeIgnored, collectParentExternals, handler, null);
    }

    /**
     * Collects status information on Working Copy items and passes it to a
     * <code>handler</code>.
     * 
     * @param path
     *            local item's path
     * @param revision
     *            if <code>remote</code> is <span
     *            class="javakeyword">true</span> this revision is used to
     *            calculate status against
     * @param recursive
     *            relevant only if <code>path</code> denotes a directory: <span
     *            class="javakeyword">true</span> to obtain status info
     *            recursively for all child entries, <span
     *            class="javakeyword">false</span> only for items located
     *            immediately in the directory itself
     * @param remote
     *            <span class="javakeyword">true</span> to check up the status
     *            of the item in the repository, that will tell if the local
     *            item is out-of-date (like <i>'-u'</i> option in the SVN
     *            client's <code>'svn status'</code> command), otherwise <span
     *            class="javakeyword">false</span>
     * @param reportAll
     *            <span class="javakeyword">true</span> to collect status
     *            information on all items including those ones that are in a
     *            <i>'normal'</i> state (unchanged), otherwise <span
     *            class="javakeyword">false</span>
     * @param includeIgnored
     *            <span class="javakeyword">true</span> to force the operation
     *            to collect information on items that were set to be ignored
     *            (like <i>'--no-ignore'</i> option in the SVN client's <code>'svn status'</code>
     *            command to disregard default and <i>'svn:ignore'</i> property
     *            ignores), otherwise <span class="javakeyword">false</span>
     * @param collectParentExternals
     *            <span class="javakeyword">false</span> to make the operation
     *            ignore information on externals definitions (like
     *            <i>'--ignore-externals'</i> option in the SVN client's <code>'svn status'</code>
     *            command), otherwise <span class="javakeyword">true</span>
     * @param handler
     *            a caller's status handler that will be involved in processing
     *            status information
     * @return the revision number the status information was collected against
     * @throws SVNException
     * @deprecated use
     *             {@link #doStatus(File, SVNRevision, SVNDepth, boolean, boolean, boolean, boolean, ISVNStatusHandler, Collection)}
     *             instead
     */
    public long doStatus(File path, SVNRevision revision, boolean recursive, boolean remote, boolean reportAll, boolean includeIgnored, boolean collectParentExternals, final ISVNStatusHandler handler)
            throws SVNException {
        return doStatus(path, revision, SVNDepth.fromRecurse(recursive), remote, reportAll, includeIgnored, collectParentExternals, handler, null);
    }

    /**
     * Given a <code>path</code> to a working copy directory (or single file),
     * calls <code>handler</code> with a set of {@link SVNStatus} objects which
     * describe the status of the <code>path</code>, and its children (recursing
     * according to <code>depth</code>).
     * 
     * <p/>
     * If <code>reportAll</code> is set, retrieves all entries; otherwise,
     * retrieves only "interesting" entries (local modifications and/or out of
     * date).
     * 
     * <p/>
     * If <code>remote</code> is set, contacts the repository and augments the
     * status objects with information about out-of-dateness (with respect to
     * <code>revision</code>).
     * 
     * <p/>
     * If {@link #isIgnoreExternals()} returns <span
     * class="javakeyword">false</span>, then recurses into externals
     * definitions (if any exist and <code>depth</code> is either
     * {@link SVNDepth#INFINITY} or {@link SVNDepth#UNKNOWN}) after handling the
     * main target. This calls the client notification handler (
     * {@link ISVNEventHandler}) with the {@link SVNEventAction#STATUS_EXTERNAL}
     * action before handling each externals definition, and with
     * {@link SVNEventAction#STATUS_COMPLETED} after each.
     * 
     * <p/>
     * <code>changeLists</code> is a collection of <code>String</code>
     * changelist names, used as a restrictive filter on items whose statuses
     * are reported; that is, doesn't report status about any item unless it's a
     * member of one of those changelists. If <code>changeLists</code> is empty
     * (or <span class="javakeyword">null</span>), no changelist filtering
     * occurs.
     * 
     * @param path
     *            working copy path
     * @param revision
     *            if <code>remote</code> is <span
     *            class="javakeyword">true</span>, status is calculated against
     *            this revision
     * @param depth
     *            tree depth to process
     * @param remote
     *            <span class="javakeyword">true</span> to check up the status
     *            of the item in the repository, that will tell if the local
     *            item is out-of-date (like <i>'-u'</i> option in the SVN
     *            client's <code>'svn status'</code> command), otherwise <span
     *            class="javakeyword">false</span>
     * @param reportAll
     *            <span class="javakeyword">true</span> to collect status
     *            information on all items including those ones that are in a
     *            <i>'normal'</i> state (unchanged), otherwise <span
     *            class="javakeyword">false</span>
     * @param includeIgnored
     *            <span class="javakeyword">true</span> to force the operation
     *            to collect information on items that were set to be ignored
     *            (like <i>'--no-ignore'</i> option in the SVN client's <code>'svn status'</code>
     *            command to disregard default and <i>'svn:ignore'</i> property
     *            ignores), otherwise <span class="javakeyword">false</span>
     * @param collectParentExternals
     *            obsolete (not used)
     * @param handler
     *            a caller's status handler that will be involved in processing
     *            status information
     * @param changeLists
     *            collection with changelist names
     * @return returns the actual revision against which the working copy was
     *         compared; the return value is not meaningful (-1) unless
     *         <code>remote</code> is set
     * @throws SVNException
     * @since 1.2, SVN 1.5
     */
    public long doStatus(File path, SVNRevision revision, SVNDepth depth, boolean remote, boolean reportAll, boolean includeIgnored, boolean collectParentExternals, final ISVNStatusHandler handler,
            final Collection<String> changeLists) throws SVNException {
        
        final SvnGetStatus getStatus = getOperationsFactory().createGetStatus();
        getStatus.setReportExternals(!isIgnoreExternals());
        getStatus.setFileListHook(SvnCodec.fileListHook(myFilesProvider));
        getStatus.setApplicalbeChangelists(changeLists);
        getStatus.setCollectParentExternals(collectParentExternals);
        getStatus.setDepth(depth);
        getStatus.setRevision(revision);
        getStatus.setRemote(remote);
        getStatus.setReportAll(reportAll);
        getStatus.setReportIgnored(includeIgnored);
        getStatus.setSingleTarget(SvnTarget.fromFile(path));
        
        getStatus.setReceiver(new ISvnObjectReceiver<SvnStatus>() {
            public void receive(SvnTarget target, SvnStatus status) throws SVNException {
                SVNWCContext context = getStatus.getOperationFactory().getWcContext();
                handler.handleStatus(SvnCodec.status(context, status));
            }
        });
        
        getStatus.run();
        
        return getStatus.getRemoteRevision();
    }

    /**
     * Collects status information on a single Working Copy item.
     * 
     * @param path
     *            local item's path
     * @param remote
     *            <span class="javakeyword">true</span> to check up the status
     *            of the item in the repository, that will tell if the local
     *            item is out-of-date (like <i>'-u'</i> option in the SVN
     *            client's <code>'svn status'</code> command), otherwise <span
     *            class="javakeyword">false</span>
     * @return an <b>SVNStatus</b> object representing status information for
     *         the item
     * @throws SVNException
     */
    public SVNStatus doStatus(final File path, boolean remote) throws SVNException {
        return doStatus(path, remote, false);
    }

    /**
     * Collects status information on a single Working Copy item.
     * 
     * @param path
     *            local item's path
     * @param remote
     *            <span class="javakeyword">true</span> to check up the status
     *            of the item in the repository, that will tell if the local
     *            item is out-of-date (like <i>'-u'</i> option in the SVN
     *            client's <code>'svn status'</code> command), otherwise <span
     *            class="javakeyword">false</span>
     * @param collectParentExternals
     *            <span class="javakeyword">false</span> to make the operation
     *            ignore information on externals definitions (like
     *            <i>'--ignore-externals'</i> option in the SVN client's <code>'svn status'</code>
     *            command), otherwise <span class="javakeyword">false</span>
     * @return an <b>SVNStatus</b> object representing status information for
     *         the item
     * @throws SVNException
     */
    public SVNStatus doStatus(File path, boolean remote, boolean collectParentExternals) throws SVNException {
        final SVNStatus[] result = new SVNStatus[] {null};
        final File absPath = path.getAbsoluteFile();
        ISVNStatusHandler handler = new ISVNStatusHandler() {
            public void handleStatus(SVNStatus status) {
                if (absPath.equals(status.getFile())) {
                    if (result[0] != null && result[0].getContentsStatus() == SVNStatusType.STATUS_EXTERNAL && absPath.isDirectory()) {
                        result[0] = status;
                        result[0].markExternal();
                    } else if (result[0] == null) {
                        result[0] = status;
                    }
                }
            }
        };
        doStatus(absPath, SVNRevision.HEAD, SVNDepth.EMPTY, remote, true, true, collectParentExternals, handler, null);
        return result[0];
    }

    public void setFilesProvider(ISVNStatusFileProvider filesProvider) {
        myFilesProvider = filesProvider;
    }
}
