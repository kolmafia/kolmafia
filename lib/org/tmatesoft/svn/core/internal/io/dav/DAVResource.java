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

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.ISVNWorkspaceMediator;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
class DAVResource {
    
    private String myWURL;
    private String myVURL;
    private String myURL;
    private String myPath;
    private ISVNWorkspaceMediator myMediator;
    private long myRevision;
    private boolean myIsCopy;
    
    private DAVConnection myConnection;
    private SVNProperties myProperties;
    private boolean myIsAdded;

    public DAVResource(ISVNWorkspaceMediator mediator, DAVConnection connection, String path, long revision) {
        this(mediator, connection, path, revision, false);
    }
    
    public DAVResource(ISVNWorkspaceMediator mediator, DAVConnection connection, String path, long revision, boolean isCopy) {
        myPath = path;
        myMediator = mediator;
        String locationPath = SVNEncodingUtil.uriEncode(connection.getLocation().getPath());
        myURL = SVNPathUtil.append(locationPath, path);
        myRevision = revision;
        myConnection = connection;
        myIsCopy = isCopy;
    }

    public void setAdded(boolean added) {
        myIsAdded = added;
    }

    public boolean isAdded() {
        return myIsAdded;
    }
    
    public boolean isCopy() {
        return myIsCopy;
    }

    public String getURL() {
        return myURL;
    }   
    
    public String getPath() {
        return myPath;
    }
    
    public String getVersionURL() {
        return myVURL;
    }    
    
    public void fetchVersionURL(DAVResource parent, boolean force) throws SVNException {
        if (!force && getVersionURL() != null) {
            return;
        }
        if (!force) {
            if (myMediator != null && DAVUtil.isUseDAVWCURL()) {
                SVNPropertyValue value = myMediator.getWorkspaceProperty(SVNEncodingUtil.uriDecode(myPath), SVNProperty.WC_URL);
                myVURL = value == null ? null : value.getString();
                if ("".equals(myVURL)) {
                    myMediator.setWorkspaceProperty(SVNEncodingUtil.uriDecode(myPath), SVNProperty.WC_URL, null);
                    myVURL = null;
                }
                if (myVURL != null) {
                    return;
                }
            }
            if (parent != null && parent.getVersionURL() != null && parent.myRevision == myRevision) {
                myVURL = SVNPathUtil.append(parent.getVersionURL(), SVNPathUtil.tail(myPath));
                return;
            }
        }
            
        // now from server.
        String path = myURL;
        if (myRevision >= 0) {
            // get baseline collection url for revision from public url.
            DAVBaselineInfo info = DAVUtil.getBaselineInfo(myConnection, null, path, myRevision, false, false, null);
            path = SVNPathUtil.append(info.baselineBase, info.baselinePath);
        }
        // get "checked-in" property from baseline collection or from HEAD, this will be vURL.
        // this shouldn't be called for copied urls.
        try {
            myVURL = DAVUtil.getPropertyValue(myConnection, path, null, DAVElement.CHECKED_IN);
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.RA_DAV_PROPS_NOT_FOUND){
                SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.PROPERTY_NOT_FOUND,
                        "Could not fetch the Version Resource URL (needed during an import or when it is missing from the local, cached props)");
                SVNErrorManager.error(error, e, SVNLogType.NETWORK);
            }
            throw e;
        }
        if (myMediator != null) {
            SVNPropertyValue urlPropertyValue = DAVUtil.isUseDAVWCURL() ? SVNPropertyValue.create(myVURL) : null;
            myMediator.setWorkspaceProperty(SVNEncodingUtil.uriDecode(myPath), SVNProperty.WC_URL, urlPropertyValue);            
        }
    }

    public String getWorkingURL() {
        return myWURL;
    }
    
    
    public void dispose() {
        myProperties = null;
    }

    public void setWorkingURL(String location) {
        myWURL = location;
    }
    
    public void putProperty(String name, String value) {
        if (myProperties == null) {
            myProperties = new SVNProperties();
        }
        myProperties.put(name, value);       
    }

    public void putProperty(String name, SVNPropertyValue value) {
        if (myProperties == null) {
            myProperties = new SVNProperties();
        }
        myProperties.put(name, value);
    }
    
    public SVNProperties getProperties() {
        return myProperties;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        sb.append(myURL);
        sb.append("][");
        sb.append(myVURL);
        sb.append("][");
        sb.append(myWURL);
        sb.append("][");
        sb.append(myPath);
        sb.append("]");
        return sb.toString();
    }
}
