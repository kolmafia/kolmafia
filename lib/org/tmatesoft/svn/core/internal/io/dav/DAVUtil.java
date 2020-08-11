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

import java.util.Iterator;
import java.util.Map;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.io.dav.handlers.DAVOptionsHandler;
import org.tmatesoft.svn.core.internal.io.dav.handlers.DAVPropertiesHandler;
import org.tmatesoft.svn.core.internal.io.dav.http.HTTPHeader;
import org.tmatesoft.svn.core.internal.io.dav.http.HTTPStatus;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class DAVUtil {
    
    public static int DEPTH_ZERO = 0;
    public static int DEPTH_ONE = 1;
    public static int DEPTH_INFINITE = -1;
    
    private static boolean ourIsUseDAVWCURL = true;
    
    public static synchronized boolean isUseDAVWCURL() {
        return ourIsUseDAVWCURL;
    }

    public static synchronized void setUseDAVWCURL(boolean useDAVWCURL) {
        ourIsUseDAVWCURL = useDAVWCURL;
    }

    public static HTTPStatus getProperties(DAVConnection connection, String path, int depth, String label, DAVElement[] properties, Map result) throws SVNException {
        HTTPHeader header = new HTTPHeader();
        if (depth == DEPTH_ZERO) {
            header.setHeaderValue(HTTPHeader.DEPTH_HEADER, "0");
        } else if (depth == DEPTH_ONE) {
            header.setHeaderValue(HTTPHeader.DEPTH_HEADER, "1");
        } else if (depth == DEPTH_INFINITE) {
            header.setHeaderValue(HTTPHeader.DEPTH_HEADER, "infinity");
        } else {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, 
                    "Invalid PROPFIND depth value: ''{0}''", new Object[]{depth});
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        if (label != null) {
            header.setHeaderValue(HTTPHeader.LABEL_HEADER, label);
        }
        StringBuffer body = DAVPropertiesHandler.generatePropertiesRequest(null, properties);
        DAVPropertiesHandler davHandler = new DAVPropertiesHandler();
        davHandler.setDAVProperties(result);        
        return connection.doPropfind(path, header, body, davHandler);
    }
    
    public static DAVProperties getResourceProperties(DAVConnection connection, String path, String label, 
            DAVElement[] properties) throws SVNException {
        Map resultMap = new SVNHashMap();
        HTTPStatus status = getProperties(connection, path, DEPTH_ZERO, label, properties, resultMap);
        if (status.getError() != null) {
            SVNErrorManager.error(status.getError(), SVNLogType.NETWORK);
        }
        if (!resultMap.isEmpty()) {
            return (DAVProperties) resultMap.values().iterator().next();
        }
        label = label == null ? "NULL" : label;
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, "Failed to find label ''{0}'' for URL ''{1}''", new Object[] {label, path});
        SVNErrorManager.error(err, SVNLogType.NETWORK);
        return null;
    }
    
    public static String getPropertyValue(DAVConnection connection, String path, String label, DAVElement property) throws SVNException {
        DAVProperties props = getResourceProperties(connection, path, label, new DAVElement[] {property});
        SVNPropertyValue value = props.getPropertyValue(property);
        if (value == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_PROPS_NOT_FOUND, "''{0}'' was not present on the resource", property.toString());
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        return value.getString();
    }
    
    public static DAVProperties getStartingProperties(DAVConnection connection, String path, String label) throws SVNException {
        return getResourceProperties(connection, path, label, DAVElement.STARTING_PROPERTIES);
    }

    public static DAVProperties findStartingProperties(DAVConnection connection, DAVRepository repos, String fullPath) throws SVNException {
        DAVProperties props = null;
        String originalPath = fullPath;
        String loppedPath = "";
        if ("".equals(fullPath)) {
            props = getStartingProperties(connection, fullPath, null);
            if (props != null) {
                if (props.getPropertyValue(DAVElement.REPOSITORY_UUID) != null && repos != null) {
                    repos.setRepositoryUUID(props.getPropertyValue(DAVElement.REPOSITORY_UUID).getString());
                }
                props.setLoppedPath(loppedPath);
            }
            return props;
        }
        
        while(!"".equals(fullPath)) {
            SVNErrorMessage err = null;
            SVNException nested=null;
            try {
                props = getStartingProperties(connection, fullPath, null);
            } catch (SVNException e) {
                if (e.getErrorMessage() == null) {
                    throw e;
                }
                err = e.getErrorMessage();
            }            
            if (err == null) {
                break;
            }
            if (err.getErrorCode() != SVNErrorCode.FS_NOT_FOUND) {
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }
            loppedPath = SVNPathUtil.append(SVNPathUtil.tail(fullPath), loppedPath);
            int length = fullPath.length();
            fullPath = SVNPathUtil.removeTail(fullPath);
            // will return "" for "/dir", hack it here, to make sure we're not missing root.
            // we assume full path always starts with "/". 
            if (length > 1 && "".equals(fullPath)) {
                fullPath = "/";
            }
            if (length == fullPath.length()) {
                SVNErrorMessage err2 = SVNErrorMessage.create(err.getErrorCode(), "The path was not part of repository");
                SVNErrorManager.error(err2, err, nested, SVNLogType.NETWORK);
            }
        }        
        if ("".equals(fullPath)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_ILLEGAL_URL, "No part of path ''{0}'' was found in repository HEAD", originalPath);
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        if (props != null) {
            if (props.getPropertyValue(DAVElement.REPOSITORY_UUID) != null && repos != null) {
                repos.setRepositoryUUID(props.getPropertyValue(DAVElement.REPOSITORY_UUID).getString());
            }
            if (props.getPropertyValue(DAVElement.BASELINE_RELATIVE_PATH) != null && repos != null) {
                String relativePath = props.getPropertyValue(DAVElement.BASELINE_RELATIVE_PATH).getString();
                relativePath = SVNEncodingUtil.uriEncode(relativePath);
                String rootPath = fullPath.substring(0, fullPath.length() - relativePath.length());
                repos.setRepositoryRoot(repos.getLocation().setPath(rootPath, true));
            }
            props.setLoppedPath(loppedPath);
        } 
        
        return props;
    }
    
    public static String getVCCPath(DAVConnection connection, DAVRepository repository, String path) throws SVNException {
        DAVProperties properties = findStartingProperties(connection, repository, path);
        SVNPropertyValue vcc = properties.getPropertyValue(DAVElement.VERSION_CONTROLLED_CONFIGURATION);
        if (vcc == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, "The VCC property was not found on the resource");
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        return vcc.getString();
    }

    public static DAVBaselineInfo getStableURL(DAVConnection connection, DAVRepository repos, String path, long revision,
                                                  boolean includeType, boolean includeRevision, DAVBaselineInfo info) throws SVNException {
        if (connection.hasHttpV2Support()) {
            info = info == null ? new DAVBaselineInfo() : info;
            if (SVNRevision.isValidRevisionNumber(revision)) {
                info.revision = revision;
            } else {
                info.revision = getLatestRevisionHttpV2(connection);
            }
            info.baselineBase = SVNPathUtil.append(connection.myRevRootStub, String.valueOf(info.revision));
            info.baselinePath = connection.getRelativePath(path);
            return info;
        } else {
            return getBaselineInfo(connection, repos, path, revision, includeType, includeRevision, info);
        }
    }

    public static DAVBaselineInfo getBaselineInfo(DAVConnection connection, DAVRepository repos, String path, long revision,
                                                  boolean includeType, boolean includeRevision, DAVBaselineInfo info) throws SVNException {
        info = info == null ? new DAVBaselineInfo() : info;

        DAVElement[] properties = includeRevision ? DAVElement.BASELINE_PROPERTIES : new DAVElement[] {DAVElement.BASELINE_COLLECTION};
        DAVProperties baselineProperties = getBaselineProperties(connection, repos, path, revision, properties);


        info.baselinePath = baselineProperties.getURL();
        SVNPropertyValue baseValue = baselineProperties.getPropertyValue(DAVElement.BASELINE_COLLECTION);
        info.baselineBase = baseValue == null ? null : baseValue.getString();
        info.baseline = baselineProperties.getOriginalURL();
        if (info.baselineBase == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, "'DAV:baseline-collection' not present on the baseline resource");
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
//        info.baselineBase = SVNEncodingUtil.uriEncode(info.baselineBase);
        if (includeRevision) {
            SVNPropertyValue version = baselineProperties.getPropertyValue(DAVElement.VERSION_NAME);
            if (version == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, "'DAV:version-name' not present on the baseline resource");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }
            try {
                info.revision = Long.parseLong(version.getString());
            } catch (NumberFormatException nfe) {
                SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_DAV_MALFORMED_DATA, nfe), SVNLogType.NETWORK);
            }
        }
        if (includeType) {
            Map propsMap = new SVNHashMap();
            path = SVNPathUtil.append(info.baselineBase, info.baselinePath);
            HTTPStatus status = getProperties(connection, path, 0, null, new DAVElement[] {DAVElement.RESOURCE_TYPE}, propsMap);
            if (status.getError() != null) {
                SVNErrorManager.error(status.getError(), SVNLogType.NETWORK);
            }
            if (!propsMap.isEmpty()) {
                DAVProperties props = (DAVProperties) propsMap.values().iterator().next();
                info.isDirectory = props != null && props.isCollection();
            }
        }
        return info;
    }

    public static DAVProperties getBaselineProperties(DAVConnection connection, DAVRepository repos, String path, long revision, DAVElement[] elements) throws SVNException {
        final boolean httpV2Enabled = (repos != null) ? repos.isHttpV2Enabled() :
                ((DAVRepository) connection.getRepository()).isHttpV2Enabled();
        if (httpV2Enabled) {
            if (revision < 0) {
                revision = getLatestRevisionHttpV2(connection);
            }
            String propFindPath = connection.myRevStub + "/" + revision;
            return getResourceProperties(connection, propFindPath, String.valueOf(SVNRepository.INVALID_REVISION), elements);
        } else {
            DAVProperties properties = null;

            String loppedPath = "";
            properties = findStartingProperties(connection, repos, path);
            SVNPropertyValue vccValue = properties.getPropertyValue(DAVElement.VERSION_CONTROLLED_CONFIGURATION);
            if (vccValue == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED,
                        "The VCC property was not found on the resource");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }
            loppedPath = properties.getLoppedPath();
            SVNPropertyValue baselineRelativePathValue = properties.getPropertyValue(DAVElement.BASELINE_RELATIVE_PATH);
            if (baselineRelativePathValue == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED,
                        "The relative-path property was not found on the resource");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }
            String baselineRelativePath = SVNEncodingUtil.uriEncode(baselineRelativePathValue.getString());
            baselineRelativePath = SVNPathUtil.append(baselineRelativePath, loppedPath);
            String label = null;
            String vcc = vccValue.getString();
            if (revision < 0) {
                vcc = getPropertyValue(connection, vcc, null, DAVElement.CHECKED_IN);
            } else {
                label = Long.toString(revision);
            }
            properties = getResourceProperties(connection, vcc, label, elements);
            properties.setURL(baselineRelativePath);
            return properties;
        }
    }

    public static long getLatestRevisionHttpV2(DAVConnection davConnection) throws SVNException {
        davConnection.myLatestRevision = SVNRepository.INVALID_REVISION;

        HTTPStatus status = davConnection.doOptions(davConnection.getLocation().getPath());
        if (status.getCode() != 200) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_OPTIONS_REQ_FAILED,
                    "OPTIONS request (for latest revision) got HTTP response code {0}",
                    new Object[]{status.getCode()});
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        davConnection.parseCapabilities(status);
        if (!SVNRevision.isValidRevisionNumber(davConnection.myLatestRevision)) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.RA_DAV_OPTIONS_REQ_FAILED, "The OPTIONS response did not include the youngest revision");
            SVNErrorManager.error(errorMessage, SVNLogType.NETWORK);
        }
        return davConnection.myLatestRevision;
    }

    public static SVNProperties filterProperties(DAVProperties source, SVNProperties target) {
        target = target == null ? new SVNProperties() : target;
        for (Iterator props = source.getProperties().entrySet().iterator(); props.hasNext();) {
            Map.Entry entry = (Map.Entry) props.next();
            DAVElement element = (DAVElement) entry.getKey();
            SVNPropertyValue value = (SVNPropertyValue) entry.getValue();
            String propertyName = getPropertyNameByElement(element);
            if (propertyName != null) {
                target.put(propertyName, value);
            }
        }
        return target;
    }

    public static String getPropertyNameByElement(DAVElement element) {
        if (element == null) {
            return null;
        }
        String namespace = element.getNamespace();
        String name = element.getName();
        if (namespace.equals(DAVElement.SVN_CUSTOM_PROPERTY_NAMESPACE)) {
            // hack!
            if (name.startsWith("svk_")) {
                name = name.substring(0, "svk".length()) + ":" + name.substring("svk".length() + 1);
            }
            return name;
        } else if (namespace.equals(DAVElement.SVN_SVN_PROPERTY_NAMESPACE)) {
            return "svn:" + name;
        } else if (element == DAVElement.CHECKED_IN) {
            return SVNProperty.WC_URL;//"svn:wc:ra_dav:version-url";
        }
        return null;
    }

    /**
     * @deprecated for binary compatibility only
     */
    public static void setSpecialWCProperties(SVNProperties props, DAVElement property, SVNPropertyValue propValue) {
        setSpecialWCProperties(props, property, propValue, false);
    }

    public static void setSpecialWCProperties(SVNProperties props, DAVElement property, SVNPropertyValue propValue, boolean isDir) {
        String propName = convertDAVElementToPropName(property, isDir);
        if (propName != null) {
            props.put(propName, propValue);
        }
    }

    public static void setSpecialWCProperties(ISVNEditor editor, boolean isDir, String path, DAVElement property, 
            SVNPropertyValue propValue) throws SVNException {
        String propName = convertDAVElementToPropName(property, isDir);
        if (propName != null) {
            if (isDir) {
                editor.changeDirProperty(propName, propValue);
            } else {
                editor.changeFileProperty(path, propName, propValue);
            }
        }
    }

    public static String getPathFromURL(String url) {
        String schemeEnd = "://";
        int ind = url.indexOf(schemeEnd);
        if (ind == -1) {
            return url;
        }
        
        url = url.substring(schemeEnd.length());
        for (int i = 0; i < url.length(); i++) {
            char currentChar = url.charAt(i);
            if (currentChar == '/' || currentChar == '?' || currentChar == '#') {
                return url.substring(i);
            }
        }
        return "/";
    }

    private static String convertDAVElementToPropName(DAVElement property, boolean isDir) {
        String propName = null;
        if (property == DAVElement.VERSION_NAME) {
            propName = SVNProperty.COMMITTED_REVISION;    
        } else if (property == DAVElement.CREATOR_DISPLAY_NAME) {
            propName = SVNProperty.LAST_AUTHOR;
        } else if (property == DAVElement.CREATION_DATE) {
            propName = SVNProperty.COMMITTED_DATE;
        } else if (property == DAVElement.REPOSITORY_UUID) {
            propName = SVNProperty.UUID;
        } else if (property == DAVElement.MD5_CHECKSUM && !isDir) {
            propName = SVNProperty.CHECKSUM;
        } else if (property == DAVElement.SHA1_CHECKSUM && !isDir) {
            propName = SVNProperty.SVNKIT_SHA1_CHECKSUM;
        }
        return propName;
    }

    public static SVNErrorMessage createUnexpectedStatusErrorMessage(HTTPStatus httpStatus, String method, String path) {
        final int code = httpStatus.getCode();
        final String location = httpStatus.getHeader().getFirstHeaderValue(HTTPHeader.LOCATION_HEADER);
        if (code != 405) {
            SVNErrorMessage errorMessage = createDefaultUnexpectedStatusErrorMessage(httpStatus, path, location);
            if (errorMessage != null) {
                return errorMessage;
            }
        }
        switch (code) {
            case 201:
                return SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, "Path ''{0}'' unexpectedly created", path);
            case 204:
                return SVNErrorMessage.create(SVNErrorCode.FS_ALREADY_EXISTS, "Path ''{0}'' already exists", path);
            case 405:
                return SVNErrorMessage.create(SVNErrorCode.RA_DAV_METHOD_NOT_ALLOWED, "The HTTP method ''{0}'' is not allowed on ''{1}''", method, path);
            default:
                return SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, "Unexpected HTTP status {0} ''{1}'' on ''{2}'' request to ''{3}''", code, httpStatus.getReason(), method, path);
        }
    }

    private static SVNErrorMessage createDefaultUnexpectedStatusErrorMessage(HTTPStatus httpStatus, String path, String location) {
        final int code = httpStatus.getCode();
        switch (code) {
            case 301:
            case 302:
            case 303:
            case 307:
            case 308:
                return SVNErrorMessage.create(SVNErrorCode.RA_DAV_RELOCATED, (code == 301) ? "Repository moved permanently to ''{0}''" : "Repository moved temporarily to '%s'", location);
            case 403:
                return SVNErrorMessage.create(SVNErrorCode.RA_DAV_FORBIDDEN, "Access to ''{0}'' forbidden", path);
            case 404:
                return SVNErrorMessage.create(SVNErrorCode.FS_NOT_FOUND, "''{0}'' path not found", path);
            case 405:
                return SVNErrorMessage.create(SVNErrorCode.RA_DAV_METHOD_NOT_ALLOWED, "HTTP method is not allowed on ''{0}''", path);
            case 409:
                return SVNErrorMessage.create(SVNErrorCode.FS_CONFLICT, "''{0}'' conflicts", path);
            case 412:
                return SVNErrorMessage.create(SVNErrorCode.RA_DAV_PRECONDITION_FAILED, "Precondition on ''{0}'' failed", path);
            case 423:
                return SVNErrorMessage.create(SVNErrorCode.FS_NO_LOCK_TOKEN, "''{0}'': no lock token available", path);
            case 411:
                return SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, "DAV request failed: 411 Content length required. The server or an intermediate proxy does not accept " +
                        "chunked encoding. Try setting 'http-chunked-requests' to 'auto' or 'no' in your client configuration.");
            case 500:
                return SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, "Unexpected server error {0} ''{1}'' on ''{2}''", code, httpStatus.getReason(), path);
            case 501:
                return SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "The requested feature is not supported by '%s'", path);
        }

        if (code >= 300 || code <= 199) {
            return SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, "Unexpected HTTP status {0} ''{1}'' on ''{2}''", code, httpStatus.getReason(), path);
        }
        return null;
    }
}
