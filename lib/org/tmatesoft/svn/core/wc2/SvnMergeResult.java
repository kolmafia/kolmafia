package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.wc.SVNStatusType;

/**
 * Represents a result of a text or properties merge operation. 
 * This class combines the following information about a merge result: a status type indicating how merge 
 * finished; base and actual (working) properties.    
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnMergeResult {
    
    private final SVNStatusType mergeOutcome;
    private SVNProperties actualProperties;
    private SVNProperties baseProperties;

    /**
     * Creates merge result object and initializes it with merge outcome (status).
     *  
     * @param mergeOutcome status of merge
     * @return newly created merge result object
     */
    public static SvnMergeResult create(SVNStatusType mergeOutcome) {
        return new SvnMergeResult(mergeOutcome);
    }
    
    private SvnMergeResult(SVNStatusType mergeOutcome) {
        this.mergeOutcome = mergeOutcome;
    }
    
    /**
     * Returns merge outcome (status).
     * 
     * @return merge outcome (status) 
     */
    public SVNStatusType getMergeOutcome() {
        return mergeOutcome;
    }

    /**
     * Returns all merge actual (working) properties.
     * 
     * @return actual properties
     */
    public SVNProperties getActualProperties() {
        if (actualProperties == null) {
            actualProperties = new SVNProperties();
        }
        return actualProperties;
    }

    /**
     * Sets all merge actual (working) properties.
     * 
     * @param actualProperties actual properties
     */
    public void setActualProperties(SVNProperties actualProperties) {
        this.actualProperties = actualProperties;
    }

    /**
     * Returns all base (pristine) properties.
     * 
     * @return base properties
     */
    public SVNProperties getBaseProperties() {
        if (baseProperties == null) {
            baseProperties = new SVNProperties();
        }
        return baseProperties;
    }

    /**
     * Sets all base (pristine) properties.
     * 
     * @param baseProperties base properties
     */
    public void setBaseProperties(SVNProperties baseProperties) {
        this.baseProperties = baseProperties;
    }
}
