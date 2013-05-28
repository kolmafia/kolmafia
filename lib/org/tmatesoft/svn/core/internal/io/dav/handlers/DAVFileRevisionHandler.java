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

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.internal.io.dav.DAVElement;
import org.tmatesoft.svn.core.internal.util.SVNXMLUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.ISVNDeltaConsumer;
import org.tmatesoft.svn.core.io.ISVNFileRevisionHandler;
import org.tmatesoft.svn.core.io.SVNFileRevision;
import org.tmatesoft.svn.util.SVNLogType;
import org.xml.sax.Attributes;


/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class DAVFileRevisionHandler extends BasicDAVDeltaHandler {

    public static StringBuffer generateFileRevisionsRequest(StringBuffer xmlBuffer,
                                                            long startRevision,
                                                            long endRevision,
                                                            String path,
                                                            boolean includeMergedRevisions) {
        xmlBuffer = xmlBuffer == null ? new StringBuffer() : xmlBuffer;
        SVNXMLUtil.addXMLHeader(xmlBuffer);
        SVNXMLUtil.openNamespaceDeclarationTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "file-revs-report", SVN_NAMESPACES_LIST, SVNXMLUtil.PREFIX_MAP, xmlBuffer);
        if (startRevision >= 0) {
            SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "start-revision", String.valueOf(startRevision), xmlBuffer);
        }
        if (endRevision >= 0) {
            SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "end-revision", String.valueOf(endRevision), xmlBuffer);
        }
        if (includeMergedRevisions) {
            SVNXMLUtil.openXMLTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "include-merged-revisions", SVNXMLUtil.XML_STYLE_SELF_CLOSING, null, xmlBuffer);
        }
        SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "path", path, xmlBuffer);
        SVNXMLUtil.addXMLFooter(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "file-revs-report", xmlBuffer);
        return xmlBuffer;
    }

    private static final DAVElement REVISION_PROPERTY = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "rev-prop");
    private static final DAVElement FILE_REVISION = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "file-rev");

    private static final DAVElement SET_PROPERTY = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "set-prop");
    private static final DAVElement DELETE_PROPERTY = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "remove-prop");
    private static final DAVElement MERGED_REVISION = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "merged-revision");

    private ISVNFileRevisionHandler myFileRevisionsHandler;
    private String myPath;
    private long myRevision;
    private SVNProperties myProperties;
    private SVNProperties myPropertiesDelta;
    private String myPropertyName;
    private String myPropertyEncoding;
    private boolean myIsMergedRevision;
    private int myCount;

    public DAVFileRevisionHandler(ISVNFileRevisionHandler handler) {
        myFileRevisionsHandler = handler;
        myCount = 0;
        init();
    }

    protected void startElement(DAVElement parent, DAVElement element, Attributes attrs) throws SVNException {
        if (element == FILE_REVISION) {
            myIsMergedRevision = false;
            myPath = attrs.getValue("path");
            if (myPath == null) {
                missingAttributeError(element, "path");
            }
            String revString = attrs.getValue("rev");
            if (revString == null) {
                missingAttributeError(element, "rev");
            }
            try {
                myRevision = Long.parseLong(revString);
            } catch (NumberFormatException nfe) {
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, nfe), SVNLogType.NETWORK);
            }
        } else if (element == REVISION_PROPERTY || element == SET_PROPERTY || element == DELETE_PROPERTY) {
            myPropertyName = attrs.getValue("name");
            if (myPropertyName == null) {
                missingAttributeError(element, "name");
            }
            myPropertyEncoding = attrs.getValue("encoding");
        } else if (element == TX_DELTA) {
            // handle file revision with props.
            if (myPath != null && myFileRevisionsHandler != null) {
                if (myProperties == null) {
                    myProperties = new SVNProperties();
                }
                if (myPropertiesDelta == null) {
                    myPropertiesDelta = new SVNProperties();
                }
                SVNFileRevision revision = new SVNFileRevision(myPath,
                        myRevision,
                        myProperties,
                        myPropertiesDelta,
                        myIsMergedRevision);
                myFileRevisionsHandler.openRevision(revision);
                myProperties = null;
                myPropertiesDelta = null;
                myPath = null;
                myFileRevisionsHandler.applyTextDelta(myPath, null);
            }
            setDeltaProcessing(true);
        } else if (element == MERGED_REVISION) {
            myIsMergedRevision = true;
        }
    }

    protected void endElement(DAVElement parent, DAVElement element, StringBuffer cdata) throws SVNException {
        if (element == FILE_REVISION) {
            if (myPath != null && myFileRevisionsHandler != null) {
                // handle file revision if was not handled yet (no tx delta).
                if (myProperties == null) {
                    myProperties = new SVNProperties();
                }
                if (myPropertiesDelta == null) {
                    myPropertiesDelta = new SVNProperties();
                }
                SVNFileRevision revision = new SVNFileRevision(myPath,
                        myRevision,
                        myProperties,
                        myPropertiesDelta);
                myFileRevisionsHandler.openRevision(revision);
            }
            // handle close revision with props?
            if (myFileRevisionsHandler != null) {
                myFileRevisionsHandler.closeRevision(myPath);
            }
            myPath = null;
            myProperties = null;
            myPropertiesDelta = null;
            myPropertyEncoding = null;
            myPropertyName = null;
        } else if (element == TX_DELTA) {
            setDeltaProcessing(false);
            myCount++;
        } else if (element == REVISION_PROPERTY) {
            if (myProperties == null) {
                myProperties = new SVNProperties();
            }
            myProperties.put(myPropertyName, cdata != null ? cdata.toString() : "");
            myPropertyName = null;
        } else if (element == SET_PROPERTY) {
            if (myPropertiesDelta == null) {
                myPropertiesDelta = new SVNProperties();
            }
            if (myPropertyName != null) {
                SVNPropertyValue propertyValue = createPropertyValue(null, myPropertyName, cdata, myPropertyEncoding);
                myPropertiesDelta.put(myPropertyName, propertyValue);
            }
            myPropertyName = null;
            myPropertyEncoding = null;
        } else if (element == DELETE_PROPERTY) {
            if (myPropertiesDelta == null) {
                myPropertiesDelta = new SVNProperties();
            }
            if (myPropertyName != null) {
                myPropertiesDelta.put(myPropertyName, (byte[]) null);
            }
            myPropertyEncoding = null;
            myPropertyName = null;
        }
    }

    public int getEntriesCount() {
        return myCount;
    }

    protected ISVNDeltaConsumer getDeltaConsumer() {
        return myFileRevisionsHandler;
    }

    protected String getCurrentPath() {
        return myPath;
    }

    private void missingAttributeError(DAVElement element, String attr) throws SVNException {
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA,
                "Missing attribute ''{0}'' on element {1}",
                new Object[]{attr, element});
        SVNErrorManager.error(err, SVNLogType.NETWORK);
    }
    
}
 