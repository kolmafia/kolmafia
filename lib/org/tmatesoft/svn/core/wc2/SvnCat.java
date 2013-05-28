package org.tmatesoft.svn.core.wc2;

import java.io.OutputStream;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNRevision;

/**
 * Represents cat operation. Outputs the content of file identified by <code>target</code> and 
 * revision to the output streams. 
 * 
 * <p/>
 * The actual node 
 * revision selected is determined by the <code>target</code> as it exists in 
 * <code>target</code>'s <code>pegRevision</code>. 
 * If <code>target</code> is URL and its <code>pegRevision</code> is 
 * {@link SVNRevision#UNDEFINED}, then it defaults to {@link SVNRevision#HEAD}. 
 * If <code>target</code> is local and its <code>pegRevision</code> is 
 * {@link SVNRevision#UNDEFINED}, then it defaults to {@link SVNRevision#WORKING}.
 * 
 * <p/>
 * If <code>revision</code> is one of:
 * <ul>
 * <li>{@link SVNRevision#BASE}
 * <li>{@link SVNRevision#WORKING}
 * <li>{@link SVNRevision#COMMITTED}
 * </ul>
 * then the file contents are taken from the working copy file item (no
 * network connection is needed). Otherwise the file item's contents are
 * taken from the repository at a particular revision.
 * 
 * {@link #run()} throws {@link org.tmatesoft.svn.core.SVNException} in the following cases:
 *             <ul>
 *             <li>exception with {@link SVNErrorCode#CLIENT_IS_DIRECTORY}
 *             error code - if <code>target</code> refers to a directory 
 *             <li>exception with {@link SVNErrorCode#UNVERSIONED_RESOURCE}
 *             error code - if <code>target</code> is not under version control
 *             <li>it's impossible to create temporary files (
 *             {@link java.io.File#createTempFile(java.lang.String,java.lang.String)
 *             createTempFile()}fails) necessary for file translating (used when <code>target</code> is URL)
 *             </ul>
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnCat extends SvnOperation<Void> {

    private boolean expandKeywords;
    private OutputStream output;

    protected SvnCat(SvnOperationFactory factory) {
        super(factory);
    }
    
    /**
     * Gets whether or not all keywords presenting in the file and listed in the file's
     * {@link org.tmatesoft.svn.core.SVNProperty#KEYWORDS}property (if set) should be substituted.
     * 
     * @return <code>true</code> if keywords should expanded, otherwise <code>false</code>
     */
    public boolean isExpandKeywords() {
        return expandKeywords;
    }

    /**
     * Sets whether or not all keywords presenting in the file and listed in the file's
     * {@link org.tmatesoft.svn.core.SVNProperty#KEYWORDS}property (if set) should be substituted.
     * 
     * @param expandKeywords <code>true</code> if keywords should expanded, otherwise <code>false</code>
     */
    public void setExpandKeywords(boolean expandKeywords) {
        this.expandKeywords = expandKeywords;
    }

    /**
     * Gets the output stream of the operation.
     * 
     * @return output stream
     */
    public OutputStream getOutput() {
        return output;
    }

    /**
     * Sets the output stream of the operation.
     * 
     * @param output output stream
     */
    public void setOutput(OutputStream output) {
        this.output = output;
    }

    @Override
    protected void ensureArgumentsAreValid() throws SVNException {
        super.ensureArgumentsAreValid();

        //here we assume we have one target

        SVNRevision resolvedPegRevision;
        SVNRevision resolvedRevision;

        if (getFirstTarget().getPegRevision() == SVNRevision.UNDEFINED) {
            resolvedPegRevision = getFirstTarget().getResolvedPegRevision(SVNRevision.HEAD, SVNRevision.WORKING);
            if (getRevision() == null || getRevision() == SVNRevision.UNDEFINED) {
                resolvedRevision = getFirstTarget().isURL() ? SVNRevision.HEAD : SVNRevision.BASE;
            } else {
                resolvedRevision = getRevision();
            }
        } else {
            resolvedPegRevision = getFirstTarget().getPegRevision();
            if (getRevision() == null || getRevision() == SVNRevision.UNDEFINED) {
                resolvedRevision = resolvedPegRevision;
            } else {
                resolvedRevision = getRevision();
            }
        }

        setRevision(resolvedRevision);
        setSingleTarget(
                getFirstTarget().isURL() ?
                        SvnTarget.fromURL(getFirstTarget().getURL(), resolvedPegRevision) :
                        SvnTarget.fromFile(getFirstTarget().getFile(), resolvedPegRevision));
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
