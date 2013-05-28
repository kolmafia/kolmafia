package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * Represents checkout operation. Checks out a working copy of <code>source</code> at revision,
 * looked up at {@link SvnTarget#getPegRevision()}, using <code>target</code> as the
 * root directory of the newly checked out working copy.
 * 
 * <p/>
 * If source {@link SvnTarget#getPegRevision()}> is {@link SVNRevision#UNDEFINED} or invalid, then it
 * defaults to {@link SVNRevision#HEAD}.
 * 
 * <p/>
 * If <code>depth</depth> is {@link SVNDepth#INFINITY}, checks out fully
 * recursively. Else if it is {@link SVNDepth#IMMEDIATES}, checks out
 * source <code>target</code> and its immediate entries (subdirectories will be
 * present, but will be at depth {@link SVNDepth#EMPTY} themselves); else
 * {@link SVNDepth#FILES}, checks out source <code>target</code> and its file entries,
 * but no subdirectories; else if {@link SVNDepth#EMPTY}, checks out
 * source <code>target</code> as an empty directory at that depth, with no entries
 * present.
 * 
 * <p/>
 * If <code>depth</depth> is {@link SVNDepth#UNKNOWN}, then behave as if for
 * {@link SVNDepth#INFINITY}, except in the case of resuming a previous
 * checkout of <code>target</code> (i.e., updating), in which case uses the
 * depth of the existing working copy.
 * 
 * <p/>
 * If externals are ignored (<code>ignoreExternals</code> is <code>true</code>), doesn't process
 * externals definitions as part of this operation.
 * 
 * <p/>
 * If <code>isAllowUnversionedObstructions</code> is <code>true</code> 
 * then the checkout tolerates existing
 * unversioned items that obstruct added paths from source target. Only
 * obstructions of the same type (file or directory) as the added item are
 * tolerated. The text of obstructing files is left as-is, effectively
 * treating it as a user modification after the checkout. Working properties
 * of obstructing items are set equal to the base properties. If
 * <code>isAllowUnversionedObstructions</code> is <code>false</code> 
 * then the checkout will abort if there
 * are any unversioned obstructing items.
 * 
 * <p/>
 * If the caller's {@link ISVNEventHandler} is non-<code>null</code>, 
 * it is invoked as the checkout processes.
 * Also {@link ISVNEventHandler#checkCancelled()} will be used at various
 * places during the checkout to check whether the caller wants to stop the
 * checkout.
 * 
 * <p/>
 * This operation requires repository access (in case the repository is not
 * on the same machine, network connection is established).
 *
 * <p/>
 * {@link #run()} method returns value of the revision actually checked out from the repository.
 * 
 * <p/>
 * {@link #run()} throws {@link org.tmatesoft.svn.core.SVNException} in the following cases:
 *             <ul>
 *             <li/>exception with {@link SVNErrorCode#UNSUPPORTED_FEATURE}
 *             error code - if <code>target</code>'s URL refers to a file rather than a directory 
 *             <li/>exception with {@link SVNErrorCode#RA_ILLEGAL_URL} error code 
 *             - if <code>target</code>'s URL
 *             does not exist, or if external item at the <code>revision</code> doesn't exist, 
 *             or if external item at the <code>revision</code> is not file or a directory
 *             <li/>exception with {@link SVNErrorCode#WC_OBSTRUCTED_UPDATE} error code  
 *             - if the working copy item 
 *             is already a working copy item for a different URL 
 *             <li/>exception with {@link SVNErrorCode#WC_NODE_KIND_CHANGE} error code 
 *             - if local file is already exists with the same name that incoming added directory
 *        	   <li/>exception with {@link SVNErrorCode#ENTRY_MISSING_URL} error code 
 *        		- if working copy item that has no URL
 *       </ul>
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnCheckout extends AbstractSvnUpdate<Long> {
    
    private SvnTarget source;

    protected SvnCheckout(SvnOperationFactory factory) {
        super(factory);
    }

    /**
    * Gets a repository location from where a working copy will be checked out.
    * 
    * @return source of repository
    */
    public SvnTarget getSource() {
        return source;
    }

    /**
     * Sets a repository location from where a working copy will be checked out.
     * 
     * @param source source of repository
     */
    public void setSource(SvnTarget source) {
        this.source = source;
    }

    @Override
    protected void ensureArgumentsAreValid() throws SVNException {
        super.ensureArgumentsAreValid();
        if (getRevision() == null) {
            setRevision(SVNRevision.UNDEFINED);
        }
        
        if (getSource() == null || !getSource().isURL() || getSource().getURL() == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.BAD_URL);
            SVNErrorManager.error(err, SVNLogType.WC);
        }

        if (!getRevision().isValid() && getFirstTarget() != null) {
            setRevision(getSource().getResolvedPegRevision());            
        }
        if (!getRevision().isValid()) {
            setRevision(SVNRevision.HEAD);
        }
        
        if (getFirstTarget() == null || getFirstTarget().getFile() == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.BAD_FILENAME, "Checkout destination path can not be NULL");
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        
        if (getRevision().getNumber() < 0 && getRevision().getDate() == null && getRevision() != SVNRevision.HEAD) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_BAD_REVISION);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
    }
    
    /**
     * Gets whether the operation changes working copy
     * @return <code>true</code> if the operation changes the working copy, otherwise <code>false</code>
     */
    @Override
    public boolean isChangesWorkingCopy() {
        return true;
    }
}
