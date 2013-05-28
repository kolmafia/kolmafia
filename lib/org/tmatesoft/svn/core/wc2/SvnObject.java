package org.tmatesoft.svn.core.wc2;

/**
 * Base class for all Svn* classes representing some kind of information.
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 * @see SvnCopySource
 * @see SvnDiffStatus
 * @see SvnInfo
 * @see SvnStatus
 * @see SvnStatusSummary
 * @see SvnRevisionRange
 * 
 */
public abstract class SvnObject {
    
    private Object userData;

    /**
     * Returns user data.
     * 
     * @return user data
     */
    public Object getUserData() {
        return userData;
    }

    /**
     * Sets user data.
     * 
     * @param userData user data
     */
    public void setUserData(Object userData) {
        this.userData = userData;
    }
    
    
}
