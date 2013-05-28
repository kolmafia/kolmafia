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

package org.tmatesoft.svn.core.internal.io.svn;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.tmatesoft.svn.core.ISVNDirEntryHandler;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNAuthenticationException;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLock;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNMergeInfo;
import org.tmatesoft.svn.core.SVNMergeInfoInheritance;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNRevisionProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.delta.SVNDeltaReader;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.util.SVNMergeInfoUtil;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNDepthFilterEditor;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.ISVNFileRevisionHandler;
import org.tmatesoft.svn.core.io.ISVNLocationEntryHandler;
import org.tmatesoft.svn.core.io.ISVNLocationSegmentHandler;
import org.tmatesoft.svn.core.io.ISVNLockHandler;
import org.tmatesoft.svn.core.io.ISVNReplayHandler;
import org.tmatesoft.svn.core.io.ISVNReporter;
import org.tmatesoft.svn.core.io.ISVNReporterBaton;
import org.tmatesoft.svn.core.io.ISVNSession;
import org.tmatesoft.svn.core.io.ISVNWorkspaceMediator;
import org.tmatesoft.svn.core.io.SVNCapability;
import org.tmatesoft.svn.core.io.SVNFileRevision;
import org.tmatesoft.svn.core.io.SVNLocationEntry;
import org.tmatesoft.svn.core.io.SVNLocationSegment;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class SVNRepositoryImpl extends SVNRepository implements ISVNReporter {

    private static final String DIRENT_KIND = "kind";
    private static final String DIRENT_SIZE = "size";
    private static final String DIRENT_HAS_PROPS = "has-props";
    private static final String DIRENT_CREATED_REV = "created-rev";
    private static final String DIRENT_TIME = "time";
    private static final String DIRENT_LAST_AUTHOR = "last-author";

    private SVNConnection myConnection;
    private String myRealm;
    private String myExternalUserName;

    protected SVNRepositoryImpl(SVNURL location, ISVNSession options) {
        super(location, options);
    }

    public void testConnection() throws SVNException {
        try {
            openConnection();
        } catch (SVNException e) {
            closeSession();
            throw e;
        } finally {
            closeConnection();
        }
    }

    public void setLocation(SVNURL url, boolean forceReconnect) throws SVNException {
        if (url == null) {
            return;
        } else if (!url.getProtocol().equals(myLocation.getProtocol())) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_NOT_IMPLEMENTED, "SVNRepository URL could not be changed from ''{0}'' to ''{1}''; create new SVNRepository instance instead", new Object[]{myLocation, url});
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        if (myConnection == null) {
            // force normal relocate, no attempt to reparent.
            forceReconnect = true;
        }
        if (forceReconnect) {
            closeSession();
            myLocation = url;
            myRealm = null;
            myRepositoryRoot = null;
            myRepositoryUUID = null;
            return;
        }
        try {
            openConnection();
            if (reparent(url)) {
                myLocation = url;
                return;
            }
            setLocation(url, true);
        } catch (SVNException e) {
            // thrown by reparent or open connection.
            closeSession();
            throw e;
        } finally {
            closeConnection();
        }
    }

    private boolean reparent(SVNURL url) throws SVNException {
        if (myConnection != null) {
            if (getLocation().equals(url)) {
                return true;
            }
            try {
                Object[] buffer = new Object[]{"reparent", url.toString()};
                write("(w(s))", buffer);
                authenticate();
                read("", null, false);

                String newLocation = url.toString();
                String rootLocation = myRepositoryRoot.toString();

                return newLocation.startsWith(rootLocation) && (newLocation.length() == rootLocation.length() || (newLocation.length() > rootLocation.length() && newLocation.charAt(rootLocation.length()) == '/'));

            } catch (SVNException e) {
                if (e instanceof SVNCancelException || e instanceof SVNAuthenticationException) {
                    throw e;
                }
            }
        }
        return false;
    }

    public long getLatestRevision() throws SVNException {
        Object[] buffer = new Object[]{"get-latest-rev"};
        List values = null;
        try {
            openConnection();
            write("(w())", buffer);
            authenticate();
            values = read("r", null, false);
        } catch (SVNException e) {
            closeSession();
            throw e;
        } finally {
            closeConnection();
        }
        return SVNReader.getLong(values, 0);
    }

    public long getDatedRevision(Date date) throws SVNException {
        if (date == null) {
            date = new Date(System.currentTimeMillis());
        }
        Object[] buffer = new Object[]{"get-dated-rev", date};
        List values = null;
        try {
            openConnection();
            write("(w(s))", buffer);
            authenticate();
            values = read("r", null, false);
        } catch (SVNException e) {
            closeSession();
            throw e;
        } finally {
            closeConnection();
        }
        return SVNReader.getLong(values, 0);
    }

    public SVNProperties getRevisionProperties(long revision, SVNProperties properties) throws SVNException {
        assertValidRevision(revision);
        if (properties == null) {
            properties = new SVNProperties();
        }
        Object[] buffer = new Object[]{"rev-proplist", getRevisionObject(revision)};
        try {
            openConnection();
            write("(w(n))", buffer);
            authenticate();
            List items = read("l", null, false);
            properties = SVNReader.getProperties(items, 0, properties);
        } catch (SVNException e) {
            closeSession();
            throw e;
        } finally {
            closeConnection();
        }
        return properties;
    }

    public SVNPropertyValue getRevisionPropertyValue(long revision, String propertyName) throws SVNException {
        assertValidRevision(revision);
        Object[] buffer = new Object[]{"rev-prop", getRevisionObject(revision), propertyName};
        List values = null;
        try {
            openConnection();
            write("(w(ns))", buffer);
            authenticate();
            values = read("(?b)", null, false);
        } catch (SVNException e) {
            closeSession();
            throw e;
        } finally {
            closeConnection();
        }
        byte[] bytes = SVNReader.getBytes(values, 0);
        return bytes == null ? null : SVNPropertyValue.create(propertyName, bytes);
    }

    public SVNNodeKind checkPath(String path, long revision) throws SVNException {
        try {
            openConnection();
            path = getLocationRelativePath(path);
            Object[] buffer = new Object[]{"check-path", path, getRevisionObject(revision)};
            write("(w(s(n)))", buffer);
            authenticate();
            List values = read("w", null, false);
            return SVNNodeKind.parseKind(SVNReader.getString(values, 0));
        } catch (SVNException e) {
            closeSession();
            throw e;
        } finally {
            closeConnection();
        }
    }

    protected int getLocationsImpl(String path, long pegRevision, long[] revisions, ISVNLocationEntryHandler handler) throws SVNException {
        assertValidRevision(pegRevision);
        for (int i = 0; i < revisions.length; i++) {
            assertValidRevision(revisions[i]);
        }
        int count = 0;
        try {
            openConnection();
            path = getLocationRelativePath(path);
            Object[] buffer = new Object[]{"get-locations", path, getRevisionObject(pegRevision), revisions};
            write("(w(sn(*n)))", buffer);
            authenticate();

            while (true) {
                SVNItem item = readItem(false);
                if (item.getKind() == SVNItem.WORD && "done".equals(item.getWord())) {
                    break;
                } else if (item.getKind() != SVNItem.LIST) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, "Location entry not a list");
                    SVNErrorManager.error(err, SVNLogType.NETWORK);
                } else {
                    List values = SVNReader.parseTuple("rs", item.getItems(), null);
                    count++;
                    if (handler != null) {
                        long revision = SVNReader.getLong(values, 0);
                        String locationPath = SVNReader.getString(values, 1);
                        if (locationPath != null) {
                            locationPath = ensureAbsolutePath(locationPath);
                            handler.handleLocationEntry(new SVNLocationEntry(revision, locationPath));
                        }
                    }
                }
            }

            read("", null, false);
        } catch (SVNException e) {
            closeSession();
            handleUnsupportedCommand(e, "'get-locations' not implemented");
        } finally {
            closeConnection();
        }
        return count;
    }

    protected long getLocationSegmentsImpl(String path, long pegRevision, long startRevision, long endRevision, ISVNLocationSegmentHandler handler) throws SVNException {
        long count = 0;
        try {
            openConnection();
            path = getLocationRelativePath(path);
            Object[] buffer = new Object[] { "get-location-segments", path, getRevisionObject(pegRevision), 
                    getRevisionObject(startRevision), getRevisionObject(endRevision) };
            write("(w(s(n)(n)(n)))", buffer);
            authenticate();
            boolean isDone = false;
            while (!isDone) {
                SVNItem item = readItem(false);
                if (item.getKind() == SVNItem.WORD && "done".equals(item.getWord())) {
                    isDone = true;
                } else if (item.getKind() != SVNItem.LIST) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, 
                            "Location segment entry not a list");
                    SVNErrorManager.error(err, SVNLogType.NETWORK);
                } else {
                    List segmentAttrs = SVNReader.parseTuple("rr(?s)", item.getItems(), null);
                    long rangeStartRevision = SVNReader.getLong(segmentAttrs, 0);
                    long rangeEndRevision = SVNReader.getLong(segmentAttrs, 1);
                    String rangePath = SVNReader.getString(segmentAttrs, 2);
                    if (SVNRepository.isInvalidRevision(rangeStartRevision) || 
                            SVNRepository.isInvalidRevision(rangeEndRevision)) {
                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, 
                                "Expected valid revision range");
                        SVNErrorManager.error(err, SVNLogType.NETWORK);
                    }
                    if (rangePath != null) {
                        rangePath = ensureAbsolutePath(rangePath);    
                    }
                    if (handler != null) {
                        handler.handleLocationSegment(new SVNLocationSegment(rangeStartRevision, rangeEndRevision, rangePath));
                    }
                    count += rangeEndRevision - rangeStartRevision + 1;
                }
            }
            read("", null, false);
        } catch (SVNException e) {
            closeSession();
            handleUnsupportedCommand(e, "'get-location-segments' not implemented");
        } finally {
            closeConnection();
        }
        return count;
    }

    public long getFile(String path, long revision, SVNProperties properties, OutputStream contents) throws SVNException {
        Long rev = revision > 0 ? new Long(revision) : null;
        try {
            openConnection();
            Object[] buffer = new Object[]{"get-file", getLocationRelativePath(path), rev,
                    Boolean.valueOf(properties != null), Boolean.valueOf(contents != null)};
            write("(w(s(n)ww))", buffer);
            authenticate();
            List values = read("(?s)rl", null, false);

            MessageDigest digest = null;
            try {
                digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "MD5 implementation not found: {0}", e.getMessage());
                SVNErrorManager.error(err, e, SVNLogType.NETWORK);
            }
            String expectedChecksum = SVNReader.getString(values, 0);

            if (properties != null) {
                properties = SVNReader.getProperties(values, 2, properties);
                properties.put(SVNProperty.REVISION, SVNReader.getString(values, 1));
                properties.put(SVNProperty.CHECKSUM, expectedChecksum);
            }
            if (contents != null) {
                while (true) {
                    SVNItem item = readItem(false);
                    if (item.getKind() != SVNItem.BYTES) {
                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, "Non-string as part of file contents");
                        SVNErrorManager.error(err, SVNLogType.NETWORK);
                    }
                    if (item.getBytes().length == 0) {
                        break;
                    }
                    if (expectedChecksum != null) {
                        digest.update(item.getBytes());
                    }
                    try {
                        contents.write(item.getBytes());
                    } catch (IOException e) {
                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_IO_ERROR, e.getMessage());
                        SVNErrorManager.error(err, SVNLogType.NETWORK);
                    }
                }
                read("", null, false);
                if (expectedChecksum != null) {
                    String resultChecksum = SVNFileUtil.toHexDigest(digest);
                    if (!expectedChecksum.equals(resultChecksum)) {
                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CHECKSUM_MISMATCH, "Checksum mismatch for ''{0}''\nexpected checksum: ''{1}''\nactual checksum: ''{2}''", new Object[]{path, expectedChecksum, resultChecksum});
                        SVNErrorManager.error(err, SVNLogType.NETWORK);
                    }
                }
            }
            return SVNReader.getLong(values, 1);
        } catch (SVNException e) {
            closeSession();
            throw e;
        } finally {
            closeConnection();
        }
    }

    public long getDir(String path, long revision, SVNProperties properties, final ISVNDirEntryHandler handler) throws SVNException {
        return getDir(path, revision, properties, SVNDirEntry.DIRENT_ALL, handler);
    }

    public long getDir(String path, long revision, SVNProperties properties, int entryFields, final ISVNDirEntryHandler handler) throws SVNException {
        Long rev = getRevisionObject(revision);
        try {
            openConnection();

            String fullPath = getFullPath(path);
            final SVNURL url = getLocation().setPath(fullPath, false);
            path = getLocationRelativePath(path);

            List individualProps = new LinkedList();
            if ((entryFields & SVNDirEntry.DIRENT_KIND) != 0) {
                individualProps.add(DIRENT_KIND);
            }
            if ((entryFields & SVNDirEntry.DIRENT_SIZE) != 0) {
                individualProps.add(DIRENT_SIZE);
            }
            if ((entryFields & SVNDirEntry.DIRENT_HAS_PROPERTIES) != 0) {
                individualProps.add(DIRENT_HAS_PROPS);
            }
            if ((entryFields & SVNDirEntry.DIRENT_CREATED_REVISION) != 0) {
                individualProps.add(DIRENT_CREATED_REV);
            }
            if ((entryFields & SVNDirEntry.DIRENT_TIME) != 0) {
                individualProps.add(DIRENT_TIME);
            }
            if ((entryFields & SVNDirEntry.DIRENT_LAST_AUTHOR) != 0) {
                individualProps.add(DIRENT_LAST_AUTHOR);
            }

            Object[] buffer = new Object[]{"get-dir", path, rev,
                    Boolean.valueOf(properties != null),
                    Boolean.valueOf(handler != null),
                    individualProps.size() > 0 ?
                            (String[]) individualProps.toArray(new String[individualProps.size()]) :
                            null};
            write("(w(s(n)ww(*w)))", buffer);
            authenticate();
            List values = read("rll", null, false);
            revision = values.get(0) != null ? SVNReader.getLong(values, 0) : revision;

            if (properties != null) {
                SVNReader.getProperties(values, 1, properties);
            }

            if (handler != null) {
                SVNURL repositoryRoot = getRepositoryRoot(false);
                List dirents = (List) values.get(2);
                for (Iterator iterator = dirents.iterator(); iterator.hasNext();) {
                    SVNItem item = (SVNItem) iterator.next();
                    if (item.getKind() != SVNItem.LIST) {
                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, "Dirlist element not a list");
                        SVNErrorManager.error(err, SVNLogType.NETWORK);
                    }
                    List direntProps = SVNReader.parseTuple("swnsr(?s)(?s)", item.getItems(), null);
                    String name = SVNReader.getString(direntProps, 0);
                    SVNNodeKind kind = SVNNodeKind.parseKind(SVNReader.getString(direntProps, 1));
                    long size = SVNReader.getLong(direntProps, 2);
                    boolean hasProps = SVNReader.getBoolean(direntProps, 3);
                    long createdRevision = SVNReader.getLong(direntProps, 4);
                    Date createdDate = SVNDate.parseDate(SVNReader.getString(direntProps, 5));
                    String lastAuthor = SVNReader.getString(direntProps, 6);
                    handler.handleDirEntry(new SVNDirEntry(url.appendPath(name, false), repositoryRoot, 
                            "".equals(name) ? SVNPathUtil.tail(url.getPath()) : name, kind, size, hasProps, createdRevision, createdDate, lastAuthor));
                }
            }
        } catch (SVNException e) {
            closeSession();
            throw e;
        } finally {
            closeConnection();
        }
        return revision;
    }

    public SVNDirEntry getDir(String path, long revision, boolean includeComment, final Collection entries) throws SVNException {
        Long rev = getRevisionObject(revision);
        // convert path to path relative to repos root.
        SVNDirEntry parentEntry = null;
        try {
            openConnection();
            final SVNURL url = getLocation().setPath(getFullPath(path), false);
            final SVNURL repositoryRoot = getRepositoryRoot(false);
            ISVNDirEntryHandler handler = new ISVNDirEntryHandler() {
                public void handleDirEntry(SVNDirEntry dirEntry) throws SVNException {
                    if (entries != null) {
                        dirEntry = new SVNDirEntry(url.appendPath(dirEntry.getName(), false), repositoryRoot, dirEntry.getName(),
                                dirEntry.getKind(), dirEntry.getSize(), dirEntry.hasProperties(), dirEntry.getRevision(), dirEntry.getDate(), dirEntry.getAuthor());
                        entries.add(dirEntry);
                    }
                }
            };
            path = getLocationRelativePath(path);
            // get parent
            Object[] buffer = new Object[]{"stat", path, getRevisionObject(revision)};
            write("(w(s(n)))", buffer);
            authenticate();
            List values = read("(?l)", null, false);
            values = (List) values.get(0);
            if (values != null) {
                List direntProps = SVNReader.parseTuple("wnsr(?s)(?s)", values, null);
                SVNNodeKind kind = SVNNodeKind.parseKind(SVNReader.getString(direntProps, 0));
                long size = SVNReader.getLong(direntProps, 1);
                boolean hasProps = SVNReader.getBoolean(direntProps, 2);
                long createdRevision = SVNReader.getLong(direntProps, 3);
                Date createdDate = SVNDate.parseDate(SVNReader.getString(direntProps, 4));
                String lastAuthor = SVNReader.getString(direntProps, 5);
                parentEntry = new SVNDirEntry(url, repositoryRoot, "", kind, size, hasProps, createdRevision, createdDate, lastAuthor);
            }

            // get entries.
            buffer = new Object[]{"get-dir", path, rev, Boolean.FALSE, Boolean.TRUE};
            write("(w(s(n)ww))", buffer);
            authenticate();
            values = read("rll", null, false);
            revision = values.get(0) != null ? SVNReader.getLong(values, 0) : revision;

            if (handler != null) {
                List dirents = (List) values.get(2);
                for (Iterator iterator = dirents.iterator(); iterator.hasNext();) {
                    SVNItem item = (SVNItem) iterator.next();
                    if (item.getKind() != SVNItem.LIST) {
                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, "Dirlist element not a list");
                        SVNErrorManager.error(err, SVNLogType.NETWORK);
                    }
                    List direntProps = SVNReader.parseTuple("swnsr(?s)(?s)", item.getItems(), null);
                    String name = SVNReader.getString(direntProps, 0);
                    SVNNodeKind kind = SVNNodeKind.parseKind(SVNReader.getString(direntProps, 1));
                    long size = SVNReader.getLong(direntProps, 2);
                    boolean hasProps = SVNReader.getBoolean(direntProps, 3);
                    long createdRevision = SVNReader.getLong(direntProps, 4);
                    Date createdDate = SVNDate.parseDate(SVNReader.getString(direntProps, 5));
                    String lastAuthor = SVNReader.getString(direntProps, 6);
                    handler.handleDirEntry(new SVNDirEntry(url.appendPath(name, false), repositoryRoot, name, kind, size, hasProps, createdRevision, createdDate, lastAuthor));
                }
            }

            // get comments.
            if (includeComment && entries != null) {
                Map messages = new SVNHashMap();
                for (Iterator ents = entries.iterator(); ents.hasNext();) {
                    SVNDirEntry entry = (SVNDirEntry) ents.next();
                    Long key = getRevisionObject(entry.getRevision());
                    if (messages.containsKey(key)) {
                        entry.setCommitMessage((String) messages.get(key));
                        continue;
                    }
                    buffer = new Object[]{"rev-prop", key, SVNRevisionProperty.LOG};
                    write("(w(ns))", buffer);
                    authenticate();
                    values = read("(?s)", null, false);
                    String msg = SVNReader.getString(values, 0);
                    messages.put(key, msg);
                    entry.setCommitMessage(msg);
                }
            }
        } catch (SVNException e) {
            closeSession();
            throw e;
        } finally {
            closeConnection();
        }
        return parentEntry;
    }

    protected int getFileRevisionsImpl(String path, long startRevision, long endRevision, boolean includeMergedRevisions,
                                ISVNFileRevisionHandler handler) throws SVNException {
        Long srev = getRevisionObject(startRevision);
        Long erev = getRevisionObject(endRevision);
        SVNDeltaReader deltaReader = new SVNDeltaReader();
        try {
            openConnection();
            Object[] buffer = new Object[]{"get-file-revs",
                    getLocationRelativePath(path),
                    srev, erev, Boolean.toString(includeMergedRevisions)};
            write("(w(s(n)(n)w))", buffer);
            authenticate();
            boolean hasRevision = false;
            int count = 0;
            while (true) {
                SVNItem item = readItem(false);
                if (item.getKind() == SVNItem.WORD && "done".equals(item.getWord())) {
                    break;
                }
                hasRevision = true;
                if (item.getKind() != SVNItem.LIST) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, "Revision entry not a list");
                    SVNErrorManager.error(err, SVNLogType.NETWORK);
                }
                List items = SVNReader.parseTuple("srll?s", item.getItems(), null);
                String name = null;
                SVNFileRevision fileRevision = null;
                if (handler != null) {
                    name = SVNReader.getString(items, 0);
                    long revision = SVNReader.getLong(items, 1);
                    SVNProperties properties = SVNReader.getProperties(items, 2, null);
                    SVNProperties propertiesDelta = SVNReader.getPropertyDiffs(items, 3, null);
                    boolean isMergedRevision = SVNReader.getBoolean(items, 4);

                    if (name != null) {
                        fileRevision = new SVNFileRevision(name, revision,
                                properties, propertiesDelta,
                                isMergedRevision);
                    }
                }

                SVNItem chunkItem = readItem(false);
                if (chunkItem.getKind() != SVNItem.BYTES) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, "Text delta chunk not a string");
                    SVNErrorManager.error(err, SVNLogType.NETWORK);
                }
                boolean hasDelta = chunkItem.getBytes().length > 0;

                if (handler != null && fileRevision != null) {
                    handler.openRevision(fileRevision);
                }

                if (hasDelta) {
                    if (handler != null) {
                        handler.applyTextDelta(name == null ? path : name, null);
                    }
                    while (true) {
                        byte[] line = chunkItem.getBytes();
                        if (line == null || line.length == 0) {
                            break;
                        }
                        deltaReader.nextWindow(line, 0, line.length, name == null ? path : name, handler);
                        chunkItem = readItem(false);
                        if (chunkItem.getKind() != SVNItem.BYTES) {
                            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, "Text delta chunk not a string");
                            SVNErrorManager.error(err, SVNLogType.NETWORK);
                        }
                    }
                    deltaReader.reset(name == null ? path : name, handler);
                    if (handler != null) {
                        handler.textDeltaEnd(name == null ? path : name);
                    }

                }
                if (handler != null) {
                    handler.closeRevision(name == null ? path : name);
                    count++;
                }
            }
            read("", null, false);

            if (!hasRevision) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, "The get-file-revs command didn't return any revisions");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }
            return count;
        } catch (SVNException e) {
            closeSession();
            handleUnsupportedCommand(e, "'get-file-revs' not implemented");
        } finally {
            closeConnection();
        }
        return -1;
    }

    protected long logImpl(String[] targetPaths, long startRevision, long endRevision,
                    boolean changedPaths, boolean strictNode, long limit,
                    boolean includeMergedRevisions, String[] revisionPropertyNames,
                    ISVNLogEntryHandler handler) throws SVNException {

        long count = 0;
        int nestLevel = 0;

        long latestRev = -1;
        if (isInvalidRevision(startRevision)) {
            startRevision = latestRev = getLatestRevision();
        }
        if (isInvalidRevision(endRevision)) {
            endRevision = latestRev != -1 ? latestRev : getLatestRevision();
        }

        try {
            openConnection();
            String[] repositoryPaths = getRepositoryPaths(targetPaths);
            if (repositoryPaths == null || repositoryPaths.length == 0) {
                repositoryPaths = new String[]{""};
            }
            if (repositoryPaths.length == 1 && "/".equals(repositoryPaths[0])) {
                repositoryPaths[0] = "";
            }

            Object[] buffer;
            boolean wantCustomRevProps = false;
            if (revisionPropertyNames != null && revisionPropertyNames.length > 0) {
                Object[] realBuffer = new Object[]{"log", repositoryPaths, getRevisionObject(startRevision),
                        getRevisionObject(endRevision), Boolean.valueOf(changedPaths),
                        Boolean.valueOf(strictNode), new Long(limit > 0 ? limit : 0),
                        Boolean.valueOf(includeMergedRevisions), "revprops", revisionPropertyNames};
                for (int i = 0; i < revisionPropertyNames.length; i++) {
                    String propName = revisionPropertyNames[i];
                    if (!SVNRevisionProperty.AUTHOR.equals(propName) &&
                            !SVNRevisionProperty.DATE.equals(propName) &&
                            !SVNRevisionProperty.LOG.equals(propName)) {
                        wantCustomRevProps = true;
                        break;
                    }
                }
                buffer = realBuffer;
                write("(w((*s)(n)(n)wwnww(*s)))", buffer);
            } else {
                buffer = new Object[]{"log",
                        repositoryPaths, getRevisionObject(startRevision), getRevisionObject(endRevision),
                        Boolean.valueOf(changedPaths), Boolean.valueOf(strictNode), new Long(limit > 0 ? limit : 0),
                        Boolean.valueOf(includeMergedRevisions), "all-revprops"};

                write("(w((*s)(n)(n)wwnww()))", buffer);
            }
            authenticate();


            while (true) {
                SVNItem item = readItem(false);
                if (item.getKind() == SVNItem.WORD && "done".equals(item.getWord())) {
                    break;
                }
                if (item.getKind() != SVNItem.LIST) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, "Log entry not a list");
                    SVNErrorManager.error(err, SVNLogType.NETWORK);
                }

                //now we read log response kind of
                // ( ( ) 1 ( ) ( 27:2008-04-02T13:32:15.165405Z ) ( 27:Log message for revision 1. ) false false 0 ( ) )
                // paths  athr                               date                            log msg hasChrn invR  rProps
                //     0 1   2                                  3                                  4     5     6 7   8

                List items = SVNReader.parseTuple("lr(?s)(?s)(?s)?ssnl?s", item.getItems(), null);
                List changedPathsList = (List) items.get(0);
                Map changedPathsMap = new SVNHashMap();
                if (changedPathsList != null && changedPathsList.size() > 0) {
                    for (Iterator iterator = changedPathsList.iterator(); iterator.hasNext();) {
                        SVNItem pathItem = (SVNItem) iterator.next();
                        if (pathItem.getKind() != SVNItem.LIST) {
                            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, "Changed-path entry not a list");
                            SVNErrorManager.error(err, SVNLogType.NETWORK);
                        }
                        List pathItems = SVNReader.parseTuple("sw(?sr)?(?s)", pathItem.getItems(), null);
                        String path = SVNReader.getString(pathItems, 0);
                        String action = SVNReader.getString(pathItems, 1);
                        String copyPath = SVNReader.getString(pathItems, 2);
                        long copyRevision = SVNReader.getLong(pathItems, 3);
                        String kind = SVNReader.getString(pathItems, 4);
                        changedPathsMap.put(path, new SVNLogEntryPath(path, action.charAt(0), copyPath, copyRevision, kind != null ? SVNNodeKind.parseKind(kind) : SVNNodeKind.UNKNOWN));
                    }
                }
                if (nestLevel == 0) {
                    count++;
                }
                long revision = 0;
                SVNProperties revisionProperties = null;
                SVNProperties logEntryProperties = new SVNProperties();
                boolean hasChildren = false;
                boolean isSubtractiveMerge = false;
                if (handler != null && !(limit > 0 && count > limit && nestLevel == 0)) {
                    revision = SVNReader.getLong(items, 1);
                    String author = SVNReader.getString(items, 2);
                    Date date = SVNReader.getDate(items, 3);
                    if (date == SVNDate.NULL) {
                        date = null;
                    }
                    String message = SVNReader.getString(items, 4);
                    hasChildren = SVNReader.getBoolean(items, 5);
                    boolean invalidRevision = SVNReader.getBoolean(items, 6);
                    revisionProperties = SVNReader.getProperties(items, 8, null);
                    if (invalidRevision) {
                        revision = SVNRepository.INVALID_REVISION;
                    }
                    isSubtractiveMerge =SVNReader.getBoolean(items, 9);
                    if (wantCustomRevProps && (revisionProperties == null)) {
                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_NOT_IMPLEMENTED, "Server does not support custom revprops via log");
                        SVNErrorManager.error(err, SVNLogType.NETWORK);
                    }

                    if (revisionProperties != null) {
                        for (Iterator iterator = revisionProperties.nameSet().iterator(); iterator.hasNext();) {
                            String name = (String) iterator.next();
                            logEntryProperties.put(name, revisionProperties.getSVNPropertyValue(name));
                        }
                    }

                    if (revisionPropertyNames == null || revisionPropertyNames.length == 0) {
                        if (author != null) {
                            logEntryProperties.put(SVNRevisionProperty.AUTHOR, author);
                        }
                        if (date != null) {
                            logEntryProperties.put(SVNRevisionProperty.DATE, SVNDate.formatDate(date));
                        }
                        if (message != null) {
                            logEntryProperties.put(SVNRevisionProperty.LOG, message);
                        }
                    } else {
                        for (int i = 0; i < revisionPropertyNames.length; i++) {
                            String revPropName = revisionPropertyNames[i];
                            if (author != null && SVNRevisionProperty.AUTHOR.equals(revPropName)) {
                                logEntryProperties.put(SVNRevisionProperty.AUTHOR, author);
                            }
                            if (date != null && SVNRevisionProperty.DATE.equals(revPropName)) {
                                logEntryProperties.put(SVNRevisionProperty.DATE, SVNDate.formatDate(date));
                            }
                            if (message != null && SVNRevisionProperty.LOG.equals(revPropName)) {
                                logEntryProperties.put(SVNRevisionProperty.LOG, message);
                            }
                        }
                    }
                }
                if (handler != null && !(limit > 0 && count > limit && nestLevel == 0)) {
                    SVNLogEntry logEntry = new SVNLogEntry(changedPathsMap, revision, logEntryProperties, hasChildren);
                    logEntry.setSubtractiveMerge(isSubtractiveMerge);
                    handler.handleLogEntry(logEntry);
                    if (logEntry.hasChildren()) {
                        nestLevel++;
                    }
                    if (logEntry.getRevision() < 0) {
                        nestLevel--;
                        if (nestLevel < 0) {
                            nestLevel = 0;
                        }
                    }
                }
            }
            read("", null, false);
            return count;
        } catch (SVNException e) {
            closeSession();
            throw e;
        } finally {
            closeConnection();
        }
    }

    public void replay(long lowRevision, long highRevision, boolean sendDeltas, ISVNEditor editor) throws SVNException {
        Object[] buffer = new Object[]{"replay", getRevisionObject(highRevision), getRevisionObject(lowRevision), Boolean.valueOf(sendDeltas)};
        try {
            openConnection();
            write("(w(nnw))", buffer);
            authenticate();
            SVNEditModeReader editReader = new SVNEditModeReader(myConnection, editor, true);
            editReader.driveEditor();
            read("", null, false);
        } catch (SVNException e) {
            closeSession();
            handleUnsupportedCommand(e, "Server doesn't support the replay command");
        } finally {
            closeConnection();
        }
    }

    public void setRevisionPropertyValue(long revision, String propertyName, SVNPropertyValue propertyValue) throws SVNException {
        assertValidRevision(revision);
        byte[] bytes = SVNPropertyValue.getPropertyAsBytes(propertyValue);        
        Object[] buffer = new Object[]{"change-rev-prop",
                getRevisionObject(revision), propertyName, bytes};
        try {
            openConnection();
            write("(w(nsb))", buffer);
            authenticate();
            read("", null, false);
        } catch (SVNException e) {
            closeSession();
            throw e;
        } finally {
            closeConnection();
        }
    }

    public ISVNEditor getCommitEditor(String logMessage, Map locks, boolean keepLocks, final ISVNWorkspaceMediator mediator) throws SVNException {
        try {
            openConnection();
            if (locks != null) {
                write("(w(s(*l)w))", new Object[]{"commit", logMessage, locks, Boolean.valueOf(keepLocks)});
            } else {
                write("(w(s))", new Object[]{"commit", logMessage});
            }
            authenticate();
            read("", null, false);
            return new SVNCommitEditor(this, myConnection, new SVNCommitEditor.ISVNCommitCallback() {
                public void run(SVNException error) {
                    if (error != null) {
                        closeSession();
                    }
                    closeConnection();
                }
            });
        } catch (SVNException e) {
            closeSession();
            closeConnection();
            throw e;
        }
    }

    public SVNLock getLock(String path) throws SVNException {
        try {
            openConnection();
            path = getLocationRelativePath(path);
            Object[] buffer = new Object[]{"get-lock", path};
            write("(w(s))", buffer);
            authenticate();
            List items = read("(?l)", null, false);
            items = (List) items.get(0);
            return SVNReader.getLock(items);
        } catch (SVNException e) {
            closeSession();
            handleUnsupportedCommand(e, "Server doesn't support the get-lock command");
        } finally {
            closeConnection();
        }
        return null;
    }

    public SVNLock[] getLocks(String path) throws SVNException {
        try {
            openConnection();
            path = getLocationRelativePath(path);
            Object[] buffer = new Object[]{"get-locks", path};
            write("(w(s))", buffer);
            authenticate();
            List items = read("l", null, false);
            items = (List) items.get(0);
            Collection locks = new ArrayList();
            for (Iterator iterator = items.iterator(); iterator.hasNext();) {
                SVNItem item = (SVNItem) iterator.next();
                if (item.getKind() != SVNItem.LIST) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, "Lock element not a list");
                    SVNErrorManager.error(err, SVNLogType.NETWORK);
                }
                SVNLock lock = SVNReader.getLock(item.getItems());
                if (lock != null) {
                    locks.add(lock);
                }
            }
            return (SVNLock[]) locks.toArray(new SVNLock[locks.size()]);
        } catch (SVNException e) {
            closeSession();
            handleUnsupportedCommand(e, "Server doesn't support the get-lock command");
        } finally {
            closeConnection();
        }
        return null;
    }

    public void lock(Map pathsToRevisions, String comment, boolean force, ISVNLockHandler handler) throws SVNException {
        try {
            openConnection();
            Object[] buffer = new Object[]{"lock-many", comment, Boolean.valueOf(force)};
            write("(w((s)w(", buffer);
            buffer = new Object[2];
            for (Iterator paths = pathsToRevisions.keySet().iterator(); paths.hasNext();) {
                buffer[0] = getLocationRelativePath((String) paths.next());
                buffer[1] = pathsToRevisions.get(buffer[0]);
                write("(s(n))", buffer);
            }
            write(")))", buffer);
            try {
                authenticate();
            } catch (SVNException e) {
                if (e.getErrorMessage() != null && e.getErrorMessage().getErrorCode() == SVNErrorCode.RA_SVN_UNKNOWN_CMD) {
                    closeSession();
                    closeConnection();
                    openConnection();
                    lock12(pathsToRevisions, comment, force, handler);
                    return;
                }
                closeSession();
                throw e;
            }
            boolean done = false;
            for (Iterator paths = pathsToRevisions.keySet().iterator(); paths.hasNext();) {
                String path = (String) paths.next();
                SVNLock lock = null;
                SVNErrorMessage error = null;
                SVNItem item = readItem(false);
                if (item.getKind() == SVNItem.WORD && "done".equals(item.getWord())) {
                    done = true;
                    break;
                }
                if (item.getKind() != SVNItem.LIST) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, "Lock response not a list");
                    SVNErrorManager.error(err, SVNLogType.NETWORK);
                }
                try {
                    List values = SVNReader.parseTuple("wl", item.getItems(), null);
                    String status = SVNReader.getString(values, 0);
                    List items = (List) values.get(1);
                    if ("success".equals(status)) {
                        lock = SVNReader.getLock(items);
                        if (lock == null) {
                            continue;
                        }
                        path = lock.getPath();
                    } else if ("failure".equals(status)) {
                        SVNReader.handleFailureStatus(items);
                    } else {
                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA);
                        SVNErrorManager.error(err, SVNLogType.NETWORK);
                    }
                } catch (SVNException e) {
                    path = getRepositoryPath(path);
                    error = e.getErrorMessage();
                }
                if (handler != null) {
                    handler.handleLock(path, lock, error);
                }
            }
            if (!done) {
                SVNItem item = readItem(false);
                if (item.getKind() != SVNItem.WORD || !"done".equals(item.getWord())) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, "Didn't receive end marker for lock responses");
                    SVNErrorManager.error(err, SVNLogType.NETWORK);
                }
            }
            read("", null, false);
        } catch (SVNException e) {
            closeSession();
            handleUnsupportedCommand(e, "Server doesn't support the lock command");
        } finally {
            closeConnection();
        }
    }

    private void lock12(Map pathsToRevisions, String comment, boolean force, ISVNLockHandler handler) throws SVNException {
        for (Iterator paths = pathsToRevisions.keySet().iterator(); paths.hasNext();) {
            String path = (String) paths.next();
            Long revision = (Long) pathsToRevisions.get(path);
            path = getLocationRelativePath(path);
            Object[] buffer = new Object[]{"lock", path, comment, Boolean.valueOf(force), revision};
            write("(w(s(s)w(n)))", buffer);
            authenticate();
            SVNErrorMessage error = null;
            List items = null;
            try {
                items = read("l", null, false);
                items = (List) items.get(0);
            } catch (SVNException e) {
                if (e.getErrorMessage() != null) {
                    SVNErrorCode code = e.getErrorMessage().getErrorCode();
                    if (code == SVNErrorCode.FS_PATH_ALREADY_LOCKED || code == SVNErrorCode.FS_OUT_OF_DATE) {
                        error = e.getErrorMessage();
                    }
                }
                if (error == null) {
                    throw e;
                }
            }
            if (handler != null) {
                SVNLock lock = items == null ? null : SVNReader.getLock(items);
                if (lock != null) {
                    path = lock.getPath();
                } else {
                    path = getRepositoryPath(path);
                }
                handler.handleLock(path, lock, error);
            }
        }
    }

    public void unlock(Map pathToTokens, boolean force, ISVNLockHandler handler) throws SVNException {
        try {
            openConnection();
            Object[] buffer = new Object[]{"unlock-many", Boolean.valueOf(force)};
            write("(w(w(", buffer);
            buffer = new Object[2];
            for (Iterator paths = pathToTokens.keySet().iterator(); paths.hasNext();) {
                buffer[0] = paths.next();
                buffer[1] = pathToTokens.get(buffer[0]);
                write("(s(s))", buffer);
            }
            write(")))", buffer);
            try {
                authenticate();
            } catch (SVNException e) {
                if (e.getErrorMessage() != null && e.getErrorMessage().getErrorCode() == SVNErrorCode.RA_SVN_UNKNOWN_CMD) {
                    closeSession();
                    closeConnection();
                    openConnection();
                    unlock12(pathToTokens, force, handler);
                    return;
                }
                throw e;
            }
            boolean done = false;
            for (Iterator paths = pathToTokens.keySet().iterator(); paths.hasNext();) {
                String path = (String) paths.next();
                String id = (String) pathToTokens.get(path);
                SVNErrorMessage error = null;
                try {
                    SVNItem item = readItem(false);
                    if (item.getKind() == SVNItem.WORD && "done".equals(item.getWord())) {
                        done = true;
                        break;
                    }
                    if (item.getKind() != SVNItem.LIST) {
                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, "Unlock response not a list");
                        SVNErrorManager.error(err, SVNLogType.NETWORK);
                    }
                    List values = SVNReader.parseTuple("wl", item.getItems(), null);
                    String status = SVNReader.getString(values, 0);
                    List items = (List) values.get(1);
                    if ("success".equals(status)) {
                        values = SVNReader.parseTuple("s", items, null);
                        path = SVNReader.getString(values, 0);
                    } else if ("failure".equals(status)) {
                        SVNReader.handleFailureStatus(items);
                    } else {
                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA);
                        SVNErrorManager.error(err, SVNLogType.NETWORK);
                    }
                } catch (SVNException e) {
                    error = e.getErrorMessage();
                }
                path = getRepositoryPath(path);
                if (handler != null) {
                    handler.handleUnlock(path, new SVNLock(path, id, null, null, null, null), error);
                }
            }
            if (!done) {
                SVNItem item = readItem(false);
                if (item.getKind() != SVNItem.WORD || !"done".equals(item.getWord())) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, "Didn't receive end marker for unlock responses");
                    SVNErrorManager.error(err, SVNLogType.NETWORK);
                }
            }
            read("", null, false);
        } catch (SVNException e) {
            closeSession();
            handleUnsupportedCommand(e, "Server doesn't support the unlock command");
        } finally {
            closeConnection();
        }
    }

    private void unlock12(Map pathToTokens, boolean force, ISVNLockHandler handler) throws SVNException {
        for (Iterator paths = pathToTokens.keySet().iterator(); paths.hasNext();) {
            String path = (String) paths.next();
            String id = (String) pathToTokens.get(path);
            path = getLocationRelativePath(path);
            if (id == null) {
                Object[] buffer = new Object[]{"get-lock", path};
                write("(w(s))", buffer);
                authenticate();
                List items = read("l", null, false);
                items = (List) items.get(0);
                SVNLock lock = SVNReader.getLock(items);
                if (lock == null) {
                    lock = new SVNLock(path, "", null, null, null, null);
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_NOT_LOCKED, "No lock on path ''{0}''", path);
                    if (handler != null) {
                        handler.handleUnlock(path, lock, err);
                    }
                    continue;
                }
                id = lock.getID();
            }
            Object[] buffer = new Object[]{"unlock", path, id, Boolean.valueOf(force)};
            write("(w(s(s)w))", buffer);
            authenticate();
            SVNErrorMessage error = null;
            try {
                read("", null, false);
            } catch (SVNException e) {
                if (e.getErrorMessage() != null && e.getErrorMessage().getErrorCode() == SVNErrorCode.RA_NOT_LOCKED) {
                    error = e.getErrorMessage();
                    error = SVNErrorMessage.create(error.getErrorCode(), error.getMessageTemplate(), path);
                } else {
                    throw e;
                }
            }
            if (handler != null) {
                path = getRepositoryPath(path);
                SVNLock lock = new SVNLock(path, id, null, null, null, null);
                handler.handleUnlock(path, lock, error);
            }
        }
    }

    public SVNDirEntry info(String path, long revision) throws SVNException {
        try {
            openConnection();
            String fullPath = getFullPath(path);
            SVNURL url = getLocation().setPath(fullPath, false);
            path = getLocationRelativePath(path);
            Object[] buffer = new Object[]{"stat", path, getRevisionObject(revision)};
            write("(w(s(n)))", buffer);
            authenticate();
            SVNDirEntry entry = null;
            List items = read("(?l)", null, false);
            if (items == null || items.isEmpty()) {
                return null;
            }
            items = (List) items.get(0);
            if (items != null && !items.isEmpty()) {
                SVNURL repositoryRoot = getRepositoryRoot(false);
                List values = SVNReader.parseTuple("wnsr(?s)(?s)", items, null);
                SVNNodeKind kind = SVNNodeKind.parseKind(SVNReader.getString(values, 0));
                long size = SVNReader.getLong(values, 1);
                boolean hasProperties = SVNReader.getBoolean(values, 2);
                long createdRevision = SVNReader.getLong(values, 3);
                Date createdDate = SVNDate.parseDate(SVNReader.getString(values, 4));
                String lastAuthor = SVNReader.getString(values, 5);
                entry = new SVNDirEntry(url, repositoryRoot, "".equals(path) ? SVNPathUtil.tail(getLocation().getPath()) : SVNPathUtil.tail(path), kind, size, hasProperties, createdRevision, createdDate, lastAuthor);
            }
            return entry;
        } catch (SVNException e) {
            closeSession();
            handleUnsupportedCommand(e, "'stat' not implemented");
        } finally {
            closeConnection();
        }
        return null;
    }

    void updateCredentials(String uuid, SVNURL rootURL) throws SVNException {
        if (getRepositoryRoot(false) != null) {
            return;
        }
        setRepositoryCredentials(uuid, rootURL);
    }

    protected void openConnection() throws SVNException {
        fireConnectionOpened();
        lock();
        // check if connection is stale.
        if (myConnection != null && myConnection.isConnectionStale()) {
            closeSession();
        }
        if (myConnection != null) {
            if (reparent(getLocation())) {
                return;
            }
            closeSession();
        }
        ISVNConnector connector = SVNRepositoryFactoryImpl.getConnectorFactory().createConnector(this);
        myConnection = new SVNConnection(connector, this);
        try {
            myConnection.open(this);
            authenticate();
        } finally {
            if (myConnection != null) {
                myRealm = myConnection.getRealm();
            }
        }
    }

    protected void closeConnection() {
        if (!getOptions().keepConnection(this)) {
            closeSession();
        }
        unlock();
        fireConnectionClosed();
    }

    public String getRealm() {
        return myRealm;
    }

    void authenticate() throws SVNException {
        if (myConnection != null) {
            myConnection.authenticate(this);
        }
    }

    private void write(String template, Object[] values) throws SVNException {
        if (myConnection == null) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_SVN_CONNECTION_CLOSED), SVNLogType.NETWORK);
        }
        myConnection.write(template, values);
    }

    private List read(String template, List values, boolean readMalformedData) throws SVNException {
        if (myConnection == null) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_SVN_CONNECTION_CLOSED), SVNLogType.NETWORK);
        }
        return myConnection.read(template, values, readMalformedData);
    }

    private SVNItem readItem(boolean readMalformedData) throws SVNException {
        if (myConnection == null) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_SVN_CONNECTION_CLOSED), SVNLogType.NETWORK);
        }
        return myConnection.readItem(readMalformedData);
    }

    private List readTuple(String template, boolean readMalformedData) throws SVNException {
        if (myConnection == null) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_SVN_CONNECTION_CLOSED), SVNLogType.NETWORK);
        }
        return myConnection.readTuple(template, readMalformedData);
    }

    /*
     * ISVNReporter methods
     */

    public void setPath(String path, String lockToken, long revision, boolean startEmpty) throws SVNException {
        setPath(path, lockToken, revision, SVNDepth.INFINITY, startEmpty);
    }

    public void deletePath(String path) throws SVNException {
        write("(w(s))", new Object[]{"delete-path", path});
    }

    public void linkPath(SVNURL url, String path,
                         String lockToken, long revision, boolean startEmpty)
            throws SVNException {
        linkPath(url, path, lockToken, revision, SVNDepth.INFINITY, startEmpty);
    }

    public void finishReport() throws SVNException {
        write("(w())", new Object[]{"finish-report"});
    }

    public void abortReport() throws SVNException {
        write("(w())", new Object[]{"abort-report"});
    }

    private String[] getRepositoryPaths(String[] paths) throws SVNException {
        if (paths == null || paths.length == 0) {
            return paths;
        }
        String[] fullPaths = new String[paths.length];
        for (int i = 0; i < paths.length; i++) {
            fullPaths[i] = getLocationRelativePath(paths[i]);
        }
        return fullPaths;
    }

    // all paths are uri-decoded.
    //
    // get repository path (path starting with /, relative to repository root).
    // get full path (path starting with /, relative to host).
    // get relative path (repository path, now relative to repository location, not starting with '/').

    public void setExternalUserName(String userName) {
        myExternalUserName = userName;
    }

    public String getExternalUserName() {
        return myExternalUserName;
    }

    public void closeSession() {
        lock(true);
        try {
            if (myConnection != null) {
                try {
                    myConnection.close();
                } catch (SVNException e) {
                    //
                } finally {
                    myConnection = null;
                }
            }
        } finally {
            unlock();
        }
    }

    private void handleUnsupportedCommand(SVNException e, String message) throws SVNException {
        if (e.getErrorMessage() != null && e.getErrorMessage().getErrorCode() == SVNErrorCode.RA_SVN_UNKNOWN_CMD) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_NOT_IMPLEMENTED, message);
            SVNErrorManager.error(err, e.getErrorMessage(), SVNLogType.NETWORK);
        }
        throw e;
    }

    public void linkPath(SVNURL url, String path, String lockToken, long revision, SVNDepth depth, boolean startEmpty) throws SVNException {
        assertValidRevision(revision);
        write("(w(ssnw(s)w))", new Object[]{"link-path", path,
                url.toString(), getRevisionObject(revision),
                Boolean.valueOf(startEmpty), lockToken, SVNDepth.asString(depth)});
    }

    public void setPath(String path, String lockToken, long revision, SVNDepth depth, boolean startEmpty) throws SVNException {
        assertValidRevision(revision);
        write("(w(snw(s)w))", new Object[]{"set-path", path,
                getRevisionObject(revision), Boolean.valueOf(startEmpty),
                lockToken, SVNDepth.asString(depth)});
    }

    public void diff(SVNURL url, long targetRevision, long revision, String target, boolean ignoreAncestry,
                     SVNDepth depth, boolean getContents, ISVNReporterBaton reporter, ISVNEditor editor) throws SVNException {
        boolean recursive = getRecurseFromDepth(depth);
        boolean hasTarget = target != null;
        target = target == null ? "" : target;
        if (url == null) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.BAD_URL, "URL can not be NULL"), SVNLogType.NETWORK);
        }

        editor = getDepthFilterEditor(editor, depth, hasTarget);
        Object[] buffer = new Object[]{"diff", getRevisionObject(targetRevision),
                target, Boolean.valueOf(recursive),
                Boolean.valueOf(ignoreAncestry), url.toString(),
                Boolean.valueOf(getContents), SVNDepth.asString(depth)};
        try {
            openConnection();
            write("(w((n)swwsww))", buffer);
            authenticate();
            reporter.report(this);
            authenticate();

            SVNEditModeReader editReader = new SVNEditModeReader(myConnection, editor, false);
            editReader.driveEditor();
            read("", null, false);

        } catch (SVNException e) {
            closeSession();
            throw e;
        } finally {
            closeConnection();
        }
    }

    public void status(long revision, String target, SVNDepth depth, ISVNReporterBaton reporter, ISVNEditor editor) throws SVNException {
        boolean recursive = getRecurseFromDepth(depth);
        boolean hasTarget = target != null;
        target = target == null ? "" : target;
        editor = getDepthFilterEditor(editor, depth, hasTarget);
        Object[] buffer = new Object[]{"status", target,
                Boolean.valueOf(recursive), getRevisionObject(revision), SVNDepth.asString(depth)};
        try {
            openConnection();
            write("(w(sw(n)w))", buffer);
            authenticate();
            reporter.report(this);
            authenticate();

            SVNEditModeReader editReader = new SVNEditModeReader(myConnection, editor, false);
            editReader.driveEditor();
            read("", null, false);

        } catch (SVNException e) {
            closeSession();
            throw e;
        } finally {
            closeConnection();
        }
    }

    public void update(SVNURL url, long revision, String target, SVNDepth depth, ISVNReporterBaton reporter,
                       ISVNEditor editor) throws SVNException {
        boolean recursive = getRecurseFromDepth(depth);
        boolean hasTarget = target != null;
        target = target == null ? "" : target;
        if (url == null) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.BAD_URL, "URL can not be NULL"), SVNLogType.NETWORK);
        }
        editor = getDepthFilterEditor(editor, depth, hasTarget);
        Object[] buffer = new Object[]{"switch", getRevisionObject(revision),
                target, Boolean.valueOf(recursive), url.toString(), SVNDepth.asString(depth)};
        try {
            openConnection();
            write("(w((n)swsw))", buffer);
            authenticate();
            reporter.report(this);
            authenticate();

            SVNEditModeReader editReader = new SVNEditModeReader(myConnection, editor, false);
            editReader.driveEditor();
            read("", null, false);

        } catch (SVNException e) {
            closeSession();
            throw e;
        } finally {
            closeConnection();
        }
    }

    public void update(long revision, String target, SVNDepth depth, boolean sendCopyFromArgs,
                       ISVNReporterBaton reporter, ISVNEditor editor) throws SVNException {
        boolean hasTarget = target != null;
        target = target == null ? "" : target;
        boolean recursive = getRecurseFromDepth(depth);
        editor = getDepthFilterEditor(editor, depth, hasTarget);
        Object[] buffer = new Object[]{"update", getRevisionObject(revision),
                target, Boolean.valueOf(recursive), SVNDepth.asString(depth), Boolean.valueOf(sendCopyFromArgs)};

        try {
            openConnection();
            write("(w((n)swww))", buffer);
            authenticate();
            reporter.report(this);
            authenticate();

            SVNEditModeReader editReader = new SVNEditModeReader(myConnection, editor, false);
            editReader.driveEditor();
            read("", null, false);

        } catch (SVNException e) {
            closeSession();
            throw e;
        } finally {
            closeConnection();
        }
    }

    protected Map getMergeInfoImpl(String[] paths, long revision, SVNMergeInfoInheritance inherit,
            boolean includeDescendants) throws SVNException {
        try {
            openConnection();
            String[] repositoryPaths = getRepositoryPaths(paths);
            if (repositoryPaths == null || repositoryPaths.length == 0) {
                repositoryPaths = new String[]{""};
            }

            Object[] buffer = new Object[]{"get-mergeinfo", repositoryPaths,
                    getRevisionObject(revision), inherit.toString(), Boolean.valueOf(includeDescendants)};
            write("(w((*s)(n)ww))", buffer);
            authenticate();

            List items = read("l", null, false);
            items = (List) items.get(0);
            Map pathsToMergeInfos = new SVNHashMap();
            for (Iterator iterator = items.iterator(); iterator.hasNext();) {
                SVNItem item = (SVNItem) iterator.next();
                if (item.getKind() != SVNItem.LIST) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, "Merge info element is not a list");
                    SVNErrorManager.error(err, SVNLogType.NETWORK);
                }
                List values = SVNReader.parseTuple("ss", item.getItems(), null);
                String path = SVNReader.getString(values, 0);
                path = path.startsWith("/") ? path.substring(1) : path;
                path = getRepositoryPath(path);
                String mergeInfoToParse = SVNReader.getString(values, 1);
                Map srcsToRangeLists = SVNMergeInfoUtil.parseMergeInfo(new StringBuffer(mergeInfoToParse), null);
                SVNMergeInfo mergeInfo = new SVNMergeInfo(path, srcsToRangeLists);
                pathsToMergeInfos.put(path, mergeInfo);
            }
            return pathsToMergeInfos;
        } catch (SVNException e) {
            closeSession();
            throw e;
        } finally {
            closeConnection();
        }
    }

	public boolean hasCapability(SVNCapability capability) throws SVNException {
	    if (capability == null) {
	        return false;
	    }
		try {
        	openConnection();
        	return myConnection.hasCapability(capability.toString());
        } catch (SVNException e) {
            closeSession();
            throw e;
        } finally {
            closeConnection();
        }
	}

    protected ISVNEditor getCommitEditorInternal(Map locks, boolean keepLocks, SVNProperties revProps, ISVNWorkspaceMediator mediator) throws SVNException {
        try {
            openConnection();
            String logMessage = revProps.getStringValue(SVNRevisionProperty.LOG);
            if (revProps.size() > 1 && !myConnection.isCommitRevprops()) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_NOT_IMPLEMENTED, "Server doesn't support setting arbitrary revision properties during commit");
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }

            write("(w(s(*l)w(*l)))", new Object[]{"commit", logMessage,
                    locks, Boolean.valueOf(keepLocks), revProps});

            authenticate();
            read("", null, false);
            return new SVNCommitEditor(this, myConnection,
                    new SVNCommitEditor.ISVNCommitCallback() {
                        public void run(SVNException error) {
                            if (error != null) {
                                closeSession();
                            }
                            closeConnection();
                        }
                    });
        } catch (SVNException e) {
            closeConnection();
            closeSession();
            throw e;
        }
    }

    protected void replayRangeImpl(long startRevision, long endRevision, long lowRevision, boolean sendDeltas, 
            ISVNReplayHandler handler) throws SVNException {
        Object[] buffer = new Object[]{"replay-range", getRevisionObject(startRevision), 
                getRevisionObject(endRevision), getRevisionObject(lowRevision), Boolean.valueOf(sendDeltas)};

        try {
            openConnection();
            write("(w(nnnw))", buffer);
            authenticate();

            for (long rev = startRevision; rev <= endRevision; rev++) {
                List items = readTuple("wl", false);
                String word = SVNReader.getString(items, 0);
                if (!"revprops".equals(word)) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_SVN_MALFORMED_DATA, 
                            "Expected ''revprops'', found ''{0}''", word);
                    SVNErrorManager.error(err, SVNLogType.NETWORK);
                }
                
                SVNProperties revProps = SVNReader.getProperties(items, 1, null);
                ISVNEditor editor = handler.handleStartRevision(rev, revProps);
                SVNEditModeReader editReader = new SVNEditModeReader(myConnection, editor, true);
                editReader.driveEditor();
                handler.handleEndRevision(rev, revProps, editor);
            }
            read("", null, false);
        } catch (SVNException svne) {
            closeSession();
            handleUnsupportedCommand(svne, "Server doesn't support the replay-range command");
        } finally {
            closeConnection();
        }
    }

    protected long getDeletedRevisionImpl(String path, long pegRevision, long endRevision) throws SVNException {
        try {
            openConnection();
            path = getLocationRelativePath(path);
            Long srev = getRevisionObject(pegRevision);
            Long erev = getRevisionObject(endRevision);
            Object[] buffer = new Object[] { "get-deleted-rev", path, srev, erev };
            write("(w(snn))", buffer);
            authenticate();
            List values = read("r", null, false);
            return SVNReader.getLong(values, 0);
        } catch (SVNException e) {
            closeSession();
            handleUnsupportedCommand(e, "'get-deleted-rev' not implemented");
        } finally {
            closeConnection();
        }
        return INVALID_REVISION; 
    }

    private static boolean getRecurseFromDepth(SVNDepth depth) {
        return depth == null || depth == SVNDepth.UNKNOWN || depth.compareTo(SVNDepth.FILES) > 0;
    }
    
    private static String ensureAbsolutePath(String path) {
        if (path != null) {
            path = SVNPathUtil.canonicalizePath(path);
        }
        if (path != null && (path.length() == 0 || path.charAt(0) != '/')) {
            return "/" + path;
        }
        return path;
    }

    private ISVNEditor getDepthFilterEditor(ISVNEditor editor, SVNDepth depth, boolean hasTarget) {
        if (depth != SVNDepth.FILES && depth != SVNDepth.INFINITY) {
            return SVNDepthFilterEditor.getDepthFilterEditor(depth, editor, hasTarget);
        }
        return editor;
    }

}
