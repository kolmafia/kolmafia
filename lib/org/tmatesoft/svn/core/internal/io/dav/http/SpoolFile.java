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
package org.tmatesoft.svn.core.internal.io.dav.http;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SpoolFile {

    private static final long LIMIT = 1024*1024*512; // 512MB
    private static final long MEMORY_TRESHOLD = 1024*100; // 100KB
    
    private File myDirectory;
    private LinkedList<File> myFiles;
    private ByteArrayOutputStream myBuffer;

    public SpoolFile(File directory) {
        myDirectory = directory;
        myFiles = new LinkedList<File>();
        myBuffer = new ByteArrayOutputStream();
    }
    
    public OutputStream openForWriting() {
        return new SpoolOutputStream();
    }
    
    public InputStream openForReading() {
        return new SpoolInputStream();
    }
    
    public void delete() throws SVNException {
        for (Iterator<File> files = myFiles.iterator(); files.hasNext();) {
            final File file = files.next();
            SVNFileUtil.deleteFile(file);
        }
        myBuffer = null;
    }
    
    private class SpoolInputStream extends InputStream {
        
        private File myCurrentFile;
        private long myCurrentSize;
        private int myBufferOffset;
        private InputStream myCurrentInput;

        public int read() throws IOException {
            byte[] buffer = new byte[1];
            int read = read(buffer);
            if (read != 1) {
                return -1;
            }
            return buffer[0] & 0xFF;
        }

        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        public int read(byte[] b, int off, int len) throws IOException {
            if (myBuffer != null) {
                int bufferSize = myBuffer.size() - myBufferOffset;
                if (bufferSize <= 0) {
                    return -1;
                }
                int toRead = Math.min(bufferSize, len);
                
                byte[] buffer = myBuffer.toByteArray();
                System.arraycopy(buffer, myBufferOffset, b, off, toRead);
                myBufferOffset += toRead;
                return toRead;
            }
            
            int read = 0;
            while(len - read > 0) {
                if (myCurrentFile == null) {
                    if (myFiles.isEmpty()) {
                        SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, 
                                "FAILED TO READ SPOOLED RESPONSE FULLY (no more files): " + (read == 0 ? -1 : read));
                        return read == 0 ? -1 : read;
                    }
                    openNextFile();
                }
                int toRead = (int) Math.min(len - read, myCurrentSize);
                int wasRead = myCurrentInput.read(b, off + read, toRead);
                if (wasRead < 0) {
                    SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, 
                            "FAILED TO READ SPOOLED RESPONSE FULLY (cannot read more from the current file): " + (read == 0 ? -1 : read));
                    return read == 0 ? -1 : read;
                }
                read += wasRead;
                myCurrentSize -= wasRead;
                if (myCurrentSize == 0) {
                    SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "SPOOLED RESPONSE FULLY READ");
                    closeCurrentFile();
                }
            }
            return read;
        }

        private void openNextFile() throws IOException {
            myCurrentFile = (File) myFiles.removeFirst();
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "READING SPOOLED FILE: " + myCurrentFile);
            myCurrentSize = myCurrentFile.length();
            SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "ABOUT TO READ: " + myCurrentSize);
            try {
                myCurrentInput = SVNFileUtil.openFileForReading(myCurrentFile, SVNLogType.NETWORK);
            } catch (SVNException e) {
                if (e.getCause() instanceof IOException) {
                    throw (IOException) e.getCause();
                }
                throw new IOException(e.getMessage());
            }
        }

        public long skip(long n) throws IOException {
            if (myBuffer != null) {
                int bufferSize = myBuffer.size() - myBufferOffset;
                if (bufferSize <= 0) {
                    return 0;
                }

                long toSkip = Math.min(bufferSize, n);
                myBufferOffset += (int) toSkip;
                return toSkip;
            }
            int skipped = 0;
            while(n - skipped > 0) {
                if (myCurrentFile == null) {
                    if (myFiles.isEmpty()) {
                        return skipped == 0 ? -1 : skipped;
                    }
                    openNextFile();
                }
                long toSkip = Math.min(n - skipped, myCurrentSize);
                long wasSkipped = myCurrentInput.skip(toSkip);
                if (wasSkipped < 0) {
                    return skipped == 0 ? -1 : skipped;
                }
                skipped += wasSkipped;
                myCurrentSize -= wasSkipped;
                if (myCurrentSize == 0) {
                    closeCurrentFile();
                }
            }
            return skipped;
        }

        private void closeCurrentFile() throws IOException {
            try {
                myCurrentInput.close();
            } finally {
                try {
                    SVNFileUtil.deleteFile(myCurrentFile);
                } catch (SVNException e) {

                }
                myCurrentFile = null;
            }
        }

        public void close() throws IOException {
            if (myCurrentFile != null) {
                closeCurrentFile();
            }
            myBuffer = null;
            myBufferOffset = 0;
        }
    }
    
    
    private class SpoolOutputStream extends OutputStream {
        
        private OutputStream myCurrentOutput;
        private long myCurrentSize;
        
        public void write(int b) throws IOException {
            write(new byte[] {(byte) (b & 0xFF)});
        }

        public void write(byte[] b) throws IOException {
            write(b, 0, b.length);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            if (myBuffer != null) {
                myBuffer.write(b, off, len);
                if (myBuffer.size() < MEMORY_TRESHOLD) {
                    return;
                }
            }
            if (myCurrentOutput == null) {
                // open first file.
                File file = createNextFile();
                SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "SPOOLING RESPONSE TO FILE: " + file);
                myFiles.add(file);
                try {
                    myCurrentOutput = SVNFileUtil.openFileForWriting(file);
                } catch (SVNException e) {
                    if (e.getCause() instanceof IOException) {
                        throw (IOException) e.getCause();
                    }
                    throw new IOException(e.getMessage());
                }
            } 

            if (myBuffer != null) {
                myBuffer.close();
                myBuffer.writeTo(myCurrentOutput);
                myCurrentSize += myBuffer.size();
                myBuffer = null;
            } else {
                myCurrentOutput.write(b, off, len);
                myCurrentSize += len;
            }
            if (myCurrentSize >= LIMIT) {
                close();
            }
        }

        public void close() throws IOException {
            if (myCurrentOutput != null) {
                try {
                    myCurrentOutput.close();
                    SVNDebugLog.getDefaultLog().logFine(SVNLogType.NETWORK, "SPOOLED: " + myCurrentSize);
                } finally {
                    myCurrentOutput = null;
                }
            }
            myCurrentSize = 0;
        }

        public void flush() throws IOException {
            if (myCurrentOutput != null) {
                myCurrentOutput.flush();
            }
        }
        
        private File createNextFile() throws IOException {
            File file = File.createTempFile("svnkit.", ".spool", myDirectory);
            file.createNewFile();
            return file;
        }
        
    }

}
