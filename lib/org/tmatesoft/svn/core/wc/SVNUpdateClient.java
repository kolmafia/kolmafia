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
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.wc2.compat.SvnCodec;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc2.SvnCanonicalizeUrls;
import org.tmatesoft.svn.core.wc2.SvnCheckout;
import org.tmatesoft.svn.core.wc2.SvnExport;
import org.tmatesoft.svn.core.wc2.SvnRelocate;
import org.tmatesoft.svn.core.wc2.SvnSwitch;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.core.wc2.SvnUpdate;

/**
 * This class provides methods which allow to check out, update, switch and
 * relocate a Working Copy as well as export an unversioned directory or file
 * from a repository.
 * 
 * <p>
 * Here's a list of the <b>SVNUpdateClient</b>'s methods matched against
 * corresponing commands of the SVN command line client:
 * 
 * <table cellpadding="3" cellspacing="1" border="0" width="40%" bgcolor="#999933">
 * <tr bgcolor="#ADB8D9" align="left">
 * <td><b>SVNKit</b></td>
 * <td><b>Subversion</b></td>
 * </tr>
 * <tr bgcolor="#EAEAEA" align="left">
 * <td>doCheckout()</td>
 * <td>'svn checkout'</td>
 * </tr>
 * <tr bgcolor="#EAEAEA" align="left">
 * <td>doUpdate()</td>
 * <td>'svn update'</td>
 * </tr>
 * <tr bgcolor="#EAEAEA" align="left">
 * <td>doSwitch()</td>
 * <td>'svn switch'</td>
 * </tr>
 * <tr bgcolor="#EAEAEA" align="left">
 * <td>doRelocate()</td>
 * <td>'svn switch --relocate oldURL newURL'</td>
 * </tr>
 * <tr bgcolor="#EAEAEA" align="left">
 * <td>doExport()</td>
 * <td>'svn export'</td>
 * </tr>
 * </table>
 * 
 * @version 1.3
 * @author TMate Software Ltd.
 * @since 1.2
 * @see <a target="_top" href="http://svnkit.com/kb/examples/">Examples</a>
 */
public class SVNUpdateClient extends SVNBasicClient {

    private ISVNExternalsHandler myExternalsHandler;
    private boolean updateLocksOnDemand;
    private boolean exportExpandsKeywords;

    /**
     * Constructs and initializes an <b>SVNUpdateClient</b> object with the
     * specified run-time configuration and authentication drivers.
     * 
     * <p>
     * If <code>options</code> is <span class="javakeyword">null</span>, then
     * this <b>SVNUpdateClient</b> will be using a default run-time
     * configuration driver which takes client-side settings from the default
     * SVN's run-time configuration area but is not able to change those
     * settings (read more on {@link ISVNOptions} and {@link SVNWCUtil}).
     * 
     * <p>
     * If <code>authManager</code> is <span class="javakeyword">null</span>,
     * then this <b>SVNUpdateClient</b> will be using a default authentication
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
    public SVNUpdateClient(ISVNAuthenticationManager authManager, ISVNOptions options) {
        super(authManager, options);
        setExternalsHandler(null);
    }

    /**
     * Constructs and initializes an <b>SVNUpdateClient</b> object with the
     * specified run-time configuration and authentication drivers.
     * 
     * <p/>
     * If <code>options</code> is <span class="javakeyword">null</span>, then
     * this <b>SVNUpdateClient</b> will be using a default run-time
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
    public SVNUpdateClient(ISVNRepositoryPool repositoryPool, ISVNOptions options) {
        super(repositoryPool, options);
        setExternalsHandler(null);
    }

    /**
     * Sets an externals handler to be used by this client object.
     * 
     * @param externalsHandler
     *            user's implementation of {@link ISVNExternalsHandler}
     * @see #getExternalsHandler()
     * @since 1.2
     */
    public void setExternalsHandler(ISVNExternalsHandler externalsHandler) {
        myExternalsHandler = externalsHandler;
    }

    /**
     * Returns an externals handler used by this update client.
     * 
     * <p/>
     * If no user's handler is provided then
     * {@link ISVNExternalsHandler#DEFAULT} is returned and used by this client
     * object by default.
     * 
     * <p/>
     * For more information what externals handlers are for, please, refer to
     * {@link ISVNExternalsHandler}.
     * 
     * @return externals handler being in use
     * @see #setExternalsHandler(ISVNExternalsHandler)
     * @since 1.2
     */
    public ISVNExternalsHandler getExternalsHandler() {
        if (myExternalsHandler == null) {
            return ISVNExternalsHandler.DEFAULT;
        }
        return myExternalsHandler;
    }

    /**
     * Brings the Working Copy item up-to-date with repository changes at the
     * specified revision.
     * 
     * <p>
     * As a revision <b>SVNRevision</b>'s pre-defined constant fields can be
     * used. For example, to update the Working Copy to the latest revision of
     * the repository use {@link SVNRevision#HEAD HEAD}.
     * 
     * @param file
     *            the Working copy item to be updated
     * @param revision
     *            the desired revision against which the item will be updated
     * @param recursive
     *            if <span class="javakeyword">true</span> and <code>file</code>
     *            is a directory then the entire tree will be updated, otherwise
     *            if <span class="javakeyword">false</span> - only items located
     *            immediately in the directory itself
     * @return the revision number to which <code>file</code> was updated to
     * @throws SVNException
     * @deprecated use
     *             {@link #doUpdate(File, SVNRevision, SVNDepth, boolean, boolean)}
     *             instead
     */
    public long doUpdate(File file, SVNRevision revision, boolean recursive) throws SVNException {
        return doUpdate(file, revision, SVNDepth.fromRecurse(recursive), false, false);
    }

    /**
     * @param file
     * @param revision
     * @param recursive
     * @param force
     * @return actual revision number
     * @throws SVNException
     * @deprecated use
     *             {@link #doUpdate(File, SVNRevision, SVNDepth, boolean, boolean)}
     *             instead
     */
    public long doUpdate(File file, SVNRevision revision, boolean recursive, boolean force) throws SVNException {
        return doUpdate(file, revision, SVNDepth.fromRecurse(recursive), force, false);
    }

    /**
     * Updates working trees <code>paths</code> to <code>revision</code>.
     * Unversioned paths that are direct children of a versioned path will cause
     * an update that attempts to add that path, other unversioned paths are
     * skipped.
     * 
     * <p/>
     * <code>revision</code> must represent a valid revision number (
     * {@link SVNRevision#getNumber()} >= 0), or date (
     * {@link SVNRevision#getDate()} != <span class="javakeyword">true</span>),
     * or be equal to {@link SVNRevision#HEAD}. If <code>revision</code> does
     * not meet these requirements, an exception with the error code
     * {@link SVNErrorCode#CLIENT_BAD_REVISION} is thrown.
     * 
     * <p/>
     * The paths in <code>paths</code> can be from multiple working copies from
     * multiple repositories, but even if they all come from the same repository
     * there is no guarantee that revision represented by
     * {@link SVNRevision#HEAD} will remain the same as each path is updated.
     * 
     * <p/>
     * If externals are {@link #isIgnoreExternals() ignored}, doesn't process
     * externals definitions as part of this operation.
     * 
     * <p/>
     * If <code>depth</code> is {@link SVNDepth#INFINITY}, updates fully
     * recursively. Else if it is {@link SVNDepth#IMMEDIATES} or
     * {@link SVNDepth#FILES}, updates each target and its file entries, but not
     * its subdirectories. Else if {@link SVNDepth#EMPTY}, updates exactly each
     * target, nonrecursively (essentially, updates the target's properties).
     * 
     * <p/>
     * If <code>depth</code> is {@link SVNDepth#UNKNOWN}, takes the working
     * depth from <code>paths</code> and then behaves as described above.
     * 
     * <p/>
     * If <code>depthIsSticky</code> is set and <code>depth</code> is not
     * {@link SVNDepth#UNKNOWN}, then in addition to updating <code>paths</code>
     * , also sets their sticky ambient depth value to <code>depth</code>.
     * 
     * <p/>
     * If <code>allowUnversionedObstructions</code> is <span
     * class="javakeyword">true</span> then the update tolerates existing
     * unversioned items that obstruct added paths. Only obstructions of the
     * same type (file or dir) as the added item are tolerated. The text of
     * obstructing files is left as-is, effectively treating it as a user
     * modification after the update. Working properties of obstructing items
     * are set equal to the base properties. If
     * <code>allowUnversionedObstructions</code> is <span
     * class="javakeyword">false</span> then the update will abort if there are
     * any unversioned obstructing items.
     * 
     * <p/>
     * If the caller's {@link ISVNEventHandler} is non-<span
     * class="javakeyword">null</span>, it is invoked for each item handled by
     * the update, and also for files restored from text-base. Also
     * {@link ISVNEventHandler#checkCancelled()} will be used at various places
     * during the update to check whether the caller wants to stop the update.
     * 
     * <p/>
     * This operation requires repository access (in case the repository is not
     * on the same machine, network connection is established).
     * 
     * @param paths
     *            working copy paths
     * @param revision
     *            revision to update to
     * @param depth
     *            tree depth to update
     * @param allowUnversionedObstructions
     *            flag that allows tolerating unversioned items during update
     * @param depthIsSticky
     *            flag that controls whether the requested depth should be
     *            written to the working copy
     * @return an array of <code>long</code> revisions with each element set to
     *         the revision to which <code>revision</code> was resolved
     * @throws SVNException
     * @since 1.2, SVN 1.5
     */
    public long[] doUpdate(File[] paths, SVNRevision revision, SVNDepth depth, boolean allowUnversionedObstructions, boolean depthIsSticky) throws SVNException {
        return doUpdate(paths, revision, depth, allowUnversionedObstructions, depthIsSticky, false);
    }

    public long[] doUpdate(File[] paths, SVNRevision revision, SVNDepth depth, boolean allowUnversionedObstructions, boolean depthIsSticky, boolean makeParents) throws SVNException {
        if (paths == null || paths.length == 0) {
            return new long[0];
        }
        SvnUpdate up = getOperationsFactory().createUpdate();
        up.setUpdateLocksOnDemand(isUpdateLocksOnDemand());
        for (int i = 0; i < paths.length; i++) {
            up.addTarget(SvnTarget.fromFile(paths[i]));
        }
        up.setRevision(revision);
        up.setDepth(depth);
        up.setDepthIsSticky(depthIsSticky);
        up.setAllowUnversionedObstructions(allowUnversionedObstructions);
        up.setIgnoreExternals(isIgnoreExternals());
        up.setMakeParents(makeParents);
        up.setExternalsHandler(SvnCodec.externalsHandler(getExternalsHandler()));

        return up.run();
    }

    /**
     * Updates working copy <code></code> to <code>revision</code>. Unversioned
     * paths that are direct children of a versioned path will cause an update
     * that attempts to add that path, other unversioned paths are skipped.
     * 
     * <p/>
     * <code>revision</code> must represent a valid revision number (
     * {@link SVNRevision#getNumber()} >= 0), or date (
     * {@link SVNRevision#getDate()} != <span class="javakeyword">true</span>),
     * or be equal to {@link SVNRevision#HEAD}. If <code>revision</code> does
     * not meet these requirements, an exception with the error code
     * {@link SVNErrorCode#CLIENT_BAD_REVISION} is thrown.
     * 
     * <p/>
     * If externals are {@link #isIgnoreExternals() ignored}, doesn't process
     * externals definitions as part of this operation.
     * 
     * <p/>
     * If <code>depth</code> is {@link SVNDepth#INFINITY}, updates fully
     * recursively. Else if it is {@link SVNDepth#IMMEDIATES} or
     * {@link SVNDepth#FILES}, updates <code>path</code> and its file entries,
     * but not its subdirectories. Else if {@link SVNDepth#EMPTY}, updates
     * exactly <code>path</code>, nonrecursively (essentially, updates the
     * target's properties).
     * 
     * <p/>
     * If <code>depth</code> is {@link SVNDepth#UNKNOWN}, takes the working
     * depth from <code>path</code> and then behaves as described above.
     * 
     * <p/>
     * If <code>depthIsSticky</code> is set and <code>depth</code> is not
     * {@link SVNDepth#UNKNOWN}, then in addition to updating <code>path</code>,
     * also sets its sticky ambient depth value to <code>depth</codes>.
     * 
     * <p/>
     * If <code>allowUnversionedObstructions</code> is <span
     * class="javakeyword">true</span> then the update tolerates existing
     * unversioned items that obstruct added paths. Only obstructions of the
     * same type (file or dir) as the added item are tolerated. The text of
     * obstructing files is left as-is, effectively treating it as a user
     * modification after the update. Working properties of obstructing items
     * are set equal to the base properties. If
     * <code>allowUnversionedObstructions</code> is <span
     * class="javakeyword">false</span> then the update will abort if there are
     * any unversioned obstructing items.
     * 
     * <p/>
     * If the caller's {@link ISVNEventHandler} is non-<span
     * class="javakeyword">null</span>, it is invoked for each item handled by
     * the update, and also for files restored from text-base. Also
     * {@link ISVNEventHandler#checkCancelled()} will be used at various places
     * during the update to check whether the caller wants to stop the update.
     * 
     * <p/>
     * This operation requires repository access (in case the repository is not
     * on the same machine, network connection is established).
     * 
     * @param path
     *            working copy path
     * @param revision
     *            revision to update to
     * @param depth
     *            tree depth to update
     * @param allowUnversionedObstructions
     *            flag that allows tollerating unversioned items during update
     * @param depthIsSticky
     *            flag that controls whether the requested depth should be
     *            written to the working copy
     * @return revision to which <code>revision</code> was resolved
     * @throws SVNException
     * @since 1.2, SVN 1.5
     */
    public long doUpdate(File path, SVNRevision revision, SVNDepth depth, boolean allowUnversionedObstructions, boolean depthIsSticky) throws SVNException {
        long[] result = doUpdate(new File[] {path}, revision, depth, allowUnversionedObstructions, depthIsSticky);
        return result[0];
    }

    /**
     * Sets whether working copies should be locked on demand or not during an
     * update process.
     * 
     * <p>
     * For additional description, please, refer to
     * {@link #isUpdateLocksOnDemand()}.
     * 
     * @param locksOnDemand
     *            <span class="javakeyword">true</span> to make update lock a
     *            working copy tree on demand only (for those subdirectories
     *            only which will be changed by update)
     */
    public void setUpdateLocksOnDemand(boolean locksOnDemand) {
        this.updateLocksOnDemand = locksOnDemand;
    }

    /**
     * Says whether the entire working copy should be locked while updating or
     * not.
     * 
     * <p/>
     * If this method returns <span class="javakeyword">false</span>, then the
     * working copy will be closed for all paths involved in the update.
     * Otherwise only those working copy subdirectories will be locked, which
     * will be either changed by the update or which contain deleted files that
     * should be restored during the update; all other versioned subdirectories
     * than won't be touched by the update will remain opened for read only
     * access without locking.
     * 
     * <p/>
     * Locking working copies on demand is intended to improve update
     * performance for large working copies because even a no-op update on a
     * huge working copy always locks the entire tree by default. And locking a
     * working copy tree means opening special lock files for privileged access
     * for all subdirectories involved. This makes an update process work
     * slower. Locking wc on demand feature suggests such a workaround to
     * enhance update performance.
     * 
     * @return <span class="javakeyword">true</span> when locking wc on demand
     */
    public boolean isUpdateLocksOnDemand() {
        return this.updateLocksOnDemand;
    }

    /**
     * Updates the Working Copy item to mirror a new URL.
     * 
     * <p>
     * As a revision <b>SVNRevision</b>'s pre-defined constant fields can be
     * used. For example, to update the Working Copy to the latest revision of
     * the repository use {@link SVNRevision#HEAD HEAD}.
     * 
     * <p>
     * Calling this method is equivalent to
     * <code>doSwitch(file, url, SVNRevision.UNDEFINED, revision, recursive)</code>.
     * 
     * @param file
     *            the Working copy item to be switched
     * @param url
     *            the repository location as a target against which the item
     *            will be switched
     * @param revision
     *            the desired revision of the repository target
     * @param recursive
     *            if <span class="javakeyword">true</span> and <code>file</code>
     *            is a directory then the entire tree will be updated, otherwise
     *            if <span class="javakeyword">false</span> - only items located
     *            immediately in the directory itself
     * @return the revision number to which <code>file</code> was updated to
     * @throws SVNException
     * @deprecated use
     *             {@link #doSwitch(File, SVNURL, SVNRevision, SVNRevision, SVNDepth, boolean, boolean)}
     *             instead
     */
    public long doSwitch(File file, SVNURL url, SVNRevision revision, boolean recursive) throws SVNException {
        return doSwitch(file, url, SVNRevision.UNDEFINED, revision, SVNDepth.getInfinityOrFilesDepth(recursive), false, false);

    }

    /**
     * Updates the Working Copy item to mirror a new URL.
     * 
     * <p>
     * As a revision <b>SVNRevision</b>'s pre-defined constant fields can be
     * used. For example, to update the Working Copy to the latest revision of
     * the repository use {@link SVNRevision#HEAD HEAD}.
     * 
     * @param file
     *            the Working copy item to be switched
     * @param url
     *            the repository location as a target against which the item
     *            will be switched
     * @param pegRevision
     *            a revision in which <code>file</code> is first looked up in
     *            the repository
     * @param revision
     *            the desired revision of the repository target
     * @param recursive
     *            if <span class="javakeyword">true</span> and <code>file</code>
     *            is a directory then the entire tree will be updated, otherwise
     *            if <span class="javakeyword">false</span> - only items located
     *            immediately in the directory itself
     * @return the revision number to which <code>file</code> was updated to
     * @throws SVNException
     * @deprecated use
     *             {@link #doSwitch(File, SVNURL, SVNRevision, SVNRevision, SVNDepth, boolean, boolean)}
     *             instead
     */
    public long doSwitch(File file, SVNURL url, SVNRevision pegRevision, SVNRevision revision, boolean recursive) throws SVNException {
        return doSwitch(file, url, pegRevision, revision, SVNDepth.getInfinityOrFilesDepth(recursive), false, false);
    }

    /**
     * @param file
     * @param url
     * @param pegRevision
     * @param revision
     * @param recursive
     * @param force
     * @return actual revision number
     * @throws SVNException
     * @deprecated use
     *             {@link #doSwitch(File, SVNURL, SVNRevision, SVNRevision, SVNDepth, boolean, boolean)}
     *             instead
     */
    public long doSwitch(File file, SVNURL url, SVNRevision pegRevision, SVNRevision revision, boolean recursive, boolean force) throws SVNException {
        return doSwitch(file, url, pegRevision, revision, SVNDepth.getInfinityOrFilesDepth(recursive), force, false);
    }

    /**
     * Switches working tree <code>path</code> to <code>url</code>\
     * <code>pegRevision</code> at <code>revision</code>.
     * 
     * <p/>
     * Summary of purpose: this is normally used to switch a working directory
     * over to another line of development, such as a branch or a tag. Switching
     * an existing working directory is more efficient than checking out
     * <code>url</code> from scratch.
     * 
     * <p/>
     * <code>revision</code> must represent a valid revision number (
     * {@link SVNRevision#getNumber()} >= 0), or date (
     * {@link SVNRevision#getDate()} != <span class="javakeyword">true</span>),
     * or be equal to {@link SVNRevision#HEAD}. If <code>revision</code> does
     * not meet these requirements, an exception with the error code
     * {@link SVNErrorCode#CLIENT_BAD_REVISION} is thrown.
     * 
     * <p/>
     * If <code>depth</code> is {@link SVNDepth#INFINITY}, switches fully
     * recursively. Else if it is {@link SVNDepth#IMMEDIATES}, switches
     * <code>path</code> and its file children (if any), and switches
     * subdirectories but does not update them. Else if {@link SVNDepth#FILES},
     * switches just file children, ignoring subdirectories completely. Else if
     * {@link SVNDepth#EMPTY}, switches just <code>path</code> and touches
     * nothing underneath it.
     * 
     * <p/>
     * If <code>depthIsSticky</code> is set and <code>depth</code> is not
     * {@link SVNDepth#UNKNOWN}, then in addition to switching <code>path</code>
     * , also sets its sticky ambient depth value to <code>depth</code>.
     * 
     * <p/>
     * If externals are {@link #isIgnoreExternals() ignored}, doesn't process
     * externals definitions as part of this operation.
     * 
     * <p/>
     * If <code>allowUnversionedObstructions</code> is <span
     * class="javakeyword">true</span> then the switch tolerates existing
     * unversioned items that obstruct added paths. Only obstructions of the
     * same type (file or dir) as the added item are tolerated. The text of
     * obstructing files is left as-is, effectively treating it as a user
     * modification after the switch. Working properties of obstructing items
     * are set equal to the base properties. If
     * <code>allowUnversionedObstructions</code> is <span
     * class="javakeyword">false</span> then the switch will abort if there are
     * any unversioned obstructing items.
     * 
     * <p/>
     * If the caller's {@link ISVNEventHandler} is non-<span
     * class="javakeyword">null</span>, it is invoked for paths affected by the
     * switch, and also for files restored from text-base. Also
     * {@link ISVNEventHandler#checkCancelled()} will be used at various places
     * during the switch to check whether the caller wants to stop the switch.
     * 
     * <p/>
     * This operation requires repository access (in case the repository is not
     * on the same machine, network connection is established).
     * 
     * @param path
     *            the Working copy item to be switched
     * @param url
     *            the repository location as a target against which the item
     *            will be switched
     * @param pegRevision
     *            a revision in which <code>path</code> is first looked up in
     *            the repository
     * @param revision
     *            the desired revision of the repository target
     * @param depth
     *            tree depth to update
     * @param allowUnversionedObstructions
     *            flag that allows tollerating unversioned items during update
     * @param depthIsSticky
     *            flag that controls whether the requested depth should be
     *            written into the working copy
     * @return value of the revision to which the working copy was actually
     *         switched
     * @throws SVNException
     * @since 1.2, SVN 1.5
     */
    public long doSwitch(File path, SVNURL url, SVNRevision pegRevision, SVNRevision revision, SVNDepth depth, boolean allowUnversionedObstructions, boolean depthIsSticky) throws SVNException {
        return doSwitch(path, url, pegRevision, revision, depth, allowUnversionedObstructions, depthIsSticky, true);
    }
    
    public long doSwitch(File path, SVNURL url, SVNRevision pegRevision, SVNRevision revision, SVNDepth depth, boolean allowUnversionedObstructions, boolean depthIsSticky, boolean ignoreAncestry) throws SVNException {
        SvnSwitch sw = getOperationsFactory().createSwitch();
        sw.setUpdateLocksOnDemand(isUpdateLocksOnDemand());
        sw.setSwitchTarget(SvnTarget.fromURL(url, pegRevision));
        sw.setSingleTarget(SvnTarget.fromFile(path));
        sw.setRevision(revision);
        sw.setDepth(depth);
        sw.setDepthIsSticky(depthIsSticky);
        sw.setAllowUnversionedObstructions(allowUnversionedObstructions);
        sw.setIgnoreAncestry(ignoreAncestry);
        sw.setIgnoreExternals(isIgnoreExternals());
        sw.setExternalsHandler(SvnCodec.externalsHandler(getExternalsHandler()));
        
        return sw.run();
    }

    /**
     * Checks out a Working Copy from a repository.
     * 
     * <p>
     * If the destination path (<code>dstPath</code>) is <span
     * class="javakeyword">null</span> then the last component of
     * <code>url</code> is used for the local directory name.
     * 
     * <p>
     * As a revision <b>SVNRevision</b>'s pre-defined constant fields can be
     * used. For example, to check out a Working Copy at the latest revision of
     * the repository use {@link SVNRevision#HEAD HEAD}.
     * 
     * @param url
     *            a repository location from where a Working Copy will be
     *            checked out
     * @param dstPath
     *            the local path where the Working Copy will be placed
     * @param pegRevision
     *            the revision at which <code>url</code> will be firstly seen in
     *            the repository to make sure it's the one that is needed
     * @param revision
     *            the desired revision of the Working Copy to be checked out
     * @param recursive
     *            if <span class="javakeyword">true</span> and <code>url</code>
     *            is a directory then the entire tree will be checked out,
     *            otherwise if <span class="javakeyword">false</span> - only
     *            items located immediately in the directory itself
     * @return the revision number of the Working Copy
     * @throws SVNException
     *             <code>url</code> refers to a file, not a directory;
     *             <code>dstPath</code> already exists but it is a file, not a
     *             directory; <code>dstPath</code> already exists and is a
     *             versioned directory but has a different URL (repository
     *             location against which the directory is controlled)
     * @deprecated use
     *             {@link #doCheckout(SVNURL, File, SVNRevision, SVNRevision, SVNDepth, boolean)}
     *             instead
     */
    public long doCheckout(SVNURL url, File dstPath, SVNRevision pegRevision, SVNRevision revision, boolean recursive) throws SVNException {
        return doCheckout(url, dstPath, pegRevision, revision, SVNDepth.fromRecurse(recursive), false);
    }

    /**
     * @param url
     * @param dstPath
     * @param pegRevision
     * @param revision
     * @param recursive
     * @param force
     * @return actual revision number
     * @throws SVNException
     * @deprecated use
     *             {@link #doCheckout(SVNURL, File, SVNRevision, SVNRevision, SVNDepth, boolean)}
     *             instead
     */
    public long doCheckout(SVNURL url, File dstPath, SVNRevision pegRevision, SVNRevision revision, boolean recursive, boolean force) throws SVNException {
        return doCheckout(url, dstPath, pegRevision, revision, SVNDepth.fromRecurse(recursive), force);
    }

    /**
     * Checks out a working copy of <code>url</code> at <code>revision</code>,
     * looked up at <code>pegRevision</code>, using <code>dstPath</code> as the
     * root directory of the newly checked out working copy.
     * 
     * <p/>
     * If <code>pegRevision</code> is {@link SVNRevision#UNDEFINED}, then it
     * defaults to {@link SVNRevision#HEAD}.
     * 
     * <p/>
     * <code>revision</code> must represent a valid revision number (
     * {@link SVNRevision#getNumber()} >= 0), or date (
     * {@link SVNRevision#getDate()} != <span class="javakeyword">true</span>),
     * or be equal to {@link SVNRevision#HEAD}. If <code>revision</code> does
     * not meet these requirements, an exception with the error code
     * {@link SVNErrorCode#CLIENT_BAD_REVISION} is thrown.
     * 
     * <p/>
     * If <code>depth</code> is {@link SVNDepth#INFINITY}, checks out fully
     * recursively. Else if it is {@link SVNDepth#IMMEDIATES}, checks out
     * <code>url</code> and its immediate entries (subdirectories will be
     * present, but will be at depth {@link SVNDepth#EMPTY} themselves); else
     * {@link SVNDepth#FILES}, checks out <code>url</code> and its file entries,
     * but no subdirectories; else if {@link SVNDepth#EMPTY}, checks out
     * <code>url</code> as an empty directory at that depth, with no entries
     * present.
     * 
     * <p/>
     * If <code>depth</code> is {@link SVNDepth#UNKNOWN}, then behave as if for
     * {@link SVNDepth#INFINITY}, except in the case of resuming a previous
     * checkout of <code>dstPath</code> (i.e., updating), in which case uses the
     * depth of the existing working copy.
     * 
     * <p/>
     * If externals are {@link #isIgnoreExternals() ignored}, doesn't process
     * externals definitions as part of this operation.
     * 
     * <p/>
     * If <code>allowUnversionedObstructions</code> is <span
     * class="javakeyword">true</span> then the checkout tolerates existing
     * unversioned items that obstruct added paths from <code>url</code>. Only
     * obstructions of the same type (file or dir) as the added item are
     * tolerated. The text of obstructing files is left as-is, effectively
     * treating it as a user modification after the checkout. Working properties
     * of obstructing items are set equal to the base properties. If
     * <code>allowUnversionedObstructions</code> is <span
     * class="javakeyword">false</span> then the checkout will abort if there
     * are any unversioned obstructing items.
     * 
     * <p/>
     * If the caller's {@link ISVNEventHandler} is non-<span
     * class="javakeyword">null</span>, it is invoked as the checkout processes.
     * Also {@link ISVNEventHandler#checkCancelled()} will be used at various
     * places during the checkout to check whether the caller wants to stop the
     * checkout.
     * 
     * <p/>
     * This operation requires repository access (in case the repository is not
     * on the same machine, network connection is established).
     * 
     * @param url
     *            a repository location from where a Working Copy will be
     *            checked out
     * @param dstPath
     *            the local path where the Working Copy will be placed
     * @param pegRevision
     *            the revision at which <code>url</code> will be firstly seen in
     *            the repository to make sure it's the one that is needed
     * @param revision
     *            the desired revision of the Working Copy to be checked out
     * @param depth
     *            tree depth
     * @param allowUnversionedObstructions
     *            flag that allows tollerating unversioned items during
     * @return value of the revision actually checked out from the repository
     * @throws SVNException
     *             <ul>
     *             <li/>{@link SVNErrorCode#UNSUPPORTED_FEATURE} - if <code>url
     *             </code> refers to a file rather than a directory <li/>
     *             {@link SVNErrorCode#RA_ILLEGAL_URL} - if <code>url</code>
     *             does not exist
     *             </ul>
     * @since 1.2, SVN 1.5
     */
    public long doCheckout(SVNURL url, File dstPath, SVNRevision pegRevision, SVNRevision revision, SVNDepth depth, boolean allowUnversionedObstructions) throws SVNException {
        SvnCheckout co = getOperationsFactory().createCheckout();
        co.setUpdateLocksOnDemand(isUpdateLocksOnDemand());
        co.setSource(SvnTarget.fromURL(url, pegRevision));
        co.setSingleTarget(SvnTarget.fromFile(dstPath));
        co.setRevision(revision);
        co.setDepth(depth);
        co.setAllowUnversionedObstructions(allowUnversionedObstructions);
        co.setIgnoreExternals(isIgnoreExternals());        
        co.setExternalsHandler(SvnCodec.externalsHandler(getExternalsHandler()));
        
        return co.run();
    }

    /**
     * Exports a clean directory or single file from a repository.
     * 
     * <p>
     * If <code>eolStyle</code> is not <span class="javakeyword">null</span>
     * then it should denote a specific End-Of-Line marker for the files to be
     * exported. Significant values for <code>eolStyle</code> are:
     * <ul>
     * <li>"CRLF" (Carriage Return Line Feed) - this causes files to contain
     * '\r\n' line ending sequences for EOL markers, regardless of the operating
     * system in use (for instance, this EOL marker is used by software on the
     * Windows platform).
     * <li>"LF" (Line Feed) - this causes files to contain '\n' line ending
     * sequences for EOL markers, regardless of the operating system in use (for
     * instance, this EOL marker is used by software on the Unix platform).
     * <li>"CR" (Carriage Return) - this causes files to contain '\r' line
     * ending sequences for EOL markers, regardless of the operating system in
     * use (for instance, this EOL marker was used by software on older
     * Macintosh platforms).
     * <li>"native" - this causes files to contain the EOL markers that are
     * native to the operating system on which SVNKit is run.
     * </ul>
     * 
     * @param url
     *            a repository location from where the unversioned
     *            directory/file will be exported
     * @param dstPath
     *            the local path where the repository items will be exported to
     * @param pegRevision
     *            the revision at which <code>url</code> will be firstly seen in
     *            the repository to make sure it's the one that is needed
     * @param revision
     *            the desired revision of the directory/file to be exported
     * @param eolStyle
     *            a string that denotes a specific End-Of-Line charecter;
     * @param force
     *            <span class="javakeyword">true</span> to fore the operation
     *            even if there are local files with the same names as those in
     *            the repository (local ones will be replaced)
     * @param recursive
     *            if <span class="javakeyword">true</span> and <code>url</code>
     *            is a directory then the entire tree will be exported,
     *            otherwise if <span class="javakeyword">false</span> - only
     *            items located immediately in the directory itself
     * @return the revision number of the exported directory/file
     * @throws SVNException
     * @deprecated use
     *             {@link #doExport(SVNURL, File, SVNRevision, SVNRevision, String, boolean, SVNDepth)}
     */
    public long doExport(SVNURL url, File dstPath, SVNRevision pegRevision, SVNRevision revision, String eolStyle, boolean force, boolean recursive) throws SVNException {
        return doExport(url, dstPath, pegRevision, revision, eolStyle, force, SVNDepth.fromRecurse(recursive));
    }

    /**
     * Exports the contents of a subversion repository into a 'clean' directory
     * (meaning a directory with no administrative directories).
     * 
     * <p/>
     * <code>pegRevision</code> is the revision where the path is first looked
     * up. If <code>pegRevision</code> is {@link SVNRevision#UNDEFINED}, then it
     * defaults to {@link SVNRevision#HEAD}.
     * 
     * <p/>
     * If externals are {@link #isIgnoreExternals() ignored}, doesn't process
     * externals definitions as part of this operation.
     * 
     * <p/>
     * <code>eolStyle</code> allows you to override the standard eol marker on
     * the platform you are running on. Can be either "LF", "CR" or "CRLF" or
     * <span class="javakeyword">null</span>. If <span
     * class="javakeyword">null</span> will use the standard eol marker. Any
     * other value will cause an exception with the error code
     * {@link SVNErrorCode#IO_UNKNOWN_EOL} error to be returned.
     * 
     * <p>
     * If <code>depth</code> is {@link SVNDepth#INFINITY}, exports fully
     * recursively. Else if it is {@link SVNDepth#IMMEDIATES}, exports
     * <code>url</code> and its immediate children (if any), but with
     * subdirectories empty and at {@link SVNDepth#EMPTY}. Else if
     * {@link SVNDepth#FILES}, exports <code>url</code> and its immediate file
     * children (if any) only. If <code>depth</code> is {@link SVNDepth#EMPTY},
     * then exports exactly <code>url</code> and none of its children.
     * 
     * @param url
     *            repository url to export from
     * @param dstPath
     *            path to export to
     * @param pegRevision
     *            the revision at which <code>url</code> will be firstly seen in
     *            the repository to make sure it's the one that is needed
     * @param revision
     *            the desired revision of the directory/file to be exported
     * @param eolStyle
     *            a string that denotes a specific End-Of-Line charecter
     * @param overwrite
     *            if <span class="javakeyword">true</span>, will cause the
     *            export to overwrite files or directories
     * @param depth
     *            tree depth
     * @return value of the revision actually exported
     * @throws SVNException
     * @since 1.2, SVN 1.5
     */
    public long doExport(SVNURL url, File dstPath, SVNRevision pegRevision, SVNRevision revision, String eolStyle, boolean overwrite, SVNDepth depth) throws SVNException {
        SvnExport export = getOperationsFactory().createExport();
        
        export.setUpdateLocksOnDemand(isUpdateLocksOnDemand());
        export.setSingleTarget(SvnTarget.fromFile(dstPath));
        export.setSource(SvnTarget.fromURL(url, pegRevision));
        export.setRevision(revision);
        export.setEolStyle(eolStyle);
        export.setForce(overwrite);
        export.setDepth(depth);
        export.setExpandKeywords(isExportExpandsKeywords());
        export.setIgnoreExternals(isIgnoreExternals());
        export.setExternalsHandler(SvnCodec.externalsHandler(getExternalsHandler()));
        
        return export.run();
    }

    /**
     * Exports a clean directory or single file from eihter a source Working
     * Copy or a repository.
     * 
     * <p>
     * How this method works:
     * <ul>
     * <li>If <code>revision</code> is different from {@link SVNRevision#BASE
     * BASE}, {@link SVNRevision#WORKING WORKING}, {@link SVNRevision#COMMITTED
     * COMMITTED}, {@link SVNRevision#UNDEFINED UNDEFINED} - then the repository
     * origin of <code>srcPath</code> will be exported (what is done by "remote"
     * {@link #doExport(SVNURL, File, SVNRevision, SVNRevision, String, boolean, boolean)
     * doExport()}).
     * <li>In other cases a clean unversioned copy of <code>srcPath</code> -
     * either a directory or a single file - is exported to <code>dstPath</code>.
     * </ul>
     * 
     * <p>
     * If <code>eolStyle</code> is not <span class="javakeyword">null</span>
     * then it should denote a specific End-Of-Line marker for the files to be
     * exported. Significant values for <code>eolStyle</code> are:
     * <ul>
     * <li>"CRLF" (Carriage Return Line Feed) - this causes files to contain
     * '\r\n' line ending sequences for EOL markers, regardless of the operating
     * system in use (for instance, this EOL marker is used by software on the
     * Windows platform).
     * <li>"LF" (Line Feed) - this causes files to contain '\n' line ending
     * sequences for EOL markers, regardless of the operating system in use (for
     * instance, this EOL marker is used by software on the Unix platform).
     * <li>"CR" (Carriage Return) - this causes files to contain '\r' line
     * ending sequences for EOL markers, regardless of the operating system in
     * use (for instance, this EOL marker was used by software on older
     * Macintosh platforms).
     * <li>"native" - this causes files to contain the EOL markers that are
     * native to the operating system on which SVNKit is run.
     * </ul>
     * 
     * @param srcPath
     *            a repository location from where the unversioned
     *            directory/file will be exported
     * @param dstPath
     *            the local path where the repository items will be exported to
     * @param pegRevision
     *            the revision at which <code>url</code> will be firstly seen in
     *            the repository to make sure it's the one that is needed
     * @param revision
     *            the desired revision of the directory/file to be exported
     * @param eolStyle
     *            a string that denotes a specific End-Of-Line charecter;
     * @param force
     *            <span class="javakeyword">true</span> to fore the operation
     *            even if there are local files with the same names as those in
     *            the repository (local ones will be replaced)
     * @param recursive
     *            if <span class="javakeyword">true</span> and <code>url</code>
     *            is a directory then the entire tree will be exported,
     *            otherwise if <span class="javakeyword">false</span> - only
     *            items located immediately in the directory itself
     * @return the revision number of the exported directory/file
     * @throws SVNException
     * @deprecated use
     *             {@link #doExport(File, File, SVNRevision, SVNRevision, String, boolean, SVNDepth)}
     */
    public long doExport(File srcPath, final File dstPath, SVNRevision pegRevision, SVNRevision revision, String eolStyle, final boolean force, boolean recursive) throws SVNException {
        return doExport(srcPath, dstPath, pegRevision, revision, eolStyle, force, SVNDepth.fromRecurse(recursive));
    }

    /**
     * Exports the contents of either a subversion repository or a subversion
     * working copy into a 'clean' directory (meaning a directory with no
     * administrative directories).
     * 
     * <p/>
     * <code>pegRevision</code> is the revision where the path is first looked
     * up when exporting from a repository. If <code>pegRevision</code> is
     * {@link SVNRevision#UNDEFINED}, then it defaults to
     * {@link SVNRevision#WORKING}.
     * 
     * <p/>
     * If <code>revision</code> is one of:
     * <ul>
     * <li/>{@link SVNRevision#BASE}
     * <li/>{@link SVNRevision#WORKING}
     * <li/>{@link SVNRevision#COMMITTED}
     * <li/>{@link SVNRevision#UNDEFINED}
     * </ul>
     * then local export is performed. Otherwise exporting from the repository.
     * 
     * <p/>
     * If externals are {@link #isIgnoreExternals() ignored}, doesn't process
     * externals definitions as part of this operation.
     * 
     * <p/>
     * <code>eolStyle</code> allows you to override the standard eol marker on
     * the platform you are running on. Can be either "LF", "CR" or "CRLF" or
     * <span class="javakeyword">null</span>. If <span
     * class="javakeyword">null</span> will use the standard eol marker. Any
     * other value will cause an exception with the error code
     * {@link SVNErrorCode#IO_UNKNOWN_EOL} error to be returned.
     * 
     * <p>
     * If <code>depth</code> is {@link SVNDepth#INFINITY}, exports fully
     * recursively. Else if it is {@link SVNDepth#IMMEDIATES}, exports
     * <code>srcPath</code> and its immediate children (if any), but with
     * subdirectories empty and at {@link SVNDepth#EMPTY}. Else if
     * {@link SVNDepth#FILES}, exports <code>srcPath</code> and its immediate
     * file children (if any) only. If <code>depth</code> is
     * {@link SVNDepth#EMPTY}, then exports exactly <code>srcPath</code> and
     * none of its children.
     * 
     * @param srcPath
     *            working copy path
     * @param dstPath
     *            path to export to
     * @param pegRevision
     *            the revision at which <code>url</code> will be firstly seen in
     *            the repository to make sure it's the one that is needed
     * @param revision
     *            the desired revision of the directory/file to be exported;
     *            used only when exporting from a repository
     * @param eolStyle
     *            a string that denotes a specific End-Of-Line charecter
     * @param overwrite
     *            if <span class="javakeyword">true</span>, will cause the
     *            export to overwrite files or directories
     * @param depth
     *            tree depth
     * @return value of the revision actually exported
     * @throws SVNException
     * @since 1.2, SVN 1.5
     */
    public long doExport(File srcPath, final File dstPath, SVNRevision pegRevision, SVNRevision revision, String eolStyle, final boolean overwrite, SVNDepth depth) throws SVNException {
        SvnExport export = getOperationsFactory().createExport();
        export.setUpdateLocksOnDemand(isUpdateLocksOnDemand());
        export.setSingleTarget(SvnTarget.fromFile(dstPath));
        export.setSource(SvnTarget.fromFile(srcPath, pegRevision));
        export.setRevision(revision);
        export.setEolStyle(eolStyle);
        export.setForce(overwrite);
        export.setIgnoreExternals(isIgnoreExternals());
        export.setDepth(depth);
        export.setExpandKeywords(isExportExpandsKeywords());
        export.setExternalsHandler(SvnCodec.externalsHandler(getExternalsHandler()));
        
        return export.run();
    }

    /**
     * Substitutes the beginning part of a Working Copy's URL with a new one.
     * 
     * <p>
     * When a repository root location or a URL schema is changed the old URL of
     * the Working Copy which starts with <code>oldURL</code> should be
     * substituted for a new URL beginning - <code>newURL</code>.
     * 
     * @param dst
     *            a Working Copy item's path
     * @param oldURL
     *            the old beginning part of the repository's URL that should be
     *            overwritten
     * @param newURL
     *            a new beginning part for the repository location that will
     *            overwrite <code>oldURL</code>
     * @param recursive
     *            if <span class="javakeyword">true</span> and <code>dst</code>
     *            is a directory then the entire tree will be relocated,
     *            otherwise if <span class="javakeyword">false</span> - only
     *            <code>dst</code> itself
     * @throws SVNException
     */
    public void doRelocate(File dst, SVNURL oldURL, SVNURL newURL, boolean recursive) throws SVNException {
        SvnRelocate relocate = getOperationsFactory().createRelocate();
        
        relocate.setSingleTarget(SvnTarget.fromFile(dst));
        relocate.setFromUrl(oldURL);
        relocate.setToUrl(newURL);
        relocate.setRecursive(recursive);

        relocate.run();
    }

    /**
     * Canonicalizes all urls in the specified Working Copy.
     * 
     * @param dst
     *            a WC path
     * @param omitDefaultPort
     *            if <span class="javakeyword">true</span> then removes all port
     *            numbers from urls which equal to default ones, otherwise does
     *            not
     * @param recursive
     *            recurses an operation
     * @throws SVNException
     */
    public void doCanonicalizeURLs(File dst, boolean omitDefaultPort, boolean recursive) throws SVNException {
        SvnCanonicalizeUrls cu = getOperationsFactory().createCanonicalizeUrls();
        cu.setSingleTarget(SvnTarget.fromFile(dst));
        cu.setDepth(recursive ? SVNDepth.INFINITY : SVNDepth.IMMEDIATES);
        cu.setOmitDefaultPort(omitDefaultPort);
        cu.setIgnoreExternals(isIgnoreExternals());
        cu.run();
    }

    /**
     * Sets whether keywords must be expanded during an export operation.
     * 
     * @param expand
     *            <span class="javakeyword">true</span> to expand; otherwise
     *            <span class="javakeyword">false</span>
     * @since 1.3
     */
    public void setExportExpandsKeywords(boolean expand) {
        this.exportExpandsKeywords = expand;
    }

    /**
     * Says whether keywords expansion during export operations is turned on or
     * not.
     * 
     * @return <span class="javakeyword">true</span> if expanding keywords;
     *         <span class="javakeyword">false</span> otherwise
     * @since 1.3
     */
    public boolean isExportExpandsKeywords() {
        return this.exportExpandsKeywords;
    }
}
