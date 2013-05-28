/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.io.fs;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.tmatesoft.svn.core.ISVNCanceller;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSRecoverer {
    private FSFS myOwner;
    private ISVNCanceller myCanceller;
    
    public FSRecoverer(FSFS owner, ISVNCanceller canceller) {
        myOwner = owner;
        myCanceller = canceller == null ? ISVNCanceller.NULL : canceller;
    }
    
    public void runRecovery() throws SVNException {
        FSWriteLock writeLock = FSWriteLock.getWriteLockForDB(myOwner);
        synchronized (writeLock) {
            try {
                writeLock.lock();
                recover();
            } finally {
                writeLock.unlock();
                FSWriteLock.release(writeLock);
            }
        }
    }
    
    private void recover() throws SVNException {
        String nextNodeID = null;
        String nextCopyID = null;
        long maxRev = getLargestRevision();
        long youngestRev = myOwner.getYoungestRevision();
        
        if (youngestRev > maxRev) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Expected current rev to be <= {0} but found {1}", 
                    new Object[] { String.valueOf(maxRev), String.valueOf(youngestRev) });
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        
        if (myOwner.getDBFormat() < FSFS.MIN_NO_GLOBAL_IDS_FORMAT) {
            long[] rootOffset = { -1 };
            String[] maxNodeID = { "0" };
            String[] maxCopyID = { "0" };
            for (long rev = 0; rev <= maxRev; rev++) {
                myCanceller.checkCancelled();
                FSFile revFile = null;
                try {
                    revFile = myOwner.getPackOrRevisionFSFile(rev);
                    FSRepositoryUtil.loadRootChangesOffset(myOwner, rev, revFile, rootOffset, null);
                    findMaxIDs(rev, revFile, rootOffset[0], maxNodeID, maxCopyID);
                } finally {
                    if (revFile != null) {
                        revFile.close();
                    }
                }
            }
            nextNodeID = FSRepositoryUtil.generateNextKey(maxNodeID[0]);
            nextCopyID = FSRepositoryUtil.generateNextKey(maxCopyID[0]);
        }
        
        File revpropFile = null;
        try {
            revpropFile = myOwner.getRevisionPropertiesFile(maxRev, false);
        } catch (SVNException svne) {
            if (svne.getErrorMessage().getErrorCode() == SVNErrorCode.FS_NO_SUCH_REVISION) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Revision {0} has a revs file but no revprops file", 
                        String.valueOf(maxRev));
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
            throw svne;
        }
        
        if (!revpropFile.isFile()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Revision {0} has a non-file where its revprops file should be", 
                    String.valueOf(maxRev));
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        
        try {
            myOwner.writeCurrentFile(maxRev, nextNodeID, nextCopyID);
        } catch (IOException ioe) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, ioe.getLocalizedMessage());
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
    }
    
    private void findMaxIDs(long rev, FSFile revFile, long offset, String[] maxNodeID, String[] maxCopyID) throws SVNException {
        revFile.seek(offset);
        Map headers = null;
        try {
            headers = revFile.readHeader();
        } finally{
            revFile.close();
        }

        String revNodeIDStr = (String) headers.get(FSRevisionNode.HEADER_ID);
        FSID revNodeID = FSID.fromString(revNodeIDStr);
        
        SVNNodeKind nodeKind = SVNNodeKind.parseKind((String) headers.get(FSRevisionNode.HEADER_TYPE));
        if (nodeKind != SVNNodeKind.DIR) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, 
                    "Recovery encountered a non-directory node");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        
        String textRep = (String) headers.get(FSRevisionNode.HEADER_TEXT);
        if (textRep == null) {
            return;
        }

        FSRevisionNode revNode = new FSRevisionNode();
        revNode.setId(revNodeID);
        revNode.setType(nodeKind);
        FSRevisionNode.parseRepresentationHeader(textRep, revNode, null, true, false);
        if (revNode.getTextRepresentation().getRevision() != rev) {
            return;
        }

        revFile.seek(revNode.getTextRepresentation().getOffset());
        FSInputStream.FSRepresentationState repState = FSInputStream.readRepresentationLine(revFile);
        if (repState.myIsDelta) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, 
                    "Recovery encountered a deltified directory representation");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        
        SVNProperties rawEntries = revFile.readProperties(false, false);
        for (Iterator entriesIter = rawEntries.nameSet().iterator(); entriesIter.hasNext();) {
            String name = (String) entriesIter.next();
            String unparsedEntry = rawEntries.getStringValue(name);
            int spaceInd = unparsedEntry.indexOf(' ');
            if (spaceInd == -1 || spaceInd == unparsedEntry.length() - 1 || spaceInd == 0) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Directory entry corrupt");
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
            
            String kindStr = unparsedEntry.substring(0, spaceInd);
            
            SVNNodeKind kind = SVNNodeKind.parseKind(kindStr);
            if (kind != SVNNodeKind.DIR && kind != SVNNodeKind.FILE) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Directory entry corrupt");
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
            
            String rawID = unparsedEntry.substring(spaceInd + 1);
            FSID id = FSID.fromString(rawID);
            if (id.getRevision() != rev) {
                continue;
            }
            
            String nodeID = id.getNodeID();
            String copyID = id.getCopyID();
            if (nodeID.compareTo(maxNodeID[0]) > 0) {
                maxNodeID[0] = nodeID;
            }
            if (copyID.compareTo(maxCopyID[0]) > 0) {
                maxCopyID[0] = copyID;
            }
            
            if (kind == SVNNodeKind.FILE) {
                continue;
            }
            findMaxIDs(rev, revFile, id.getOffset(), maxNodeID, maxCopyID);
        }
    }
    
    private long getLargestRevision() throws SVNException {
        long right = 1;
        while (true) {
            try {
                myOwner.getPackOrRevisionFSFile(right); 
            } catch (SVNException svne) {
                if (svne.getErrorMessage().getErrorCode() == SVNErrorCode.FS_NO_SUCH_REVISION) {
                    break;
                } 
                throw svne;
            }
            right <<= 1;
        }
        
        long left = right >> 1;
        
        while (left + 1 < right) {
            long probe = left + (right - left)/2;
            try {
                myOwner.getPackOrRevisionFSFile(probe); 
                left = probe;
            } catch (SVNException svne) {
                if (svne.getErrorMessage().getErrorCode() == SVNErrorCode.FS_NO_SUCH_REVISION) {
                    right = probe;
                } else {
                    throw svne;
                }
            }
        }
        return left;
    }
}
