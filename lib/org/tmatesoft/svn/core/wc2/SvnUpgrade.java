package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;

/**
 * Represents operation for upgrading the metadata storage format for a working copy.
 * <code>Target</code> should represent working copy path to be upgraded.
 * 
 * <p/>
 * {@link #run()} returns {@link SvnWcGeneration} of resulting working copy.
 * {@link #run()} throws {@link org.tmatesoft.svn.core.SVNException} if the following is true:
 * <ul>
 * <li>exception with {@link SVNErrorCode#ILLEGAL_TARGET} error code 
 * - if <code>target</code> is not a local path
 * <li>exception with {@link SVNErrorCode#ENTRY_NOT_FOUND} error code 
 * - if <code>target</code> is not a versioned directory
 * <li>exception with {@link SVNErrorCode#WC_INVALID_OP_ON_CWD} error code 
 * - if <code>target</code> is not a pre-1.7 working copy directory
 * <li>exception with {@link SVNErrorCode#WC_INVALID_OP_ON_CWD} error code 
 * - if <code>target</code> is not a pre-1.7 working copy root
 * <li>exception with {@link SVNErrorCode#WC_UNSUPPORTED_FORMAT} error code 
 * - if a <code>target</code> doesn't have a repository URL
 * <li>exception with {@link SVNErrorCode#WC_CORRUPT} error code 
 * - if a working copy is corrupt
 * </ul> 
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 * @since 1.7 (SVN 1.7)
 */
public class SvnUpgrade extends SvnOperation<SvnWcGeneration> {
    
    protected SvnUpgrade(SvnOperationFactory factory) {
        super(factory);
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
