package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;


/**
 * Represents proplist operation.
 * Gets the <code>target</code>'s properties or revision properties.
 * 
 * If single property has to be read, a caller should use
 * {@link ISvnObjectReceiver} to fetch the property value by name. 
 * 
 * {@link #run()} returns {@link SvnWcGeneration} of resulting working copy.
 * This method throws {@link SVNException} if one of the following is true:
 *             <ul>
 *             <li><code>propertyName</code> starts with the
 *             {@link org.tmatesoft.svn.core.SVNProperty#SVN_WC_PREFIX svn:wc:} prefix
 *             <li><code>target</code> is not under version control
 *             </ul>
 * 
 * @author TMate Software Ltd.  
 * @version 1.7   
 */
public class SvnGetProperties extends SvnReceivingOperation<SVNProperties> {

    private boolean revisionProperties;
    private long revisionNumber;

    protected SvnGetProperties(SvnOperationFactory factory) {
        super(factory);
    }

    @Override
    protected void ensureArgumentsAreValid() throws SVNException {
        if (getDepth() == SVNDepth.UNKNOWN) {
            setDepth(SVNDepth.EMPTY);
        }
        if (getRevision() == null || !getRevision().isValid()) {
            if (getFirstTarget() != null) {
                setRevision(getFirstTarget().getResolvedPegRevision());
            }
        }
        super.ensureArgumentsAreValid();
    }

    /**
     * Gets whether it is revision properties.
     * 
     * @return <code>true</code> if it is revision properties, <code>true</code> if it is <code>target</code>'s properties
     */
    public boolean isRevisionProperties() {
        return revisionProperties;
    }

    /**
     * Sets whether it is revision properties.
     * 
     * @param revisionProperties <code>true</code> if it is revision properties, <code>true</code> if it is <code>target</code>'s properties
     */
    public void setRevisionProperties(boolean revisionProperties) {
        this.revisionProperties = revisionProperties;
    }

    /**
     * Sets properties revision number, only for revision properties.
     * 
     * @return revision number of properties
     */
    public long getRevisionNumber() {
        return revisionNumber;
    }
    
    /**
     * Sets properties revision number, only for revision properties.
     * 
     * @param  revisionNumber revision number of properties
     */
    public void setRevisionNumber(long revisionNumber) {
        this.revisionNumber = revisionNumber;
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
