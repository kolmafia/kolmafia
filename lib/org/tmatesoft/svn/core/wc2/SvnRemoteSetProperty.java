package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.ISVNCommitHandler;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
import org.tmatesoft.svn.core.wc.SVNRevision;

/**
 * Sets <code>propertyName</code> to <code>propertyValue</code> on each <code>targets</code>. 
 * If <code>propertyValue</code> is <code>null</code>, operation will delete the property.
 * Targets should represent URLs.
 * 
 * <p/>
 * <code>baseRevision</code> must not be null; in this case, the property
 * will only be set if it has not changed since <code>baseRevision</code>.
 * 
 * <p/>
 * The {@link ISVNAuthenticationManager authentication manager} and
 * {@link ISVNCommitHandler commit handler}, either provided by a caller or
 * default ones, will be used to immediately attempt to commit the property
 * change in the repository.
 * 
 * {@link #run()} returns {@link SVNCommitInfo} commit information if the commit succeeds.
 * This method throws SVNException if the following is true:
 *             <ul>
 *             <li><code>url</code> does not exist in <code>baseRevision
 *             </code> <li>exception with
 *             {@link SVNErrorCode#CLIENT_PROPERTY_NAME} error code - if
 *             <code>propertyName</code> is a revision property name or not a
 *             valid property name or not a regular property name (one
 *             starting with an <span class="javastring">"svn:entry"</span>
 *             or <code>"svn:wc"</code> prefix) <li>
 *             exception with {@link SVNErrorCode#UNSUPPORTED_FEATURE} error
 *             code - if <code>propertyName</code> is either equal to
 *             {@link SVNProperty#EOL_STYLE} or {@link SVNProperty#KEYWORDS}
 *             or {@link SVNProperty#CHARSET}
 *             </ul>
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnRemoteSetProperty extends AbstractSvnCommit {

    private boolean force;
    private String propertyName;
    private SVNPropertyValue propertyValue;
    private SVNRevision baseRevision;
    private ISvnObjectReceiver<SVNPropertyData> propertyReceiver;
    private SVNPropertyValue originalPropertyValue;

    protected SvnRemoteSetProperty(SvnOperationFactory factory) {
        super(factory);
    }

    /**
     * Returns whether to skip validity checking of <code>propertyName</code> and <code>propertyValue</code>.
     *  
     * @return force <code>true</code> if validity checking should be skipped, otherwise <code>false</code>
     * @see #setForce(boolean)
     */
    public boolean isForce() {
        return force;
    }

    /**
    * Sets whether to skip validity checking of <code>propertyName</code> and <code>propertyValue</code>. 
    * If <code>force</code> is <code>true</code>, this
    * operation does no validity checking. But if <code>force</code> is <code>false</code>, 
    * and <code>propertyName</code> is not a
    * valid property for <code>targets</code>, it throws an exception, either with
    * an error code {@link org.tmatesoft.svn.core.SVNErrorCode#ILLEGAL_TARGET}
    * (if the property is not appropriate for target), or with
    * {@link org.tmatesoft.svn.core.SVNErrorCode#BAD_MIME_TYPE} (if
    * <code>propertyName</code> is <code>"svn:mime-type"</code>,
    * but <code>propertyValue</code> is not a valid mime-type).
    * 
    * @param force <code>true</code> if validity checking should be skipped, otherwise <code>false</code>
    */
    public void setForce(boolean force) {
        this.force = force;
    }
    
    /**
     * Gets name of the property. 
     * 
     * @return name of the property
     * @see #setPropertyName(String)
     */
    public String getPropertyName() {
        return propertyName;
    }
    
    /**
     * Sets name of the property. 
     * If <code>propertyName</code> is an svn-controlled property (i.e. prefixed
     * with <span class="javastring">"svn:"</span>), then the caller is
     * responsible for ensuring that the value uses LF line-endings.
     * 
     */
    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }
    
    /**
     * Returns the property's value. 
     * If <code>propertyValue</code> is <code>null</code>, operation will delete the property.
     * 
     * @return value of the property
     */
    public SVNPropertyValue getPropertyValue() {
        return propertyValue;
    }
    
    /**
     * Sets the property's value. 
     * If <code>propertyValue</code> is <code>null</code>, operation will delete the property.
     * 
     * @param propertyValue value of the property
     */
    public void setPropertyValue(SVNPropertyValue propertyValue) {
        this.propertyValue = propertyValue;
    }

    /**
     * Returns operation's revision to change properties against
     * 
     * @return base revision of the operation
     */
    public SVNRevision getBaseRevision() {
        return baseRevision;
    }
    
    /**
     * Sets operation's revision to change properties against
     * 
     * @param baseRevision base revision of the operation
     */
    public void setBaseRevision(SVNRevision baseRevision) {
        this.baseRevision = baseRevision;
    }

    /**
     * Returns operation's property receiver.
     * 
     * @return property receiver of the operation
     */
    public ISvnObjectReceiver<SVNPropertyData> getPropertyReceiver() {
        return propertyReceiver;
    }

    /**
     * Sets operation's property receiver.
     * 
     * @param propertyReceiver property receiver of the operation
     */
    public void setPropertyReceiver(ISvnObjectReceiver<SVNPropertyData> propertyReceiver) {
        this.propertyReceiver = propertyReceiver;
    }

    /**
     * Returns property's original value, it was set by caller
     * 
     * @return original value of the property
     */
    public SVNPropertyValue getOriginalPropertyValue() {
        return originalPropertyValue;
    }

    /**
     * Sets property's original value to hold this information for the caller
     * 
     * @param originalPropertyValue original value of the property
     */
    public void setOriginalPropertyValue(SVNPropertyValue originalPropertyValue) {
        this.originalPropertyValue = originalPropertyValue;
    }

    @Override
    protected void ensureArgumentsAreValid() throws SVNException {
        super.ensureArgumentsAreValid();
        if (getBaseRevision() == null) {
            setBaseRevision(SVNRevision.HEAD);
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
