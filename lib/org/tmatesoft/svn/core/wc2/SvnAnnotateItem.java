package org.tmatesoft.svn.core.wc2;

import java.io.File;
import java.util.Date;

import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNRevisionProperty;
import org.tmatesoft.svn.core.internal.util.SVNDate;

/**
 * Provides information for annotate item in {@link SvnAnnotate} operation.
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnAnnotateItem {

    private long revision;
    private SVNProperties revisionProperties;
    private SVNProperties mergedRevisionProperties;
    private String line;
    private long mergedRevision;
    private String mergedPath;
    private int lineNumber;
    private File contents;
    private boolean isEof;
    private boolean isRevision;
    private boolean isLine;
    private boolean returnResult;

    public SvnAnnotateItem(boolean isEof) {
        this.isEof = true;
    }

    /**
    * Constructs and initializes an <b>SvnAnnotateItem</b> object with the
    * specified parameters.
    * 
    * @param date modification date
    * @param revision modification revision
    * @param author author of modification
    * @param line modified line
    * @param mergedDate date of merge
    * @param mergedRevision revision of merge
    * @param mergedAuthor author of merge
    * @param mergedPath path of merge
    * @param lineNumber number of line
    */
    public SvnAnnotateItem(Date date, long revision, String author, String line, Date mergedDate,
                           long mergedRevision, String mergedAuthor, String mergedPath, int lineNumber) {
        this.isLine = true;
        this.revisionProperties = createRevisionProperties(author, date);
        this.revision = revision;
        this.line = line;
        this.mergedRevisionProperties = createRevisionProperties(mergedAuthor, mergedDate);
        this.mergedRevision = mergedRevision;
        this.mergedPath = mergedPath;
        this.lineNumber = lineNumber;
    }

    /**
     * Constructs and initializes an <b>SvnAnnotateItem</b> object with the
     * specified parameters.
     * 
     * @param date modification date
     * @param revision revision of modification
     * @param author author of modification
     * @param contents contents represented in file
     */
    public SvnAnnotateItem(Date date, long revision, String author, File contents) {
        this.isRevision = true;
        this.revisionProperties = createRevisionProperties(author, date);
        this.revision = revision;
        this.contents = contents;
    }

    /**
     * Gets date of modification.
     * 
     * @return date of modification
     */
    public Date getDate() {
        return getDate(getRevisionProperties());
    }

    /**
     * Gets modification revision.
     * 
     * @return modification revision
     */
    public long getRevision() {
        return revision;
    }

    /**
     * Gets the properties of modification revision.
     * 
     * @return revision properties
     */
    public SVNProperties getRevisionProperties() {
        return revisionProperties;
    }

    /**
     * Gets author of modification.
     * 
     * @return modification author
     */
    public String getAuthor() {
        return getAuthor(getRevisionProperties());
    }

    /**
     * Gets date of merge.
     * 
     * @return merge date
     */
    public Date getMergedDate() {
        return getDate(getMergedRevisionProperties());
    }

    /**
     * Gets modified line.
     * 
     * @return modified line
     */
    public String getLine() {
        return line;
    }

    /**
     * Gets revision of merge.
     * 
     * @return merge revision
     */
    public long getMergedRevision() {
        return mergedRevision;
    }

    /**
     * Gets properties of merge revision.
     * 
     * @return merge revision properties
     */
    public SVNProperties getMergedRevisionProperties() {
        return mergedRevisionProperties;
    }

    /**
     * Gets author of merge revision.
     * 
     * @return merge revision author
     */
    public String getMergedAuthor() {
        return getAuthor(getMergedRevisionProperties());
    }

    /**
     * Gets path of merge revision.
     * 
     * @return merge revision path
     */
    public String getMergedPath() {
        return mergedPath;
    }

    /**
     * Gets line number of modification
     * 
     * @return line number of modification
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Gets contents of modification in <code>File</code>
     * 
     * @return modification contents
     */
    public File getContents() {
        return contents;
    }

    /**
     * Gets whether or not end of file reached.
     * 
     * @return <code>true</code> if it is end of file, otherwise <code>false</code>
     */
    public boolean isEof() {
        return isEof;
    }

    /**
     * Gets whether or not item is line.
     * 
     * @return <code>true</code> if item is line, otherwise <code>false</code>
     */
    public boolean isLine() {
        return isLine;
    }

    /**
     * Gets whether or not item is revision.
     * 
     * @return <code>true</code> if item is revision, otherwise <code>false</code>
     */
    public boolean isRevision() {
        return isRevision;
    }

    /**
     * Sets whether or not item was handled.
     * 
     * @param returnResult <code>true</code> if item was handled, otherwise <code>false</code>
     */
    public void setReturnResult(boolean returnResult) {
        this.returnResult = returnResult;
    }

    /**
     * Gets whether or not item was handled.
     * 
     * @return <code>true</code> if item was handled, otherwise <code>false</code>
     */
    public boolean getReturnResult() {
        return returnResult;
    }

    private SVNProperties createRevisionProperties(String author, Date date) {
        if (author == null && date == null) {
            return null;
        }
        SVNProperties properties = new SVNProperties();
        if (author != null) {
            properties.put(SVNRevisionProperty.AUTHOR, author);
        }
        if (date != null) {
            properties.put(SVNRevisionProperty.DATE, SVNDate.fromDate(date).format());
        }
        return properties;
    }

    private String getAuthor(SVNProperties revisionProperties) {
        if (revisionProperties == null) {
            return null;
        }
        return revisionProperties.getStringValue(SVNRevisionProperty.AUTHOR);
    }

    private Date getDate(SVNProperties revisionProperties) {
        if (revisionProperties == null) {
            return null;
        }
        String dateString = revisionProperties.getStringValue(SVNRevisionProperty.DATE);
        if (dateString == null) {
            return null;
        }
        return SVNDate.parseDate(dateString);
    }
}
