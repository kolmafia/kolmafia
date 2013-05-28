package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNURL;

/**
 * Represents relocate operation.
 * Substitutes the beginning part of a working copy <code>target</code>'s URL with a new one.
 * 
 * <p>
 * When a repository root location or a URL schema is changed the <code>fromUrl</code> of
 * the working copy which starts with <code>fromUrl</code> should be
 * substituted for a new URL beginning - <code>toUrl</code>, or full <code>target</code>'s URL
 * should be substituted with <code>toUrl</code>.
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnRelocate extends SvnOperation<SVNURL> {
    
    private SVNURL fromUrl;
    private SVNURL toUrl;
    private boolean ignoreExternals;
    private boolean recursive;

    protected SvnRelocate(SvnOperationFactory factory) {
        super(factory);
    }

    /**
     * Returns the old beginning part of the <code>target</code>'s repository URL that should be overwritten.
     * Optional parameter, if <code>null</code> full <code>target</code>'s repository URL should be overwritten.
     * 
     * @return old beginning part of the repository URL of the <code>target</code>
     */
    public SVNURL getFromUrl() {
        return fromUrl;
    }
    
    /**
    * Sets the old beginning part of the <code>target</code>'s repository URL that should be overwritten.
    * Optional parameter, if <code>null</code> full <code>target</code>'s repository URL should be overwritten.
    * 
    * @param fromUrl old beginning part of the repository URL of the <code>target</code>
    */
   public void setFromUrl(SVNURL fromUrl) {
       this.fromUrl = fromUrl;
   }

    /**
     * Gets the new beginning part for the repository location or full repository location 
     * that will overwrite <code>target</code>'s repository URL.
     * If <code>fromUrl</code> is <code>null</code> full URL should be overwritten,
     * otherwise only beginning part should be overwritten.
     * 
     * @return new repository path or part of repository path of the <code>target</code>
     */
    public SVNURL getToUrl() {
        return toUrl;
    }
    
    /**
     * Sets the new beginning part for the repository location or full repository location 
     * that will overwrite <code>target</code>'s repository URL.
     * If <code>fromUrl</code> is <code>null</code> full URL should be overwritten,
     * otherwise only beginning part should be overwritten.
     * 
     * @param toUrl new repository path or part of repository path of the <code>target</code>
     */
    public void setToUrl(SVNURL toUrl) {
        this.toUrl = toUrl;
    }

    /**
     * Returns whether to ignore externals definitions.
     * 
     * @return <code>true</code> if externals definitions should be ignored, otherwise <code>false</code>
     */
    public boolean isIgnoreExternals() {
        return ignoreExternals;
    }

    /**
     * Sets whether to ignore externals definitions.
     * 
     * @param ignoreExternals <code>true</code> if externals definitions should be ignored, otherwise <code>false</code>
     */
    public void setIgnoreExternals(boolean ignoreExternals) {
        this.ignoreExternals = ignoreExternals;
    }

    /**
     * Sets whether to relocate entire tree of <code>toUrl</code> if it is a directory.
     * Only relevant for 1.6 working copies.
     * 
     * @param recursive <code>true</code> if the entire tree of <code>toUrl</code> directory should be relocated, otherwise <code>false</code>
     */
    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }
    
    /**
     * Returns whether to relocate entire tree of <code>target</code> if it is a directory.
     * Only relevant for 1.6 working copies.
     * 
     * @return <code>true</code> if the entire tree of <code>target</code> directory should be relocated, otherwise <code>false</code>
     */
    public boolean isRecursive() {
        return this.recursive;
    }

    @Override
    protected void initDefaults() {
        super.initDefaults();
        setRecursive(true);
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
