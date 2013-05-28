package org.tmatesoft.svn.core.wc2;

import java.io.File;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * Represents export operation.
 * Exports the contents of either a subversion repository or a subversion
 * working copy (<code>source</code>) into a <code>target</code> - 'clean' directory (meaning a directory with no
 * administrative directories).
 * 
 * <p/>
 * <code>source</code>'s <code>pegRevision</code> is the revision where the path is first looked
 * up when exporting from a repository. If <code>source</code>'s <code>pegRevision</code> is
 * {@link SVNRevision#UNDEFINED}, then it defaults to
 * {@link SVNRevision#WORKING}.
 * 
 * <p/>
 * If <code>revision</code> is one of:
 * <ul>
 * <li/>{@link SVNRevision#BASE}
 * <li/>{@link SVNRevision#WORKING}
 * <li/>{@link SVNRevision#COMMITTED}
 * </ul>
 * then local export is performed. Otherwise exporting from the repository.
 * If <code>revision</code> is {@link SVNRevision#UNDEFINED} it defaults to {@link SVNRevision#WORKING}.
 * 
 * <p/>
 * If externals are ignored (<code>ignoreExternals</code> is <code>true</code>), doesn't process
 * externals definitions as part of this operation.
 * 
 * <p/>
 * <code>eolStyle</code> allows you to override the standard eol marker on
 * the platform you are running on. Can be either "LF", "CR" or "CRLF" or
 * <code>null</code>. If <code>null</code> will use the standard eol marker. Any
 * other value will cause an exception with the error code
 * {@link SVNErrorCode#IO_UNKNOWN_EOL} error to be returned.
 * 
 * <p>
 * If <code>depth</code> is {@link SVNDepth#INFINITY}, exports fully
 * recursively. Else if it is {@link SVNDepth#IMMEDIATES}, exports
 * <code>source</code> and its immediate children (if any), but with
 * subdirectories empty and at{@link SVNDepth#EMPTY}. Else if
 * {@link SVNDepth#FILES}, exports <code>source</code> and its immediate
 * file children (if any) only. If <code>depth</code> is
 * {@link SVNDepth#EMPTY}, then exports exactly <code>source</code> and
 * none of its children.
 * 
 * <p/>
 * {@link #run()} method returns value of the revision actually exported.
 * 
 * <p/>
 * {@link #run()} throws {@link org.tmatesoft.svn.core.SVNException} in the following cases:
 *             <ul>
 *             <li/>exception with {@link SVNErrorCode#IO_ERROR} error code 
 *             - if <code>target</code>'s directory already exists and <code>force</code> is <code>false</code>
 *             <li/>exception with {@link SVNErrorCode#ILLEGAL_TARGET} error code 
 *             - if destination file already exists and <code>force</code> is <code>false</code>,
 *             	or if destination directory exists and should be overridden by source file
 *             </ul>
 *              
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnExport extends AbstractSvnUpdate<Long> {
    
    private boolean force;
    private boolean expandKeywords;
    private String eolStyle;
    private SvnTarget source;

    protected SvnExport(SvnOperationFactory factory) {
        super(factory);
    }
    
    /**
     * Gets whether to overwrite files or directories.
     * 
     * @return <code>true</code> if export should overwrite files or directories, otherwise <code>false</code>
     */
    public boolean isForce() {
        return force;
    }

    /**
     * Gets whether or not all keywords presenting in the file and listed in the file's
     * {@link org.tmatesoft.svn.core.SVNProperty#KEYWORDS}property (if set) should be substituted.
     * 
     * @return <code>true</code> if keywords should expanded, otherwise <code>false</code>
     * @since 1.7, SVN 1.7
     */
    public boolean isExpandKeywords() {
        return expandKeywords;
    }

    /**
     * Returns the string that denotes a specific End-Of-Line character.
     * 
     * @return specific End-Of-Line character of the operation
     * @see #setEolStyle(String)
     */
    public String getEolStyle() {
        return eolStyle;
    }

    /**
     * Sets whether to overwrite files or directories.
     * 
     * @param force <code>true</code> if export should overwrite files or directories, otherwise <code>false</code>
     */
    public void setForce(boolean force) {
        this.force = force;
    }

    /**
     * Sets whether or not all keywords presenting in the file and listed in the file's
     * {@link org.tmatesoft.svn.core.SVNProperty#KEYWORDS}property (if set) should be substituted.
     * 
     * @param expandKeywords <code>true</code> if keywords should expanded, otherwise <code>false</code>
     * @since 1.7, SVN 1.7
     */
    public void setExpandKeywords(boolean expandKeywords) {
        this.expandKeywords = expandKeywords;
    }

    /**
     * Sets the string that denotes a specific End-Of-Line character.
     * 
     * <code>eolStyle</code> allows you to override the standard eol marker on
     * the platform you are running on. Can be either "LF", "CR" or "CRLF" or
     * <code>null</code>. If <code>null</code> will use the standard eol marker. Any
     * other value will cause an exception with the error code
     * {@link SVNErrorCode#IO_UNKNOWN_EOL} error to be returned.
     * 
     * @param eolStyle specific End-Of-Line character of the operation
     */
    public void setEolStyle(String eolStyle) {
        this.eolStyle = eolStyle;
    }

    /**
     * Returns export's source - working copy path or repository URL.
     * 
     * @return source of the export 
     */
    public SvnTarget getSource() {
        return source;
    }
    
    /**
     * Sets export's source - working copy path or repository URL.
     * 
     * @param source source of the export 
     */
    public void setSource(SvnTarget source) {
        this.source = source;
    }

    @Override
    protected void ensureArgumentsAreValid() throws SVNException {
        if (getFirstTarget() == null || !getFirstTarget().isLocal()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, "Destination path is required for export.");
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        if (getSource() == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, "Source is required for export.");
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        if (getDepth() == null || getDepth() == SVNDepth.UNKNOWN) {
            setDepth(SVNDepth.INFINITY);
        }
        if (getRevision() == SVNRevision.UNDEFINED) {            
            setRevision(getSource().getResolvedPegRevision());
        }
        super.ensureArgumentsAreValid();
    }

    @Override
    protected void initDefaults() {
        super.initDefaults();
        setExpandKeywords(true);
        setDepth(SVNDepth.INFINITY);
    }

    @Override
    protected File getOperationalWorkingCopy() {
        if (getSource().isLocal()) {
            return getSource().getFile();
        }
        return null;
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
