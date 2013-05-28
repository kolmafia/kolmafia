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

package org.tmatesoft.svn.core.internal.io.dav.handlers;

import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLock;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVBaselineInfo;
import org.tmatesoft.svn.core.internal.io.dav.DAVConnection;
import org.tmatesoft.svn.core.internal.io.dav.DAVElement;
import org.tmatesoft.svn.core.internal.io.dav.DAVProperties;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepository;
import org.tmatesoft.svn.core.internal.io.dav.DAVUtil;
import org.tmatesoft.svn.core.internal.io.dav.http.IHTTPConnectionFactory;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.util.SVNXMLUtil;
import org.tmatesoft.svn.core.internal.wc.IOExceptionWrapper;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.ISVNDeltaConsumer;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.ISVNReporter;
import org.tmatesoft.svn.core.io.ISVNReporterBaton;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;
import org.tmatesoft.svn.util.SVNLogType;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;


/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class DAVEditorHandler extends BasicDAVDeltaHandler {
    
    public static final String PLACEHOLDER_PROPERTY_NAME = SVNProperty.SVN_PREFIX + "BOGOSITY";

    public static StringBuffer generateEditorRequest(final DAVConnection connection, StringBuffer xmlBuffer, 
            String url, long targetRevision, String target, String dstPath, SVNDepth depth, 
            final Map lockTokens, boolean ignoreAncestry, boolean resourceWalk, boolean fetchContents, 
            boolean sendCopyFromArgs, boolean sendAll, ISVNReporterBaton reporterBaton) throws SVNException {
        xmlBuffer = SVNXMLUtil.addXMLHeader(xmlBuffer);
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        
        Map attrs = new SVNHashMap();
        attrs.put("send-all", Boolean.toString(sendAll));
        SVNXMLUtil.openNamespaceDeclarationTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "update-report", 
                SVN_NAMESPACES_LIST, SVNXMLUtil.PREFIX_MAP, attrs, xmlBuffer, true);
        
        SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "src-path", url, xmlBuffer);
        if (targetRevision >= 0) {
            SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "target-revision", String.valueOf(targetRevision), xmlBuffer);
        }
        if (target != null && !"".equals(target)) {
            SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "update-target", target, xmlBuffer);
        }
        if (dstPath != null) {
            SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "dst-path", dstPath, xmlBuffer);
        }
        if (depth == SVNDepth.FILES || depth == SVNDepth.EMPTY) {
            SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "recursive", "no", xmlBuffer);
        }
        SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "depth", SVNDepth.asString(depth), xmlBuffer);

        if (ignoreAncestry) {
            SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "ignore-ancestry", "yes", xmlBuffer);
        }
        if (sendCopyFromArgs) {
            SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "send-copyfrom-args", "yes", xmlBuffer);
        }
        if (resourceWalk) {
            SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "resource-walk", "yes", xmlBuffer);
        }
        if (sendAll && !fetchContents) {
            SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "text-deltas", "no", xmlBuffer);
        }
        
        final StringBuffer report = xmlBuffer;
        
        reporterBaton.report(new ISVNReporter() {
            public void setPath(String path, String lockToken, long revision, boolean startEmpty) throws SVNException {
                setPath(path, lockToken, revision, SVNDepth.INFINITY, startEmpty);
            }

            public void deletePath(String path) {
                SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "missing", path, report);
            }

            public void linkPath(SVNURL url, String path, String lockToken, long revision, boolean startEmpty) throws SVNException {
                linkPath(url, path, lockToken, revision, SVNDepth.INFINITY, startEmpty);
            }

            public void finishReport() {
            }

            public void linkPath(SVNURL url, String path, String lockToken, long revision, SVNDepth depth, boolean startEmpty) throws SVNException {
                Map attrs = new SVNHashMap();
                attrs.put(REVISION_ATTR, String.valueOf(revision));
                attrs.put("depth", SVNDepth.asString(depth));
                if (lockToken != null) {
                    if (lockTokens != null) {
                        lockTokens.put(path, lockToken);
                    }
                    attrs.put("lock-token", lockToken);
                }
                if (startEmpty) {
                    attrs.put("start-empty", Boolean.TRUE.toString());
                }
                String linkedPath = url.getURIEncodedPath();
                DAVBaselineInfo info = DAVUtil.getBaselineInfo(connection, null, linkedPath, revision, false, false, null);
                linkedPath = SVNEncodingUtil.uriDecode(info.baselinePath); 
                
                if (!linkedPath.startsWith("/")) {
                    linkedPath = "/" + linkedPath;
                }
                
                attrs.put("linkpath", linkedPath);
                SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "entry", path, attrs, report);
            }

            public void setPath(String path, String lockToken, long revision, SVNDepth depth, boolean startEmpty) throws SVNException {
                Map attrs = new SVNHashMap();
                attrs.put(REVISION_ATTR, String.valueOf(revision));
                attrs.put("depth", SVNDepth.asString(depth));
                if (lockToken != null) {
                    if (lockTokens != null) {
                        lockTokens.put(path, lockToken);
                    }
                    attrs.put("lock-token", lockToken);
                }
                if (startEmpty) {
                    attrs.put("start-empty", Boolean.TRUE.toString());
                }
                SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "entry", path, attrs, report);
            }

            public void abortReport() throws SVNException {
            }
        });
        SVNXMLUtil.addXMLFooter(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "update-report", xmlBuffer);
        return xmlBuffer;
    }

    protected static final DAVElement TARGET_REVISION = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "target-revision");
    protected static final DAVElement UPDATE_REPORT = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "update-report");
    protected static final DAVElement RESOURCE = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "resource");
    protected static final DAVElement OPEN_DIRECTORY = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "open-directory");
    protected static final DAVElement ADD_DIRECTORY = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "add-directory");
    protected static final DAVElement ABSENT_DIRECTORY = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "absent-directory");
    protected static final DAVElement OPEN_FILE = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "open-file");
    protected static final DAVElement ADD_FILE = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "add-file");
    protected static final DAVElement ABSENT_FILE = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "absent-file");
    protected static final DAVElement DELETE_ENTRY = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "delete-entry");
    protected static final DAVElement FETCH_PROPS = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "fetch-props");
    protected static final DAVElement SET_PROP = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "set-prop");
    protected static final DAVElement REMOVE_PROP = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "remove-prop");
    protected static final DAVElement FETCH_FILE = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "fetch-file");

    protected static final String REVISION_ATTR = "rev";
    protected static final String NAME_ATTR = "name";
    protected static final String ENCODING_ATTR = "encoding";
    protected static final String COPYFROM_REV_ATTR = "copyfrom-rev";
    protected static final String COPYFROM_PATH_ATTR = "copyfrom-path";
    protected static final String SEND_ALL_ATTR = "send-all";
    protected static final String BC_URL_ATTR = "bc-url";
    protected static final String BASE_CHECKSUM_ATTR = "base-checksum";
    protected static final String PATH_ATTR = "path";

    protected ISVNEditor myEditor;
    protected String myPath;
    protected String myPropertyName;
    protected boolean myIsDirectory;
    protected Stack myDirs; 
    protected Map myLockTokens;
    protected DAVRepository myOwner;
    protected boolean myIsFetchContent;

    private String myChecksum;
    private String myEncoding;
    private ISVNDeltaConsumer myDeltaConsumer;
    private boolean myIsReceiveAll;
    private DAVConnection myConnection;
    private IHTTPConnectionFactory myConnectionFactory;
    private String myHref;
    private String myCurrentWCPath;
    private boolean myIsInResource;
    private boolean myIsFetchProps;
    private boolean myHasTarget;
    private Map myVersionURLs;

    public DAVEditorHandler(IHTTPConnectionFactory connectionFactory, DAVRepository owner, ISVNEditor editor, 
            Map lockTokens, boolean fetchContent, boolean hasTarget) {
        myConnectionFactory = connectionFactory;
        myOwner = owner;
        myEditor = editor;
        myIsFetchContent = fetchContent;
        myHasTarget = hasTarget;
        myLockTokens = lockTokens;
        
        myDeltaConsumer = fetchContent ? (ISVNDeltaConsumer) editor : new ISVNDeltaConsumer() {
            public void applyTextDelta(String path, String baseChecksum) throws SVNException {
                myEditor.applyTextDelta(path, baseChecksum);
            }

            public OutputStream textDeltaChunk(String path, SVNDiffWindow diffWindow) throws SVNException {
                return null;
            }

            public void textDeltaEnd(String path) throws SVNException {
                myEditor.textDeltaEnd(path);
            }
        };
        init();
        myDirs = new Stack();
        myVersionURLs = new SVNHashMap();
    }

    public void closeConnection() {
        if (myConnection != null) {
            myConnection.close();
            myConnection = null;
        }
    }
    
    protected void startElement(DAVElement parent, DAVElement element, Attributes attrs) throws SVNException {
        if (element == UPDATE_REPORT) {
            String receiveAll = attrs.getValue(SEND_ALL_ATTR);
            if (receiveAll != null && Boolean.valueOf(receiveAll).booleanValue()) {
                myIsReceiveAll = true;
            }
        } else if (element == TARGET_REVISION) {
            long revision = -1;
            try {
                revision = Long.parseLong(attrs.getValue(REVISION_ATTR));
            } catch (NumberFormatException nfe) {
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, nfe), SVNLogType.NETWORK);
            }
            myEditor.targetRevision(revision);
        } else if (element == ABSENT_DIRECTORY) {
            String name = attrs.getValue(NAME_ATTR);
            myEditor.absentDir(SVNPathUtil.append(myPath, name));
        } else if (element == ABSENT_FILE) {
            String name = attrs.getValue(NAME_ATTR);
            myEditor.absentFile(SVNPathUtil.append(myPath, name));
        } else if (element == OPEN_DIRECTORY) {
            String revAttr = attrs.getValue(REVISION_ATTR);
            if (revAttr == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, 
                        "Missing rev attr in open-directory element");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }
            
            long revision = -1;
            try {
                revision = Long.parseLong(revAttr);
            } catch (NumberFormatException nfe) {
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, nfe), SVNLogType.NETWORK);
            }
            myIsDirectory = true;
            if (myPath == null) {
                myPath = "";
                myEditor.openRoot(revision);
            } else {
                String name = attrs.getValue(NAME_ATTR);
                if (name == null) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, 
                            "Missing name attr in open-directory element");
                    SVNErrorManager.error(err, SVNLogType.NETWORK);
                }
                myPath = SVNPathUtil.append(myPath, name);
                myEditor.openDir(myPath, revision);
            }
            
            DirInfo dirInfo = new DirInfo();
            myDirs.push(dirInfo);
        } else if (element == ADD_DIRECTORY) {
            myIsDirectory = true;
            String name = attrs.getValue(NAME_ATTR);
            if (name == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, 
                        "Missing name attr in add-directory element");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }
            String copyFromPath = attrs.getValue(COPYFROM_PATH_ATTR);
            long copyFromRev = -1;
            if (copyFromPath != null) {
                String copyFromRevString = attrs.getValue(COPYFROM_REV_ATTR);
                if (copyFromRevString == null) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, 
                            "Missing copyfrom-rev attr in add-directory element");
                    SVNErrorManager.error(err, SVNLogType.NETWORK);
                }
                try {
                    copyFromRev = Long.parseLong(copyFromRevString);
                } catch (NumberFormatException nfe) {
                    SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, nfe), SVNLogType.NETWORK);
                }
            }
            myPath = SVNPathUtil.append(myPath, name);
            myEditor.addDir(myPath, copyFromPath, copyFromRev);
            
            DirInfo dirInfo = new DirInfo(); 
            myDirs.push(dirInfo);
            
            dirInfo.myIsFetchProps = true;
            
            String bcURL = attrs.getValue(BC_URL_ATTR);
            if (!myIsReceiveAll && bcURL != null) {
                DAVElement[] elements = null;
                Map propsMap = new SVNHashMap();
                DAVUtil.getProperties(getConnection(), DAVUtil.getPathFromURL(bcURL), 1, null, elements, propsMap);
                
                if (!propsMap.isEmpty()) {
                    dirInfo.myChildren = new SVNHashMap();
                    for (Iterator propsIter = propsMap.values().iterator(); propsIter.hasNext();) {
                        DAVProperties props = (DAVProperties) propsIter.next();
                        SVNPropertyValue vcURL = props.getPropertyValue(DAVElement.CHECKED_IN);
                        if (vcURL != null) {
                            dirInfo.myChildren.put(SVNPropertyValue.getPropertyAsString(vcURL), props);
                        }
                    }
                }
            }
        } else if (element == OPEN_FILE) {
            myIsDirectory = false;
            String revAttr = attrs.getValue(REVISION_ATTR);
            if (revAttr == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, 
                        "Missing rev attr in open-file element");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }
            long revision = -1;
            try {
                revision = Long.parseLong(revAttr);
            } catch (NumberFormatException nfe) {
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, nfe), SVNLogType.NETWORK);
            }
            String name = attrs.getValue(NAME_ATTR);
            if (name == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, 
                        "Missing name attr in open-file element");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }
            myPath = SVNPathUtil.append(myPath, name);
            myEditor.openFile(myPath, revision);
            myIsFetchProps = false;
        } else if (element == ADD_FILE) {
            myIsDirectory = false;
            String name = attrs.getValue(NAME_ATTR);
            if (name == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, 
                        "Missing name attr in add-file element");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }
            myPath = SVNPathUtil.append(myPath, name);
            String copyFromPath = attrs.getValue(COPYFROM_PATH_ATTR);
            long copyFromRev = -1;
            if (copyFromPath != null) {
                String copyFromRevisionAttr = attrs.getValue(COPYFROM_REV_ATTR);
                if (copyFromRevisionAttr == null) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, 
                            "Missing copyfrom-rev attr in add-file element");
                    SVNErrorManager.error(err, SVNLogType.NETWORK);
                }
                try {
                    copyFromRev = Long.parseLong(copyFromRevisionAttr);
                } catch (NumberFormatException nfe) {
                    SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, nfe), SVNLogType.NETWORK);
                }
            }
            myEditor.addFile(myPath, copyFromPath, copyFromRev);
            myIsFetchProps = true;
        } else if (element == DELETE_ENTRY) {
            String name = attrs.getValue(NAME_ATTR);
            if (name == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, 
                        "Missing name attr in delete-entry element");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }
        
            myEditor.deleteEntry(SVNPathUtil.append(myPath, name), -1);
        } else if (element == SET_PROP) {
            myPropertyName = attrs.getValue(NAME_ATTR);
            if (myPropertyName == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, 
                        "Missing name attr in set-prop element");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }
            myEncoding = attrs.getValue(ENCODING_ATTR);
        } else if (element == REMOVE_PROP) {
            String name = attrs.getValue(NAME_ATTR);
            if (name == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, 
                        "Missing name attr in remove-prop element");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }
            if (myIsDirectory) {
                myEditor.changeDirProperty(name, null);
            } else {
                myEditor.changeFileProperty(myPath, name, null);
            }
        } else if (element == FETCH_FILE) {
            String baseChecksum = attrs.getValue(BASE_CHECKSUM_ATTR);
            myChecksum = null;
            if (!myIsReceiveAll) {
                fetchFile(baseChecksum);
            }
        } else if (element == RESOURCE) {
            myCurrentWCPath = attrs.getValue(PATH_ATTR);
            if (myCurrentWCPath == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, 
                        "Missing path attr in resource element");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }
            myIsInResource = true;
        } else if (element == FETCH_PROPS) {
            if (!myIsFetchContent) {
                if (myIsDirectory) {
                    myEditor.changeDirProperty(PLACEHOLDER_PROPERTY_NAME, null);
                } else {
                    myEditor.changeFileProperty(myPath, PLACEHOLDER_PROPERTY_NAME, null);
                }
            } else {
                if (myIsDirectory) {
                    DirInfo dirInfo = (DirInfo) myDirs.peek();
                    dirInfo.myIsFetchProps = true;
                } else {
                    myIsFetchProps = true;
                }
            }
        } else if (element == TX_DELTA) {
            String baseChecksum = attrs.getValue(BASE_CHECKSUM_ATTR);
            if (myIsReceiveAll) {
                setDeltaProcessing(true);
                myEditor.applyTextDelta(myPath, baseChecksum);
            }
        }
    }

    protected void endElement(DAVElement parent, DAVElement element, StringBuffer cdata) throws SVNException {
        if (element == RESOURCE) {
            myIsInResource = false;
        } else if (element == UPDATE_REPORT) {
            myEditor.closeEdit();
        } else if (element == OPEN_DIRECTORY || element == ADD_DIRECTORY) {
            if (myDirs.size() != 1 || !myHasTarget) {
                addNodeProperties(myPath, true);
            }
            myEditor.closeDir();
            myChecksum = null;
            myPath = SVNPathUtil.removeTail(myPath);
            myDirs.pop();
        } else if (element == ADD_FILE) {
            if (!myIsReceiveAll) {
                fetchFile(null);
                addNodeProperties(myPath, false);
            }
            myEditor.closeFile(myPath, myChecksum);
            myChecksum = null;
            myPath = SVNPathUtil.removeTail(myPath);
            myIsDirectory = true;
        } else if (element == OPEN_FILE) {
            addNodeProperties(myPath, false);
            myEditor.closeFile(myPath, myChecksum);
            myChecksum = null;
            myPath = SVNPathUtil.removeTail(myPath);
            myIsDirectory = true;
        } else if (element == DAVElement.MD5_CHECKSUM) {
            myChecksum = cdata.toString();
        } else if (element == DAVElement.CREATOR_DISPLAY_NAME ||
                element == DAVElement.VERSION_NAME ||
                element == DAVElement.CREATION_DATE) {
            DAVUtil.setSpecialWCProperties(myEditor, myIsDirectory, myPath, element, 
                    SVNPropertyValue.create(cdata.toString()));
        } else if (element == DAVElement.HREF) {
            if (myIsFetchContent) {
                myHref = cdata.toString();
                
                if (myIsInResource) {
                    myVersionURLs.put(myCurrentWCPath, myHref);
                } else if (myIsDirectory) {
                    if (myDirs.size() != 1 || !myHasTarget) {
                        try {
                            SVNPropertyValue davWCURL = DAVUtil.isUseDAVWCURL() ? SVNPropertyValue.create(myHref) : null;
                            myEditor.changeDirProperty(SVNProperty.WC_URL, davWCURL);
                        } catch (SVNCancelException ce) {
                            throw ce;
                        } catch (SVNException svne) {                        
                            SVNErrorManager.error(svne.getErrorMessage().wrap("Could not save the URL of the version resource"), SVNLogType.NETWORK);
                        }
                        DirInfo topDirInfo = (DirInfo) myDirs.peek();
                        topDirInfo.myVSNURL = myHref;
                    }
                } else {
                    try {
                        SVNPropertyValue davWCURL = DAVUtil.isUseDAVWCURL() ? SVNPropertyValue.create(myHref) : null;
                        myEditor.changeFileProperty(myPath, SVNProperty.WC_URL, davWCURL);
                    } catch (SVNCancelException ce) {
                        throw ce;
                    } catch (SVNException svne) {
                        SVNErrorManager.error(svne.getErrorMessage().wrap("Could not save the URL of the version resource"), SVNLogType.NETWORK);
                    }
                }
            }
        } else if (element == SET_PROP) {
            if (myPropertyName == null) {
                myPropertyName = computeWCPropertyName(element);
            }
            SVNPropertyValue value = createPropertyValue(null, myPropertyName, cdata, myEncoding);
            
            if (myIsDirectory) {
                myEditor.changeDirProperty(myPropertyName, value);
            } else {
                myEditor.changeFileProperty(myPath, myPropertyName, value);
            }
            
            myPropertyName = null;
            myEncoding = null;
        } else if (element == TX_DELTA) {
            if (myIsReceiveAll) {
                setDeltaProcessing(false);
            }
        }
    }

    protected String getCurrentPath() {
        return myPath;
    }

    protected ISVNDeltaConsumer getDeltaConsumer() {
        return myDeltaConsumer;
    }

    protected void fetchFile(String baseChecksum) throws SVNException {
        setDeltaProcessing(true);
        try {
            myEditor.applyTextDelta(myPath, baseChecksum);
        } catch (SVNCancelException ce) {
            throw ce;
        } catch (SVNException svne) {
            SVNErrorManager.error(svne.getErrorMessage().wrap("Could not save file"), SVNLogType.NETWORK);
        }
        
        if (myIsFetchContent) {
            SVNErrorManager.assertionFailure(myHref != null, "myHref is null", SVNLogType.NETWORK);
            String deltaBaseVersionURL = myPath != null ? (String) myVersionURLs.get(myPath) : null;
            DeltaOutputStreamWrapper osWrapper = new DeltaOutputStreamWrapper(deltaBaseVersionURL != null, myPath);
            DAVConnection connection = getConnection();
            connection.doGet(myHref, deltaBaseVersionURL, osWrapper);
        }
        setDeltaProcessing(false);
    }
    
    protected void addNodeProperties(String path, boolean isDir) throws SVNException {
        if (myIsReceiveAll) {
            return;
        }
        
        if (!myIsFetchContent) {
            return;
        }
        
        if (!isDir) {
            String lockToken = (String) myLockTokens.get(path);
            if (lockToken != null) {
                String fullPath = myOwner.doGetFullPath(path);
                fullPath = SVNEncodingUtil.uriEncode(fullPath);
                DAVConnection connection = getConnection(); 
                SVNLock lock = connection.doGetLock(fullPath, myOwner);
                
                if (!(lock != null && lock.getID() != null && lockToken.equals(lock.getID()))) {
                    myEditor.changeFileProperty(path, SVNProperty.LOCK_TOKEN, null);
                }
            }
            
            if (!myIsFetchProps) {
                return;
            }
            
            DirInfo topDirInfo = (DirInfo) myDirs.peek();
            DAVProperties props = topDirInfo.myChildren != null ? (DAVProperties ) topDirInfo.myChildren.get(myHref) 
                    : null;
            if (props == null) {
                props = DAVUtil.getResourceProperties(getConnection(), myHref, null, null);
            }
            addProps(props, false);
        } else {
            DirInfo topDirInfo = (DirInfo) myDirs.peek();
            if (!topDirInfo.myIsFetchProps) {
                return;
            }

            DAVProperties props = topDirInfo.myChildren != null ? (DAVProperties ) topDirInfo.myChildren.get(topDirInfo.myVSNURL) 
                    : null;
            if (props == null) {
                props = DAVUtil.getResourceProperties(getConnection(), topDirInfo.myVSNURL, null, null);
            }
            addProps(props, true);
        }
    }
    
    private void addProps(DAVProperties props, boolean isDir) throws SVNException {
        Map propsMap = props.getProperties();
        for (Iterator propsIter = propsMap.keySet().iterator(); propsIter.hasNext();) {
            DAVElement element = (DAVElement) propsIter.next();
            SVNPropertyValue propValue = (SVNPropertyValue) propsMap.get(element);
            String elementNamespace = element.getNamespace();
            if (elementNamespace.equals(DAVElement.SVN_CUSTOM_PROPERTY_NAMESPACE)) {
                String propName = element.getName();
                if (isDir) {
                    myEditor.changeDirProperty(propName, propValue);
                } else {
                    myEditor.changeFileProperty(myPath, propName, propValue);
                }
                continue;
            }
            
            if (elementNamespace.equals(DAVElement.SVN_SVN_PROPERTY_NAMESPACE)) {
                String propName = SVNProperty.SVN_PREFIX + element.getName();
                if (isDir) {
                    myEditor.changeDirProperty(propName, propValue);
                } else {
                    myEditor.changeFileProperty(myPath, propName, propValue);
                }
            } else {
                DAVUtil.setSpecialWCProperties(myEditor, isDir, myPath, element, propValue);
            }
        }
    }
    
    private DAVConnection getConnection() throws SVNException {
        if (myConnection == null) {
            myConnection = new DAVConnection(myConnectionFactory, myOwner);
            myConnection.setReportResponseSpooled(myOwner.isSpoolResponse());
            myConnection.open(myOwner);
        }
        return myConnection;
    }
    
    private static String computeWCPropertyName(DAVElement element) {
        return SVNProperty.SVN_ENTRY_PREFIX + element.getName();
    }
    
    private class DirInfo {
        private boolean myIsFetchProps;
        private Map myChildren;
        private String myVSNURL;
    }
    
    private class DeltaOutputStreamWrapper extends OutputStream {
        private boolean myIsDelta;
        private SVNDeltaGenerator myDeltaGenerator;
        private String myPath;
        
        public DeltaOutputStreamWrapper(boolean isDelta, String path) {
            myIsDelta = isDelta;
            myPath = path;
        }
        
        public void write(int b) throws IOException {
            if (myIsDelta) {
                try {
                    DAVEditorHandler.this.characters(new char[] { (char) (b & 0xff) }, 0, 1);
                } catch (SAXException saxe) {
                    throw new IOException(saxe.getMessage());
                }
            } else {
                try {
                    getDeltaGenerator().sendDelta(myPath, new byte[] { (byte) b }, 1, myDeltaConsumer);
                } catch (SVNException svne) {
                    IOExceptionWrapper ioeWrapper = new IOExceptionWrapper(svne);
                    throw ioeWrapper;
                }
            }
        }
        
        public void write(byte[] b, int off, int len) throws IOException {
            if (myIsDelta) {
                char[] chars = new char[len];
                for (int i = 0; i < len; i++) { 
                    chars[i] = (char) (b[off + i] & 0xff);
                }
                
                try {
                    DAVEditorHandler.this.characters(chars, 0, chars.length);
                } catch (SAXException saxe) {
                    throw new IOException(saxe.getMessage());
                }
            } else {
                try {
                    byte[] target = new byte[len];
                    System.arraycopy(b, off, target, 0, len);
                    getDeltaGenerator().sendDelta(myPath, target, len, myDeltaConsumer);
                } catch (SVNException svne) {
                    IOExceptionWrapper ioeWrapper = new IOExceptionWrapper(svne);
                    throw ioeWrapper;
                }
            }
        }
        
        public void write(byte[] b) throws IOException {
            if (myIsDelta) {
                char[] chars = new char[b.length];
                for (int i = 0; i < b.length; i++) { 
                    chars[i] = (char) (b[i] & 0xff);
                }
                
                try {
                    DAVEditorHandler.this.characters(chars, 0, chars.length);
                } catch (SAXException saxe) {
                    throw new IOException(saxe.getMessage());
                }
            } else {
                try {
                    getDeltaGenerator().sendDelta(myPath, b, b.length, myDeltaConsumer);
                } catch (SVNException svne) {
                    IOExceptionWrapper ioeWrapper = new IOExceptionWrapper(svne);
                    throw ioeWrapper;
                }
                
            }
        }
        
        private SVNDeltaGenerator getDeltaGenerator() {
            if (myDeltaGenerator == null) {
                myDeltaGenerator = new SVNDeltaGenerator();
            }
            return myDeltaGenerator;
        }
    }
}
