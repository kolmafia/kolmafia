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
import java.util.Iterator;
import java.util.Map;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.internal.io.dav.DAVElement;
import org.tmatesoft.svn.core.internal.io.dav.DAVUtil;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.util.SVNXMLUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.ISVNWorkspaceMediator;
import org.tmatesoft.svn.util.SVNLogType;

import org.xml.sax.Attributes;


/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class DAVMergeHandler extends BasicDAVHandler {

    public static StringBuffer generateMergeRequest(StringBuffer xmlBuffer, String path, String activityURL, 
            Map locks) {
        xmlBuffer = xmlBuffer == null ? new StringBuffer() : xmlBuffer;
        SVNXMLUtil.addXMLHeader(xmlBuffer);
        SVNXMLUtil.openNamespaceDeclarationTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "merge", DAV_NAMESPACES_LIST, 
                SVNXMLUtil.PREFIX_MAP, xmlBuffer);
        SVNXMLUtil.openXMLTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "source", SVNXMLUtil.XML_STYLE_NORMAL, null, 
                xmlBuffer);
        SVNXMLUtil.openCDataTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "href", activityURL, xmlBuffer);
        SVNXMLUtil.closeXMLTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "source", xmlBuffer);
        SVNXMLUtil.openXMLTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "no-auto-merge", 
                SVNXMLUtil.XML_STYLE_SELF_CLOSING, null, xmlBuffer);
        SVNXMLUtil.openXMLTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "no-checkout", 
                SVNXMLUtil.XML_STYLE_SELF_CLOSING, null, xmlBuffer);
        SVNXMLUtil.openXMLTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "prop", SVNXMLUtil.XML_STYLE_NORMAL, null, 
                xmlBuffer);
        SVNXMLUtil.openXMLTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "checked-in", 
                SVNXMLUtil.XML_STYLE_SELF_CLOSING, null, xmlBuffer);
        SVNXMLUtil.openXMLTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "version-name", 
                SVNXMLUtil.XML_STYLE_SELF_CLOSING, null, xmlBuffer);
        SVNXMLUtil.openXMLTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "resourcetype", 
                SVNXMLUtil.XML_STYLE_SELF_CLOSING, null, xmlBuffer);
        SVNXMLUtil.openXMLTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "creationdate", 
                SVNXMLUtil.XML_STYLE_SELF_CLOSING, null, xmlBuffer);
        SVNXMLUtil.openXMLTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "creator-displayname", 
                SVNXMLUtil.XML_STYLE_SELF_CLOSING, null, xmlBuffer);
        SVNXMLUtil.closeXMLTag(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "prop", xmlBuffer);
        if (locks != null && !locks.isEmpty()) {
            xmlBuffer = generateLockDataRequest(xmlBuffer, path, null, locks);
        }
        SVNXMLUtil.addXMLFooter(SVNXMLUtil.DAV_NAMESPACE_PREFIX, "merge", xmlBuffer);
        return xmlBuffer;

    }

    public static StringBuffer generateLockDataRequest(StringBuffer target, String root, String path, Map locks) {
        target = target == null ? new StringBuffer() : target;
        SVNXMLUtil.openNamespaceDeclarationTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "lock-token-list", 
                SVN_NAMESPACES_LIST, SVNXMLUtil.PREFIX_MAP, target);
        for (Iterator paths = locks.keySet().iterator(); paths.hasNext();) {
            String lockPath = (String) paths.next();
            if (path == null || SVNPathUtil.getPathAsChild(path, lockPath) != null) {
                String relativePath = SVNPathUtil.getRelativePath(root, lockPath);
                String token = (String) locks.get(lockPath);
                SVNXMLUtil.openXMLTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "lock", SVNXMLUtil.XML_STYLE_NORMAL, 
                        null, target);
                SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "lock-path", 
                        SVNEncodingUtil.uriDecode(relativePath), target);
                SVNXMLUtil.openCDataTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "lock-token", token, target);
                SVNXMLUtil.closeXMLTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "lock", target);
            }
        }
        SVNXMLUtil.closeXMLTag(SVNXMLUtil.SVN_NAMESPACE_PREFIX, "lock-token-list", target);
        return target;
    }

    public static boolean hasChildPaths(String path, Map locks) {
        for (Iterator paths = locks.keySet().iterator(); paths.hasNext();) {
            String lockPath = (String) paths.next();
            if (SVNPathUtil.getPathAsChild(path, lockPath) != null) {
                return true;
            }
        }
        return false;
    }

    private ISVNWorkspaceMediator myMediator;
    private Map myPathsMap;

    private static final DAVElement RESPONSE = DAVElement.getElement(DAVElement.DAV_NAMESPACE, "response");
    private static final DAVElement POST_COMMIT_ERROR = DAVElement.getElement(DAVElement.SVN_NAMESPACE, "post-commit-err");

    private String myAuthor;
    private Date myCommitDate;
    private long myRevision;

    private String myRepositoryPath;
    private String myVersionPath;

    private DAVElement myResourceType;
    private SVNCommitInfo myCommitInfo;
    private SVNErrorMessage myPostCommitError;

    public DAVMergeHandler(ISVNWorkspaceMediator mediator, Map pathsMap) {
        myMediator = mediator;
        myPathsMap = pathsMap;

        init();
    }

    public SVNCommitInfo getCommitInfo() {
        return myCommitInfo;
    }

    protected void startElement(DAVElement parent, DAVElement element, Attributes attrs) throws SVNException {
        if (element == RESPONSE) {
            myResourceType = null;
            myRepositoryPath = null;
            myVersionPath = null;

            myAuthor = null;
            myCommitDate = null;
            myRevision = -1;
        }
    }

    protected void endElement(DAVElement parent, DAVElement element, StringBuffer cdata) throws SVNException {
        if (element == POST_COMMIT_ERROR) {
            myPostCommitError = SVNErrorMessage.create(SVNErrorCode.REPOS_POST_COMMIT_HOOK_FAILED, cdata.toString(), SVNErrorMessage.TYPE_WARNING);
        } else if (element == DAVElement.HREF) {
            if (parent == RESPONSE) {
                myRepositoryPath = cdata.toString();
                myRepositoryPath = SVNEncodingUtil.uriDecode(myRepositoryPath);
                myRepositoryPath = SVNPathUtil.canonicalizePath(myRepositoryPath);
            } else if (parent == DAVElement.CHECKED_IN) {
                myVersionPath = cdata.toString();
                myVersionPath = SVNPathUtil.canonicalizePath(myVersionPath);
            }
        } else if (parent == DAVElement.RESOURCE_TYPE && element == DAVElement.BASELINE) {
            myResourceType = element;
        } else if (parent == DAVElement.RESOURCE_TYPE && element == DAVElement.COLLECTION) {
            myResourceType = element;
        } else if (element == RESPONSE) {
            // all resource info is collected, do something.
            if (myResourceType == DAVElement.BASELINE) {
                myCommitInfo = new SVNCommitInfo(myRevision, myAuthor, myCommitDate, myPostCommitError);
            } else {
                String reposPath = SVNEncodingUtil.uriEncode(myRepositoryPath);
                String path = (String) myPathsMap.get(reposPath);
                if (path != null && myMediator != null) {
                    String versionURLPropName = SVNProperty.WC_URL; //"svn:wc:ra_dav:version-url";
                    SVNPropertyValue urlPropertyValue = DAVUtil.isUseDAVWCURL() ? SVNPropertyValue.create(myVersionPath) : null; 
                    myMediator.setWorkspaceProperty(SVNEncodingUtil.uriDecode(path), versionURLPropName, urlPropertyValue);
                }
            }
        } else if (element == DAVElement.CREATION_DATE) {
            myCommitDate = SVNDate.parseDate(cdata.toString());
        } else if (element == DAVElement.CREATOR_DISPLAY_NAME) {
            myAuthor = cdata.toString();
        } else if (element == DAVElement.VERSION_NAME) {
            try {
                myRevision = Long.parseLong(cdata.toString());
            } catch (NumberFormatException nfe) {
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, nfe), SVNLogType.NETWORK);
            }
        } else if (parent == DAVElement.PROPSTAT && element == DAVElement.STATUS) {
            // should be 200
        }
    }

}
