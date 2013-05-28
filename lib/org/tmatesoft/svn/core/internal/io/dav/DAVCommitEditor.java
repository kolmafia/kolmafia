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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNRevisionProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.handlers.DAVMergeHandler;
import org.tmatesoft.svn.core.internal.io.dav.handlers.DAVProppatchHandler;
import org.tmatesoft.svn.core.internal.io.dav.http.HTTPBodyInputStream;
import org.tmatesoft.svn.core.internal.io.dav.http.HTTPHeader;
import org.tmatesoft.svn.core.internal.io.dav.http.HTTPStatus;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.ISVNWorkspaceMediator;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
class DAVCommitEditor implements ISVNEditor {
    
//    private String myLogMessage;
    private DAVConnection myConnection;
    private SVNURL myLocation;
	private DAVRepository myRepository;
    private Runnable myCloseCallback;
    private String myActivity;

    private Stack myDirsStack;
    private ISVNWorkspaceMediator myCommitMediator;
    private Map myPathsMap;
    private Map myFilesMap;
    private String myBaseChecksum;
    private SVNProperties myRevProps;
    
    public DAVCommitEditor(DAVRepository repository, DAVConnection connection, String message, ISVNWorkspaceMediator mediator, Runnable closeCallback) {
        this(repository, connection, (SVNProperties) null, mediator, closeCallback);
        myRevProps = new SVNProperties();
        if (message != null) {
            myRevProps.put(SVNRevisionProperty.LOG, message);
        }
    }

    public DAVCommitEditor(DAVRepository repository, DAVConnection connection, SVNProperties revProps, ISVNWorkspaceMediator mediator, Runnable closeCallback) {
        myConnection = connection;
        myLocation = repository.getLocation();
        myRepository = repository;
        myCloseCallback = closeCallback;
        myCommitMediator = mediator;
        myDirsStack = new Stack();
        myPathsMap = new SVNHashMap();
        myFilesMap = new SVNHashMap();
        myRevProps = revProps != null ? revProps : new SVNProperties();
    }

    /* do nothing */
    public void targetRevision(long revision) throws SVNException {
    }
    public void absentDir(String path) throws SVNException {
    }
    public void absentFile(String path) throws SVNException {
    }

    public void openRoot(long revision) throws SVNException {
        // make activity
        myActivity = createActivity();
        DAVResource root = new DAVResource(myCommitMediator, myConnection, "", revision);
        root.fetchVersionURL(null, false);
        myDirsStack.push(root);
        myPathsMap.put(root.getURL(), root.getPath());
    }

    public void deleteEntry(String path, long revision) throws SVNException {
        path = SVNEncodingUtil.uriEncode(path);
        // get parent's working copy. (checkout? or use checked out?)
        DAVResource parentResource = (DAVResource) myDirsStack.peek();
        checkoutResource(parentResource, true);
        String wPath = parentResource.getWorkingURL();
		// get root wURL and delete from it!

        // append name part of the path to the checked out location
		// should we append full name here?
        String url;
		if (myDirsStack.size() == 1) {
			wPath = SVNPathUtil.append(parentResource.getWorkingURL(), path);
            url = SVNPathUtil.append(parentResource.getURL(), path);
		} else {
			// we are inside openDir()...
			wPath = SVNPathUtil.append(wPath, SVNPathUtil.tail(path));
            url = SVNPathUtil.append(parentResource.getURL(), SVNPathUtil.tail(path));
		}
        // call DELETE for the composed path
        myConnection.doDelete(url, wPath, revision);
		if (myDirsStack.size() == 1) {
			myPathsMap.put(SVNPathUtil.append(parentResource.getURL(), path), path);
		} else {
			myPathsMap.put(SVNPathUtil.append(parentResource.getURL(), SVNPathUtil.tail(path)), path);
		}
    }


    public void addDir(String path, String copyPath, long copyRevision) throws SVNException {
        path = SVNEncodingUtil.uriEncode(path);

        DAVResource parentResource = (DAVResource) myDirsStack.peek();
        checkoutResource(parentResource, true);
        String wPath = parentResource.getWorkingURL();

        DAVResource newDir = new DAVResource(myCommitMediator, myConnection, path, -1, copyPath != null);
        newDir.setWorkingURL(SVNPathUtil.append(wPath, SVNPathUtil.tail(path)));
        newDir.setAdded(true);

        myDirsStack.push(newDir);
        myPathsMap.put(newDir.getURL(), path);
        if (copyPath != null) {
            // convert to full path?
            copyPath = myRepository.doGetFullPath(copyPath);
            copyPath = SVNEncodingUtil.uriEncode(copyPath);
            DAVBaselineInfo info = DAVUtil.getBaselineInfo(myConnection, myRepository, copyPath, copyRevision, false, false, null);
            copyPath = SVNPathUtil.append(info.baselineBase, info.baselinePath);

            // full url.
            wPath = myLocation.setPath(newDir.getWorkingURL(), true).toString();
            myConnection.doCopy(copyPath, wPath, 1);
        } else {
            try {
                myConnection.doMakeCollection(newDir.getWorkingURL());
            } catch (SVNException e) {
                // check if dir already exists.
                if (!e.getErrorMessage().getErrorCode().isAuthentication() && 
                        e.getErrorMessage().getErrorCode() != SVNErrorCode.CANCELLED) {
                    SVNErrorMessage err = null;
                    try {
                        DAVBaselineInfo info = DAVUtil.getBaselineInfo(myConnection, myRepository, newDir.getURL(), -1, false, false, null);
                        if (info != null) {
                            err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_ALREADY_EXISTS, "Path ''{0}'' already exists", newDir.getURL());
                        }
                    } catch (SVNException inner) {
                    }
                    if (err != null) {
                        SVNErrorManager.error(err, SVNLogType.NETWORK);
                    }
                }
                throw e;
            }
        }
    }

    public void openDir(String path, long revision) throws SVNException {
        path = SVNEncodingUtil.uriEncode(path);
        // do nothing,
        DAVResource parent = myDirsStack.peek() != null ? (DAVResource) myDirsStack.peek() : null;
        DAVResource directory = new DAVResource(myCommitMediator, myConnection, path, revision, parent != null && parent.isCopy());
        if (parent != null && parent.getVersionURL() == null) {
            // part of copied structure -> derive wurl
            directory.setWorkingURL(SVNPathUtil.append(parent.getWorkingURL(), SVNPathUtil.tail(path)));
        } else {
            directory.fetchVersionURL(parent, false);
        }
        myDirsStack.push(directory);
        myPathsMap.put(directory.getURL(), directory.getPath());
    }

    public void changeDirProperty(String name, SVNPropertyValue value) throws SVNException {
        DAVResource directory = (DAVResource) myDirsStack.peek();
        checkoutResource(directory, true);
        directory.putProperty(name, value);
        myPathsMap.put(directory.getURL(), directory.getPath());
    }

    public void closeDir() throws SVNException {
        DAVResource resource = (DAVResource) myDirsStack.pop();
        // do proppatch if there were property changes.
        if (resource.getProperties() != null) {
            StringBuffer request = DAVProppatchHandler.generatePropertyRequest(null, resource.getProperties());
            myConnection.doProppatch(resource.getURL(), resource.getWorkingURL(), request, null, null);
        }
        resource.dispose();
    }

    public void addFile(String path, String copyPath, long copyRevision) throws SVNException {
        String originalPath = path;
        path = SVNEncodingUtil.uriEncode(path);
        // checkout parent collection.
        DAVResource parentResource = (DAVResource) myDirsStack.peek();
        checkoutResource(parentResource, true);
        String wPath = parentResource.getWorkingURL();
        // create child resource.
        DAVResource newFile = new DAVResource(myCommitMediator, myConnection, path, -1, copyPath != null);
        newFile.setWorkingURL(SVNPathUtil.append(wPath, SVNPathUtil.tail(path)));

        if (!parentResource.isAdded() && !myPathsMap.containsKey(newFile.getURL())) {
        	String filePath = SVNPathUtil.append(parentResource.getURL(), SVNPathUtil.tail(path));
            SVNErrorMessage err1 = null;
            SVNErrorMessage err2 = null;
            try {
                DAVUtil.getResourceProperties(myConnection, filePath, null, DAVElement.STARTING_PROPERTIES);
            } catch (SVNException e) {
                if (e.getErrorMessage() == null) {
                    throw e;
                }
                err1 = e.getErrorMessage();
            }
            try {
                DAVUtil.getResourceProperties(myConnection, newFile.getWorkingURL(), null, DAVElement.STARTING_PROPERTIES);
            } catch (SVNException e) {
                if (e.getErrorMessage() == null) {
                    throw e;
                }
                err2 = e.getErrorMessage();
            }
            if (err1 == null && err2 == null) {
                err1 = SVNErrorMessage.create(SVNErrorCode.RA_DAV_ALREADY_EXISTS, "File ''{0}'' already exists", filePath);
                SVNErrorManager.error(err1, SVNLogType.NETWORK);
            } else if ((err1 != null && err1.getErrorCode() == SVNErrorCode.FS_NOT_FOUND) ||
                    (err2 != null && err2.getErrorCode() == SVNErrorCode.FS_NOT_FOUND)) {
                // skip
            }  else {
                SVNErrorManager.error(err1, err2, SVNLogType.NETWORK);
            }
        }
        // put to have working URL to make PUT or PROPPATCH later (in closeFile())
        myPathsMap.put(newFile.getURL(), newFile.getPath());
        myFilesMap.put(originalPath, newFile);

        newFile.setAdded(true);
        if (copyPath != null) {
            copyPath = myRepository.doGetFullPath(copyPath);
            copyPath = SVNEncodingUtil.uriEncode(copyPath);
            DAVBaselineInfo info = DAVUtil.getBaselineInfo(myConnection, myRepository, copyPath, copyRevision, false, false, null);
            copyPath = SVNPathUtil.append(info.baselineBase, info.baselinePath);

            // do "COPY" copyPath to parents working url ?
            wPath = myLocation.setPath(newFile.getWorkingURL(), true).toString();
            myConnection.doCopy(copyPath, wPath, 0);
        } 
    }

    public void openFile(String path, long revision) throws SVNException {
        String originalPath = path;
        path = SVNEncodingUtil.uriEncode(path);
        DAVResource file = new DAVResource(myCommitMediator, myConnection, path, revision);
        DAVResource parent = (DAVResource) myDirsStack.peek();
        if (parent.getVersionURL() == null) {
            // part of copied structure -> derive wurl
            file.setWorkingURL(SVNPathUtil.append(parent.getWorkingURL(), SVNPathUtil.tail(path)));
        } else {
            file.fetchVersionURL(parent, false);
        }
        checkoutResource(file, true);
        myPathsMap.put(file.getURL(), file.getPath());
        myFilesMap.put(originalPath, file);
    }
    
    public void applyTextDelta(String path, String baseChecksum) throws SVNException {
        myCurrentDelta = null;
        myIsFirstWindow = true;
        myDeltaFile = null;
        myBaseChecksum = baseChecksum;
    }
    
    private OutputStream myCurrentDelta = null;
    private File myDeltaFile;
    private boolean myIsAborted;
    private boolean myIsFirstWindow;
    
    public OutputStream textDeltaChunk(String path, SVNDiffWindow diffWindow) throws SVNException {
        // save window, create temp file.
        try {
            if (myCurrentDelta == null) {
                myDeltaFile = SVNFileUtil.createTempFile("svnkit", ".tmp");
                myCurrentDelta = SVNFileUtil.openFileForWriting(myDeltaFile);
            }
            diffWindow.writeTo(myCurrentDelta, myIsFirstWindow);
            myIsFirstWindow = false;
            return SVNFileUtil.DUMMY_OUT;
        } catch (IOException e) {
            SVNFileUtil.closeFile(myCurrentDelta);
            SVNFileUtil.deleteFile(myDeltaFile);
            myDeltaFile = null;
            myCurrentDelta = null;
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e.getMessage());
            SVNErrorManager.error(err, e, SVNLogType.NETWORK);
            return null;
        }
    }
    public void textDeltaEnd(String path) throws SVNException {
        SVNFileUtil.closeFile(myCurrentDelta);
    }

    public void changeFileProperty(String path, String name, SVNPropertyValue value)  throws SVNException {
        DAVResource currentFile = (DAVResource) myFilesMap.get(path);
        currentFile.putProperty(name, value);
    }

    public void closeFile(String path, String textChecksum) throws SVNException {
        // do PUT of delta if there was one (diff window + temp file).
        // do subsequent PUT of all diff windows...
        DAVResource currentFile = (DAVResource) myFilesMap.get(path);
        try {
            if (myDeltaFile != null) {
                InputStream combinedData = null;
                try {
                    combinedData = new HTTPBodyInputStream(myDeltaFile);
                    myConnection.doPutDiff(currentFile.getURL(), currentFile.getWorkingURL(), combinedData, myDeltaFile.length(),
                            myBaseChecksum, textChecksum);

                } catch (SVNException e) {
                    HTTPStatus httpStatus = myConnection.getLastStatus();
                    if (e.getErrorMessage().getErrorCode() == SVNErrorCode.RA_DAV_REQUEST_FAILED && httpStatus != null) {
                        switch (httpStatus.getCode()) {
                            case 423:
                                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.RA_NOT_LOCKED,
                                        "No lock on path ''{0}'' (Status {0} on PUT Request)",
                                        new Object[]{currentFile.getWorkingURL(), httpStatus.getCode()});
                                SVNErrorManager.error(errorMessage, e, SVNLogType.CLIENT);
                            default:
                                throw e;
                        }

                    } else {
                        throw e;
                    }
                } finally {
                    SVNFileUtil.closeFile(combinedData);
                    SVNFileUtil.deleteFile(myDeltaFile);
                    myDeltaFile = null;
                }
            }
            // do proppatch if there were property changes.
            if (currentFile.getProperties() != null) {
                StringBuffer request = DAVProppatchHandler.generatePropertyRequest(null, currentFile.getProperties());
                myConnection.doProppatch(currentFile.getURL(), currentFile.getWorkingURL(), request, null, null);
            }
        } finally {
            currentFile.dispose();
            myCurrentDelta = null;
            myBaseChecksum = null;
            myFilesMap.remove(path);
        }
    }
    
    public SVNCommitInfo closeEdit() throws SVNException {
	    try {
		    if (!myDirsStack.isEmpty()) {
		        DAVResource resource = (DAVResource) myDirsStack.pop();
		        // do proppatch if there were property changes.
		        if (resource.getProperties() != null) {
		            StringBuffer request = DAVProppatchHandler.generatePropertyRequest(null, resource.getProperties());
		            myConnection.doProppatch(resource.getURL(), resource.getWorkingURL(), request, null, null);
		        }
		        resource.dispose();
		    }
		    DAVMergeHandler handler = new DAVMergeHandler(myCommitMediator, myPathsMap);
		    HTTPStatus status = myConnection.doMerge(myActivity, true, handler);
		    if (status.getError() != null) {
                // DELETE shouldn't be called anymore if there is an error or MERGE.
                // calling abortEdit will do nothing on closeEdit failure now.
                myIsAborted = true;
		        SVNErrorManager.error(status.getError(), SVNLogType.NETWORK);
		    }
		    return handler.getCommitInfo();
	    }
	    finally {
            // we should run abort edit if exception is thrown
            // abort edit will not be run if there was an error (from server side) on MERGE.
            try {
                abortEdit();
            } catch (SVNException e) {
                SVNDebugLog.getDefaultLog().logError(SVNLogType.DEFAULT, e);
            }

            // always run close callback to 'unlock' SVNRepository.
            runCloseCallback();            
	    }
    }
    
    public void abortEdit() throws SVNException {
        if (myIsAborted) {
            return;
        }
        myIsAborted = true;
	    try {
		    try {
			    // DELETE activity
			    if (myActivity != null) {
			        myConnection.doDelete(myActivity);
			    }
		    }
		    finally {
			    // dispose all resources!
			    if (myFilesMap != null) {
			        for (Iterator files = myFilesMap.values().iterator(); files.hasNext();) {
			            DAVResource file = (DAVResource) files.next();
			            file.dispose();
			        }
			        myFilesMap = null;
			    }
			    for(Iterator files = myDirsStack.iterator(); files.hasNext();) {
			        DAVResource resource = (DAVResource) files.next();
			        resource.dispose();
			    }
			    myDirsStack = null;
		    }
	    }
	    finally {
            runCloseCallback();
	    }
    }
    
    private void runCloseCallback() {
        if (myCloseCallback != null) {
            myCloseCallback.run();
            myCloseCallback = null;
        }
    }
    
    private String createActivity() throws SVNException {
        String activity = myConnection.doMakeActivity(myCommitMediator);
        // checkout head...
        String path = SVNEncodingUtil.uriEncode(myLocation.getPath());
        String vcc = DAVUtil.getPropertyValue(myConnection, path, null, DAVElement.VERSION_CONTROLLED_CONFIGURATION);
        
        HTTPStatus status = null;
        for (int i = 0; i < 5; i++) {
            String head = DAVUtil.getPropertyValue(myConnection, vcc, null, DAVElement.CHECKED_IN);
            try {
                status = myConnection.doCheckout(activity, null, head, false);
                break;
            } catch (SVNException svne) {
                if (svne.getErrorMessage().getErrorCode() != SVNErrorCode.APMOD_BAD_BASELINE || i == 4) {
                    throw svne;
                }
            }
        }
        String location = status.getHeader().getFirstHeaderValue(HTTPHeader.LOCATION_HEADER);
        if (location == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, "The CHECKOUT response did not contain a 'Location:' header");
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        if (myRevProps != null && myRevProps.size() > 0) {
            StringBuffer request = DAVProppatchHandler.generatePropertyRequest(null, myRevProps);
            SVNErrorMessage context = null;//SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, "applying log message to {0}", path);
            try {
                myConnection.doProppatch(null, location, request, null, context);
            } catch (SVNException e) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, "applying log message to {0}", path);
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }
        }
        return activity;
    }
    
    private void checkoutResource(DAVResource resource, boolean allow404) throws SVNException {
        if (resource.getWorkingURL() != null) {
            return;
        }
        HTTPStatus status = null;
        try {
            status = myConnection.doCheckout(myActivity, resource.getURL(), resource.getVersionURL(), allow404);
            if (allow404 && status.getCode() == 404) {
                resource.fetchVersionURL(null, true);
                status = myConnection.doCheckout(myActivity, resource.getURL(), resource.getVersionURL(), false);
            }
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.FS_CONFLICT) {
                String path = resource.getPath();
                if ("".equals(path)) {
                    path = resource.getURL();
                }
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CONFLICT, "File or directory ''{0}'' is out of date; try updating", path);
                SVNErrorManager.error(err, e.getErrorMessage(), SVNLogType.NETWORK);
            }
            throw e;
        }
        String location = status != null ? status.getHeader().getFirstHeaderValue(HTTPHeader.LOCATION_HEADER) : null;
        if (location == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, "The CHECKOUT response did not contain a 'Location:' header");
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        resource.setWorkingURL(location);
    }
    
}
