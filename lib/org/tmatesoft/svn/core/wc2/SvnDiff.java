package org.tmatesoft.svn.core.wc2;

import java.io.File;
import java.io.OutputStream;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc2.ng.ISvnDiffGenerator;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNewDiffGenerator;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnOldDiffGenerator;
import org.tmatesoft.svn.core.wc.DefaultSVNDiffGenerator;
import org.tmatesoft.svn.core.wc.ISVNDiffGenerator;
import org.tmatesoft.svn.core.wc.SVNDiffOptions;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.util.SVNLogType;
 
/**
 * Represents diff operation.
 * Produces diff output which describes the delta between <code>target</code>
 * in its <code>pegRevision</code>, as it changed between <code>startRevision</code> and <code>endRevision</code>,
 * or between first <code>target</code> at <code>startRevision</code> and second <code>target</code> at <code>endRevision</code>.
 * Writes the output of the diff to <code>output</code> stream.
 * 
 * <ul>
 * <li>
 * If it is diff between <code>startRevision</code> and <code>endRevision</code> of one <code>target</code>:
 * 
 * <p/>
 * <code>Target</code> can be either working copy path or URL.
 * 
 * <p/>
 * If <code>pegRevision</code> is {@link SVNRevision#isValid() invalid}, behaves identically to 
 * diff between two targets, using <code>target</code>'s path for both targets.
 * </li>
 * 
 * <li>
 * If it is diff between first <code>target</code> and second <code>target</code>:
 * 
 * <p/>
 * First and second <code>targets</code> can be either working copy path or URL, but cannot be both URLs.
 * If so {@link UnsupportedOperationException} is thrown.
 * 
 * <p/>
 * Both <code>targets</code> must represent the same node kind -- that is, if first <code>target</code> is a directory,
 * second <code>target</code> must also be, and if first <code>target</code> is a file,
 * second <code>target</code> must also be.
 * 
 * </li>
 * </ul>
 * 
 * <p/>
 * If this operation object uses {@link DefaultSVNDiffGenerator} and there was
 * a non-<code>null</code> {@link DefaultSVNDiffGenerator#setBasePath(File) base path} provided to
 * it, the original path and modified path will have this base path stripped
 * from the front of the respective paths. If the base path is not <null>null</null> 
 * but is not a parent path of the target, an exception with the {@link SVNErrorCode#BAD_RELATIVE_PATH} error code
 * is thrown.
 * 
 * <p/>
 * If <code>noDiffDeleted</code> or old {@link ISVNDiffGenerator#isDiffDeleted()} is <code>true</span>, then no diff output will be generated on
 * deleted files.
 * 
 * <p/>
 * Generated headers are encoded using {@link ISvnDiffGenerator#getEncoding()}.
 * 
 * <p/>
 * Diffs output will not be generated for binary files, unless
 * {@link ISvnDiffGenerator#isForcedBinaryDiff()} is <code>true</code>, in which case diffs will be shown
 * regardless of the content types.
 * 
 * <p/>
 * If this operation object uses {@link DefaultSVNDiffGenerator} then a caller
 * can set {@link SVNDiffOptions} to it which will be used to pass
 * additional options to the diff processes invoked to compare files.
 * 
 * <p/>
 * If <code>depth</code> is {@link SVNDepth#INFINITY}, diffs fully
 * recursively. Else if it is {@link SVNDepth#IMMEDIATES}, diffs the named
 * paths and their file children (if any), and diffs properties of
 * subdirectories, but does not descend further into the subdirectories.
 * Else if {@link SVNDepth#FILES}, behaves as if for
 * {@link SVNDepth#IMMEDIATES} except doesn't diff properties of
 * subdirectories. If {@link SVNDepth#EMPTY}, diffs exactly the named paths
 * but nothing underneath them.
 * 
 * <p/>
 * <code>ignoreAncestry</code> controls whether or not items being diffed will
 * be checked for relatedness first. Unrelated items are typically
 * transmitted to the editor as a deletion of one thing and the addition of
 * another, but if this flag is <code>false</code>,
 * unrelated items will be diffed as if they were related.
 * 
 * <p/>
 * <code>changeLists</code> is a collection of <code>String</code>
 * changelist names, used as a restrictive filter on items whose differences
 * are reported; that is, doesn't generate diffs about any item unless it's
 * a member of one of those changelists. If <code>changeLists</code> is
 * empty (or <code>null</code>), no changelist filtering
 * occurs.
 * 
 * <p/>
 * Note: changelist filtering only applies to diffs in which at least one
 * side of the diff represents working copy data.
 * 
 * <p/>
 * If both <code>startRevision</code> and <code>endRevision</code> is either {@link SVNRevision#WORKING} or
 * {@link SVNRevision#BASE}, then it will be a url-against-wc; otherwise, a
 * url-against-url diff.
 * 
 * <p/>
 * If <code>startRevision</code> is neither {@link SVNRevision#BASE}, nor
 * {@link SVNRevision#WORKING}, nor {@link SVNRevision#COMMITTED}, and if,
 * on the contrary, <code>endRevision</code> is one of the aforementioned revisions,
 * then a wc-against-url diff is performed; if <code>endRevision</code> also is not
 * one of those revision constants, then a url-against-url diff is
 * performed. Otherwise it's a url-against-wc diff.
 * 
 * <p/>
 * {@link #run()} method throws {@link SVNException} if one of the following is true:
 *             <ul>
 *             <li>exception with {@link SVNErrorCode#CLIENT_BAD_REVISION}
 *             error code - if either of <code>startRevision</code> and <code>endRevision</code>
 *             is {@link SVNRevision#isValid() invalid}; if both <code>startRevision</code> and <code>endRevision</code> are either
 *             {@link SVNRevision#WORKING} or {@link SVNRevision#BASE} 
 *             <li>exception with {@link SVNErrorCode#FS_NOT_FOUND} error code -
 *             <code>target</code> can not be found in either <code>startRevision</code>
 *             or <code>endRevision</code>
 *             </ul>
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnDiff extends SvnOperation<Void> {
    
    private ISvnDiffGenerator diffGenerator;
    private SVNDiffOptions diffOptions;
    private OutputStream output;

    private SvnTarget source;
    private SvnTarget firstSource;
    private SvnTarget secondSource;

    private SVNRevision startRevision;
    private SVNRevision endRevision;
    
    private boolean ignoreAncestry;
    private boolean noDiffDeleted;
    private boolean showCopiesAsAdds;
    private boolean ignoreContentType;
    private File relativeToDirectory;
    private boolean useGitDiffFormat;

    protected SvnDiff(SvnOperationFactory factory) {
        super(factory);

        setIgnoreAncestry(true);
    }

    /**
     * Sets the diff's <code>source</code> with start and end revisions for one-source type of operation.
     *
     * @param source source of the diff
     * @param start start revision of the diff
     * @param end end revision of the diff
     */
    public void setSource(SvnTarget source, SVNRevision start, SVNRevision end) {
        this.source = source;
        this.startRevision = start;
        this.endRevision = end;
        if (source != null) {
            setSources(null, null);
        }
    }

    /**
     * Sets both diff's <code>sources</code>.
     *
     * @param source1 first source of the diff
     * @param source2 second source of the diff
     */
    public void setSources(SvnTarget source1, SvnTarget source2) {
        this.firstSource = source1;
        this.secondSource = source2;
        if (firstSource != null) {
            setSource(null, null, null);
        }
    }

    /**
     * Gets the diff's <code>source</code> with start and end revisions for one-target type of operation.
     *
     * @return source of the diff
     */
    public SvnTarget getSource() {
        return source;
    }

    public SVNRevision getStartRevision() {
        return startRevision;
    }

    public SVNRevision getEndRevision() {
        return endRevision;
    }

    public SvnTarget getFirstSource() {
        return firstSource;
    }

    public SvnTarget getSecondSource() {
        return secondSource;
    }

    public void setRelativeToDirectory(File relativeToDirectory) {
        this.relativeToDirectory = relativeToDirectory;
    }

    public File getRelativeToDirectory() {
        return relativeToDirectory;
    }

    /**
     * Returns operation's diff generator.
     * If not set, {@link DefaultSVNDiffGenerator} is used.
     * 
     * @return diff generator of the operation
     */
    public ISvnDiffGenerator getDiffGenerator() {
        return diffGenerator;
    }

    /**
     * Sets operation's diff generator of type ISVNDiffGenerator.
     * Used for compatibility with 1.6 version.
     * 
     * @param diffGenerator diff generator of the operation of type ISVNDiffGenerator
     */
    public void setDiffGenerator(ISVNDiffGenerator diffGenerator) {
        if (diffGenerator == null) {
            setDiffGenerator((ISvnDiffGenerator) null);
        } else if (diffGenerator instanceof SvnNewDiffGenerator) {
            setDiffGenerator(((SvnNewDiffGenerator) diffGenerator).getDelegate());
        } else {
            setDiffGenerator(new SvnOldDiffGenerator(diffGenerator));
        }
    }

    /**
     * Sets operation's diff generator.
     * 
     * @param diffGenerator diff generator of the operation
     */
    public void setDiffGenerator(ISvnDiffGenerator diffGenerator) {
        this.diffGenerator = diffGenerator;
    }

    /**
     * Returns the operation's diff options controlling white-spaces and eol-styles.
     * 
     * @return diff options of the operation
     */
    public SVNDiffOptions getDiffOptions() {
        return diffOptions;
    }

    /**
     * Sets the operation's diff options controlling white-spaces and eol-styles.
     * 
     * @param diffOptions diff options of the operation
     */
    public void setDiffOptions(SVNDiffOptions diffOptions) {
        this.diffOptions = diffOptions;
    }

    /**
     * Returns output stream where the differences will be written to.
     * 
     * @return output stream of the diff's result
     */
    public OutputStream getOutput() {
        return output;
    }

    /**
     * Sets output stream where the differences will be written to.
     * 
     * @param output output stream of the diff's result
     */
    public void setOutput(OutputStream output) {
        this.output = output;
    }

    /**
     * Returns the paths ancestry should not be noticed while calculating differences.
     * 
     * @return <code>true</code> if the paths ancestry should not be noticed while calculating differences, otherwise <code>false</code>
     * @see #setIgnoreAncestry(boolean)
     */
    public boolean isIgnoreAncestry() {
        return ignoreAncestry;
    }

    /**
     * Sets whether or not items being diffed should
     * be checked for relatedness first. Unrelated items are typically
     * transmitted to the editor as a deletion of one thing and the addition of
     * another, but if this flag is <code>false</code>,
     * unrelated items will be diffed as if they were related.
     * 
     * @param ignoreAncestry <code>true</code> if the paths ancestry should not be noticed while calculating differences, otherwise <code>false</code>
     */
    public void setIgnoreAncestry(boolean ignoreAncestry) {
        this.ignoreAncestry = ignoreAncestry;
    }

    /**
     * Returns whether to generate differences for deleted files.   
     * In 1.6 version it was {@link ISVNDiffGenerator#isDiffDeleted()}.
     * 
     * @return <code>true</code> if deleted files should not be diffed, otherwise <code>false</code>
     */
    public boolean isNoDiffDeleted() {
        return noDiffDeleted;
    }

    /**
     * Sets whether to generate differences for deleted files.   
     * In 1.6 version it was {@link org.tmatesoft.svn.core.wc.ISVNDiffGenerator#setDiffDeleted(boolean)}.
     * 
     * @param noDiffDeleted <code>true</code> if deleted files should not be diffed, otherwise <code>false</code>
     */
    public void setNoDiffDeleted(boolean noDiffDeleted) {
        this.noDiffDeleted = noDiffDeleted;
    }

    /**
     * Returns whether to report copies and moves as it were adds. 
     * 
     * @return <code>true</code> if copies and moves should be reported as adds, otherwise <code>false</code>
     * @since 1.7, SVN 1.7
     */
    public boolean isShowCopiesAsAdds() {
        return showCopiesAsAdds;
    }

    /**
     * Sets whether to report copies and moves as it were adds. 
     * 
     * @param showCopiesAsAdds <code>true</code> if copies and moves should be reported as adds, otherwise <code>false</code>
     * @since 1.7, SVN 1.7
     */
    public void setShowCopiesAsAdds(boolean showCopiesAsAdds) {
        this.showCopiesAsAdds = showCopiesAsAdds;
    }

    public boolean isIgnoreContentType() {
        return ignoreContentType;
    }

    public void setIgnoreContentType(boolean ignoreContentType) {
        this.ignoreContentType = ignoreContentType;
    }

    /**
     * Returns whether to report in Git diff format. 
     * 
     * @return <code>true</code> if report should be in report in Git diff format, otherwise <code>false</code>
     * @since 1.7
     */
    public boolean isUseGitDiffFormat() {
        return useGitDiffFormat;
    }

    /**
     * Sets whether to report in Git diff format. 
     * 
     * @param useGitDiffFormat <code>true</code> if report should be in report in Git diff format, otherwise <code>false</code>
     * @since 1.7
     */
    public void setUseGitDiffFormat(boolean useGitDiffFormat) {
        this.useGitDiffFormat = useGitDiffFormat;
    }

    @Override
    protected int getMinimumTargetsCount() {
        return super.getMinimumTargetsCount();
    }

    @Override
    protected int getMaximumTargetsCount() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected void ensureArgumentsAreValid() throws SVNException {
        if (getRelativeToDirectory() != null && hasRemoteTargets()) {
            //TODO
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "Relative directory cannot be specified with remote targets");
            SVNErrorManager.error(err, SVNLogType.CLIENT);
        }

        if (getOutput() == null) {
            //TODO
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "No output is specified.");
            SVNErrorManager.error(err, SVNLogType.CLIENT);
        }
    }

    @Override
    protected File getOperationalWorkingCopy() {
        if (getSource() != null && getSource().isFile()) {
            return getSource().getFile();
        } else {
            if (getFirstSource() != null && getFirstSource().isFile()) {
                return getFirstSource().getFile();
            }

            if (getSecondSource() != null && getSecondSource().isFile()) {
                return getSecondSource().getFile();
            }
        }

        return null;
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
