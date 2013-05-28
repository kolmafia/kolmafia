/*
 * ====================================================================
 * Copyright (c) 2004-2011 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.wc17;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc.ISVNCommitPathHandler;
import org.tmatesoft.svn.core.internal.wc.SVNCommitUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.admin.SVNChecksumInputStream;
import org.tmatesoft.svn.core.internal.wc.admin.SVNChecksumOutputStream;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.PristineContentsInfo;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.WritableBaseInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbInfo.InfoField;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc2.SvnChecksum;
import org.tmatesoft.svn.core.wc2.SvnCommitItem;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNCommitter17 implements ISVNCommitPathHandler {

    private SVNWCContext myContext;
    private Map<String, SvnCommitItem> myCommittables;
    private SVNURL myRepositoryRoot;
    private Map<File, SvnChecksum> myMd5Checksums;
    private Map<File, SvnChecksum> mySha1Checksums;
    private Map<String, SvnCommitItem> myModifiedFiles;
    private SVNDeltaGenerator myDeltaGenerator;
    private Collection<File> deletedPaths;

    public SVNCommitter17(SVNWCContext context, Map<String, SvnCommitItem> committables, SVNURL repositoryRoot, Collection<File> tmpFiles, Map<File, SvnChecksum> md5Checksums,
            Map<File, SvnChecksum> sha1Checksums) {
        myContext = context;
        myCommittables = committables;
        myRepositoryRoot = repositoryRoot;
        myMd5Checksums = md5Checksums;
        mySha1Checksums = sha1Checksums;
        myModifiedFiles = new TreeMap<String, SvnCommitItem>();
        
        deletedPaths = new TreeSet<File>();
    }

    public static SVNCommitInfo commit(SVNWCContext context, Collection<File> tmpFiles, Map<String, SvnCommitItem> committables, SVNURL repositoryRoot, ISVNEditor commitEditor,
            Map<File, SvnChecksum> md5Checksums, Map<File, SvnChecksum> sha1Checksums) throws SVNException {
        SVNCommitter17 committer = new SVNCommitter17(context, committables, repositoryRoot, tmpFiles, md5Checksums, sha1Checksums);
        SVNCommitUtil.driveCommitEditor(committer, committables.keySet(), commitEditor, -1);
        committer.sendTextDeltas(commitEditor);
        return commitEditor.closeEdit();
    }
    
    public Collection<File> getDeletedPaths() {
        return deletedPaths;
    }

    public boolean handleCommitPath(String commitPath, ISVNEditor commitEditor) throws SVNException {
        SvnCommitItem item = myCommittables.get(commitPath);
        myContext.checkCancelled();
        if (item.hasFlag(SvnCommitItem.COPY)) {
            if (item.getCopyFromUrl() == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.BAD_URL, "Commit item ''{0}'' has copy flag but no copyfrom URL", item.getPath());
                SVNErrorManager.error(err, SVNLogType.WC);
            } else if (item.getCopyFromRevision() < 0) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_BAD_REVISION, "Commit item ''{0}'' has copy flag but an invalid revision", item.getPath());
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        }
        boolean closeDir = false;
        File localAbspath = null;
        if (item.getKind() != SVNNodeKind.NONE && item.getPath() != null) {
            localAbspath = item.getPath();
        }
        long rev = item.getRevision();
        SVNEvent event = null;
        if (item.hasFlag(SvnCommitItem.ADD) && item.hasFlag(SvnCommitItem.DELETE)) {
            event = SVNEventFactory.createSVNEvent(localAbspath, item.getKind(), null, SVNRepository.INVALID_REVISION, SVNEventAction.COMMIT_REPLACED, null, null, null);
            event.setPreviousRevision(rev);
        } else if (item.hasFlag(SvnCommitItem.DELETE)) {
            event = SVNEventFactory.createSVNEvent(localAbspath, item.getKind(), null, SVNRepository.INVALID_REVISION, SVNEventAction.COMMIT_DELETED, null, null, null);
            event.setPreviousRevision(rev);
        } else if (item.hasFlag(SvnCommitItem.ADD)) {
            String mimeType = null;
            if (item.getKind() == SVNNodeKind.FILE && localAbspath != null) {
                mimeType = myContext.getProperty(localAbspath, SVNProperty.MIME_TYPE);
            }
            event = SVNEventFactory.createSVNEvent(localAbspath, item.getKind(), mimeType, SVNRepository.INVALID_REVISION, SVNEventAction.COMMIT_ADDED, null, null, null);
            event.setPreviousRevision(item.getCopyFromRevision() >= 0 ? item.getCopyFromRevision() : -1);
            event.setPreviousURL(item.getCopyFromUrl());
        } else if (item.hasFlag(SvnCommitItem.TEXT_MODIFIED) || item.hasFlag(SvnCommitItem.PROPS_MODIFIED)) {
            SVNStatusType contentState = SVNStatusType.UNCHANGED;
            if (item.hasFlag(SvnCommitItem.TEXT_MODIFIED)) {
                contentState = SVNStatusType.CHANGED;
            }
            SVNStatusType propState = SVNStatusType.UNCHANGED;
            if (item.hasFlag(SvnCommitItem.PROPS_MODIFIED) ) {
                propState = SVNStatusType.CHANGED;
            }
            event = SVNEventFactory.createSVNEvent(localAbspath, item.getKind(), null, SVNRepository.INVALID_REVISION, contentState, propState, null, SVNEventAction.COMMIT_MODIFIED, null, null, null);
            event.setPreviousRevision(rev);
        }
        if (event != null) {
            event.setURL(item.getUrl());
            if (myContext.getEventHandler() != null) {
                myContext.getEventHandler().handleEvent(event, ISVNEventHandler.UNKNOWN);
            }
        }
        if (item.hasFlag(SvnCommitItem.DELETE)) {
            try {
                commitEditor.deleteEntry(commitPath, rev);
            } catch (SVNException e) {
                fixError(localAbspath, commitPath, e, item.getKind());
            }
            if (!item.hasFlag(SvnCommitItem.ADD)) {
                deletedPaths.add(localAbspath);
            }
        }
        long cfRev = item.getCopyFromRevision();
        Map<String, SVNPropertyValue> outgoingProperties =  item.getOutgoingProperties();
        boolean fileOpen = false;
        if (item.hasFlag(SvnCommitItem.ADD)) {
            String copyFromPath = getCopyFromPath(item.getCopyFromUrl());
            if (item.getKind() == SVNNodeKind.FILE) {
                commitEditor.addFile(commitPath, copyFromPath, cfRev);
                fileOpen = true;
            } else {
                commitEditor.addDir(commitPath, copyFromPath, cfRev);
                closeDir = true;
            }
            
            if (outgoingProperties != null) {
                for (Iterator<String> propsIter = outgoingProperties.keySet().iterator(); propsIter.hasNext();) {
                    String propName = propsIter.next();
                    SVNPropertyValue propValue = outgoingProperties.get(propName);
                    if (item.getKind() == SVNNodeKind.FILE) {
                        commitEditor.changeFileProperty(commitPath, propName, propValue);
                    } else {
                        commitEditor.changeDirProperty(propName, propValue);
                    }
                }
                outgoingProperties = null;
            }
        }
        if (item.hasFlag(SvnCommitItem.PROPS_MODIFIED)) { // || (outgoingProperties != null && !outgoingProperties.isEmpty())) {
            if (item.getKind() == SVNNodeKind.FILE) {
                if (!fileOpen) {
                    try {
                        commitEditor.openFile(commitPath, rev);
                    } catch (SVNException e) {
                        fixError(localAbspath, commitPath, e, SVNNodeKind.FILE);
                    }
                }
                fileOpen = true;
            } else if (!item.hasFlag(SvnCommitItem.ADD)) {
                // do not open dir twice.
                try {
                    if ("".equals(commitPath)) {
                        commitEditor.openRoot(rev);
                    } else {
                        commitEditor.openDir(commitPath, rev);
                    }
                } catch (SVNException svne) {
                    fixError(localAbspath, commitPath, svne, SVNNodeKind.DIR);
                }
                closeDir = true;
            }
            if (item.hasFlag(SvnCommitItem.PROPS_MODIFIED)) {
                try {
                    sendPropertiesDelta(localAbspath, commitPath, item, commitEditor);
                } catch (SVNException e) {
                    fixError(localAbspath, commitPath, e, item.getKind());
                }
            }
            if (outgoingProperties != null) {
                for (Iterator<String> propsIter = outgoingProperties.keySet().iterator(); propsIter.hasNext();) {
                    String propName = propsIter.next();
                    SVNPropertyValue propValue = outgoingProperties.get(propName);
                    if (item.getKind() == SVNNodeKind.FILE) {
                        commitEditor.changeFileProperty(commitPath, propName, propValue);
                    } else {
                        commitEditor.changeDirProperty(propName, propValue);
                    }
                }
            }
        }
        if (item.hasFlag(SvnCommitItem.TEXT_MODIFIED) && item.getKind() == SVNNodeKind.FILE) {
            if (!fileOpen) {
                try {
                    commitEditor.openFile(commitPath, rev);
                } catch (SVNException e) {
                    fixError(localAbspath, commitPath, e, SVNNodeKind.FILE);
                }
            }
            myModifiedFiles.put(commitPath, item);
        } else if (fileOpen) {
            try {
            commitEditor.closeFile(commitPath, null);
            } catch (SVNException e) {
                fixError(localAbspath, commitPath, e, SVNNodeKind.FILE);
            }
        }
        return closeDir;
    }

    private void fixError(File localAbspath, String path, SVNException e, SVNNodeKind kind) throws SVNException {
        SVNErrorMessage err = e.getErrorMessage();

        if (err.getErrorCode() == SVNErrorCode.FS_NOT_FOUND ||
                err.getErrorCode() == SVNErrorCode.FS_ALREADY_EXISTS ||
                err.getErrorCode() == SVNErrorCode.FS_TXN_OUT_OF_DATE ||
                err.getErrorCode() == SVNErrorCode.RA_DAV_PATH_NOT_FOUND ||
                err.getErrorCode() == SVNErrorCode.RA_DAV_ALREADY_EXISTS ||
                err.hasChildWithErrorCode(SVNErrorCode.RA_OUT_OF_DATE)) {
            if (myContext.getEventHandler() != null) {
                SVNEvent event;
                if (localAbspath != null) {
                    event = SVNEventFactory.createSVNEvent(localAbspath, kind, null, -1, SVNEventAction.FAILED_OUT_OF_DATE, null, err, null);
                } else {
                    //TODO: add baseUrl parameter
                    //TODO: add url-based events
//                    event = SVNEventFactory.createSVNEvent(new File(path).getAbsoluteFile(), kind, null, -1, SVNEventAction.FAILED_OUT_OF_DATE, null, err, null);
                    event = null;
                }

                if (event != null) {
                    myContext.getEventHandler().handleEvent(event, ISVNEventHandler.UNKNOWN);
                }
            }

            err = SVNErrorMessage.create(SVNErrorCode.WC_NOT_UP_TO_DATE,
                    kind == SVNNodeKind.DIR ?
                            "Directory ''{0}'' is out of date" :
                            "File ''{0}'' is out of date",
                    localAbspath);

            throw new SVNException(err);
        } else if (err.hasChildWithErrorCode(SVNErrorCode.FS_NO_LOCK_TOKEN) ||
                err.getErrorCode() == SVNErrorCode.FS_LOCK_OWNER_MISMATCH ||
                err.getErrorCode() == SVNErrorCode.RA_NOT_LOCKED) {
            if (myContext.getEventHandler() != null) {
                SVNEvent event;
                if (localAbspath != null) {
                    event = SVNEventFactory.createSVNEvent(localAbspath, kind, null, -1, SVNEventAction.FAILED_LOCKED, null, err, null);
                } else {
                    //TODO
                    event = null;
                }

                if (event != null) {
                    myContext.getEventHandler().handleEvent(event, ISVNEventHandler.UNKNOWN);
                }
            }

            err = SVNErrorMessage.create(SVNErrorCode.CLIENT_NO_LOCK_TOKEN,
                    kind == SVNNodeKind.DIR ?
                            "Directory ''{0}'' is locked in another working copy" :
                            "File ''{0}'' is locked in another working copy",
                    localAbspath);
            throw new SVNException(err);
        } else if (err.hasChildWithErrorCode(SVNErrorCode.RA_DAV_FORBIDDEN) ||
                err.getErrorCode() == SVNErrorCode.AUTHZ_UNWRITABLE) {
            if (myContext.getEventHandler() != null) {
                SVNEvent event;
                if (localAbspath != null) {
                    event = SVNEventFactory.createSVNEvent(localAbspath, kind, null, -1, SVNEventAction.FAILED_FORBIDDEN_BY_SERVER, null, err, null);
                } else {
                    //TODO
                    event = null;
                }

                if (event != null) {
                    myContext.getEventHandler().handleEvent(event, ISVNEventHandler.UNKNOWN);
                }

                err = SVNErrorMessage.create(SVNErrorCode.CLIENT_FORBIDDEN_BY_SERVER,
                        kind == SVNNodeKind.DIR ?
                                "Changing directory ''{0}'' is forbidden by the server" :
                                "Changing file ''{0}'' is forbidden by the server",
                        localAbspath);
                throw new SVNException(err);
            }
        }

        throw e;
    }

    private String getCopyFromPath(SVNURL url) {
        if (url == null) {
            return null;
        }
        String path = url.getPath();
        if (myRepositoryRoot.getPath().equals(path)) {
            return "/";
        }
        return path.substring(myRepositoryRoot.getPath().length());
    }

    private void sendPropertiesDelta(File localAbspath, String commitPath, SvnCommitItem item, ISVNEditor commitEditor) throws SVNException {
        SVNNodeKind kind = myContext.readKind(localAbspath, false);
        SVNProperties propMods = myContext.getPropDiffs(localAbspath).propChanges;
        for (Object i : propMods.nameSet()) {
            String propName = (String) i;
            SVNPropertyValue propValue = propMods.getSVNPropertyValue(propName);
            if (kind == SVNNodeKind.FILE) {
                commitEditor.changeFileProperty(commitPath, propName, propValue);
            } else {
                commitEditor.changeDirProperty(propName, propValue);
            }
        }
    }

    public void sendTextDeltas(ISVNEditor editor) throws SVNException {
        for (String path : myModifiedFiles.keySet()) {
            SvnCommitItem item = myModifiedFiles.get(path);
            myContext.checkCancelled();
            File itemAbspath = item.getPath();
            if (myContext.getEventHandler() != null) {
                SVNEvent event = SVNEventFactory.createSVNEvent(itemAbspath, SVNNodeKind.FILE, null, SVNRepository.INVALID_REVISION, SVNEventAction.COMMIT_DELTA_SENT, null, null, null);
                myContext.getEventHandler().handleEvent(event, ISVNEventHandler.UNKNOWN);
            }
            
            boolean fulltext = item.hasFlag(SvnCommitItem.ADD);
            TransmittedChecksums transmitTextDeltas = transmitTextDeltas(path, itemAbspath, fulltext, editor);
            SvnChecksum newTextBaseMd5Checksum = transmitTextDeltas.md5Checksum;
            SvnChecksum newTextBaseSha1Checksum = transmitTextDeltas.sha1Checksum;
            if (myMd5Checksums != null) {
                myMd5Checksums.put(itemAbspath, newTextBaseMd5Checksum);
            }
            if (mySha1Checksums != null) {
                mySha1Checksums.put(itemAbspath, newTextBaseSha1Checksum);
            }
        }
    }

    private static class TransmittedChecksums {

        public SvnChecksum md5Checksum;
        public SvnChecksum sha1Checksum;
    }

    private TransmittedChecksums transmitTextDeltas(String path, File localAbspath, boolean fulltext, ISVNEditor editor) throws SVNException {
        InputStream localStream = SVNFileUtil.DUMMY_IN;
        InputStream baseStream = SVNFileUtil.DUMMY_IN;
        SvnChecksum expectedMd5Checksum = null;
        SvnChecksum localMd5Checksum = null;
        SvnChecksum verifyChecksum = null;
        SVNChecksumOutputStream localSha1ChecksumStream = null;
        SVNChecksumInputStream verifyChecksumStream = null;
        SVNErrorMessage error = null;
        File newPristineTmpAbspath = null;
        
        try {
            localStream = myContext.getTranslatedStream(localAbspath, localAbspath, true, false);
            WritableBaseInfo openWritableBase = myContext.openWritableBase(localAbspath, false, true);
            OutputStream newPristineStream = openWritableBase.stream;
            newPristineTmpAbspath = openWritableBase.tempBaseAbspath;
            localSha1ChecksumStream = openWritableBase.sha1ChecksumStream;
            localStream = new CopyingStream(newPristineStream, localStream);
            File baseFile = null;
            if (!fulltext) {
                PristineContentsInfo pristineContents = myContext.getPristineContents(localAbspath, true, true);
                baseFile = pristineContents.path;
                baseStream = pristineContents.stream;
                if (baseStream == null) {
                    baseStream = SVNFileUtil.DUMMY_IN;
                }
                expectedMd5Checksum = myContext.getDb().readInfo(localAbspath, InfoField.checksum).checksum;
                if (expectedMd5Checksum != null && expectedMd5Checksum.getKind() != SvnChecksum.Kind.md5) {
                    expectedMd5Checksum = myContext.getDb().getPristineMD5(localAbspath, expectedMd5Checksum);
                }
                if (expectedMd5Checksum != null) {
                    verifyChecksumStream = new SVNChecksumInputStream(baseStream, SVNChecksumInputStream.MD5_ALGORITHM);
                    baseStream = verifyChecksumStream;
                } else {
                    expectedMd5Checksum = new SvnChecksum(SvnChecksum.Kind.md5, SVNFileUtil.computeChecksum(baseFile));
                }
            }
            editor.applyTextDelta(path, expectedMd5Checksum!=null ? expectedMd5Checksum.getDigest() : null);
            if (myDeltaGenerator == null) {
                myDeltaGenerator = new SVNDeltaGenerator();
            }
            localMd5Checksum = new SvnChecksum(SvnChecksum.Kind.md5, myDeltaGenerator.sendDelta(path, baseStream, 0, localStream, editor, true));

            if (verifyChecksumStream != null) {
                //SVNDeltaGenerator#sendDelta doesn't guarantee to read the whole stream (e.g. if baseStream has no data, it is not touched at all)
                //so we read verifyChecksumStream to force MD5 calculation
                readRemainingStream(verifyChecksumStream, baseFile);

                verifyChecksum = new SvnChecksum(SvnChecksum.Kind.md5, verifyChecksumStream.getDigest());
            }
        } catch (SVNException svne) {
            error = svne.getErrorMessage().wrap("While preparing ''{0}'' for commit", localAbspath);
        } finally {
            SVNFileUtil.closeFile(localStream);
            SVNFileUtil.closeFile(baseStream);
        }
        if (expectedMd5Checksum != null && verifyChecksum != null && !expectedMd5Checksum.equals(verifyChecksum)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CHECKSUM_MISMATCH, "Checksum mismatch for ''{0}''; expected: ''{1}'', actual: ''{2}''", new Object[] {
                    localAbspath, expectedMd5Checksum.getDigest(), verifyChecksum.getDigest()
            });
            SVNErrorMessage corruptedBaseErr = SVNErrorMessage.create(SVNErrorCode.WC_CORRUPT_TEXT_BASE);
            SVNErrorManager.error(corruptedBaseErr, err, SVNLogType.WC);
        }
        if (error != null) {
            SVNErrorManager.error(error, SVNLogType.WC);
        }
        try {
            editor.closeFile(path, localMd5Checksum!=null ? localMd5Checksum.getDigest() : null);
        } catch (SVNException e) {
            fixError(localAbspath, path, e, SVNNodeKind.FILE);
        }
        SvnChecksum localSha1Checksum = new SvnChecksum(SvnChecksum.Kind.sha1, localSha1ChecksumStream.getDigest());
        myContext.getDb().installPristine(newPristineTmpAbspath, localSha1Checksum, localMd5Checksum);
        TransmittedChecksums result = new TransmittedChecksums();
        result.md5Checksum = localMd5Checksum;
        result.sha1Checksum = localSha1Checksum;
        return result;
    }

    private void readRemainingStream(SVNChecksumInputStream verifyChecksumStream, File sourceFile) throws SVNException {
        final byte[] buffer = new byte[1024];

        int bytesRead;
        do {
            try {
                bytesRead = verifyChecksumStream.read(buffer);
            } catch (IOException e) {
                SVNErrorMessage err;
                if (sourceFile != null) {
                    err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot read from file ''{0}'': {1}", new Object[]{
                            sourceFile, e.getMessage()
                    });
                } else {
                    err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "Cannot read from stream: {0}", new Object[]{
                            sourceFile, e.getMessage()
                    });
                }
                SVNErrorManager.error(err, Level.FINE, SVNLogType.WC);
                return;
            }
        } while (bytesRead >= 0);
    }

    private class CopyingStream extends FilterInputStream {

        private OutputStream myOutput;

        public CopyingStream(OutputStream out, InputStream in) {
            super(in);
            myOutput = out;
        }

        public int read() throws IOException {
            int r = super.read();
            if (r != -1) {
                myOutput.write(r);
            }
            return r;
        }

        public int read(byte[] b) throws IOException {
            int r = super.read(b);
            if (r != -1) {
                myOutput.write(b, 0, r);
            }
            return r;
        }

        public int read(byte[] b, int off, int len) throws IOException {
            int r = super.read(b, off, len);
            if (r != -1) {
                myOutput.write(b, off, r);
            }
            return r;
        }

        public void close() throws IOException {
            try{
                myOutput.close();
            } finally {
                super.close();
            }
        }

    }

}
