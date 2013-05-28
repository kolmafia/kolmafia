package org.tmatesoft.svn.core.wc2;

import java.io.File;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * Produces a diff summary which lists the changed items between
 * <code>source</code> in its <code>pegRevision</code>, as it changed
 * between <code>startRevision</code> and <code>endRevision</code>,
 * or diff summary between <code>firstSource</code> at its <code>pegRevision</code> 
 * and <code>secondSource</code> at its <code>pegRevision</code>.
 * Changes are produced without creating text deltas.
 * 
 * <ul>
 * <li>
 * If it is diff between <code>startRevision</code> and <code>endRevision</code> of one <code>source</code>:
 * 
 * <p/>
 * <code>Source</code> can be either working copy path or URL.
 * 
 * <p/>
 * If <code>pegRevision</code> is {@link SVNRevision#isValid() invalid}, behaves identically to 
 * diff between two sources, using <code>source</code>'s path for both sources.
 * </li>
 * 
 * <li>
 * If it is diff between first <code>source</code> and second <code>source</code>:
 * 
 * <p/>
 * First and second <code>sources</code> can be either working copy path or URL.
 * 
 * <p/>
 * Both <code>sources</code> must represent the same node kind -- that is, if first <code>source</code> is a directory,
 * second <code>sources</code> must also be, and if first <code>sources</code> is a file,
 * second <code>sources</code> must also be.
 * 
 * </li>
 * </ul>
 * 
 * The operation may report false positives if <code>ignoreAncestry</code> is
 * <code>false</code>, since a file might have been
 * modified between two revisions, but still have the same contents.
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
 * {@link #run()} method throws {@link SVNException} in the following cases:
 *             <ul>
 *             <li/>exception with {@link SVNErrorCode#CLIENT_BAD_REVISION}
 *             error code - if either <code>startRevision</code> or <code>endRevision</code> is
 *             {@link SVNRevision#isValid() invalid} 
 *             <li/>exception with
 *             {@link SVNErrorCode#UNSUPPORTED_FEATURE} error code - if
 *             either of <code>startRevision</code> or </code>endRevision</code> is either
 *             {@link SVNRevision#WORKING} or {@link SVNRevision#BASE}
 *             </ul>
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnDiffSummarize extends SvnReceivingOperation<SvnDiffStatus> {

    private SvnTarget firstSource;
    private SvnTarget secondSource;
    
    private SvnTarget source;
    private SVNRevision startRevision;
    private SVNRevision endRevision;
    
    private boolean ignoreAncestry;

    protected SvnDiffSummarize(SvnOperationFactory factory) {
        super(factory);
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
    public boolean isIgnoreAncestry() {
        return ignoreAncestry;
    }
    public void setIgnoreAncestry(boolean ignoreAncestry) {
        this.ignoreAncestry = ignoreAncestry;
    }

    @Override
    protected File getOperationalWorkingCopy() {
        if (getSource() != null && getSource().isFile()) {
            return getSource().getFile();
        } else if (getFirstSource() != null && getFirstSource().isFile()) {
            return getFirstSource().getFile();
        } else if (getSecondSource() != null && getSecondSource().isFile()) {
            return getSecondSource().getFile();
        }
        
        return null;
    }

    @Override
    protected void ensureArgumentsAreValid() throws SVNException {
        if (getSource() == null || getSource().getPegRevision() == null || getSource().getPegRevision() == SVNRevision.UNDEFINED) {
            final SvnTarget firstSource = getFirstSource();
            final SvnTarget secondSource = getSecondSource();
            ensureArgumentsAreValid(firstSource.getURL(), firstSource.getFile(), firstSource.getPegRevision(),
                    secondSource.getURL(), secondSource.getFile(), secondSource.getPegRevision(),
                    null);
        } else {
            final SvnTarget source = getSource();
            ensureArgumentsAreValid(source.getURL(), source.getFile(), getStartRevision(),
                    source.getURL(), source.getFile(), getEndRevision(),
                    source.getPegRevision());
        }
    }

    private void ensureArgumentsAreValid(SVNURL url1, File path1, SVNRevision revision1,
                                         SVNURL url2, File path2, SVNRevision revision2,
                                         SVNRevision pegRevision) throws SVNException {
        if (pegRevision == null) {
            pegRevision = SVNRevision.UNDEFINED;
        }
        ensureRevisionIsValid(revision1);
        ensureRevisionIsValid(revision2);

        final boolean isPath1Local = startRevision == SVNRevision.WORKING || startRevision == SVNRevision.BASE;
        final boolean isPath2Local = endRevision == SVNRevision.WORKING || endRevision == SVNRevision.BASE;

        final boolean isRepos1;
        final boolean isRepos2;

        if (pegRevision != SVNRevision.UNDEFINED) {
            if (isPath1Local && isPath2Local) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_BAD_REVISION, "At least one revision must be non-local for a pegged diff");
                SVNErrorManager.error(err, SVNLogType.DEFAULT);
            }

            isRepos1 = !isPath1Local;
            isRepos2 = !isPath2Local;
        } else {
            isRepos1 = !isPath1Local || (url1 != null);
            isRepos2 = !isPath2Local || (url2 != null);
        }

        if (!isRepos1 || !isRepos2) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "Summarizing diff can only compare repository to repository");
            SVNErrorManager.error(err, SVNLogType.DEFAULT);
        }
    }

    private void ensureRevisionIsValid(SVNRevision revision) throws SVNException {
        final boolean revisionIsValid = revision != null && revision.isValid();
        if (!revisionIsValid) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_BAD_REVISION, "Not all required revisions are specified");
            SVNErrorManager.error(err, SVNLogType.DEFAULT);
        }
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
