package org.tmatesoft.svn.core.io;

import org.tmatesoft.svn.core.wc2.SvnChecksum;

import java.io.InputStream;

public interface ISVNWorkingCopyContentMediator {
    InputStream getContentAsStream(SvnChecksum checksum);
}
