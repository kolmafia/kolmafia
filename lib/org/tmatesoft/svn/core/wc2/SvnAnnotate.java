package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.wc.ISVNAnnotateHandler;
import org.tmatesoft.svn.core.wc.SVNDiffOptions;
import org.tmatesoft.svn.core.wc.SVNRevision;

/**
 * Represents annotate operation. 
 * Obtains and reports annotation information for each line-blame item
 * associated with revision <code>endRevision</code> of <code>target</code>, using
 * <code>startRevision</code> as the default source of all blame. 
 * Passes annotation information to a annotation handler if provided.
 * 
 * <p/>
 * <code>Target</code> can represent URL or working copy path (used to get corresponding URLs).
 * 
 * <p/>
 * <code>Target</code>'s <code>pegRevision</code> indicates in which revision <code>target</code> is
 * valid. If <code>pegRevision</code> is {@link SVNRevision#UNDEFINED}, then
 * it defaults to {@link SVNRevision#HEAD}.
 * 
 * <p/>
 * <ul>
 * <li>
 * <b>If working copy is SVN 1.7 working copy:</b>
 * <p/>
 * If <code>endRevision</code> is {@link SVNRevision#UNDEFINED}, 
 * then it defaults to {@link SVNRevision#HEAD} if <code>target</code>
 * is URL or {@link SVNRevision#WORKING} if <code>target</code> is working copy path.
 * </li>
 * <li>
 * <b>If working copy is SVN 1.6 working copy:</b>
 * <p/>
 * If <code>startRevision</code> is <code>null</code> or
 * {@link SVNRevision#isValid() invalid}, then it defaults to revision 1. 
 * If <code>endRevision</code> is <code>null</code> or
 * {@link SVNRevision#isValid() invalid}, then it defaults to 
 * <code>target</code>'s <code>pegRevision</code>.
 * </li>
 * </ul>
 * 
 * <p/>
 * Note: this operation requires repository access.
 * 
 * <p/>
 * {@link #run()} method returns {@link SvnAnnotateItem} information reported by the operation.
 * {@link #run()} throws {@link org.tmatesoft.svn.core.SVNException} in the following cases:
 *             <ul>
 *             <li/>exception with {@link SVNErrorCode#CLIENT_BAD_REVISION} error code 
 *             - if <code>startRevision</code> is older than <code> endRevision</code> 
 *             <li/>exception with {@link SVNErrorCode#CLIENT_BAD_REVISION} error code 
 *             - if both <code>startRevision</code> and <code> endRevision</code> are either <code>null</code> or
 *             {@link SVNRevision#isValid() invalid} 
 *             <li/>exception with {@link SVNErrorCode#CLIENT_IS_BINARY_FILE} error code 
 *             - if any of the revisions of <code>target</code>'s path have a binary
 *             mime-type, unless <code>ignoreMimeType</code> is <code>true</code>, in which case blame
 *             information will be generated regardless of the MIME types of
 *             the revisions
 *             <li/>exception with {@link SVNErrorCode#UNSUPPORTED_FEATURE} error code - 
 *             if either <code>startRevision</code> or <code>endRevision
 *             </code> is {@link SVNRevision#WORKING} (for SVN 1.6 working copy only).
 *             </ul>
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnAnnotate extends SvnReceivingOperation<SvnAnnotateItem> {
    
    private boolean useMergeHistory;
    private boolean ignoreMimeType;
    private ISVNAnnotateHandler handler;
    private SVNRevision startRevision;
    private SVNRevision endRevision;
    private String inputEncoding;
    private SVNDiffOptions diffOptions;
    
    protected SvnAnnotate(SvnOperationFactory factory) {
        super(factory);
    }
    
    /**
     * Gets the caller's handler to process annotation information.
     * 
     * @return handler to process annotation information if set
     */
    public ISVNAnnotateHandler getHandler() {
        return handler;
    }

    /**
     * Sets the caller's handler to process annotation information.
     * 
     * @param handler handler to process annotation information
     */
    public void setHandler(ISVNAnnotateHandler handler) {
        this.handler = handler;
    }

    /**
     * Gets whether or not data based upon revisions which have been merged to targets also should be returned.
     * 
     * @return <code>true</code> if merged history should be used, otherwise <code>false</code>
     */
    public boolean isUseMergeHistory() {
        return useMergeHistory;
    }

    /**
     * Sets whether or not data based upon revisions which have been merged to targets also should be returned.
     * 
     * @param useMergeHistory <code>true</code> if merged history should be use, otherwise <code>false</code>
     */
    public void setUseMergeHistory(boolean useMergeHistory) {
        this.useMergeHistory = useMergeHistory;
    }

    /**
     * Gets whether or not operation should be run on all files treated as text, 
     * no matter what SVNKit has inferred from the mime-type property.
     * 
     * @return <code>true</code> if mime types should be ignored, otherwise <code>false</code>
     */
    public boolean isIgnoreMimeType() {
        return ignoreMimeType;
    }

    /**
     * Sets whether or not operation should be run on all files treated as text, 
     * no matter what SVNKit has inferred from the mime-type property.
     * 
     * @param ignoreMimeType <code>true</code> if mime types should be ignored, otherwise <code>false</code>
     */
    public void setIgnoreMimeType(boolean ignoreMimeType) {
        this.ignoreMimeType = ignoreMimeType;
    }
    
    /**
     * Gets the revision of the operation to start from.
     * 
     * @return revision to start from
     */
    public SVNRevision getStartRevision() {
        return startRevision;
    }

    /**
     * Sets the revision of the operation to start from.
     * 
     * @param startRevision revision to start from
     */
    public void setStartRevision(SVNRevision startRevision) {
        this.startRevision = startRevision;
    }
    
    /**
     * Gets the revision of the operation to end with.
     * 
     * @return revision to end with
     */
    public SVNRevision getEndRevision() {
        return endRevision;
    }

    /**
     * Sets the revision of the operation to end with.
     * 
     * @param endRevision revision to end with
     */
    public void setEndRevision(SVNRevision endRevision) {
        this.endRevision = endRevision;
    }
    
    /**
     * Gets the name of character set to decode input bytes.
     * 
     * @return name of character set
     */
    public String getInputEncoding() {
        return inputEncoding;
    }

    /**
     * Sets the name of character set to decode input bytes.
     * 
     * @param inputEncoding name of character set
     */
    public void setInputEncoding(String inputEncoding) {
        this.inputEncoding = inputEncoding;
    }
    
    /**
     * Gets diff options for the operation.
     * 
     * @return diff options
     */
    public SVNDiffOptions getDiffOptions() {
        return diffOptions;
    }
    
    /**
     * Sets diff options for the operation.
     * 
     * @param diffOptions diff options
     */
    public void setDiffOptions(SVNDiffOptions diffOptions) {
    	this.diffOptions = diffOptions;
    }

    /**
     * Gets whether the operation changes working copy
     * @return <code>true</code> if the operation changes the working copy, otherwise <code>false</code>
     */
    @Override
    public boolean isChangesWorkingCopy() {
        return false;
    }
}
