package org.tmatesoft.svn.core.wc2;

import java.io.File;

import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNStatusType;

/**
 * Represents short information on path changes in {@link SvnDiffSummarize} operation. 
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 * @see SvnDiffSummarize
 */
public class SvnDiffStatus extends SvnObject {

    private SVNStatusType modificationType;
    private boolean propertiesModified;
    private SVNNodeKind kind;
    private SVNURL url;
    private String path;
    private File file;
    
    /**
     * Returns the type of modification for the current 
     * item. 
     * 
     * @return a path change type
     */
    public SVNStatusType getModificationType() {
        return modificationType;
    }
    
    /**
     * Sets the type of modification for the current 
     * item. 
     * 
     * @param modificationType a path change type
     */
    public void setModificationType(SVNStatusType modificationType) {
        this.modificationType = modificationType;
    }
    
    /**
     * Returns whether properties of the working copy item are modified. 
     *  
     * @return <code>true</code> if properties were modified in a particular revision, otherwise <code>false</code> 
     */
    public boolean isPropertiesModified() {
        return propertiesModified;
    }
    
    /**
     * Sets whether properties of the working copy item are modified. 
     *  
     * @param propertiesModified <code>true</code> if properties were modified in a particular revision, otherwise <code>false</code> 
     */
    public void setPropertiesModified(boolean propertiesModified) {
        this.propertiesModified = propertiesModified;
    }
    
    /**
     * Returns the node kind of the working copy item. 
     * 
     * @return node kind
     */
    public SVNNodeKind getKind() {
        return kind;
    }
    
    /**
     * Sets the node kind of the working copy item. 
     * 
     * @param kind node kind
     */
    public void setKind(SVNNodeKind kind) {
        this.kind = kind;
    }
    
    /**
     * Returns URL of the item.
     * 
     * @return item url
     */
    public SVNURL getUrl() {
        return url;
    }
    
    /**
     * Sets URL of the item.
     * 
     * @param url item url
     */
    public void setUrl(SVNURL url) {
        this.url = url;
    }
    
    /**
     * Returns a relative path of the item. 
     * Relative path is set for working copy items and relative to the anchor of diff status operation.
     * 
     * @return item path
     */
    public String getPath() {
        return path;
    }
    
    
    /**
     * Sets a relative path of the item. 
     * Relative path should be set for working copy items and relative to the anchor of diff status operation.
     * 
     * @param path item path
     */
    public void setPath(String path) {
        this.path = path;
    }
    
    /**
     * Returns <code>File</code> representation of the working copy item path. 
     * 
     * @return working copy item path as <code>File</code> 
     */
    public File getFile() {
        return file;
    }
    
    /**
     * Sets <code>File</code> representation of the working copy item path. 
     * 
     * @param file working copy item path as <code>File</code> 
     */
    public void setFile(File file) {
        this.file = file;
    }
    
    

}
