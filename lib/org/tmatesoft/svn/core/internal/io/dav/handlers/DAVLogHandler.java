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

import java.util.Date;
import java.util.Map;

import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNRevisionProperty;
import org.tmatesoft.svn.core.internal.io.dav.DAVElement;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.util.SVNXMLUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;

import org.xml.sax.Attributes;


/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class DAVLogHandler extends BasicDAVHandler {

    public static StringBuffer generateLogRequest(StringBuffer xmlBuffer, long startRevision, long endRevision,
                                                  boolean includeChangedPaths, boolean strictNodes, boolean includeMergedRevisions,
                                                  String[] revPropNames, long limit, String[] paths) {
        xmlBuffer = xmlBuffer == null ? new StringBuffer() : xmlBuffer;
        SVNXMLUtil.addXMLHeader(xmlBuffer);
        SVNXMLUtil.openNamespaceDeclarationTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "log-report", 
                SVN_NAMESPACES_LIST, SVNXMLUtil.PREFIX_MAP, xmlBuffer);
        if (startRevision >= 0) {
            SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "start-revision", 
                    String.valueOf(startRevision), xmlBuffer);
        }
        if (endRevision >= 0) {
            SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "end-revision", 
                    String.valueOf(endRevision), xmlBuffer);
        }
        if (limit > 0) {
            SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "limit", String.valueOf(limit), xmlBuffer);
        }
        if (includeChangedPaths) {
            SVNXMLUtil.openXMLTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "discover-changed-paths", 
                    SVNXMLUtil.XML_STYLE_SELF_CLOSING, null, xmlBuffer);
        }
        if (strictNodes) {
            SVNXMLUtil.openXMLTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "strict-node-history", 
                    SVNXMLUtil.XML_STYLE_SELF_CLOSING, null, xmlBuffer);
        }
        if (includeMergedRevisions) {
            SVNXMLUtil.openXMLTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "include-merged-revisions", 
                    SVNXMLUtil.XML_STYLE_SELF_CLOSING, null, xmlBuffer);
        }
        if (revPropNames != null) {
            for (int i = 0; i < revPropNames.length; i++) {
                String revPropName = revPropNames[i];
                SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "revprop", revPropName, xmlBuffer);
            }
        } else {
            SVNXMLUtil.openXMLTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "all-revprops", 
                    SVNXMLUtil.XML_STYLE_SELF_CLOSING, null, xmlBuffer);
        }

        for (int i = 0; i < paths.length; i++) {
            SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "path", paths[i], xmlBuffer);
        }
        SVNXMLUtil.addXMLFooter(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "log-report", xmlBuffer);
        return xmlBuffer;
    }

    private static final DAVElement LOG_ITEM = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "log-item");
    private static final DAVElement ADDED_PATH = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "added-path");
    private static final DAVElement DELETED_PATH = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "deleted-path");
    private static final DAVElement MODIFIED_PATH = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "modified-path");
    private static final DAVElement REPLACED_PATH = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "replaced-path");
    private static final DAVElement HAS_CHILDREN = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "has-children");
    private static final DAVElement REVPROP = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "revprop");
    private static final DAVElement SUBTRACTIVE_MERGE = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "subtractive-merge");

    private ISVNLogEntryHandler myLogEntryHandler;
    private long myRevision;
    private Map myPaths;
    private String myAuthor;
    private Date myDate;
    private String myComment;
    private SVNLogEntryPath myPath;
    private long myCount;
    private long myLimit;
    private int myNestLevel;
    
    private boolean myIsCompatibleMode;
    private boolean myHasChildren;
    private boolean myIsWantAuthor;
    private boolean myIsWantDate;
    private boolean myIsWantComment;
    private boolean myIsWantCustomRevProps;
    private String myRevPropName;
    private SVNProperties myRevProps;
    private boolean myIsSubtractiveMerge;

    public DAVLogHandler(ISVNLogEntryHandler handler, long limit, String[] revPropNames) {
        myLogEntryHandler = handler;
        myRevision = -1;
        myCount = 0;
        myLimit = limit;
        if (revPropNames != null && revPropNames.length > 0) {
            for (int i = 0; i < revPropNames.length; i++) {
                String revPropName = revPropNames[i];
                if (SVNRevisionProperty.AUTHOR.equals(revPropName)) {
                    myIsWantAuthor = true;
                } else if (SVNRevisionProperty.LOG.equals(revPropName)) {
                    myIsWantComment = true;
                } else if (SVNRevisionProperty.DATE.equals(revPropName)) {
                    myIsWantDate = true;
                } else {
                    myIsWantCustomRevProps = true;
                }
            }
        } else {
            myIsWantAuthor = myIsWantComment = myIsWantDate = true;
        }

        init();
    }
    
    public boolean isWantCustomRevprops() {
        return myIsWantCustomRevProps;
    }

    public boolean isCompatibleMode() {
        return myIsCompatibleMode;
    }

    protected void startElement(DAVElement parent, DAVElement element, Attributes attrs) throws SVNException {
        char type = 0;
        String copyPath = null;
        long copyRevision = -1;

        if (element == REVPROP) {
            myRevPropName = attrs.getValue("name");
            if (myRevPropName == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA,
                        "Missing name attr in revprop element");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }

        } else if (element == HAS_CHILDREN) {
            myHasChildren = true;
        } else if (element == SUBTRACTIVE_MERGE) {
            myIsSubtractiveMerge = true;
        }

        if (element == ADDED_PATH || element == REPLACED_PATH) {
            type = element == ADDED_PATH ? SVNLogEntryPath.TYPE_ADDED : SVNLogEntryPath.TYPE_REPLACED;
            copyPath = attrs.getValue("copyfrom-path");
            String copyRevisionStr = attrs.getValue("copyfrom-rev");
            if (copyPath != null && copyRevisionStr != null) {
                try {
                    copyRevision = Long.parseLong(copyRevisionStr);
                } catch (NumberFormatException e) {
                }
            }
        } else if (element == MODIFIED_PATH) {
            type = SVNLogEntryPath.TYPE_MODIFIED;
        } else if (element == DELETED_PATH) {
            type = SVNLogEntryPath.TYPE_DELETED;
        }
        if (type != 0) {
            SVNNodeKind nodeKind = SVNNodeKind.UNKNOWN;
            String nodeKindStr = attrs.getValue("node-kind");
            if (nodeKindStr != null) {
                nodeKind = SVNNodeKind.parseKind(nodeKindStr);
            }
            myPath = new SVNLogEntryPath(null, type, copyPath, copyRevision, nodeKind);
        }

    }

    protected void endElement(DAVElement parent, DAVElement element, StringBuffer cdata) throws SVNException {
        if (element == LOG_ITEM) {
            if (myNestLevel == 0) {
                myCount++;
            }
            if (myLimit > 0 && myCount > myLimit && myNestLevel == 0) {
                myIsCompatibleMode = true;
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN);
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }
            if (myLogEntryHandler != null) {
                if (myPaths == null) {
                    myPaths = new SVNHashMap();
                }
                if (myRevProps == null) {
                    myRevProps = new SVNProperties();
                }
                if (myAuthor != null) {
                    myRevProps.put(SVNRevisionProperty.AUTHOR, myAuthor);
                }
                if (myComment != null) {
                    myRevProps.put(SVNRevisionProperty.LOG, myComment);
                }
                if (myDate != null) {
                    myRevProps.put(SVNRevisionProperty.DATE, SVNDate.formatDate(myDate));
                }
                SVNLogEntry logEntry = new SVNLogEntry(myPaths, myRevision, myRevProps, myHasChildren);
                logEntry.setSubtractiveMerge(myIsSubtractiveMerge);
                myLogEntryHandler.handleLogEntry(logEntry);
                if (logEntry.hasChildren()) {
                    myNestLevel++;
                }
                if (logEntry.getRevision() < 0) {
                    myNestLevel = myNestLevel <= 0 ? 0 : myNestLevel -1;
                }
            }
            myPaths = null;
            myRevProps = null;
            myRevision = -1;
            myAuthor = null;
            myDate = null;
            myComment = null;
            myRevPropName = null;
            myHasChildren = false;
            myIsSubtractiveMerge = false;
        } else if (element == DAVElement.VERSION_NAME && cdata != null) {
            try {
                myRevision = Long.parseLong(cdata.toString());
            } catch (NumberFormatException nfe) {
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, nfe), SVNLogType.NETWORK);
            }
        } else if (element == REVPROP) {
            if (myRevProps == null) {
                myRevProps = new SVNProperties();
            }
            if (myRevPropName != null && cdata != null) {
                myRevProps.put(myRevPropName, cdata.toString());
            }
        } else if (element == DAVElement.CREATOR_DISPLAY_NAME && cdata != null) {
            if (myIsWantAuthor) {
                myAuthor = cdata.toString();
            }
        } else if (element == DAVElement.COMMENT && cdata != null) {
            if (myIsWantComment) {
                myComment = cdata.toString();
            }
        } else if (element == DAVElement.DATE && cdata != null) {
            if (myIsWantDate) {
                myDate = SVNDate.parseDate(cdata.toString());
            }
        } else if (element == ADDED_PATH || element == MODIFIED_PATH || element == REPLACED_PATH || element == DELETED_PATH) {
            if (myPath != null && cdata != null) {
                if (myPaths == null) {
                    myPaths = new SVNHashMap();
                }
                myPath.setPath(cdata.toString());
                String path = myPath.getPath();
                myPath.setPath(path);
                myPaths.put(myPath.getPath(), myPath);
            }
            myPath = null;
        }
    }

    public long getEntriesCount() {
        return myCount;
    }
}