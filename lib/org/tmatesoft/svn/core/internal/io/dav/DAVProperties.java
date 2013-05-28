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
package org.tmatesoft.svn.core.internal.io.dav;

import java.util.Map;

import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class DAVProperties {
    
    private Map myProperties = new SVNHashMap();
    private boolean myIsCollection;
    private String myURL;
    private String myLoppedPath;
    private String myOriginalURL;
    
    public String getURL() {
        return myURL;
    }
    
    public boolean isCollection() {
        return myIsCollection;
    }
    
    public Map getProperties() {
        return myProperties;
    }
    
    public SVNPropertyValue getPropertyValue(DAVElement name) {
        return (SVNPropertyValue) myProperties.get(name);
    }
    
    public void setLoppedPath(String loppedPath) {
        myLoppedPath = loppedPath;
    }
    
    public String getLoppedPath() {
        return myLoppedPath;
    }
    
    public void setProperty(DAVElement name, SVNPropertyValue value) {
        myProperties.put(name, value);
    }
    
    public void setURL(String url) {
        myOriginalURL = myURL;
        myURL = url;
    }
    
    public String getOriginalURL() {
        return myOriginalURL;
    }
    
    public void setCollection(boolean collection) {
        myIsCollection = collection;
    }
}