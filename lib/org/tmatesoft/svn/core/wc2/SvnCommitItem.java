package org.tmatesoft.svn.core.wc2;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;

/**
 * Provides information about a committed 
 * revision. Commit information includes:
 * <ol>
 * <li>a path;
 * <li>a node kind;
 * <li>URL;
 * <li>a revision number;
 * <li>copy from URL;
 * <li>copy from revision number;
 * <li>flags;
 * <li>outgoing properties.
 * </ol>
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnCommitItem {
    
    public static final int ADD = 0x01;
    public static final int DELETE = 0x02;
    public static final int TEXT_MODIFIED = 0x04;
    public static final int PROPS_MODIFIED = 0x08;
    public static final int COPY = 0x10;
    public static final int LOCK = 0x20;
    
    private File path;
    private SVNNodeKind kind;
    private SVNURL url;
    private long revision;
    
    private SVNURL copyFromUrl;
    private long copyFromRevision;
    
    private int flags;
    private Map<String, SVNPropertyValue> outgoingProperties;
    private Map<String, SVNPropertyValue> incomingProperties;
    
    /**
     * Returns commit item's working copy path.
     * 
     * @return working copy path of the commit item
     */
    public File getPath() {
        return path;
    }
    
    /**
     * Returns commit item's node kind.
     * 
     * @return node kind of the commit item
     */
    public SVNNodeKind getKind() {
        return kind;
    }
    
    /**
     * Returns commit item's repository URL.
     * 
     * @return URL of the source copy item
     */
    public SVNURL getUrl() {
        return url;
    }
    
    /**
     * Returns the revision number the repository was committed to.
     * 
     * @return revision number of the commit item
     */
    public long getRevision() {
        return revision;
    }
    
    /**
     * Returns URL from whose the item was copied.
     * 
     * @return copy item URL
     */
    public SVNURL getCopyFromUrl() {
        return copyFromUrl;
    }
    
    /**
     * Returns revision number of the repository item from whose working copy item was copied.
     * 
     * @return revision number of the source copy item
     */
    public long getCopyFromRevision() {
        return copyFromRevision;
    }
    
    /**
     * Returns commit item's flags.
     * 
     * @return the flags of the commit item
     * @see #setFlags(int)
     */
    public int getFlags() {
        return flags;
    }
    
    /**
     * Sets commit item's working copy path.
     * 
     * @param path working copy path of the commit item
     */
    public void setPath(File path) {
        this.path = path;
    }
    
    /**
     * Sets commit item's node kind.
     * 
     * @param kind node kind of the commit item
     */
    public void setKind(SVNNodeKind kind) {
        this.kind = kind;
    }
    
    /**
     * Sets commit item's repository URL.
     * 
     * @param url repository URL of the commit item
     */
    public void setUrl(SVNURL url) {
        this.url = url;
    }
    
    /**
     * Sets the revision number the repository was committed to.
     * 
     * @param revision revision number of the commit item
     */
    public void setRevision(long revision) {
        this.revision = revision;
    }
    
    /**
     * Sets URL from whose the item was copied.
     * 
     * @param copyFromUrl URL of the source copy item
     */
    public void setCopyFromUrl(SVNURL copyFromUrl) {
        this.copyFromUrl = copyFromUrl;
    }
    
    /**
     * Sets revision number of the repository item from whose working copy item was copied.
     * 
     * @param copyFromRevision revision number of the source copy item
     */
    public void setCopyFromRevision(long copyFromRevision) {
        this.copyFromRevision = copyFromRevision;
    }
    
    /**
     * Sets commit item's flags.
     * They can be the following value(s):
     * <ul>
     * <li>{@link #ADD}</li>
     * <li>{@link #DELETE}</li>
     * <li>{@link #TEXT_MODIFIED}</li>
     * <li>{@link #PROPS_MODIFIED}</li>
     * <li>{@link #COPY}</li>
     * <li>{@link #LOCK}</li>
     * </ul>
     * 
     * @param commitFlags the flags of the commit item
     */
    public void setFlags(int commitFlags) {
        this.flags = commitFlags;
    }
    
    /**
     * Checks whether commit item has the flag
     * @param flag the value of the flag
     * @return <code>true</code> if commit item flags contain the requested value, otherwise <code>false</code>
     */
    public boolean hasFlag(int flag) {
        return (getFlags() & flag) != 0;
    }
    
    /**
     * Returns all properties that should be committed within the item.
     * 
     * @return properties of the commit item 
     */
    public Map<String, SVNPropertyValue> getOutgoingProperties() {
        return outgoingProperties;
    }

    /**
     * Adds property with the name and the value that should be committed within the item.
     * 
     * @param name of the property
     * @param value of the property
     */
    public void addOutgoingProperty(String name, SVNPropertyValue value) {
        if (outgoingProperties == null) {
            outgoingProperties = new HashMap<String, SVNPropertyValue>();
        }
        if (name != null) {
            if (value != null) {
                outgoingProperties.put(name, value);
            } else {
                outgoingProperties.remove(name);
            }
        }
    }

    public Map<String, SVNPropertyValue> getIncomingProperties() {
        return incomingProperties;
    }

    public void addIncomingProperty(String name, SVNPropertyValue value) {
        if (incomingProperties == null) {
            incomingProperties = new HashMap<String, SVNPropertyValue>();
        }
        if (name != null) {
            incomingProperties.put(name, value);
        }
    }
}
