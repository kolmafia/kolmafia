/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.wc;

import java.util.Collection;
import java.util.LinkedList;


/**
 * The <b>SVNDiffOptions</b> class is used to contain some rules for controlling the 
 * result of comparing two files. Such rules are used in diff/merge/annotate operations 
 * when it's necessary to say whether a file should be or should not be considered as 
 * changed. 
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public class SVNDiffOptions {
    
    private boolean myIsIgnoreAllWhitespace;
    private boolean myIsIgnoreAmountOfWhitespace;
    private boolean myIsIgnoreEOLStyle;

    /**
     * Creates a new diff options object. 
     * Equivalent to <code>SVNDiffOptions(false, false, false)</code>.  
     */
    public SVNDiffOptions() {
        this(false, false, false);
    }

    /**
     * Creates a new diff options object. 
     * 
     * @param ignoreAllWhitespace         controls whether whitespace differences must be ignored
     * @param ignoreAmountOfWhiteSpace    controls whether number of whitespaces must be ignored
     * @param ignoreEOLStyle              controls whether eol-marker differences must be ignored
     */
    public SVNDiffOptions(boolean ignoreAllWhitespace, boolean ignoreAmountOfWhiteSpace, boolean ignoreEOLStyle) {
        myIsIgnoreAllWhitespace = ignoreAllWhitespace;
        myIsIgnoreAmountOfWhitespace = ignoreAmountOfWhiteSpace;
        myIsIgnoreEOLStyle = ignoreEOLStyle;
    }
    
    /**
     * Says whether all whitespaces must be ignored while comparing files.
     * 
     * @return <span class="javakeyword">true</span> if ignored, otherwise 
     *         <span class="javakeyword">false</span> 
     */
    public boolean isIgnoreAllWhitespace() {
        return myIsIgnoreAllWhitespace;
    }
    
    /**
     * Sets whether all whitespaces must be ignored while comparing files. 
     * 
     * @param isIgnoreAllWhitespace controls whether whitespaces are to 
     *                              be ignored 
     */
    public void setIgnoreAllWhitespace(boolean isIgnoreAllWhitespace) {
        myIsIgnoreAllWhitespace = isIgnoreAllWhitespace;
    }
    
    /**
     * Says whether amont of whitespaces must be ignored while comparing files.
     * 
     * @return <span class="javakeyword">true</span> if ignored, otherwise 
     *         <span class="javakeyword">false</span> 
     */
    public boolean isIgnoreAmountOfWhitespace() {
        return myIsIgnoreAmountOfWhitespace;
    }
    
    /**
     * Sets whether number of whitespaces must be ignored while comparing files. 
     * 
     * @param isIgnoreAmountOfWhitespace controls whether number of whitespaces is
     *                                   to be ignored
     */
    public void setIgnoreAmountOfWhitespace(boolean isIgnoreAmountOfWhitespace) {
        myIsIgnoreAmountOfWhitespace = isIgnoreAmountOfWhitespace;
    }
    
    /**
     * Says whether eol style must be ignored while comparing files.
     * 
     * @return <span class="javakeyword">true</span> if ignored, otherwise 
     *         <span class="javakeyword">false</span> 
     */
    public boolean isIgnoreEOLStyle() {
        return myIsIgnoreEOLStyle;
    }
    
    /**
     * Sets whether eol style must be ignored while comparing files. 
     * 
     * @param isIgnoreEOLStyle controls whether eol style is
     *                         to be ignored
     */
    public void setIgnoreEOLStyle(boolean isIgnoreEOLStyle) {
        myIsIgnoreEOLStyle = isIgnoreEOLStyle;
    }

    /**
     * Returns a collection containing diff options as <code>String</code> object.
     * 
     * <p/>
     * If {@link #isIgnoreAllWhitespace()} is <span class="javakeyword">true</span>, 
     * <span class="javastring">"-w"</span> is added to the result. 
     * If {@link #isIgnoreAmountOfWhitespace()} is <span class="javakeyword">true</span>, 
     * <span class="javastring">"-b"</span> is added to the result. 
     * If {@link #isIgnoreEOLStyle()} is <span class="javakeyword">true</span>, 
     * <span class="javastring">"--ignore-eol-style"</span> is added to the result. 
     * 
     * @return    collection with <code>String</code> representations of diff options 
     * @since     1.2.0 
     */
    public Collection toOptionsCollection() {
        Collection opts = new LinkedList();
        if (isIgnoreAllWhitespace()) {
            opts.add("-w");
        }
        if (isIgnoreAmountOfWhitespace()) {
            opts.add("-b");
        }
        if (isIgnoreEOLStyle()) {
            opts.add("--ignore-eol-style");
        }
        return opts;
    }
    
}
