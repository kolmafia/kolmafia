package org.tmatesoft.svn.core.wc2;

/**
 * Represents status summary information for local working copy item,
 * including all its children.
 * is used in {@link SvnGetStatusSummary}.
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 * @since 1.7
 * @see SvnGetStatusSummary
 */
public class SvnStatusSummary extends SvnObject {

    private long minRevision;
    private long maxRevision;
    private boolean isModified;
    private boolean isSparseCheckout;
    private boolean isSwitched;
    
    /**
     * Returns the smallest revision between all found items.
     * 
     * @return the smallest revision
     */
    public long getMinRevision() {
        return minRevision;
    }
    
    /**
     * Returns the biggest item's revision between all found items.
     * 
     * @return the biggest revision
     */
    public long getMaxRevision() {
        return maxRevision;
    }
    
    /**
     * Returns whether at least one of the items is changed in working copy.
     * 
     * @return <code>true</code> if one of the items is changed, otherwise <code>false</code>
     */
    public boolean isModified() {
        return isModified;
    }
    
    /**
     * Gets whether the items are result of "sparse" checkout.
     * 
     * @return <code>true</code> if items are result of "sparse" checkout, otherwise <code>false</code>
     * @since 1.7
     */
    public boolean isSparseCheckout() {
        return isSparseCheckout;
    }
    
    /**
     * Returns whether at least one of the items is switched to a different repository location.
     * 
     * @return <code>true</code> if one of the items is switched to a different repository location, otherwise <code>false</code>
     */
    public boolean isSwitched() {
        return isSwitched;
    }
    
    /**
     * Sets the smallest revision between all found items.
     * 
     * @param minRevision the smallest revision
     */
    public void setMinRevision(long minRevision) {
        this.minRevision = minRevision;
    }
    
    /**
     * Sets the biggest item's revision between all found items.
     * 
     * @param maxRevision the biggest revision
     */
    public void setMaxRevision(long maxRevision) {
        this.maxRevision = maxRevision;
    }
    
    /**
     * Sets whether at least one of the items is changed in working copy.
     * 
     * @param isModified <code>true</code> if one of the items is changed, otherwise <code>false</code>
     */
    public void setModified(boolean isModified) {
        this.isModified = isModified;
    }
    
    /**
     * Sets whether the items are result of "sparse" checkout.
     * 
     * @param isSparseCheckout <code>true</code> if items are result of "sparse" checkout, otherwise <code>false</code>
     * @since 1.7
     */
    public void setSparseCheckout(boolean isSparseCheckout) {
        this.isSparseCheckout = isSparseCheckout;
    }
    
    /**
     * Sets whether at least one of the items is switched to a different repository location.
     * 
     * @param isSwitched <code>true</code> if one of the items is switched to a different repository location, otherwise <code>false</code>
     */
    public void setSwitched(boolean isSwitched) {
        this.isSwitched = isSwitched;
    }
    
    /**
     * Returns <code>String</code> representation of summary status.
     * @return summary status as <code>String</code>
     */
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append(getMinRevision());
        if (getMaxRevision() != getMinRevision()) {
            result.append(":");
            result.append(getMaxRevision());
        }
        
        result.append(isModified() ? "M" : "");
        result.append(isSwitched() ? "S" : "");
        result.append(isSparseCheckout() ? "P" : "");
        
        return result.toString();
    }
}
