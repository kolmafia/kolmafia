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

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.tmatesoft.svn.core.ISVNDirEntryHandler;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLock;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNMergeInfo;
import org.tmatesoft.svn.core.SVNMergeInfoInheritance;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNRevisionProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.handlers.DAVDateRevisionHandler;
import org.tmatesoft.svn.core.internal.io.dav.handlers.DAVDeletedRevisionHandler;
import org.tmatesoft.svn.core.internal.io.dav.handlers.DAVEditorHandler;
import org.tmatesoft.svn.core.internal.io.dav.handlers.DAVFileRevisionHandler;
import org.tmatesoft.svn.core.internal.io.dav.handlers.DAVLocationSegmentsHandler;
import org.tmatesoft.svn.core.internal.io.dav.handlers.DAVLocationsHandler;
import org.tmatesoft.svn.core.internal.io.dav.handlers.DAVLogHandler;
import org.tmatesoft.svn.core.internal.io.dav.handlers.DAVMergeInfoHandler;
import org.tmatesoft.svn.core.internal.io.dav.handlers.DAVProppatchHandler;
import org.tmatesoft.svn.core.internal.io.dav.handlers.DAVReplayHandler;
import org.tmatesoft.svn.core.internal.io.dav.http.HTTPStatus;
import org.tmatesoft.svn.core.internal.io.dav.http.IHTTPConnectionFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSErrors;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.util.SVNHashSet;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNDepthFilterEditor;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.ISVNFileRevisionHandler;
import org.tmatesoft.svn.core.io.ISVNLocationEntryHandler;
import org.tmatesoft.svn.core.io.ISVNLocationSegmentHandler;
import org.tmatesoft.svn.core.io.ISVNLockHandler;
import org.tmatesoft.svn.core.io.ISVNReplayHandler;
import org.tmatesoft.svn.core.io.ISVNReporterBaton;
import org.tmatesoft.svn.core.io.ISVNSession;
import org.tmatesoft.svn.core.io.ISVNWorkspaceMediator;
import org.tmatesoft.svn.core.io.SVNCapability;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class DAVRepository extends SVNRepository {

    private DAVConnection myConnection;
    private IHTTPConnectionFactory myConnectionFactory;
    private boolean myIsSpoolResponse;
    
    private static boolean ourIsKeepCredentials = Boolean.valueOf(System.getProperty("svnkit.http.keepCredentials", Boolean.TRUE.toString())).booleanValue();
    
    public static void setKeepCredentials(boolean keepCredentials) {
        ourIsKeepCredentials = keepCredentials;
    }
    
    protected DAVRepository(IHTTPConnectionFactory connectionFactory, SVNURL location, ISVNSession options) {
        super(location, options);
        myConnectionFactory = connectionFactory;
    }
    
    public void testConnection() throws SVNException {
        try {
            openConnection();
            myRepositoryRoot = null;
            myRepositoryUUID = null;
            DAVConnection connection = getConnection();
            connection.fetchRepositoryUUID(this);
            connection.fetchRepositoryRoot(this);
        } finally {
            closeConnection();
        }
    }
    
    public boolean hasRepositoryUUID() {
        return myRepositoryUUID != null;
    }
    
    public void setRepositoryUUID(String uuid) {
        myRepositoryUUID = uuid;
    }

    public boolean hasRepositoryRoot() {
        return myRepositoryRoot != null;
    }

    public void setRepositoryRoot(SVNURL root) {
        myRepositoryRoot = root;
    }
    
    public SVNURL getRepositoryRoot(boolean forceConnection) throws SVNException {
        if (myRepositoryRoot != null && !forceConnection) {
            return myRepositoryRoot;
        }
        if (myRepositoryRoot == null) {
            try {
                openConnection();
                DAVConnection connection = getConnection();
                connection.fetchRepositoryRoot(this);
            } finally {
                closeConnection();
            }
        }
        return myRepositoryRoot;
    }

    public String getRepositoryUUID(boolean forceConnection) throws SVNException {
        if (myRepositoryUUID != null && !forceConnection) {
            return myRepositoryUUID;
        }
        if (myRepositoryUUID == null) {
            try {
                openConnection();
                DAVConnection connection = getConnection();
                connection.fetchRepositoryUUID(this);
            } finally {
                closeConnection();
            }
        }
        return myRepositoryUUID;
    }
    
    public void setSpoolResponse(boolean spool) {
        myIsSpoolResponse = spool;
        DAVConnection connection = getConnection();
        if (connection != null) {
            connection.setReportResponseSpooled(spool);
        }
    }
    
    public boolean isSpoolResponse() {
        return myIsSpoolResponse;
    }
    
    public void setAuthenticationManager(ISVNAuthenticationManager authManager) {
        DAVConnection connection = getConnection();
        if (authManager != getAuthenticationManager() && connection != null) {
            connection.clearAuthenticationCache();
        }
        super.setAuthenticationManager(authManager);
    }

    public long getLatestRevision() throws SVNException {        
        try {
            openConnection();
            String path = getLocation().getPath();
            path = SVNEncodingUtil.uriEncode(path);
            DAVConnection connection = getConnection();
            DAVBaselineInfo info = DAVUtil.getBaselineInfo(connection, this, path, -1, false, true, null);
            return info.revision;
        } finally {
            closeConnection();
        }
    }

    public long getDatedRevision(Date date) throws SVNException {
    	date = date == null ? new Date(System.currentTimeMillis()) : date;
		DAVDateRevisionHandler handler = new DAVDateRevisionHandler();
		StringBuffer request = DAVDateRevisionHandler.generateDateRevisionRequest(null, date);
    	try {
    		openConnection();
            String path = getLocation().getURIEncodedPath();
            DAVConnection connection = getConnection();
            path = DAVUtil.getVCCPath(connection, this, path);
			HTTPStatus status = connection.doReport(path, request, handler);
            if (status.getError() != null) {
                if (status.getError().getErrorCode() == SVNErrorCode.UNSUPPORTED_FEATURE) {
                    SVNErrorMessage err2 = SVNErrorMessage.create(status.getError().getErrorCode(), 
                            "Server does not support date-based operations");
                    SVNErrorManager.error(err2, status.getError(), SVNLogType.NETWORK);
                }
                SVNErrorManager.error(status.getError(), SVNLogType.NETWORK);
            }
    	} finally {
    		closeConnection();
    	}
    	return handler.getRevisionNumber();
    }

    public SVNNodeKind checkPath(String path, long revision) throws SVNException {
        DAVBaselineInfo info = null;
        SVNNodeKind kind = SVNNodeKind.NONE;
        try {
            openConnection();
            path = doGetFullPath(path);
            path = SVNEncodingUtil.uriEncode(path);
            DAVConnection connection = getConnection();
            info = DAVUtil.getBaselineInfo(connection, this, path, revision, true, false, info);
            kind = info.isDirectory ? SVNNodeKind.DIR : SVNNodeKind.FILE;
        } catch (SVNException e) {
            SVNErrorMessage error = e.getErrorMessage();
            while (error != null) {
                if (error.getErrorCode() == SVNErrorCode.FS_NOT_FOUND) {
                    return kind;
                }
                error = error.getChildErrorMessage();
            }
            throw e;
        } finally {
            closeConnection();
        }
        return kind;
    }
    
    public SVNProperties getRevisionProperties(long revision, SVNProperties properties) throws SVNException {
        properties = properties == null ? new SVNProperties() : properties;
        try {
            openConnection();
            String path = getLocation().getPath();
            path = SVNEncodingUtil.uriEncode(path);
            DAVConnection connection = getConnection();
            DAVProperties source = DAVUtil.getBaselineProperties(connection, this, path, revision, null);
            properties = DAVUtil.filterProperties(source, properties);
            if (revision >= 0) {
                String commitMessage = properties.getStringValue(SVNRevisionProperty.LOG);
                getOptions().saveCommitMessage(DAVRepository.this, revision, commitMessage);
            }
        } finally {
            closeConnection();
        }
        return properties;
    }
    
    public SVNPropertyValue getRevisionPropertyValue(long revision, String propertyName) throws SVNException {
        SVNProperties properties = getRevisionProperties(revision, null);
        return properties.getSVNPropertyValue(propertyName);
    }

    public long getFile(String path, long revision, final SVNProperties properties, OutputStream contents) throws SVNException {
        long fileRevision = revision;
        try {
            openConnection();
            path = doGetFullPath(path);
            path = SVNEncodingUtil.uriEncode(path);
            DAVConnection connection = getConnection();
            if (revision != -2) {
                DAVBaselineInfo info = DAVUtil.getBaselineInfo(connection, this, path, revision, false, true, null);
                path = SVNPathUtil.append(info.baselineBase, info.baselinePath);
                fileRevision = info.revision; 
            }
            if (properties != null) {
                DAVProperties props = DAVUtil.getResourceProperties(connection, path, null, null);
                DAVUtil.filterProperties(props, properties);
                for (Iterator names = props.getProperties().keySet().iterator(); names.hasNext();) {
                    DAVElement property = (DAVElement) names.next();
                    DAVUtil.setSpecialWCProperties(properties, property, props.getPropertyValue(property));
                }
                if (fileRevision >= 0) {
                    properties.put(SVNProperty.REVISION, Long.toString(fileRevision));
                }
            }
            if (contents != null) {
                connection.doGet(path, contents);
            }
        } finally {
            closeConnection();
        }
        return fileRevision;
    }

    public long getDir(String path, long revision, final SVNProperties properties, final ISVNDirEntryHandler handler) throws SVNException {
        return getDir(path, revision, properties, SVNDirEntry.DIRENT_ALL, handler);
    }

    public long getDir(String path, long revision, SVNProperties properties, int entryFields, ISVNDirEntryHandler handler) throws SVNException {
        long dirRevision = revision;
        try {
            openConnection();
            path = doGetFullPath(path);
            path = SVNEncodingUtil.uriEncode(path);
            final String fullPath = path;
            DAVConnection connection = getConnection();
            if (revision != -2) {
                DAVBaselineInfo info = DAVUtil.getBaselineInfo(connection, this, path, revision, false, true, null);
                path = SVNPathUtil.append(info.baselineBase, info.baselinePath);
                dirRevision = info.revision; 
            }
            
            DAVProperties deadProp = DAVUtil.getResourceProperties(connection, path, null, new DAVElement[] {DAVElement.DEADPROP_COUNT});
            boolean supportsDeadPropCount = deadProp != null && deadProp.getPropertyValue(DAVElement.DEADPROP_COUNT) != null ;
            
            if (handler != null) {
                DAVElement[] whichProps = null;
                if ((entryFields & SVNDirEntry.DIRENT_HAS_PROPERTIES) == 0 ||
                        supportsDeadPropCount) {
                    
                    List individualProps = new LinkedList();
                    
                    if ((entryFields & SVNDirEntry.DIRENT_KIND) != 0) {
                        individualProps.add(DAVElement.RESOURCE_TYPE);
                    }
                    if ((entryFields & SVNDirEntry.DIRENT_SIZE) != 0) {
                        individualProps.add(DAVElement.GET_CONTENT_LENGTH);
                    }
                    if ((entryFields & SVNDirEntry.DIRENT_HAS_PROPERTIES) != 0) {
                        individualProps.add(DAVElement.DEADPROP_COUNT);
                    }
                    if ((entryFields & SVNDirEntry.DIRENT_CREATED_REVISION) != 0) {
                        individualProps.add(DAVElement.VERSION_NAME);
                    }
                    if ((entryFields & SVNDirEntry.DIRENT_TIME) != 0) {
                        individualProps.add(DAVElement.CREATION_DATE);
                    }
                    if ((entryFields & SVNDirEntry.DIRENT_LAST_AUTHOR) != 0) {
                        individualProps.add(DAVElement.CREATOR_DISPLAY_NAME);
                    }
                    whichProps = (DAVElement[]) individualProps.toArray(new DAVElement[individualProps.size()]);
                }
                final int parentPathSegments = SVNPathUtil.getSegmentsCount(path);
                Map dirEntsMap = new SVNHashMap();
                HTTPStatus status = DAVUtil.getProperties(connection, path, DAVUtil.DEPTH_ONE, null, whichProps, dirEntsMap);
                if (status.getError() != null) {
                    SVNErrorManager.error(status.getError(), SVNLogType.NETWORK);
                }
                if (!hasRepositoryRoot()) {
                    connection.fetchRepositoryRoot(this);                    
                }
                SVNURL repositryRoot = getRepositoryRoot(false);
                for(Iterator dirEnts = dirEntsMap.keySet().iterator(); dirEnts.hasNext();) {
                    String url = (String) dirEnts.next();
                    DAVProperties child = (DAVProperties) dirEntsMap.get(url);
                    String href = child.getURL();
                    if (parentPathSegments == SVNPathUtil.getSegmentsCount(href)) {
                        continue;
                    }
                    String name = SVNEncodingUtil.uriDecode(SVNPathUtil.tail(href));
                    
                    SVNNodeKind kind = SVNNodeKind.UNKNOWN;
                    if ((entryFields & SVNDirEntry.DIRENT_KIND) != 0) {
                        kind = child.isCollection() ? SVNNodeKind.DIR : SVNNodeKind.FILE;  
                    }
                    
                    long size = 0;
                    if ((entryFields & SVNDirEntry.DIRENT_SIZE) != 0) {
                    SVNPropertyValue sizeValue = child.getPropertyValue(DAVElement.GET_CONTENT_LENGTH);
                        if (sizeValue != null) {
                            try {
                                size = Long.parseLong(sizeValue.getString());
                            } catch (NumberFormatException nfe) {
                                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, nfe), SVNLogType.NETWORK);
                            }
                        }
                    }

                    boolean hasProperties = false;
                    if ((entryFields & SVNDirEntry.DIRENT_HAS_PROPERTIES) != 0) {
                        if (supportsDeadPropCount) {
                            SVNPropertyValue propVal = child.getPropertyValue(DAVElement.DEADPROP_COUNT);
                            if (propVal == null) {
                                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.INCOMPLETE_DATA, 
                                        "Server response missing the expected deadprop-count property");
                                SVNErrorManager.error(err, SVNLogType.NETWORK);
                            } else {
                                long propCount = -1;
                                try {
                                    propCount = Long.parseLong(propVal.getString());
                                } catch (NumberFormatException nfe) {
                                    SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, nfe), SVNLogType.NETWORK);
                                }
                                hasProperties = propCount > 0;
                            }
                        } else {
                            for(Iterator props = child.getProperties().keySet().iterator(); props.hasNext();) {
                                DAVElement property = (DAVElement) props.next();
                                if (DAVElement.SVN_CUSTOM_PROPERTY_NAMESPACE.equals(property.getNamespace()) || 
                                        DAVElement.SVN_SVN_PROPERTY_NAMESPACE.equals(property.getNamespace())) {
                                    hasProperties = true;
                                    break;
                                }
                            }
                        }
                    }                    
                    
                    long lastRevision = INVALID_REVISION;
                    if ((entryFields & SVNDirEntry.DIRENT_CREATED_REVISION) != 0) {
                        Object revisionStr = child.getPropertyValue(DAVElement.VERSION_NAME);
                        if (revisionStr != null) {
                            try {
                                lastRevision = Long.parseLong(revisionStr.toString());
                            } catch (NumberFormatException nfe) {
                                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA);
                                SVNErrorManager.error(err, SVNLogType.NETWORK);
                            }
                        }
                    }

                    Date date = null;
                    if ((entryFields & SVNDirEntry.DIRENT_TIME) != 0) {
                        SVNPropertyValue dateValue = child.getPropertyValue(DAVElement.CREATION_DATE);
                        if (dateValue != null) {
                            date = SVNDate.parseDate(dateValue.getString());
                        }
                    }

                    String author = null;
                    if ((entryFields & SVNDirEntry.DIRENT_LAST_AUTHOR) != 0) {
                        SVNPropertyValue authorValue = child.getPropertyValue(DAVElement.CREATOR_DISPLAY_NAME);
                        author = authorValue == null ? null : authorValue.getString();
                    }
                    
                    SVNURL childURL = getLocation().setPath(fullPath, true);
                    childURL = childURL.appendPath(name, false);
                    SVNDirEntry dirEntry = new SVNDirEntry(childURL, repositryRoot, name, kind, size, hasProperties, lastRevision, date, author);
                    handler.handleDirEntry(dirEntry);
                }                
            }
            if (properties != null) {
                DAVProperties dirProps = DAVUtil.getResourceProperties(connection, path, null, null);
                DAVUtil.filterProperties(dirProps, properties);
                for(Iterator props = dirProps.getProperties().keySet().iterator(); props.hasNext();) {
                    DAVElement property = (DAVElement) props.next();
                    DAVUtil.setSpecialWCProperties(properties, property, dirProps.getPropertyValue(property));
                }
            }
        } finally {
            closeConnection();
        }
        return dirRevision;
    }

    public SVNDirEntry getDir(String path, long revision, boolean includeComments, final Collection entries) throws SVNException {
        final SVNDirEntry[] parent = new SVNDirEntry[1];
        final String[] parentVCC = new String[1];
        try {
            openConnection();
            path = doGetFullPath(path);
            path = SVNEncodingUtil.uriEncode(path);
            final String fullPath = path;
            DAVConnection connection = getConnection();
            if (revision >= 0) {
                DAVBaselineInfo info = DAVUtil.getBaselineInfo(connection, this, path, revision, false, true, null);
                path = SVNPathUtil.append(info.baselineBase, info.baselinePath);
            }
            final int parentPathSegments = SVNPathUtil.getSegmentsCount(path);
            final List vccs = new ArrayList();
            
            DAVElement[] dirProperties = new DAVElement[] {DAVElement.VERSION_CONTROLLED_CONFIGURATION, 
                    DAVElement.VERSION_NAME, DAVElement.GET_CONTENT_LENGTH, DAVElement.RESOURCE_TYPE, 
                    DAVElement.CREATOR_DISPLAY_NAME, DAVElement.CREATION_DATE};
            Map dirEntsMap = new SVNHashMap();
            HTTPStatus status = DAVUtil.getProperties(connection, path, DAVUtil.DEPTH_ONE, null, dirProperties, dirEntsMap);
            if (status.getError() != null) {
                SVNErrorManager.error(status.getError(), SVNLogType.NETWORK);
            }
            for(Iterator dirEnts = dirEntsMap.keySet().iterator(); dirEnts.hasNext();) {
                String url = (String) dirEnts.next();
                DAVProperties child = (DAVProperties) dirEntsMap.get(url);
                String href = child.getURL();
                String name = "";
                if (parentPathSegments != SVNPathUtil.getSegmentsCount(href)) {
                    name = SVNEncodingUtil.uriDecode(SVNPathUtil.tail(href));
                }
                SVNNodeKind kind = SVNNodeKind.FILE;
                Object revisionStr = child.getPropertyValue(DAVElement.VERSION_NAME);
                long lastRevision = -1;
                if (revisionStr != null) {
                    try {
                        lastRevision = Long.parseLong(revisionStr.toString());
                    } catch (NumberFormatException nfe) {
                        SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, nfe), SVNLogType.NETWORK);
                    }
                }
                SVNPropertyValue sizeValue = child.getPropertyValue(DAVElement.GET_CONTENT_LENGTH);
                long size = 0;
                if (sizeValue != null) {
                    try {
                        size = Long.parseLong(sizeValue.getString());
                    } catch (NumberFormatException nfe) {
                        SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, nfe), SVNLogType.NETWORK);
                    }
                }
                if (child.isCollection()) {
                    kind = SVNNodeKind.DIR;
                }
                SVNPropertyValue authorValue = child.getPropertyValue(DAVElement.CREATOR_DISPLAY_NAME);
                String author = authorValue == null ? null : authorValue.getString();
                SVNPropertyValue dateValue = child.getPropertyValue(DAVElement.CREATION_DATE);
                Date date = dateValue != null ? SVNDate.parseDate(dateValue.getString()) : null;
                connection.fetchRepositoryRoot(this);
                SVNURL repositoryRoot = getRepositoryRoot(false);
                SVNURL childURL = getLocation().setPath(fullPath, true);
                if ("".equals(name)) {
                    parent[0] = new SVNDirEntry(childURL, repositoryRoot, name, kind, size, false, lastRevision, 
                            date, author);
                    SVNPropertyValue vcc = child.getPropertyValue(DAVElement.VERSION_CONTROLLED_CONFIGURATION);
                    parentVCC[0] = vcc == null ? null : vcc.getString();
                } else {
                    childURL = childURL.appendPath(name, false);
                    if (entries != null) {
                    	entries.add(new SVNDirEntry(childURL, repositoryRoot, name, kind, size, false, lastRevision, 
                    	        date, author));
                    }
                    SVNPropertyValue vccValue = child.getPropertyValue(DAVElement.VERSION_CONTROLLED_CONFIGURATION);
                    vccs.add(vccValue);
                }
            }

            if (includeComments) {
                DAVElement logProperty = DAVElement.getElement(DAVElement.SVN_SVN_PROPERTY_NAMESPACE, "log");
                Iterator ents = entries != null ? entries.iterator() : null;
                SVNDirEntry entry = parent[0];
                String vcc = parentVCC[0];
                int index = 0;
                while(true) {
                    if (entry.getRevision() >= 0 && vcc != null) {
                        String label = Long.toString(entry.getRevision());
                        if (entry.getDate() != null && getOptions().hasCommitMessage(this, entry.getRevision())) {
                            String message = getOptions().getCommitMessage(this, entry.getRevision());
                            entry.setCommitMessage(message);
                        } else if (entry.getDate() != null && vcc != null) {
                            final SVNDirEntry currentEntry = entry;
                            String commitMessage = null;
                            try {
                                commitMessage = DAVUtil.getPropertyValue(connection, vcc, label, logProperty);
                            } catch (SVNException e) {
                                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.RA_DAV_PROPS_NOT_FOUND) {
                                    throw e;
                                }
                            }                        
                            getOptions().saveCommitMessage(DAVRepository.this, currentEntry.getRevision(), commitMessage);
                            currentEntry.setCommitMessage(commitMessage);
                        }
                    }
                    if (ents != null && ents.hasNext()) {
                        entry = (SVNDirEntry) ents.next();
                        SVNPropertyValue vccValue = (SVNPropertyValue) vccs.get(index);
                        vcc = vccValue != null ? vccValue.getString() : null;
                        index++;
                    } else {
                        break;
                    }
                }
            }
        } finally {
            closeConnection();
        }
        return parent[0];
    }

    public void replay(long lowRevision, long highRevision, boolean sendDeltas, ISVNEditor editor) throws SVNException {
        try {
            openConnection();
            StringBuffer request = DAVReplayHandler.generateReplayRequest(highRevision, lowRevision, sendDeltas);
            DAVReplayHandler handler = new DAVReplayHandler(editor, true);

            String bcPath = SVNEncodingUtil.uriEncode(getLocation().getPath());
            DAVConnection connection = getConnection();
            HTTPStatus status = connection.doReport(bcPath, request, handler);
            if (status.getCode() == 501) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_NOT_IMPLEMENTED, 
                        "'replay' REPORT not implemented");
                SVNErrorManager.error(err, status.getError(), SVNLogType.NETWORK);
            } else if (status.getError() != null) {
                SVNErrorManager.error(status.getError(), SVNLogType.NETWORK);
            }
        } finally {
            closeConnection();
        }
    }

    public void setRevisionPropertyValue(long revision, String propertyName, SVNPropertyValue propertyValue) throws SVNException {
        assertValidRevision(revision);

        StringBuffer request = DAVProppatchHandler.generatePropertyRequest(null, propertyName, propertyValue);
        try {
            openConnection();
            // get baseline url and proppatch.
            DAVConnection connection = getConnection();
            DAVBaselineInfo info = DAVUtil.getBaselineInfo(connection, this, 
                    SVNEncodingUtil.uriEncode(getLocation().getPath()), revision, false, false, null);
            String path = SVNPathUtil.append(info.baselineBase, info.baselinePath);
            path = info.baseline;
            DAVProppatchHandler handler = new DAVProppatchHandler();
            SVNErrorMessage requestError = null;
            try {
                connection.doProppatch(null, path, request, handler, null);
            } catch (SVNException e) {
                requestError = e.getErrorMessage();                
            }
            if (requestError != null || handler.getError() != null){
                if (requestError != null){
                    requestError.setChildErrorMessage(handler.getError());
                } else {
                    requestError = handler.getError();
                }
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, 
                        "DAV request failed; it's possible that the repository's " +
                        "pre-revprop-change hook either failed or is non-existent");
                SVNErrorManager.error(err, requestError, SVNLogType.NETWORK);
            }
        } finally {
            closeConnection();
        }
    }

    public ISVNEditor getCommitEditor(String logMessage, Map locks, boolean keepLocks, ISVNWorkspaceMediator mediator) throws SVNException {
        return getCommitEditor(logMessage, locks, keepLocks, null, mediator);
    }

    public SVNLock getLock(String path) throws SVNException {
        try {
            openConnection();
            path = doGetFullPath(path);
            path = SVNEncodingUtil.uriEncode(path);
            DAVConnection connection = getConnection();
            return connection.doGetLock(path, this);
        } finally {
            closeConnection();
        }
    }

    public SVNLock[] getLocks(String path) throws SVNException {
        try {
            openConnection();
            path = doGetFullPath(path);
            path = SVNEncodingUtil.uriEncode(path);
            DAVConnection connection = getConnection();
            return connection.doGetLocks(path);
        } finally {
            closeConnection();
        }
    }

    public void lock(Map pathsToRevisions, String comment, boolean force, ISVNLockHandler handler) throws SVNException {
        try {
            openConnection();
            DAVConnection connection = getConnection();

            for(Iterator paths = pathsToRevisions.keySet().iterator(); paths.hasNext();) {
                String path = (String) paths.next();
                Long revision = (Long) pathsToRevisions.get(path);
                String repositoryPath = doGetRepositoryPath(path);
                path = doGetFullPath(path);
                path = SVNEncodingUtil.uriEncode(path);
                SVNLock lock = null;
                SVNErrorMessage error = null;
                long revisionNumber = revision != null ? revision.longValue() : -1;
                try {
                     lock = connection.doLock(path, this, comment, force, revisionNumber);
                } catch (SVNException e) {
                    error = null;
                    if (e.getErrorMessage() != null) {
                        if (FSErrors.isLockError(e.getErrorMessage())) {
                            error = e.getErrorMessage();                            
                        }
                    }
                    if (error == null) {
                        throw e;
                    }
                }
                if (handler != null) {
                    handler.handleLock(repositoryPath, lock, error);
                }
            }
        } finally {
            closeConnection();
        }
    }

    public void unlock(Map pathToTokens, boolean force, ISVNLockHandler handler) throws SVNException {
        try {
            openConnection();
            DAVConnection connection = getConnection();

            for (Iterator paths = pathToTokens.keySet().iterator(); paths.hasNext();) {
                String path = (String) paths.next();
                String shortPath = path;
                String id = (String) pathToTokens.get(path);
                String repositoryPath = doGetRepositoryPath(path);
                path = doGetFullPath(path);
                path = SVNEncodingUtil.uriEncode(path);
                SVNErrorMessage error = null;
                try {
                    connection.doUnlock(path, this, id, force);
                    error = null;
                } catch (SVNException e) {
                    if (e.getErrorMessage() != null && 
                            (e.getErrorMessage().getErrorCode() == SVNErrorCode.RA_NOT_LOCKED  || FSErrors.isUnlockError(e.getErrorMessage()))) {
                        error = e.getErrorMessage();

                        if (repositoryPath != null && repositoryPath.startsWith("/")) {
                            shortPath = repositoryPath.substring("/".length());
                        } else if (repositoryPath != null) {
                            shortPath = repositoryPath;
                        } else {
                            shortPath = "";
                        }
                        error = SVNErrorMessage.create(error.getErrorCode(), error.getMessageTemplate(), shortPath);
                    } else {
                        throw e;
                    }
                }
                if (handler != null) {
                    handler.handleUnlock(repositoryPath, new SVNLock(repositoryPath, id, null, null, null, null), error);
                }
            }
        } finally {
            closeConnection();
        }
    }

    public SVNDirEntry info(String path, long revision) throws SVNException {
        try {
            openConnection();
            path = doGetFullPath(path);
            path = SVNEncodingUtil.uriEncode(path);
            final String fullPath = path;
            DAVConnection connection = getConnection();

            if (revision >= 0) {
                try {
                    DAVBaselineInfo info = DAVUtil.getBaselineInfo(connection, this, path, revision, false, true, null);
                    path = SVNPathUtil.append(info.baselineBase, info.baselinePath);
                } catch (SVNException e) {
                    if (e.getErrorMessage() != null && e.getErrorMessage().getErrorCode() == SVNErrorCode.FS_NOT_FOUND) {
                        return null;
                    }
                    throw e;
                }
            }
            DAVElement[] elements = null;
            Map propsMap = new SVNHashMap();
            HTTPStatus status = DAVUtil.getProperties(connection, path, 0, null, elements, propsMap);
            if (status.getError() != null) {
                if (status.getError().getErrorCode() == SVNErrorCode.FS_NOT_FOUND) {
                    return null;
                }
                SVNErrorManager.error(status.getError(), SVNLogType.NETWORK);
            }
            if (!propsMap.isEmpty()) {
                DAVProperties props = (DAVProperties) propsMap.values().iterator().next();
                return createDirEntry(fullPath, props);
            }
        } finally {
            closeConnection();
        }
        return null;
    }

    public void closeSession() {
        lock(true);
        try {
            if (myConnection != null) {
                myConnection.close();
                myConnection = null;
            }
        } finally {
            unlock();
        }
    }

    public String doGetFullPath(String relativeOrRepositoryPath) throws SVNException {
        if (relativeOrRepositoryPath == null) {
            return doGetFullPath("/");
        }
        String fullPath;
        if (relativeOrRepositoryPath.length() > 0 && relativeOrRepositoryPath.charAt(0) == '/') {
            DAVConnection connection = getConnection();
            connection.fetchRepositoryRoot(this);
            fullPath = SVNPathUtil.append(myRepositoryRoot.getPath(), relativeOrRepositoryPath);
        } else {
            fullPath = SVNPathUtil.append(getLocation().getPath(), relativeOrRepositoryPath);
        }
        if (!fullPath.startsWith("/")) {
            fullPath = "/" + fullPath;
        }
        return fullPath;
    }
    
    public void diff(SVNURL url, long targetRevision, long revision, String target, boolean ignoreAncestry, 
            SVNDepth depth, boolean getContents, ISVNReporterBaton reporter, ISVNEditor editor) throws SVNException {
        if (url == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_ILLEGAL_URL, "URL could not be NULL");
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        if (revision < 0) {
            revision = targetRevision;
        }
        boolean sendAll = myConnectionFactory.useSendAllForDiff(this);
        runReport(getLocation(), targetRevision, target, url.toString(), depth, ignoreAncestry, false, 
                getContents, false, sendAll, false, true, reporter, editor);
    }

    public void status(long revision, String target, SVNDepth depth, ISVNReporterBaton reporter, ISVNEditor editor) throws SVNException {
        runReport(getLocation(), revision, target, null, depth, false, false, false, false, true, false, 
                false, reporter, editor);
    }

    public void update(SVNURL url, long revision, String target, SVNDepth depth, ISVNReporterBaton reporter, ISVNEditor editor) throws SVNException {
        if (url == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_ILLEGAL_URL, "URL could not be NULL");
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        runReport(getLocation(), revision, target, url.toString(), depth, true, false, true, false, true, true, 
                false, reporter, editor);
    }

    public void update(long revision, String target, SVNDepth depth, boolean sendCopyFromArgs, 
            ISVNReporterBaton reporter, ISVNEditor editor) throws SVNException {
        runReport(getLocation(), revision, target, null, depth, false, false, true, sendCopyFromArgs, true, 
                false, false, reporter, editor);
    }

    public boolean hasCapability(SVNCapability capability) throws SVNException {
        if (capability == SVNCapability.COMMIT_REVPROPS) {
            return true;
        }
	    try {
            openConnection();
            DAVConnection connection = getConnection();
            String result = connection.getCapabilityResponse(capability);
            if (DAVConnection.DAV_CAPABILITY_SERVER_YES.equals(result)) {
                if (capability == SVNCapability.MERGE_INFO) {
                    SVNException error = null;
                    try {
                        doGetMergeInfo(new String[]{""}, -1, SVNMergeInfoInheritance.EXPLICIT, false);
                    } catch (SVNException svne) {
                        error = svne;
                    }
                    if (error != null){
                        if (error.getErrorMessage().getErrorCode() == SVNErrorCode.UNSUPPORTED_FEATURE) {
                            result = DAVConnection.DAV_CAPABILITY_NO;
                        } else if (error.getErrorMessage().getErrorCode() == SVNErrorCode.FS_NOT_FOUND) {
                            result = DAVConnection.DAV_CAPABILITY_YES;
                        } else {
                            throw error;
                        }
                    } else {
                        result = DAVConnection.DAV_CAPABILITY_YES;
                    }
                    connection.setCapability(SVNCapability.MERGE_INFO, result);
                } else {
                    SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.UNKNOWN_CAPABILITY,
                            "Don''t know how to handle ''{0}'' for capability ''{1}''",
                            new Object[]{DAVConnection.DAV_CAPABILITY_SERVER_YES, SVNCapability.MERGE_INFO});
                    SVNErrorManager.error(error, SVNLogType.NETWORK);
                }
            }
            if (DAVConnection.DAV_CAPABILITY_YES.equals(result)) {
                return true;
            } else if (DAVConnection.DAV_CAPABILITY_NO.equals(result)) {
                return false;
            } else if (result == null) {
                SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.UNKNOWN_CAPABILITY,
                        "Don''t know anything about capability ''{0}''",
                        new Object[]{capability});
                SVNErrorManager.error(error, SVNLogType.NETWORK);
            } else {
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_OPTIONS_REQ_FAILED,
                        "Attempt to fetch capability ''{0}'' resulted in ''{1}''",
                        new Object[]{capability, result}), SVNLogType.NETWORK);
            }
        } finally {
            closeConnection();
        }
        return false;
    }

    protected int getFileRevisionsImpl(String path, long startRevision, long endRevision, 
            boolean includeMergedRevisions, ISVNFileRevisionHandler handler) throws SVNException {
        String bcPath = getLocation().getPath();
        bcPath = SVNEncodingUtil.uriEncode(bcPath);
        try {
            openConnection();
            DAVConnection connection = getConnection();

            path = "".equals(path) ? "" : doGetRepositoryPath(path);
            DAVFileRevisionHandler davHandler = new DAVFileRevisionHandler(handler);
            StringBuffer request = DAVFileRevisionHandler.generateFileRevisionsRequest(null, startRevision, 
                    endRevision, path, includeMergedRevisions);
            long revision = -1;
            if (isValidRevision(startRevision) && isValidRevision(endRevision)) {
                revision = Math.max(startRevision, endRevision);                
            }
            DAVBaselineInfo info = DAVUtil.getBaselineInfo(connection, this, bcPath, revision, false, false, null);
            bcPath = SVNPathUtil.append(info.baselineBase, info.baselinePath);
            HTTPStatus status = connection.doReport(bcPath, request, davHandler);
            if (status.getCode() == 501) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_NOT_IMPLEMENTED, 
                        "'get-file-revs' REPORT not implemented");
                SVNErrorManager.error(err, status.getError(), SVNLogType.NETWORK);
            } else if (status.getError() != null) {
                SVNErrorManager.error(status.getError(), SVNLogType.NETWORK);
            }
            if (davHandler.getEntriesCount() <= 0) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, 
                        "The file-revs report didn't contain any revisions");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }
            return davHandler.getEntriesCount();
        } finally {
            closeConnection();
        }
    }
    
    //TODO: FIXME
    protected long logImpl(String[] targetPaths, long startRevision, long endRevision, 
                    boolean changedPath, boolean strictNode, long limit, 
                    boolean includeMergedRevisions, String[] revPropNames, 
                    final ISVNLogEntryHandler handler) throws SVNException {
        if (targetPaths == null || targetPaths.length == 0) {
            targetPaths = new String[]{""};
        }
        DAVLogHandler davHandler = null;
        ISVNLogEntryHandler cachingHandler = new ISVNLogEntryHandler() {
                public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {
                    if (logEntry.getDate() != null) {
                        getOptions().saveCommitMessage(DAVRepository.this, logEntry.getRevision(), logEntry.getMessage());
                    }
                    if (handler != null) {
                        handler.handleLogEntry(logEntry);
                    }
                }            
        };
        
        long latestRev = -1;
        if (isInvalidRevision(startRevision)) {
            startRevision = latestRev = getLatestRevision();
        }
        if (isInvalidRevision(endRevision)) {
            endRevision = latestRev != -1 ? latestRev : getLatestRevision(); 
        }
        
        try {
            openConnection();
            DAVConnection connection = getConnection();
            String[] fullPaths = new String[targetPaths.length];
            
            for (int i = 0; i < targetPaths.length; i++) {
                fullPaths[i] = doGetFullPath(targetPaths[i]);
            }
            Collection relativePaths = new SVNHashSet();
            String path = SVNPathUtil.condencePaths(fullPaths, relativePaths, false);
            if (relativePaths.isEmpty()) {
                relativePaths.add("");
            }
            fullPaths = (String[]) relativePaths.toArray(new String[relativePaths.size()]);
            
            StringBuffer request = DAVLogHandler.generateLogRequest(null, startRevision, endRevision, changedPath, 
                    strictNode, includeMergedRevisions, revPropNames, limit, fullPaths);
            davHandler = new DAVLogHandler(cachingHandler, limit, revPropNames);
            if (davHandler.isWantCustomRevprops()) {
                String capability = connection.getCapabilityResponse(SVNCapability.LOG_REVPROPS);
                if (!DAVConnection.DAV_CAPABILITY_SERVER_YES.equals(capability) &&
                        !DAVConnection.DAV_CAPABILITY_YES.equals(capability)) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_NOT_IMPLEMENTED, 
                            "Server does not support custom revprops via log");
                    SVNErrorManager.error(err, SVNLogType.NETWORK);
                }
            }
            long revision = Math.max(startRevision, endRevision);
            path = SVNEncodingUtil.uriEncode(path);
            DAVBaselineInfo info = DAVUtil.getBaselineInfo(connection, this, path, revision, false, false, null);
            path = SVNPathUtil.append(info.baselineBase, info.baselinePath);
            try {
                HTTPStatus status = connection.doReport(path, request, davHandler);
                if (status.getError() != null) {
                    SVNErrorManager.error(status.getError(), SVNLogType.NETWORK);
                }
            } catch (SVNException e) {
                if (e.getErrorMessage() != null && e.getErrorMessage().getErrorCode() == SVNErrorCode.UNKNOWN && 
                        davHandler.isCompatibleMode()) {
                    cachingHandler.handleLogEntry(SVNLogEntry.EMPTY_ENTRY);
                } else {
                    throw e;
                }
                
            }
        } finally {
            closeConnection();
        }
        return davHandler.getEntriesCount();
    }

    protected void openConnection() throws SVNException {
        fireConnectionOpened();
        lock();
        if (myConnection == null) {
            myConnection = createDAVConnection(myConnectionFactory, this);
            myConnection.setReportResponseSpooled(isSpoolResponse());
            myConnection.open(this);
        }
    }

    protected DAVConnection createDAVConnection(IHTTPConnectionFactory connectionFactory, DAVRepository repo) {
        return new DAVConnection(connectionFactory, repo); 
    }
    
    protected void closeConnection() {
        DAVConnection connection = getConnection();
        if (connection != null && !ourIsKeepCredentials) {
            connection.clearAuthenticationCache();
        }
        if (!getOptions().keepConnection(this)) {
            closeSession();
        }
        unlock();
        fireConnectionClosed();
    }
    
    protected int getLocationsImpl(String path, long pegRevision, long[] revisions, ISVNLocationEntryHandler handler) throws SVNException {
        try {
            openConnection();
            DAVConnection connection = getConnection();

            if (path.startsWith("/")) {
                // (root + path), relative to location
                connection.fetchRepositoryRoot(this);
                path = SVNPathUtil.append(myRepositoryRoot.getPath(), path);
                if (path.equals(getLocation().getPath())) {
                    path = "";
                } else {
                    path = path.substring(getLocation().getPath().length() + 1);
                }
            }
            StringBuffer request = DAVLocationsHandler.generateLocationsRequest(null, path, pegRevision, revisions);
            
            DAVLocationsHandler davHandler = new DAVLocationsHandler(handler);
            String root = getLocation().getPath();
            root = SVNEncodingUtil.uriEncode(root);
            DAVBaselineInfo info = DAVUtil.getBaselineInfo(connection, this, root, pegRevision, false, false, null);            
            path = SVNPathUtil.append(info.baselineBase, info.baselinePath);
            HTTPStatus status = connection.doReport(path, request, davHandler);
            if (status.getCode() == 501) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_NOT_IMPLEMENTED, 
                        "'get-locations' REPORT not implemented");
                SVNErrorManager.error(err, status.getError(), SVNLogType.NETWORK);
            } else if (status.getError() != null) {
                SVNErrorManager.error(status.getError(), SVNLogType.NETWORK);
            }
            return davHandler.getEntriesCount();
        } finally {
            closeConnection();
        }
    }

    protected long getLocationSegmentsImpl(String path, long pegRevision, long startRevision, long endRevision, ISVNLocationSegmentHandler handler) throws SVNException {
        try {
            openConnection();
            DAVConnection connection = getConnection();

            boolean absolutePath = path.startsWith("/");
            if (absolutePath) {
                // (root + path), relative to location
                connection.fetchRepositoryRoot(this);
                path = SVNPathUtil.append(myRepositoryRoot.getPath(), path);
                if (path.equals(getLocation().getPath())) {
                    path = "";
                } else {
                    path = path.substring(myRepositoryRoot.getPath().length() + 1);
                }
            }

            StringBuffer request = DAVLocationSegmentsHandler.generateGetLocationSegmentsRequest(null, path, 
                    pegRevision, startRevision, endRevision); 
            DAVLocationSegmentsHandler davHandler = new DAVLocationSegmentsHandler(handler);
            String root = absolutePath ? myRepositoryRoot.getPath() : getLocation().getPath();
            root = SVNEncodingUtil.uriEncode(root);
            DAVBaselineInfo info = DAVUtil.getBaselineInfo(connection, this, root, pegRevision, false, 
                    false, null);            
            path = SVNPathUtil.append(info.baselineBase, info.baselinePath);
            HTTPStatus status = connection.doReport(path, request, davHandler);
            if (status.getCode() == 501) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_NOT_IMPLEMENTED, 
                        "'get-location-segments' REPORT not implemented");
                SVNErrorManager.error(err, status.getError(), SVNLogType.NETWORK);
            } else if (status.getError() != null) {
                SVNErrorManager.error(status.getError(), SVNLogType.NETWORK);
            }
            return davHandler.getTotalRevisions();
        } finally {
            closeConnection();
        }
    }

    protected String doGetRepositoryPath(String relativePath) throws SVNException {
        if (relativePath == null) {
            return "/";
        }
        if (relativePath.length() > 0 && relativePath.charAt(0) == '/') {
            return relativePath;
        }
        String fullPath = SVNPathUtil.append(getLocation().getPath(), relativePath);
        DAVConnection connection = getConnection();
        connection.fetchRepositoryRoot(this);
        String repositoryPath = fullPath.substring(myRepositoryRoot.getPath().length());
        if ("".equals(repositoryPath)) {
            return "/";
        }
        return repositoryPath;
    }

    protected Map getMergeInfoImpl(String[] paths, long revision, SVNMergeInfoInheritance inherit,
            boolean includeDescendants) throws SVNException {
        try {
            openConnection();
            return doGetMergeInfo(paths, revision, inherit, includeDescendants);            
        } finally {
            closeConnection();
        }
    }

    protected void replayRangeImpl(long startRevision, long endRevision, long lowRevision, boolean sendDeltas, 
            ISVNReplayHandler handler) throws SVNException {
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_NOT_IMPLEMENTED);
        SVNErrorManager.error(err, SVNLogType.NETWORK);
    }

    protected ISVNEditor getCommitEditorInternal(Map locks, boolean keepLocks, SVNProperties revProps, ISVNWorkspaceMediator mediator) throws SVNException {
        try {
            openConnection();
            DAVConnection connection = getConnection();

            Map translatedLocks = null;
            if (locks != null && !locks.isEmpty()) {
                translatedLocks = new SVNHashMap();
                connection.fetchRepositoryRoot(this);
                String root = myRepositoryRoot.getPath();
                root = SVNEncodingUtil.uriEncode(root);
                for (Iterator paths = locks.keySet().iterator(); paths.hasNext();) {
                    String path = (String) paths.next();
                    String lock = (String) locks.get(path);
    
                    if (path.startsWith("/")) {
                        path = SVNPathUtil.append(root, SVNEncodingUtil.uriEncode(path));
                    } else {
                        path = doGetFullPath(path);
                        path = SVNEncodingUtil.uriEncode(path);
                    }
                    translatedLocks.put(path, lock);
                }
            }
            connection.setLocks(translatedLocks, keepLocks);
            return new DAVCommitEditor(this, connection, revProps, mediator, new Runnable() {
                public void run() {
                    closeConnection();
                }
            });
        } catch (Throwable th) {
            closeConnection();
            if (th instanceof SVNException) {
                throw (SVNException) th;
            } 
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "can not get commit editor: ''{0}''", th.getLocalizedMessage());
            SVNErrorManager.error(err, th, SVNLogType.NETWORK);
            return null;
        }
    }

    protected long getDeletedRevisionImpl(String path, long pegRevision, long endRevision) throws SVNException {
        try {
            openConnection();
            DAVConnection connection = getConnection();
            String thisSessionPath = doGetFullPath("");
            thisSessionPath = SVNEncodingUtil.uriEncode(thisSessionPath);
            
            DAVBaselineInfo info = DAVUtil.getBaselineInfo(connection, this, thisSessionPath, pegRevision, false, false, null);
            String finalBCPath = SVNPathUtil.append(info.baselineBase, info.baselinePath);
            StringBuffer requestBody = DAVDeletedRevisionHandler.generateGetDeletedRevisionRequest(null, path, pegRevision, endRevision);
            DAVDeletedRevisionHandler handler = new DAVDeletedRevisionHandler();
            HTTPStatus status = connection.doReport(finalBCPath, requestBody, handler);
            if (status.getCode() == 501) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_NOT_IMPLEMENTED, "'get-deleted-rev' REPORT not implemented");
                SVNErrorManager.error(err, status.getError(), SVNLogType.NETWORK);
            }
            return handler.getRevision();
        } finally {
            closeConnection();
        }
    }

    protected DAVConnection getConnection() {
        return myConnection;
    }

    protected IHTTPConnectionFactory getConnectionFactory() {
        return myConnectionFactory;
    }

    private Map doGetMergeInfo(String[] paths, long revision, SVNMergeInfoInheritance inherit, boolean includeDescendants) throws SVNException {
        String path = doGetFullPath("");
        path = SVNEncodingUtil.uriEncode(path);
        DAVConnection connection = getConnection();
        DAVBaselineInfo info = DAVUtil.getBaselineInfo(connection, this, path, revision, false, true, null);
        path = SVNPathUtil.append(info.baselineBase, info.baselinePath);
        
        if (paths == null || paths.length == 0) {
            paths = new String[]{""};
        }
        String[] repositoryPaths = new String[paths.length];
        for (int i = 0; i < paths.length; i++) {
            repositoryPaths[i] = paths[i];
        }
        StringBuffer request = DAVMergeInfoHandler.generateMergeInfoRequest(null, revision, repositoryPaths, inherit, includeDescendants);
        DAVMergeInfoHandler handler = new DAVMergeInfoHandler();
        HTTPStatus status = connection.doReport(path, request, handler);
        if (status.getCode() == 501) {
	        SVNErrorMessage err = status.getError() != null ? status.getError() : SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "Server does not support mergeinfo");
	        SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        if (status.getError() != null) {
            SVNErrorManager.error(status.getError(), SVNLogType.NETWORK);
        }
        Map mergeInfo = handler.getMergeInfo();
        if (mergeInfo == null) {
	        return null;
        }
	    Map mergeInfoWithPath = new HashMap();
        for (Iterator items = mergeInfo.entrySet().iterator(); items.hasNext();) {
            Map.Entry item = (Map.Entry) items.next();
            SVNMergeInfo value = (SVNMergeInfo) item.getValue();
            if (value != null) {
                String repositoryPath = (String) item.getKey();
                if (repositoryPath.startsWith("/")) {
                    repositoryPath = repositoryPath.substring("/".length());
                }
                repositoryPath = doGetRepositoryPath(repositoryPath);
                mergeInfoWithPath.put(repositoryPath, new SVNMergeInfo(repositoryPath, value.getMergeSourcesToMergeLists()));
            }
        }
        return mergeInfoWithPath;
    }
    
    private void runReport(SVNURL url, long targetRevision, String target, String dstPath, SVNDepth depth, 
            boolean ignoreAncestry, boolean resourceWalk, boolean fetchContents, boolean sendCopyFromArgs, 
            boolean sendAll, boolean closeEditorOnException, boolean spool, ISVNReporterBaton reporter, ISVNEditor editor) throws SVNException {
        boolean serverSupportsDepth = hasCapability(SVNCapability.DEPTH);
        if (depth != SVNDepth.FILES && depth != SVNDepth.INFINITY && !serverSupportsDepth) {
            editor = SVNDepthFilterEditor.getDepthFilterEditor(depth, editor, target != null);
        }
        
        DAVEditorHandler handler = null;
        try {
            openConnection();
            DAVConnection connection = getConnection();

            Map lockTokens = new SVNHashMap();
            StringBuffer request = DAVEditorHandler.generateEditorRequest(connection, null, 
                    url.toString(), targetRevision, target, dstPath, depth, lockTokens, ignoreAncestry, 
                    resourceWalk, fetchContents, sendCopyFromArgs, sendAll, reporter);
            handler = new DAVEditorHandler(myConnectionFactory, this, editor, lockTokens, fetchContents, 
                    target != null && !"".equals(target));
            String bcPath = SVNEncodingUtil.uriEncode(getLocation().getPath());
            try {
                bcPath = DAVUtil.getVCCPath(connection, this, bcPath);
            } catch (SVNException e) {
                if (closeEditorOnException) {
                    editor.closeEdit();
                }
                throw e;
            }
            HTTPStatus status = connection.doReport(bcPath, request, handler, spool);
            if (status.getError() != null) {
                SVNErrorManager.error(status.getError(), SVNLogType.NETWORK);
            }
        } finally {
            if (handler != null) {
                handler.closeConnection();
            }
            closeConnection();
        }
    }

    private SVNDirEntry createDirEntry(String fullPath, DAVProperties child) throws SVNException {
        String href = child.getURL();
        href = SVNEncodingUtil.uriDecode(href);
        // build direntry
        SVNNodeKind kind = SVNNodeKind.FILE;
        Object revisionStr = child.getPropertyValue(DAVElement.VERSION_NAME);
        long lastRevision = -1;
        if (revisionStr != null) {
            try {
                lastRevision = Long.parseLong(revisionStr.toString());
            } catch (NumberFormatException nfe) {
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, nfe), SVNLogType.NETWORK);
            }
        }
        SVNPropertyValue sizeValue = child.getPropertyValue(DAVElement.GET_CONTENT_LENGTH);
        
        long size = 0;
        if (sizeValue != null) {
            try {
                size = Long.parseLong(sizeValue.getString());
            } catch (NumberFormatException nfe) {
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, nfe), SVNLogType.NETWORK);
            }
        }
        if (child.isCollection()) {
            kind = SVNNodeKind.DIR;
        }
        SVNPropertyValue authorValue = child.getPropertyValue(DAVElement.CREATOR_DISPLAY_NAME);
        String author = authorValue == null ? null : authorValue.getString();
        SVNPropertyValue dateValue = child.getPropertyValue(DAVElement.CREATION_DATE);
        Date date = dateValue != null ? SVNDate.parseDate(dateValue.getString()) : null;
        boolean hasProperties = false;
        for (Iterator props = child.getProperties().keySet().iterator(); props.hasNext();) {
            DAVElement property = (DAVElement) props.next();
            if (DAVElement.SVN_CUSTOM_PROPERTY_NAMESPACE.equals(property.getNamespace()) ||
                    DAVElement.SVN_SVN_PROPERTY_NAMESPACE.equals(property.getNamespace())) {
                hasProperties = true;
                break;
            }
        }
        DAVConnection connection = getConnection();
        connection.fetchRepositoryRoot(this);            
        SVNURL repositoryRoot = getRepositoryRoot(false);
        SVNURL url = getLocation().setPath(fullPath, true);
        String name = repositoryRoot.equals(url) ? "" : SVNPathUtil.tail(href);
        return new SVNDirEntry(url, repositoryRoot, name, kind, size, hasProperties, lastRevision, date, author);
    }

}

