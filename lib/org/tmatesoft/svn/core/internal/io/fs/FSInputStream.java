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
package org.tmatesoft.svn.core.internal.io.fs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.internal.delta.SVNDeltaCombiner;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSInputStream extends InputStream {

    private LinkedList myRepStateList = new LinkedList();
    private int myChunkIndex;
    private boolean isChecksumFinalized;
    private String myHexChecksum;
    private long myLength;
    private long myOffset;
    private MessageDigest myDigest;
    private ByteBuffer myBuffer;
    private SVNDeltaCombiner myCombiner;

    private FSInputStream(SVNDeltaCombiner combiner, FSRepresentation representation, FSFS owner) throws SVNException {
        myCombiner = combiner;
        myChunkIndex = 0;
        isChecksumFinalized = false;
        myHexChecksum = representation.getMD5HexDigest();
        myOffset = 0;
        myLength = representation.getExpandedSize();
        try {
            myDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "MD5 implementation not found: {0}", nsae.getLocalizedMessage());
            SVNErrorManager.error(err, nsae, SVNLogType.FSFS);
        }

        try {
            buildRepresentationList(representation, myRepStateList, owner);
        } catch (SVNException svne) {
            /*
             * Something terrible has happened while building rep list, need to
             * close any files still opened
             */
            close();
            throw svne;
        }
    }

    public static InputStream createDeltaStream(SVNDeltaCombiner combiner, FSRevisionNode fileNode, FSFS owner) throws SVNException {
        if (fileNode == null) {
            return SVNFileUtil.DUMMY_IN;
        } else if (fileNode.getType() != SVNNodeKind.FILE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_FILE, "Attempted to get textual contents of a *non*-file node");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        FSRepresentation representation = fileNode.getTextRepresentation();
        if (representation == null) {
            return SVNFileUtil.DUMMY_IN;
        }
        return new FSInputStream(combiner, representation, owner);
    }

    public static InputStream createDeltaStream(SVNDeltaCombiner combiner, FSRepresentation fileRep, FSFS owner) throws SVNException {
        if (fileRep == null) {
            return SVNFileUtil.DUMMY_IN;
        }
        return new FSInputStream(combiner, fileRep, owner);
    }

    public int read(byte[] buf, int offset, int length) throws IOException {
        try {
            return readContents(buf, offset, length);
        } catch (SVNException svne) {
            throw new IOException(svne.getMessage());
        }
    }

    public int read() throws IOException {
        byte[] buf = new byte[1];
        int r = read(buf, 0, 1);
        if (r < 0) {
            return -1;
        }
        return buf[0] & 0xFF;
    }

    private int readContents(byte[] buf, int offset, int length) throws SVNException {
        length = getContents(buf, offset, length);
        if (!isChecksumFinalized && length >= 0) {
            myDigest.update(buf, offset, length);
            myOffset += length;

            if (myOffset == myLength) {
                isChecksumFinalized = true;
                String hexDigest = SVNFileUtil.toHexDigest(myDigest);

                if (!myHexChecksum.equals(hexDigest)) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Checksum mismatch while reading representation:\n   expected:  {0}\n     actual:  {1}", new Object[] {
                            myHexChecksum, hexDigest
                    });
                    SVNErrorManager.error(err, SVNLogType.FSFS);
                }
            }
        }

        return length;
    }

    private int getContents(byte[] buffer, int offset, int length) throws SVNException {
        int remaining = length;
        int targetPos = offset;
        int read = 0;

        while (remaining > 0) {

            if (myBuffer != null && myBuffer.hasRemaining()) {
                int copyLength = Math.min(myBuffer.remaining(), remaining);
                /* Actually copy the data. */
                myBuffer.get(buffer, targetPos, copyLength);
                targetPos += copyLength;
                remaining -= copyLength;
                read += copyLength;
            } else {
                FSRepresentationState resultState = (FSRepresentationState) myRepStateList.getFirst();
                if (resultState.myOffset == resultState.myEnd) {
                    if (read == 0) {
                        read = -1;
                    }
                    break;
                }
                myCombiner.reset();
                for (ListIterator states = myRepStateList.listIterator(); states.hasNext();) {
                    FSRepresentationState curState = (FSRepresentationState) states.next();

                    while (curState.myChunkIndex < myChunkIndex) {
                        myCombiner.skipWindow(curState.myFile);
                        curState.myChunkIndex++;
                        curState.myOffset = curState.myFile.position();
                        if (curState.myOffset >= curState.myEnd) {
                            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Reading one svndiff window read beyond the end of the representation");
                            SVNErrorManager.error(err, SVNLogType.FSFS);
                        }
                    }
                    SVNDiffWindow window = myCombiner.readWindow(curState.myFile, curState.myVersion);
                    ByteBuffer target = myCombiner.addWindow(window);
                    curState.myChunkIndex++;
                    curState.myOffset = curState.myFile.position();
                    if (target != null) {
                        myBuffer = target;
                        myChunkIndex++;
                        break;
                    }
                }
            }
        }
        return read;
    }

    public void close() {
        for (Iterator states = myRepStateList.iterator(); states.hasNext();) {
            FSRepresentationState state = (FSRepresentationState) states.next();
            if (state.myFile != null) {
                state.myFile.close();
            }
            states.remove();
        }
    }

    private FSRepresentationState buildRepresentationList(FSRepresentation firstRep, LinkedList result, FSFS owner) throws SVNException {
        FSFile file = null;
        FSRepresentation rep = new FSRepresentation(firstRep);
        ByteBuffer buffer = ByteBuffer.allocate(4);
        try {
            while (true) {
                file = owner.openAndSeekRepresentation(rep);
                FSRepresentationState repState = readRepresentationLine(file);
                repState.myFile = file;
                repState.myStart = file.position();
                repState.myOffset = repState.myStart;
                repState.myEnd = repState.myStart + rep.getSize();
                if (!repState.myIsDelta) {
                    return repState;
                }
                buffer.clear();
                int r = file.read(buffer);

                byte[] header = buffer.array();
                if (!(header[0] == 'S' && header[1] == 'V' && header[2] == 'N' && r == 4)) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Malformed svndiff data in representation");
                    SVNErrorManager.error(err, SVNLogType.FSFS);
                }
                repState.myVersion = header[3];
                repState.myChunkIndex = 0;
                repState.myOffset += 4;
                /*
                 * Push this rep onto the list. If it's self-compressed, we're
                 * done.
                 */
                result.addLast(repState);
                if (repState.myIsDeltaVsEmpty) {
                    return null;
                }
                rep.setRevision(repState.myBaseRevision);
                rep.setOffset(repState.myBaseOffset);
                rep.setSize(repState.myBaseLength);
                rep.setTxnId(null);
            }
        } catch (IOException ioe) {
            file.close();
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, ioe.getLocalizedMessage());
            SVNErrorManager.error(err, ioe, SVNLogType.FSFS);
        } catch (SVNException svne) {
            file.close();
            throw svne;
        }
        return null;
    }

    public static FSRepresentationState readRepresentationLine(FSFile file) throws SVNException {
        try {
            String line = file.readLine(160);
            FSRepresentationState repState = new FSRepresentationState();
            repState.myIsDelta = false;
            if (FSRepresentation.REP_PLAIN.equals(line)) {
                return repState;
            }
            if (FSRepresentation.REP_DELTA.equals(line)) {
                /* This is a delta against the empty stream. */
                repState.myIsDelta = true;
                repState.myIsDeltaVsEmpty = true;
                return repState;
            }
            repState.myIsDelta = true;
            repState.myIsDeltaVsEmpty = false;

            /* We have hopefully a DELTA vs. a non-empty base revision. */
            int delimiterInd = line.indexOf(' ');
            if (delimiterInd == -1) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Malformed representation header");
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }

            String header = line.substring(0, delimiterInd);

            if (!FSRepresentation.REP_DELTA.equals(header)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Malformed representation header");
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }

            line = line.substring(delimiterInd + 1);

            try {
                delimiterInd = line.indexOf(' ');
                if (delimiterInd == -1) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Malformed representation header");
                    SVNErrorManager.error(err, SVNLogType.FSFS);
                }
                String baseRevision = line.substring(0, delimiterInd);
                repState.myBaseRevision = Long.parseLong(baseRevision);

                line = line.substring(delimiterInd + 1);
                delimiterInd = line.indexOf(' ');
                if (delimiterInd == -1) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Malformed representation header");
                    SVNErrorManager.error(err, SVNLogType.FSFS);
                }
                String baseOffset = line.substring(0, delimiterInd);
                repState.myBaseOffset = Long.parseLong(baseOffset);

                line = line.substring(delimiterInd + 1);

                repState.myBaseLength = Long.parseLong(line);
            } catch (NumberFormatException nfe) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Malformed representation header");
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
            return repState;
        } catch (SVNException svne) {
            file.close();
            throw svne;
        }
    }

    public static class FSRepresentationState {
        FSFile myFile;
        /* The starting offset for the raw svndiff/plaintext data minus header. */
        long myStart;
        /* The current offset into the file. */
        long myOffset;
        /* The end offset of the raw data. */
        long myEnd;
        /* If a delta, what svndiff version? */
        int myVersion;
        int myChunkIndex;
        boolean myIsDelta;
        boolean myIsDeltaVsEmpty;
        long myBaseRevision;
        long myBaseOffset;
        long myBaseLength;
    }

}
