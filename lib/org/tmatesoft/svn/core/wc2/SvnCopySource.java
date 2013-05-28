package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.wc.SVNRevision;

/**
 * Provides copy source information in copy operations.
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 * @see SvnCopy
 */
public class SvnCopySource extends SvnObject {
    
    private SvnTarget source;
    private SVNRevision revision;
    private boolean copyContents;
    
    /**
     * Creates a new <code>SvnCopySource</code> object and initializes its fields. 
     * 
     * @param source      source target with optional <code>pegRevision</code>, can be file or URL
     * @param revision    revision of <code>target</code>
     */
    public static SvnCopySource create(SvnTarget source, SVNRevision revision) {
        return new SvnCopySource(source, revision);
    }
    
    private SvnCopySource(SvnTarget source, SVNRevision revision) {
        setSource(source);
        if (revision == null || !revision.isValid()) {
            revision = source.getResolvedPegRevision();
        }
        setRevision(revision);
    }
    
    /**
     * Calculates whether <code>source</code> is local and <code>revision</code> is local.
     * 
     * @return <code>true</code> if the <code>source</code> and <code>revision</code> are local, otherwise <code>false</code>
     */
    public boolean isLocal() {
        return getSource().isLocal() && getRevision().isLocal();
    }
    
    /**
     * Returns the copy source target, can be working copy file or URL with optional <code>pegRevision</code>.
     * 
     * @return copy source target
     */
    public SvnTarget getSource() {
        return source;
    }

    /**
     * Returns the revision of the source. 
     * 
     * @return source revision
     */
    public SVNRevision getRevision() {
        return revision;
    }

    /**
     * Sets the copy source target, can be working copy file or URL with optional <code>pegRevision</code>.
     * 
     * @param source copy source target
     */
    private void setSource(SvnTarget source) {
        this.source = source;
    }
    
    /**
     * Sets the revision of the source.
     * 
     * @param revision source revision
     */
    private void setRevision(SVNRevision revision) {
        this.revision = revision;
    }

    /**
     * Tells whether the contents of this copy source should be copied rather than the copy source itself.
     * This is relevant only for directory copy sources. If a user {@link #setCopyContents(boolean) specifies} 
     * to copy contents of a file he will get an {@link org.tmatesoft.svn.core.SVNException}. So, if this copy source represents a 
     * directory and if this method returns <code>true</code>, children of this copy source 
     * directory will be copied to the target instead of the copy source.    
     * 
     * @return   <code>true</code> to expand copy source to children; otherwise <code>false</code>  
     */
    public boolean isCopyContents() {
        return copyContents;
    }

    /**
     * Sets whether to expand this copy source to its contents or not. 
     * 
     * @param copyContents    <code>true</code> to expand copy source to children; otherwise <code>false</code>  
     * @see                  #isCopyContents()
     */
    public void setCopyContents(boolean copyContents) {
        this.copyContents = copyContents;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((revision == null) ? 0 : revision.hashCode());
        result = prime * result + ((source == null) ? 0 : source.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SvnCopySource other = (SvnCopySource) obj;
        if (revision == null) {
            if (other.revision != null) {
                return false;
            }
        } else if (!revision.equals(other.revision)) {
            return false;
        }
        if (source == null) {
            if (other.source != null) {
                return false;
            }
        } else if (!source.equals(other.source)) {
            return false;
        }
        return true;
    }

    /**
     * Returns <code>String</code> representation of the object
     * 
     * @return object as <code>String</code>
     */
    @Override
    public String toString() {
        return getSource().toString() + " r" + getRevision();
    }
}
