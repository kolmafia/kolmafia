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
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.internal.io.dav.DAVElement;
import org.tmatesoft.svn.core.internal.util.SVNXMLUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.util.SVNLogType;
import org.xml.sax.Attributes;


/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class DAVReplayHandler extends DAVEditorHandler {

    public static StringBuffer generateReplayRequest(long highRevision, long lowRevision, boolean sendDeltas) {
        StringBuffer xmlBuffer = new StringBuffer();
        SVNXMLUtil.addXMLHeader(xmlBuffer);
        SVNXMLUtil.openNamespaceDeclarationTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "replay-report", 
                SVN_NAMESPACES_LIST, SVNXMLUtil.PREFIX_MAP, xmlBuffer);
        SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "revision", String.valueOf(highRevision), 
                xmlBuffer);
        SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "low-water-mark", String.valueOf(lowRevision), 
                xmlBuffer);
        SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "send-deltas", sendDeltas ? "1" : "0", 
                xmlBuffer);
        SVNXMLUtil.addXMLFooter(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "replay-report", xmlBuffer);
        return xmlBuffer;
    }

    protected static final DAVElement EDITOR_REPORT = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "editor-report");
    protected static final DAVElement OPEN_ROOT = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "open-root");
    protected static final DAVElement APPLY_TEXT_DELTA = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "apply-textdelta");
    protected static final DAVElement CLOSE_FILE = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "close-file");
    protected static final DAVElement CLOSE_DIRECTORY = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "close-directory");
    protected static final DAVElement CHANGE_FILE_PROPERTY = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "change-file-prop");
    protected static final DAVElement CHANGE_DIR_PROPERTY = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "change-dir-prop");

    protected static final String CHECKSUM_ATTR = "checksum";
    protected static final String DEL_ATTR = "del";

    public DAVReplayHandler(ISVNEditor editor, boolean fetchContent) {
        super(null, null, editor, null, fetchContent, false);
    }

    protected void startElement(DAVElement parent, DAVElement element, Attributes attrs) throws SVNException {
        if (element == TARGET_REVISION) {
            String rev = attrs.getValue(REVISION_ATTR);
            if (rev == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, "Missing revision attr in target-revision element");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            } else {
                try {
                    myEditor.targetRevision(Long.parseLong(rev));
                } catch (NumberFormatException nfe) {
                    SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, nfe), SVNLogType.NETWORK);
                }
            }
        } else if (element == OPEN_ROOT) {
            String rev = attrs.getValue(REVISION_ATTR);
            if (rev == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, "Missing revision attr in open-root element");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            } else {
                try {
                    myEditor.openRoot(Long.parseLong(rev));
                } catch (NumberFormatException nfe) {
                    SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, nfe), SVNLogType.NETWORK);
                }
                myPath = "";
                myIsDirectory = true;
            }
        } else if (element == DELETE_ENTRY) {
            String path = attrs.getValue(NAME_ATTR);
            String rev = attrs.getValue(REVISION_ATTR);
            if (path == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, "Missing name attr in delete-entry element");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            } else if (rev == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, "Missing rev attr in delete-entry element");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            } else {
                try {
                    myEditor.deleteEntry(path, Long.parseLong(rev));
                } catch (NumberFormatException nfe) {
                    SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, nfe), SVNLogType.NETWORK);
                }
            }
        } else if (element == OPEN_DIRECTORY || element == ADD_DIRECTORY) {
            String path = attrs.getValue(NAME_ATTR);
            String rev = attrs.getValue(REVISION_ATTR);

            if (path == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, "Missing name attr in " + (element == OPEN_DIRECTORY ? "open-directory" : "add-directory") + " element");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            } else {
                long revision = -1;
                if (rev != null) {
                    try {
                        revision =Long.parseLong(rev);
                    } catch (NumberFormatException nfe) {
                        SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, nfe), SVNLogType.NETWORK);
                    }
                }
                if (element == OPEN_DIRECTORY) {
                    myEditor.openDir(path, revision);
                } else {
                    String copyFromPath = attrs.getValue(COPYFROM_PATH_ATTR);
                    String cfRevision = attrs.getValue(COPYFROM_REV_ATTR);
                    long copyFromRevision = -1;
                    if (cfRevision != null) {
                        try {
                            copyFromRevision = Long.parseLong(cfRevision);
                        } catch (NumberFormatException nfe) {
                            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, nfe), SVNLogType.NETWORK);
                        }
                    }
                    myEditor.addDir(path, copyFromPath, copyFromRevision);
                }
            }
            myPath = path;
            myIsDirectory = true;
        } else if (element == OPEN_FILE || element == ADD_FILE) {
            String path = attrs.getValue(NAME_ATTR);
            if (path == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, "Missing name attr in " + (element == OPEN_FILE ? "open-file" : "add-file") + " element");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }

            if (element == ADD_FILE) {
                String copyFromPath = attrs.getValue(COPYFROM_PATH_ATTR);
                String cfRevision = attrs.getValue(COPYFROM_REV_ATTR);
                long copyFromRevision = -1;
                if (cfRevision != null) {
                    try {
                        copyFromRevision = Long.parseLong(cfRevision);
                    } catch (NumberFormatException nfe) {
                        SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, nfe), SVNLogType.NETWORK);
                    }
                }
                myEditor.addFile(path, copyFromPath, copyFromRevision);
            } else {
                String rev = attrs.getValue(REVISION_ATTR);
                long revision = -1;
                if (rev != null) {
                    try {
                        revision = Long.parseLong(rev);
                    } catch (NumberFormatException nfe) {
                        SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, nfe), SVNLogType.NETWORK);
                    }
                }
                myEditor.openFile(path, revision);
            }
            myIsDirectory = false;
            myPath = path;
        } else if (element == APPLY_TEXT_DELTA) {
            if (myIsDirectory) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, "Got apply-textdelta element without preceding add-file or open-file");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }

            String checksum = attrs.getValue(CHECKSUM_ATTR);
            try {
                myEditor.applyTextDelta(myPath, checksum);
                setDeltaProcessing(true);
            } catch (SVNException svne) {
                //
            }
        } else if (element == CLOSE_FILE) {
            if (myIsDirectory) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, "Got close-file element without preceding add-file or open-file");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            } else {
                String checksum = attrs.getValue(CHECKSUM_ATTR);
                myEditor.closeFile(myPath, checksum);
                myIsDirectory = true;
            }
        } else if (element == CLOSE_DIRECTORY) {
            if (!myIsDirectory) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, "Got close-directory element without ever opening a directory");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            } else {
                myEditor.closeDir();
            }
        } else if (element == CHANGE_FILE_PROPERTY || element == CHANGE_DIR_PROPERTY) {
            String name = attrs.getValue(NAME_ATTR);
            if (name == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, "Missing name attr in " + (element == CHANGE_FILE_PROPERTY ? "change-file-prop" : "change-dir-prop") + " element");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            } else {
                if (attrs.getValue(DEL_ATTR) != null) {
                    if (element == CHANGE_FILE_PROPERTY) {
                        myEditor.changeFileProperty(myPath, name, null);
                    } else {
                        myEditor.changeDirProperty(name, null);
                    }
                    myPropertyName = null;
                } else {
                    myPropertyName = name;
                }
            }
        }
    }

    protected void endElement(DAVElement parent, DAVElement element, StringBuffer cdata) throws SVNException {
        if (element == APPLY_TEXT_DELTA) {
            setDeltaProcessing(false);
        } else if (element == CHANGE_FILE_PROPERTY || element == CHANGE_DIR_PROPERTY) {
            if (cdata != null && !"".equals(cdata.toString()) && myPropertyName == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, "Got cdata content for a prop delete");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }
            if (myPropertyName != null) {
                SVNPropertyValue propertyValue = createPropertyValueFromBase64(null, myPropertyName, cdata);
                if (element == CHANGE_FILE_PROPERTY) {
                    myEditor.changeFileProperty(myPath, myPropertyName, propertyValue);
                } else {
                    myEditor.changeDirProperty(myPropertyName, propertyValue);
                }
            }
        }
    }
}
