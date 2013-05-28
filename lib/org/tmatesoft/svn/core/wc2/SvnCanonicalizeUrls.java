package org.tmatesoft.svn.core.wc2;

/**
 * Canonicalizes all urls in the working copy <code>target</code>.
 *  
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnCanonicalizeUrls extends SvnOperation<Void> {

    private boolean omitDefaultPort;
    private boolean ignoreExternals;
    
    protected SvnCanonicalizeUrls(SvnOperationFactory factory) {
        super(factory);
    }

    /**
     * Returns whether to remove all port numbers from URLs which equal to default ones.
     *            
     * @return <code>true</code> if the default port numbers should be removed, otherwise <code>false</code>
     */
    public boolean isOmitDefaultPort() {
        return omitDefaultPort;
    }

    /**
     * Sets whether to remove all port numbers from URLs which equal to default ones.
     *            
     * @param omitDefaultPort <code>true</code> if the default port numbers should be removed, otherwise <code>false</code>
     */
    public void setOmitDefaultPort(boolean omitDefaultPort) {
        this.omitDefaultPort = omitDefaultPort;
    }

    /**
     * Returns whether to ignore externals definitions.
     * 
     * @return <code>true</code> if externals definitions should be ignored, otherwise <code>false</code>
     * @since 1.7
     */
    public boolean isIgnoreExternals() {
        return ignoreExternals;
    }
    
    /**
     * Sets whether to ignore externals definitions.
     * 
     * @param ignoreExternals <code>true</code> if externals definitions should be ignored, otherwise <code>false</code>
     * @since 1.7
     */
    public void setIgnoreExternals(boolean ignoreExternals) {
        this.ignoreExternals = ignoreExternals;
    }

    @Override
    protected void initDefaults() {
        super.initDefaults();
        setOmitDefaultPort(true);
    }

}
