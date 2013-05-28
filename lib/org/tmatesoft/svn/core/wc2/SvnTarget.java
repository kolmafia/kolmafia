package org.tmatesoft.svn.core.wc2;

import java.io.File;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc16.SVNUpdateClient16;
import org.tmatesoft.svn.core.wc.SVNRevision;

/**
 * Represents target of the operation on whose operation will be executed.
 * Can specify working copy path or repository URL.
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnTarget {
    
    private SVNURL url;
    private File file;
    private SVNRevision pegRevision;
    
    /**
     * Creates a target from file
     * @param file target file
     * @return a new <code>SvnTarget</code> object representing the file
     */
    public static SvnTarget fromFile(File file) {
        return fromFile(file, SVNRevision.UNDEFINED);
    }

    /**
     * Creates a target from file and its peg revision
     * @param file target file
     * @param pegRevision revision in which the file item is first looked up
     * @return a new <code>SvnTarget</code> object representing the file with revisition 
     */
    public static SvnTarget fromFile(File file, SVNRevision pegRevision) {
        return new SvnTarget(file, pegRevision);
    }

    /**
     * Creates a target from URL
     * @param url target URL
     * @return a new <code>SvnTarget</code> object representing URL
     */
    public static SvnTarget fromURL(SVNURL url) {
        return fromURL(url, SVNRevision.UNDEFINED);
    }

    /**
     * Creates a target from URL and its peg revision
     * @param url target URL
     * @param pegRevision revision in which the file item is first looked up
     * @return a new <code>SvnTarget</code> object representing URL with revisition 
     */
    public static SvnTarget fromURL(SVNURL url, SVNRevision pegRevision) {
        return new SvnTarget(url, pegRevision);
    }
    
    private SvnTarget(File file, SVNRevision pegRevision) {
        this.file = new File(SVNPathUtil.validateFilePath(file.getAbsolutePath()));
        setPegRevision(pegRevision);
    }
    
    private SvnTarget(SVNURL url, SVNRevision pegRevision) {
        this.url = getCanonicalUrl(url);
        setPegRevision(pegRevision);
    }
    
    private SVNURL getCanonicalUrl(SVNURL url) {
        if (url == null) {
            return null;
        }
        SVNURL canonicalUrl = null;
        try {
            canonicalUrl = SVNUpdateClient16.canonicalizeURL(url, true);
        } catch (SVNException e) {
        }        
        return canonicalUrl != null ? canonicalUrl : url;
    }

    /**
     * Determines whether target is located in the local working copy and its peg revision is working copy specific.
     * @return <code>true</code> if the target and its peg revision refers to local working copy, otherwise <code>false</code>
     */
    public boolean isLocal() {
        return isFile() && getResolvedPegRevision().isLocal();
    }
    
    /**
     * Determines whether target represents file
     * @return <code>true</code> if the target is file, otherwise <code>false</code>
     */
    public boolean isFile() {
        return this.file != null;
    }
    
    /**
     * Determines whether target represents URL
     * @return <code>true</code> if the target is URL, otherwise <code>false</code>
     */
    public boolean isURL() {
        return this.url != null;
    }
    
    /**
     * Returns target's URL, if target is not URL returns null. 
     * @return url of the target
     */
    public SVNURL getURL() {
        return this.url;
    }
    
    /**
     * Returns target's file, if target is not file returns null. 
     * @return url of the target
     */
    public File getFile() {
        return this.file;
    }
    
    /**
     * Returns target's peg revision, if it was not defined returns null. 
     * @return peg revision of the target
     */
    public SVNRevision getPegRevision() {
        return this.pegRevision;
    }

    /**
     * Calls <code>getResolvedPegRevision</code> with {@link org.tmatesoft.svn.core.wc.SVNRevision#HEAD}, 
     * {@link org.tmatesoft.svn.core.wc.SVNRevision#WORKING} as default values
     * @return peg revision of the target
     * @see #getResolvedPegRevision(SVNRevision, SVNRevision)
     */
    public SVNRevision getResolvedPegRevision() {
        return getResolvedPegRevision(SVNRevision.HEAD, SVNRevision.WORKING);
    }
    
    /**
     * Returns target's peg revision if defined, if not defined determines whether target is remote or local, 
     * and returns corresponding default revision
     * @param defaultRemote default revision if target is remote target
     * @param defaultLocal default revision if target is local target
     * @return peg revision of the target
     */
    public SVNRevision getResolvedPegRevision(SVNRevision defaultRemote, SVNRevision defaultLocal) {
        if (getPegRevision() == null || getPegRevision() == SVNRevision.UNDEFINED) {
            if (defaultLocal == null) {
                defaultLocal = SVNRevision.WORKING;
            }
            if (defaultRemote == null) {
                defaultRemote = SVNRevision.HEAD;
            }
            return isURL() ? defaultRemote : defaultLocal;
        }
        
        return getPegRevision();
    }
    
    private void setPegRevision(SVNRevision revision) {
        if (revision == null) {
            revision = SVNRevision.UNDEFINED;
        }
        this.pegRevision = revision;
    }

    /**
     * Determines whether target is remote or local, and returns corresponding <code>String</code> representation of the target's path
     * @return <code>String</code> representation of the target's path
     * @throws {@link IllegalStateException} if neither file not URL was specified as a target  
     */
    public String getPathOrUrlString() {
        if (isFile()) {
            return getFile().getPath();
        } else if (isURL()) {
            return getURL().toString();
        }

        throw new IllegalStateException("A target can be either an URL or a path");
    }
    
    /**
     * Determines whether target is remote or local, and returns corresponding <code>String</code> representation of the target's path
     * @return <code>String</code> representation of the target
     * @throws {@link IllegalStateException} if neither file not URL was specified as a target's path  
     */
    public String getPathOrUrlDecodedString() {
        if (isFile()) {
            return getFile().getPath();
        } else if (isURL()) {
            return getURL().toString();
        }

        throw new IllegalStateException("A target can be either an URL or a path");
    }

    /**
     * Determines whether target is remote or local, and returns corresponding <code>String</code> 
     * representation of the target's path and peg revision.
     *
     * @return <code>String</code> of the target's path and peg revision
     */
    public String toString() {
        if (isFile()) {
            return getFile().getAbsolutePath() + '@' + getPegRevision();
        } else if (isURL()) {
            return getURL().toString() + '@' + getPegRevision();
        }
        return "INVALID TARGET";
    }
}
