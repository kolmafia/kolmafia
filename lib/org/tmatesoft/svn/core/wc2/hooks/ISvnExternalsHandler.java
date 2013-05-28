package org.tmatesoft.svn.core.wc2.hooks;

import java.io.File;

import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.AbstractSvnUpdate;
import org.tmatesoft.svn.core.wc2.SvnRemoteCopy;

/**
 * Implementing this interface allows handle an external definition and control whether 
 * to skip externals definitions processing in some operations.  
 * 
 * <p/>
 * Such handlers, if provided, are used in update, wc-to-url copying operations.
 *  
 * @author TMate Software Ltd.
 * @version 1.7
 * @see AbstractSvnUpdate
 * @see SvnRemoteCopy
 */
public interface ISvnExternalsHandler {

    /**
     * Handles an external definition and says whether to skip it or not.
     * This method receives external definition parameters and returns whether 
     * <code>null</code> to indicate that this external definition must be excluded 
     * from processing (for example, not updated during an update), or a non-
     * <code>null</code> array. This array should contain at least two {@link SVNRevision}
     * objects [revision, pegRevision] which will be used by the operation instead of 
     * <code>externalRevision</code> and <code>externalPegRevision</code> respectively passed into 
     * this handle method.
     * 
     * <p/>
     * <code>externalWorkingRevision</code> is always {@link SVNRevision#UNDEFINED} for update/checkout operations. 
     * 
     * @param externalPath              path of the external to be processed
     * @param externalURL               URL of the external to be processed or <code>null</code> 
     *                                  if external is about to be removed
     * @param externalRevision          default revision to checkout/copy external at or update to
     * @param externalPegRevision       default peg revision to use for checkout/update/copy of external
     * @param externalsDefinition       raw <span class="javastring">svn:externals</span> property value
     * @param externalsWorkingRevision  current external working copy revision (relevant only for wc-to-url 
     *                                  copying operations)
     * @return                          array of {@link SVNRevision}s in form of {revision, pegRevision} or 
     *                                  <code>null</code> to skip processing 
     *                                  of this external
     */
    public SVNRevision[] handleExternal(File externalPath, SVNURL externalURL, SVNRevision externalRevision, 
            SVNRevision externalPegRevision, String externalsDefinition, SVNRevision externalsWorkingRevision);

}
