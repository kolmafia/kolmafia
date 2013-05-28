package org.tmatesoft.svn.core.wc2;

/**
 * Represents status operation as local working copy summary status for <code>target</code> and all its children.
 * {@link #run()} method returns {@link SvnStatusSummary} object which
 * describe the summary status of the working copy <code>target</code> and all its children.
 *  * 
 *
 * <p/>
 * Reported all entries, unmodified, local modifications out of date.
 * 
 * <p/>
 * Doesn't connect to repository.
 * 
 * <p/>
 * <code>changeLists</code> are not used.
 * 
 * <p/>
 * Externals are not processed.
 *
 * @author TMate Software Ltd.
 * @version 1.7
 * @since 1.7
 * @see SvnStatusSummary
 */
public class SvnGetStatusSummary extends SvnOperation<SvnStatusSummary> {

    private String trailUrl;
    private boolean isCommitted;

    protected SvnGetStatusSummary(SvnOperationFactory factory) {
        super(factory);
    }

    /**
     * Returns whether to retrieve last committed revisions or current revisions.
     * 
     * @return <code>true</code> if last committed revisions should be retrieved, otherwise <code>false</code>
     */
    public boolean isCommitted() {
        return isCommitted;
    }

    /**
     * Sets whether to retrieve last committed revisions or current revisions.
     * 
     * @param isCommitted <code>true</code> if last committed revisions should be retrieved, otherwise <code>false</code>
     */
    public void setCommitted(boolean isCommitted) {
        this.isCommitted = isCommitted;
    }

    /**
     * Returns URL for checking whether <code>target</code> was switched by comparing with <code>target</code>'s URL. 
     * Used for 1.6 working copies only.
     * 
     * @return trail URL
     */
    public String getTrailUrl() {
        return trailUrl;
    }

    /**
     * Sets URL for checking whether <code>target</code> was switched by comparing with <code>target</code>'s URL. 
     * Used for 1.6 working copies only.
     * 
     * @param trailUrl trail URL
     */
    public void setTrailUrl(String trailUrl) {
        this.trailUrl = trailUrl;
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
