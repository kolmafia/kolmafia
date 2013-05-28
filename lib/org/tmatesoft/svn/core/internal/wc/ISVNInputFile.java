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
package org.tmatesoft.svn.core.internal.wc;

import java.io.IOException;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public interface ISVNInputFile {

    public void seek(long pos) throws IOException;

    public long getFilePointer() throws IOException;
    
    public int read() throws IOException;
    
    public int read(byte[] b) throws IOException;
    
    public int read(byte[] b, int off, int len) throws IOException;
    
    public long length() throws IOException;
    
    public void close() throws IOException;

    public static final ISVNInputFile DUMMY_INPUT_FILE = new ISVNInputFile() {

        public void seek(long pos) throws IOException {
        }

        public long getFilePointer() throws IOException {
            return 0;
        }
        
        public int read() throws IOException {
            return -1;
        }
        
        public int read(byte[] b) throws IOException {
            return -1;
        }
        
        public int read(byte[] b, int off, int len) throws IOException {
            return -1;
        }
        
        public void close() throws IOException {
        }

        public long length() throws IOException {
            return 0;
        }
        
    };
    
}
