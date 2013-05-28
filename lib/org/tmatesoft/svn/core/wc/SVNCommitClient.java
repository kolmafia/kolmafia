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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc2.compat.SvnCodec;
import org.tmatesoft.svn.core.internal.wc2.compat.SvnCodec.SVNCommitPacketWrapper;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver;
import org.tmatesoft.svn.core.wc2.SvnCommit;
import org.tmatesoft.svn.core.wc2.SvnCommitPacket;
import org.tmatesoft.svn.core.wc2.SvnImport;
import org.tmatesoft.svn.core.wc2.SvnRemoteDelete;
import org.tmatesoft.svn.core.wc2.SvnRemoteMkDir;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * The <b>SVNCommitClient</b> class provides methods to perform operations that
 * relate to committing changes to an SVN repository. These operations are
 * similar to respective commands of the native SVN command line client and
 * include ones which operate on working copy items as well as ones that operate
 * only on a repository.
 * 
 * <p>
 * Here's a list of the <b>SVNCommitClient</b>'s commit-related methods matched
 * against corresponing commands of the SVN command line client:
 * 
 * <table cellpadding="3" cellspacing="1" border="0" width="40%" bgcolor="#999933">
 * <tr bgcolor="#ADB8D9" align="left">
 * <td><b>SVNKit</b></td>
 * <td><b>Subversion</b></td>
 * </tr>
 * <tr bgcolor="#EAEAEA" align="left">
 * <td>doCommit()</td>
 * <td>'svn commit'</td>
 * </tr>
 * <tr bgcolor="#EAEAEA" align="left">
 * <td>doImport()</td>
 * <td>'svn import'</td>
 * </tr>
 * <tr bgcolor="#EAEAEA" align="left">
 * <td>doDelete()</td>
 * <td>'svn delete URL'</td>
 * </tr>
 * <tr bgcolor="#EAEAEA" align="left">
 * <td>doMkDir()</td>
 * <td>'svn mkdir URL'</td>
 * </tr>
 * </table>
 * 
 * @version 1.3
 * @author TMate Software Ltd.
 * @since 1.2
 * @see <a target="_top" href="http://svnkit.com/kb/examples/">Examples</a>
 */
public class SVNCommitClient extends SVNBasicClient {

    private ISVNCommitHandler commitHandler;
    private ISVNCommitParameters commitParameters;
    private boolean failOnMultipleRepositories;

    /**
     * Constructs and initializes an <b>SVNCommitClient</b> object with the
     * specified run-time configuration and authentication drivers.
     * 
     * <p>
     * If <code>options</code> is <span class="javakeyword">null</span>, then
     * this <b>SVNCommitClient</b> will be using a default run-time
     * configuration driver which takes client-side settings from the default
     * SVN's run-time configuration area but is not able to change those
     * settings (read more on {@link ISVNOptions} and {@link SVNWCUtil}).
     * 
     * <p>
     * If <code>authManager</code> is <span class="javakeyword">null</span>,
     * then this <b>SVNCommitClient</b> will be using a default authentication
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
    public SVNCommitClient(ISVNAuthenticationManager authManager, ISVNOptions options) {
        super(authManager, options);
        setCommitHander(null);
        setCommitParameters(null);
        setCommitHandler(null);
    }

    /**
     * Constructs and initializes an <b>SVNCommitClient</b> object with the
     * specified run-time configuration and repository pool object.
     * 
     * <p/>
     * If <code>options</code> is <span class="javakeyword">null</span>, then
     * this <b>SVNCommitClient</b> will be using a default run-time
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
    public SVNCommitClient(ISVNRepositoryPool repositoryPool, ISVNOptions options) {
        super(repositoryPool, options);
        setCommitHander(null);
        setCommitParameters(null);
        setCommitHandler(null);
    }

    /**
     * @param handler
     * @deprecated use {@link #setCommitHandler(ISVNCommitHandler)} instead
     */
    public void setCommitHander(ISVNCommitHandler handler) {
        if (handler == null) {
            handler = new DefaultSVNCommitHandler();
        }
        this.commitHandler = handler;
    }

    /**
     * Sets an implementation of <b>ISVNCommitHandler</b> to the commit handler
     * that will be used during commit operations to handle commit log messages.
     * The handler will receive a clien's log message and items (represented as
     * <b>SVNCommitItem</b> objects) that will be committed. Depending on
     * implementor's aims the initial log message can be modified (or something
     * else) and returned back.
     * 
     * <p>
     * If using <b>SVNCommitClient</b> without specifying any commit handler
     * then a default one will be used - {@link DefaultSVNCommitHandler}.
     * 
     * @param handler
     *            an implementor's handler that will be used to handle commit
     *            log messages
     * @see #getCommitHandler()
     * @see ISVNCommitHandler
     */
    public void setCommitHandler(ISVNCommitHandler handler) {
        if (handler == null) {
            handler = new DefaultSVNCommitHandler();
        }
        this.commitHandler = handler;
    }

    /**
     * Returns the specified commit handler (if set) being in use or a default
     * one (<b>DefaultSVNCommitHandler</b>) if no special implementations of
     * <b>ISVNCommitHandler</b> were previously provided.
     * 
     * @return the commit handler being in use or a default one
     * @see #setCommitHander(ISVNCommitHandler)
     * @see ISVNCommitHandler
     * @see DefaultSVNCommitHandler
     */
    public ISVNCommitHandler getCommitHandler() {
        return this.commitHandler;
    }

    /**
     * Sets commit parameters to use.
     * 
     * <p>
     * When no parameters are set {@link DefaultSVNCommitParameters default}
     * ones are used.
     * 
     * @param parameters
     *            commit parameters
     * @see #getCommitParameters()
     */
    public void setCommitParameters(ISVNCommitParameters parameters) {
        this.commitParameters = parameters;
    }

    /**
     * Returns commit parameters.
     * 
     * <p>
     * If no user parameters were previously specified, once creates and returns
     * {@link DefaultSVNCommitParameters default} ones.
     * 
     * @return commit parameters
     * @see #setCommitParameters(ISVNCommitParameters)
     */
    public ISVNCommitParameters getCommitParameters() {
        return this.commitParameters;
    }

    /**
     * Committs removing specified URL-paths from the repository. This call is
     * equivalent to <code>doDelete(urls, commitMessage, null)</code>.
     * 
     * @param urls
     *            an array containing URL-strings that represent repository
     *            locations to be removed
     * @param commitMessage
     *            a string to be a commit log message
     * @return information on a new revision as the result of the commit
     * @throws SVNException
     *             if one of the following is true:
     *             <ul>
     *             <li>a URL does not exist
     *             <li>probably some of URLs refer to different repositories
     *             </ul>
     * @see #doDelete(SVNURL[], String, SVNProperties)
     */
    public SVNCommitInfo doDelete(SVNURL[] urls, String commitMessage) throws SVNException {
    	SvnRemoteDelete delete = getOperationsFactory().createRemoteDelete();
    	if (getCommitHandler() != null)
    		delete.setCommitHandler(SvnCodec.commitHandler(getCommitHandler()));
    	for (int i = 0; i < urls.length; i++) {
    		delete.addTarget(SvnTarget.fromURL(urls[i]));            
        }
    	delete.setCommitMessage(commitMessage);
    	return delete.run();
    }

    /**
     * Deletes items from a repository.
     * 
     * <p/>
     * If non-<span class="javakeyword">null</span>,
     * <code>revisionProperties</code> holds additional, custom revision
     * properties (<code>String</code> names mapped to {@link SVNPropertyValue}
     * values) to be set on the new revision. This table cannot contain any
     * standard Subversion properties.
     * 
     * <p/>
     * {@link #getCommitHandler() Commit handler} will be asked for a commit log
     * message.
     * 
     * <p/>
     * If the caller's {@link ISVNEventHandler event handler} is not <span
     * class="javakeyword">null</span> and if the commit succeeds, the handler
     * will be called with {@link SVNEventAction#COMMIT_COMPLETED} event action.
     * 
     * @param urls
     *            repository urls to delete
     * @param commitMessage
     *            commit log message
     * @param revisionProperties
     *            custom revision properties
     * @return information about the new committed revision
     * @throws SVNException
     *             in the following cases:
     *             <ul>
     *             <li/>exception with {@link SVNErrorCode#RA_ILLEGAL_URL} error
     *             code - if cannot compute common root url for <code>urls
     *             </code> <li/>exception with {@link SVNErrorCode#FS_NOT_FOUND}
     *             error code - if some of <code>urls</code> does not exist
     *             </ul>
     * @since 1.2.0, SVN 1.5.0
     */
    public SVNCommitInfo doDelete(SVNURL[] urls, String commitMessage, SVNProperties revisionProperties) throws SVNException {
    	SvnRemoteDelete delete = getOperationsFactory().createRemoteDelete();
    	if (getCommitHandler() != null)
    		delete.setCommitHandler(SvnCodec.commitHandler(getCommitHandler()));
    	for (int i = 0; i < urls.length; i++) {
    		delete.addTarget(SvnTarget.fromURL(urls[i]));            
        }
    	delete.setCommitMessage(commitMessage);
    	delete.setRevisionProperties(revisionProperties);
    	return delete.run();
    }

    /**
     * Committs a creation of a new directory/directories in the repository.
     * 
     * @param urls
     *            an array containing URL-strings that represent new repository
     *            locations to be created
     * @param commitMessage
     *            a string to be a commit log message
     * @return information on a new revision as the result of the commit
     * @throws SVNException
     *             if some of URLs refer to different repositories
     */
    public SVNCommitInfo doMkDir(SVNURL[] urls, String commitMessage) throws SVNException {
    	SvnRemoteMkDir mkdir = getOperationsFactory().createRemoteMkDir();
    	if (getCommitHandler() != null)
    		mkdir.setCommitHandler(SvnCodec.commitHandler(getCommitHandler()));
    	for (int i = 0; i < urls.length; i++) {
    		mkdir.addTarget(SvnTarget.fromURL(urls[i]));            
        }
    	mkdir.setCommitMessage(commitMessage);
    	return mkdir.run();
    }

    /**
     * Creates directory(ies) in a repository.
     * 
     * <p/>
     * If <code>makeParents</code> is <span class="javakeyword">true</span>,
     * creates any non-existent parent directories also.
     * 
     * <p/>
     * If non-<span class="javakeyword">null</span>,
     * <code>revisionProperties</code> holds additional, custom revision
     * properties (<code>String</code> names mapped to {@link SVNPropertyValue}
     * values) to be set on the new revision. This table cannot contain any
     * standard Subversion properties.
     * 
     * <p/>
     * {@link #getCommitHandler() Commit handler} will be asked for a commit log
     * message.
     * 
     * <p/>
     * If the caller's {@link ISVNEventHandler event handler} is not <span
     * class="javakeyword">null</span> and if the commit succeeds, the handler
     * will be called with {@link SVNEventAction#COMMIT_COMPLETED} event action.
     * 
     * @param urls
     *            repository locations to create
     * @param commitMessage
     *            commit log message
     * @param revisionProperties
     *            custom revision properties
     * @param makeParents
     *            if <span class="javakeyword">true</span>, creates all
     *            non-existent parent directories
     * @return information about the new committed revision
     * @throws SVNException
     *             in the following cases:
     *             <ul>
     *             <li/>exception with {@link SVNErrorCode#RA_ILLEGAL_URL} error
     *             code - if cannot compute common root url for <code>urls
     *             </code> <li/>exception with {@link SVNErrorCode#FS_NOT_FOUND}
     *             error code - if some of <code>urls</code> does not exist
     *             </ul>
     * @since 1.2.0, SVN 1.5.0
     */
    public SVNCommitInfo doMkDir(SVNURL[] urls, String commitMessage, SVNProperties revisionProperties, boolean makeParents) throws SVNException {
    	SvnRemoteMkDir mkdir = getOperationsFactory().createRemoteMkDir();
    	if (getCommitHandler() != null)
    		mkdir.setCommitHandler(SvnCodec.commitHandler(getCommitHandler()));
    	for (int i = 0; i < urls.length; i++) {
    		mkdir.addTarget(SvnTarget.fromURL(urls[i]));            
        }
    	mkdir.setCommitMessage(commitMessage);
    	mkdir.setRevisionProperties(revisionProperties);
    	mkdir.setMakeParents(makeParents);
    	return mkdir.run();
    }

    /**
     * Committs an addition of a local unversioned file or directory into the
     * repository.
     * 
     * <p/>
     * This method is identical to
     * <code>doImport(path, dstURL, commitMessage, null, true, false, SVNDepth.fromRecurse(recursive))</code>.
     * 
     * @param path
     *            a local unversioned file or directory to be imported into the
     *            repository
     * @param dstURL
     *            a URL-string that represents a repository location where the
     *            <code>path</code> will be imported
     * @param commitMessage
     *            a string to be a commit log message
     * @param recursive
     *            this flag is relevant only when the <code>path</code> is a
     *            directory: if <span class="javakeyword">true</span> then the
     *            entire directory tree will be imported including all child
     *            directories, otherwise only items located in the directory
     *            itself
     * @return information on a new revision as the result of the commit
     * @throws SVNException
     *             if one of the following is true:
     *             <ul>
     *             <li><code>dstURL</code> is invalid <li>the path denoted by
     *             <code>dstURL</code> already exists <li><code>path</code>
     *             contains a reserved name - <i>'.svn'</i>
     *             </ul>
     * @deprecated use
     *             {@link #doImport(File, SVNURL, String, SVNProperties, boolean, boolean, SVNDepth)}
     *             instead
     */
    public SVNCommitInfo doImport(File path, SVNURL dstURL, String commitMessage, boolean recursive) throws SVNException {
        return doImport(path, dstURL, commitMessage, null, true, true, SVNDepth.fromRecurse(recursive));
    }

    /**
     * Committs an addition of a local unversioned file or directory into the
     * repository.
     * 
     * <p/>
     * This method is identical to
     * <code>doImport(path, dstURL, commitMessage, null, useGlobalIgnores, false, SVNDepth.fromRecurse(recursive))</code>.
     * 
     * @param path
     *            a local unversioned file or directory to be imported into the
     *            repository
     * @param dstURL
     *            a URL-string that represents a repository location where the
     *            <code>path</code> will be imported
     * @param commitMessage
     *            a string to be a commit log message
     * @param useGlobalIgnores
     *            if <span class="javakeyword">true</span> then those paths that
     *            match global ignore patterns controlled by a config options
     *            driver (see
     *            {@link org.tmatesoft.svn.core.wc.ISVNOptions#getIgnorePatterns()}
     *            ) will not be imported, otherwise global ignore patterns are
     *            not used
     * @param recursive
     *            this flag is relevant only when the <code>path</code> is a
     *            directory: if <span class="javakeyword">true</span> then the
     *            entire directory tree will be imported including all child
     *            directories, otherwise only items located in the directory
     *            itself
     * @return information on a new revision as the result of the commit
     * @throws SVNException
     *             if one of the following is true:
     *             <ul>
     *             <li><code>dstURL</code> is invalid <li>the path denoted by
     *             <code>dstURL</code> already exists <li><code>path</code>
     *             contains a reserved name - <i>'.svn'</i>
     *             </ul>
     * @deprecated use
     *             {@link #doImport(File, SVNURL, String, SVNProperties, boolean, boolean, SVNDepth)}
     *             instead
     */
    public SVNCommitInfo doImport(File path, SVNURL dstURL, String commitMessage, boolean useGlobalIgnores, boolean recursive) throws SVNException {
        return doImport(path, dstURL, commitMessage, null, useGlobalIgnores, true, SVNDepth.fromRecurse(recursive));
    }

    /**
     * Imports file or directory <code>path</code> into repository directory
     * <code>dstURL</code> at HEAD revision. If some components of
     * <code>dstURL</code> do not exist, then creates parent directories as
     * necessary.
     * 
     * <p/>
     * If <code>path</code> is a directory, the contents of that directory are
     * imported directly into the directory identified by <code>dstURL</code>.
     * Note that the directory <code>path</code> itself is not imported -- that
     * is, the base name of <code>path<code> is not part of the import.
     * 
     * <p/>
     * If <code>path</code> is a file, then the parent of <code>dstURL</code> is
     * the directory receiving the import. The base name of <code>dstURL</code>
     * is the filename in the repository. In this case if <code>dstURL</code>
     * already exists, throws {@link SVNException}.
     * 
     * <p/>
     * If the caller's {@link ISVNEventHandler event handler} is not <span
     * class="javakeyword">null</span> it will be called as the import
     * progresses with {@link SVNEventAction#COMMIT_ADDED} action. If the commit
     * succeeds, the handler will be called with
     * {@link SVNEventAction#COMMIT_COMPLETED} event action.
     * 
     * <p/>
     * If non-<span class="javakeyword">null</span>,
     * <code>revisionProperties</code> holds additional, custom revision
     * properties (<code>String</code> names mapped to {@link SVNPropertyValue}
     * values) to be set on the new revision. This table cannot contain any
     * standard Subversion properties.
     * 
     * <p/>
     * {@link #getCommitHandler() Commit handler} will be asked for a commit log
     * message.
     * 
     * <p/>
     * If <code>depth</code> is {@link SVNDepth#EMPTY}, imports just
     * <code>path</code> and nothing below it. If {@link SVNDepth#FILES},
     * imports <code>path</code> and any file children of <code>path</code>. If
     * {@link SVNDepth#IMMEDIATES}, imports <code>path</code>, any file
     * children, and any immediate subdirectories (but nothing underneath those
     * subdirectories). If {@link SVNDepth#INFINITY}, imports <code>path</code>
     * and everything under it fully recursively.
     * 
     * <p/>
     * If <code>useGlobalIgnores</code> is <span
     * class="javakeyword">false</span>, doesn't add files or directories that
     * match ignore patterns.
     * 
     * <p/>
     * If <code>ignoreUnknownNodeTypes</code> is <span
     * class="javakeyword">false</span>, ignores files of which the node type is
     * unknown, such as device files and pipes.
     * 
     * @param path
     *            path to import
     * @param dstURL
     *            import destination url
     * @param commitMessage
     *            commit log message
     * @param revisionProperties
     *            custom revision properties
     * @param useGlobalIgnores
     *            whether matching against global ignore patterns should take
     *            place
     * @param ignoreUnknownNodeTypes
     *            whether to ignore files of unknown node types or not
     * @param depth
     *            tree depth to process
     * @return information about the new committed revision
     * @throws SVNException
     *             in the following cases:
     *             <ul>
     *             <li/>exception with {@link SVNErrorCode#ENTRY_NOT_FOUND}
     *             error code - if <code>path</code> does not exist <li/>
     *             exception with {@link SVNErrorCode#ENTRY_EXISTS} error code -
     *             if <code>dstURL</code> already exists and <code>path</code>
     *             is a file <li/>exception with
     *             {@link SVNErrorCode#CL_ADM_DIR_RESERVED} error code - if
     *             trying to import an item with a reserved SVN name (like
     *             <code>'.svn'</code> or <code>'_svn'</code>)
     *             </ul>
     * @since 1.2.0, New in SVN 1.5.0
     */
    public SVNCommitInfo doImport(File path, SVNURL dstURL, String commitMessage, SVNProperties revisionProperties, boolean useGlobalIgnores, boolean ignoreUnknownNodeTypes, SVNDepth depth) throws SVNException {
        SvnImport svnImport = getOperationsFactory().createImport();
        svnImport.setCommitHandler(SvnCodec.commitHandler(getCommitHandler()));
        svnImport.setCommitMessage(commitMessage);
        svnImport.setRevisionProperties(revisionProperties);
        svnImport.addTarget(SvnTarget.fromURL(dstURL));
        svnImport.setSource(path);
        svnImport.setDepth(depth);
        svnImport.setUseGlobalIgnores(useGlobalIgnores);
        
        return svnImport.run();
    }

    /**
     * Committs local changes to the repository.
     * 
     * <p/>
     * This method is identical to
     * <code>doCommit(paths, keepLocks, commitMessage, null, null, false, force, SVNDepth.fromRecurse(recursive))</code>.
     * 
     * @param paths
     *            an array of local items which should be traversed to commit
     *            changes they have to the repository
     * @param keepLocks
     *            if <span class="javakeyword">true</span> and there are local
     *            items that were locked then the commit will left them locked,
     *            otherwise the items will be unlocked after the commit succeeds
     * @param commitMessage
     *            a string to be a commit log message
     * @param force
     *            <span class="javakeyword">true</span> to force a non-recursive
     *            commit; if <code>recursive</code> is set to <span
     *            class="javakeyword">true</span> the <code>force</code> flag is
     *            ignored
     * @param recursive
     *            relevant only for directory items: if <span
     *            class="javakeyword">true</span> then the entire directory tree
     *            will be committed including all child directories, otherwise
     *            only items located in the directory itself
     * @return information on a new revision as the result of the commit
     * @throws SVNException
     * @deprecated use
     *             {@link #doCommit(File[], boolean, String, SVNProperties, String[], boolean, boolean, SVNDepth)}
     *             instead
     */
    public SVNCommitInfo doCommit(File[] paths, boolean keepLocks, String commitMessage, boolean force, boolean recursive) throws SVNException {
        return doCommit(paths, keepLocks, commitMessage, null, null, false, force, SVNDepth.fromRecurse(recursive));
    }

    /**
     * Commits files or directories into repository.
     * 
     * <p/>
     * <code>paths</code> need not be canonicalized nor condensed; this method
     * will take care of that. If
     * <code>targets has zero elements, then do nothing and return
     * immediately without error.
     * 
     * <p/>
     * If non-<span class="javakeyword">null</span>, <code>revisionProperties</code>
     * holds additional, custom revision properties (<code>String</code> names
     * mapped to {@link SVNPropertyValue} values) to be set on the new revision.
     * This table cannot contain any standard Subversion properties.
     * 
     * <p/>
     * If the caller's {@link ISVNEventHandler event handler} is not <span
     * class="javakeyword">null</span> it will be called as the commit
     * progresses with any of the following actions:
     * {@link SVNEventAction#COMMIT_MODIFIED},
     * {@link SVNEventAction#COMMIT_ADDED},
     * {@link SVNEventAction#COMMIT_DELETED},
     * {@link SVNEventAction#COMMIT_REPLACED}. If the commit succeeds, the
     * handler will be called with {@link SVNEventAction#COMMIT_COMPLETED} event
     * action.
     * 
     * <p/>
     * If <code>depth</code> is {@link SVNDepth#INFINITY}, commits all changes
     * to and below named targets. If <code>depth</code> is
     * {@link SVNDepth#EMPTY}, commits only named targets (that is, only
     * property changes on named directory targets, and property and content
     * changes for named file targets). If <code>depth</code> is
     * {@link SVNDepth#FILES}, behaves as above for named file targets, and for
     * named directory targets, commits property changes on a named directory
     * and all changes to files directly inside that directory. If
     * {@link SVNDepth#IMMEDIATES}, behaves as for {@link SVNDepth#FILES}, and
     * for subdirectories of any named directory target commits as though for
     * {@link SVNDepth#EMPTY}.
     * 
     * <p/>
     * Unlocks paths in the repository, unless <code>keepLocks</code> is <span
     * class="javakeyword">true</span>.
     * 
     * <p/>
     * <code>changelists</code> is an array of <code>String</code> changelist
     * names, used as a restrictive filter on items that are committed; that is,
     * doesn't commit anything unless it's a member of one of those changelists.
     * After the commit completes successfully, removes changelist associations
     * from the targets, unless <code>keepChangelist</code> is set. If
     * <code>changelists</code> is empty (or altogether <span
     * class="javakeyword">null</span>), no changelist filtering occurs.
     * 
     * <p/>
     * If no exception is thrown and {@link SVNCommitInfo#getNewRevision()} is
     * invalid (<code>&lt;0</code>), then the commit was a no-op; nothing needed
     * to be committed.
     * 
     * @param paths
     *            paths to commit
     * @param keepLocks
     *            whether to unlock or not files in the repository
     * @param commitMessage
     *            commit log message
     * @param revisionProperties
     *            custom revision properties
     * @param changelists
     *            changelist names array
     * @param keepChangelist
     *            whether to remove <code>changelists</code> or not
     * @param force
     *            <span class="javakeyword">true</span> to force a non-recursive
     *            commit; if <code>depth</code> is {@link SVNDepth#INFINITY} the
     *            <code>force</code> flag is ignored
     * @param depth
     *            tree depth to process
     * @return information about the new committed revision
     * @throws SVNException
     * @since 1.2.0, New in Subversion 1.5.0
     */
    public SVNCommitInfo doCommit(File[] paths, boolean keepLocks, String commitMessage, SVNProperties revisionProperties, String[] changelists, boolean keepChangelist, boolean force, SVNDepth depth)
            throws SVNException {
        SVNCommitPacket[] packets = doCollectCommitItems(paths, keepLocks, force, depth, true, changelists);
        if (packets != null) {
            SVNCommitInfo[] infos = doCommit(packets, keepLocks, keepChangelist, commitMessage, revisionProperties);
            if (infos != null && infos.length > 0) {
                return infos[0];
            }
        }
        return SVNCommitInfo.NULL;
    }

    /**
     * Committs local changes made to the Working Copy items to the repository.
     * 
     * <p>
     * This method is identical to
     * <code>doCommit(commitPacket, keepLocks, false, commitMessage, null)</code>.
     * 
     * <p>
     * <code>commitPacket</code> contains commit items ({@link SVNCommitItem})
     * which represent local Working Copy items that were changed and are to be
     * committed. Commit items are gathered into a single
     * {@link SVNCommitPacket} by invoking
     * {@link #doCollectCommitItems(File[], boolean, boolean, boolean)
     * doCollectCommitItems()}.
     * 
     * @param commitPacket
     *            a single object that contains items to be committed
     * @param keepLocks
     *            if <span class="javakeyword">true</span> and there are local
     *            items that were locked then the commit will left them locked,
     *            otherwise the items will be unlocked after the commit succeeds
     * @param commitMessage
     *            a string to be a commit log message
     * @return information on a new revision as the result of the commit
     * @throws SVNException
     * @see SVNCommitItem
     */
    public SVNCommitInfo doCommit(SVNCommitPacket commitPacket, boolean keepLocks, String commitMessage) throws SVNException {
        return doCommit(commitPacket, keepLocks, false, commitMessage, null);
    }

    /**
     * Commits files or directories into repository.
     * 
     * <p/>
     * This method is identical to
     * {@link #doCommit(File[], boolean, String, SVNProperties, String[], boolean, boolean, SVNDepth)}
     * except for it receives a commit packet instead of paths array. The
     * aforementioned method collects commit items into a commit packet given
     * working copy paths. This one accepts already collected commit items
     * provided in <code>commitPacket</code>.
     * 
     * <p/>
     * <code>commitPacket</code> contains commit items ({@link SVNCommitItem})
     * which represent local Working Copy items that are to be committed. Commit
     * items are gathered in a single {@link SVNCommitPacket} by invoking either
     * {@link #doCollectCommitItems(File[], boolean, boolean, SVNDepth, String[])}
     * or
     * {@link #doCollectCommitItems(File[], boolean, boolean, SVNDepth, boolean, String[])}.
     * 
     * <p/>
     * For more details on parameters, please, refer to
     * {@link #doCommit(File[], boolean, String, SVNProperties, String[], boolean, boolean, SVNDepth)}.
     * 
     * @param commitPacket
     *            a single object that contains items to be committed
     * @param keepLocks
     *            if <span class="javakeyword">true</span> and there are local
     *            items that were locked then the commit will left them locked,
     *            otherwise the items will be unlocked after the commit succeeds
     * @param keepChangelist
     *            whether to remove changelists or not
     * @param commitMessage
     *            commit log message
     * @param revisionProperties
     *            custom revision properties
     * @return information about the new committed revision
     * @throws SVNException
     * @since 1.2.0, SVN 1.5.0
     */
    public SVNCommitInfo doCommit(SVNCommitPacket commitPacket, boolean keepLocks, boolean keepChangelist, String commitMessage, SVNProperties revisionProperties) throws SVNException {
        SVNCommitInfo[] infos = doCommit(new SVNCommitPacket[] {commitPacket}, keepLocks, keepChangelist, commitMessage, revisionProperties);
        if (infos != null && infos.length > 0) {
            return infos[0];
        }
        return SVNCommitInfo.NULL;
    }

    /**
     * Committs local changes, made to the Working Copy items, to the
     * repository.
     * 
     * <p>
     * <code>commitPackets</code> is an array of packets that contain commit
     * items (<b>SVNCommitItem</b>) which represent local Working Copy items
     * that were changed and are to be committed. Commit items are gathered in a
     * single <b>SVNCommitPacket</b> by invoking
     * {@link #doCollectCommitItems(File[], boolean, boolean, boolean)
     * doCollectCommitItems()}.
     * 
     * <p>
     * This allows to commit separate trees of Working Copies "belonging" to
     * different repositories. One packet per one repository. If repositories
     * are different (it means more than one commit will be done),
     * <code>commitMessage</code> may be replaced by a commit handler to be a
     * specific one for each commit.
     * 
     * <p>
     * This method is identical to
     * <code>doCommit(commitPackets, keepLocks, false, commitMessage, null)</code>.
     * 
     * @param commitPackets
     *            logically grouped items to be committed
     * @param keepLocks
     *            if <span class="javakeyword">true</span> and there are local
     *            items that were locked then the commit will left them locked,
     *            otherwise the items will be unlocked after the commit succeeds
     * @param commitMessage
     *            a string to be a commit log message
     * @return committed information
     * @throws SVNException
     */
    public SVNCommitInfo[] doCommit(SVNCommitPacket[] commitPackets, boolean keepLocks, String commitMessage) throws SVNException {
        return doCommit(commitPackets, keepLocks, false, commitMessage, null);
    }

    /**
     * Commits files or directories into repository.
     * 
     * <p>
     * <code>commitPackets</code> is an array of packets that contain commit
     * items ({@link SVNCommitItem}) which represent local Working Copy items
     * that were changed and are to be committed. Commit items are gathered in a
     * single {@link SVNCommitPacket} by invoking
     * {@link #doCollectCommitItems(File[], boolean, boolean, SVNDepth, String[])}
     * or
     * {@link #doCollectCommitItems(File[], boolean, boolean, SVNDepth, boolean, String[])}.
     * 
     * <p>
     * This allows to commit items from separate Working Copies checked out from
     * the same or different repositories. For each commit packet
     * {@link #getCommitHandler() commit handler} is invoked to produce a commit
     * message given the one <code>commitMessage</code> passed to this method.
     * Each commit packet is committed in a separate transaction.
     * 
     * <p/>
     * For more details on parameters, please, refer to
     * {@link #doCommit(File[], boolean, String, SVNProperties, String[], boolean, boolean, SVNDepth)}.
     * 
     * @param commitPackets
     *            commit packets containing commit commit items per one commit
     * @param keepLocks
     *            if <span class="javakeyword">true</span> and there are local
     *            items that were locked then the commit will left them locked,
     *            otherwise the items will be unlocked by the commit
     * @param keepChangelist
     *            whether to remove changelists or not
     * @param commitMessage
     *            a string to be a commit log message
     * @param revisionProperties
     *            custom revision properties
     * @return information about the new committed revisions
     * @throws SVNException
     * @since 1.2.0, SVN 1.5.0
     */
    public SVNCommitInfo[] doCommit(SVNCommitPacket[] commitPackets, boolean keepLocks, boolean keepChangelist, String commitMessage, SVNProperties revisionProperties) throws SVNException {
        final SVNCommitInfo[] infos = new SVNCommitInfo[commitPackets.length];
        if (commitPackets.length == 0) {
            return infos;
        }
        // assert that all commit packets belongs to the same operation;
        SvnCommit sharedOperation = null;
        for (int i = 0; i < commitPackets.length; i++) {
            final SvnCommit commitOperation = ((SVNCommitPacketWrapper) commitPackets[i]).getOperation();
            if (sharedOperation == null) {
                sharedOperation = commitOperation;
            }
            if (commitOperation != sharedOperation) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "Commit packets created by different commit operations may not be mixed.");
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        }        
        // dispose packets that were collected, but are not part of this commit request.
        final SvnCommitPacket[] operationPackets = sharedOperation.splitCommitPackets(sharedOperation.isCombinePackets());
        final Set<SvnCommitPacket> userPacketsSet = new HashSet<SvnCommitPacket>();
        for (int i = 0; i < commitPackets.length; i++) {
            final SvnCommitPacket userPacket = ((SVNCommitPacketWrapper) commitPackets[i]).getPacket();
            userPacketsSet.add(userPacket);
        }
        
        for (int i = 0; i < operationPackets.length; i++) {
            if (!userPacketsSet.contains(operationPackets[i])) {
                operationPackets[i].dispose();
            } 
        }
        
        SvnCommit commit = sharedOperation;
        commit.setCommitMessage(commitMessage);
        commit.setCommitHandler(SvnCodec.commitHandler(getCommitHandler()));
        commit.setCommitParameters(SvnCodec.commitParameters(getCommitParameters()));
        if (revisionProperties != null) {
            for (String propertyName : revisionProperties.nameSet()) {
                SVNPropertyValue value = revisionProperties.getSVNPropertyValue(propertyName);
                if (value != null) {
                    commit.setRevisionProperty(propertyName, value);
                }
            }
        }
        
        commit.setKeepLocks(keepLocks);
        commit.setKeepChangelists(keepChangelist);
        commit.setReceiver(new ISvnObjectReceiver<SVNCommitInfo>() {            
            int index = 0;
            public void receive(SvnTarget target, SVNCommitInfo object) throws SVNException {
                infos[index++] = object; 
            }
        });
        commit.run();
        return infos;
    }

    /**
     * Collects commit items (containing detailed information on each Working
     * Copy item that was changed and need to be committed to the repository)
     * into a single {@link SVNCommitPacket}.
     * 
     * <p/>
     * This method is equivalent to
     * <code>doCollectCommitItems(paths, keepLocks, force, SVNDepth.fromRecurse(recursive), null)</code>.
     * 
     * @param paths
     *            an array of local items which should be traversed to collect
     *            information on every changed item (one <b>SVNCommitItem</b>
     *            per each modified local item)
     * @param keepLocks
     *            if <span class="javakeyword">true</span> and there are local
     *            items that were locked then these items will be left locked
     *            after traversing all of them, otherwise the items will be
     *            unlocked
     * @param force
     *            forces collecting commit items for a non-recursive commit
     * @param recursive
     *            relevant only for directory items: if <span
     *            class="javakeyword">true</span> then the entire directory tree
     *            will be traversed including all child directories, otherwise
     *            only items located in the directory itself will be processed
     * @return an <b>SVNCommitPacket</b> containing all Working Copy items
     *         having local modifications and represented as
     *         <b>SVNCommitItem</b> objects; if no modified items were found
     *         then {@link SVNCommitPacket#EMPTY} is returned
     * @throws SVNException
     * @deprecated use
     *             {@link #doCollectCommitItems(File[], boolean, boolean, SVNDepth, String[])}
     *             instead
     */
    public SVNCommitPacket doCollectCommitItems(File[] paths, boolean keepLocks, boolean force, boolean recursive) throws SVNException {
        SVNCommitPacket[] packets = doCollectCommitItems(paths, keepLocks, force, SVNDepth.fromRecurse(recursive), true, null);
        if (packets != null && packets.length > 0) {
            return packets[0];
        }
        return SVNCommitPacket.EMPTY;
    }

    /**
     * Collects commit items (containing detailed information on each Working
     * Copy item that contains changes and need to be committed to the
     * repository) into a single {@link SVNCommitPacket}. Further this commit
     * packet can be passed to
     * {@link #doCommit(SVNCommitPacket, boolean, boolean, String, SVNProperties)}
     * .
     * 
     * <p/>
     * For more details on parameters, please, refer to
     * {@link #doCommit(File[], boolean, String, SVNProperties, String[], boolean, boolean, SVNDepth)}.
     * 
     * @param paths
     *            an array of local items which should be traversed to collect
     *            information on every changed item (one <b>SVNCommitItem</b>
     *            per each modified local item)
     * @param keepLocks
     *            if <span class="javakeyword">true</span> and there are local
     *            items that were locked then these items will be left locked
     *            after traversing all of them, otherwise the items will be
     *            unlocked
     * @param force
     *            forces collecting commit items for a non-recursive commit
     * @param depth
     *            tree depth to process
     * @param changelists
     *            changelist names array
     * @return commit packet containing commit items
     * @throws SVNException
     * @since 1.2.0
     */
    public SVNCommitPacket doCollectCommitItems(File[] paths, boolean keepLocks, boolean force, SVNDepth depth, String[] changelists) throws SVNException {
        SVNCommitPacket[] packets = doCollectCommitItems(paths, keepLocks, force, depth, true, changelists);
        if (packets != null && packets.length > 0) {
            return packets[0];
        }
        return SVNCommitPacket.EMPTY;
    }

    /**
     * Collects commit items (containing detailed information on each Working
     * Copy item that was changed and need to be committed to the repository)
     * into different <b>SVNCommitPacket</b>s.
     * 
     * <p/>
     * This method is identical to
     * <code>doCollectCommitItems(paths, keepLocks, force, SVNDepth.fromRecurse(recursive), combinePackets, null)</code>.
     * 
     * @param paths
     *            an array of local items which should be traversed to collect
     *            information on every changed item (one <b>SVNCommitItem</b>
     *            per each modified local item)
     * @param keepLocks
     *            if <span class="javakeyword">true</span> and there are local
     *            items that were locked then these items will be left locked
     *            after traversing all of them, otherwise the items will be
     *            unlocked
     * @param force
     *            forces collecting commit items for a non-recursive commit
     * @param recursive
     *            relevant only for directory items: if <span
     *            class="javakeyword">true</span> then the entire directory tree
     *            will be traversed including all child directories, otherwise
     *            only items located in the directory itself will be processed
     * @param combinePackets
     *            if <span class="javakeyword">true</span> then collected commit
     *            packets will be joined into a single one, so that to be
     *            committed in a single transaction
     * @return an array of commit packets
     * @throws SVNException
     * @deprecated use
     *             {@link #doCollectCommitItems(File[], boolean, boolean, SVNDepth, boolean, String[])}
     *             instead
     */
    public SVNCommitPacket[] doCollectCommitItems(File[] paths, boolean keepLocks, boolean force, boolean recursive, boolean combinePackets) throws SVNException {
        return doCollectCommitItems(paths, keepLocks, force, SVNDepth.fromRecurse(recursive), combinePackets, null);
    }

    /**
     * Collects commit items (containing detailed information on each Working
     * Copy item that was changed and need to be committed to the repository)
     * into different <code>SVNCommitPacket</code>s. This method may be
     * considered as an advanced version of the
     * {@link #doCollectCommitItems(File[], boolean, boolean, SVNDepth, String[])}
     * method. Its main difference from the aforementioned method is that it
     * provides an ability to collect commit items from different working copies
     * checked out from the same repository and combine them into a single
     * commit packet. This is attained via setting <code>combinePackets</code>
     * into <span class="javakeyword">true</span>. However even if
     * <code>combinePackets</code> is set, combining may only occur if (besides
     * that the paths must be from the same repository) URLs of
     * <code>paths</code> are formed of identical components, that is protocol
     * name, host name, port number (if any) must match for all paths. Otherwise
     * combining will not occur.
     * 
     * <p/>
     * Combined items will be committed in a single transaction.
     * 
     * <p/>
     * For details on other parameters, please, refer to
     * {@link #doCommit(File[], boolean, String, SVNProperties, String[], boolean, boolean, SVNDepth)}.
     * 
     * @param paths
     *            an array of local items which should be traversed to collect
     *            information on every changed item (one <b>SVNCommitItem</b>
     *            per each modified local item)
     * @param keepLocks
     *            if <span class="javakeyword">true</span> and there are local
     *            items that were locked then these items will be left locked
     *            after traversing all of them, otherwise the items will be
     *            unlocked
     * @param force
     *            forces collecting commit items for a non-recursive commit
     * @param depth
     *            tree depth to process
     * @param combinePackets
     *            whether combining commit packets into a single commit packet
     *            is allowed or not
     * @param changelists
     *            changelist names array
     * @return array of commit packets
     * @throws SVNException
     *             in the following cases:
     *             <ul>
     *             <li/>exception with {@link SVNErrorCode#ENTRY_MISSING_URL}
     *             error code - if working copy root of either path has no url
     *             </ul>
     * @since 1.2.0
     */
    public SVNCommitPacket[] doCollectCommitItems(File[] paths, boolean keepLocks, boolean force, SVNDepth depth, boolean combinePackets, String[] changelists) throws SVNException {
        SvnCommit commit = getOperationsFactory().createCommit();
        for (int i = 0; i < paths.length; i++) {
            commit.addTarget(SvnTarget.fromFile(paths[i]));
        }
        commit.setKeepLocks(keepLocks);
        commit.setDepth(depth);
        commit.setForce(force);
        commit.setFailOnMultipleRepositories(this.failOnMultipleRepositories);
        commit.setCommitParameters(SvnCodec.commitParameters(getCommitParameters()));
        commit.setCombinePackets(combinePackets);
        if (changelists != null && changelists.length > 0) {
            commit.setApplicalbeChangelists(Arrays.asList(changelists));
        }
        
        SvnCommitPacket packet = commit.collectCommitItems();  
        if (packet != null) {
            final SvnCommitPacket[] packets = commit.splitCommitPackets(combinePackets);
            final SVNCommitPacket[] result = new SVNCommitPacket[packets.length];
            for (int i = 0; i < packets.length; i++) {
                result[i] = SvnCodec.commitPacket(commit, packets[i]);
            }
            return result;
        }
        
        return new SVNCommitPacket[0];
    }

    public void setFailOnMultipleRepositories(boolean fail) {
        failOnMultipleRepositories = fail;
    }

}
